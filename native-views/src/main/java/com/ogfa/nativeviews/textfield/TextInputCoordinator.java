package com.ogfa.nativeviews.textfield;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.text.Editable;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns Canvas text fields and bridges the focused field to Android's input method.
 *
 * <p>The host view delegates {@code onCreateInputConnection()},
 * {@code onCheckIsTextEditor()}, touch, draw, and optional hardware key events to this
 * group.</p>
 */
public final class TextInputCoordinator implements TextFieldHost, AutoCloseable {

    private final View hostView;
    private final InputMethodManager inputMethodManager;
    private final ArrayList<TextField> drawingOrder = new ArrayList<>();
    private final Map<String, TextField> fieldsById = new LinkedHashMap<>();

    private TextField focusedField;
    private TextField touchTarget;
    private FieldInputConnection inputConnection;
    private boolean hideKeyboardWhenTouchOutside = true;

    public TextInputCoordinator(View hostView) {
        this.hostView = Objects.requireNonNull(
                hostView,
                "Host view cannot be null."
        );
        inputMethodManager = (InputMethodManager) hostView.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        hostView.setFocusable(true);
        hostView.setFocusableInTouchMode(true);
    }

    public void register(TextField field) {
        Objects.requireNonNull(field, "TextField cannot be null.");
        if (fieldsById.containsKey(field.getId())) {
            throw new IllegalArgumentException(
                    "TextInputCoordinator already contains ID: " + field.getId()
            );
        }
        drawingOrder.add(field);
        fieldsById.put(field.getId(), field);
        invalidateComponent();
    }

    public TextField find(String id) {
        return fieldsById.get(id);
    }

    public boolean contains(String id) {
        return fieldsById.containsKey(id);
    }

    public boolean unregister(String id) {
        TextField field = fieldsById.remove(id);
        if (field == null) {
            return false;
        }
        if (field == focusedField) {
            clearFocus();
        }
        drawingOrder.remove(field);
        field.detach();
        invalidateComponent();
        return true;
    }

    public int size() {
        return drawingOrder.size();
    }

    public boolean isEmpty() {
        return drawingOrder.isEmpty();
    }

    public TextField getFocusedField() {
        return focusedField;
    }

    public boolean hasFocusedField() {
        return focusedField != null;
    }

    public TextInputCoordinator setHideKeyboardWhenTouchOutside(boolean enabled) {
        hideKeyboardWhenTouchOutside = enabled;
        return this;
    }

    public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        for (TextField field : drawingOrder) {
            field.draw(canvas);
        }
        if (focusedField != null) {
            hostView.postInvalidateDelayed(
                    TextField.CURSOR_BLINK_INTERVAL_MS
            );
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchTarget = findTopmostField(event.getX(), event.getY());
                if (touchTarget == null) {
                    if (hideKeyboardWhenTouchOutside) {
                        clearFocus();
                    }
                    return false;
                }
                requestFocus(touchTarget);
                touchTarget.placeCursorFromTouch(event.getX());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (touchTarget == null) {
                    return false;
                }
                touchTarget.placeCursorFromTouch(event.getX());
                return true;

            case MotionEvent.ACTION_UP:
                boolean handled = touchTarget != null;
                touchTarget = null;
                return handled;

            case MotionEvent.ACTION_CANCEL:
                boolean cancelled = touchTarget != null;
                touchTarget = null;
                return cancelled;

            default:
                return false;
        }
    }

    @Override
    public boolean requestFocus(TextField field) {
        if (field == null || !field.isEnabled()) {
            return false;
        }
        if (focusedField != field) {
            if (focusedField != null) {
                focusedField.setFocusedInternal(false);
            }
            focusedField = field;
            focusedField.setFocusedInternal(true);
            inputConnection = null;
        }

        hostView.requestFocus();
        restartInput();
        hostView.post(() -> {
            if (focusedField == field && inputMethodManager != null) {
                inputMethodManager.showSoftInput(
                        hostView,
                        InputMethodManager.SHOW_IMPLICIT
                );
            }
        });
        invalidateComponent();
        return true;
    }

    public void clearFocus() {
        if (focusedField == null) {
            return;
        }
        focusedField.finishComposingText();
        focusedField.setFocusedInternal(false);
        focusedField = null;
        touchTarget = null;
        inputConnection = null;
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    hostView.getWindowToken(),
                    0
            );
            inputMethodManager.restartInput(hostView);
        }
        invalidateComponent();
    }

    @Override
    public void clearFocus(TextField field) {
        if (focusedField == field) clearFocus();
    }

    /**
     * Called by the host view's {@code onCreateInputConnection()} override.
     */
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        Objects.requireNonNull(outAttrs, "EditorInfo cannot be null.");
        if (focusedField == null) {
            return null;
        }
        focusedField.configureEditorInfo(outAttrs);
        inputConnection = new FieldInputConnection(hostView);
        return inputConnection;
    }

    /**
     * Handles hardware delete, arrows, Enter, and printable key events.
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Objects.requireNonNull(event, "KeyEvent cannot be null.");
        if (focusedField == null) {
            return false;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                focusedField.deleteBeforeCursor();
                return true;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                focusedField.deleteAfterCursor();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                moveCursor(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                moveCursor(1);
                return true;
            case KeyEvent.KEYCODE_ENTER:
                return handleEditorAction(
                        focusedField.getImeOptions()
                                & EditorInfo.IME_MASK_ACTION
                );
            default:
                int unicode = event.getUnicodeChar();
                if (unicode != 0 && !Character.isISOControl(unicode)) {
                    focusedField.replaceSelection(
                            new String(Character.toChars(unicode)),
                            1
                    );
                    return true;
                }
                return false;
        }
    }

    public void restartInput() {
        if (focusedField != null && inputMethodManager != null) {
            inputMethodManager.restartInput(hostView);
        }
    }

    public void clear() {
        clearFocus();
        for (TextField field : drawingOrder) {
            field.detach();
        }
        drawingOrder.clear();
        fieldsById.clear();
        invalidateComponent();
    }

    public void release() {
        clear();
    }

    @Override
    public void close() {
        release();
    }

    @Override
    public View getHostView() {
        return hostView;
    }

    @Override
    public void invalidateComponent() {
        hostView.invalidate();
    }

    @Override
    public void postInvalidateComponentOnAnimation() {
        hostView.postInvalidateOnAnimation();
    }

    @Override
    public void updateSelection(TextField field) {
        if (field != focusedField || inputMethodManager == null) {
            return;
        }
        Editable editable = field.getEditable();
        inputMethodManager.updateSelection(
                hostView,
                field.getSelectionStart(),
                field.getSelectionEnd(),
                BaseInputConnection.getComposingSpanStart(editable),
                BaseInputConnection.getComposingSpanEnd(editable)
        );
    }

    private TextField findTopmostField(float x, float y) {
        for (int index = drawingOrder.size() - 1; index >= 0; index--) {
            TextField field = drawingOrder.get(index);
            if (field.contains(x, y)) {
                return field;
            }
        }
        return null;
    }

    private void moveCursor(int codePointDelta) {
        Editable editable = focusedField.getEditable();
        int current = focusedField.getSelectionEnd();
        int target;
        try {
            target = Character.offsetByCodePoints(
                    editable,
                    current,
                    codePointDelta
            );
        } catch (IndexOutOfBoundsException exception) {
            target = codePointDelta < 0 ? 0 : editable.length();
        }
        focusedField.setSelection(target);
    }

    private boolean handleEditorAction(int actionId) {
        if (focusedField == null) {
            return false;
        }
        if (focusedField.performEditorAction(actionId)) {
            return true;
        }
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            return focusNext();
        }
        if (actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_GO
                || actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_SEARCH) {
            clearFocus();
            return true;
        }
        return false;
    }

    private boolean focusNext() {
        if (focusedField == null || drawingOrder.isEmpty()) {
            return false;
        }
        int current = drawingOrder.indexOf(focusedField);
        for (int offset = 1; offset <= drawingOrder.size(); offset++) {
            TextField candidate = drawingOrder.get(
                    (current + offset) % drawingOrder.size()
            );
            if (candidate.isEnabled()) {
                return requestFocus(candidate);
            }
        }
        return false;
    }

    private boolean performContextMenuAction(int id) {
        if (focusedField == null) {
            return false;
        }
        Editable editable = focusedField.getEditable();
        int start = focusedField.getSelectionStart();
        int end = focusedField.getSelectionEnd();
        ClipboardManager clipboard = (ClipboardManager) hostView.getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);

        if (id == android.R.id.selectAll) {
            focusedField.setSelection(0, editable.length());
            return true;
        }
        if (id == android.R.id.copy) {
            if (clipboard != null && start != end) {
                clipboard.setPrimaryClip(ClipData.newPlainText(
                        focusedField.getId(),
                        editable.subSequence(start, end)
                ));
            }
            return true;
        }
        if (id == android.R.id.cut) {
            if (clipboard != null && start != end) {
                clipboard.setPrimaryClip(ClipData.newPlainText(
                        focusedField.getId(),
                        editable.subSequence(start, end)
                ));
                focusedField.replaceSelection("", 1);
            }
            return true;
        }
        if (id == android.R.id.paste) {
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence value = clip.getItemAt(0)
                            .coerceToText(hostView.getContext());
                    focusedField.replaceSelection(value, 1);
                }
            }
            return true;
        }
        return false;
    }

    private final class FieldInputConnection extends BaseInputConnection {

        private FieldInputConnection(View targetView) {
            super(targetView, true);
        }

        @Override
        public Editable getEditable() {
            return focusedField == null ? null : focusedField.getEditable();
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            TextField field = focusedField;
            if (field == null) {
                return false;
            }
            String before = field.getText();
            boolean handled = super.commitText(text, newCursorPosition);
            field.afterImeEdit(before);
            return handled;
        }

        @Override
        public boolean setComposingText(
                CharSequence text,
                int newCursorPosition
        ) {
            TextField field = focusedField;
            if (field == null) {
                return false;
            }
            String before = field.getText();
            boolean handled = super.setComposingText(
                    text,
                    newCursorPosition
            );
            field.afterImeEdit(before);
            return handled;
        }

        @Override
        public boolean finishComposingText() {
            TextField field = focusedField;
            if (field == null) {
                return false;
            }
            boolean handled = super.finishComposingText();
            field.afterImeStateChange();
            return handled;
        }

        @Override
        public boolean setComposingRegion(int start, int end) {
            TextField field = focusedField;
            if (field == null) {
                return false;
            }
            boolean handled = super.setComposingRegion(start, end);
            field.afterImeStateChange();
            return handled;
        }

        @Override
        public boolean deleteSurroundingText(
                int beforeLength,
                int afterLength
        ) {
            TextField field = focusedField;
            if (field == null) {
                return false;
            }
            String before = field.getText();
            boolean handled = super.deleteSurroundingText(
                    beforeLength,
                    afterLength
            );
            field.afterImeEdit(before);
            return handled;
        }

        @Override
        public boolean deleteSurroundingTextInCodePoints(
                int beforeLength,
                int afterLength
        ) {
            TextField field = focusedField;
            if (field == null) {
                return false;
            }
            String before = field.getText();
            boolean handled = super.deleteSurroundingTextInCodePoints(
                    beforeLength,
                    afterLength
            );
            field.afterImeEdit(before);
            return handled;
        }

        @Override
        public boolean setSelection(int start, int end) {
            TextField field = focusedField;
            if (field == null) {
                return false;
            }
            boolean handled = super.setSelection(start, end);
            field.afterImeStateChange();
            return handled;
        }

        @Override
        public boolean performEditorAction(int actionCode) {
            return handleEditorAction(actionCode);
        }

        @Override
        public boolean performContextMenuAction(int id) {
            return TextInputCoordinator.this.performContextMenuAction(id);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && onKeyDown(event.getKeyCode(), event)) {
                return true;
            }
            return super.sendKeyEvent(event);
        }
    }
}

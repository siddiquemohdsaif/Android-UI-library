package com.ogfa.nativeviews.textfield;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;

import androidx.core.content.res.ResourcesCompat;

import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;

import java.util.Objects;

/**
 * A single-line, Canvas-rendered text editor backed by Android's native IME APIs.
 *
 * <p>Instances are owned by a ZLayer. The field draws with an
 * Android {@link Typeface}; it does not create a bitmap for each keystroke.</p>
 */
public final class TextField implements Component {

    public static final long CURSOR_BLINK_INTERVAL_MS = 500L;

    private final String id;
    private final RectF bounds;
    private final SpannableStringBuilder editable = new SpannableStringBuilder();
    private final TextPaint textPaint = new TextPaint(
            Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG
    );
    private final TextPaint hintPaint = new TextPaint(
            Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG
    );
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF contentBounds = new RectF();

    private TextFieldHost owner;
    private String hint;
    private int inputType;
    private int imeOptions;
    private int maxLength;
    private int textColor;
    private int hintColor;
    private int cursorColor;
    private int selectionColor;
    private int backgroundColor;
    private int focusedBackgroundColor;
    private int strokeColor;
    private int focusedStrokeColor;
    private float textSizePx;
    private float horizontalPaddingPx;
    private float verticalPaddingPx;
    private float cornerRadiusPx;
    private float strokeWidthPx;
    private float cursorWidthPx;
    private float horizontalScrollPx;
    private boolean enabled;
    private boolean visible = true;
    private boolean touchCaptured;
    private boolean password;
    private boolean focused;
    private long cursorBlinkStartedAt;
    private OnTextChangedListener textChangedListener;
    private OnEditorActionListener editorActionListener;
    private OnFocusChangedListener focusChangedListener;

    private TextField(Builder builder, View hostView) {
        id = requireId(builder.id);
        bounds = builder.resolveBounds(hostView);
        requireBounds(bounds);

        hint = builder.hint;
        inputType = builder.inputType;
        imeOptions = builder.imeOptions;
        maxLength = builder.maxLength;
        textColor = builder.textColor;
        hintColor = builder.hintColor;
        cursorColor = builder.cursorColor;
        selectionColor = builder.selectionColor;
        backgroundColor = builder.backgroundColor;
        focusedBackgroundColor = builder.focusedBackgroundColor;
        strokeColor = builder.strokeColor;
        focusedStrokeColor = builder.focusedStrokeColor;
        textSizePx = builder.textSizePx > 0f
                ? builder.textSizePx
                : bounds.height() * 0.42f;
        horizontalPaddingPx = builder.horizontalPaddingPx >= 0f
                ? builder.horizontalPaddingPx
                : bounds.height() * 0.18f;
        verticalPaddingPx = builder.verticalPaddingPx >= 0f
                ? builder.verticalPaddingPx
                : bounds.height() * 0.10f;
        cornerRadiusPx = builder.cornerRadiusPx >= 0f
                ? builder.cornerRadiusPx
                : bounds.height() * 0.18f;
        strokeWidthPx = builder.strokeWidthPx;
        cursorWidthPx = builder.cursorWidthPx;
        enabled = builder.enabled;
        password = builder.password;
        textChangedListener = builder.textChangedListener;
        editorActionListener = builder.editorActionListener;
        focusChangedListener = builder.focusChangedListener;

        Typeface typeface = builder.typeface != null
                ? builder.typeface
                : resolveTypeface(builder.context, builder.fontId);
        textPaint.setTypeface(typeface);
        hintPaint.setTypeface(typeface);
        textPaint.setTextSize(textSizePx);
        hintPaint.setTextSize(textSizePx);
        textPaint.setColor(textColor);
        hintPaint.setColor(hintColor);

        backgroundPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidthPx);
        cursorPaint.setColor(cursorColor);
        cursorPaint.setStrokeWidth(cursorWidthPx);
        selectionPaint.setColor(selectionColor);

        if (horizontalPaddingPx * 2f >= bounds.width()
                || verticalPaddingPx * 2f >= bounds.height()) {
            throw new IllegalArgumentException(
                    "TextField padding leaves no drawable content area."
            );
        }

        applyLengthFilter();
        setTextInternal(builder.initialText, false);
    }

    public String getId() {
        return id;
    }

    public RectF getBounds() {
        return new RectF(bounds);
    }

    public String getText() {
        return editable.toString();
    }

    public Editable getEditable() {
        return editable;
    }

    public TextField setText(CharSequence text) {
        setTextInternal(text, true);
        return this;
    }

    public TextField clear() {
        return setText("");
    }

    public TextField setHint(String hint) {
        this.hint = hint == null ? "" : hint;
        invalidate();
        return this;
    }

    public String getHint() {
        return hint;
    }

    public TextField setMaxLength(int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("Maximum length must be greater than zero.");
        }
        this.maxLength = maxLength;
        applyLengthFilter();
        if (editable.length() > maxLength) {
            replaceRange(maxLength, editable.length(), "", false);
        }
        return this;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public TextField setInputType(int inputType) {
        this.inputType = inputType;
        restartInput();
        return this;
    }

    public int getInputType() {
        return inputType;
    }

    public TextField setImeOptions(int imeOptions) {
        this.imeOptions = imeOptions;
        restartInput();
        return this;
    }

    public int getImeOptions() {
        return imeOptions;
    }

    public TextField setPassword(boolean password) {
        this.password = password;
        ensureCursorVisible();
        invalidate();
        return this;
    }

    public boolean isPassword() {
        return password;
    }

    public TextField setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && focused && owner != null) {
            owner.clearFocus(this);
        }
        invalidate();
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isFocused() {
        return focused;
    }

    public TextField requestFocus() {
        if (owner == null) {
            throw new IllegalStateException(
                    "Add the field to a ZLayer before requesting focus."
            );
        }
        owner.requestFocus(this);
        return this;
    }

    public TextField clearFocus() {
        if (owner != null && focused) {
            owner.clearFocus(this);
        }
        return this;
    }

    public TextField setSelection(int index) {
        return setSelection(index, index);
    }

    public TextField setSelection(int start, int end) {
        requireSelection(start, end);
        BaseInputConnection.removeComposingSpans(editable);
        Selection.setSelection(editable, start, end);
        resetCursorBlink();
        ensureCursorVisible();
        updateImeSelection();
        invalidate();
        return this;
    }

    public int getSelectionStart() {
        return normalizedSelectionStart();
    }

    public int getSelectionEnd() {
        return normalizedSelectionEnd();
    }

    public TextField setOnTextChangedListener(
            OnTextChangedListener listener
    ) {
        textChangedListener = listener;
        return this;
    }

    public TextField setOnEditorActionListener(
            OnEditorActionListener listener
    ) {
        editorActionListener = listener;
        return this;
    }

    public TextField setOnFocusChangedListener(
            OnFocusChangedListener listener
    ) {
        focusChangedListener = listener;
        return this;
    }

    @Override
    public void attach(ComponentHost owner) {
        if (!(owner instanceof TextFieldHost)) {
            throw new IllegalArgumentException(
                    "TextField requires a TextFieldHost."
            );
        }
        TextFieldHost textFieldHost = (TextFieldHost) owner;
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException(
                    "TextField already belongs to another group."
            );
        }
        this.owner = textFieldHost;
    }

    void detach() {
        focused = false;
        owner = null;
    }

    void setFocusedInternal(boolean focused) {
        if (this.focused == focused) {
            return;
        }
        this.focused = focused;
        if (focused) {
            resetCursorBlink();
            ensureSelection();
            ensureCursorVisible();
        }
        if (focusChangedListener != null) {
            focusChangedListener.onFocusChanged(id, focused);
        }
        invalidate();
    }

    boolean contains(float x, float y) {
        return visible && enabled && bounds.contains(x, y);
    }

    void placeCursorFromTouch(float touchX) {
        CharSequence displayText = getDisplayText();
        float localX = touchX - contentBounds().left + horizontalScrollPx;
        int bestIndex = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int index = 0; index <= displayText.length(); index++) {
            float x = measurePrefix(displayText, index);
            float distance = Math.abs(localX - x);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        setSelection(Math.min(bestIndex, editable.length()));
    }

    @Override
    public void draw(Canvas canvas) {
        if (!visible) return;
        backgroundPaint.setColor(
                focused ? focusedBackgroundColor : backgroundColor
        );
        canvas.drawRoundRect(
                bounds,
                cornerRadiusPx,
                cornerRadiusPx,
                backgroundPaint
        );

        if (strokeWidthPx > 0f) {
            strokePaint.setColor(focused ? focusedStrokeColor : strokeColor);
            float inset = strokeWidthPx / 2f;
            RectF strokeBounds = new RectF(bounds);
            strokeBounds.inset(inset, inset);
            canvas.drawRoundRect(
                    strokeBounds,
                    Math.max(0f, cornerRadiusPx - inset),
                    Math.max(0f, cornerRadiusPx - inset),
                    strokePaint
            );
        }

        RectF content = contentBounds();
        int saveCount = canvas.save();
        canvas.clipRect(content);

        CharSequence displayText = getDisplayText();
        boolean showHint = displayText.length() == 0;
        TextPaint activePaint = showHint ? hintPaint : textPaint;
        CharSequence value = showHint ? hint : displayText;
        Paint.FontMetrics metrics = activePaint.getFontMetrics();
        float baseline = content.centerY()
                - (metrics.ascent + metrics.descent) / 2f;
        float originX = content.left - horizontalScrollPx;

        if (focused && !password && editable.length() > 0) {
            drawSelection(canvas, content, baseline, originX, displayText);
        }
        canvas.drawText(value, 0, value.length(), originX, baseline, activePaint);

        if (focused && enabled && isCursorVisible()) {
            int cursorIndex = normalizedSelectionEnd();
            float cursorX = originX + measurePrefix(displayText, cursorIndex);
            cursorPaint.setColor(cursorColor);
            canvas.drawLine(
                    cursorX,
                    baseline + metrics.ascent,
                    cursorX,
                    baseline + metrics.descent,
                    cursorPaint
            );
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        switch (event.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                touchCaptured = contains(event.getX(), event.getY());
                if (touchCaptured) {
                    owner.requestFocus(this);
                    placeCursorFromTouch(event.getX());
                }
                return touchCaptured;
            case android.view.MotionEvent.ACTION_MOVE:
                if (!touchCaptured) return false;
                placeCursorFromTouch(event.getX());
                return true;
            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                boolean handled = touchCaptured;
                touchCaptured = false;
                return handled;
            default:
                return touchCaptured;
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public TextField setVisible(boolean visible) {
        this.visible = visible;
        invalidate();
        return this;
    }

    @Override
    public void release() {
        detach();
    }

    private void drawSelection(
            Canvas canvas,
            RectF content,
            float baseline,
            float originX,
            CharSequence displayText
    ) {
        int start = normalizedSelectionStart();
        int end = normalizedSelectionEnd();
        if (start == end) {
            return;
        }
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float left = originX + measurePrefix(displayText, start);
        float right = originX + measurePrefix(displayText, end);
        canvas.drawRect(
                Math.max(content.left, left),
                baseline + metrics.ascent,
                Math.min(content.right, right),
                baseline + metrics.descent,
                selectionPaint
        );
    }

    CharSequence getDisplayText() {
        if (!password) {
            return editable;
        }
        StringBuilder masked = new StringBuilder(editable.length());
        for (int index = 0; index < editable.length(); index++) {
            masked.append('\u2022');
        }
        return masked;
    }

    void replaceSelection(
            CharSequence replacement,
            int newCursorPosition,
            boolean composing
    ) {
        Objects.requireNonNull(replacement, "Replacement text cannot be null.");
        int composingStart = BaseInputConnection.getComposingSpanStart(editable);
        int composingEnd = BaseInputConnection.getComposingSpanEnd(editable);
        int start;
        int end;
        if (composingStart >= 0 && composingEnd >= 0) {
            start = Math.min(composingStart, composingEnd);
            end = Math.max(composingStart, composingEnd);
        } else {
            start = normalizedSelectionStart();
            end = normalizedSelectionEnd();
        }

        String before = editable.toString();
        int originalLength = editable.length();
        BaseInputConnection.removeComposingSpans(editable);
        editable.replace(start, end, replacement);
        int insertedLength = editable.length()
                - (originalLength - (end - start));
        int replacementEnd = start + Math.max(0, insertedLength);
        if (composing && replacementEnd > start) {
            editable.setSpan(
                    new ComposingMarker(),
                    start,
                    replacementEnd,
                    Editable.SPAN_EXCLUSIVE_EXCLUSIVE
                            | Editable.SPAN_COMPOSING
            );
        }

        int cursor = newCursorPosition > 0
                ? replacementEnd + newCursorPosition - 1
                : start + newCursorPosition;
        cursor = clamp(cursor, 0, editable.length());
        Selection.setSelection(editable, cursor);
        afterEdit(before);
    }

    void deleteSurroundingText(int beforeLength, int afterLength) {
        if (beforeLength < 0 || afterLength < 0) {
            throw new IllegalArgumentException(
                    "Surrounding-text lengths cannot be negative."
            );
        }
        int selectionStart = normalizedSelectionStart();
        int selectionEnd = normalizedSelectionEnd();
        int start = selectionStart == selectionEnd
                ? Math.max(0, selectionStart - beforeLength)
                : selectionStart;
        int end = selectionStart == selectionEnd
                ? Math.min(editable.length(), selectionEnd + afterLength)
                : selectionEnd;
        replaceRange(start, end, "", true);
    }

    void finishComposingText() {
        BaseInputConnection.removeComposingSpans(editable);
        updateImeSelection();
        invalidate();
    }

    void setComposingRegion(int start, int end) {
        int safeStart = clamp(Math.min(start, end), 0, editable.length());
        int safeEnd = clamp(Math.max(start, end), 0, editable.length());
        BaseInputConnection.removeComposingSpans(editable);
        if (safeEnd > safeStart) {
            editable.setSpan(
                    new ComposingMarker(),
                    safeStart,
                    safeEnd,
                    Editable.SPAN_EXCLUSIVE_EXCLUSIVE
                            | Editable.SPAN_COMPOSING
            );
        }
        Selection.setSelection(editable, safeEnd);
        resetCursorBlink();
        ensureCursorVisible();
        updateImeSelection();
        invalidate();
    }

    void deleteBeforeCursor() {
        int start = normalizedSelectionStart();
        int end = normalizedSelectionEnd();
        if (start != end) {
            replaceRange(start, end, "", true);
            return;
        }
        if (start == 0) {
            return;
        }
        int previous = Character.offsetByCodePoints(editable, start, -1);
        replaceRange(previous, start, "", true);
    }

    void deleteAfterCursor() {
        int start = normalizedSelectionStart();
        int end = normalizedSelectionEnd();
        if (start != end) {
            replaceRange(start, end, "", true);
            return;
        }
        if (end >= editable.length()) {
            return;
        }
        int next = Character.offsetByCodePoints(editable, end, 1);
        replaceRange(end, next, "", true);
    }

    boolean performEditorAction(int actionId) {
        return editorActionListener != null
                && editorActionListener.onEditorAction(id, actionId);
    }

    void configureEditorInfo(EditorInfo outAttrs) {
        outAttrs.inputType = inputType;
        outAttrs.imeOptions = imeOptions;
        outAttrs.initialSelStart = normalizedSelectionStart();
        outAttrs.initialSelEnd = normalizedSelectionEnd();
        outAttrs.hintText = hint;
        outAttrs.fieldName = id;
    }

    boolean isCursorVisible() {
        long elapsed = SystemClock.uptimeMillis() - cursorBlinkStartedAt;
        return (elapsed / CURSOR_BLINK_INTERVAL_MS) % 2L == 0L;
    }

    void resetCursorBlink() {
        cursorBlinkStartedAt = SystemClock.uptimeMillis();
    }

    void invalidate() {
        if (owner != null) {
            owner.invalidateComponent();
        }
    }

    private void restartInput() {
        if (focused && owner != null) {
            owner.restartInput();
        }
    }

    private void setTextInternal(CharSequence text, boolean notify) {
        Objects.requireNonNull(text, "Text cannot be null.");
        String before = editable.toString();
        BaseInputConnection.removeComposingSpans(editable);
        editable.replace(0, editable.length(), text);
        Selection.setSelection(editable, editable.length());
        ensureCursorVisible();
        if (notify && !before.contentEquals(editable)) {
            notifyTextChanged();
        }
        updateImeSelection();
        invalidate();
    }

    private void replaceRange(
            int start,
            int end,
            CharSequence replacement,
            boolean notify
    ) {
        String before = editable.toString();
        int originalLength = editable.length();
        BaseInputConnection.removeComposingSpans(editable);
        editable.replace(start, end, replacement);
        int insertedLength = editable.length()
                - (originalLength - (end - start));
        Selection.setSelection(
                editable,
                clamp(start + Math.max(0, insertedLength), 0, editable.length())
        );
        if (notify) {
            afterEdit(before);
        } else {
            ensureCursorVisible();
            invalidate();
        }
    }

    private void afterEdit(String before) {
        resetCursorBlink();
        ensureCursorVisible();
        if (!before.contentEquals(editable)) {
            notifyTextChanged();
        }
        updateImeSelection();
        invalidate();
    }

    private void notifyTextChanged() {
        if (textChangedListener != null) {
            textChangedListener.onTextChanged(id, editable.toString());
        }
    }

    private void updateImeSelection() {
        if (owner != null && focused) {
            owner.updateSelection(this);
        }
    }

    private void applyLengthFilter() {
        editable.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(maxLength)
        });
    }

    private void ensureSelection() {
        if (Selection.getSelectionStart(editable) < 0
                || Selection.getSelectionEnd(editable) < 0) {
            Selection.setSelection(editable, editable.length());
        }
    }

    private int normalizedSelectionStart() {
        ensureSelection();
        return Math.min(
                Selection.getSelectionStart(editable),
                Selection.getSelectionEnd(editable)
        );
    }

    private int normalizedSelectionEnd() {
        ensureSelection();
        return Math.max(
                Selection.getSelectionStart(editable),
                Selection.getSelectionEnd(editable)
        );
    }

    private void ensureCursorVisible() {
        RectF content = contentBounds();
        CharSequence displayText = getDisplayText();
        float cursorX = measurePrefix(displayText, normalizedSelectionEnd());
        float availableWidth = Math.max(1f, content.width());
        if (cursorX - horizontalScrollPx > availableWidth) {
            horizontalScrollPx = cursorX - availableWidth;
        } else if (cursorX - horizontalScrollPx < 0f) {
            horizontalScrollPx = cursorX;
        }
        float maximumScroll = Math.max(
                0f,
                textPaint.measureText(displayText, 0, displayText.length())
                        - availableWidth
        );
        horizontalScrollPx = Math.max(
                0f,
                Math.min(horizontalScrollPx, maximumScroll)
        );
    }

    private RectF contentBounds() {
        contentBounds.set(
                bounds.left + horizontalPaddingPx,
                bounds.top + verticalPaddingPx,
                bounds.right - horizontalPaddingPx,
                bounds.bottom - verticalPaddingPx
        );
        return contentBounds;
    }

    private float measurePrefix(CharSequence value, int end) {
        int safeEnd = clamp(end, 0, value.length());
        return textPaint.measureText(value, 0, safeEnd);
    }

    private void requireSelection(int start, int end) {
        if (start < 0 || end < 0
                || start > editable.length()
                || end > editable.length()) {
            throw new IndexOutOfBoundsException(
                    "Selection " + start + ".." + end
                            + " is outside text length " + editable.length() + "."
            );
        }
    }

    private static Typeface resolveTypeface(Context context, int fontId) {
        if (fontId == 0) {
            return Typeface.DEFAULT;
        }
        try {
            Typeface typeface = ResourcesCompat.getFont(context, fontId);
            if (typeface == null) {
                throw new IllegalArgumentException(
                        "Unable to load font resource ID: " + fontId
                );
            }
            return typeface;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Unable to load font resource ID: " + fontId,
                    exception
            );
        }
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("TextField ID cannot be empty.");
        }
        return id;
    }

    private static void requireBounds(RectF bounds) {
        if (bounds == null
                || !Float.isFinite(bounds.left)
                || !Float.isFinite(bounds.top)
                || !Float.isFinite(bounds.right)
                || !Float.isFinite(bounds.bottom)
                || bounds.width() <= 0f
                || bounds.height() <= 0f) {
            throw new IllegalArgumentException(
                    "TextField bounds must be positive and finite."
            );
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class ComposingMarker {
    }

    public interface OnTextChangedListener {
        void onTextChanged(String id, String text);
    }

    public interface OnEditorActionListener {
        boolean onEditorAction(String id, int actionId);
    }

    public interface OnFocusChangedListener {
        void onFocusChanged(String id, boolean focused);
    }

    public static final class Builder implements ComponentFactory<TextField> {
        private final Context context;
        private final String id;
        private final Position position;
        private final float figmaWidth;
        private final float figmaHeight;
        private final RectF explicitBounds;

        private String hint = "";
        private CharSequence initialText = "";
        private int fontId;
        private Typeface typeface = Typeface.DEFAULT;
        private int inputType = android.text.InputType.TYPE_CLASS_TEXT;
        private int imeOptions = EditorInfo.IME_ACTION_DONE;
        private int maxLength = Integer.MAX_VALUE;
        private int textColor = Color.WHITE;
        private int hintColor = 0x88ffffff;
        private int cursorColor = Color.WHITE;
        private int selectionColor = 0x6633b5e5;
        private int backgroundColor = 0x33000000;
        private int focusedBackgroundColor = 0x44000000;
        private int strokeColor = 0x66ffffff;
        private int focusedStrokeColor = Color.WHITE;
        private float textSizePx = -1f;
        private float horizontalPaddingPx = -1f;
        private float verticalPaddingPx = -1f;
        private float cornerRadiusPx = -1f;
        private float strokeWidthPx = 2f;
        private float cursorWidthPx = 3f;
        private boolean enabled = true;
        private boolean password;
        private OnTextChangedListener textChangedListener;
        private OnEditorActionListener editorActionListener;
        private OnFocusChangedListener focusChangedListener;

        public Builder(
                Context context,
                String id,
                Position position,
                float figmaWidth,
                float figmaHeight
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.position = Objects.requireNonNull(
                    position,
                    "Position cannot be null."
            );
            if (figmaWidth <= 0f || figmaHeight <= 0f
                    || !Float.isFinite(figmaWidth)
                    || !Float.isFinite(figmaHeight)) {
                throw new IllegalArgumentException(
                        "Figma width and height must be positive and finite."
                );
            }
            this.figmaWidth = figmaWidth;
            this.figmaHeight = figmaHeight;
            explicitBounds = null;
        }

        public Builder(
                Context context,
                String id,
                Position position,
                Size size
        ) {
            this(
                    context,
                    id,
                    position,
                    Objects.requireNonNull(size, "Size cannot be null.").getWidth(),
                    size.getHeight()
            );
        }

        public Builder(Context context, String id, RectF bounds) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            explicitBounds = new RectF(
                    Objects.requireNonNull(bounds, "Bounds cannot be null.")
            );
            position = null;
            figmaWidth = 0f;
            figmaHeight = 0f;
        }

        public Builder setFont(int fontResourceId) {
            if (fontResourceId == 0) {
                throw new IllegalArgumentException(
                        "Font resource ID cannot be zero."
                );
            }
            fontId = fontResourceId;
            typeface = null;
            return this;
        }

        public Builder setFont(Typeface typeface) {
            this.typeface = Objects.requireNonNull(
                    typeface,
                    "Typeface cannot be null."
            );
            fontId = 0;
            return this;
        }

        public Builder useDefaultFont() {
            typeface = Typeface.DEFAULT;
            fontId = 0;
            return this;
        }

        public Builder setHint(String hint) {
            this.hint = hint == null ? "" : hint;
            return this;
        }

        public Builder setText(CharSequence text) {
            initialText = Objects.requireNonNull(text, "Text cannot be null.");
            return this;
        }

        public Builder setMaxLength(int maxLength) {
            if (maxLength <= 0) {
                throw new IllegalArgumentException(
                        "Maximum length must be greater than zero."
                );
            }
            this.maxLength = maxLength;
            return this;
        }

        public Builder setInputType(int inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder setImeOptions(int imeOptions) {
            this.imeOptions = imeOptions;
            return this;
        }

        public Builder setTextColor(int color) {
            textColor = color;
            return this;
        }

        public Builder setHintColor(int color) {
            hintColor = color;
            return this;
        }

        public Builder setCursorColor(int color) {
            cursorColor = color;
            return this;
        }

        public Builder setSelectionColor(int color) {
            selectionColor = color;
            return this;
        }

        public Builder setBackgroundColor(int normalColor, int focusedColor) {
            backgroundColor = normalColor;
            focusedBackgroundColor = focusedColor;
            return this;
        }

        public Builder setStrokeColor(int normalColor, int focusedColor) {
            strokeColor = normalColor;
            focusedStrokeColor = focusedColor;
            return this;
        }

        public Builder setTextSize(float pixels) {
            textSizePx = requireNonNegative(pixels, "Text size");
            if (textSizePx == 0f) {
                throw new IllegalArgumentException(
                        "Text size must be greater than zero."
                );
            }
            return this;
        }

        public Builder setPadding(float horizontalPixels, float verticalPixels) {
            horizontalPaddingPx = requireNonNegative(
                    horizontalPixels,
                    "Horizontal padding"
            );
            verticalPaddingPx = requireNonNegative(
                    verticalPixels,
                    "Vertical padding"
            );
            return this;
        }

        public Builder setCornerRadius(float pixels) {
            cornerRadiusPx = requireNonNegative(pixels, "Corner radius");
            return this;
        }

        public Builder setStrokeWidth(float pixels) {
            strokeWidthPx = requireNonNegative(pixels, "Stroke width");
            return this;
        }

        public Builder setCursorWidth(float pixels) {
            cursorWidthPx = requireNonNegative(pixels, "Cursor width");
            if (cursorWidthPx == 0f) {
                throw new IllegalArgumentException(
                        "Cursor width must be greater than zero."
                );
            }
            return this;
        }

        public Builder setPassword(boolean password) {
            this.password = password;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setOnTextChangedListener(
                OnTextChangedListener listener
        ) {
            textChangedListener = listener;
            return this;
        }

        public Builder setOnEditorActionListener(
                OnEditorActionListener listener
        ) {
            editorActionListener = listener;
            return this;
        }

        public Builder setOnFocusChangedListener(
                OnFocusChangedListener listener
        ) {
            focusChangedListener = listener;
            return this;
        }

        @Override
        public TextField build(View hostView) {
            return new TextField(this, hostView);
        }

        private RectF resolveBounds(View hostView) {
            if (explicitBounds != null) {
                return new RectF(explicitBounds);
            }
            return position.toRectF(hostView, figmaWidth, figmaHeight);
        }

        private static float requireNonNegative(float value, String label) {
            if (value < 0f || !Float.isFinite(value)) {
                throw new IllegalArgumentException(
                        label + " must be non-negative and finite."
                );
            }
            return value;
        }
    }
}

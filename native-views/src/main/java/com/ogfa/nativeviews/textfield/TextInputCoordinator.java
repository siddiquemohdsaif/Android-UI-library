package com.ogfa.nativeviews.textfield;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build.VERSION;
import android.os.SystemClock;
import android.util.Log;
import android.text.Editable;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TextInputCoordinator implements TextFieldHost, AutoCloseable {
   private static final String PROFILE_TAG = "TextInputProfile";
   private final View hostView;
   private final InputMethodManager inputMethodManager;
   private final ArrayList<TextField> drawingOrder = new ArrayList();
   private final Map<String, TextField> fieldsById = new LinkedHashMap();
   private TextField focusedField;
   private TextField touchTarget;
   private FieldInputConnection inputConnection;
   private boolean hideKeyboardWhenTouchOutside = true;
   private final int touchSlop;
   private float touchDownX;
   private float touchDownY;
   private boolean longPressTriggered;
   private ActionMode selectionActionMode;
   private TextField actionModeField;
   private boolean insertionActionMode;
   private float selectionAnchorOffsetX;
   private float selectionAnchorOffsetY;
   private final FocusTransferProfile focusTransferProfile = new FocusTransferProfile();
   private final Runnable longPressRunnable = this::handleLongPress;
   private final Runnable pendingFocusDismissRunnable = () -> {
      String fieldId = this.focusTransferProfile.expire(SystemClock.uptimeMillis());
      if (fieldId == null) return;
      Log.d(PROFILE_TAG, "event=focus_transfer_expired field=" + fieldId
            + " elapsedMs=" + this.focusTransferProfile.getLastElapsedMs());
      this.hideKeyboardAndRestartInput();
   };

   private void deferFocusedFieldRemoval(TextField field) {
      this.hostView.removeCallbacks(this.longPressRunnable);
      if (this.selectionActionMode != null) this.selectionActionMode.finish();
      field.finishComposingText();
      field.setFocusedInternal(false, false);
      this.focusedField = null;
      this.touchTarget = null;
      this.inputConnection = null;
      this.focusTransferProfile.begin(field.getId(), SystemClock.uptimeMillis());
      this.hostView.removeCallbacks(this.pendingFocusDismissRunnable);
      this.hostView.post(this.pendingFocusDismissRunnable);
      Log.d(PROFILE_TAG, "event=focus_transfer_deferred field=" + field.getId());
   }

   private void handleLongPress() {
      TextField var1 = this.touchTarget;
      if (var1 != null && var1 == this.focusedField) {
         if (var1.selectWordAt(this.touchDownX, this.touchDownY)) {
            this.longPressTriggered = true;
            this.hostView.performHapticFeedback(0);
            this.startSelectionActionMode();
         }
      }
   }

   public TextInputCoordinator(View var1) {
      this.hostView = (View)Objects.requireNonNull(var1, "Host view cannot be null.");
      this.inputMethodManager = (InputMethodManager)var1.getContext().getSystemService("input_method");
      this.touchSlop = ViewConfiguration.get(var1.getContext()).getScaledTouchSlop();
      var1.setFocusable(true);
      var1.setFocusableInTouchMode(true);
   }

   public void register(TextField var1) {
      Objects.requireNonNull(var1, "TextField cannot be null.");
      if (this.fieldsById.containsKey(var1.getId())) {
         throw new IllegalArgumentException("TextInputCoordinator already contains ID: " + var1.getId());
      } else {
         this.drawingOrder.add(var1);
         this.fieldsById.put(var1.getId(), var1);
         if (this.focusTransferProfile.consume(var1.getId(), SystemClock.uptimeMillis())) {
            this.hostView.removeCallbacks(this.pendingFocusDismissRunnable);
            this.focusedField = var1;
            var1.setFocusedInternal(true, false);
            this.inputConnection = null;
            this.hostView.requestFocus();
            this.restartInput();
            Log.d(PROFILE_TAG, "event=focus_transfer_completed field=" + var1.getId()
                  + " elapsedMs=" + this.focusTransferProfile.getLastElapsedMs());
         }
         this.invalidateComponent();
      }
   }

   public TextField find(String var1) {
      return (TextField)this.fieldsById.get(var1);
   }

   public boolean contains(String var1) {
      return this.fieldsById.containsKey(var1);
   }

   public boolean unregister(String var1) {
      TextField var2 = (TextField)this.fieldsById.remove(var1);
      if (var2 == null) {
         return false;
      } else {
         if (var2 == this.focusedField) {
            this.deferFocusedFieldRemoval(var2);
         }

         this.drawingOrder.remove(var2);
         var2.detach();
         this.invalidateComponent();
         return true;
      }
   }

   public int size() {
      return this.drawingOrder.size();
   }

   public boolean isEmpty() {
      return this.drawingOrder.isEmpty();
   }

   public TextField getFocusedField() {
      return this.focusedField;
   }

   public boolean hasFocusedField() {
      return this.focusedField != null;
   }

   public TextInputCoordinator setHideKeyboardWhenTouchOutside(boolean var1) {
      this.hideKeyboardWhenTouchOutside = var1;
      return this;
   }

   public void draw(Canvas var1) {
      Objects.requireNonNull(var1, "Canvas cannot be null.");
      Iterator var2 = this.drawingOrder.iterator();

      while(var2.hasNext()) {
         TextField var3 = (TextField)var2.next();
         var3.draw(var1);
      }

      if (this.focusedField != null) {
         this.hostView.postInvalidateDelayed(500L);
      }

   }

   public boolean onTouchEvent(MotionEvent var1) {
      Objects.requireNonNull(var1, "MotionEvent cannot be null.");
      switch (var1.getActionMasked()) {
         case 0:
            this.touchTarget = this.findTopmostField(var1.getX(), var1.getY());
            if (this.touchTarget == null) {
               if (this.hideKeyboardWhenTouchOutside) {
                  this.clearFocus();
               }

               return false;
            }

            this.requestFocus(this.touchTarget);
            this.touchTarget.placeCursorFromTouch(var1.getX(), var1.getY());
            this.touchDownX = var1.getX();
            this.touchDownY = var1.getY();
            this.longPressTriggered = false;
            this.hostView.postDelayed(this.longPressRunnable, (long)ViewConfiguration.getLongPressTimeout());
            return true;
         case 1:
            this.hostView.removeCallbacks(this.longPressRunnable);
            boolean var2 = this.touchTarget != null;
            this.touchTarget = null;
            return var2;
         case 2:
            if (this.touchTarget == null) {
               return false;
            }

            if (!this.longPressTriggered && (Math.abs(var1.getX() - this.touchDownX) > (float)this.touchSlop || Math.abs(var1.getY() - this.touchDownY) > (float)this.touchSlop)) {
               this.hostView.removeCallbacks(this.longPressRunnable);
            }

            if (!this.longPressTriggered) {
               this.touchTarget.placeCursorFromTouch(var1.getX(), var1.getY());
            }

            return true;
         case 3:
            this.hostView.removeCallbacks(this.longPressRunnable);
            boolean var3 = this.touchTarget != null;
            this.touchTarget = null;
            return var3;
         default:
            return false;
      }
   }

   public boolean requestFocus(TextField var1) {
      if (var1 != null && var1.isEnabled()) {
         if (this.focusedField != var1) {
            if (this.selectionActionMode != null) this.selectionActionMode.finish();
            if (this.focusedField != null) {
               this.focusedField.setFocusedInternal(false);
            }

            this.focusedField = var1;
            this.focusedField.setFocusedInternal(true);
            this.inputConnection = null;
         }

         this.hostView.requestFocus();
         this.restartInput();
         this.hostView.post(() -> {
            if (this.focusedField == var1 && this.inputMethodManager != null) {
               this.inputMethodManager.showSoftInput(this.hostView, 1);
            }

         });
         this.invalidateComponent();
         return true;
      } else {
         return false;
      }
   }

   public void clearFocus() {
      this.clearFocus(true, true);
   }

   private void hideKeyboardAndRestartInput() {
      if (this.inputMethodManager != null) {
         this.inputMethodManager.hideSoftInputFromWindow(this.hostView.getWindowToken(), 0);
         this.inputMethodManager.restartInput(this.hostView);
      }
   }

   private void clearFocus(boolean notifyListener, boolean expirePendingTransfer) {
      this.hostView.removeCallbacks(this.pendingFocusDismissRunnable);
      if (expirePendingTransfer && this.focusTransferProfile.hasPending()) {
         this.focusTransferProfile.expire(SystemClock.uptimeMillis());
         this.hideKeyboardAndRestartInput();
      }
      if (this.focusedField != null) {
         this.hostView.removeCallbacks(this.longPressRunnable);
         if (this.selectionActionMode != null) {
            this.selectionActionMode.finish();
         }

         this.focusedField.finishComposingText();
         this.focusedField.setFocusedInternal(false, notifyListener);
         this.focusedField = null;
         this.touchTarget = null;
         this.inputConnection = null;
         this.hideKeyboardAndRestartInput();

         this.invalidateComponent();
      }
   }

   public void clearFocus(TextField var1) {
      if (this.focusedField == var1) {
         this.clearFocus();
      }

   }

   public InputConnection onCreateInputConnection(EditorInfo var1) {
      Objects.requireNonNull(var1, "EditorInfo cannot be null.");
      if (this.focusedField == null) {
         return null;
      } else {
         this.focusedField.configureEditorInfo(var1);
         this.inputConnection = new FieldInputConnection(this.hostView);
         return this.inputConnection;
      }
   }

   public boolean onKeyDown(int var1, KeyEvent var2) {
      Objects.requireNonNull(var2, "KeyEvent cannot be null.");
      if (this.focusedField == null) {
         return false;
      } else {
         if (var2.isCtrlPressed()) {
            switch (var1) {
               case KeyEvent.KEYCODE_A:
                  return this.performContextMenuAction(android.R.id.selectAll);
               case KeyEvent.KEYCODE_C:
                  return this.performContextMenuAction(android.R.id.copy);
               case KeyEvent.KEYCODE_V:
                  return this.performContextMenuAction(android.R.id.paste);
               case KeyEvent.KEYCODE_X:
                  return this.performContextMenuAction(android.R.id.cut);
               default:
                  break;
            }
         }
         switch (var1) {
            case 21:
               this.moveCursor(-1, var2.isShiftPressed());
               return true;
            case 22:
               this.moveCursor(1, var2.isShiftPressed());
               return true;
            case 66:
               if (this.focusedField.isMultiline()) {
                  this.focusedField.replaceSelection("\n", 1, false);
                  return true;
               }

               return this.handleEditorAction(this.focusedField.getImeOptions() & 255);
            case 67:
               this.focusedField.deleteBeforeCursor();
               return true;
            case 112:
               this.focusedField.deleteAfterCursor();
               return true;
            default:
               int var3 = var2.getUnicodeChar();
               if (var3 != 0 && !Character.isISOControl(var3)) {
                  this.focusedField.replaceSelection(new String(Character.toChars(var3)), 1, false);
                  return true;
               } else {
                  return false;
               }
         }
      }
   }

   public void restartInput() {
      if (this.focusedField != null && this.inputMethodManager != null) {
         this.inputMethodManager.restartInput(this.hostView);
      }

   }

   public void clear() {
      this.clearFocus();
      Iterator var1 = this.drawingOrder.iterator();

      while(var1.hasNext()) {
         TextField var2 = (TextField)var1.next();
         var2.detach();
      }

      this.drawingOrder.clear();
      this.fieldsById.clear();
      this.invalidateComponent();
   }

   public void release() {
      this.clear();
   }

   public void close() {
      this.release();
   }

   public View getHostView() {
      return this.hostView;
   }

   public void invalidateComponent() {
      this.hostView.invalidate();
   }

   public void postInvalidateComponentOnAnimation() {
      this.hostView.postInvalidateOnAnimation();
   }

   public void updateSelection(TextField var1) {
      if (var1 == this.focusedField) {
         Editable var2 = var1.getEditable();
         boolean collapsed = var1.getSelectionStart() == var1.getSelectionEnd();
         if (this.selectionActionMode != null && collapsed != this.insertionActionMode) {
            this.selectionActionMode.finish();
         }

         if (this.inputMethodManager != null) {
            this.inputMethodManager.updateSelection(this.hostView, var1.getRawSelectionStart(), var1.getRawSelectionEnd(), BaseInputConnection.getComposingSpanStart(var2), BaseInputConnection.getComposingSpanEnd(var2));
         }
         if (this.selectionActionMode != null) {
            this.selectionActionMode.invalidate();
            if (VERSION.SDK_INT >= 23) this.selectionActionMode.invalidateContentRect();
         }

      }
   }

   private TextField findTopmostField(float var1, float var2) {
      for(int var3 = this.drawingOrder.size() - 1; var3 >= 0; --var3) {
         TextField var4 = (TextField)this.drawingOrder.get(var3);
         if (var4.contains(var1, var2)) {
            return var4;
         }
      }

      return null;
   }

   private void moveCursor(int var1, boolean extendSelection) {
      Editable var2 = this.focusedField.getEditable();
      int rawStart = this.focusedField.getRawSelectionStart();
      int rawEnd = this.focusedField.getRawSelectionEnd();
      if (!extendSelection && rawStart != rawEnd) {
         this.focusedField.setSelection(var1 < 0
                 ? this.focusedField.getSelectionStart()
                 : this.focusedField.getSelectionEnd());
         return;
      }
      int next = var1 < 0
              ? TextBoundaryHelper.previousGrapheme(var2, rawEnd)
              : TextBoundaryHelper.nextGrapheme(var2, rawEnd);
      if (extendSelection) this.focusedField.setSelection(rawStart, next);
      else this.focusedField.setSelection(next);
   }

   private boolean handleEditorAction(int var1) {
      if (this.focusedField == null) {
         return false;
      } else if (this.focusedField.performEditorAction(var1)) {
         return true;
      } else if (var1 == 5) {
         return this.focusNext();
      } else if (var1 != 6 && var1 != 2 && var1 != 4 && var1 != 3) {
         return false;
      } else {
         this.clearFocus();
         return true;
      }
   }

   private boolean focusNext() {
      if (this.focusedField != null && !this.drawingOrder.isEmpty()) {
         int var1 = this.drawingOrder.indexOf(this.focusedField);

         for(int var2 = 1; var2 <= this.drawingOrder.size(); ++var2) {
            TextField var3 = (TextField)this.drawingOrder.get((var1 + var2) % this.drawingOrder.size());
            if (var3.isEnabled()) {
               return this.requestFocus(var3);
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean performContextMenuAction(int var1) {
      if (this.focusedField == null) {
         return false;
      } else {
         Editable var2 = this.focusedField.getEditable();
         int var3 = this.focusedField.getSelectionStart();
         int var4 = this.focusedField.getSelectionEnd();
         ClipboardManager var5 = (ClipboardManager)this.hostView.getContext().getSystemService("clipboard");
         boolean hasSelection = var3 != var4;
         boolean password = this.focusedField.isPassword();
         if (var1 == android.R.id.selectAll) {
            if (!this.focusedField.isSelectionEnabled() || var2.length() == 0) return false;
            this.focusedField.setSelection(0, var2.length());
            return true;
         } else if (var1 == android.R.id.copy) {
            if (password || !this.focusedField.isCopyEnabled() || !hasSelection || var5 == null) return false;
            var5.setPrimaryClip(ClipData.newPlainText(this.focusedField.getId(), var2.subSequence(var3, var4)));
            return true;
         } else if (var1 == android.R.id.cut) {
            if (password || !this.focusedField.isCutEnabled() || !hasSelection || var5 == null) return false;
            var5.setPrimaryClip(ClipData.newPlainText(this.focusedField.getId(), var2.subSequence(var3, var4)));
            this.focusedField.replaceSelection("", 1, false);
            return true;
         } else if (var1 == android.R.id.paste) {
            if (!this.focusedField.isPasteEnabled() || var5 == null || !var5.hasPrimaryClip()) return false;
            if (var5.hasPrimaryClip()) {
               ClipData var6 = var5.getPrimaryClip();
               if (var6 != null && var6.getItemCount() > 0) {
                  CharSequence var7 = var6.getItemAt(0).coerceToText(this.hostView.getContext());
                  this.focusedField.replaceSelection(var7, 1, false);
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private void startSelectionActionMode() {
      this.startTextActionMode(false);
   }

   private void startTextActionMode(boolean insertion) {
      if (this.focusedField == null) return;
      if (this.selectionActionMode != null && this.actionModeField == this.focusedField
              && this.insertionActionMode == insertion) {
         this.selectionActionMode.invalidate();
         if (VERSION.SDK_INT >= 23) this.selectionActionMode.invalidateContentRect();
         return;
      }
      if (this.selectionActionMode != null) {
         this.selectionActionMode.finish();
      }
      this.actionModeField = this.focusedField;
      this.insertionActionMode = insertion;

      ActionMode.Callback2 var1 = new ActionMode.Callback2() {
         public boolean onCreateActionMode(ActionMode var1, Menu var2) {
            Context context = TextInputCoordinator.this.hostView.getContext();
            var2.add(0, android.R.id.cut, 0, context.getString(android.R.string.cut)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            var2.add(0, android.R.id.copy, 1, context.getString(android.R.string.copy)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            var2.add(0, android.R.id.paste, 2, context.getString(android.R.string.paste)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            var2.add(0, android.R.id.selectAll, 3, context.getString(android.R.string.selectAll)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            TextInputCoordinator.this.prepareSelectionMenu(var1, var2);
            TextField field = TextInputCoordinator.this.actionModeField;
            TextSelectionActionCallback callback = field.getSelectionActionCallback();
            if (callback != null) callback.onCreateActionMode(field, var1, var2);
            return var2.hasVisibleItems();
         }

         public boolean onPrepareActionMode(ActionMode var1, Menu var2) {
            TextInputCoordinator.this.prepareSelectionMenu(var1, var2);
            TextField field = TextInputCoordinator.this.actionModeField;
            TextSelectionActionCallback callback = field.getSelectionActionCallback();
            if (callback != null) callback.onPrepareActionMode(field, var1, var2);
            return true;
         }

         public boolean onActionItemClicked(ActionMode var1, MenuItem var2) {
            TextField field = TextInputCoordinator.this.actionModeField;
            if (field == null || field != TextInputCoordinator.this.focusedField) return false;
            TextSelectionActionCallback callback = field.getSelectionActionCallback();
            if (callback != null && callback.onActionItemClicked(field, var1, var2)) return true;
            boolean var3 = TextInputCoordinator.this.performContextMenuAction(var2.getItemId());
            if (var3) {
               if (var2.getItemId() == android.R.id.selectAll) {
                  TextInputCoordinator.this.hostView.post(() ->
                          TextInputCoordinator.this.showSelectionActionMode(field));
               } else {
                  var1.finish();
               }
            }

            return var3;
         }

         public void onDestroyActionMode(ActionMode var1) {
            if (TextInputCoordinator.this.selectionActionMode == var1) {
               TextInputCoordinator.this.selectionActionMode = null;
               TextInputCoordinator.this.actionModeField = null;
               TextInputCoordinator.this.insertionActionMode = false;
            }

            TextInputCoordinator.this.invalidateComponent();
         }

         public void onGetContentRect(ActionMode var1, View var2, Rect var3) {
            RectF var4 = TextInputCoordinator.this.focusedField == null ? new RectF(0.0F, 0.0F, (float)var2.getWidth(), (float)var2.getHeight()) : TextInputCoordinator.this.focusedField.getSelectionActionBounds();
            var4.offset(TextInputCoordinator.this.selectionAnchorOffsetX, TextInputCoordinator.this.selectionAnchorOffsetY);
            if (TextInputCoordinator.this.focusedField != null) {
               RectF var5 = TextInputCoordinator.this.focusedField.getBounds();
               var5.offset(TextInputCoordinator.this.selectionAnchorOffsetX, TextInputCoordinator.this.selectionAnchorOffsetY);
               float var6 = Math.max(1.0F, Math.min(var4.height(), var5.height()));
               float var7 = Math.max(1.0F, Math.min(var4.width(), var5.width()));
               var4.left = Math.max(var5.left, Math.min(var4.left, var5.right - var7));
               var4.top = Math.max(var5.top, Math.min(var4.top, var5.bottom - var6));
               var4.right = var4.left + var7;
               var4.bottom = var4.top + var6;
            }

            var4.left = Math.max(0.0F, Math.min(var4.left, (float)var2.getWidth() - 1.0F));
            var4.right = Math.max(var4.left + 1.0F, Math.min(var4.right, (float)var2.getWidth()));
            var4.top = Math.max(0.0F, Math.min(var4.top, (float)var2.getHeight() - 1.0F));
            var4.bottom = Math.max(var4.top + 1.0F, Math.min(var4.bottom, (float)var2.getHeight()));
            var3.set(Math.round(var4.left), Math.round(var4.top), Math.round(var4.right), Math.round(var4.bottom));
         }
      };
      this.selectionActionMode = VERSION.SDK_INT >= 23 ? this.hostView.startActionMode(var1, 1) : this.hostView.startActionMode(var1);
      if (this.selectionActionMode == null) {
         Activity var2 = findActivity(this.hostView.getContext());
         if (var2 != null) {
            this.selectionActionMode = VERSION.SDK_INT >= 23 ? var2.startActionMode(var1, 1) : var2.startActionMode(var1);
         }
      }

      if (this.selectionActionMode != null && VERSION.SDK_INT >= 23) {
         this.selectionActionMode.invalidateContentRect();
      }
      if (this.selectionActionMode == null) {
         this.actionModeField = null;
         this.insertionActionMode = false;
      }

   }

   private void prepareSelectionMenu(ActionMode mode, Menu menu) {
      TextField field = this.actionModeField;
      if (field == null) return;
      boolean hasSelection = field.getSelectionStart() != field.getSelectionEnd();
      boolean password = field.isPassword();
      Editable text = field.getEditable();
      ClipboardManager clipboard = (ClipboardManager)this.hostView.getContext()
              .getSystemService(Context.CLIPBOARD_SERVICE);
      MenuItem cut = menu.findItem(android.R.id.cut);
      MenuItem copy = menu.findItem(android.R.id.copy);
      MenuItem paste = menu.findItem(android.R.id.paste);
      MenuItem selectAll = menu.findItem(android.R.id.selectAll);
      if (cut != null) cut.setVisible(hasSelection && !password && field.isSelectionEnabled() && field.isCutEnabled());
      if (copy != null) copy.setVisible(hasSelection && !password && field.isSelectionEnabled() && field.isCopyEnabled());
      if (paste != null) paste.setVisible(field.isPasteEnabled()
              && clipboard != null && clipboard.hasPrimaryClip());
      if (selectAll != null) selectAll.setVisible(field.isSelectionEnabled()
              && text.length() > 0 && !(field.getSelectionStart() == 0
              && field.getSelectionEnd() == text.length()));
   }

   private static Activity findActivity(Context var0) {
      Context var1;
      Context var2;
      for(var1 = var0; var1 instanceof ContextWrapper; var1 = var2) {
         if (var1 instanceof Activity) {
            return (Activity)var1;
         }

         var2 = ((ContextWrapper)var1).getBaseContext();
         if (var2 == var1) {
            break;
         }
      }

      return var1 instanceof Activity ? (Activity)var1 : null;
   }

   public void showSelectionActionMode(TextField var1) {
      this.showSelectionActionMode(var1, 0.0F, 0.0F);
   }

   public void showSelectionActionMode(TextField var1, float var2, float var3) {
      if (var1 != null && var1 == this.focusedField
              && var1.isSelectionEnabled() && !var1.isPassword()
              && var1.getSelectionStart() != var1.getSelectionEnd()) {
         this.selectionAnchorOffsetX = var2;
         this.selectionAnchorOffsetY = var3;
         this.startSelectionActionMode();
      }
   }

   public void showInsertionActionMode(TextField field) {
      if (field != null && field == this.focusedField && field.isEnabled()
              && field.getSelectionStart() == field.getSelectionEnd()) {
         this.selectionAnchorOffsetX = 0.0F;
         this.selectionAnchorOffsetY = 0.0F;
         this.startTextActionMode(true);
      }
   }

   public void dismissSelectionActionMode(boolean var1) {
      TextField var2 = this.focusedField;
      if (this.selectionActionMode != null) {
         this.selectionActionMode.finish();
      }

      if (var1 && var2 != null && var2.getSelectionStart() != var2.getSelectionEnd()) {
         var2.setSelection(var2.getSelectionEnd());
      }

   }

   private final class FieldInputConnection extends BaseInputConnection {
      private boolean deleteHandledOnDown;

      private FieldInputConnection(View var2) {
         super(var2, true);
      }

      public Editable getEditable() {
         return TextInputCoordinator.this.focusedField == null ? null : TextInputCoordinator.this.focusedField.getEditable();
      }

      public boolean commitText(CharSequence var1, int var2) {
         TextField var3 = TextInputCoordinator.this.focusedField;
         if (var3 == null) {
            return false;
         } else {
            String var4 = var3.getText();
            boolean var5 = super.commitText(var1, var2);
            var3.afterImeEdit(var4);
            return var5;
         }
      }

      public boolean setComposingText(CharSequence var1, int var2) {
         TextField var3 = TextInputCoordinator.this.focusedField;
         if (var3 == null) {
            return false;
         } else {
            String var4 = var3.getText();
            boolean var5 = super.setComposingText(var1, var2);
            var3.afterImeEdit(var4);
            return var5;
         }
      }

      public boolean finishComposingText() {
         TextField var1 = TextInputCoordinator.this.focusedField;
         if (var1 == null) {
            return false;
         } else {
            boolean var2 = super.finishComposingText();
            var1.afterImeStateChange();
            return var2;
         }
      }

      public boolean setComposingRegion(int var1, int var2) {
         TextField var3 = TextInputCoordinator.this.focusedField;
         if (var3 == null) {
            return false;
         } else {
            boolean var4 = super.setComposingRegion(var1, var2);
            var3.afterImeStateChange();
            return var4;
         }
      }

      public boolean deleteSurroundingText(int var1, int var2) {
         TextField var3 = TextInputCoordinator.this.focusedField;
         if (var3 == null) {
            return false;
         } else {
            String var4 = var3.getText();
            boolean var5 = super.deleteSurroundingText(var1, var2);
            var3.afterImeEdit(var4);
            return var5;
         }
      }

      public boolean deleteSurroundingTextInCodePoints(int var1, int var2) {
         TextField var3 = TextInputCoordinator.this.focusedField;
         if (var3 == null) {
            return false;
         } else {
            String var4 = var3.getText();
            boolean var5 = super.deleteSurroundingTextInCodePoints(var1, var2);
            var3.afterImeEdit(var4);
            return var5;
         }
      }

      public boolean setSelection(int var1, int var2) {
         TextField var3 = TextInputCoordinator.this.focusedField;
         if (var3 == null) {
            return false;
         } else {
            boolean var4 = super.setSelection(var1, var2);
            var3.afterImeStateChange();
            return var4;
         }
      }

      public boolean performEditorAction(int var1) {
         return TextInputCoordinator.this.handleEditorAction(var1);
      }

      public boolean performContextMenuAction(int var1) {
         return TextInputCoordinator.this.performContextMenuAction(var1);
      }

      public boolean sendKeyEvent(KeyEvent var1) {
         if (var1.getKeyCode() == KeyEvent.KEYCODE_DEL
                 || var1.getKeyCode() == KeyEvent.KEYCODE_FORWARD_DEL) {
            if (var1.getAction() == KeyEvent.ACTION_DOWN) {
               this.deleteHandledOnDown = TextInputCoordinator.this.onKeyDown(
                       var1.getKeyCode(), var1);
               if (this.deleteHandledOnDown) {
                  return true;
               }
            } else if (var1.getAction() == KeyEvent.ACTION_UP) {
               if (this.deleteHandledOnDown) {
                  this.deleteHandledOnDown = false;
                  return true;
               }

               // Some OEM IMEs send only ACTION_UP for delete.
               if (TextInputCoordinator.this.onKeyDown(var1.getKeyCode(), var1)) {
                  return true;
               }
            }
         } else if (var1.getAction() == KeyEvent.ACTION_DOWN
                 && TextInputCoordinator.this.onKeyDown(var1.getKeyCode(), var1)) {
            return true;
         }

         return super.sendKeyEvent(var1);
      }
   }
}

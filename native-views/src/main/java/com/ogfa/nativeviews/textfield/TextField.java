package com.ogfa.nativeviews.textfield;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.os.SystemClock;
import android.os.Build.VERSION;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import androidx.core.content.res.ResourcesCompat;
import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.selection.TextSelectionController;
import com.ogfa.nativeviews.text.selection.TextSelectionGeometry;
import com.ogfa.nativeviews.text.selection.TextSelectionHandles;
import com.ogfa.nativeviews.text.selection.TextSelectionMagnifier;
import java.util.Objects;

public final class TextField implements Component, TextSelectionGeometry {
   public static final long DEFAULT_CURSOR_BLINK_INTERVAL_MS = 500L;
   private static final long MAGNIFIER_TOOLBAR_DELAY_MS =
           TextSelectionMagnifier.TOOLBAR_DELAY_MS;
   private final Context context;
   private final View hostView;
   private final String id;
   private final RectF baseBounds = new RectF();
   private final RectF bounds = new RectF();
   private final SpannableStringBuilder editable = new SpannableStringBuilder();
   private final TextPaint textPaint = new TextPaint(129);
   private final TextPaint hintPaint = new TextPaint(129);
   private final Paint backgroundPaint = new Paint(1);
   private final Paint strokePaint = new Paint(1);
   private final Paint cursorPaint = new Paint(1);
   private final Paint selectionPaint = new Paint(1);
   private final Matrix drawMatrix = new Matrix();
   private final Paint scrollBarPaint = new Paint(1);
   private final TextSelectionHandles selectionHandles;
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
   private final int selectionHandleColor;
   private int backgroundColor;
   private int focusedBackgroundColor;
   private int strokeColor;
   private int focusedStrokeColor;
   private float textSizePx;
   private float horizontalPaddingPx;
   private float verticalPaddingPx;
   private float cornerRadiusPx;
   private float strokeWidthPx;
   private float cursorWidth;
   private float cursorWidthPx;
   private boolean cursorWidthInPixels;
   private float cursorHeight;
   private float cursorHeightPx;
   private boolean cursorHeightInPixels;
   private long cursorBlinkIntervalMs;
   private float horizontalScrollPx;
   private float verticalScrollPx;
   private float multilineBottomEndInsetPx;
   private StaticLayout multilineTextLayout;
   private StaticLayout multilineHintLayout;
   private String multilineTextValue;
   private String multilineHintValue;
   private int multilineLayoutWidth = -1;
   private boolean multilineLayoutDirty = true;
   private FontVariation fontVariation;
   private boolean enabled;
   private boolean visible = true;
   private boolean horizontalCentered;
   private boolean verticalCentered;
   private boolean touchCaptured;
   private boolean touchDragging;
   private boolean touchLongPressTriggered;
   private boolean touchInsideSelection;
   private boolean touchCursorDragCandidate;
   private boolean touchLongPressInsertion;
   private boolean touchCursorDragging;
   private boolean selectionHandleDragging;
   private boolean magnifierVisible;
   private boolean magnifierPending;
   private boolean doubleTapSelecting;
   private boolean touchStayedInTapRegion;
   private boolean touchBeganFocused;
   private int touchedSelectionHandle;
   private float cursorDragOffsetX;
   private float cursorDragOffsetY;
   private int handleDragLine;
   private int selectionAnchorStart;
   private int selectionAnchorEnd;
   private int selectionAutoScrollDirection;
   private float selectionDragX;
   private float selectionDragY;
   private boolean selectionAutoScrollPosted;
   private final Runnable selectionAutoScrollRunnable = this::runSelectionAutoScroll;
   private final Runnable showMagnifierRunnable = () -> {
      this.magnifierPending = false;
      if (this.touchCaptured && this.focused && this.visible) {
         this.magnifierVisible = true;
         this.invalidate();
      }
   };
   private final Runnable showSelectionToolbarRunnable = () -> {
      if (!this.touchCaptured && !this.magnifierVisible
              && this.owner != null && this.focused) {
         this.showTextActionMode();
      }
   };
   private float touchDownX;
   private float touchDownY;
   private long touchDownTime;
   private float touchLastY;
   private long lastTapUpTime;
   private float lastTapX;
   private float lastTapY;
   private final int touchSlop;
   private final int doubleTapSlop;
   private final Runnable touchLongPressRunnable = this::handleTouchLongPress;
   private boolean password;
   private boolean selectionEnabled;
   private boolean cutEnabled;
   private boolean copyEnabled;
   private boolean pasteEnabled;
   private boolean focused;
   private long cursorBlinkStartedAt;
   private OnTextChangedListener textChangedListener;
   private OnEditorActionListener editorActionListener;
   private OnFocusChangedListener focusChangedListener;
   private TextSelectionActionCallback selectionActionCallback;

   private TextField(Builder var1, View var2) {
      this.context = var1.context.getApplicationContext();
      this.hostView = (View)Objects.requireNonNull(var2, "Host view cannot be null.");
      this.touchSlop = ViewConfiguration.get(var2.getContext()).getScaledTouchSlop();
      this.doubleTapSlop = ViewConfiguration.get(var2.getContext()).getScaledDoubleTapSlop();
      this.id = requireId(var1.id);
      this.baseBounds.set(var1.resolveBounds(var2));
      requireBounds(this.baseBounds);
      this.horizontalCentered = var1.horizontalCentered;
      this.verticalCentered = var1.verticalCentered;
      this.applyParentAlignment();
      this.hint = var1.hint;
      this.inputType = var1.inputType;
      this.imeOptions = var1.imeOptions;
      this.maxLength = var1.maxLength;
      this.textColor = var1.textColor;
      this.hintColor = var1.hintColor;
      this.cursorColor = var1.cursorColor;
      this.selectionHandleColor = var1.selectionHandleColor;
      this.selectionHandles = new TextSelectionHandles(
              var2.getContext(), this.selectionHandleColor);
      this.selectionColor = var1.selectionColor;
      this.backgroundColor = var1.backgroundColor;
      this.focusedBackgroundColor = var1.focusedBackgroundColor;
      this.strokeColor = var1.strokeColor;
      this.focusedStrokeColor = var1.focusedStrokeColor;
      this.textSizePx = var1.textSize > 0.0F ? var1.resolveDimension(var2, var1.textSize, var1.textSizeInPixels) : this.bounds.height() * 0.42F;
      this.horizontalPaddingPx = var1.horizontalPadding >= 0.0F ? var1.resolveDimension(var2, var1.horizontalPadding, var1.paddingInPixels) : this.bounds.height() * 0.18F;
      this.verticalPaddingPx = var1.verticalPadding >= 0.0F ? var1.resolveDimension(var2, var1.verticalPadding, var1.paddingInPixels) : this.bounds.height() * 0.1F;
      this.cornerRadiusPx = var1.cornerRadius >= 0.0F ? var1.resolveDimension(var2, var1.cornerRadius, var1.cornerRadiusInPixels) : this.bounds.height() * 0.18F;
      this.strokeWidthPx = var1.resolveDimension(var2, var1.strokeWidth, var1.strokeWidthInPixels);
      this.cursorWidth = var1.cursorWidth;
      this.cursorWidthInPixels = var1.cursorWidthInPixels;
      this.cursorWidthPx = var1.resolveCursorWidth(var2);
      this.cursorHeight = var1.cursorHeight;
      this.cursorHeightInPixels = var1.cursorHeightInPixels;
      this.cursorHeightPx = var1.cursorHeight > 0.0F
              ? var1.resolveCursorHeight(var2) : -1.0F;
      this.cursorBlinkIntervalMs = var1.cursorBlinkIntervalMs;
      this.enabled = var1.enabled;
      this.password = var1.password;
      this.selectionEnabled = var1.selectionEnabled;
      this.cutEnabled = var1.cutEnabled;
      this.copyEnabled = var1.copyEnabled;
      this.pasteEnabled = var1.pasteEnabled;
      this.selectionActionCallback = var1.selectionActionCallback;
      this.textChangedListener = var1.textChangedListener;
      this.editorActionListener = var1.editorActionListener;
      this.focusChangedListener = var1.focusChangedListener;
      this.fontVariation = var1.fontVariation;
      Typeface var3 = var1.typeface != null ? var1.typeface : resolveTypeface(var1.context, var1.fontId);
      this.textPaint.setTypeface(var3);
      this.hintPaint.setTypeface(var3);
      this.applyFontVariation();
      this.textPaint.setTextSize(this.textSizePx);
      this.hintPaint.setTextSize(this.textSizePx);
      this.textPaint.setColor(this.textColor);
      this.hintPaint.setColor(this.hintColor);
      this.backgroundPaint.setStyle(Style.FILL);
      this.strokePaint.setStyle(Style.STROKE);
      this.strokePaint.setStrokeWidth(this.strokeWidthPx);
      this.cursorPaint.setColor(this.cursorColor);
      this.cursorPaint.setStrokeWidth(this.cursorWidthPx);
      this.selectionPaint.setColor(this.selectionColor);
      if (!(this.horizontalPaddingPx * 2.0F >= this.bounds.width()) && !(this.verticalPaddingPx * 2.0F >= this.bounds.height())) {
         this.applyLengthFilter();
         this.setTextInternal(var1.initialText, false);
      } else {
         throw new IllegalArgumentException("TextField padding leaves no drawable content area.");
      }
   }

   public String getId() {
      return this.id;
   }

   public RectF getBounds() {
      return new RectF(this.bounds);
   }

   public TextField setRegion(RectF var1) {
      Objects.requireNonNull(var1, "TextField region cannot be null.");
      requireBounds(var1);
      this.baseBounds.set(var1);
      this.applyParentAlignment();
      if (!(this.horizontalPaddingPx * 2.0F >= this.bounds.width()) && !(this.verticalPaddingPx * 2.0F >= this.bounds.height())) {
         this.invalidateMultilineLayout();
         this.ensureCursorVisible();
         this.invalidate();
         return this;
      } else {
         throw new IllegalArgumentException("TextField padding leaves no drawable content area.");
      }
   }

   public TextField setMultilineBottomEndInsetPx(float var1) {
      float var2 = Math.max(0.0F, var1);
      if (Math.abs(this.multilineBottomEndInsetPx - var2) < 0.5F) {
         return this;
      } else {
         this.multilineBottomEndInsetPx = var2;
         this.invalidateMultilineLayout();
         this.ensureCursorVisible();
         this.invalidate();
         return this;
      }
   }

   public String getText() {
      return this.editable.toString();
   }

   public Editable getEditable() {
      return this.editable;
   }

   public float getCursorWidth() {
      return this.cursorWidth;
   }

   public float getResolvedCursorWidth() {
      return this.cursorWidthPx;
   }

   public boolean isCursorWidthInPixels() {
      return this.cursorWidthInPixels;
   }

   public float getCursorHeight() {
      return this.cursorHeight;
   }

   public float getResolvedCursorHeight() {
      return this.cursorHeightPx;
   }

   public boolean isCursorHeightInPixels() {
      return this.cursorHeightInPixels;
   }

   public long getCursorBlinkIntervalMs() {
      return this.cursorBlinkIntervalMs;
   }

   public boolean isHorizontalCentered() {
      return this.horizontalCentered;
   }

   public boolean isVerticalCentered() {
      return this.verticalCentered;
   }

   public TextField setHorizontalCenter(boolean var1) {
      if (this.horizontalCentered == var1) {
         return this;
      } else {
         this.horizontalCentered = var1;
         this.applyParentAlignment();
         this.ensureCursorVisible();
         this.invalidate();
         return this;
      }
   }

   public TextField horizontalCenter(boolean var1) {
      return this.setHorizontalCenter(var1);
   }

   public TextField setVerticalCenter(boolean var1) {
      if (this.verticalCentered == var1) {
         return this;
      } else {
         this.verticalCentered = var1;
         this.applyParentAlignment();
         this.ensureCursorVisible();
         this.invalidate();
         return this;
      }
   }

   public TextField verticalCenter(boolean var1) {
      return this.setVerticalCenter(var1);
   }

   public TextField setText(CharSequence var1) {
      this.setTextInternal(var1, true);
      return this;
   }

   public TextField clear() {
      return this.setText("");
   }

   public TextField setHint(String var1) {
      this.hint = var1 == null ? "" : var1;
      this.invalidate();
      return this;
   }

   public String getHint() {
      return this.hint;
   }

   public Typeface getTypeface() {
      return this.textPaint.getTypeface();
   }

   public TextField setFont(int var1) {
      if (var1 == 0) {
         throw new IllegalArgumentException("Font resource ID cannot be zero.");
      } else {
         return this.setFont(resolveTypeface(this.context, var1));
      }
   }

   public TextField setFont(Typeface var1) {
      Typeface var2 = (Typeface)Objects.requireNonNull(var1, "Typeface cannot be null.");
      this.textPaint.setTypeface(var2);
      this.hintPaint.setTypeface(var2);
      this.applyFontVariation();
      this.ensureCursorVisible();
      this.invalidate();
      return this;
   }

   public TextField useDefaultFont() {
      return this.setFont(Typeface.DEFAULT);
   }

   public FontVariation getFontVariation() {
      return this.fontVariation;
   }

   public TextField setFontVariations(FontVariation var1) {
      this.fontVariation = (FontVariation)Objects.requireNonNull(var1, "Font variation cannot be null.");
      this.applyFontVariation();
      this.ensureCursorVisible();
      this.invalidate();
      return this;
   }

   public TextField clearFontVariations() {
      this.fontVariation = null;
      this.applyFontVariation();
      this.ensureCursorVisible();
      this.invalidate();
      return this;
   }

   public TextField setMaxLength(int var1) {
      if (var1 <= 0) {
         throw new IllegalArgumentException("Maximum length must be greater than zero.");
      } else {
         this.maxLength = var1;
         this.applyLengthFilter();
         if (this.editable.length() > var1) {
            this.replaceRange(var1, this.editable.length(), "", false);
         }

         return this;
      }
   }

   public int getMaxLength() {
      return this.maxLength;
   }

   public TextField setInputType(int var1) {
      this.inputType = var1;
      this.restartInput();
      return this;
   }

   public int getInputType() {
      return this.inputType;
   }

   public TextField setImeOptions(int var1) {
      this.imeOptions = var1;
      this.restartInput();
      return this;
   }

   public int getImeOptions() {
      return this.imeOptions;
   }

   public TextField setPassword(boolean var1) {
      this.password = var1;
      if (var1 && this.getSelectionStart() != this.getSelectionEnd()) {
         this.setSelection(this.getRawSelectionEnd());
      }
      this.ensureCursorVisible();
      this.invalidate();
      return this;
   }

   public boolean isPassword() {
      return this.password;
   }

   public boolean isSelectionEnabled() { return this.selectionEnabled; }
   public boolean isCutEnabled() { return this.cutEnabled; }
   public boolean isCopyEnabled() { return this.copyEnabled; }
   public boolean isPasteEnabled() { return this.pasteEnabled; }
   public TextSelectionActionCallback getSelectionActionCallback() {
      return this.selectionActionCallback;
   }

   public TextField setSelectionEnabled(boolean value) {
      this.selectionEnabled = value;
      if (!value && this.getSelectionStart() != this.getSelectionEnd()) {
         this.setSelection(this.getRawSelectionEnd());
      }
      this.refreshSelectionUi();
      return this;
   }

   public TextField setCutEnabled(boolean value) { this.cutEnabled = value; this.refreshSelectionUi(); return this; }
   public TextField setCopyEnabled(boolean value) { this.copyEnabled = value; this.refreshSelectionUi(); return this; }
   public TextField setPasteEnabled(boolean value) { this.pasteEnabled = value; this.refreshSelectionUi(); return this; }
   public TextField setSelectionActionCallback(TextSelectionActionCallback value) {
      this.selectionActionCallback = value;
      this.refreshSelectionUi();
      return this;
   }

   private void refreshSelectionUi() {
      if (this.focused && this.owner != null) this.owner.updateSelection(this);
      this.invalidate();
   }

   public TextField setEnabled(boolean var1) {
      this.enabled = var1;
      if (!var1 && this.focused && this.owner != null) {
         this.owner.clearFocus(this);
      }

      this.invalidate();
      return this;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public TextField requestFocus() {
      if (this.owner == null) {
         throw new IllegalStateException("Add the field to a ZLayer before requesting focus.");
      } else {
         this.owner.requestFocus(this);
         return this;
      }
   }

   public TextField clearFocus() {
      if (this.owner != null && this.focused) {
         this.owner.clearFocus(this);
      }

      return this;
   }

   public TextField setSelection(int var1) {
      return this.setSelection(var1, var1);
   }

   public TextField setSelection(int var1, int var2) {
      this.requireSelection(var1, var2);
      Selection.setSelection(this.editable, var1, var2);
      this.resetCursorBlink();
      this.ensureCursorVisible();
      this.updateImeSelection();
      this.invalidate();
      return this;
   }

   public int getSelectionStart() {
      return this.normalizedSelectionStart();
   }

   public int getSelectionEnd() {
      return this.normalizedSelectionEnd();
   }

   /** Returns the fixed/raw selection endpoint, which may be after the extent. */
   public int getSelectionAnchor() { return this.getRawSelectionStart(); }

   /** Returns the active/raw selection endpoint used by Shift+arrow movement. */
   public int getSelectionExtent() { return this.getRawSelectionEnd(); }

   int getRawSelectionStart() {
      this.ensureSelection();
      return clamp(Selection.getSelectionStart(this.editable), 0, this.editable.length());
   }

   int getRawSelectionEnd() {
      this.ensureSelection();
      return clamp(Selection.getSelectionEnd(this.editable), 0, this.editable.length());
   }

   public TextField setOnTextChangedListener(OnTextChangedListener var1) {
      this.textChangedListener = var1;
      return this;
   }

   public TextField setOnEditorActionListener(OnEditorActionListener var1) {
      this.editorActionListener = var1;
      return this;
   }

   public TextField setOnFocusChangedListener(OnFocusChangedListener var1) {
      this.focusChangedListener = var1;
      return this;
   }

   public void attach(ComponentHost var1) {
      if (!(var1 instanceof TextFieldHost)) {
         throw new IllegalArgumentException("TextField requires a TextFieldHost.");
      } else {
         TextFieldHost var2 = (TextFieldHost)var1;
         if (this.owner != null && this.owner != var1) {
            throw new IllegalStateException("TextField already belongs to another group.");
         } else {
            this.owner = var2;
            this.applyParentAlignment();
            this.ensureCursorVisible();
         }
      }
   }

   void detach() {
      this.hideMagnifier();
      this.hostView.removeCallbacks(this.touchLongPressRunnable);
      this.stopSelectionAutoScroll();
      this.lastTapUpTime = 0L;
      this.doubleTapSelecting = false;
      this.collapseSelectionOnBlur();
      this.focused = false;
      this.owner = null;
   }

   private void applyParentAlignment() {
      this.bounds.set(this.baseBounds);
      if ((this.horizontalCentered || this.verticalCentered) && !this.baseBounds.isEmpty()) {
         RectF var1 = this.owner == null ? new RectF(0.0F, 0.0F, (float)this.hostView.getWidth(), (float)this.hostView.getHeight()) : this.owner.getComponentBounds();
         float var2;
         if (this.horizontalCentered) {
            var2 = this.baseBounds.width();
            this.bounds.left = var1.centerX() - var2 / 2.0F;
            this.bounds.right = this.bounds.left + var2;
         }

         if (this.verticalCentered) {
            var2 = this.baseBounds.height();
            this.bounds.top = var1.centerY() - var2 / 2.0F;
            this.bounds.bottom = this.bounds.top + var2;
         }

      }
   }

   void setFocusedInternal(boolean var1) {
      this.setFocusedInternal(var1, true);
   }

   void setFocusedInternal(boolean var1, boolean notifyListener) {
      if (this.focused != var1) {
         this.focused = var1;
         if (var1) {
            this.resetCursorBlink();
            this.ensureSelection();
            this.ensureCursorVisible();
         } else {
            this.hideMagnifier();
             this.lastTapUpTime = 0L;
             this.doubleTapSelecting = false;
            this.collapseSelectionOnBlur();
         }

         if (notifyListener && this.focusChangedListener != null) {
            this.focusChangedListener.onFocusChanged(this.id, var1);
         }

         this.invalidate();
      }
   }

   private void collapseSelectionOnBlur() {
      int activeEnd = Selection.getSelectionEnd(this.editable);
      int cursor = activeEnd < 0 ? this.editable.length()
              : clamp(activeEnd, 0, this.editable.length());
      if (Selection.getSelectionStart(this.editable) != cursor || activeEnd != cursor) {
         Selection.setSelection(this.editable, cursor);
      }
   }

   boolean contains(float var1, float var2) {
      return this.visible && this.enabled && this.bounds.contains(var1, var2);
   }

   /** Whether a touch hits a visible cursor or selection handle in the owning layer's coordinates. */
   public boolean containsSelectionHandle(float x, float y) {
      return this.visible && this.enabled && this.focused
              && this.handleTouchBounds().contains(x, y)
              && this.touchedSelectionHandle(x, y) != 0;
   }

   void placeCursorFromTouch(float var1) {
      this.placeCursorFromTouch(var1, this.contentBounds().centerY());
   }

   void placeCursorFromTouch(float var1, float var2) {
      this.setSelection(this.cursorOffsetFromTouch(var1, var2));
   }

   boolean selectWordAt(float var1, float var2) {
      return this.selectWordAtOffset(this.cursorOffsetFromTouch(var1, var2));
   }

   boolean isLongPressAtTextEnd(float x, float y) {
      if (this.editable.length() == 0
              || this.normalizedSelectionStart() != this.editable.length()
              || this.normalizedSelectionEnd() != this.editable.length()) return false;
      PointF caret = this.magnifierSourcePoint();
      RectF line = this.magnifierLineBounds();
      return x >= caret.x - this.touchSlop && x <= line.right + this.touchSlop
              && y >= line.top - this.touchSlop && y <= line.bottom + this.touchSlop;
   }

   private boolean selectWordAtOffset(int offset) {
      if (this.editable.length() != 0 && !this.password && this.selectionEnabled) {
         int[] range = TextBoundaryHelper.wordAt(
                 this.editable, offset);
         this.setSelection(range[0], range[1]);
         this.selectionAnchorStart = range[0];
         this.selectionAnchorEnd = range[1];
         return range[0] < range[1];
      } else {
         return false;
      }
   }

   private int cursorOffsetFromTouch(float var1, float var2) {
      CharSequence var3 = this.getDisplayText();
      float var6;
      int var7;
      if (this.isMultiline()) {
         RectF var9 = this.contentBounds();
         StaticLayout var10 = this.multilineLayout(var3, this.textPaint, false);
         var6 = this.multilineLayoutTop(var9, var10);
         var7 = var10.getLineForVertical(Math.round(var2 - var6));
         return clamp(var10.getOffsetForHorizontal(var7, Math.max(0.0F, var1 - var9.left)), 0, this.editable.length());
      } else {
         float var4 = var1 - this.contentBounds().left + this.horizontalScrollPx;
         int var5 = 0;
         var6 = Float.MAX_VALUE;

         for(var7 = 0; var7 <= var3.length(); ++var7) {
            float var8 = Math.abs(var4 - this.measurePrefix(var3, var7));
            if (var8 < var6) {
               var6 = var8;
               var5 = var7;
            }
         }

         return clamp(var5, 0, this.editable.length());
      }
   }

   public void draw(Canvas var1) {
      if (!this.visible) return;
      var1.getMatrix(this.drawMatrix);
      this.drawField(var1, true, false);
   }

   /** The regular pass draws the field chrome around shared text content. */
   private void drawField(Canvas var1, boolean drawHandles, boolean forceCursor) {
      if (this.visible) {
         this.backgroundPaint.setColor(this.focused ? this.focusedBackgroundColor : this.backgroundColor);
         var1.drawRoundRect(this.bounds, this.cornerRadiusPx, this.cornerRadiusPx, this.backgroundPaint);
         if (this.strokeWidthPx > 0.0F) {
            this.strokePaint.setColor(this.focused ? this.focusedStrokeColor : this.strokeColor);
            float var2 = this.strokeWidthPx / 2.0F;
            RectF var3 = new RectF(this.bounds);
            var3.inset(var2, var2);
            var1.drawRoundRect(var3, Math.max(0.0F, this.cornerRadiusPx - var2), Math.max(0.0F, this.cornerRadiusPx - var2), this.strokePaint);
         }
         this.drawTextContent(var1, drawHandles, forceCursor);
      }
   }

   /** Uses the same layout, selection and caret geometry in both render passes. */
   private void drawTextContent(Canvas var1, boolean drawHandles, boolean forceCursor) {
         RectF var19 = this.contentBounds();
         int var20 = var1.save();
         var1.clipRect(var19);
         CharSequence var4 = this.getDisplayText();
         boolean var5 = var4.length() == 0;
         TextPaint var6 = var5 ? this.hintPaint : this.textPaint;
         Object var7 = var5 ? this.hint : var4;
         float var9;
         int var11;
         float var14;
         float var15;
         float var16;
         float var17;
         if (this.isMultiline()) {
            StaticLayout var21 = this.multilineLayout((CharSequence)var7, var6, var5);
            var9 = this.multilineLayoutTop(var19, var21);
            if (this.focused && !this.password && this.editable.length() > 0) {
               this.drawMultilineSelection(var1, var21, var19.left, var9);
            }

            var1.save();
            var1.translate(var19.left, var9);
            var21.draw(var1);
            var1.restore();
            if (this.focused && this.enabled && (forceCursor || this.isCursorVisible())
                    && this.normalizedSelectionStart() == this.normalizedSelectionEnd()) {
               StaticLayout var22 = this.multilineLayout(var4, this.textPaint, false);
               var11 = clamp(this.normalizedSelectionEnd(), 0, var22.getText().length());
               int var23 = var22.getLineForOffset(var11);
               float var24 = var19.left + var22.getPrimaryHorizontal(var11);
               var14 = this.multilineLayoutTop(var19, var22) + (float)var22.getLineTop(var23);
               var15 = this.multilineLayoutTop(var19, var22) + (float)var22.getLineBottom(var23);
               var16 = Math.max(1.0F, (float)Math.round(this.cursorWidthPx));
               var17 = Math.max(var19.left, Math.min(var24, var19.right - var16));
               this.cursorPaint.setColor(this.cursorColor);
               var1.drawRect(var17, this.cursorTop(var14, var15, var19),
                       var17 + var16, this.cursorBottom(var14, var15, var19),
                       this.cursorPaint);
            }

            if (drawHandles) this.drawMultilineScrollBar(var1, var19, var21);
            var1.restoreToCount(var20);
            if (drawHandles && this.focused && this.enabled && this.editable.length() > 0) {
               if (this.normalizedSelectionStart() == this.normalizedSelectionEnd()) {
                  this.drawCursorHandle(var1, var19);
               } else if (!this.password) {
                  this.drawMultilineSelectionHandles(var1, var21, var19.left, var9);
               }
            }
         } else {
            Paint.FontMetrics var8 = var6.getFontMetrics();
            var9 = var19.centerY() - (var8.ascent + var8.descent) / 2.0F;
            float var10 = var19.left - this.horizontalScrollPx;
            if (this.focused && !this.password && this.editable.length() > 0) {
               this.drawSelection(var1, var19, var9, var10, var4);
            }

            var1.drawText((CharSequence)var7, 0, ((CharSequence)var7).length(), var10, var9, var6);
            if (this.focused && this.enabled && (forceCursor || this.isCursorVisible())
                    && this.normalizedSelectionStart() == this.normalizedSelectionEnd()) {
               var11 = this.normalizedSelectionEnd();
               float var12 = var10 + this.measurePrefix(var4, var11);
               Paint.FontMetrics var13 = this.textPaint.getFontMetrics();
               var14 = var19.centerY() - (var13.ascent + var13.descent) / 2.0F;
               var15 = Math.max(1.0F, (float)Math.round(this.cursorWidthPx));
               var16 = (float)Math.ceil((double)var19.left);
               var17 = Math.max(var16, (float)Math.floor((double)(var19.right - var15)));
               float var18 = Math.max(var16, Math.min((float)Math.round(var12 - var15 / 2.0F), var17));
               this.cursorPaint.setColor(this.cursorColor);
               float var25 = var14 + var13.ascent;
               float var26 = var14 + var13.descent;
               var1.drawRect(var18, this.cursorTop(var25, var26, var19),
                       var18 + var15, this.cursorBottom(var25, var26, var19),
                       this.cursorPaint);
            }

            var1.restoreToCount(var20);
            if (drawHandles && this.focused && this.enabled && this.editable.length() > 0) {
               if (this.normalizedSelectionStart() == this.normalizedSelectionEnd()) {
                  this.drawCursorHandle(var1, var19);
               } else if (!this.password) {
                  this.drawSingleLineSelectionHandles(var1, var19, var9, var10, var4);
               }
            }
         }
   }

   /** Drawn by the group after every layer so the lens stays above the scene. */
   public void drawMagnifier(Canvas canvas) {
      this.drawMagnifier(canvas, this.hostView, 0f, 0f);
   }

   /** Draw the lens in an ancestor's canvas, beyond this field's host bounds. */
   public void drawMagnifier(Canvas canvas, View overlayHost, float hostX, float hostY) {
      if (!this.magnifierVisible || !this.focused || !this.visible) return;
      if (overlayHost.getWidth() <= 0 || overlayHost.getHeight() <= 0) return;
      Matrix contentToOverlay = new Matrix(this.drawMatrix);
      contentToOverlay.postTranslate(hostX, hostY);
      TextSelectionMagnifier.draw(canvas, overlayHost, contentToOverlay,
              this.magnifierSourcePoint(), this.magnifierLineBounds(),
              this.magnifierBackgroundColor(), this.textSizePx, this.cursorColor,
              this.normalizedSelectionStart() != this.normalizedSelectionEnd(),
              content -> this.drawTextContent(content, false, true));
   }

   /** Whether an ancestor drawing the lens should request another frame. */
   public boolean isMagnifierActive() {
      return this.magnifierVisible || this.magnifierPending;
   }

   private PointF magnifierSourcePoint() {
      int offset = this.magnifierActiveOffset();
      RectF content = this.contentBounds();
      CharSequence displayText = this.getDisplayText();
      if (this.isMultiline()) {
         StaticLayout layout = this.multilineLayout(displayText, this.textPaint, false);
         int line = layout.getLineForOffset(clamp(offset, 0, displayText.length()));
         float top = this.multilineLayoutTop(content, layout);
         return new PointF(content.left + layout.getPrimaryHorizontal(offset),
                 top + (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f);
      }
      return new PointF(content.left - this.horizontalScrollPx
              + this.measurePrefix(displayText, offset), content.centerY());
   }

   private int magnifierActiveOffset() {
      return this.touchedSelectionHandle == 1
              ? this.normalizedSelectionStart() : this.getRawSelectionEnd();
   }

   private int magnifierBackgroundColor() {
      int fieldColor = this.focused ? this.focusedBackgroundColor : this.backgroundColor;
      if (Color.alpha(fieldColor) == 255) return fieldColor;
      int foreground = this.editable.length() == 0 ? this.hintColor : this.textColor;
      float brightness = Color.red(foreground) * 0.2126f
              + Color.green(foreground) * 0.7152f
              + Color.blue(foreground) * 0.0722f;
      return brightness < 140f ? 0xfff4f4f4 : 0xff353535;
   }

   private RectF magnifierLineBounds() {
      RectF content = this.contentBounds();
      if (!this.isMultiline()) return content;
      StaticLayout layout = this.multilineLayout(
              this.getDisplayText(), this.textPaint, false);
      int line = layout.getLineForOffset(this.magnifierActiveOffset());
      float top = this.multilineLayoutTop(content, layout);
      return new RectF(content.left,
              Math.max(content.top, top + layout.getLineTop(line)),
              content.right,
              Math.min(content.bottom, top + layout.getLineBottom(line)));
   }

   private void showMagnifier() {
      this.hostView.removeCallbacks(this.showSelectionToolbarRunnable);
      this.hostView.removeCallbacks(this.showMagnifierRunnable);
      if (this.owner != null) this.owner.hideSelectionActionMode(this);
      if (this.magnifierVisible) return;
      this.magnifierPending = true;
      this.hostView.postDelayed(this.showMagnifierRunnable, MAGNIFIER_TOOLBAR_DELAY_MS);
   }

   private void hideMagnifier() {
      this.hostView.removeCallbacks(this.showSelectionToolbarRunnable);
      this.hostView.removeCallbacks(this.showMagnifierRunnable);
      this.magnifierPending = false;
      if (this.magnifierVisible) {
         this.magnifierVisible = false;
         this.invalidate();
      }
   }

   private void showSelectionToolbarAfterMagnifier(boolean magnifierWasActive) {
      if (this.owner == null) return;
      if (magnifierWasActive) {
         this.hostView.postDelayed(this.showSelectionToolbarRunnable,
                 MAGNIFIER_TOOLBAR_DELAY_MS);
      } else {
         this.showTextActionMode();
      }
   }

   private void showTextActionMode() {
      if (this.normalizedSelectionStart() != this.normalizedSelectionEnd()) {
         this.owner.showSelectionActionMode(this);
      } else {
         this.owner.showInsertionActionMode(this);
      }
   }

   private float cursorTop(float naturalTop, float naturalBottom, RectF clipBounds) {
      if (this.cursorHeightPx <= 0.0F) return Math.max(clipBounds.top, naturalTop);
      float center = (naturalTop + naturalBottom) / 2.0F;
      return Math.max(clipBounds.top, center - this.cursorHeightPx / 2.0F);
   }

   private float cursorBottom(float naturalTop, float naturalBottom, RectF clipBounds) {
      if (this.cursorHeightPx <= 0.0F) return Math.min(clipBounds.bottom, naturalBottom);
      float center = (naturalTop + naturalBottom) / 2.0F;
      return Math.min(clipBounds.bottom, center + this.cursorHeightPx / 2.0F);
   }

   @Override public CharSequence selectionText() { return this.editable; }

   @Override public int selectionOffsetAt(float x, float y) {
      return this.cursorOffsetFromTouch(x, y);
   }

   @Override public void applySelection(int anchor, int extent) {
      this.setSelection(anchor, extent);
   }

   public boolean onTouchEvent(MotionEvent var1) {
      switch (var1.getActionMasked()) {
         case 0:
            this.stopSelectionAutoScroll();
            this.touchCaptured = this.contains(var1.getX(), var1.getY())
                    || this.containsSelectionHandle(var1.getX(), var1.getY());
            if (this.touchCaptured) {
                this.hostView.removeCallbacks(this.showSelectionToolbarRunnable);
                this.touchCursorDragCandidate = false;
                this.touchCursorDragging = false;
                this.touchLongPressInsertion = false;
                this.selectionHandleDragging = false;
               this.touchBeganFocused = this.focused;
               if (!this.focused) this.owner.requestFocus(this);
               this.touchedSelectionHandle = this.touchedSelectionHandle(var1.getX(), var1.getY());
               this.touchInsideSelection = this.touchedSelectionHandle == 0
                       && this.isTouchInsideSelection(var1.getX(), var1.getY());
               boolean secondTap = this.isSecondTap(var1)
                       && this.touchedSelectionHandle != 1
                       && this.touchedSelectionHandle != 2
                       && !this.touchInsideSelection;
               this.doubleTapSelecting = secondTap && (this.touchedSelectionHandle == 3
                       ? this.selectWordAtOffset(this.normalizedSelectionEnd())
                       : this.selectWordAt(var1.getX(), var1.getY()));
               if (this.touchedSelectionHandle != 0 || this.doubleTapSelecting) {
                  this.showMagnifier();
               }
               if (this.doubleTapSelecting) this.lastTapUpTime = 0L;
               if (this.touchedSelectionHandle == 0) {
                  if (!this.touchInsideSelection && !this.doubleTapSelecting) {
                     // Keep the current scroll and selection until this is known to be a tap.
                     this.touchCursorDragCandidate = !this.isMultilineScrollable()
                             || this.normalizedSelectionStart() == this.normalizedSelectionEnd();
                  }
               } else if (!this.doubleTapSelecting) {
                  this.selectionAnchorStart = this.normalizedSelectionStart();
                  this.selectionAnchorEnd = this.normalizedSelectionEnd();
                  if (this.touchedSelectionHandle == 3) {
                     PointF endpoint = this.cursorHandleEndpoint();
                     this.cursorDragOffsetX = var1.getX() - endpoint.x;
                     this.cursorDragOffsetY = var1.getY() - this.cursorDragAnchorY(endpoint);
                  }
                  if (this.isMultiline() && this.touchedSelectionHandle != 0) {
                     StaticLayout layout = this.multilineLayout(
                             this.getDisplayText(), this.textPaint, false);
                     int offset = this.touchedSelectionHandle == 1
                             ? this.normalizedSelectionStart()
                             : this.normalizedSelectionEnd();
                     this.handleDragLine = layout.getLineForOffset(offset);
                  }
               }

               this.touchDownX = var1.getX();
               this.touchDownY = this.touchLastY = var1.getY();
               this.touchDownTime = var1.getEventTime();
               this.touchDragging = false;
               this.touchLongPressTriggered = false;
               this.touchStayedInTapRegion = true;
               if (this.touchedSelectionHandle == 0 && !this.touchInsideSelection
                       && !this.doubleTapSelecting) {
                  this.hostView.postDelayed(this.touchLongPressRunnable, (long)ViewConfiguration.getLongPressTimeout());
               }
            }

            return this.touchCaptured;
         case 1:
            boolean magnifierWasActive = this.magnifierVisible || this.magnifierPending;
            this.hideMagnifier();
            this.hostView.removeCallbacks(this.touchLongPressRunnable);
            boolean var3 = this.touchCaptured;
            if (this.touchCaptured && this.touchStayedInTapRegion) {
               this.touchStayedInTapRegion = TextSelectionController.isWithinTouchSlop(
                       var1.getX(), var1.getY(), this.touchDownX, this.touchDownY,
                       this.touchSlop);
            }
            boolean completedTap = this.touchCaptured && !this.touchDragging
                    && !this.touchLongPressTriggered && !this.doubleTapSelecting
                    && this.touchStayedInTapRegion
                    && (this.touchedSelectionHandle == 0 || this.touchedSelectionHandle == 3)
                    && !this.touchInsideSelection
                    && var1.getEventTime() - this.touchDownTime
                    <= (long)ViewConfiguration.getDoubleTapTimeout();
            if (completedTap) {
               this.lastTapUpTime = var1.getEventTime();
               this.lastTapX = this.touchDownX;
               this.lastTapY = this.touchDownY;
            } else {
               this.lastTapUpTime = 0L;
            }
            if (this.touchInsideSelection && !this.touchDragging
                    && this.touchStayedInTapRegion) {
               this.placeCursorFromTouch(var1.getX(), var1.getY());
            } else if (this.touchCaptured && !this.touchDragging
                    && !this.touchLongPressTriggered && !this.doubleTapSelecting
                    && this.touchStayedInTapRegion && this.touchedSelectionHandle == 0) {
               this.placeCursorFromTouch(var1.getX(), var1.getY());
            }
            if (this.touchBeganFocused && !this.touchDragging
                    && !this.touchLongPressTriggered && this.touchStayedInTapRegion
                    && this.touchedSelectionHandle == 0 && this.owner != null) {
               this.owner.requestFocus(this);
            }
            if ((this.doubleTapSelecting || this.touchLongPressTriggered
                    || this.touchedSelectionHandle == 1 || this.touchedSelectionHandle == 2)
                    && this.owner != null) {
               this.showSelectionToolbarAfterMagnifier(magnifierWasActive);
            }
            this.touchCaptured = false;
            this.touchDragging = false;
            this.touchLongPressTriggered = false;
            this.touchLongPressInsertion = false;
            this.touchInsideSelection = false;
            this.touchBeganFocused = false;
            this.doubleTapSelecting = false;
             this.touchCursorDragCandidate = false;
             this.touchCursorDragging = false;
            this.selectionHandleDragging = false;
            this.touchedSelectionHandle = 0;
            this.stopSelectionAutoScroll();
            return var3;
         case 2:
            if (!this.touchCaptured) {
               return false;
            }
            if (this.touchStayedInTapRegion) {
               this.touchStayedInTapRegion = TextSelectionController.isWithinTouchSlop(
                       var1.getX(), var1.getY(), this.touchDownX, this.touchDownY,
                       this.touchSlop);
               if (!this.touchStayedInTapRegion) {
                  this.hostView.removeCallbacks(this.touchLongPressRunnable);
               }
            }
            if (this.doubleTapSelecting) {
               this.extendDoubleTapSelection(var1.getX(), var1.getY());
               this.updateSelectionAutoScroll(var1.getX(), var1.getY());
               return true;
            } else if (this.touchedSelectionHandle != 0) {
               if ((this.touchedSelectionHandle == 1 || this.touchedSelectionHandle == 2)
                       && !this.selectionHandleDragging) {
                  if (!TextSelectionController.isWithinTouchSlop(
                          var1.getX(), var1.getY(), this.touchDownX, this.touchDownY,
                          this.touchSlop)) {
                     this.selectionHandleDragging = true;
                     if (this.owner != null) this.owner.hideSelectionActionMode(this);
                  } else {
                     return true;
                  }
               }
               this.applySelectionDrag(var1.getX(), var1.getY());
               this.updateSelectionAutoScroll(var1.getX(), var1.getY());
               return true;
            } else {
               if (this.touchLongPressTriggered) {
                  if (this.touchLongPressInsertion) {
                     this.placeCursorFromTouch(var1.getX(), var1.getY());
                  } else {
                     this.applySelectionDrag(var1.getX(), var1.getY());
                     this.updateSelectionAutoScroll(var1.getX(), var1.getY());
                  }
                  return true;
               }

                float horizontalDistance = var1.getX() - this.touchDownX;
                float verticalDistance = var1.getY() - this.touchDownY;
                if (this.touchCursorDragCandidate && !this.touchDragging
                        && Math.abs(horizontalDistance) > (float)this.touchSlop
                        && Math.abs(horizontalDistance) >= Math.abs(verticalDistance)) {
                   this.touchDragging = true;
                   this.touchCursorDragging = true;
                   this.showMagnifier();
                   this.hostView.removeCallbacks(this.touchLongPressRunnable);
                }

                if (this.touchCursorDragging) {
                   this.placeCursorFromTouch(var1.getX(), this.touchDownY);
                   if (!this.isMultiline()) {
                      this.updateSelectionAutoScroll(var1.getX(), this.touchDownY);
                   }
                   return true;
                }

               float var2 = this.touchLastY - var1.getY();
               if (!this.touchDragging && this.isMultilineScrollable() && Math.abs(var1.getY() - this.touchDownY) > (float)this.touchSlop) {
                  this.touchDragging = true;
                   this.touchCursorDragCandidate = false;
                  this.hostView.removeCallbacks(this.touchLongPressRunnable);
               }

               if (this.touchDragging) {
                  this.scrollMultilineBy(var2);
                  this.touchLastY = var1.getY();
               }

               return true;
            }
         case 3:
            boolean magnifierWasActiveOnCancel = this.magnifierVisible || this.magnifierPending;
            this.hideMagnifier();
            this.hostView.removeCallbacks(this.touchLongPressRunnable);
            boolean var4 = this.touchCaptured;
            if (this.selectionHandleDragging && this.owner != null) {
               this.showSelectionToolbarAfterMagnifier(magnifierWasActiveOnCancel);
            }
            this.touchCaptured = false;
            this.touchDragging = false;
            this.touchLongPressTriggered = false;
            this.touchLongPressInsertion = false;
            this.touchInsideSelection = false;
            this.touchBeganFocused = false;
            this.doubleTapSelecting = false;
            this.lastTapUpTime = 0L;
             this.touchCursorDragCandidate = false;
             this.touchCursorDragging = false;
            this.selectionHandleDragging = false;
            this.touchedSelectionHandle = 0;
            this.stopSelectionAutoScroll();
            return var4;
         default:
            return this.touchCaptured;
      }
   }

   private void handleTouchLongPress() {
      if (this.touchCaptured && !this.touchDragging && this.owner != null) {
         if (this.isLongPressAtTextEnd(this.touchDownX, this.touchDownY)) {
            this.touchLongPressInsertion = true;
            this.touchLongPressTriggered = true;
            this.touchCursorDragCandidate = false;
            this.hostView.performHapticFeedback(0);
         } else if (this.selectWordAt(this.touchDownX, this.touchDownY)) {
            this.showMagnifier();
            this.touchCursorDragCandidate = false;
            this.touchLongPressTriggered = true;
            this.hostView.performHapticFeedback(0);
            this.updateSelectionAutoScroll(this.touchDownX, this.touchDownY);
          } else {
             this.touchLongPressInsertion = true;
             this.touchLongPressTriggered = true;
             this.hostView.performHapticFeedback(0);
         }
      }
   }

   private boolean isSecondTap(MotionEvent event) {
      return TextSelectionController.isSecondTap(event, this.lastTapUpTime,
              this.lastTapX, this.lastTapY, this.doubleTapSlop);
   }

   private boolean isTouchInsideSelection(float x, float y) {
      int start = this.normalizedSelectionStart();
      int end = this.normalizedSelectionEnd();
      if (start == end || !this.contentBounds().contains(x, y)) return false;
      int offset = this.cursorOffsetFromTouch(x, y);
      return offset >= start && offset <= end;
   }

   private void extendLongPressSelection(float var1, float var2) {
      TextSelectionController.extendLongPress(this,
              this.selectionAnchorStart, this.selectionAnchorEnd, var1, var2);
   }

   private void extendDoubleTapSelection(float x, float y) {
      TextSelectionController.extendDoubleTap(this,
              this.selectionAnchorStart, this.selectionAnchorEnd, x, y);
   }

   private void applySelectionDrag(float var1, float var2) {
      if (this.doubleTapSelecting) {
         this.extendDoubleTapSelection(var1, var2);
      } else if (this.touchedSelectionHandle == 3) {
         float x = var1 - this.cursorDragOffsetX;
         float y = var2 - this.cursorDragOffsetY;
         if (this.isMultiline()) {
            this.setSelection(this.handleDragOffsetFromTouch(x, y));
         } else {
            this.placeCursorFromTouch(x, y);
         }
      } else if (this.touchedSelectionHandle == 1) {
         int movingOffset = this.isMultiline()
                 ? this.handleDragOffsetFromTouch(var1, var2)
                 : this.cursorOffsetFromTouch(var1, var2);
         TextSelectionController.dragStartHandle(this,
                 movingOffset, this.selectionAnchorEnd);
      } else if (this.touchedSelectionHandle == 2) {
         int movingOffset = this.isMultiline()
                 ? this.handleDragOffsetFromTouch(var1, var2)
                 : this.cursorOffsetFromTouch(var1, var2);
         TextSelectionController.dragEndHandle(this,
                 this.selectionAnchorStart, movingOffset);
      } else {
         this.extendLongPressSelection(var1, var2);
      }

   }

   private int handleDragOffsetFromTouch(float x, float y) {
      RectF content = this.contentBounds();
      StaticLayout layout = this.multilineLayout(
              this.getDisplayText(), this.textPaint, false);
      float vertical = y - this.multilineLayoutTop(content, layout);
      int line = Math.max(0, Math.min(this.handleDragLine,
              layout.getLineCount() - 1));

      // Selection handles wait for the following line's bottom; the cursor
      // handle still switches at the next line's bottom.
      int thresholdLineOffset = this.touchedSelectionHandle == 3 ? 1 : 2;
      int lastLine = layout.getLineCount() - 1;
      float lastLineHeight = layout.getLineBottom(lastLine)
              - layout.getLineTop(lastLine);
      while (line < lastLine) {
         int thresholdLine = line + thresholdLineOffset;
         float threshold = layout.getLineBottom(Math.min(thresholdLine, lastLine))
                 + Math.max(0, thresholdLine - lastLine) * lastLineHeight;
         if (vertical < threshold) break;
         line++;
      }
      while (line > 0 && vertical < layout.getLineTop(line)) {
         line--;
      }
      this.handleDragLine = line;
      int offset = layout.getOffsetForHorizontal(line,
              Math.max(0.0F, x - content.left));
      return clamp(offset, 0, this.editable.length());
   }

   private void updateSelectionAutoScroll(float var1, float var2) {
      this.selectionDragX = var1;
      this.selectionDragY = var2;
      float scrollX = this.touchedSelectionHandle == 3 && !this.doubleTapSelecting
              ? var1 - this.cursorDragOffsetX : var1;
      float scrollY = this.touchedSelectionHandle == 3 && !this.doubleTapSelecting
              ? var2 - this.cursorDragOffsetY : var2;
      RectF var3 = this.contentBounds();
      float var4 = Math.max(16.0F, this.textSizePx * 0.8F);
      if (!this.isMultiline()) {
         float var5 = this.maxHorizontalScroll();
         if (scrollX <= var3.left + var4 && this.horizontalScrollPx > 0.0F) {
            this.selectionAutoScrollDirection = -1;
         } else if (scrollX >= var3.right - var4
                 && this.horizontalScrollPx < var5 - 0.5F) {
            this.selectionAutoScrollDirection = 1;
         } else {
            this.stopSelectionAutoScroll();
            return;
         }
      } else {
         if (!this.isMultilineScrollable()) {
            this.stopSelectionAutoScroll();
            return;
         }

         if (scrollY <= var3.top + var4 && this.verticalScrollPx > 0.0F) {
            this.selectionAutoScrollDirection = -1;
         } else {
            if (!(scrollY >= var3.bottom - var4) || !this.canScrollMultilineForward()) {
               this.stopSelectionAutoScroll();
               return;
            }

            this.selectionAutoScrollDirection = 1;
         }
      }

      if (!this.selectionAutoScrollPosted) {
         this.selectionAutoScrollPosted = true;
         this.hostView.postOnAnimation(this.selectionAutoScrollRunnable);
      }
   }

   private void runSelectionAutoScroll() {
      this.selectionAutoScrollPosted = false;
      if (this.touchCaptured && this.selectionAutoScrollDirection != 0
              && (this.touchLongPressTriggered || this.doubleTapSelecting
              || this.touchedSelectionHandle != 0
              || this.touchCursorDragging)) {
         float var2 = Math.max(4.0F, this.textSizePx * 0.32F) * (float)this.selectionAutoScrollDirection;
         float var1;
         float var3;
         if (this.isMultiline()) {
            var1 = this.verticalScrollPx;
            this.scrollMultilineBy(var2);
            var3 = this.verticalScrollPx;
         } else {
            var1 = this.horizontalScrollPx;
            this.horizontalScrollPx = Math.max(0.0F,
                    Math.min(this.maxHorizontalScroll(), this.horizontalScrollPx + var2));
            var3 = this.horizontalScrollPx;
            this.invalidate();
         }

         if (Math.abs(var3 - var1) < 0.5F) {
            this.stopSelectionAutoScroll();
         } else {
            if (this.touchCursorDragging) {
               this.placeCursorFromTouch(this.selectionDragX, this.touchDownY);
            } else {
               this.applySelectionDrag(this.selectionDragX, this.selectionDragY);
            }
            this.updateSelectionAutoScroll(this.selectionDragX, this.selectionDragY);
         }
      }
   }

   private boolean canScrollMultilineForward() {
      StaticLayout var1 = this.multilineLayout(this.getDisplayText(), this.textPaint, false);
      float var2 = Math.max(0.0F, (float)var1.getHeight() - this.contentBounds().height());
      return this.verticalScrollPx < var2 - 0.5F;
   }

   private float maxHorizontalScroll() {
      if (this.isMultiline()) {
         return 0.0F;
      }

      CharSequence var1 = this.getDisplayText();
      return Math.max(0.0F,
              this.textPaint.measureText(var1, 0, var1.length())
                      - this.contentBounds().width());
   }

   private void stopSelectionAutoScroll() {
      this.selectionAutoScrollDirection = 0;
      if (this.selectionAutoScrollPosted) {
         this.hostView.removeCallbacks(this.selectionAutoScrollRunnable);
         this.selectionAutoScrollPosted = false;
      }

   }

   RectF getSelectionActionBounds() {
      int selectionStart = this.normalizedSelectionStart();
      RectF content = this.contentBounds();
      float halfWidth = Math.max(2.0F, this.textSizePx * 0.08F);
      if (this.isMultiline()) {
         StaticLayout layout = this.multilineLayout(this.getDisplayText(), this.textPaint, false);
         int offset = clamp(selectionStart, 0, layout.getText().length());
         int line = layout.getLineForOffset(offset);
         float layoutTop = this.multilineLayoutTop(content, layout);
         float anchorX = content.left + layout.getPrimaryHorizontal(offset);
         float lineTop = layoutTop + (float)layout.getLineTop(line);
         float lineBottom = layoutTop + (float)layout.getLineBottom(line);
         if (lineBottom <= content.top || lineTop >= content.bottom) {
            return this.selectionActionBoundsAtFieldTop(anchorX, halfWidth);
         }

         float lineHeight = Math.max(1.0F, lineBottom - lineTop);
         float visibleTop = Math.max(content.top,
                 Math.min(lineTop, content.bottom - lineHeight));
         anchorX = Math.max(content.left, Math.min(anchorX, content.right));
         return new RectF(anchorX - halfWidth, visibleTop,
                 anchorX + halfWidth, visibleTop + lineHeight);
      } else {
         Paint.FontMetrics metrics = this.textPaint.getFontMetrics();
         float baseline = content.centerY() - (metrics.ascent + metrics.descent) / 2.0F;
         float anchorX = content.left - this.horizontalScrollPx
                 + this.measurePrefix(this.getDisplayText(), selectionStart);
         if (anchorX < content.left || anchorX > content.right) {
            return this.selectionActionBoundsAtFieldTop(anchorX, halfWidth);
         }

         return new RectF(anchorX - halfWidth, baseline + metrics.ascent,
                 anchorX + halfWidth, baseline + metrics.descent);
      }
   }

   private RectF selectionActionBoundsAtFieldTop(float anchorX, float halfWidth) {
      float clampedX = Math.max(this.bounds.left, Math.min(anchorX, this.bounds.right));
      return new RectF(clampedX - halfWidth, this.bounds.top,
              clampedX + halfWidth, this.bounds.top + 1.0F);
   }

   private boolean isMultilineScrollable() {
      if (!this.isMultiline()) {
         return false;
      } else {
         StaticLayout var1 = this.multilineLayout(this.getDisplayText(), this.textPaint, false);
         return (float)var1.getHeight() > this.contentBounds().height() + 0.5F;
      }
   }

   private void scrollMultilineBy(float var1) {
      StaticLayout var2 = this.multilineLayout(this.getDisplayText(), this.textPaint, false);
      float var3 = Math.max(0.0F, (float)var2.getHeight() - this.contentBounds().height());
      this.verticalScrollPx = Math.max(0.0F, Math.min(var3, this.verticalScrollPx + var1));
      this.resetCursorBlink();
      this.invalidate();
   }

   public boolean isVisible() {
      return this.visible;
   }

   public TextField setVisible(boolean var1) {
      this.visible = var1;
      this.invalidate();
      return this;
   }

   public void release() {
      this.detach();
   }

   private void drawSelection(Canvas var1, RectF var2, float var3, float var4, CharSequence var5) {
      int var6 = this.normalizedSelectionStart();
      int var7 = this.normalizedSelectionEnd();
      if (var6 != var7) {
         Paint.FontMetrics var8 = this.textPaint.getFontMetrics();
         float var9 = var4 + this.measurePrefix(var5, var6);
         float var10 = var4 + this.measurePrefix(var5, var7);
         var1.drawRect(Math.max(var2.left, var9), var3 + var8.ascent, Math.min(var2.right, var10), var3 + var8.descent, this.selectionPaint);
      }
   }

   private void drawSingleLineSelectionHandles(
           Canvas canvas, RectF content, float baseline, float textLeft, CharSequence text) {
      int start = this.normalizedSelectionStart();
      int end = this.normalizedSelectionEnd();
      if (start == end) return;
      Paint.FontMetrics metrics = this.textPaint.getFontMetrics();
      float endpointY = baseline + metrics.descent;
      PointF startEndpoint = this.selectionEndpointPoint(
              content, textLeft + this.measurePrefix(text, start), endpointY);
      PointF endEndpoint = this.selectionEndpointPoint(
              content, textLeft + this.measurePrefix(text, end), endpointY);
      this.drawSelectionHandle(canvas, startEndpoint, 1);
      this.drawSelectionHandle(canvas, endEndpoint, 2);
   }

   private void drawMultilineSelection(Canvas var1, StaticLayout var2, float var3, float var4) {
      int var5 = this.normalizedSelectionStart();
      int var6 = this.normalizedSelectionEnd();
      if (var5 != var6) {
         Path var7 = new Path();
         var2.getSelectionPath(var5, var6, var7);
         var1.save();
         var1.translate(var3, var4);
         var1.drawPath(var7, this.selectionPaint);
         var1.restore();
      }
   }

   private void drawMultilineSelectionHandles(
           Canvas canvas, StaticLayout layout, float left, float top) {
      int start = this.normalizedSelectionStart();
      int end = this.normalizedSelectionEnd();
      if (start == end) return;
      RectF content = this.contentBounds();
      PointF startEndpoint = this.selectionEndpointPoint(layout, left, top, start);
      PointF endEndpoint = this.selectionEndpointPoint(layout, left, top, end);
      this.drawSelectionHandle(canvas, startEndpoint, 1);
      this.drawSelectionHandle(canvas, endEndpoint, 2);
   }

   private float selectionHandleRadius() {
      return Math.max(8.0F, this.textSizePx * 0.32F);
   }

   private PointF cursorHandleEndpoint() {
      int offset = this.normalizedSelectionEnd();
      RectF content = this.contentBounds();
      if (this.isMultiline()) {
         StaticLayout layout = this.multilineLayout(this.getDisplayText(), this.textPaint, false);
         float top = this.multilineLayoutTop(content, layout);
         int line = layout.getLineForOffset(offset);
         if (top + this.handleLineBottom(layout, line) <= content.top
                 || top + layout.getLineTop(line) >= content.bottom) return null;
         return this.selectionEndpointPoint(layout, content.left, top, offset);
      }

      Paint.FontMetrics metrics = this.textPaint.getFontMetrics();
      float baseline = content.centerY() - (metrics.ascent + metrics.descent) / 2.0F;
      return this.selectionEndpointPoint(content,
              content.left - this.horizontalScrollPx
                      + this.measurePrefix(this.getDisplayText(), offset),
              baseline + metrics.descent);
   }

   private float cursorDragAnchorY(PointF endpoint) {
      if (!this.isMultiline()) return endpoint.y;
      RectF content = this.contentBounds();
      StaticLayout layout = this.multilineLayout(this.getDisplayText(), this.textPaint, false);
      int line = layout.getLineForOffset(this.normalizedSelectionEnd());
      float top = this.multilineLayoutTop(content, layout);
      float visibleTop = Math.max(content.top, top + layout.getLineTop(line));
      float visibleBottom = Math.min(content.bottom, top + this.handleLineBottom(layout, line));
      return (visibleTop + visibleBottom) / 2.0F;
   }

   private void drawCursorHandle(Canvas canvas, RectF content) {
      if (this.normalizedSelectionEnd() >= this.editable.length()) return;
      PointF endpoint = this.cursorHandleEndpoint();
      if (endpoint != null) this.drawSelectionHandle(canvas, endpoint, 3);
   }

   private PointF selectionEndpointPoint(StaticLayout layout, float left, float top, int offset) {
      int safeOffset = clamp(offset, 0, layout.getText().length());
      int line = layout.getLineForOffset(safeOffset);
      RectF content = this.contentBounds();
      float lineTop = top + layout.getLineTop(line);
      float lineBottom = top + this.handleLineBottom(layout, line);
      if (lineBottom <= content.top || lineTop >= content.bottom) return null;
      return new PointF(left + layout.getPrimaryHorizontal(safeOffset), lineBottom);
   }

   private int handleLineBottom(StaticLayout layout, int line) {
      return VERSION.SDK_INT >= 34
              ? layout.getLineBottom(line, false)
              : layout.getLineBottom(line);
   }

   private PointF selectionEndpointPoint(RectF content, float x, float y) {
      return x < content.left || x > content.right ? null : new PointF(x, y);
   }

   private RectF handleTouchBounds() {
      return this.selectionHandles.touchBounds(this.bounds,
              this.selectionHandleRadius(), this.touchSlop);
   }

   private void drawSelectionHandle(Canvas canvas, PointF endpoint, int type) {
      this.selectionHandles.draw(canvas, endpoint, type, this.bounds,
              this.selectionHandleRadius(), this.selectionHandleColor,
              this.cursorPaint);
   }

   private int touchedSelectionHandle(float var1, float var2) {
      int var3 = this.normalizedSelectionStart();
      int var4 = this.normalizedSelectionEnd();
      if (var3 == var4 && var4 < this.editable.length()) {
         PointF endpoint = this.cursorHandleEndpoint();
         if (endpoint != null && this.isHandleHit(var1, var2, endpoint, 3)) return 3;
      }
      if (var3 != var4 && this.isMultiline()) {
         RectF var5 = this.contentBounds();
         StaticLayout var6 = this.multilineLayout(this.getDisplayText(), this.textPaint, false);
         float var7 = this.multilineLayoutTop(var5, var6);
         PointF start = this.selectionEndpointPoint(var6, var5.left, var7, var3);
         PointF end = this.selectionEndpointPoint(var6, var5.left, var7, var4);
         if (this.isHandleHit(var1, var2, start, 1)) return 1;
         return this.isHandleHit(var1, var2, end, 2) ? 2 : 0;
      } else if (var3 != var4) {
         RectF content = this.contentBounds();
         Paint.FontMetrics metrics = this.textPaint.getFontMetrics();
         float baseline = content.centerY() - (metrics.ascent + metrics.descent) / 2.0F;
         float textLeft = content.left - this.horizontalScrollPx;
         float endpointY = baseline + metrics.descent;
         PointF start = this.selectionEndpointPoint(content,
                 textLeft + this.measurePrefix(this.getDisplayText(), var3), endpointY);
         PointF end = this.selectionEndpointPoint(content,
                 textLeft + this.measurePrefix(this.getDisplayText(), var4), endpointY);
         if (this.isHandleHit(var1, var2, start, 1)) return 1;
         return this.isHandleHit(var1, var2, end, 2) ? 2 : 0;
      }
      return 0;
   }

   private boolean isHandleHit(float x, float y, PointF endpoint, int type) {
      return this.selectionHandles.hit(x, y, endpoint, type,
              this.selectionHandleRadius(), this.touchSlop);
   }

   private void drawMultilineScrollBar(Canvas var1, RectF var2, StaticLayout var3) {
      float var4 = var2.height();
      float var5 = (float)var3.getHeight();
      if (!(var5 <= var4 + 0.5F)) {
         float var6 = Math.max(3.0F, this.textSizePx * 0.12F);
         float var7 = Math.max(2.0F, var6);
         float var8 = Math.max(1.0F, var4 - var7 * 2.0F);
         float var9 = Math.max(this.textSizePx * 1.5F, var8 * var4 / var5);
         var9 = Math.min(var8, var9);
         float var10 = Math.max(1.0F, var5 - var4);
         float var11 = Math.max(0.0F, Math.min(1.0F, this.verticalScrollPx / var10));
         float var12 = var2.right - var6;
         float var13 = var2.top + var7 + (var8 - var9) * var11;
         this.scrollBarPaint.setColor(-1721996411);
         var1.drawRoundRect(var12, var13, var12 + var6, var13 + var9, var6 / 2.0F, var6 / 2.0F, this.scrollBarPaint);
      }
   }

   CharSequence getDisplayText() {
      if (!this.password) {
         return this.editable;
      } else {
         StringBuilder var1 = new StringBuilder(this.editable.length());

         for(int var2 = 0; var2 < this.editable.length(); ++var2) {
            var1.append('•');
         }

         return var1;
      }
   }

   void replaceSelection(CharSequence var1, int var2, boolean var3) {
      Objects.requireNonNull(var1, "Replacement text cannot be null.");
      int var4 = BaseInputConnection.getComposingSpanStart(this.editable);
      int var5 = BaseInputConnection.getComposingSpanEnd(this.editable);
      int var6;
      int var7;
      if (var4 >= 0 && var5 >= 0) {
         var6 = Math.min(var4, var5);
         var7 = Math.max(var4, var5);
      } else {
         var6 = this.normalizedSelectionStart();
         var7 = this.normalizedSelectionEnd();
      }

      String var8 = this.editable.toString();
      int var9 = this.editable.length();
      BaseInputConnection.removeComposingSpans(this.editable);
      this.invalidateMultilineLayout();
      this.editable.replace(var6, var7, var1);
      int var10 = this.editable.length() - (var9 - (var7 - var6));
      int var11 = var6 + Math.max(0, var10);
      if (var3 && var11 > var6) {
         this.editable.setSpan(new ComposingMarker(), var6, var11, 289);
      }

      int var12 = var2 > 0 ? var11 + var2 - 1 : var6 + var2;
      var12 = clamp(var12, 0, this.editable.length());
      Selection.setSelection(this.editable, var12);
      this.afterEdit(var8);
   }

   void deleteSurroundingText(int var1, int var2) {
      if (var1 >= 0 && var2 >= 0) {
         int var3 = this.normalizedSelectionStart();
         int var4 = this.normalizedSelectionEnd();
         int var5 = var3 == var4 ? Math.max(0, var3 - var1) : var3;
         int var6 = var3 == var4 ? Math.min(this.editable.length(), var4 + var2) : var4;
         this.replaceRange(var5, var6, "", true);
      } else {
         throw new IllegalArgumentException("Surrounding-text lengths cannot be negative.");
      }
   }

   void finishComposingText() {
      BaseInputConnection.removeComposingSpans(this.editable);
      this.updateImeSelection();
      this.invalidate();
   }

   void setComposingRegion(int var1, int var2) {
      int var3 = clamp(Math.min(var1, var2), 0, this.editable.length());
      int var4 = clamp(Math.max(var1, var2), 0, this.editable.length());
      BaseInputConnection.removeComposingSpans(this.editable);
      if (var4 > var3) {
         this.editable.setSpan(new ComposingMarker(), var3, var4, 289);
      }

      Selection.setSelection(this.editable, var4);
      this.resetCursorBlink();
      this.ensureCursorVisible();
      this.updateImeSelection();
      this.invalidate();
   }

   void deleteBeforeCursor() {
      int var1 = this.normalizedSelectionStart();
      int var2 = this.normalizedSelectionEnd();
      if (var1 != var2) {
         this.replaceRange(var1, var2, "", true);
      } else if (var1 != 0) {
         int var3 = TextBoundaryHelper.previousGrapheme(this.editable, var1);
         this.replaceRange(var3, var1, "", true);
      }
   }

   void deleteAfterCursor() {
      int var1 = this.normalizedSelectionStart();
      int var2 = this.normalizedSelectionEnd();
      if (var1 != var2) {
         this.replaceRange(var1, var2, "", true);
      } else if (var2 < this.editable.length()) {
         int var3 = TextBoundaryHelper.nextGrapheme(this.editable, var2);
         this.replaceRange(var2, var3, "", true);
      }
   }

   boolean performEditorAction(int var1) {
      return this.editorActionListener != null && this.editorActionListener.onEditorAction(this.id, var1);
   }

   void configureEditorInfo(EditorInfo var1) {
      var1.inputType = this.inputType;
      var1.imeOptions = this.imeOptions;
      var1.initialSelStart = this.getRawSelectionStart();
      var1.initialSelEnd = this.getRawSelectionEnd();
      var1.hintText = this.hint;
      var1.fieldName = this.id;
   }

   boolean isCursorVisible() {
      long var1 = SystemClock.uptimeMillis() - this.cursorBlinkStartedAt;
      return var1 / this.cursorBlinkIntervalMs % 2L == 0L;
   }

   void resetCursorBlink() {
      this.cursorBlinkStartedAt = SystemClock.uptimeMillis();
   }

   void invalidate() {
      if (this.owner != null) {
         this.owner.invalidateComponent();
      }

   }

   private void restartInput() {
      if (this.focused && this.owner != null) {
         this.owner.restartInput();
      }

   }

   private void setTextInternal(CharSequence var1, boolean var2) {
      Objects.requireNonNull(var1, "Text cannot be null.");
      String var3 = this.editable.toString();
      BaseInputConnection.removeComposingSpans(this.editable);
      this.invalidateMultilineLayout();
      this.editable.replace(0, this.editable.length(), var1);
      Selection.setSelection(this.editable, this.editable.length());
      this.ensureCursorVisible();
      if (var2 && !var3.contentEquals(this.editable)) {
         this.notifyTextChanged();
      }

      this.updateImeSelection();
      this.invalidate();
   }

   private void replaceRange(int var1, int var2, CharSequence var3, boolean var4) {
      String var5 = this.editable.toString();
      int var6 = this.editable.length();
      BaseInputConnection.removeComposingSpans(this.editable);
      this.invalidateMultilineLayout();
      this.editable.replace(var1, var2, var3);
      int var7 = this.editable.length() - (var6 - (var2 - var1));
      Selection.setSelection(this.editable, clamp(var1 + Math.max(0, var7), 0, this.editable.length()));
      if (var4) {
         this.afterEdit(var5);
      } else {
         this.ensureCursorVisible();
         this.invalidate();
      }

   }

   private void afterEdit(String var1) {
      this.invalidateMultilineLayout();
      this.resetCursorBlink();
      this.ensureCursorVisible();
      if (!var1.contentEquals(this.editable)) {
         this.notifyTextChanged();
      }

      this.updateImeSelection();
      this.invalidate();
   }

   void afterImeEdit(String before) {
      this.invalidateMultilineLayout();
      this.resetCursorBlink();
      this.ensureCursorVisible();
      if (!before.contentEquals(this.editable)) {
         this.notifyTextChanged();
      }

      this.updateImeSelection();
      this.invalidate();
   }

   void afterImeStateChange() {
      this.resetCursorBlink();
      this.ensureCursorVisible();
      this.updateImeSelection();
      this.invalidate();
   }

   private void notifyTextChanged() {
      if (this.textChangedListener != null) {
         this.textChangedListener.onTextChanged(this.id, this.editable.toString());
      }

   }

   private void updateImeSelection() {
      if (this.owner != null && this.focused) {
         this.owner.updateSelection(this);
      }

   }

   private void applyLengthFilter() {
      this.editable.setFilters(new InputFilter[]{new InputFilter.LengthFilter(this.maxLength)});
   }

   private void ensureSelection() {
      if (Selection.getSelectionStart(this.editable) < 0 || Selection.getSelectionEnd(this.editable) < 0) {
         Selection.setSelection(this.editable, this.editable.length());
      }

   }

   private int normalizedSelectionStart() {
      this.ensureSelection();
      return Math.min(Selection.getSelectionStart(this.editable), Selection.getSelectionEnd(this.editable));
   }

   private int normalizedSelectionEnd() {
      this.ensureSelection();
      return Math.max(Selection.getSelectionStart(this.editable), Selection.getSelectionEnd(this.editable));
   }

   private int activeSelectionEnd() {
      this.ensureSelection();
      if (this.touchedSelectionHandle == 1) {
         return this.normalizedSelectionStart();
      }

      if (this.touchedSelectionHandle == 2) {
         return this.normalizedSelectionEnd();
      }

      return clamp(Selection.getSelectionEnd(this.editable), 0, this.editable.length());
   }

   private void ensureCursorVisible() {
      RectF var1 = this.contentBounds();
      CharSequence var2 = this.getDisplayText();
      if (this.isMultiline()) {
         this.horizontalScrollPx = 0.0F;
         StaticLayout var9 = this.multilineLayout(var2, this.textPaint, false);
         int var10 = clamp(this.activeSelectionEnd(), 0, var9.getText().length());
         int var11 = var9.getLineForOffset(var10);
         float var6 = (float)var9.getLineTop(var11);
         float var7 = (float)var9.getLineBottom(var11);
         float var8 = Math.max(1.0F, var1.height());
         if (var7 - this.verticalScrollPx > var8) {
            this.verticalScrollPx = var7 - var8;
         } else if (var6 - this.verticalScrollPx < 0.0F) {
            this.verticalScrollPx = var6;
         }

         this.verticalScrollPx = Math.max(0.0F, Math.min(this.verticalScrollPx, Math.max(0.0F, (float)var9.getHeight() - var8)));
      } else {
         this.verticalScrollPx = 0.0F;
         float var3 = this.measurePrefix(var2, this.activeSelectionEnd());
         float var4 = Math.max(1.0F, var1.width());
         if (var3 - this.horizontalScrollPx > var4) {
            this.horizontalScrollPx = var3 - var4;
         } else if (var3 - this.horizontalScrollPx < 0.0F) {
            this.horizontalScrollPx = var3;
         }

         float var5 = Math.max(0.0F, this.textPaint.measureText(var2, 0, var2.length()) - var4);
         this.horizontalScrollPx = Math.max(0.0F, Math.min(this.horizontalScrollPx, var5));
      }
   }

   private RectF contentBounds() {
      this.contentBounds.set(this.bounds.left + this.horizontalPaddingPx, this.bounds.top + this.verticalPaddingPx, this.bounds.right - this.horizontalPaddingPx, this.bounds.bottom - this.verticalPaddingPx);
      return this.contentBounds;
   }

   private float measurePrefix(CharSequence var1, int var2) {
      int var3 = clamp(var2, 0, var1.length());
      return this.textPaint.measureText(var1, 0, var3);
   }

   boolean isMultiline() {
      return (this.inputType & 131072) != 0;
   }

   private StaticLayout multilineLayout(CharSequence var1, TextPaint var2, boolean var3) {
      int var4 = Math.max(1, (int)Math.floor((double)this.contentBounds().width()));
      if (this.multilineLayoutWidth != var4) {
         this.multilineLayoutWidth = var4;
         this.multilineLayoutDirty = true;
      }

      String var5 = var1 == null ? "" : var1.toString();
      String var6 = var3 ? this.multilineHintValue : this.multilineTextValue;
      StaticLayout var7 = var3 ? this.multilineHintLayout : this.multilineTextLayout;
      if (this.multilineLayoutDirty || var7 == null || !var5.equals(var6)) {
         if (this.multilineLayoutDirty) {
            this.multilineTextLayout = null;
            this.multilineHintLayout = null;
            this.multilineTextValue = null;
            this.multilineHintValue = null;
            this.multilineLayoutDirty = false;
         }

         var7 = this.buildMultilineLayout(var5, var2, var4, var3);
         if (var3) {
            this.multilineHintLayout = var7;
            this.multilineHintValue = var5;
         } else {
            this.multilineTextLayout = var7;
            this.multilineTextValue = var5;
         }
      }

      return var7;
   }

   private StaticLayout buildMultilineLayout(String var1, TextPaint var2, int var3, boolean var4) {
      StaticLayout var5 = android.text.StaticLayout.Builder.obtain(var1, 0, var1.length(), var2, var3).setAlignment(Alignment.ALIGN_NORMAL).setIncludePad(false).build();
      if (!var4 && !(this.multilineBottomEndInsetPx <= 0.0F)) {
         int var6 = Math.max(1, var5.getLineCount());
         int[] var7 = new int[var6];
         var7[var6 - 1] = Math.min(var3 - 1, Math.round(this.multilineBottomEndInsetPx));
         return android.text.StaticLayout.Builder.obtain(var1, 0, var1.length(), var2, var3).setAlignment(Alignment.ALIGN_NORMAL).setIncludePad(false).setIndents((int[])null, var7).build();
      } else {
         return var5;
      }
   }

   private float multilineLayoutTop(RectF var1, StaticLayout var2) {
      float var3 = Math.max(0.0F, (var1.height() - (float)var2.getHeight()) / 2.0F);
      return var1.top + var3 - this.verticalScrollPx;
   }

   private void invalidateMultilineLayout() {
      this.multilineLayoutDirty = true;
      this.multilineTextLayout = null;
      this.multilineHintLayout = null;
      this.multilineTextValue = null;
      this.multilineHintValue = null;
   }

   private void requireSelection(int var1, int var2) {
      if (var1 < 0 || var2 < 0 || var1 > this.editable.length() || var2 > this.editable.length()) {
         throw new IndexOutOfBoundsException("Selection " + var1 + ".." + var2 + " is outside text length " + this.editable.length() + ".");
      }
   }

   private static Typeface resolveTypeface(Context var0, int var1) {
      if (var1 == 0) {
         return Typeface.DEFAULT;
      } else {
         try {
            Typeface var2 = ResourcesCompat.getFont(var0, var1);
            if (var2 == null) {
               throw new IllegalArgumentException("Unable to load font resource ID: " + var1);
            } else {
               return var2;
            }
         } catch (RuntimeException var3) {
            throw new IllegalArgumentException("Unable to load font resource ID: " + var1, var3);
         }
      }
   }

   private void applyFontVariation() {
      if (VERSION.SDK_INT >= 26) {
         String var1 = this.fontVariation == null ? null : this.fontVariation.toSettings();
         this.textPaint.setFontVariationSettings(var1);
         this.hintPaint.setFontVariationSettings(var1);
      }
   }

   private static String requireId(String var0) {
      if (var0 != null && !var0.trim().isEmpty()) {
         return var0;
      } else {
         throw new IllegalArgumentException("TextField ID cannot be empty.");
      }
   }

   private static void requireBounds(RectF var0) {
      if (var0 == null || !Float.isFinite(var0.left) || !Float.isFinite(var0.top) || !Float.isFinite(var0.right) || !Float.isFinite(var0.bottom) || var0.width() <= 0.0F || var0.height() <= 0.0F) {
         throw new IllegalArgumentException("TextField bounds must be positive and finite.");
      }
   }

   private static int clamp(int var0, int var1, int var2) {
      return Math.max(var1, Math.min(var2, var0));
   }

   public static final class Builder implements ComponentFactory<TextField> {
      private final Context context;
      private final String id;
      private final Position position;
      private final float figmaWidth;
      private final float figmaHeight;
      private final RectF explicitBounds;
      private String hint;
      private CharSequence initialText;
      private int fontId;
      private Typeface typeface;
      private FontVariation fontVariation;
      private int inputType;
      private int imeOptions;
      private int maxLength;
      private int textColor;
      private int hintColor;
      private int cursorColor;
      private int selectionColor;
      private int selectionHandleColor;
      private int backgroundColor;
      private int focusedBackgroundColor;
      private int strokeColor;
      private int focusedStrokeColor;
      private float textSize;
      private boolean textSizeInPixels;
      private float horizontalPadding;
      private float verticalPadding;
      private boolean paddingInPixels;
      private float cornerRadius;
      private boolean cornerRadiusInPixels;
      private float strokeWidth;
      private boolean strokeWidthInPixels;
      private float cursorWidth;
      private boolean cursorWidthInPixels;
      private float cursorHeight = -1.0F;
      private boolean cursorHeightInPixels;
      private long cursorBlinkIntervalMs = DEFAULT_CURSOR_BLINK_INTERVAL_MS;
      private boolean enabled;
      private boolean password;
      private boolean selectionEnabled = true;
      private boolean cutEnabled = true;
      private boolean copyEnabled = true;
      private boolean pasteEnabled = true;
      private boolean horizontalCentered;
      private boolean verticalCentered;
      private OnTextChangedListener textChangedListener;
      private OnEditorActionListener editorActionListener;
      private OnFocusChangedListener focusChangedListener;
      private TextSelectionActionCallback selectionActionCallback;

      public Builder(Context var1, String var2, Position var3, float var4, float var5) {
         this.hint = "";
         this.initialText = "";
         this.typeface = Typeface.DEFAULT;
         this.inputType = 1;
         this.imeOptions = 6;
         this.maxLength = Integer.MAX_VALUE;
         this.textColor = -1;
         this.hintColor = -1996488705;
         this.cursorColor = -1;
         this.selectionColor = 1714664933;
         this.selectionHandleColor = 0xFF6633B5;
         this.backgroundColor = 855638016;
         this.focusedBackgroundColor = 1140850688;
         this.strokeColor = 1728053247;
         this.focusedStrokeColor = -1;
         this.textSize = -1.0F;
         this.horizontalPadding = -1.0F;
         this.verticalPadding = -1.0F;
         this.cornerRadius = -1.0F;
         this.strokeWidth = 2.0F;
         this.cursorWidth = 3.0F;
         this.enabled = true;
         this.context = (Context)Objects.requireNonNull(var1, "Context cannot be null.");
         this.id = var2;
         this.position = (Position)Objects.requireNonNull(var3, "Position cannot be null.");
         if (!(var4 <= 0.0F) && !(var5 <= 0.0F) && Float.isFinite(var4) && Float.isFinite(var5)) {
            this.figmaWidth = var4;
            this.figmaHeight = var5;
            this.explicitBounds = null;
         } else {
            throw new IllegalArgumentException("Figma width and height must be positive and finite.");
         }
      }

      public Builder(Context var1, String var2, Position var3, Size var4) {
         this(var1, var2, var3, ((Size)Objects.requireNonNull(var4, "Size cannot be null.")).getWidth(), var4.getHeight());
      }

      public Builder(Context var1, String var2, RectF var3) {
         this.hint = "";
         this.initialText = "";
         this.typeface = Typeface.DEFAULT;
         this.inputType = 1;
         this.imeOptions = 6;
         this.maxLength = Integer.MAX_VALUE;
         this.textColor = -1;
         this.hintColor = -1996488705;
         this.cursorColor = -1;
         this.selectionColor = 1714664933;
         this.selectionHandleColor = 0xFF6633B5;
         this.backgroundColor = 855638016;
         this.focusedBackgroundColor = 1140850688;
         this.strokeColor = 1728053247;
         this.focusedStrokeColor = -1;
         this.textSize = -1.0F;
         this.horizontalPadding = -1.0F;
         this.verticalPadding = -1.0F;
         this.cornerRadius = -1.0F;
         this.strokeWidth = 2.0F;
         this.cursorWidth = 3.0F;
         this.enabled = true;
         this.context = (Context)Objects.requireNonNull(var1, "Context cannot be null.");
         this.id = var2;
         this.explicitBounds = new RectF((RectF)Objects.requireNonNull(var3, "Bounds cannot be null."));
         this.position = null;
         this.figmaWidth = 0.0F;
         this.figmaHeight = 0.0F;
      }

      public Builder setFont(int var1) {
         if (var1 == 0) {
            throw new IllegalArgumentException("Font resource ID cannot be zero.");
         } else {
            this.fontId = var1;
            this.typeface = null;
            return this;
         }
      }

      public Builder setFont(Typeface var1) {
         this.typeface = (Typeface)Objects.requireNonNull(var1, "Typeface cannot be null.");
         this.fontId = 0;
         return this;
      }

      public Builder useDefaultFont() {
         this.typeface = Typeface.DEFAULT;
         this.fontId = 0;
         return this;
      }

      public Builder setFontVariations(FontVariation var1) {
         this.fontVariation = (FontVariation)Objects.requireNonNull(var1, "Font variation cannot be null.");
         return this;
      }

      public Builder clearFontVariations() {
         this.fontVariation = null;
         return this;
      }

      public Builder setHint(String var1) {
         this.hint = var1 == null ? "" : var1;
         return this;
      }

      public Builder setText(CharSequence var1) {
         this.initialText = (CharSequence)Objects.requireNonNull(var1, "Text cannot be null.");
         return this;
      }

      public Builder setMaxLength(int var1) {
         if (var1 <= 0) {
            throw new IllegalArgumentException("Maximum length must be greater than zero.");
         } else {
            this.maxLength = var1;
            return this;
         }
      }

      public Builder setInputType(int var1) {
         this.inputType = var1;
         return this;
      }

      public Builder setImeOptions(int var1) {
         this.imeOptions = var1;
         return this;
      }

      public Builder setTextColor(int var1) {
         this.textColor = var1;
         return this;
      }

      public Builder setHintColor(int var1) {
         this.hintColor = var1;
         return this;
      }

      public Builder setCursorColor(int var1) {
         this.cursorColor = var1;
         return this;
      }

      public Builder setSelectionColor(int var1) {
         this.selectionColor = var1;
         return this;
      }

      public Builder setSelectionHandleColor(int color) {
         this.selectionHandleColor = color;
         return this;
      }

      public Builder setBackgroundColor(int var1, int var2) {
         this.backgroundColor = var1;
         this.focusedBackgroundColor = var2;
         return this;
      }

      public Builder setStrokeColor(int var1, int var2) {
         this.strokeColor = var1;
         this.focusedStrokeColor = var2;
         return this;
      }

      public Builder setTextSize(float var1) {
         this.textSize = requireNonNegative(var1, "Text size");
         if (this.textSize == 0.0F) {
            throw new IllegalArgumentException("Text size must be greater than zero.");
         } else {
            this.textSizeInPixels = false;
            return this;
         }
      }

      public Builder setTextSizePx(float var1) {
         this.textSize = requireNonNegative(var1, "Text size");
         if (this.textSize == 0.0F) {
            throw new IllegalArgumentException("Text size must be greater than zero.");
         } else {
            this.textSizeInPixels = true;
            return this;
         }
      }

      public Builder setPadding(float var1, float var2) {
         this.horizontalPadding = requireNonNegative(var1, "Horizontal padding");
         this.verticalPadding = requireNonNegative(var2, "Vertical padding");
         this.paddingInPixels = false;
         return this;
      }

      public Builder setPaddingPx(float var1, float var2) {
         this.horizontalPadding = requireNonNegative(var1, "Horizontal padding");
         this.verticalPadding = requireNonNegative(var2, "Vertical padding");
         this.paddingInPixels = true;
         return this;
      }

      public Builder setCornerRadius(float var1) {
         this.cornerRadius = requireNonNegative(var1, "Corner radius");
         this.cornerRadiusInPixels = false;
         return this;
      }

      public Builder setCornerRadiusPx(float var1) {
         this.cornerRadius = requireNonNegative(var1, "Corner radius");
         this.cornerRadiusInPixels = true;
         return this;
      }

      public Builder setStrokeWidth(float var1) {
         this.strokeWidth = requireNonNegative(var1, "Stroke width");
         this.strokeWidthInPixels = false;
         return this;
      }

      public Builder setStrokeWidthPx(float var1) {
         this.strokeWidth = requireNonNegative(var1, "Stroke width");
         this.strokeWidthInPixels = true;
         return this;
      }

      public Builder setCursorWidth(float var1) {
         this.cursorWidth = requirePositive(var1, "Cursor width");
         this.cursorWidthInPixels = false;
         return this;
      }

      public Builder setCursorWidthPx(float var1) {
         this.cursorWidth = requirePositive(var1, "Cursor width");
         this.cursorWidthInPixels = true;
         return this;
      }

      public Builder setCursorHeight(float var1) {
         this.cursorHeight = requirePositive(var1, "Cursor height");
         this.cursorHeightInPixels = false;
         return this;
      }

      public Builder setCursorHeightPx(float var1) {
         this.cursorHeight = requirePositive(var1, "Cursor height");
         this.cursorHeightInPixels = true;
         return this;
      }

      public Builder setCursorBlinkIntervalMs(long var1) {
         if (var1 <= 0L) {
            throw new IllegalArgumentException("Cursor blink interval must be greater than 0.");
         }
         this.cursorBlinkIntervalMs = var1;
         return this;
      }

      public Builder setPassword(boolean var1) {
         this.password = var1;
         return this;
      }

      public Builder setSelectionEnabled(boolean value) { this.selectionEnabled = value; return this; }
      public Builder setCutEnabled(boolean value) { this.cutEnabled = value; return this; }
      public Builder setCopyEnabled(boolean value) { this.copyEnabled = value; return this; }
      public Builder setPasteEnabled(boolean value) { this.pasteEnabled = value; return this; }
      public Builder setSelectionActionCallback(TextSelectionActionCallback value) {
         this.selectionActionCallback = value;
         return this;
      }

      public Builder setEnabled(boolean var1) {
         this.enabled = var1;
         return this;
      }

      public Builder setHorizontalCenter(boolean var1) {
         this.horizontalCentered = var1;
         return this;
      }

      public Builder horizontalCenter(boolean var1) {
         return this.setHorizontalCenter(var1);
      }

      public Builder setVerticalCenter(boolean var1) {
         this.verticalCentered = var1;
         return this;
      }

      public Builder verticalCenter(boolean var1) {
         return this.setVerticalCenter(var1);
      }

      public Builder setOnTextChangedListener(OnTextChangedListener var1) {
         this.textChangedListener = var1;
         return this;
      }

      public Builder setOnEditorActionListener(OnEditorActionListener var1) {
         this.editorActionListener = var1;
         return this;
      }

      public Builder setOnFocusChangedListener(OnFocusChangedListener var1) {
         this.focusChangedListener = var1;
         return this;
      }

      public TextField build(View var1) {
         return new TextField(this, var1);
      }

      private RectF resolveBounds(View var1) {
         return this.explicitBounds != null ? new RectF(this.explicitBounds) : this.position.toRectF(var1, this.figmaWidth, this.figmaHeight);
      }

      private float resolveCursorWidth(View var1) {
         return this.resolveDimension(var1, this.cursorWidth, this.cursorWidthInPixels);
      }

      private float resolveCursorHeight(View var1) {
         return this.resolveDimension(var1, this.cursorHeight, this.cursorHeightInPixels);
      }

      private float resolveDimension(View var1, float var2, boolean var3) {
         if (var3) {
            return var2;
         } else {
            return this.position != null ? this.position.toRuntimePixels(var1, var2) : FigmaConfig.getDefault().toRuntime(var2, (float)var1.getWidth());
         }
      }

      private static float requirePositive(float var0, String var1) {
         if (!(var0 <= 0.0F) && Float.isFinite(var0)) {
            return var0;
         } else {
            throw new IllegalArgumentException(var1 + " must be positive and finite.");
         }
      }

      private static float requireNonNegative(float var0, String var1) {
         if (!(var0 < 0.0F) && Float.isFinite(var0)) {
            return var0;
         } else {
            throw new IllegalArgumentException(var1 + " must be non-negative and finite.");
         }
      }

   }

   public interface OnTextChangedListener {
      void onTextChanged(String var1, String var2);
   }

   public interface OnEditorActionListener {
      boolean onEditorAction(String var1, int var2);
   }

   public interface OnFocusChangedListener {
      void onFocusChanged(String var1, boolean var2);
   }

   private static final class ComposingMarker {
   }
}

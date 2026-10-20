package com.ogfa.nativeviews.text;

import android.content.Context;
import android.content.ContextWrapper;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.text.internal.TypefaceCache;
import com.ogfa.nativeviews.text.selection.TextSelectionController;
import com.ogfa.nativeviews.text.selection.TextSelectionActions;
import com.ogfa.nativeviews.text.selection.TextSelectionGeometry;
import com.ogfa.nativeviews.text.selection.TextSelectionHandles;
import com.ogfa.nativeviews.text.selection.TextSelectionMagnifier;

import java.util.Objects;

/**
 * A non-editable native Android text component rendered directly on a Canvas.
 *
 * <p>Instances are created through {@link Builder} and owned by a
 * a ZLayer. A region is supplied either as Figma-space
 * {@code Position + Size} or as an explicit runtime {@link RectF}.</p>
 */
public final class Text implements Component, TextSelectionGeometry {

    private static final long MAGNIFIER_TOOLBAR_DELAY_MS = TextSelectionMagnifier.TOOLBAR_DELAY_MS;

    public enum Alignment {
        START,
        CENTER,
        END
    }

    public enum VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM
    }

    public enum Overflow {
        CLIP,
        ELLIPSIZE_START,
        ELLIPSIZE_MIDDLE,
        ELLIPSIZE_END
    }

    private final Context context;
    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final RectF contentBounds = new RectF();
    private final TextPaint paint = new TextPaint(
            TextPaint.ANTI_ALIAS_FLAG
                    | TextPaint.SUBPIXEL_TEXT_FLAG
                    | TextPaint.DITHER_FLAG
    );
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix drawMatrix = new Matrix();
    private final TextSelectionHandles selectionHandles;

    private ComponentHost owner;
    private CharSequence value;
    private TextStyle style;
    private StaticLayout layout;
    private float dimensionScale;
    private float layoutTop;
    private float verticalScrollPx;
    private float horizontalScrollPx;
    private boolean visible = true;
    private boolean enabled;
    private boolean horizontalCentered;
    private boolean released;
    private boolean touchCaptured;
    private boolean clickCancelled;
    private boolean selectionEnabled;
    private boolean copyEnabled;
    private TextSelectionActionCallback selectionActionCallback;
    private final int selectionHandleColor;
    private int selectionStart;
    private int selectionEnd;
    private int selectionAnchorStart;
    private int selectionAnchorEnd;
    private int draggedHandle;
    private boolean selectingByGesture;
    private boolean doubleTapSelecting;
    private boolean longPressTriggered;
    private boolean touchInsideSelection;
    private boolean selectionHandleDragging;
    private boolean touchStayedInTapRegion;
    private boolean magnifierVisible;
    private boolean magnifierPending;
    private int handleDragLine;
    private int selectionAutoScrollDirection;
    private boolean horizontalAutoScroll;
    private boolean selectionAutoScrollPosted;
    private float selectionDragX;
    private float selectionDragY;
    private float touchDownX;
    private float touchDownY;
    private float touchLastX;
    private float touchLastY;
    private long lastTapTime;
    private float lastTapX;
    private float lastTapY;
    private final int touchSlop;
    private final int doubleTapSlop;
    private ActionMode selectionActionMode;
    private final Runnable longPressRunnable = this::selectAtLongPress;
    private final Runnable magnifierRunnable = () -> {
        magnifierPending = false;
        if (touchCaptured && selectionEnabled) {
            magnifierVisible = true;
            invalidate();
        }
    };
    private final Runnable toolbarRunnable = () -> {
        if (!touchCaptured && !magnifierVisible && selectionStart != selectionEnd) {
            showSelectionToolbar();
        }
    };
    private final Runnable selectionAutoScrollRunnable = this::runSelectionAutoScroll;
    private OnClickListener clickListener;

    private Text(Builder builder, View hostView) {
        context = builder.context.getApplicationContext();
        this.hostView = Objects.requireNonNull(
                hostView,
                "Host view cannot be null."
        );
        id = requireId(builder.id);
        value = Objects.requireNonNull(builder.value, "Text cannot be null.");
        style = builder.styleBuilder.build();
        enabled = builder.enabled;
        horizontalCentered = builder.horizontalCentered;
        clickListener = builder.clickListener;
        selectionEnabled = builder.selectionEnabled;
        copyEnabled = builder.copyEnabled;
        selectionActionCallback = builder.selectionActionCallback;
        selectionHandleColor = builder.selectionHandleColor;
        selectionPaint.setColor(builder.selectionColor);
        handlePaint.setColor(builder.selectionHandleColor);
        selectionHandles = new TextSelectionHandles(
                hostView.getContext(), builder.selectionHandleColor);
        ViewConfiguration configuration = ViewConfiguration.get(hostView.getContext());
        touchSlop = configuration.getScaledTouchSlop();
        doubleTapSlop = configuration.getScaledDoubleTapSlop();
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        rebuildLayout();
    }

    public String getId() {
        return id;
    }

    public CharSequence getText() {
        return value;
    }

    @Override public CharSequence selectionText() { return value; }

    @Override public int selectionOffsetAt(float x, float y) {
        return offsetAt(x, y);
    }

    @Override public void applySelection(int anchor, int extent) {
        setSelection(anchor, extent);
    }

    public RectF getBounds() {
        return new RectF(bounds);
    }

    public float getMeasuredTextWidth() {
        if (layout == null) {
            return 0f;
        }
        float width = 0f;
        for (int line = 0; line < layout.getLineCount(); line++) {
            width = Math.max(width, layout.getLineWidth(line));
        }
        return Math.min(width, contentBounds.width());
    }

    public float getMeasuredTextHeight() {
        return layout == null ? 0f : Math.min(
                layout.getHeight(),
                contentBounds.height()
        );
    }

    public boolean isVisible() {
        return visible;
    }

    public Text setVisible(boolean visible) {
        ensureActive();
        this.visible = visible;
        if (!visible) clearSelection();
        invalidate();
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Text setEnabled(boolean enabled) {
        ensureActive();
        this.enabled = enabled;
        if (!enabled) clearSelection();
        return this;
    }

    public boolean isClickable() {
        return clickListener != null;
    }

    public boolean isHorizontalCentered() {
        return horizontalCentered;
    }

    public Text setHorizontalCenter(boolean enabled) {
        ensureActive();
        if (horizontalCentered == enabled) {
            return this;
        }
        horizontalCentered = enabled;
        applyHorizontalPosition();
        rebuildLayout();
        return this;
    }

    /**
     * Fluent alias for {@link #setHorizontalCenter(boolean)}.
     */
    public Text horizontalCenter(boolean enabled) {
        return setHorizontalCenter(enabled);
    }

    public Text setOnClickListener(OnClickListener listener) {
        ensureActive();
        clickListener = listener;
        return this;
    }

    public Text setText(CharSequence text) {
        ensureActive();
        value = Objects.requireNonNull(text, "Text cannot be null.");
        clearSelection();
        rebuildLayout();
        return this;
    }

    public boolean isSelectionEnabled() { return selectionEnabled; }

    public boolean isCopyEnabled() { return copyEnabled; }

    public Text setCopyEnabled(boolean enabled) {
        ensureActive();
        copyEnabled = enabled;
        if (selectionActionMode != null) selectionActionMode.invalidate();
        return this;
    }

    public TextSelectionActionCallback getSelectionActionCallback() {
        return selectionActionCallback;
    }

    public Text setSelectionActionCallback(TextSelectionActionCallback callback) {
        ensureActive();
        selectionActionCallback = callback;
        if (selectionActionMode != null) selectionActionMode.invalidate();
        return this;
    }

    public Text setSelectionEnabled(boolean enabled) {
        ensureActive();
        if (selectionEnabled == enabled) return this;
        selectionEnabled = enabled;
        if (!enabled) clearSelection();
        rebuildLayout();
        return this;
    }

    public int getSelectionStart() { return Math.min(selectionStart, selectionEnd); }
    public int getSelectionEnd() { return Math.max(selectionStart, selectionEnd); }
    public int getSelectionAnchor() { return selectionStart; }
    public int getSelectionExtent() { return selectionEnd; }

    public boolean hasSelection() {
        return selectionStart != selectionEnd;
    }

    /** Hit test in host View coordinates, including layer and parent transforms. */
    public boolean containsSelectionTouchOnHost(float x, float y) {
        if (!hasSelection() || !visible || released) return false;
        Matrix inverse = new Matrix();
        if (!drawMatrix.invert(inverse)) return false;
        float[] point = {x, y};
        inverse.mapPoints(point);
        return bounds.contains(point[0], point[1])
                || hitHandle(point[0], point[1]) != 0;
    }

    public Text setSelection(int start, int end) {
        ensureActive();
        if (!selectionEnabled) return this;
        if (start < 0 || end < 0 || start > value.length() || end > value.length()) {
            throw new IndexOutOfBoundsException("Selection " + start + ".." + end
                    + " is outside text length " + value.length() + ".");
        }
        selectionStart = start;
        selectionEnd = end;
        if (selectionStart == selectionEnd) hideSelectionToolbar();
        else if (selectionActionMode != null) selectionActionMode.invalidateContentRect();
        invalidate();
        return this;
    }

    public Text setSelection(int offset) { return setSelection(offset, offset); }

    public Text clearSelection() {
        hostView.removeCallbacks(longPressRunnable);
        hideMagnifier();
        stopSelectionAutoScroll();
        if (selectionActionMode != null) selectionActionMode.finish();
        selectionStart = selectionEnd = 0;
        invalidate();
        return this;
    }

    public Text setStyle(TextStyle style) {
        ensureActive();
        this.style = Objects.requireNonNull(style, "TextStyle cannot be null.");
        rebuildLayout();
        return this;
    }

    public Text setTextColor(int color) {
        return updateStyle(new TextStyle.Builder(style).setTextColor(color));
    }

    public Text setAlpha(float alpha) {
        return updateStyle(new TextStyle.Builder(style).setAlpha(alpha));
    }

    public Text setTextSize(float size) {
        return updateStyle(new TextStyle.Builder(style).setTextSize(size));
    }

    public Text setTextSizePx(float pixels) {
        return updateStyle(
                new TextStyle.Builder(style).setTextSizePx(pixels)
        );
    }

    public Text setLetterSpacing(float spacing) {
        return updateStyle(
                new TextStyle.Builder(style).setLetterSpacing(spacing)
        );
    }

    public Text setLetterSpacingPx(float pixels) {
        return updateStyle(
                new TextStyle.Builder(style).setLetterSpacingPx(pixels)
        );
    }

    public Text setLetterSpacingPercent(float percent) {
        return updateStyle(
                new TextStyle.Builder(style).setLetterSpacingPercent(percent)
        );
    }

    public Text setLineSpacing(float spacing) {
        return updateStyle(
                new TextStyle.Builder(style).setLineSpacing(spacing)
        );
    }

    public Text setLineSpacingPx(float pixels) {
        return updateStyle(
                new TextStyle.Builder(style).setLineSpacingPx(pixels)
        );
    }

    public Text setLineHeightPercent(float percent) {
        return updateStyle(
                new TextStyle.Builder(style).setLineHeightPercent(percent)
        );
    }

    public Text setPadding(float horizontal, float vertical) {
        return updateStyle(
                new TextStyle.Builder(style).setPadding(horizontal, vertical)
        );
    }

    public Text setPaddingPx(float horizontal, float vertical) {
        return updateStyle(
                new TextStyle.Builder(style).setPaddingPx(
                        horizontal,
                        vertical
                )
        );
    }

    public Text setShadow(float radius, float dx, float dy, int color) {
        return updateStyle(
                new TextStyle.Builder(style).setShadow(radius, dx, dy, color)
        );
    }

    public Text setShadowPx(float radius, float dx, float dy, int color) {
        return updateStyle(
                new TextStyle.Builder(style).setShadowPx(
                        radius,
                        dx,
                        dy,
                        color
                )
        );
    }

    public Text clearShadow() {
        return updateStyle(new TextStyle.Builder(style).clearShadow());
    }

    public Text setFont(int fontResourceId) {
        return updateStyle(
                new TextStyle.Builder(style).setFont(fontResourceId)
        );
    }

    public Text setFontAsset(String assetPath) {
        return updateStyle(
                new TextStyle.Builder(style).setFontAsset(assetPath)
        );
    }

    public Text setFont(Typeface typeface) {
        return updateStyle(new TextStyle.Builder(style).setFont(typeface));
    }

    public Text useDefaultFont() {
        return updateStyle(new TextStyle.Builder(style).useDefaultFont());
    }

    public FontVariation getFontVariation() {
        return style.fontVariation;
    }

    public Text setFontVariations(FontVariation variation) {
        return updateStyle(
                new TextStyle.Builder(style).setFontVariations(variation)
        );
    }

    public Text clearFontVariations() {
        return updateStyle(
                new TextStyle.Builder(style).clearFontVariations()
        );
    }

    public Text setAlignment(Alignment alignment) {
        return updateStyle(
                new TextStyle.Builder(style).setAlignment(alignment)
        );
    }

    public Text setVerticalAlignment(VerticalAlignment alignment) {
        return updateStyle(
                new TextStyle.Builder(style).setVerticalAlignment(alignment)
        );
    }

    public Text setMaxLines(int maxLines) {
        return updateStyle(
                new TextStyle.Builder(style).setMaxLines(maxLines)
        );
    }

    public Text setOverflow(Overflow overflow) {
        return updateStyle(
                new TextStyle.Builder(style).setOverflow(overflow)
        );
    }

    public Text setWrapEnabled(boolean enabled) {
        return updateStyle(
                new TextStyle.Builder(style).setWrapEnabled(enabled)
        );
    }

    public Text setRegion(Position position, Size size) {
        ensureActive();
        resolveRegion(
                Objects.requireNonNull(position, "Position cannot be null."),
                Objects.requireNonNull(size, "Size cannot be null."),
                null
        );
        rebuildLayout();
        return this;
    }

    public Text setRegion(RectF bounds) {
        ensureActive();
        resolveRegion(
                null,
                null,
                new RectF(Objects.requireNonNull(
                        bounds,
                        "Bounds cannot be null."
                ))
        );
        rebuildLayout();
        return this;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stopSelectionAutoScroll();
                touchCaptured = acceptsTouch(event.getX(), event.getY());
                clickCancelled = false;
                selectingByGesture = false;
                doubleTapSelecting = false;
                longPressTriggered = false;
                selectionHandleDragging = false;
                touchStayedInTapRegion = true;
                hostView.removeCallbacks(toolbarRunnable);
                draggedHandle = selectionEnabled ? hitHandle(event.getX(), event.getY()) : 0;
                if (touchCaptured && selectionEnabled) {
                    touchDownX = event.getX();
                    touchDownY = event.getY();
                    touchLastX = touchDownX;
                    touchLastY = touchDownY;
                    touchInsideSelection = draggedHandle == 0 && isTouchInsideSelection(
                            event.getX(), event.getY());
                    if (draggedHandle != 0) {
                        selectionAnchorStart = Math.min(selectionStart, selectionEnd);
                        selectionAnchorEnd = Math.max(selectionStart, selectionEnd);
                        handleDragLine = layout.getLineForOffset(
                                draggedHandle == 1 ? selectionAnchorStart : selectionAnchorEnd);
                        showMagnifier();
                    } else if (!touchInsideSelection && isSecondTap(event)
                            && selectWordAt(event.getX(), event.getY())) {
                        selectingByGesture = true;
                        doubleTapSelecting = true;
                        lastTapTime = 0L;
                        showMagnifier();
                    } else if (!touchInsideSelection) {
                        hostView.postDelayed(longPressRunnable,
                                ViewConfiguration.getLongPressTimeout());
                    }
                }
                return touchCaptured;
            case MotionEvent.ACTION_MOVE:
                if (!touchCaptured) return false;
                if (selectionEnabled && touchStayedInTapRegion
                        && !TextSelectionController.isWithinTouchSlop(
                                event.getX(), event.getY(), touchDownX, touchDownY,
                                touchSlop)) {
                    touchStayedInTapRegion = false;
                    hostView.removeCallbacks(longPressRunnable);
                }
                if (selectionEnabled && (draggedHandle != 0 || selectingByGesture)) {
                    if (draggedHandle != 0 && !selectionHandleDragging) {
                        if (touchStayedInTapRegion) return true;
                        selectionHandleDragging = true;
                        hideSelectionToolbar();
                    }
                    updateSelectionDrag(event.getX(), event.getY());
                    updateSelectionAutoScroll(event.getX(), event.getY());
                    return true;
                }
                if (selectionEnabled && touchInsideSelection
                        && !touchStayedInTapRegion && canScrollSelection()) {
                    clickCancelled = true;
                    scrollSelectionBy(touchLastX - event.getX(),
                            touchLastY - event.getY());
                    touchLastX = event.getX();
                    touchLastY = event.getY();
                    return true;
                }
                touchLastX = event.getX();
                touchLastY = event.getY();
                if (!touchStayedInTapRegion) clickCancelled = true;
                if (!acceptsTouch(event.getX(), event.getY())) clickCancelled = true;
                return true;
            case MotionEvent.ACTION_UP:
                if (!touchCaptured) return false;
                boolean magnifierWasActive = magnifierVisible || magnifierPending;
                hideMagnifier();
                hostView.removeCallbacks(longPressRunnable);
                boolean selected = selectingByGesture || longPressTriggered || draggedHandle != 0;
                boolean click = !selected && !clickCancelled
                        && acceptsTouch(event.getX(), event.getY());
                touchCaptured = false;
                if (selected) {
                    showSelectionToolbarAfterMagnifier(magnifierWasActive);
                } else if (selectionEnabled) {
                    if (touchInsideSelection && touchStayedInTapRegion) {
                        setSelection(offsetAt(event.getX(), event.getY()),
                                offsetAt(event.getX(), event.getY()));
                        hideSelectionToolbar();
                        click = false;
                    } else if (touchStayedInTapRegion && !clickCancelled) {
                        lastTapTime = event.getEventTime();
                        lastTapX = touchDownX;
                        lastTapY = touchDownY;
                        if (selectionStart != selectionEnd) clearSelection();
                    } else {
                        lastTapTime = 0L;
                    }
                }
                draggedHandle = 0;
                doubleTapSelecting = false;
                selectingByGesture = false;
                touchInsideSelection = false;
                selectionHandleDragging = false;
                stopSelectionAutoScroll();
                if (click) performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                boolean handled = touchCaptured;
                boolean restoreToolbar = selectionHandleDragging;
                boolean magnifierWasPending = magnifierVisible || magnifierPending;
                hideMagnifier();
                hostView.removeCallbacks(longPressRunnable);
                touchCaptured = false;
                clickCancelled = true;
                draggedHandle = 0;
                selectingByGesture = false;
                doubleTapSelecting = false;
                touchInsideSelection = false;
                selectionHandleDragging = false;
                lastTapTime = 0L;
                stopSelectionAutoScroll();
                if (restoreToolbar) showSelectionToolbarAfterMagnifier(magnifierWasPending);
                return handled;
            default:
                return touchCaptured;
        }
    }

    public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || layout == null) {
            return;
        }
        canvas.getMatrix(drawMatrix);
        drawTextContent(canvas, true);
    }

    private void drawTextContent(Canvas canvas, boolean drawHandles) {
        int saveCount = canvas.save();
        canvas.clipRect(selectionEnabled ? contentBounds : bounds);
        canvas.translate(contentBounds.left - horizontalScrollPx,
                layoutTop - verticalScrollPx);
        if (selectionEnabled && selectionStart != selectionEnd) {
            Path selectionPath = new Path();
            layout.getSelectionPath(Math.min(selectionStart, selectionEnd),
                    Math.max(selectionStart, selectionEnd), selectionPath);
            canvas.drawPath(selectionPath, selectionPaint);
        }
        layout.draw(canvas);
        canvas.restoreToCount(saveCount);
        if (drawHandles && selectionEnabled && selectionStart != selectionEnd) {
            drawSelectionHandles(canvas);
        }
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        clearSelection();
        owner = null;
        layout = null;
    }

    @Override
    public void attach(ComponentHost owner) {
        ensureActive();
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException(
                    "Text already belongs to another component host."
            );
        }
        this.owner = owner;
        if (horizontalCentered) {
            applyHorizontalPosition();
            rebuildLayout();
        }
    }

    private boolean acceptsTouch(float x, float y) {
        return !released
                && visible
                && enabled
                && (clickListener != null || selectionEnabled)
                && (bounds.contains(x, y) || selectionEnabled && hitHandle(x, y) != 0);
    }

    private void performClick() {
        if (!released
                && visible
                && enabled
                && clickListener != null) {
            clickListener.onClick(id);
        }
    }

    private void selectAtLongPress() {
        if (!touchCaptured || !selectionEnabled || released) return;
        if (selectWordAt(touchDownX, touchDownY)) {
            longPressTriggered = true;
            selectingByGesture = true;
            clickCancelled = true;
            lastTapTime = 0L;
            showMagnifier();
            hostView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            updateSelectionAutoScroll(touchDownX, touchDownY);
        }
    }

    private boolean selectWordAt(float x, float y) {
        if (value.length() == 0 || !contentBounds.contains(x, y)) return false;
        int[] word = TextSelectionController.wordAt(this, x, y);
        if (word[0] == word[1]) return false;
        selectionAnchorStart = word[0];
        selectionAnchorEnd = word[1];
        setSelection(word[0], word[1]);
        return true;
    }

    private int offsetAt(float x, float y) {
        if (layout == null) return 0;
        int line = layout.getLineForVertical(Math.max(0,
                Math.min(layout.getHeight() - 1,
                        (int) (y - layoutTop + verticalScrollPx))));
        int offset = layout.getOffsetForHorizontal(line,
                x - contentBounds.left + horizontalScrollPx);
        return Math.max(0, Math.min(value.length(), offset));
    }

    private void updateSelectionDrag(float x, float y) {
        int offset = draggedHandle != 0 ? handleDragOffsetAt(x, y) : offsetAt(x, y);
        if (doubleTapSelecting) {
            TextSelectionController.extendDoubleTap(this,
                    selectionAnchorStart, selectionAnchorEnd, x, y);
            return;
        }
        if (draggedHandle == 1) {
            TextSelectionController.dragStartHandle(this, offset, selectionAnchorEnd);
        } else if (draggedHandle == 2) {
            TextSelectionController.dragEndHandle(this, selectionAnchorStart, offset);
        } else {
            TextSelectionController.extendLongPress(this,
                    selectionAnchorStart, selectionAnchorEnd, x, y);
        }
    }

    private int handleDragOffsetAt(float x, float y) {
        if (layout == null) return 0;
        float vertical = y - layoutTop + verticalScrollPx;
        int line = Math.max(0, Math.min(handleDragLine, layout.getLineCount() - 1));
        int lastLine = layout.getLineCount() - 1;
        float lastLineHeight = layout.getLineBottom(lastLine) - layout.getLineTop(lastLine);
        while (line < lastLine) {
            int thresholdLine = line + 2;
            float threshold = layout.getLineBottom(Math.min(thresholdLine, lastLine))
                    + Math.max(0, thresholdLine - lastLine) * lastLineHeight;
            if (vertical < threshold) break;
            line++;
        }
        while (line > 0 && vertical < layout.getLineTop(line)) line--;
        handleDragLine = line;
        return Math.max(0, Math.min(value.length(),
                layout.getOffsetForHorizontal(line,
                        Math.max(0f, x - contentBounds.left + horizontalScrollPx))));
    }

    private boolean isTouchInsideSelection(float x, float y) {
        if (selectionStart == selectionEnd || !contentBounds.contains(x, y)) return false;
        int offset = offsetAt(x, y);
        return offset >= Math.min(selectionStart, selectionEnd)
                && offset <= Math.max(selectionStart, selectionEnd);
    }

    private boolean canScrollSelection() {
        return layout != null && (style.wrapEnabled
                ? layout.getHeight() > contentBounds.height() + 0.5f
                : maxHorizontalScroll() > 0.5f);
    }

    private void scrollSelectionBy(float dx, float dy) {
        if (style.wrapEnabled) {
            float maximum = Math.max(0f, layout.getHeight() - contentBounds.height());
            verticalScrollPx = Math.max(0f, Math.min(maximum, verticalScrollPx + dy));
        } else {
            horizontalScrollPx = Math.max(0f,
                    Math.min(maxHorizontalScroll(), horizontalScrollPx + dx));
        }
        invalidate();
    }

    private void updateSelectionAutoScroll(float x, float y) {
        selectionDragX = x;
        selectionDragY = y;
        horizontalAutoScroll = !style.wrapEnabled;
        float maxScroll = horizontalAutoScroll
                ? maxHorizontalScroll()
                : Math.max(0f, layout.getHeight() - contentBounds.height());
        if (maxScroll < 0.5f) {
            stopSelectionAutoScroll();
            return;
        }
        float edge = Math.max(16f, paint.getTextSize() * 0.8f);
        float coordinate = horizontalAutoScroll ? x : y;
        float leading = horizontalAutoScroll ? contentBounds.left : contentBounds.top;
        float trailing = horizontalAutoScroll ? contentBounds.right : contentBounds.bottom;
        float scroll = horizontalAutoScroll ? horizontalScrollPx : verticalScrollPx;
        if (coordinate <= leading + edge && scroll > 0f) {
            selectionAutoScrollDirection = -1;
        } else if (coordinate >= trailing - edge && scroll < maxScroll - 0.5f) {
            selectionAutoScrollDirection = 1;
        } else {
            stopSelectionAutoScroll();
            return;
        }
        if (!selectionAutoScrollPosted) {
            selectionAutoScrollPosted = true;
            hostView.postOnAnimation(selectionAutoScrollRunnable);
        }
    }

    private void runSelectionAutoScroll() {
        selectionAutoScrollPosted = false;
        if (!touchCaptured || selectionAutoScrollDirection == 0
                || !(selectingByGesture || draggedHandle != 0)) return;
        float maxScroll = horizontalAutoScroll
                ? maxHorizontalScroll()
                : Math.max(0f, layout.getHeight() - contentBounds.height());
        float before = horizontalAutoScroll ? horizontalScrollPx : verticalScrollPx;
        float after = Math.max(0f, Math.min(maxScroll, before
                + selectionAutoScrollDirection * Math.max(4f, paint.getTextSize() * 0.32f)));
        if (horizontalAutoScroll) horizontalScrollPx = after;
        else verticalScrollPx = after;
        if (Math.abs(before - after) < 0.5f) {
            stopSelectionAutoScroll();
        } else {
            updateSelectionDrag(selectionDragX, selectionDragY);
            invalidate();
            updateSelectionAutoScroll(selectionDragX, selectionDragY);
        }
    }

    private void stopSelectionAutoScroll() {
        selectionAutoScrollDirection = 0;
        if (selectionAutoScrollPosted) {
            hostView.removeCallbacks(selectionAutoScrollRunnable);
            selectionAutoScrollPosted = false;
        }
    }

    private float maxHorizontalScroll() {
        return layout == null || style.wrapEnabled ? 0f
                : Math.max(0f, paint.measureText(value, 0, value.length())
                        - contentBounds.width());
    }

    private boolean isSecondTap(MotionEvent event) {
        return TextSelectionController.isSecondTap(event, lastTapTime,
                lastTapX, lastTapY, doubleTapSlop);
    }

    private float handleRadius() {
        return Math.max(8f, paint.getTextSize() * 0.32f);
    }

    private int hitHandle(float x, float y) {
        if (layout == null || selectionStart == selectionEnd) return 0;
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        if (!handleTouchBounds().contains(x, y)) return 0;
        if (isHandleHit(x, y, selectionEndpoint(start), 1)) return 1;
        if (isHandleHit(x, y, selectionEndpoint(end), 2)) return 2;
        return 0;
    }

    private void drawSelectionHandles(Canvas canvas) {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        drawSelectionHandle(canvas, selectionEndpoint(start), 1);
        drawSelectionHandle(canvas, selectionEndpoint(end), 2);
    }

    private PointF selectionEndpoint(int offset) {
        if (layout == null) return null;
        int safe = Math.max(0, Math.min(offset, layout.getText().length()));
        int line = layout.getLineForOffset(safe);
        float top = layoutTop - verticalScrollPx + layout.getLineTop(line);
        float bottom = layoutTop - verticalScrollPx + layout.getLineBottom(line);
        if (bottom <= contentBounds.top || top >= contentBounds.bottom) return null;
        return new PointF(contentBounds.left - horizontalScrollPx
                + layout.getPrimaryHorizontal(safe), bottom);
    }

    private RectF handleTouchBounds() {
        return selectionHandles.touchBounds(bounds, handleRadius(), touchSlop);
    }

    private boolean isHandleHit(float x, float y, PointF endpoint, int type) {
        return selectionHandles.hit(x, y, endpoint, type, handleRadius(), touchSlop);
    }

    private void drawSelectionHandle(Canvas canvas, PointF endpoint, int type) {
        selectionHandles.draw(canvas, endpoint, type, bounds, handleRadius(),
                selectionHandleColor, handlePaint);
    }

    /** Drawn by the group above all layers, matching TextField's selection lens. */
    public void drawMagnifier(Canvas canvas) {
        if (!magnifierVisible || !visible || released || layout == null) return;
        if (hostView.getWidth() <= 0 || hostView.getHeight() <= 0) return;
        TextSelectionMagnifier.draw(canvas, hostView, drawMatrix,
                magnifierSourcePoint(), magnifierLineBounds(),
                magnifierBackgroundColor(), paint.getTextSize(), paint.getColor(),
                selectionStart != selectionEnd,
                content -> drawTextContent(content, false));
    }

    private PointF magnifierSourcePoint() {
        int offset = draggedHandle == 1
                ? Math.min(selectionStart, selectionEnd)
                : Math.max(selectionStart, selectionEnd);
        int line = layout.getLineForOffset(offset);
        return new PointF(contentBounds.left - horizontalScrollPx
                + layout.getPrimaryHorizontal(offset),
                layoutTop - verticalScrollPx
                        + (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f);
    }

    private RectF magnifierLineBounds() {
        int offset = draggedHandle == 1
                ? Math.min(selectionStart, selectionEnd)
                : Math.max(selectionStart, selectionEnd);
        int line = layout.getLineForOffset(offset);
        return new RectF(contentBounds.left,
                Math.max(contentBounds.top,
                        layoutTop - verticalScrollPx + layout.getLineTop(line)),
                contentBounds.right,
                Math.min(contentBounds.bottom,
                        layoutTop - verticalScrollPx + layout.getLineBottom(line)));
    }

    private int magnifierBackgroundColor() {
        int foreground = paint.getColor();
        float brightness = Color.red(foreground) * 0.2126f
                + Color.green(foreground) * 0.7152f
                + Color.blue(foreground) * 0.0722f;
        return brightness < 140f ? 0xfff4f4f4 : 0xff353535;
    }

    private void showMagnifier() {
        hostView.removeCallbacks(toolbarRunnable);
        hostView.removeCallbacks(magnifierRunnable);
        hideSelectionToolbar();
        if (magnifierVisible) return;
        magnifierPending = true;
        hostView.postDelayed(magnifierRunnable, MAGNIFIER_TOOLBAR_DELAY_MS);
    }

    private void hideMagnifier() {
        hostView.removeCallbacks(toolbarRunnable);
        hostView.removeCallbacks(magnifierRunnable);
        magnifierPending = false;
        if (magnifierVisible) {
            magnifierVisible = false;
            invalidate();
        }
    }

    private void showSelectionToolbarAfterMagnifier(boolean wasActive) {
        if (wasActive) {
            hostView.postDelayed(toolbarRunnable, MAGNIFIER_TOOLBAR_DELAY_MS);
        } else {
            showSelectionToolbar();
        }
    }

    private void hideSelectionToolbar() {
        if (selectionActionMode != null) selectionActionMode.finish();
    }

    private void showSelectionToolbar() {
        if (selectionStart == selectionEnd || released || !visible || !enabled) return;
        hideSelectionToolbar();
        ActionMode.Callback2 callback = new ActionMode.Callback2() {
            @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                TextSelectionActions.addCommonActions(menu, context, 0, 1, 2, "Share");
                menu.findItem(android.R.id.copy).setVisible(copyEnabled);
                menu.findItem(android.R.id.selectAll).setVisible(
                        getSelectionStart() != 0 || getSelectionEnd() != value.length());
                if (selectionActionCallback != null) {
                    selectionActionCallback.onCreateActionMode(Text.this, mode, menu);
                }
                TextSelectionActions.updateSecondaryActionPlacement(menu);
                return menu.hasVisibleItems();
            }
            @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                MenuItem copy = menu.findItem(android.R.id.copy);
                if (copy != null) copy.setVisible(copyEnabled && hasSelection());
                MenuItem selectAll = menu.findItem(android.R.id.selectAll);
                if (selectAll != null) selectAll.setVisible(
                        getSelectionStart() != 0 || getSelectionEnd() != value.length());
                if (selectionActionCallback != null) {
                    selectionActionCallback.onPrepareActionMode(Text.this, mode, menu);
                }
                TextSelectionActions.updateSecondaryActionPlacement(menu);
                return true;
            }
            @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (selectionActionCallback != null
                        && selectionActionCallback.onActionItemClicked(
                                Text.this, mode, item)) return true;
                if (item.getItemId() == android.R.id.selectAll) {
                    setSelection(0, value.length());
                    mode.invalidate();
                    mode.invalidateContentRect();
                    return true;
                }
                String selected = value.subSequence(Math.min(selectionStart, selectionEnd),
                        Math.max(selectionStart, selectionEnd)).toString();
                if (item.getItemId() == android.R.id.copy) {
                    if (!copyEnabled || !hasSelection()) return false;
                    TextSelectionActions.copy(context, null, selected);
                } else if (item.getItemId() == android.R.id.shareText) {
                    if (!TextSelectionActions.share(hostView.getContext(), selected)) return false;
                } else return false;
                mode.finish();
                return true;
            }
            @Override public void onDestroyActionMode(ActionMode mode) {
                if (selectionActionMode == mode) selectionActionMode = null;
            }
            @Override public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
                int start = Math.min(selectionStart, selectionEnd);
                int line = layout.getLineForOffset(start);
                float x = contentBounds.left - horizontalScrollPx
                        + layout.getPrimaryHorizontal(start);
                float top = layoutTop - verticalScrollPx + layout.getLineTop(line);
                float bottom = layoutTop - verticalScrollPx + layout.getLineBottom(line);
                if (bottom <= contentBounds.top || top >= contentBounds.bottom) {
                    top = bounds.top;
                    bottom = top + 1f;
                }
                RectF anchor = new RectF(x - 2f, top, x + 2f, bottom);
                drawMatrix.mapRect(anchor);
                anchor.left = Math.max(0f, Math.min(anchor.left, view.getWidth() - 1f));
                anchor.right = Math.max(anchor.left + 1f,
                        Math.min(anchor.right, view.getWidth()));
                anchor.top = Math.max(0f, Math.min(anchor.top, view.getHeight() - 1f));
                anchor.bottom = Math.max(anchor.top + 1f,
                        Math.min(anchor.bottom, view.getHeight()));
                outRect.set(Math.round(anchor.left), Math.round(anchor.top),
                        Math.round(anchor.right), Math.round(anchor.bottom));
            }
        };
        selectionActionMode = hostView.startActionMode(callback, ActionMode.TYPE_FLOATING);
        if (selectionActionMode == null) {
            Activity activity = findActivity(hostView.getContext());
            if (activity != null) {
                selectionActionMode = activity.startActionMode(
                        callback, ActionMode.TYPE_FLOATING);
            }
        }
        if (selectionActionMode != null) selectionActionMode.invalidateContentRect();
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            Context base = ((ContextWrapper) current).getBaseContext();
            if (base == current) break;
            current = base;
        }
        return current instanceof Activity ? (Activity) current : null;
    }

    private Text updateStyle(TextStyle.Builder builder) {
        ensureActive();
        style = builder.build();
        rebuildLayout();
        return this;
    }

    private void resolveRegion(
            Position position,
            Size size,
            RectF explicitBounds
    ) {
        if (explicitBounds != null) {
            requireBounds(explicitBounds);
            baseBounds.set(explicitBounds);
            dimensionScale = FigmaConfig.getDefault().getScale(
                    hostView.getWidth()
            );
            applyHorizontalPosition();
            return;
        }
        Objects.requireNonNull(position, "Position cannot be null.");
        Objects.requireNonNull(size, "Size cannot be null.");
        RectF resolved = position.toRectF(hostView, size);
        requireBounds(resolved);
        baseBounds.set(resolved);
        dimensionScale = position.getScale(hostView);
        applyHorizontalPosition();
    }

    private void applyHorizontalPosition() {
        bounds.set(baseBounds);
        if (!horizontalCentered || baseBounds.isEmpty()) {
            return;
        }
        RectF parentBounds = owner == null
                ? new RectF(0f, 0f, hostView.getWidth(), hostView.getHeight())
                : owner.getComponentBounds();
        float width = baseBounds.width();
        bounds.left = parentBounds.centerX() - width / 2f;
        bounds.right = bounds.left + width;
    }

    private void rebuildLayout() {
        ensureActive();
        applyPaint();

        float horizontalPadding = scaleDimension(
                style.horizontalPadding,
                style.paddingUnit
        );
        float verticalPadding = scaleDimension(
                style.verticalPadding,
                style.paddingUnit
        );
        contentBounds.set(bounds);
        contentBounds.inset(horizontalPadding, verticalPadding);
        if (contentBounds.width() <= 0f || contentBounds.height() <= 0f) {
            throw new IllegalArgumentException(
                    "Text padding leaves no drawable content region."
            );
        }

        int availableWidth = Math.max(1, (int) Math.floor(contentBounds.width()));
        if (selectionEnabled && !style.wrapEnabled) {
            availableWidth = Math.max(availableWidth,
                    (int) Math.ceil(paint.measureText(value, 0, value.length())
                            + paint.getTextSize()));
        }
        int maxLines = selectionEnabled ? Integer.MAX_VALUE
                : style.wrapEnabled ? style.maxLines : 1;
        if ((style.overflow == Overflow.ELLIPSIZE_START
                || style.overflow == Overflow.ELLIPSIZE_MIDDLE)
                && maxLines != 1) {
            throw new IllegalArgumentException(
                    "Start and middle ellipsizing require maxLines = 1."
            );
        }

        StaticLayout.Builder layoutBuilder = StaticLayout.Builder.obtain(
                        value,
                        0,
                        value.length(),
                        paint,
                        availableWidth
                )
                .setAlignment(toLayoutAlignment(style.alignment))
                .setIncludePad(false)
                .setLineSpacing(
                        scaleDimension(
                                style.lineSpacing,
                                style.lineSpacingUnit
                        ),
                        style.lineSpacingMultiplier
                )
                .setMaxLines(maxLines);

        TextUtils.TruncateAt truncateAt = selectionEnabled
                ? null : toTruncateAt(style.overflow);
        if (truncateAt != null) {
            layoutBuilder
                    .setEllipsize(truncateAt)
                    .setEllipsizedWidth(availableWidth);
        }

        layout = layoutBuilder.build();
        horizontalScrollPx = Math.max(0f,
                Math.min(horizontalScrollPx, maxHorizontalScroll()));
        if (selectionEnabled && layout.getHeight() > contentBounds.height()) {
            layoutTop = contentBounds.top;
            verticalScrollPx = Math.max(0f, Math.min(verticalScrollPx,
                    layout.getHeight() - contentBounds.height()));
            invalidate();
            return;
        }
        verticalScrollPx = 0f;
        switch (style.verticalAlignment) {
            case CENTER:
                layoutTop = contentBounds.top
                        + (contentBounds.height() - layout.getHeight()) / 2f;
                break;
            case BOTTOM:
                layoutTop = contentBounds.bottom - layout.getHeight();
                break;
            case TOP:
            default:
                layoutTop = contentBounds.top;
                break;
        }
        invalidate();
    }

    private void applyPaint() {
        // A centered Text is rebuilt after it is attached to its owner. Clear
        // the variation from the previous layout before restoring the base
        // typeface; otherwise some Android versions derive the next variation
        // from the already-varied face and fall back to its default weight.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            paint.setFontVariationSettings(null);
        }
        paint.setTypeface(resolveTypeface());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            paint.setFontVariationSettings(
                    style.fontVariation == null
                            ? null
                            : style.fontVariation.toSettings()
            );
        }
        float textSize = style.textSize > 0f
                ? scaleTextSize(style)
                : bounds.height() * 0.5f;
        paint.setTextSize(textSize);
        paint.setColor(style.textColor);
        paint.setAlpha(Math.round(
                Color.alpha(style.textColor) * style.alpha
        ));
        if (style.letterSpacingUnit == TextStyle.DimensionUnit.PERCENT) {
            paint.setLetterSpacing(style.letterSpacing / 100f);
        } else {
            float letterSpacingPx = scaleDimension(
                    style.letterSpacing,
                    style.letterSpacingUnit
            );
            paint.setLetterSpacing(letterSpacingPx / textSize);
        }
        if (style.shadowRadius > 0f) {
            paint.setShadowLayer(
                    scaleDimension(style.shadowRadius, style.shadowUnit),
                    scaleDimension(style.shadowDx, style.shadowUnit),
                    scaleDimension(style.shadowDy, style.shadowUnit),
                    style.shadowColor
            );
        } else {
            paint.clearShadowLayer();
        }
    }

    private Typeface resolveTypeface() {
        switch (style.fontSource) {
            case RESOURCE:
                return TypefaceCache.fromResource(
                        context,
                        style.fontResourceId
                );
            case ASSET:
                return TypefaceCache.fromAsset(
                        context,
                        style.fontAssetPath
                );
            case TYPEFACE:
                return style.typeface;
            case DEFAULT:
            default:
                return Typeface.DEFAULT;
        }
    }

    private float scaleTextSize(TextStyle style) {
        return style.textSizeUnit == TextStyle.DimensionUnit.PIXELS
                ? style.textSize
                : style.textSize * dimensionScale;
    }

    private float scaleDimension(
            float value,
            TextStyle.DimensionUnit unit
    ) {
        return unit == TextStyle.DimensionUnit.PIXELS
                ? value
                : value * dimensionScale;
    }

    private void invalidate() {
        if (owner != null) {
            owner.postInvalidateComponentOnAnimation();
        }
    }

    private void ensureActive() {
        if (released) {
            throw new IllegalStateException("Text has been released: " + id);
        }
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Text ID cannot be null or blank."
            );
        }
        return id.trim();
    }

    private static void requireBounds(RectF bounds) {
        if (!Float.isFinite(bounds.left)
                || !Float.isFinite(bounds.top)
                || !Float.isFinite(bounds.right)
                || !Float.isFinite(bounds.bottom)
                || bounds.width() <= 0f
                || bounds.height() <= 0f) {
            throw new IllegalArgumentException(
                    "Text bounds must have positive finite dimensions."
            );
        }
    }

    private static Layout.Alignment toLayoutAlignment(Alignment alignment) {
        switch (alignment) {
            case CENTER:
                return Layout.Alignment.ALIGN_CENTER;
            case END:
                return Layout.Alignment.ALIGN_OPPOSITE;
            case START:
            default:
                return Layout.Alignment.ALIGN_NORMAL;
        }
    }

    private static TextUtils.TruncateAt toTruncateAt(Overflow overflow) {
        switch (overflow) {
            case ELLIPSIZE_START:
                return TextUtils.TruncateAt.START;
            case ELLIPSIZE_MIDDLE:
                return TextUtils.TruncateAt.MIDDLE;
            case ELLIPSIZE_END:
                return TextUtils.TruncateAt.END;
            case CLIP:
            default:
                return null;
        }
    }

    public interface OnClickListener {
        void onClick(String id);
    }

    public static final class Builder implements ComponentFactory<Text> {

        private final Context context;
        private final String id;
        private final CharSequence value;
        private final Position position;
        private final Size size;
        private final RectF explicitBounds;
        private TextStyle.Builder styleBuilder = new TextStyle.Builder();
        private boolean enabled = true;
        private boolean horizontalCentered;
        private boolean selectionEnabled;
        private boolean copyEnabled = true;
        private TextSelectionActionCallback selectionActionCallback;
        private int selectionColor = 0x6633b5e5;
        private int selectionHandleColor = 0xff6633b5;
        private OnClickListener clickListener;

        public Builder(
                Context context,
                String id,
                CharSequence text,
                Position position,
                Size size
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            value = Objects.requireNonNull(text, "Text cannot be null.");
            this.position = Objects.requireNonNull(
                    position,
                    "Position cannot be null."
            );
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
            explicitBounds = null;
        }

        public Builder(
                Context context,
                String id,
                CharSequence text,
                RectF bounds
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            value = Objects.requireNonNull(text, "Text cannot be null.");
            explicitBounds = new RectF(Objects.requireNonNull(
                    bounds,
                    "Bounds cannot be null."
            ));
            position = null;
            size = null;
        }

        public Builder setStyle(TextStyle style) {
            styleBuilder = new TextStyle.Builder(
                    Objects.requireNonNull(style, "TextStyle cannot be null.")
            );
            return this;
        }

        public Builder setHorizontalCenter(boolean enabled) {
            horizontalCentered = enabled;
            return this;
        }

        /**
         * Fluent alias for {@link #setHorizontalCenter(boolean)}.
         */
        public Builder horizontalCenter(boolean enabled) {
            return setHorizontalCenter(enabled);
        }

        public Builder useDefaultFont() {
            styleBuilder.useDefaultFont();
            return this;
        }

        public Builder setFont(int fontResourceId) {
            styleBuilder.setFont(fontResourceId);
            return this;
        }

        public Builder setFontAsset(String assetPath) {
            styleBuilder.setFontAsset(assetPath);
            return this;
        }

        public Builder setFont(Typeface typeface) {
            styleBuilder.setFont(typeface);
            return this;
        }

        public Builder setFontVariations(FontVariation variation) {
            styleBuilder.setFontVariations(variation);
            return this;
        }

        public Builder clearFontVariations() {
            styleBuilder.clearFontVariations();
            return this;
        }

        public Builder setTextSize(float size) {
            styleBuilder.setTextSize(size);
            return this;
        }

        public Builder setTextSizePx(float pixels) {
            styleBuilder.setTextSizePx(pixels);
            return this;
        }

        public Builder setTextColor(int color) {
            styleBuilder.setTextColor(color);
            return this;
        }

        public Builder setAlpha(float alpha) {
            styleBuilder.setAlpha(alpha);
            return this;
        }

        public Builder setLetterSpacing(float spacing) {
            styleBuilder.setLetterSpacing(spacing);
            return this;
        }

        public Builder setLetterSpacingPx(float pixels) {
            styleBuilder.setLetterSpacingPx(pixels);
            return this;
        }

        public Builder setLetterSpacingPercent(float percent) {
            styleBuilder.setLetterSpacingPercent(percent);
            return this;
        }

        public Builder setLineSpacing(float spacing) {
            styleBuilder.setLineSpacing(spacing);
            return this;
        }

        public Builder setLineSpacingPx(float pixels) {
            styleBuilder.setLineSpacingPx(pixels);
            return this;
        }

        public Builder setLineSpacingMultiplier(float multiplier) {
            styleBuilder.setLineSpacingMultiplier(multiplier);
            return this;
        }

        public Builder setLineHeightPercent(float percent) {
            styleBuilder.setLineHeightPercent(percent);
            return this;
        }

        public Builder setPadding(float horizontal, float vertical) {
            styleBuilder.setPadding(horizontal, vertical);
            return this;
        }

        public Builder setPaddingPx(float horizontal, float vertical) {
            styleBuilder.setPaddingPx(horizontal, vertical);
            return this;
        }

        public Builder setAlignment(Alignment alignment) {
            styleBuilder.setAlignment(alignment);
            return this;
        }

        public Builder setVerticalAlignment(
                VerticalAlignment alignment
        ) {
            styleBuilder.setVerticalAlignment(alignment);
            return this;
        }

        public Builder setOverflow(Overflow overflow) {
            styleBuilder.setOverflow(overflow);
            return this;
        }

        public Builder setMaxLines(int maxLines) {
            styleBuilder.setMaxLines(maxLines);
            return this;
        }

        public Builder setWrapEnabled(boolean enabled) {
            styleBuilder.setWrapEnabled(enabled);
            return this;
        }

        public Builder setShadow(
                float radius,
                float dx,
                float dy,
                int color
        ) {
            styleBuilder.setShadow(radius, dx, dy, color);
            return this;
        }

        public Builder setShadowPx(
                float radius,
                float dx,
                float dy,
                int color
        ) {
            styleBuilder.setShadowPx(radius, dx, dy, color);
            return this;
        }

        public Builder clearShadow() {
            styleBuilder.clearShadow();
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setSelectionEnabled(boolean enabled) {
            selectionEnabled = enabled;
            return this;
        }

        public Builder setCopyEnabled(boolean enabled) {
            copyEnabled = enabled;
            return this;
        }

        public Builder setSelectionActionCallback(TextSelectionActionCallback callback) {
            selectionActionCallback = callback;
            return this;
        }

        public Builder setSelectionColor(int color) {
            selectionColor = color;
            return this;
        }

        public Builder setSelectionHandleColor(int color) {
            selectionHandleColor = color;
            return this;
        }

        public Builder setOnClickListener(OnClickListener listener) {
            clickListener = listener;
            return this;
        }

        @Override
        public Text build(View hostView) {
            return new Text(this, hostView);
        }
    }
}

package com.ogfa.nativeviews.switchcomponent;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.ogfa.nativeviews.card.DropShadow;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.selection.OnCheckedChangeListener;
import com.ogfa.nativeviews.selection.SelectableComponent;
import com.ogfa.nativeviews.selection.internal.CheckedStateController;

import java.util.Objects;

/** Native Canvas switch with tap, drag, animation, and Figma-aware styling. */
public final class Switch implements SelectableComponent {
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

    private static final float DEFAULT_THUMB_PADDING = 4f;
    private static final long DEFAULT_ANIMATION_DURATION = 180L;
    private static final long DEFAULT_RIPPLE_DURATION = 240L;
    private static final float DEFAULT_DISABLED_ALPHA = 0.45f;

    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final RectF trackBounds = new RectF();
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final CheckedStateController state;
    private final int touchSlop;

    private ComponentHost owner;
    private FigmaConfig figmaConfig;
    private float dimensionScale;
    private float progress;
    private float resolvedCornerRadius;
    private float resolvedThumbPadding;
    private float resolvedThumbSize;
    private float resolvedStrokeWidth;
    private float minThumbCenterX;
    private float maxThumbCenterX;
    private int checkedTrackColor;
    private int uncheckedTrackColor;
    private int disabledCheckedTrackColor;
    private int disabledUncheckedTrackColor;
    private int thumbColor;
    private int disabledCheckedThumbColor;
    private int disabledUncheckedThumbColor;
    private int strokeColor;
    private int disabledStrokeColor;
    private boolean disabledStrokeColorExplicit;
    private float cornerRadius;
    private boolean cornerRadiusAuto;
    private boolean cornerRadiusInPixels;
    private float thumbPadding;
    private boolean thumbPaddingInPixels;
    private float thumbSize;
    private boolean thumbSizeAuto;
    private boolean thumbSizeInPixels;
    private float strokeWidth;
    private boolean strokeWidthInPixels;
    private boolean strokeEnabled;
    private DropShadow thumbShadow;
    private boolean thumbShadowEnabled;
    private boolean disabledThumbShadowEnabled;
    private boolean thumbShadowInPixels;
    private float alpha;
    private float disabledAlpha;
    private boolean visible;
    private boolean enabled;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private boolean released;
    private OnCheckedChangeListener checkedChangeListener;
    private Runnable soundAction;
    private Runnable hapticAction;
    private Interpolator animationInterpolator;

    private boolean touchCaptured;
    private boolean dragging;
    private float downX;
    private boolean downChecked;
    private boolean rippleEnabled;
    private int rippleColor;
    private long rippleDuration;
    private float rippleX;
    private float rippleY;
    private float rippleProgress;
    private ValueAnimator rippleAnimator;

    private Switch(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        id = requireId(builder.id);
        touchSlop = ViewConfiguration.get(builder.context).getScaledTouchSlop();
        checkedTrackColor = builder.checkedTrackColor;
        uncheckedTrackColor = builder.uncheckedTrackColor;
        disabledCheckedTrackColor = builder.disabledCheckedTrackColor;
        disabledUncheckedTrackColor = builder.disabledUncheckedTrackColor;
        thumbColor = builder.thumbColor;
        disabledCheckedThumbColor = builder.disabledCheckedThumbColor;
        disabledUncheckedThumbColor = builder.disabledUncheckedThumbColor;
        strokeColor = builder.strokeColor;
        disabledStrokeColor = builder.disabledStrokeColor;
        disabledStrokeColorExplicit = builder.disabledStrokeColorExplicit;
        cornerRadius = builder.cornerRadius;
        cornerRadiusAuto = builder.cornerRadiusAuto;
        cornerRadiusInPixels = builder.cornerRadiusInPixels;
        thumbPadding = builder.thumbPadding;
        thumbPaddingInPixels = builder.thumbPaddingInPixels;
        thumbSize = builder.thumbSize;
        thumbSizeAuto = builder.thumbSizeAuto;
        thumbSizeInPixels = builder.thumbSizeInPixels;
        strokeWidth = builder.strokeWidth;
        strokeWidthInPixels = builder.strokeWidthInPixels;
        strokeEnabled = builder.strokeEnabled;
        thumbShadow = builder.thumbShadow;
        thumbShadowEnabled = builder.thumbShadowEnabled;
        disabledThumbShadowEnabled = builder.disabledThumbShadowEnabled;
        thumbShadowInPixels = builder.thumbShadowInPixels;
        alpha = builder.alpha;
        disabledAlpha = builder.disabledAlpha;
        visible = builder.visible;
        enabled = builder.enabled;
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        checkedChangeListener = builder.checkedChangeListener;
        soundAction = builder.soundAction;
        hapticAction = builder.hapticAction;
        animationInterpolator = builder.animationInterpolator;
        rippleEnabled = builder.rippleEnabled;
        rippleColor = builder.rippleColor;
        rippleDuration = builder.rippleDuration;
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        state = new CheckedStateController(
                builder.checked,
                builder.animationDuration,
                toInterpolator(animationInterpolator),
                new CheckedStateController.Callback() {
                    @Override public void onProgressChanged(float value) {
                        progress = value;
                        invalidateOnAnimation();
                    }
                    @Override public void onCheckedChanged(boolean value, boolean fromUser) {
                        if (checkedChangeListener != null) {
                            checkedChangeListener.onCheckedChanged(id, value, fromUser);
                        }
                        if (fromUser) runFeedback();
                    }
                }
        );
        progress = state.getProgress();
        rebuildGeometry();
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() { return new RectF(bounds); }
    public FigmaConfig getFigmaConfig() { return figmaConfig; }
    public float getDimensionScale() { return dimensionScale; }
    @Override public boolean isChecked() { ensureActive(); return state.isChecked(); }
    public float getThumbProgress() { return progress; }
    public float getResolvedTrackCornerRadius() { return resolvedCornerRadius; }
    public float getResolvedThumbPadding() { return resolvedThumbPadding; }
    public float getResolvedThumbSize() { return resolvedThumbSize; }
    public boolean isAnimating() { return state.isAnimating(); }
    @Override public boolean isVisible() { return visible; }
    @Override public boolean isEnabled() { return enabled; }
    public float getAlpha() { return alpha; }
    public float getDisabledAlpha() { return disabledAlpha; }
    public int getDisabledCheckedTrackColor() { return disabledCheckedTrackColor; }
    public int getDisabledUncheckedTrackColor() { return disabledUncheckedTrackColor; }
    public int getDisabledCheckedThumbColor() { return disabledCheckedThumbColor; }
    public int getDisabledUncheckedThumbColor() { return disabledUncheckedThumbColor; }
    public int getDisabledStrokeColor() { return disabledStrokeColor; }
    public boolean isDisabledThumbShadowEnabled() { return disabledThumbShadowEnabled; }

    @Override public Switch setChecked(boolean value) { ensureActive(); state.setChecked(value, true, false); return this; }
    @Override public Switch setCheckedImmediately(boolean value) { ensureActive(); state.setChecked(value, false, false); return this; }
    @Override public Switch toggle() { return setChecked(!isChecked()); }
    @Override public Switch toggleImmediately() { return setCheckedImmediately(!isChecked()); }
    @Override public Switch setOnCheckedChangeListener(OnCheckedChangeListener value) { ensureActive(); checkedChangeListener = value; return this; }
    @Override public Switch removeOnCheckedChangeListener() { return setOnCheckedChangeListener(null); }

    public Switch setRegion(Position position, Size size) {
        ensureActive(); resolveRegion(Objects.requireNonNull(position), Objects.requireNonNull(size), null);
        applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public Switch setRegion(RectF value) {
        ensureActive(); resolveRegion(null, null, Objects.requireNonNull(value));
        applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public Switch horizontalCenter(boolean value) { ensureActive(); horizontalCentered = value; applyParentAlignment(); rebuildGeometry(); invalidate(); return this; }
    public Switch verticalCenter(boolean value) { ensureActive(); verticalCentered = value; applyParentAlignment(); rebuildGeometry(); invalidate(); return this; }
    public Switch setAlpha(float value) { ensureActive(); alpha = requireAlpha(value); invalidate(); return this; }
    public Switch setVisible(boolean value) { ensureActive(); visible = value; if (!value) cancelTouch(); invalidate(); return this; }
    public Switch setEnabled(boolean value) { ensureActive(); enabled = value; if (!value) cancelTouch(); invalidate(); return this; }

    public Switch setCheckedTrackColor(int value) { checkedTrackColor = value; invalidate(); return this; }
    public Switch setUncheckedTrackColor(int value) { uncheckedTrackColor = value; invalidate(); return this; }
    public Switch setDisabledTrackColor(int value) { disabledCheckedTrackColor = value; disabledUncheckedTrackColor = value; invalidate(); return this; }
    public Switch setDisabledCheckedTrackColor(int value) { disabledCheckedTrackColor = value; invalidate(); return this; }
    public Switch setDisabledUncheckedTrackColor(int value) { disabledUncheckedTrackColor = value; invalidate(); return this; }
    public Switch setThumbColor(int value) { thumbColor = value; invalidate(); return this; }
    public Switch setDisabledThumbColor(int value) { disabledCheckedThumbColor = value; disabledUncheckedThumbColor = value; invalidate(); return this; }
    public Switch setDisabledCheckedThumbColor(int value) { disabledCheckedThumbColor = value; invalidate(); return this; }
    public Switch setDisabledUncheckedThumbColor(int value) { disabledUncheckedThumbColor = value; invalidate(); return this; }
    public Switch setDisabledStrokeColor(int value) { disabledStrokeColor = value; disabledStrokeColorExplicit = true; invalidate(); return this; }
    public Switch setDisabledAlpha(float value) { disabledAlpha = requireAlpha(value); invalidate(); return this; }

    public Switch setTrackCornerRadius(float value) { cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackCornerRadiusPx(float value) { cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackCornerRadiusAuto() { cornerRadiusAuto = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbPadding(float value) { thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbPaddingPx(float value) { thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbSize(float value) { thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbSizePx(float value) { thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbSizeAuto() { thumbSizeAuto = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackStroke(float width, int color) { strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = false; strokeEnabled = width > 0f; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackStrokePx(float width, int color) { strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = true; strokeEnabled = width > 0f; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackStrokeEnabled(boolean value) { strokeEnabled = value; invalidate(); return this; }
    public Switch setThumbShadow(DropShadow value) { thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = false; invalidate(); return this; }
    public Switch setThumbShadowPx(DropShadow value) { thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = true; invalidate(); return this; }
    public Switch setThumbShadowEnabled(boolean value) { thumbShadowEnabled = value; invalidate(); return this; }
    public Switch setDisabledThumbShadowEnabled(boolean value) { disabledThumbShadowEnabled = value; invalidate(); return this; }

    public Switch setAnimationDuration(long value) { state.setDuration(value); return this; }
    public Switch setAnimationInterpolator(Interpolator value) { animationInterpolator = Objects.requireNonNull(value); state.setInterpolator(toInterpolator(value)); return this; }
    public Switch finishAnimation() { state.finishAnimation(); return this; }
    public Switch cancelAnimation() { state.cancelAnimation(); return this; }
    public Switch setRippleEnabled(boolean value) { rippleEnabled = value; if (!value) cancelRipple(); invalidate(); return this; }
    public Switch setRippleColor(int value) { rippleColor = value; invalidate(); return this; }
    public Switch setRippleDuration(long value) { rippleDuration = requireDuration(value, "Ripple duration"); return this; }
    public Switch setSoundAction(Runnable value) { soundAction = value; return this; }
    public Switch setHapticAction(Runnable value) { hapticAction = value; return this; }

    @Override public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || alpha <= 0f) return;
        float effectiveAlpha = alpha * (enabled ? 1f : disabledAlpha);
        int save = effectiveAlpha < 1f
                ? canvas.saveLayerAlpha(bounds, Math.round(255f * effectiveAlpha))
                : canvas.save();
        float radius = resolvedCornerRadius;
        trackPaint.setColor(enabled
                ? blend(uncheckedTrackColor, checkedTrackColor, progress)
                : blend(disabledUncheckedTrackColor, disabledCheckedTrackColor, progress));
        canvas.drawRoundRect(trackBounds, radius, radius, trackPaint);
        if (strokeEnabled && resolvedStrokeWidth > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(resolvedStrokeWidth);
            strokePaint.setColor(enabled ? strokeColor : disabledStrokeColor);
            RectF strokeBounds = new RectF(trackBounds);
            strokeBounds.inset(resolvedStrokeWidth / 2f, resolvedStrokeWidth / 2f);
            canvas.drawRoundRect(strokeBounds, Math.max(0f, radius - resolvedStrokeWidth / 2f), Math.max(0f, radius - resolvedStrokeWidth / 2f), strokePaint);
        }
        int clip = canvas.save();
        canvas.clipRect(trackBounds);
        drawRipple(canvas);
        canvas.restoreToCount(clip);
        float thumbX = minThumbCenterX + (maxThumbCenterX - minThumbCenterX) * progress;
        float thumbY = trackBounds.centerY();
        float thumbRadius = resolvedThumbSize / 2f;
        if (thumbShadowEnabled && (enabled || disabledThumbShadowEnabled)
                && thumbShadow.getColor() != Color.TRANSPARENT) {
            drawThumbShadow(canvas, thumbX, thumbY, thumbRadius);
        }
        thumbPaint.setColor(enabled
                ? thumbColor
                : blend(disabledUncheckedThumbColor, disabledCheckedThumbColor, progress));
        canvas.drawCircle(thumbX, thumbY, thumbRadius, thumbPaint);
        canvas.restoreToCount(save);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!acceptsTouch(event.getX(), event.getY())) return false;
                touchCaptured = true; dragging = false; downX = event.getX(); downChecked = state.isChecked();
                startRipple(event.getX(), event.getY()); return true;
            case MotionEvent.ACTION_MOVE:
                if (!touchCaptured) return false;
                if (!dragging && Math.abs(event.getX() - downX) > touchSlop) dragging = true;
                if (dragging) {
                    float travel = maxThumbCenterX - minThumbCenterX;
                    state.setDragProgress(travel <= 0f ? 0f : (event.getX() - minThumbCenterX) / travel);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!touchCaptured) return false;
                touchCaptured = false;
                if (dragging) state.commitDrag(progress >= 0.5f, true);
                else if (bounds.contains(event.getX(), event.getY())) state.setChecked(!downChecked, true, true);
                else state.setChecked(downChecked, true, false);
                dragging = false; finishRipple(); return true;
            case MotionEvent.ACTION_CANCEL:
                boolean handled = touchCaptured;
                if (touchCaptured) state.setChecked(downChecked, true, false);
                touchCaptured = false; dragging = false; cancelRipple(); return handled;
            default: return touchCaptured;
        }
    }

    @Override public void attach(ComponentHost value) {
        ensureActive();
        Objects.requireNonNull(value, "Component host cannot be null.");
        if (owner != null && owner != value) throw new IllegalStateException("Switch already belongs to another host.");
        owner = value; applyParentAlignment(); rebuildGeometry();
    }

    @Override public void release() {
        if (released) return;
        cancelTouch(); cancelRipple(); state.release();
        checkedChangeListener = null; soundAction = null; hapticAction = null; owner = null; released = true;
    }

    private void resolveRegion(Position position, Size size, RectF explicit) {
        if (explicit != null) {
            requireBounds(explicit); baseBounds.set(explicit);
            figmaConfig = FigmaConfig.getDefault(); dimensionScale = figmaConfig.getScale(hostView.getWidth());
        } else {
            Objects.requireNonNull(position, "Position cannot be null."); Objects.requireNonNull(size, "Size cannot be null.");
            baseBounds.set(position.toRectF(hostView, size)); figmaConfig = position.getFigmaConfig(); dimensionScale = position.getScale(hostView);
        }
        bounds.set(baseBounds);
    }

    private void applyParentAlignment() {
        bounds.set(baseBounds);
        if (owner == null) return;
        RectF parent = owner.getComponentBounds();
        if (horizontalCentered) bounds.offsetTo(parent.centerX() - bounds.width() / 2f, bounds.top);
        if (verticalCentered) bounds.offsetTo(bounds.left, parent.centerY() - bounds.height() / 2f);
    }

    private void rebuildGeometry() {
        trackBounds.set(bounds);
        resolvedCornerRadius = cornerRadiusAuto ? bounds.height() / 2f : Math.min(resolve(cornerRadius, cornerRadiusInPixels), bounds.height() / 2f);
        resolvedThumbPadding = resolve(thumbPadding, thumbPaddingInPixels);
        float available = bounds.height() - 2f * resolvedThumbPadding;
        if (!(available > 0f)) throw new IllegalArgumentException("Thumb padding leaves no drawable height.");
        resolvedThumbSize = thumbSizeAuto ? available : resolve(thumbSize, thumbSizeInPixels);
        if (resolvedThumbSize > available + 0.001f) throw new IllegalArgumentException("Thumb size cannot exceed track height minus padding.");
        if (resolvedThumbSize > bounds.width() - 2f * resolvedThumbPadding + 0.001f) throw new IllegalArgumentException("Thumb size cannot exceed available track width.");
        resolvedStrokeWidth = resolve(strokeWidth, strokeWidthInPixels);
        float thumbRadius = resolvedThumbSize / 2f;
        minThumbCenterX = bounds.left + resolvedThumbPadding + thumbRadius;
        maxThumbCenterX = bounds.right - resolvedThumbPadding - thumbRadius;
    }

    private void drawThumbShadow(Canvas canvas, float x, float y, float radius) {
        float scale = thumbShadowInPixels ? 1f : dimensionScale;
        float offsetX = thumbShadow.getX() * scale;
        float offsetY = thumbShadow.getY() * scale;
        float spread = thumbShadow.getSpread() * scale;
        float blur = thumbShadow.getBlur() * scale;
        int color = thumbShadow.getColor();
        int baseAlpha = Color.alpha(color);
        int steps = blur <= 0f ? 1 : 8;
        for (int i = steps; i >= 1; i--) {
            float fraction = i / (float) steps;
            shadowPaint.setColor((color & 0x00ffffff) | (Math.round(baseAlpha * (1f - fraction * 0.82f)) << 24));
            canvas.drawCircle(x + offsetX, y + offsetY, radius + spread + blur * fraction, shadowPaint);
        }
    }

    private void startRipple(float x, float y) {
        if (!rippleEnabled) return;
        cancelRipple(); rippleX = x; rippleY = y; rippleProgress = 0f;
        rippleAnimator = ValueAnimator.ofFloat(0f, 1f);
        rippleAnimator.setDuration(rippleDuration); rippleAnimator.setInterpolator(new DecelerateInterpolator());
        rippleAnimator.addUpdateListener(value -> { rippleProgress = (float) value.getAnimatedValue(); invalidateOnAnimation(); });
        rippleAnimator.start();
    }
    private void finishRipple() { if (rippleAnimator != null && rippleAnimator.isRunning()) return; cancelRipple(); }
    private void drawRipple(Canvas canvas) {
        if (!rippleEnabled || rippleAnimator == null) return;
        float max = (float) Math.hypot(Math.max(rippleX - bounds.left, bounds.right - rippleX), Math.max(rippleY - bounds.top, bounds.bottom - rippleY));
        int sourceAlpha = Color.alpha(rippleColor);
        ripplePaint.setColor((rippleColor & 0x00ffffff) | (Math.round(sourceAlpha * (1f - rippleProgress)) << 24));
        canvas.drawCircle(rippleX, rippleY, max * rippleProgress, ripplePaint);
    }
    private void cancelRipple() { if (rippleAnimator != null) rippleAnimator.cancel(); rippleAnimator = null; rippleProgress = 0f; }
    private void cancelTouch() { if (touchCaptured) state.setChecked(downChecked, true, false); touchCaptured = false; dragging = false; cancelRipple(); }
    private boolean acceptsTouch(float x, float y) { return visible && enabled && !released && bounds.contains(x, y); }
    private void runFeedback() { if (soundAction != null) soundAction.run(); if (hapticAction != null) hapticAction.run(); }
    private float resolve(float value, boolean pixels) { return pixels ? value : value * dimensionScale; }
    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
    private void invalidateOnAnimation() { if (owner != null) owner.postInvalidateComponentOnAnimation(); }
    private void ensureActive() { if (released) throw new IllegalStateException("Switch has been released: " + id); }

    private static TimeInterpolator toInterpolator(Interpolator value) {
        switch (Objects.requireNonNull(value)) {
            case LINEAR: return new LinearInterpolator();
            case EASE_IN: return new AccelerateInterpolator();
            case EASE_OUT: return new DecelerateInterpolator();
            default: return new AccelerateDecelerateInterpolator();
        }
    }
    private static int blend(int start, int end, float fraction) {
        int a = Math.round(Color.alpha(start) + (Color.alpha(end) - Color.alpha(start)) * fraction);
        int r = Math.round(Color.red(start) + (Color.red(end) - Color.red(start)) * fraction);
        int g = Math.round(Color.green(start) + (Color.green(end) - Color.green(start)) * fraction);
        int b = Math.round(Color.blue(start) + (Color.blue(end) - Color.blue(start)) * fraction);
        return Color.argb(a, r, g, b);
    }
    private static String requireId(String value) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Switch ID cannot be blank."); return value.trim(); }
    private static void requireBounds(RectF value) { if (value.width() <= 0f || value.height() <= 0f) throw new IllegalArgumentException("Switch bounds must be positive."); }
    private static float requireAlpha(float value) { if (!Float.isFinite(value) || value < 0f || value > 1f) throw new IllegalArgumentException("Alpha must be in [0, 1]."); return value; }
    private static float requireNonNegative(float value, String label) { if (!Float.isFinite(value) || value < 0f) throw new IllegalArgumentException(label + " must be non-negative and finite."); return value; }
    private static float requirePositive(float value, String label) { if (!Float.isFinite(value) || value <= 0f) throw new IllegalArgumentException(label + " must be positive and finite."); return value; }
    private static long requireDuration(long value, String label) { if (value < 0L) throw new IllegalArgumentException(label + " cannot be negative."); return value; }

    public static final class Builder implements ComponentFactory<Switch> {
        private final Context context;
        private final String id;
        private Position position;
        private Size size;
        private RectF explicitBounds;
        private boolean checked;
        private int checkedTrackColor = 0xff019cc4;
        private int uncheckedTrackColor = 0xffb8c0c8;
        private int disabledCheckedTrackColor = 0xffd5d9dd;
        private int disabledUncheckedTrackColor = 0xffd5d9dd;
        private int thumbColor = Color.WHITE;
        private int disabledCheckedThumbColor = 0xffeeeeee;
        private int disabledUncheckedThumbColor = 0xffeeeeee;
        private int strokeColor = Color.TRANSPARENT;
        private int disabledStrokeColor = Color.TRANSPARENT;
        private boolean disabledStrokeColorExplicit;
        private float cornerRadius;
        private boolean cornerRadiusAuto = true;
        private boolean cornerRadiusInPixels;
        private float thumbPadding = DEFAULT_THUMB_PADDING;
        private boolean thumbPaddingInPixels;
        private float thumbSize;
        private boolean thumbSizeAuto = true;
        private boolean thumbSizeInPixels;
        private float strokeWidth;
        private boolean strokeWidthInPixels;
        private boolean strokeEnabled;
        private DropShadow thumbShadow = new DropShadow(0f, 2f, 6f, 0f, Color.argb(35, 0, 0, 0));
        private boolean thumbShadowEnabled;
        private boolean disabledThumbShadowEnabled = true;
        private boolean thumbShadowInPixels;
        private long animationDuration = DEFAULT_ANIMATION_DURATION;
        private Interpolator animationInterpolator = Interpolator.EASE_OUT;
        private boolean rippleEnabled;
        private int rippleColor = 0x22000000;
        private long rippleDuration = DEFAULT_RIPPLE_DURATION;
        private float alpha = 1f;
        private float disabledAlpha = DEFAULT_DISABLED_ALPHA;
        private boolean visible = true;
        private boolean enabled = true;
        private boolean horizontalCentered;
        private boolean verticalCentered;
        private OnCheckedChangeListener checkedChangeListener;
        private Runnable soundAction;
        private Runnable hapticAction;

        public Builder(Context context, String id, Position position, Size size) {
            this.context = Objects.requireNonNull(context, "Context cannot be null."); this.id = requireId(id);
            this.position = Objects.requireNonNull(position, "Position cannot be null."); this.size = Objects.requireNonNull(size, "Size cannot be null.");
        }
        public Builder(Context context, String id, RectF bounds) {
            this.context = Objects.requireNonNull(context, "Context cannot be null."); this.id = requireId(id);
            explicitBounds = new RectF(Objects.requireNonNull(bounds, "Bounds cannot be null.")); requireBounds(explicitBounds);
        }
        public Builder setChecked(boolean value) { checked = value; return this; }
        public Builder setCheckedTrackColor(int value) { checkedTrackColor = value; return this; }
        public Builder setUncheckedTrackColor(int value) { uncheckedTrackColor = value; return this; }
        public Builder setDisabledTrackColor(int value) { disabledCheckedTrackColor = value; disabledUncheckedTrackColor = value; return this; }
        public Builder setDisabledCheckedTrackColor(int value) { disabledCheckedTrackColor = value; return this; }
        public Builder setDisabledUncheckedTrackColor(int value) { disabledUncheckedTrackColor = value; return this; }
        public Builder setThumbColor(int value) { thumbColor = value; return this; }
        public Builder setDisabledThumbColor(int value) { disabledCheckedThumbColor = value; disabledUncheckedThumbColor = value; return this; }
        public Builder setDisabledCheckedThumbColor(int value) { disabledCheckedThumbColor = value; return this; }
        public Builder setDisabledUncheckedThumbColor(int value) { disabledUncheckedThumbColor = value; return this; }
        public Builder setDisabledStrokeColor(int value) { disabledStrokeColor = value; disabledStrokeColorExplicit = true; return this; }
        public Builder setDisabledAlpha(float value) { disabledAlpha = requireAlpha(value); return this; }
        public Builder setTrackCornerRadius(float value) { cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = false; return this; }
        public Builder setTrackCornerRadiusPx(float value) { cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = true; return this; }
        public Builder setTrackCornerRadiusAuto() { cornerRadiusAuto = true; return this; }
        public Builder setThumbPadding(float value) { thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = false; return this; }
        public Builder setThumbPaddingPx(float value) { thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = true; return this; }
        public Builder setThumbSize(float value) { thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = false; return this; }
        public Builder setThumbSizePx(float value) { thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = true; return this; }
        public Builder setThumbSizeAuto() { thumbSizeAuto = true; return this; }
        public Builder setTrackStroke(float width, int color) { strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = false; strokeEnabled = width > 0f; return this; }
        public Builder setTrackStrokePx(float width, int color) { strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = true; strokeEnabled = width > 0f; return this; }
        public Builder setTrackStrokeEnabled(boolean value) { strokeEnabled = value; return this; }
        public Builder setThumbShadow(DropShadow value) { thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = false; return this; }
        public Builder setThumbShadowPx(DropShadow value) { thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = true; return this; }
        public Builder setThumbShadowEnabled(boolean value) { thumbShadowEnabled = value; return this; }
        public Builder setDisabledThumbShadowEnabled(boolean value) { disabledThumbShadowEnabled = value; return this; }
        public Builder setAnimationDuration(long value) { animationDuration = requireDuration(value, "Animation duration"); return this; }
        public Builder setAnimationInterpolator(Interpolator value) { animationInterpolator = Objects.requireNonNull(value); return this; }
        public Builder setRippleEnabled(boolean value) { rippleEnabled = value; return this; }
        public Builder setRippleColor(int value) { rippleColor = value; return this; }
        public Builder setRippleDuration(long value) { rippleDuration = requireDuration(value, "Ripple duration"); return this; }
        public Builder setSoundAction(Runnable value) { soundAction = value; return this; }
        public Builder setHapticAction(Runnable value) { hapticAction = value; return this; }
        public Builder setAlpha(float value) { alpha = requireAlpha(value); return this; }
        public Builder setVisible(boolean value) { visible = value; return this; }
        public Builder setEnabled(boolean value) { enabled = value; return this; }
        public Builder horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder verticalCenter(boolean value) { verticalCentered = value; return this; }
        public Builder setOnCheckedChangeListener(OnCheckedChangeListener value) { checkedChangeListener = value; return this; }
        @Override public Switch build(View hostView) { return new Switch(this, hostView); }
    }
}

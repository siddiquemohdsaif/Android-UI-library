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
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.selection.OnCheckedChangeListener;
import com.ogfa.nativeviews.selection.SelectableComponent;
import com.ogfa.nativeviews.selection.internal.CheckedStateController;
import com.ogfa.nativeviews.switchcomponent.internal.ColorSwitchRenderer;
import com.ogfa.nativeviews.switchcomponent.internal.ComplexImageSwitchRenderer;
import com.ogfa.nativeviews.switchcomponent.internal.SimpleImageSwitchRenderer;
import com.ogfa.nativeviews.switchcomponent.internal.SwitchRenderState;
import com.ogfa.nativeviews.switchcomponent.internal.SwitchRenderer;

import java.util.Objects;

/** Native Canvas switch with tap, drag, animation, and Figma-aware styling. */
public final class Switch implements SelectableComponent {
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
    public enum RenderMode { COLOR, COMPLEX_IMAGE, SIMPLE_IMAGE }
    public enum ImageTransition { CROSS_FADE, SNAP }

    private static final float DEFAULT_THUMB_PADDING = 4f;
    private static final long DEFAULT_ANIMATION_DURATION = 180L;
    private static final long DEFAULT_RIPPLE_DURATION = 240L;
    private static final float DEFAULT_DISABLED_ALPHA = 0.45f;

    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final RectF trackBounds = new RectF();
    private final RectF thumbBounds = new RectF();
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SwitchRenderState renderState = new SwitchRenderState();
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
    private RenderMode renderMode;
    private ImageTransition imageTransition;
    private SwitchImages switchImages;
    private SwitchRenderer renderer;
    private Image.ScaleType trackImageScaleType;
    private Image.ScaleType thumbImageScaleType;
    private Image.ScaleType switchImageScaleType;
    private boolean imageFiltering;
    private boolean dragEnabled;
    private boolean disabledAlphaExplicit;

    private boolean touchCaptured;
    private boolean dragging;
    private boolean tapCancelled;
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
        renderMode = builder.renderMode;
        imageTransition = builder.imageTransition;
        switchImages = builder.switchImages;
        trackImageScaleType = builder.trackImageScaleType;
        thumbImageScaleType = builder.thumbImageScaleType;
        switchImageScaleType = builder.switchImageScaleType;
        imageFiltering = builder.imageFiltering;
        dragEnabled = builder.dragEnabled;
        disabledAlphaExplicit = builder.disabledAlphaExplicit;
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
        rebuildRenderer();
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
    public RenderMode getRenderMode() { return renderMode; }
    public SwitchImages getSwitchImages() { return switchImages; }
    public ImageTransition getImageTransition() { return imageTransition; }
    public Image.ScaleType getTrackImageScaleType() { return trackImageScaleType; }
    public Image.ScaleType getThumbImageScaleType() { return thumbImageScaleType; }
    public Image.ScaleType getSwitchImageScaleType() { return switchImageScaleType; }
    public boolean isImageFilteringEnabled() { return imageFiltering; }
    public boolean isDragEnabled() { return dragEnabled; }

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

    public Switch setCheckedTrackColor(int value) { requireColorMode(); checkedTrackColor = value; invalidate(); return this; }
    public Switch setUncheckedTrackColor(int value) { requireColorMode(); uncheckedTrackColor = value; invalidate(); return this; }
    public Switch setDisabledTrackColor(int value) { requireColorMode(); disabledCheckedTrackColor = value; disabledUncheckedTrackColor = value; invalidate(); return this; }
    public Switch setDisabledCheckedTrackColor(int value) { requireColorMode(); disabledCheckedTrackColor = value; invalidate(); return this; }
    public Switch setDisabledUncheckedTrackColor(int value) { requireColorMode(); disabledUncheckedTrackColor = value; invalidate(); return this; }
    public Switch setThumbColor(int value) { requireColorMode(); thumbColor = value; invalidate(); return this; }
    public Switch setDisabledThumbColor(int value) { requireColorMode(); disabledCheckedThumbColor = value; disabledUncheckedThumbColor = value; invalidate(); return this; }
    public Switch setDisabledCheckedThumbColor(int value) { requireColorMode(); disabledCheckedThumbColor = value; invalidate(); return this; }
    public Switch setDisabledUncheckedThumbColor(int value) { requireColorMode(); disabledUncheckedThumbColor = value; invalidate(); return this; }
    public Switch setDisabledStrokeColor(int value) { requireColorMode(); disabledStrokeColor = value; disabledStrokeColorExplicit = true; invalidate(); return this; }
    public Switch setDisabledAlpha(float value) { disabledAlpha = requireAlpha(value); disabledAlphaExplicit = true; invalidate(); return this; }

    public Switch setTrackCornerRadius(float value) { requireColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackCornerRadiusPx(float value) { requireColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackCornerRadiusAuto() { requireColorMode(); cornerRadiusAuto = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbPadding(float value) { requireSeparateThumbMode(); thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbPaddingPx(float value) { requireSeparateThumbMode(); thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbSize(float value) { requireSeparateThumbMode(); thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbSizePx(float value) { requireSeparateThumbMode(); thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setThumbSizeAuto() { requireSeparateThumbMode(); thumbSizeAuto = true; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackStroke(float width, int color) { requireColorMode(); strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = false; strokeEnabled = width > 0f; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackStrokePx(float width, int color) { requireColorMode(); strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = true; strokeEnabled = width > 0f; rebuildGeometry(); invalidate(); return this; }
    public Switch setTrackStrokeEnabled(boolean value) { requireColorMode(); strokeEnabled = value; invalidate(); return this; }
    public Switch setThumbShadow(DropShadow value) { requireSeparateThumbMode(); thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = false; invalidate(); return this; }
    public Switch setThumbShadowPx(DropShadow value) { requireSeparateThumbMode(); thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = true; invalidate(); return this; }
    public Switch setThumbShadowEnabled(boolean value) { requireSeparateThumbMode(); thumbShadowEnabled = value; invalidate(); return this; }
    public Switch setDisabledThumbShadowEnabled(boolean value) { requireSeparateThumbMode(); disabledThumbShadowEnabled = value; invalidate(); return this; }

    public Switch setSwitchImages(SwitchImages value) {
        ensureActive();
        cancelTouch();
        state.cancelAnimation();
        switchImages = Objects.requireNonNull(value, "Switch images cannot be null.");
        switchImages.validateActive();
        renderMode = switchImages.getMode() == SwitchImages.Mode.COMPLEX
                ? RenderMode.COMPLEX_IMAGE : RenderMode.SIMPLE_IMAGE;
        if (!disabledAlphaExplicit) disabledAlpha = 1f;
        rebuildRenderer();
        dragEnabled = renderer.supportsDrag();
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Switch useColorRendering() {
        ensureActive(); cancelTouch(); state.cancelAnimation();
        switchImages = null; renderMode = RenderMode.COLOR;
        if (!disabledAlphaExplicit) disabledAlpha = DEFAULT_DISABLED_ALPHA;
        dragEnabled = true;
        rebuildRenderer(); rebuildGeometry(); invalidate(); return this;
    }

    public Switch setTrackImageScaleType(Image.ScaleType value) {
        requireMode(RenderMode.COMPLEX_IMAGE, "Track image scale type");
        trackImageScaleType = Objects.requireNonNull(value);
        ((ComplexImageSwitchRenderer) renderer).setTrackScaleType(value); invalidate(); return this;
    }
    public Switch setThumbImageScaleType(Image.ScaleType value) {
        requireMode(RenderMode.COMPLEX_IMAGE, "Thumb image scale type");
        thumbImageScaleType = Objects.requireNonNull(value);
        ((ComplexImageSwitchRenderer) renderer).setThumbScaleType(value); invalidate(); return this;
    }
    public Switch setSwitchImageScaleType(Image.ScaleType value) {
        requireMode(RenderMode.SIMPLE_IMAGE, "Switch image scale type");
        switchImageScaleType = Objects.requireNonNull(value);
        ((SimpleImageSwitchRenderer) renderer).setScaleType(value); invalidate(); return this;
    }
    public Switch setImageTransition(ImageTransition value) {
        requireMode(RenderMode.SIMPLE_IMAGE, "Image transition");
        imageTransition = Objects.requireNonNull(value);
        ((SimpleImageSwitchRenderer) renderer).setTransition(value); invalidate(); return this;
    }
    public Switch setImageFiltering(boolean value) {
        if (renderMode == RenderMode.COLOR) throw new IllegalStateException("Image filtering is unavailable in COLOR rendering mode.");
        imageFiltering = value; renderer.setFilterBitmap(value); invalidate(); return this;
    }
    public Switch setDragEnabled(boolean value) {
        ensureActive();
        if (value && !renderer.supportsDrag()) {
            throw new IllegalStateException("Simple image mode cannot provide continuous thumb dragging. Use complex image or color mode.");
        }
        dragEnabled = value; return this;
    }

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
        populateRenderState();
        renderer.drawTrack(canvas, renderState);
        int clip = canvas.save();
        canvas.clipRect(trackBounds);
        drawRipple(canvas);
        canvas.restoreToCount(clip);
        float thumbX = thumbBounds.centerX();
        float thumbY = thumbBounds.centerY();
        float thumbRadius = thumbBounds.width() / 2f;
        if (renderer.usesSeparateThumb()
                && thumbShadowEnabled && (enabled || disabledThumbShadowEnabled)
                && thumbShadow.getColor() != Color.TRANSPARENT) {
            drawThumbShadow(canvas, thumbX, thumbY, thumbRadius);
        }
        renderer.drawThumb(canvas, renderState);
        canvas.restoreToCount(save);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!acceptsTouch(event.getX(), event.getY())) return false;
                touchCaptured = true; dragging = false; tapCancelled = false;
                downX = event.getX(); downChecked = state.isChecked();
                startRipple(event.getX(), event.getY()); return true;
            case MotionEvent.ACTION_MOVE:
                if (!touchCaptured) return false;
                if (!dragging && dragEnabled && renderer.supportsDrag()
                        && Math.abs(event.getX() - downX) > touchSlop) dragging = true;
                if (!dragEnabled && Math.abs(event.getX() - downX) > touchSlop) {
                    tapCancelled = true;
                }
                if (dragging) {
                    float travel = maxThumbCenterX - minThumbCenterX;
                    state.setDragProgress(travel <= 0f ? 0f : (event.getX() - minThumbCenterX) / travel);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!touchCaptured) return false;
                touchCaptured = false;
                if (dragging) state.commitDrag(progress >= 0.5f, true);
                else if (!tapCancelled && bounds.contains(event.getX(), event.getY())) state.setChecked(!downChecked, true, true);
                else state.setChecked(downChecked, true, false);
                dragging = false; tapCancelled = false; finishRipple(); return true;
            case MotionEvent.ACTION_CANCEL:
                boolean handled = touchCaptured;
                if (touchCaptured) state.setChecked(downChecked, true, false);
                touchCaptured = false; dragging = false; tapCancelled = false; cancelRipple(); return handled;
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
        cancelTouch(); cancelRipple(); state.release(); renderer.release();
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
        updateThumbBounds();
    }

    private void updateThumbBounds() {
        float centerX = minThumbCenterX + (maxThumbCenterX - minThumbCenterX) * progress;
        float centerY = trackBounds.centerY();
        float radius = resolvedThumbSize / 2f;
        thumbBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    private void populateRenderState() {
        updateThumbBounds();
        renderState.trackBounds.set(trackBounds);
        renderState.thumbBounds.set(thumbBounds);
        renderState.progress = progress;
        renderState.enabled = enabled;
        renderState.cornerRadius = resolvedCornerRadius;
        renderState.strokeWidth = resolvedStrokeWidth;
        renderState.strokeEnabled = strokeEnabled;
        renderState.checkedTrackColor = checkedTrackColor;
        renderState.uncheckedTrackColor = uncheckedTrackColor;
        renderState.disabledCheckedTrackColor = disabledCheckedTrackColor;
        renderState.disabledUncheckedTrackColor = disabledUncheckedTrackColor;
        renderState.thumbColor = thumbColor;
        renderState.disabledCheckedThumbColor = disabledCheckedThumbColor;
        renderState.disabledUncheckedThumbColor = disabledUncheckedThumbColor;
        renderState.strokeColor = strokeColor;
        renderState.disabledStrokeColor = disabledStrokeColor;
    }

    private void rebuildRenderer() {
        if (renderer != null) renderer.release();
        switch (renderMode) {
            case COMPLEX_IMAGE:
                switchImages.validateActive();
                renderer = new ComplexImageSwitchRenderer(
                        switchImages, trackImageScaleType, thumbImageScaleType, imageFiltering);
                break;
            case SIMPLE_IMAGE:
                switchImages.validateActive();
                renderer = new SimpleImageSwitchRenderer(
                        switchImages, switchImageScaleType, imageTransition, imageFiltering);
                break;
            default:
                renderer = new ColorSwitchRenderer();
        }
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
    private void cancelTouch() { if (touchCaptured) state.setChecked(downChecked, true, false); touchCaptured = false; dragging = false; tapCancelled = false; cancelRipple(); }
    private boolean acceptsTouch(float x, float y) { return visible && enabled && !released && bounds.contains(x, y); }
    private void runFeedback() { if (soundAction != null) soundAction.run(); if (hapticAction != null) hapticAction.run(); }
    private float resolve(float value, boolean pixels) { return pixels ? value : value * dimensionScale; }
    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
    private void invalidateOnAnimation() { if (owner != null) owner.postInvalidateComponentOnAnimation(); }
    private void ensureActive() { if (released) throw new IllegalStateException("Switch has been released: " + id); }

    private void requireColorMode() {
        ensureActive();
        if (renderMode != RenderMode.COLOR) {
            throw new IllegalStateException("This styling API is only available in COLOR rendering mode.");
        }
    }
    private void requireSeparateThumbMode() {
        ensureActive();
        if (renderMode == RenderMode.SIMPLE_IMAGE) {
            throw new IllegalStateException("Simple image mode has no separately configurable thumb.");
        }
    }
    private void requireMode(RenderMode expected, String feature) {
        ensureActive();
        if (renderMode != expected) {
            throw new IllegalStateException(feature + " requires " + expected + " rendering mode.");
        }
    }

    private static TimeInterpolator toInterpolator(Interpolator value) {
        switch (Objects.requireNonNull(value)) {
            case LINEAR: return new LinearInterpolator();
            case EASE_IN: return new AccelerateInterpolator();
            case EASE_OUT: return new DecelerateInterpolator();
            default: return new AccelerateDecelerateInterpolator();
        }
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
        private RenderMode renderMode = RenderMode.COLOR;
        private ImageTransition imageTransition = ImageTransition.CROSS_FADE;
        private SwitchImages switchImages;
        private Image.ScaleType trackImageScaleType = Image.ScaleType.FIT_XY;
        private Image.ScaleType thumbImageScaleType = Image.ScaleType.FIT_CENTER;
        private Image.ScaleType switchImageScaleType = Image.ScaleType.FIT_XY;
        private boolean imageFiltering = true;
        private boolean dragEnabled = true;
        private boolean disabledAlphaExplicit;
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
        public Builder(
                Context context,
                String id,
                SwitchImages images,
                Position position,
                Size size
        ) {
            this(context, id, position, size);
            configureImages(images);
        }
        public Builder(Context context, String id, SwitchImages images, RectF bounds) {
            this(context, id, bounds);
            configureImages(images);
        }
        public Builder setChecked(boolean value) { checked = value; return this; }
        public Builder setCheckedTrackColor(int value) { requireBuilderColorMode(); checkedTrackColor = value; return this; }
        public Builder setUncheckedTrackColor(int value) { requireBuilderColorMode(); uncheckedTrackColor = value; return this; }
        public Builder setDisabledTrackColor(int value) { requireBuilderColorMode(); disabledCheckedTrackColor = value; disabledUncheckedTrackColor = value; return this; }
        public Builder setDisabledCheckedTrackColor(int value) { requireBuilderColorMode(); disabledCheckedTrackColor = value; return this; }
        public Builder setDisabledUncheckedTrackColor(int value) { requireBuilderColorMode(); disabledUncheckedTrackColor = value; return this; }
        public Builder setThumbColor(int value) { requireBuilderColorMode(); thumbColor = value; return this; }
        public Builder setDisabledThumbColor(int value) { requireBuilderColorMode(); disabledCheckedThumbColor = value; disabledUncheckedThumbColor = value; return this; }
        public Builder setDisabledCheckedThumbColor(int value) { requireBuilderColorMode(); disabledCheckedThumbColor = value; return this; }
        public Builder setDisabledUncheckedThumbColor(int value) { requireBuilderColorMode(); disabledUncheckedThumbColor = value; return this; }
        public Builder setDisabledStrokeColor(int value) { requireBuilderColorMode(); disabledStrokeColor = value; disabledStrokeColorExplicit = true; return this; }
        public Builder setDisabledAlpha(float value) { disabledAlpha = requireAlpha(value); disabledAlphaExplicit = true; return this; }
        public Builder setTrackCornerRadius(float value) { requireBuilderColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = false; return this; }
        public Builder setTrackCornerRadiusPx(float value) { requireBuilderColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusAuto = false; cornerRadiusInPixels = true; return this; }
        public Builder setTrackCornerRadiusAuto() { requireBuilderColorMode(); cornerRadiusAuto = true; return this; }
        public Builder setThumbPadding(float value) { requireBuilderSeparateThumb(); thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = false; return this; }
        public Builder setThumbPaddingPx(float value) { requireBuilderSeparateThumb(); thumbPadding = requireNonNegative(value, "Thumb padding"); thumbPaddingInPixels = true; return this; }
        public Builder setThumbSize(float value) { requireBuilderSeparateThumb(); thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = false; return this; }
        public Builder setThumbSizePx(float value) { requireBuilderSeparateThumb(); thumbSize = requirePositive(value, "Thumb size"); thumbSizeAuto = false; thumbSizeInPixels = true; return this; }
        public Builder setThumbSizeAuto() { requireBuilderSeparateThumb(); thumbSizeAuto = true; return this; }
        public Builder setTrackStroke(float width, int color) { requireBuilderColorMode(); strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = false; strokeEnabled = width > 0f; return this; }
        public Builder setTrackStrokePx(float width, int color) { requireBuilderColorMode(); strokeWidth = requireNonNegative(width, "Stroke width"); strokeColor = color; if (!disabledStrokeColorExplicit) disabledStrokeColor = color; strokeWidthInPixels = true; strokeEnabled = width > 0f; return this; }
        public Builder setTrackStrokeEnabled(boolean value) { requireBuilderColorMode(); strokeEnabled = value; return this; }
        public Builder setThumbShadow(DropShadow value) { requireBuilderSeparateThumb(); thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = false; return this; }
        public Builder setThumbShadowPx(DropShadow value) { requireBuilderSeparateThumb(); thumbShadow = Objects.requireNonNull(value); thumbShadowInPixels = true; return this; }
        public Builder setThumbShadowEnabled(boolean value) { requireBuilderSeparateThumb(); thumbShadowEnabled = value; return this; }
        public Builder setDisabledThumbShadowEnabled(boolean value) { requireBuilderSeparateThumb(); disabledThumbShadowEnabled = value; return this; }
        public Builder setAnimationDuration(long value) { animationDuration = requireDuration(value, "Animation duration"); return this; }
        public Builder setAnimationInterpolator(Interpolator value) { animationInterpolator = Objects.requireNonNull(value); return this; }
        public Builder setRippleEnabled(boolean value) { rippleEnabled = value; return this; }
        public Builder setRippleColor(int value) { rippleColor = value; return this; }
        public Builder setRippleDuration(long value) { rippleDuration = requireDuration(value, "Ripple duration"); return this; }
        public Builder setTrackImageScaleType(Image.ScaleType value) {
            requireBuilderMode(RenderMode.COMPLEX_IMAGE, "Track image scale type");
            trackImageScaleType = Objects.requireNonNull(value); return this;
        }
        public Builder setThumbImageScaleType(Image.ScaleType value) {
            requireBuilderMode(RenderMode.COMPLEX_IMAGE, "Thumb image scale type");
            thumbImageScaleType = Objects.requireNonNull(value); return this;
        }
        public Builder setSwitchImageScaleType(Image.ScaleType value) {
            requireBuilderMode(RenderMode.SIMPLE_IMAGE, "Switch image scale type");
            switchImageScaleType = Objects.requireNonNull(value); return this;
        }
        public Builder setImageTransition(ImageTransition value) {
            requireBuilderMode(RenderMode.SIMPLE_IMAGE, "Image transition");
            imageTransition = Objects.requireNonNull(value); return this;
        }
        public Builder setImageFiltering(boolean value) {
            if (renderMode == RenderMode.COLOR) {
                throw new IllegalStateException("Image filtering is unavailable in COLOR rendering mode.");
            }
            imageFiltering = value; return this;
        }
        public Builder setDragEnabled(boolean value) {
            if (value && renderMode == RenderMode.SIMPLE_IMAGE) {
                throw new IllegalStateException("Simple image mode cannot provide continuous thumb dragging. Use complex image or color mode.");
            }
            dragEnabled = value; return this;
        }
        public Builder setSoundAction(Runnable value) { soundAction = value; return this; }
        public Builder setHapticAction(Runnable value) { hapticAction = value; return this; }
        public Builder setAlpha(float value) { alpha = requireAlpha(value); return this; }
        public Builder setVisible(boolean value) { visible = value; return this; }
        public Builder setEnabled(boolean value) { enabled = value; return this; }
        public Builder horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder verticalCenter(boolean value) { verticalCentered = value; return this; }
        public Builder setOnCheckedChangeListener(OnCheckedChangeListener value) { checkedChangeListener = value; return this; }
        @Override public Switch build(View hostView) { return new Switch(this, hostView); }

        private void configureImages(SwitchImages value) {
            switchImages = Objects.requireNonNull(value, "Switch images cannot be null.");
            switchImages.validateActive();
            renderMode = switchImages.getMode() == SwitchImages.Mode.COMPLEX
                    ? RenderMode.COMPLEX_IMAGE : RenderMode.SIMPLE_IMAGE;
            disabledAlpha = 1f;
            dragEnabled = renderMode == RenderMode.COMPLEX_IMAGE;
        }
        private void requireBuilderColorMode() {
            if (renderMode != RenderMode.COLOR) {
                throw new IllegalStateException("This styling API is only available in COLOR rendering mode.");
            }
        }
        private void requireBuilderSeparateThumb() {
            if (renderMode == RenderMode.SIMPLE_IMAGE) {
                throw new IllegalStateException("Simple image mode has no separately configurable thumb.");
            }
        }
        private void requireBuilderMode(RenderMode expected, String feature) {
            if (renderMode != expected) {
                throw new IllegalStateException(feature + " requires " + expected + " rendering mode.");
            }
        }
    }
}

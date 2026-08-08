package com.ogfa.nativeviews.radiobutton;

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

import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.radiobutton.internal.ColorRadioRenderer;
import com.ogfa.nativeviews.radiobutton.internal.ImageRadioRenderer;
import com.ogfa.nativeviews.radiobutton.internal.RadioRenderState;
import com.ogfa.nativeviews.radiobutton.internal.RadioRenderer;
import com.ogfa.nativeviews.selection.OnCheckedChangeListener;
import com.ogfa.nativeviews.selection.SelectableComponent;
import com.ogfa.nativeviews.selection.internal.CheckedStateController;

import java.util.Objects;

/** Native Canvas radio control with optional RadioSelection mutual exclusion. */
public final class RadioButton implements SelectableComponent {
    public enum RenderMode { COLOR, IMAGE }
    public enum ImageTransition { CROSS_FADE, SNAP }
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

    private static final long DEFAULT_SELECTION_DURATION = 160L;
    private static final long DEFAULT_PRESS_DURATION = 100L;
    private static final long DEFAULT_RIPPLE_DURATION = 240L;
    private static final float DEFAULT_PRESSED_SCALE = 0.92f;
    private static final float DEFAULT_DISABLED_ALPHA = 0.65f;

    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RadioRenderState renderState = new RadioRenderState();
    private final CheckedStateController checkedState;
    private final int touchSlop;

    private ComponentHost owner;
    private FigmaConfig figmaConfig;
    private float dimensionScale;
    private RadioSelection selection;
    private OnCheckedChangeListener checkedChangeListener;
    private OnRadioClickListener clickListener;
    private boolean suppressCheckedCallback;

    private RenderMode renderMode;
    private RadioButtonImages images;
    private RadioRenderer renderer;
    private Image.ScaleType imageScaleType;
    private ImageTransition imageTransition;
    private boolean imageFiltering;

    private int checkedColor;
    private int uncheckedColor;
    private int dotColor;
    private int backgroundColor;
    private int disabledCheckedColor;
    private int disabledUncheckedColor;
    private int disabledDotColor;
    private int disabledBackgroundColor;
    private float ringWidth;
    private boolean ringWidthInPixels;
    private float dotSize;
    private boolean dotSizeInPixels;
    private float padding;
    private boolean paddingInPixels;
    private float resolvedRingWidth;
    private float resolvedDotSize;
    private float resolvedPadding;

    private boolean visible;
    private boolean enabled;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private boolean released;
    private float alpha;
    private float disabledAlpha;
    private Interpolator animationInterpolator;

    private boolean touchCaptured;
    private boolean touchCancelled;
    private float downX;
    private float downY;
    private float pressedScale;
    private float currentPressedScale = 1f;
    private long pressAnimationDuration;
    private ValueAnimator pressAnimator;
    private boolean rippleEnabled;
    private int rippleColor;
    private long rippleDuration;
    private float rippleX;
    private float rippleY;
    private float rippleProgress;
    private ValueAnimator rippleAnimator;
    private Runnable soundAction;
    private Runnable hapticAction;

    private RadioButton(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        id = requireId(builder.id);
        touchSlop = ViewConfiguration.get(builder.context).getScaledTouchSlop();
        checkedChangeListener = builder.checkedChangeListener;
        clickListener = builder.clickListener;
        renderMode = builder.renderMode;
        images = builder.images;
        imageScaleType = builder.imageScaleType;
        imageTransition = builder.imageTransition;
        imageFiltering = builder.imageFiltering;
        checkedColor = builder.checkedColor;
        uncheckedColor = builder.uncheckedColor;
        dotColor = builder.dotColor;
        backgroundColor = builder.backgroundColor;
        disabledCheckedColor = builder.disabledCheckedColor;
        disabledUncheckedColor = builder.disabledUncheckedColor;
        disabledDotColor = builder.disabledDotColor;
        disabledBackgroundColor = builder.disabledBackgroundColor;
        ringWidth = builder.ringWidth;
        ringWidthInPixels = builder.ringWidthInPixels;
        dotSize = builder.dotSize;
        dotSizeInPixels = builder.dotSizeInPixels;
        padding = builder.padding;
        paddingInPixels = builder.paddingInPixels;
        visible = builder.visible;
        enabled = builder.enabled;
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        alpha = builder.alpha;
        disabledAlpha = builder.disabledAlpha;
        animationInterpolator = builder.animationInterpolator;
        pressedScale = builder.pressedScale;
        pressAnimationDuration = builder.pressAnimationDuration;
        rippleEnabled = builder.rippleEnabled;
        rippleColor = builder.rippleColor;
        rippleDuration = builder.rippleDuration;
        soundAction = builder.soundAction;
        hapticAction = builder.hapticAction;
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        checkedState = new CheckedStateController(
                builder.checked,
                builder.selectionAnimationDuration,
                toInterpolator(animationInterpolator),
                new CheckedStateController.Callback() {
                    @Override public void onProgressChanged(float progress) {
                        invalidateOnAnimation();
                    }
                    @Override public void onCheckedChanged(boolean checked, boolean fromUser) {
                        if (!suppressCheckedCallback && checkedChangeListener != null) {
                            checkedChangeListener.onCheckedChanged(id, checked, fromUser);
                        }
                    }
                }
        );
        rebuildRenderer();
        rebuildGeometry();
        if (builder.selection != null) attachSelection(builder.selection);
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() { return new RectF(bounds); }
    public FigmaConfig getFigmaConfig() { return figmaConfig; }
    public float getDimensionScale() { return dimensionScale; }
    @Override public boolean isChecked() { ensureActive(); return checkedState.isChecked(); }
    @Override public boolean isVisible() { return visible; }
    @Override public boolean isEnabled() { return enabled; }
    public boolean isAnimating() { return checkedState.isAnimating(); }
    public float getSelectionProgress() { return checkedState.getProgress(); }
    public RadioSelection getSelection() { return selection; }
    public RenderMode getRenderMode() { return renderMode; }
    public RadioButtonImages getRadioButtonImages() { return images; }
    public Image.ScaleType getImageScaleType() { return imageScaleType; }
    public float getAlpha() { return alpha; }
    public float getDisabledAlpha() { return disabledAlpha; }
    public float getResolvedRingWidth() { return resolvedRingWidth; }
    public float getResolvedDotSize() { return resolvedDotSize; }
    public float getResolvedPadding() { return resolvedPadding; }

    public RadioButton select() { return setChecked(true); }
    @Override public RadioButton setChecked(boolean value) {
        return setCheckedInternal(value, true, true, false);
    }
    public RadioButton setChecked(boolean value, boolean notifyListener) {
        return setCheckedInternal(value, true, notifyListener, false);
    }
    @Override public RadioButton setCheckedImmediately(boolean value) {
        return setCheckedInternal(value, false, true, false);
    }
    public RadioButton setCheckedImmediately(boolean value, boolean notifyListener) {
        return setCheckedInternal(value, false, notifyListener, false);
    }
    @Override public RadioButton toggle() { return setChecked(!isChecked()); }
    @Override public RadioButton toggleImmediately() {
        return setCheckedImmediately(!isChecked());
    }
    @Override public RadioButton setOnCheckedChangeListener(OnCheckedChangeListener value) {
        ensureActive(); checkedChangeListener = value; return this;
    }
    @Override public RadioButton removeOnCheckedChangeListener() {
        return setOnCheckedChangeListener(null);
    }
    public RadioButton setOnClickListener(OnRadioClickListener value) {
        ensureActive(); clickListener = value; return this;
    }
    public RadioButton removeOnClickListener() { return setOnClickListener(null); }

    public RadioButton setSelection(RadioSelection value) {
        ensureActive(); value = Objects.requireNonNull(value, "RadioSelection cannot be null.");
        if (selection == value) return this;
        if (selection != null) selection.unregister(this);
        attachSelection(value); return this;
    }
    public RadioButton removeFromSelection() {
        ensureActive();
        if (selection != null) {
            RadioSelection old = selection; selection = null; old.unregister(this);
        }
        return this;
    }

    public RadioButton setRegion(Position position, Size size) {
        ensureActive(); resolveRegion(Objects.requireNonNull(position), Objects.requireNonNull(size), null);
        applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public RadioButton setRegion(RectF value) {
        ensureActive(); resolveRegion(null, null, Objects.requireNonNull(value));
        applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public RadioButton horizontalCenter(boolean value) {
        ensureActive(); horizontalCentered = value; applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public RadioButton verticalCenter(boolean value) {
        ensureActive(); verticalCentered = value; applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public RadioButton setVisible(boolean value) {
        ensureActive(); visible = value; if (!value) cancelTouch(); invalidate(); return this;
    }
    public RadioButton setEnabled(boolean value) {
        ensureActive();
        if (enabled == value) return this;
        enabled = value; if (!value) cancelTouch();
        if (selection != null) selection.onButtonEnabledChanged(this);
        invalidate(); return this;
    }
    public RadioButton setAlpha(float value) { ensureActive(); alpha = requireAlpha(value); invalidate(); return this; }
    public RadioButton setDisabledAlpha(float value) { ensureActive(); disabledAlpha = requireAlpha(value); invalidate(); return this; }

    public RadioButton setCheckedColor(int value) { requireColorMode(); checkedColor = value; invalidate(); return this; }
    public RadioButton setUncheckedColor(int value) { requireColorMode(); uncheckedColor = value; invalidate(); return this; }
    public RadioButton setDotColor(int value) { requireColorMode(); dotColor = value; invalidate(); return this; }
    public RadioButton setBackgroundColor(int value) { requireColorMode(); backgroundColor = value; invalidate(); return this; }
    public RadioButton setDisabledCheckedColor(int value) { requireColorMode(); disabledCheckedColor = value; invalidate(); return this; }
    public RadioButton setDisabledUncheckedColor(int value) { requireColorMode(); disabledUncheckedColor = value; invalidate(); return this; }
    public RadioButton setDisabledDotColor(int value) { requireColorMode(); disabledDotColor = value; invalidate(); return this; }
    public RadioButton setDisabledBackgroundColor(int value) { requireColorMode(); disabledBackgroundColor = value; invalidate(); return this; }
    public RadioButton setRingWidth(float value) { requireColorMode(); ringWidth = requirePositive(value, "Ring width"); ringWidthInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public RadioButton setRingWidthPx(float value) { requireColorMode(); ringWidth = requirePositive(value, "Ring width"); ringWidthInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public RadioButton setDotSize(float value) { requireColorMode(); dotSize = requirePositive(value, "Dot size"); dotSizeInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public RadioButton setDotSizePx(float value) { requireColorMode(); dotSize = requirePositive(value, "Dot size"); dotSizeInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public RadioButton setPadding(float value) { requireColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public RadioButton setPaddingPx(float value) { requireColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = true; rebuildGeometry(); invalidate(); return this; }

    public RadioButton setRadioButtonImages(RadioButtonImages value) {
        ensureActive(); images = Objects.requireNonNull(value, "RadioButton images cannot be null.");
        images.validateActive(); renderMode = RenderMode.IMAGE; disabledAlpha = 1f;
        rebuildRenderer(); invalidate(); return this;
    }
    public RadioButton useColorRendering() {
        ensureActive(); images = null; renderMode = RenderMode.COLOR;
        rebuildRenderer(); invalidate(); return this;
    }
    public RadioButton setImageScaleType(Image.ScaleType value) {
        requireImageMode(); imageScaleType = Objects.requireNonNull(value);
        ((ImageRadioRenderer) renderer).setScaleType(value); invalidate(); return this;
    }
    public RadioButton setImageTransition(ImageTransition value) {
        requireImageMode(); imageTransition = Objects.requireNonNull(value);
        ((ImageRadioRenderer) renderer).setTransition(value); invalidate(); return this;
    }
    public RadioButton setImageFiltering(boolean value) {
        requireImageMode(); imageFiltering = value; renderer.setImageFiltering(value); invalidate(); return this;
    }

    public RadioButton setSelectionAnimationDuration(long value) {
        ensureActive(); checkedState.setDuration(requireDuration(value, "Selection animation duration")); return this;
    }
    public RadioButton setAnimationInterpolator(Interpolator value) {
        ensureActive(); animationInterpolator = Objects.requireNonNull(value);
        checkedState.setInterpolator(toInterpolator(value)); return this;
    }
    public RadioButton finishAnimation() { ensureActive(); checkedState.finishAnimation(); return this; }
    public RadioButton cancelAnimation() { ensureActive(); checkedState.cancelAnimation(); return this; }
    public RadioButton setPressedScale(float value) { ensureActive(); pressedScale = requireScale(value); return this; }
    public RadioButton setPressAnimationDuration(long value) { ensureActive(); pressAnimationDuration = requireDuration(value, "Press animation duration"); return this; }
    public RadioButton setRippleEnabled(boolean value) { ensureActive(); rippleEnabled = value; if (!value) cancelRipple(); invalidate(); return this; }
    public RadioButton setRippleColor(int value) { ensureActive(); rippleColor = value; invalidate(); return this; }
    public RadioButton setRippleDuration(long value) { ensureActive(); rippleDuration = requireDuration(value, "Ripple duration"); return this; }
    public RadioButton setSoundAction(Runnable value) { ensureActive(); soundAction = value; return this; }
    public RadioButton setHapticAction(Runnable value) { ensureActive(); hapticAction = value; return this; }

    @Override public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || alpha <= 0f) return;
        float effectiveAlpha = alpha * (enabled ? 1f : disabledAlpha);
        int save = effectiveAlpha < 1f
                ? canvas.saveLayerAlpha(bounds, Math.round(255f * effectiveAlpha))
                : canvas.save();
        canvas.scale(currentPressedScale, currentPressedScale, bounds.centerX(), bounds.centerY());
        populateRenderState(); renderer.draw(canvas, renderState); drawRipple(canvas);
        canvas.restoreToCount(save);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!acceptsTouch(event.getX(), event.getY())) return false;
                touchCaptured = true; touchCancelled = false;
                downX = event.getX(); downY = event.getY();
                animatePressTo(pressedScale); startRipple(downX, downY); return true;
            case MotionEvent.ACTION_MOVE:
                if (!touchCaptured) return false;
                if (Math.abs(event.getX() - downX) > touchSlop
                        || Math.abs(event.getY() - downY) > touchSlop
                        || !bounds.contains(event.getX(), event.getY())) touchCancelled = true;
                return true;
            case MotionEvent.ACTION_UP:
                if (!touchCaptured) return false;
                boolean click = !touchCancelled && bounds.contains(event.getX(), event.getY());
                touchCaptured = false; touchCancelled = false;
                animatePressTo(1f); finishRipple();
                if (click) {
                    if (clickListener != null) clickListener.onClick(id);
                    setCheckedInternal(true, true, true, true);
                    runFeedback();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                boolean handled = touchCaptured;
                touchCaptured = false; touchCancelled = false;
                animatePressTo(1f); cancelRipple(); return handled;
            default: return touchCaptured;
        }
    }

    @Override public void attach(ComponentHost value) {
        ensureActive(); Objects.requireNonNull(value, "Component host cannot be null.");
        if (owner != null && owner != value) {
            throw new IllegalStateException("RadioButton already belongs to another host.");
        }
        owner = value; applyParentAlignment(); rebuildGeometry();
    }

    @Override public void release() {
        if (released) return;
        cancelTouch(); cancelPressAnimation(); cancelRipple();
        if (selection != null) {
            RadioSelection old = selection; selection = null; old.unregister(this);
        }
        checkedState.release(); renderer.release();
        checkedChangeListener = null; clickListener = null;
        soundAction = null; hapticAction = null; owner = null; released = true;
    }

    void applyCheckedFromSelection(
            boolean value, boolean animate, boolean fromUser, boolean notifyListener) {
        applyLocalChecked(value, animate, fromUser, notifyListener);
    }
    void detachSelectionFromController(RadioSelection controller) {
        if (selection == controller) selection = null;
    }

    private RadioButton setCheckedInternal(
            boolean value, boolean animate, boolean notifyListener, boolean fromUser) {
        ensureActive();
        if (selection != null) {
            selection.requestChecked(this, value, animate, notifyListener, fromUser);
        } else {
            applyLocalChecked(value, animate, fromUser, notifyListener);
        }
        return this;
    }
    private void applyLocalChecked(
            boolean value, boolean animate, boolean fromUser, boolean notifyListener) {
        suppressCheckedCallback = !notifyListener;
        try {
            checkedState.setChecked(value, animate, fromUser);
        } finally {
            suppressCheckedCallback = false;
        }
    }
    private void attachSelection(RadioSelection value) {
        selection = value;
        try {
            value.register(this, checkedState.isChecked());
        } catch (RuntimeException error) {
            selection = null; throw error;
        }
    }

    private void resolveRegion(Position position, Size size, RectF explicit) {
        if (explicit != null) {
            requireBounds(explicit); baseBounds.set(explicit);
            figmaConfig = FigmaConfig.getDefault();
            dimensionScale = figmaConfig.getScale(hostView.getWidth());
        } else {
            Objects.requireNonNull(position, "Position cannot be null.");
            Objects.requireNonNull(size, "Size cannot be null.");
            baseBounds.set(position.toRectF(hostView, size));
            figmaConfig = position.getFigmaConfig();
            dimensionScale = position.getScale(hostView);
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
        resolvedRingWidth = resolve(ringWidth, ringWidthInPixels);
        resolvedDotSize = resolve(dotSize, dotSizeInPixels);
        resolvedPadding = resolve(padding, paddingInPixels);
        float radius = Math.min(bounds.width(), bounds.height()) / 2f - resolvedPadding;
        if (!(radius > 0f)) throw new IllegalArgumentException("RadioButton padding leaves no circle region.");
        if (resolvedRingWidth > radius) throw new IllegalArgumentException("RadioButton ring width exceeds its radius.");
        float maxDot = 2f * Math.max(0f, radius - resolvedRingWidth);
        if (resolvedDotSize > maxDot + 0.001f) {
            throw new IllegalArgumentException("RadioButton dot does not fit inside the ring.");
        }
    }
    private void populateRenderState() {
        renderState.bounds.set(bounds);
        renderState.progress = checkedState.getProgress();
        renderState.enabled = enabled;
        renderState.ringWidth = resolvedRingWidth;
        renderState.dotSize = resolvedDotSize;
        renderState.padding = resolvedPadding;
        renderState.checkedColor = checkedColor;
        renderState.uncheckedColor = uncheckedColor;
        renderState.dotColor = dotColor;
        renderState.backgroundColor = backgroundColor;
        renderState.disabledCheckedColor = disabledCheckedColor;
        renderState.disabledUncheckedColor = disabledUncheckedColor;
        renderState.disabledDotColor = disabledDotColor;
        renderState.disabledBackgroundColor = disabledBackgroundColor;
    }
    private void rebuildRenderer() {
        if (renderer != null) renderer.release();
        renderer = renderMode == RenderMode.IMAGE
                ? new ImageRadioRenderer(images, imageScaleType, imageTransition, imageFiltering)
                : new ColorRadioRenderer();
    }

    private void animatePressTo(float target) {
        cancelPressAnimation();
        if (pressAnimationDuration == 0L) {
            currentPressedScale = target; invalidate(); return;
        }
        pressAnimator = ValueAnimator.ofFloat(currentPressedScale, target);
        pressAnimator.setDuration(pressAnimationDuration);
        pressAnimator.setInterpolator(new DecelerateInterpolator());
        pressAnimator.addUpdateListener(value -> {
            currentPressedScale = (float) value.getAnimatedValue(); invalidateOnAnimation();
        });
        pressAnimator.start();
    }
    private void startRipple(float x, float y) {
        if (!rippleEnabled) return;
        cancelRipple(); rippleX = x; rippleY = y; rippleProgress = 0f;
        rippleAnimator = ValueAnimator.ofFloat(0f, 1f);
        rippleAnimator.setDuration(rippleDuration);
        rippleAnimator.setInterpolator(new DecelerateInterpolator());
        rippleAnimator.addUpdateListener(value -> {
            rippleProgress = (float) value.getAnimatedValue(); invalidateOnAnimation();
        });
        rippleAnimator.start();
    }
    private void drawRipple(Canvas canvas) {
        if (!rippleEnabled || rippleAnimator == null) return;
        int save = canvas.save(); canvas.clipRect(bounds);
        float radius = (float) Math.hypot(
                Math.max(rippleX - bounds.left, bounds.right - rippleX),
                Math.max(rippleY - bounds.top, bounds.bottom - rippleY));
        int sourceAlpha = Color.alpha(rippleColor);
        ripplePaint.setColor((rippleColor & 0x00ffffff)
                | (Math.round(sourceAlpha * (1f - rippleProgress)) << 24));
        canvas.drawCircle(rippleX, rippleY, radius * rippleProgress, ripplePaint);
        canvas.restoreToCount(save);
    }
    private void finishRipple() {
        if (rippleAnimator != null && rippleAnimator.isRunning()) return;
        cancelRipple();
    }
    private void cancelTouch() {
        touchCaptured = false; touchCancelled = false;
        animatePressTo(1f); cancelRipple();
    }
    private void cancelPressAnimation() {
        if (pressAnimator != null) pressAnimator.cancel(); pressAnimator = null;
    }
    private void cancelRipple() {
        if (rippleAnimator != null) rippleAnimator.cancel();
        rippleAnimator = null; rippleProgress = 0f;
    }
    private boolean acceptsTouch(float x, float y) {
        return visible && enabled && !released && bounds.contains(x, y);
    }
    private void runFeedback() {
        if (soundAction != null) soundAction.run();
        if (hapticAction != null) hapticAction.run();
    }
    private float resolve(float value, boolean pixels) { return pixels ? value : value * dimensionScale; }
    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
    private void invalidateOnAnimation() { if (owner != null) owner.postInvalidateComponentOnAnimation(); }
    private void ensureActive() { if (released) throw new IllegalStateException("RadioButton has been released: " + id); }
    private void requireColorMode() {
        ensureActive();
        if (renderMode != RenderMode.COLOR) throw new IllegalStateException("This styling API requires COLOR rendering mode.");
    }
    private void requireImageMode() {
        ensureActive();
        if (renderMode != RenderMode.IMAGE) throw new IllegalStateException("This API requires IMAGE rendering mode.");
    }

    private static TimeInterpolator toInterpolator(Interpolator value) {
        switch (Objects.requireNonNull(value)) {
            case LINEAR: return new LinearInterpolator();
            case EASE_IN: return new AccelerateInterpolator();
            case EASE_OUT: return new DecelerateInterpolator();
            default: return new AccelerateDecelerateInterpolator();
        }
    }
    private static String requireId(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("RadioButton ID cannot be blank.");
        return value.trim();
    }
    private static void requireBounds(RectF value) {
        if (value.width() <= 0f || value.height() <= 0f) throw new IllegalArgumentException("RadioButton bounds must be positive.");
    }
    private static float requireAlpha(float value) {
        if (!Float.isFinite(value) || value < 0f || value > 1f) throw new IllegalArgumentException("Alpha must be in [0, 1].");
        return value;
    }
    private static float requireNonNegative(float value, String label) {
        if (!Float.isFinite(value) || value < 0f) throw new IllegalArgumentException(label + " must be non-negative and finite.");
        return value;
    }
    private static float requirePositive(float value, String label) {
        if (!Float.isFinite(value) || value <= 0f) throw new IllegalArgumentException(label + " must be positive and finite.");
        return value;
    }
    private static float requireScale(float value) {
        if (!Float.isFinite(value) || value <= 0f || value > 1f) throw new IllegalArgumentException("Pressed scale must be in (0, 1].");
        return value;
    }
    private static long requireDuration(long value, String label) {
        if (value < 0L) throw new IllegalArgumentException(label + " cannot be negative.");
        return value;
    }

    public static final class Builder implements ComponentFactory<RadioButton> {
        private final Context context;
        private final String id;
        private Position position;
        private Size size;
        private RectF explicitBounds;
        private boolean checked;
        private RadioSelection selection;
        private int checkedColor = 0xff019cc4;
        private int uncheckedColor = 0xff656565;
        private int dotColor = 0xff019cc4;
        private int backgroundColor = Color.TRANSPARENT;
        private int disabledCheckedColor = 0xff9e9e9e;
        private int disabledUncheckedColor = 0xffbdbdbd;
        private int disabledDotColor = 0xffeeeeee;
        private int disabledBackgroundColor = Color.TRANSPARENT;
        private float ringWidth = 3f;
        private boolean ringWidthInPixels;
        private float dotSize = 22f;
        private boolean dotSizeInPixels;
        private float padding = 3f;
        private boolean paddingInPixels;
        private long selectionAnimationDuration = DEFAULT_SELECTION_DURATION;
        private Interpolator animationInterpolator = Interpolator.EASE_OUT;
        private float pressedScale = DEFAULT_PRESSED_SCALE;
        private long pressAnimationDuration = DEFAULT_PRESS_DURATION;
        private boolean rippleEnabled;
        private int rippleColor = 0x33019cc4;
        private long rippleDuration = DEFAULT_RIPPLE_DURATION;
        private float alpha = 1f;
        private float disabledAlpha = DEFAULT_DISABLED_ALPHA;
        private boolean visible = true;
        private boolean enabled = true;
        private boolean horizontalCentered;
        private boolean verticalCentered;
        private RenderMode renderMode = RenderMode.COLOR;
        private RadioButtonImages images;
        private Image.ScaleType imageScaleType = Image.ScaleType.FIT_CENTER;
        private ImageTransition imageTransition = ImageTransition.CROSS_FADE;
        private boolean imageFiltering = true;
        private OnCheckedChangeListener checkedChangeListener;
        private OnRadioClickListener clickListener;
        private Runnable soundAction;
        private Runnable hapticAction;

        public Builder(Context context, String id, Position position, Size size) {
            this.context = Objects.requireNonNull(context, "Context cannot be null.");
            this.id = requireId(id);
            this.position = Objects.requireNonNull(position, "Position cannot be null.");
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
        }
        public Builder(Context context, String id, RectF bounds) {
            this.context = Objects.requireNonNull(context, "Context cannot be null.");
            this.id = requireId(id);
            explicitBounds = new RectF(Objects.requireNonNull(bounds, "Bounds cannot be null."));
            requireBounds(explicitBounds);
        }
        public Builder(Context context, String id, RadioButtonImages images, Position position, Size size) {
            this(context, id, position, size); configureImages(images);
        }
        public Builder(Context context, String id, RadioButtonImages images, RectF bounds) {
            this(context, id, bounds); configureImages(images);
        }
        public Builder setChecked(boolean value) { checked = value; return this; }
        public Builder setSelection(RadioSelection value) { selection = Objects.requireNonNull(value); return this; }
        public Builder setCheckedColor(int value) { requireBuilderColorMode(); checkedColor = value; return this; }
        public Builder setUncheckedColor(int value) { requireBuilderColorMode(); uncheckedColor = value; return this; }
        public Builder setDotColor(int value) { requireBuilderColorMode(); dotColor = value; return this; }
        public Builder setBackgroundColor(int value) { requireBuilderColorMode(); backgroundColor = value; return this; }
        public Builder setDisabledCheckedColor(int value) { requireBuilderColorMode(); disabledCheckedColor = value; return this; }
        public Builder setDisabledUncheckedColor(int value) { requireBuilderColorMode(); disabledUncheckedColor = value; return this; }
        public Builder setDisabledDotColor(int value) { requireBuilderColorMode(); disabledDotColor = value; return this; }
        public Builder setDisabledBackgroundColor(int value) { requireBuilderColorMode(); disabledBackgroundColor = value; return this; }
        public Builder setRingWidth(float value) { requireBuilderColorMode(); ringWidth = requirePositive(value, "Ring width"); ringWidthInPixels = false; return this; }
        public Builder setRingWidthPx(float value) { requireBuilderColorMode(); ringWidth = requirePositive(value, "Ring width"); ringWidthInPixels = true; return this; }
        public Builder setDotSize(float value) { requireBuilderColorMode(); dotSize = requirePositive(value, "Dot size"); dotSizeInPixels = false; return this; }
        public Builder setDotSizePx(float value) { requireBuilderColorMode(); dotSize = requirePositive(value, "Dot size"); dotSizeInPixels = true; return this; }
        public Builder setPadding(float value) { requireBuilderColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = false; return this; }
        public Builder setPaddingPx(float value) { requireBuilderColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = true; return this; }
        public Builder setSelectionAnimationDuration(long value) { selectionAnimationDuration = requireDuration(value, "Selection animation duration"); return this; }
        public Builder setAnimationInterpolator(Interpolator value) { animationInterpolator = Objects.requireNonNull(value); return this; }
        public Builder setPressedScale(float value) { pressedScale = requireScale(value); return this; }
        public Builder setPressAnimationDuration(long value) { pressAnimationDuration = requireDuration(value, "Press animation duration"); return this; }
        public Builder setRippleEnabled(boolean value) { rippleEnabled = value; return this; }
        public Builder setRippleColor(int value) { rippleColor = value; return this; }
        public Builder setRippleDuration(long value) { rippleDuration = requireDuration(value, "Ripple duration"); return this; }
        public Builder setImageScaleType(Image.ScaleType value) { requireBuilderImageMode(); imageScaleType = Objects.requireNonNull(value); return this; }
        public Builder setImageTransition(ImageTransition value) { requireBuilderImageMode(); imageTransition = Objects.requireNonNull(value); return this; }
        public Builder setImageFiltering(boolean value) { requireBuilderImageMode(); imageFiltering = value; return this; }
        public Builder setAlpha(float value) { alpha = requireAlpha(value); return this; }
        public Builder setDisabledAlpha(float value) { disabledAlpha = requireAlpha(value); return this; }
        public Builder setVisible(boolean value) { visible = value; return this; }
        public Builder setEnabled(boolean value) { enabled = value; return this; }
        public Builder horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder verticalCenter(boolean value) { verticalCentered = value; return this; }
        public Builder setOnCheckedChangeListener(OnCheckedChangeListener value) { checkedChangeListener = value; return this; }
        public Builder setOnClickListener(OnRadioClickListener value) { clickListener = value; return this; }
        public Builder setSoundAction(Runnable value) { soundAction = value; return this; }
        public Builder setHapticAction(Runnable value) { hapticAction = value; return this; }
        @Override public RadioButton build(View hostView) { return new RadioButton(this, hostView); }

        private void configureImages(RadioButtonImages value) {
            images = Objects.requireNonNull(value, "RadioButton images cannot be null.");
            images.validateActive(); renderMode = RenderMode.IMAGE; disabledAlpha = 1f;
        }
        private void requireBuilderColorMode() {
            if (renderMode != RenderMode.COLOR) throw new IllegalStateException("This styling API requires COLOR rendering mode.");
        }
        private void requireBuilderImageMode() {
            if (renderMode != RenderMode.IMAGE) throw new IllegalStateException("This API requires IMAGE rendering mode.");
        }
    }
}

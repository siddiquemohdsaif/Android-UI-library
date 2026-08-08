package com.ogfa.nativeviews.checkbox;

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

import com.ogfa.nativeviews.checkbox.internal.CheckBoxRenderState;
import com.ogfa.nativeviews.checkbox.internal.CheckBoxRenderer;
import com.ogfa.nativeviews.checkbox.internal.ColorCheckBoxRenderer;
import com.ogfa.nativeviews.checkbox.internal.ImageCheckBoxRenderer;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.selection.OnCheckedChangeListener;
import com.ogfa.nativeviews.selection.SelectableComponent;

import java.util.EnumSet;
import java.util.Objects;

/** Native Canvas CheckBox with two/three-state interaction and color/image rendering. */
public final class CheckBox implements SelectableComponent {
    public enum State { UNCHECKED, CHECKED, INDETERMINATE }
    public enum RenderMode { COLOR, IMAGE }
    public enum ImageTransition { CROSS_FADE, SNAP }
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

    private static final long DEFAULT_STATE_DURATION = 160L;
    private static final long DEFAULT_PRESS_DURATION = 100L;
    private static final long DEFAULT_RIPPLE_DURATION = 240L;
    private static final float DEFAULT_PRESSED_SCALE = 0.92f;
    private static final float DEFAULT_DISABLED_ALPHA = 0.65f;

    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final CheckBoxRenderState renderState = new CheckBoxRenderState();
    private final int touchSlop;

    private ComponentHost owner;
    private FigmaConfig figmaConfig;
    private float dimensionScale;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private boolean visible;
    private boolean enabled;
    private boolean released;
    private float alpha;
    private float disabledAlpha;

    private State stateValue;
    private State fromState;
    private State[] toggleOrder;
    private boolean indeterminateEnabled;
    private float transitionProgress = 1f;
    private long stateAnimationDuration;
    private Interpolator animationInterpolator;
    private ValueAnimator stateAnimator;
    private OnCheckedChangeListener checkedChangeListener;
    private OnStateChangeListener stateChangeListener;

    private RenderMode renderMode;
    private CheckBoxImages images;
    private CheckBoxRenderer renderer;
    private Image.ScaleType imageScaleType;
    private ImageTransition imageTransition;
    private boolean imageFiltering;

    private int checkedColor;
    private int uncheckedColor;
    private int indeterminateColor;
    private int disabledCheckedColor;
    private int disabledUncheckedColor;
    private int disabledIndeterminateColor;
    private int checkMarkColor;
    private int disabledCheckMarkColor;
    private int strokeColor;
    private int disabledStrokeColor;
    private float cornerRadius;
    private boolean cornerRadiusInPixels;
    private float strokeWidth;
    private boolean strokeWidthInPixels;
    private float checkMarkWidth;
    private boolean checkMarkWidthInPixels;
    private float padding;
    private boolean paddingInPixels;
    private float resolvedCornerRadius;
    private float resolvedStrokeWidth;
    private float resolvedCheckMarkWidth;
    private float resolvedPadding;

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

    private CheckBox(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        id = requireId(builder.id);
        touchSlop = ViewConfiguration.get(builder.context).getScaledTouchSlop();
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        visible = builder.visible;
        enabled = builder.enabled;
        alpha = builder.alpha;
        disabledAlpha = builder.disabledAlpha;
        stateValue = builder.state;
        fromState = stateValue;
        toggleOrder = builder.toggleOrder.clone();
        indeterminateEnabled = builder.indeterminateEnabled;
        stateAnimationDuration = builder.stateAnimationDuration;
        animationInterpolator = builder.animationInterpolator;
        checkedChangeListener = builder.checkedChangeListener;
        stateChangeListener = builder.stateChangeListener;
        renderMode = builder.renderMode;
        images = builder.images;
        imageScaleType = builder.imageScaleType;
        imageTransition = builder.imageTransition;
        imageFiltering = builder.imageFiltering;
        checkedColor = builder.checkedColor;
        uncheckedColor = builder.uncheckedColor;
        indeterminateColor = builder.indeterminateColor;
        disabledCheckedColor = builder.disabledCheckedColor;
        disabledUncheckedColor = builder.disabledUncheckedColor;
        disabledIndeterminateColor = builder.disabledIndeterminateColor;
        checkMarkColor = builder.checkMarkColor;
        disabledCheckMarkColor = builder.disabledCheckMarkColor;
        strokeColor = builder.strokeColor;
        disabledStrokeColor = builder.disabledStrokeColor;
        cornerRadius = builder.cornerRadius;
        cornerRadiusInPixels = builder.cornerRadiusInPixels;
        strokeWidth = builder.strokeWidth;
        strokeWidthInPixels = builder.strokeWidthInPixels;
        checkMarkWidth = builder.checkMarkWidth;
        checkMarkWidthInPixels = builder.checkMarkWidthInPixels;
        padding = builder.padding;
        paddingInPixels = builder.paddingInPixels;
        pressedScale = builder.pressedScale;
        pressAnimationDuration = builder.pressAnimationDuration;
        rippleEnabled = builder.rippleEnabled;
        rippleColor = builder.rippleColor;
        rippleDuration = builder.rippleDuration;
        soundAction = builder.soundAction;
        hapticAction = builder.hapticAction;
        validateStateSupport(stateValue);
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        rebuildRenderer();
        rebuildGeometry();
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() { return new RectF(bounds); }
    public FigmaConfig getFigmaConfig() { return figmaConfig; }
    public float getDimensionScale() { return dimensionScale; }
    public State getState() { ensureActive(); return stateValue; }
    @Override public boolean isChecked() { ensureActive(); return stateValue == State.CHECKED; }
    public boolean isIndeterminate() { ensureActive(); return stateValue == State.INDETERMINATE; }
    public boolean isIndeterminateEnabled() { return indeterminateEnabled; }
    public boolean isAnimating() { return stateAnimator != null && stateAnimator.isRunning(); }
    public RenderMode getRenderMode() { return renderMode; }
    public CheckBoxImages getCheckBoxImages() { return images; }
    public Image.ScaleType getImageScaleType() { return imageScaleType; }
    @Override public boolean isVisible() { return visible; }
    @Override public boolean isEnabled() { return enabled; }
    public float getAlpha() { return alpha; }
    public float getDisabledAlpha() { return disabledAlpha; }
    public float getResolvedCornerRadius() { return resolvedCornerRadius; }
    public float getResolvedStrokeWidth() { return resolvedStrokeWidth; }
    public float getResolvedCheckMarkWidth() { return resolvedCheckMarkWidth; }
    public float getResolvedPadding() { return resolvedPadding; }

    @Override public CheckBox setChecked(boolean value) {
        return setStateInternal(value ? State.CHECKED : State.UNCHECKED, true, true, false);
    }
    public CheckBox setChecked(boolean value, boolean notifyListener) {
        return setStateInternal(value ? State.CHECKED : State.UNCHECKED,
                true, notifyListener, false);
    }
    @Override public CheckBox setCheckedImmediately(boolean value) {
        return setStateInternal(value ? State.CHECKED : State.UNCHECKED, false, true, false);
    }
    public CheckBox setCheckedImmediately(boolean value, boolean notifyListener) {
        return setStateInternal(value ? State.CHECKED : State.UNCHECKED,
                false, notifyListener, false);
    }
    public CheckBox setState(State value) {
        return setStateInternal(value, true, true, false);
    }
    public CheckBox setState(State value, boolean notifyListener) {
        return setStateInternal(value, true, notifyListener, false);
    }
    public CheckBox setStateImmediately(State value) {
        return setStateInternal(value, false, true, false);
    }
    @Override public CheckBox toggle() {
        return setStateInternal(nextState(), true, true, false);
    }
    @Override public CheckBox toggleImmediately() {
        return setStateInternal(nextState(), false, true, false);
    }
    @Override public CheckBox setOnCheckedChangeListener(OnCheckedChangeListener value) {
        ensureActive(); checkedChangeListener = value; return this;
    }
    @Override public CheckBox removeOnCheckedChangeListener() {
        return setOnCheckedChangeListener(null);
    }
    public CheckBox setOnStateChangeListener(OnStateChangeListener value) {
        ensureActive(); stateChangeListener = value; return this;
    }
    public CheckBox removeOnStateChangeListener() {
        return setOnStateChangeListener(null);
    }
    public CheckBox setIndeterminateEnabled(boolean value) {
        ensureActive();
        if (value && renderMode == RenderMode.IMAGE && !images.supportsIndeterminate()) {
            throw new IllegalStateException(
                    "Indeterminate interaction requires indeterminate CheckBox images.");
        }
        indeterminateEnabled = value; invalidate(); return this;
    }
    public CheckBox setToggleOrder(State... value) {
        ensureActive(); toggleOrder = requireToggleOrder(value); return this;
    }

    public CheckBox setRegion(Position position, Size size) {
        ensureActive(); resolveRegion(Objects.requireNonNull(position), Objects.requireNonNull(size), null);
        applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public CheckBox setRegion(RectF value) {
        ensureActive(); resolveRegion(null, null, Objects.requireNonNull(value));
        applyParentAlignment(); rebuildGeometry(); invalidate(); return this;
    }
    public CheckBox horizontalCenter(boolean value) {
        ensureActive(); horizontalCentered = value; applyParentAlignment();
        rebuildGeometry(); invalidate(); return this;
    }
    public CheckBox verticalCenter(boolean value) {
        ensureActive(); verticalCentered = value; applyParentAlignment();
        rebuildGeometry(); invalidate(); return this;
    }
    public CheckBox setVisible(boolean value) {
        ensureActive(); visible = value; if (!value) cancelTouch(); invalidate(); return this;
    }
    public CheckBox setEnabled(boolean value) {
        ensureActive(); enabled = value; if (!value) cancelTouch(); invalidate(); return this;
    }
    public CheckBox setAlpha(float value) {
        ensureActive(); alpha = requireAlpha(value); invalidate(); return this;
    }
    public CheckBox setDisabledAlpha(float value) {
        ensureActive(); disabledAlpha = requireAlpha(value); invalidate(); return this;
    }

    public CheckBox setCheckedColor(int value) { requireColorMode(); checkedColor = value; invalidate(); return this; }
    public CheckBox setUncheckedColor(int value) { requireColorMode(); uncheckedColor = value; invalidate(); return this; }
    public CheckBox setIndeterminateColor(int value) { requireColorMode(); indeterminateColor = value; invalidate(); return this; }
    public CheckBox setDisabledCheckedColor(int value) { requireColorMode(); disabledCheckedColor = value; invalidate(); return this; }
    public CheckBox setDisabledUncheckedColor(int value) { requireColorMode(); disabledUncheckedColor = value; invalidate(); return this; }
    public CheckBox setDisabledIndeterminateColor(int value) { requireColorMode(); disabledIndeterminateColor = value; invalidate(); return this; }
    public CheckBox setCheckMarkColor(int value) { requireColorMode(); checkMarkColor = value; invalidate(); return this; }
    public CheckBox setDisabledCheckMarkColor(int value) { requireColorMode(); disabledCheckMarkColor = value; invalidate(); return this; }
    public CheckBox setStrokeColor(int value) { requireColorMode(); strokeColor = value; invalidate(); return this; }
    public CheckBox setDisabledStrokeColor(int value) { requireColorMode(); disabledStrokeColor = value; invalidate(); return this; }
    public CheckBox setCornerRadius(float value) { requireColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public CheckBox setCornerRadiusPx(float value) { requireColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public CheckBox setStrokeWidth(float value) { requireColorMode(); strokeWidth = requireNonNegative(value, "Stroke width"); strokeWidthInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public CheckBox setStrokeWidthPx(float value) { requireColorMode(); strokeWidth = requireNonNegative(value, "Stroke width"); strokeWidthInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public CheckBox setCheckMarkWidth(float value) { requireColorMode(); checkMarkWidth = requirePositive(value, "Check-mark width"); checkMarkWidthInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public CheckBox setCheckMarkWidthPx(float value) { requireColorMode(); checkMarkWidth = requirePositive(value, "Check-mark width"); checkMarkWidthInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public CheckBox setPadding(float value) { requireColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public CheckBox setPaddingPx(float value) { requireColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = true; rebuildGeometry(); invalidate(); return this; }

    public CheckBox setCheckBoxImages(CheckBoxImages value) {
        ensureActive(); cancelStateAnimation();
        images = Objects.requireNonNull(value, "CheckBox images cannot be null.");
        images.validateActive();
        if (stateValue == State.INDETERMINATE && !images.supportsIndeterminate()) {
            throw new IllegalStateException(
                    "Current indeterminate state requires indeterminate CheckBox images.");
        }
        renderMode = RenderMode.IMAGE;
        disabledAlpha = 1f;
        rebuildRenderer(); invalidate(); return this;
    }
    public CheckBox useColorRendering() {
        ensureActive(); cancelStateAnimation(); images = null;
        renderMode = RenderMode.COLOR; rebuildRenderer(); invalidate(); return this;
    }
    public CheckBox setImageScaleType(Image.ScaleType value) {
        requireImageMode(); imageScaleType = Objects.requireNonNull(value);
        ((ImageCheckBoxRenderer) renderer).setScaleType(value); invalidate(); return this;
    }
    public CheckBox setImageTransition(ImageTransition value) {
        requireImageMode(); imageTransition = Objects.requireNonNull(value);
        ((ImageCheckBoxRenderer) renderer).setTransition(value); invalidate(); return this;
    }
    public CheckBox setImageFiltering(boolean value) {
        requireImageMode(); imageFiltering = value; renderer.setImageFiltering(value);
        invalidate(); return this;
    }

    public CheckBox setStateAnimationDuration(long value) {
        ensureActive(); stateAnimationDuration = requireDuration(value, "State animation duration"); return this;
    }
    public CheckBox setAnimationInterpolator(Interpolator value) {
        ensureActive(); animationInterpolator = Objects.requireNonNull(value); return this;
    }
    public CheckBox finishAnimation() {
        ensureActive(); if (stateAnimator != null) stateAnimator.end(); return this;
    }
    public CheckBox cancelAnimation() {
        ensureActive(); cancelStateAnimation(); fromState = stateValue;
        transitionProgress = 1f; invalidate(); return this;
    }
    public CheckBox setPressedScale(float value) {
        ensureActive(); pressedScale = requireScale(value); return this;
    }
    public CheckBox setPressAnimationDuration(long value) {
        ensureActive(); pressAnimationDuration = requireDuration(value, "Press animation duration"); return this;
    }
    public CheckBox setRippleEnabled(boolean value) {
        ensureActive(); rippleEnabled = value; if (!value) cancelRipple(); invalidate(); return this;
    }
    public CheckBox setRippleColor(int value) { ensureActive(); rippleColor = value; invalidate(); return this; }
    public CheckBox setRippleDuration(long value) { ensureActive(); rippleDuration = requireDuration(value, "Ripple duration"); return this; }
    public CheckBox setSoundAction(Runnable value) { ensureActive(); soundAction = value; return this; }
    public CheckBox setHapticAction(Runnable value) { ensureActive(); hapticAction = value; return this; }

    @Override public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || alpha <= 0f) return;
        float effectiveAlpha = alpha * (enabled ? 1f : disabledAlpha);
        int layer = effectiveAlpha < 1f
                ? canvas.saveLayerAlpha(bounds, Math.round(255f * effectiveAlpha))
                : canvas.save();
        canvas.scale(currentPressedScale, currentPressedScale, bounds.centerX(), bounds.centerY());
        populateRenderState();
        renderer.draw(canvas, renderState);
        drawRipple(canvas);
        canvas.restoreToCount(layer);
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
                        || !bounds.contains(event.getX(), event.getY())) {
                    touchCancelled = true;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!touchCaptured) return false;
                boolean click = !touchCancelled && bounds.contains(event.getX(), event.getY());
                touchCaptured = false; touchCancelled = false;
                animatePressTo(1f); finishRipple();
                if (click) setStateInternal(nextState(), true, true, true);
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
            throw new IllegalStateException("CheckBox already belongs to another host.");
        }
        owner = value; applyParentAlignment(); rebuildGeometry();
    }

    @Override public void release() {
        if (released) return;
        cancelTouch(); cancelStateAnimation(); cancelPressAnimation(); cancelRipple();
        renderer.release(); checkedChangeListener = null; stateChangeListener = null;
        soundAction = null; hapticAction = null; owner = null; released = true;
    }

    private CheckBox setStateInternal(
            State value, boolean animate, boolean notifyListener, boolean fromUser) {
        ensureActive(); value = Objects.requireNonNull(value, "CheckBox state cannot be null.");
        validateStateSupport(value);
        State old = stateValue;
        if (old == value) return this;
        boolean oldChecked = old == State.CHECKED;
        stateValue = value;
        fromState = old;
        startStateTransition(animate);
        if (notifyListener) {
            if (stateChangeListener != null) stateChangeListener.onStateChanged(id, value, fromUser);
            boolean newChecked = value == State.CHECKED;
            if (checkedChangeListener != null && oldChecked != newChecked) {
                checkedChangeListener.onCheckedChanged(id, newChecked, fromUser);
            }
        }
        if (fromUser) runFeedback();
        return this;
    }

    private void startStateTransition(boolean animate) {
        cancelStateAnimation();
        if (!animate || stateAnimationDuration == 0L) {
            transitionProgress = 1f; fromState = stateValue; invalidate(); return;
        }
        transitionProgress = 0f;
        stateAnimator = ValueAnimator.ofFloat(0f, 1f);
        stateAnimator.setDuration(stateAnimationDuration);
        stateAnimator.setInterpolator(toInterpolator(animationInterpolator));
        stateAnimator.addUpdateListener(value -> {
            transitionProgress = (float) value.getAnimatedValue();
            if (transitionProgress >= 1f) fromState = stateValue;
            invalidateOnAnimation();
        });
        stateAnimator.start();
    }

    private State nextState() {
        State[] order = indeterminateEnabled
                ? toggleOrder : new State[]{State.UNCHECKED, State.CHECKED};
        int index = -1;
        for (int i = 0; i < order.length; i++) if (order[i] == stateValue) index = i;
        return order[(index + 1) % order.length];
    }

    private void validateStateSupport(State value) {
        if (value == State.INDETERMINATE
                && renderMode == RenderMode.IMAGE && !images.supportsIndeterminate()) {
            throw new IllegalStateException(
                    "Indeterminate state requires indeterminate CheckBox images.");
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
        if (horizontalCentered) {
            bounds.offsetTo(parent.centerX() - bounds.width() / 2f, bounds.top);
        }
        if (verticalCentered) {
            bounds.offsetTo(bounds.left, parent.centerY() - bounds.height() / 2f);
        }
    }

    private void rebuildGeometry() {
        resolvedCornerRadius = Math.min(resolve(cornerRadius, cornerRadiusInPixels),
                Math.min(bounds.width(), bounds.height()) / 2f);
        resolvedStrokeWidth = resolve(strokeWidth, strokeWidthInPixels);
        resolvedCheckMarkWidth = resolve(checkMarkWidth, checkMarkWidthInPixels);
        resolvedPadding = resolve(padding, paddingInPixels);
        if (resolvedPadding * 2f >= Math.min(bounds.width(), bounds.height())) {
            throw new IllegalArgumentException("CheckBox padding leaves no drawable mark region.");
        }
    }

    private void populateRenderState() {
        renderState.bounds.set(bounds);
        renderState.fromState = fromState;
        renderState.state = stateValue;
        renderState.transitionProgress = transitionProgress;
        renderState.enabled = enabled;
        renderState.cornerRadius = resolvedCornerRadius;
        renderState.strokeWidth = resolvedStrokeWidth;
        renderState.markWidth = resolvedCheckMarkWidth;
        renderState.padding = resolvedPadding;
        renderState.checkedColor = checkedColor;
        renderState.uncheckedColor = uncheckedColor;
        renderState.indeterminateColor = indeterminateColor;
        renderState.disabledCheckedColor = disabledCheckedColor;
        renderState.disabledUncheckedColor = disabledUncheckedColor;
        renderState.disabledIndeterminateColor = disabledIndeterminateColor;
        renderState.checkMarkColor = checkMarkColor;
        renderState.disabledCheckMarkColor = disabledCheckMarkColor;
        renderState.strokeColor = strokeColor;
        renderState.disabledStrokeColor = disabledStrokeColor;
    }

    private void rebuildRenderer() {
        if (renderer != null) renderer.release();
        renderer = renderMode == RenderMode.IMAGE
                ? new ImageCheckBoxRenderer(images, imageScaleType, imageTransition, imageFiltering)
                : new ColorCheckBoxRenderer();
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
            currentPressedScale = (float) value.getAnimatedValue();
            invalidateOnAnimation();
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
        int save = canvas.save();
        canvas.clipRect(bounds);
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
    private void cancelStateAnimation() {
        if (stateAnimator != null) stateAnimator.cancel(); stateAnimator = null;
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
    private float resolve(float value, boolean pixels) {
        return pixels ? value : value * dimensionScale;
    }
    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
    private void invalidateOnAnimation() {
        if (owner != null) owner.postInvalidateComponentOnAnimation();
    }
    private void ensureActive() {
        if (released) throw new IllegalStateException("CheckBox has been released: " + id);
    }
    private void requireColorMode() {
        ensureActive();
        if (renderMode != RenderMode.COLOR) {
            throw new IllegalStateException("This styling API requires COLOR rendering mode.");
        }
    }
    private void requireImageMode() {
        ensureActive();
        if (renderMode != RenderMode.IMAGE) {
            throw new IllegalStateException("This API requires IMAGE rendering mode.");
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
    private static State[] requireToggleOrder(State[] values) {
        Objects.requireNonNull(values, "Toggle order cannot be null.");
        if (values.length < 2 || values.length > 3) {
            throw new IllegalArgumentException("Toggle order must contain two or three states.");
        }
        EnumSet<State> set = EnumSet.noneOf(State.class);
        for (State value : values) {
            if (value == null || !set.add(value)) {
                throw new IllegalArgumentException("Toggle order cannot contain null or duplicate states.");
            }
        }
        if (!set.contains(State.UNCHECKED) || !set.contains(State.CHECKED)) {
            throw new IllegalArgumentException("Toggle order must contain UNCHECKED and CHECKED.");
        }
        return values.clone();
    }
    private static String requireId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("CheckBox ID cannot be blank.");
        }
        return value.trim();
    }
    private static void requireBounds(RectF value) {
        if (value.width() <= 0f || value.height() <= 0f) {
            throw new IllegalArgumentException("CheckBox bounds must be positive.");
        }
    }
    private static float requireAlpha(float value) {
        if (!Float.isFinite(value) || value < 0f || value > 1f) {
            throw new IllegalArgumentException("Alpha must be in [0, 1].");
        }
        return value;
    }
    private static float requireNonNegative(float value, String label) {
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(label + " must be non-negative and finite.");
        }
        return value;
    }
    private static float requirePositive(float value, String label) {
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(label + " must be positive and finite.");
        }
        return value;
    }
    private static float requireScale(float value) {
        if (!Float.isFinite(value) || value <= 0f || value > 1f) {
            throw new IllegalArgumentException("Pressed scale must be in (0, 1].");
        }
        return value;
    }
    private static long requireDuration(long value, String label) {
        if (value < 0L) throw new IllegalArgumentException(label + " cannot be negative.");
        return value;
    }

    public static final class Builder implements ComponentFactory<CheckBox> {
        private final Context context;
        private final String id;
        private Position position;
        private Size size;
        private RectF explicitBounds;
        private State state = State.UNCHECKED;
        private State[] toggleOrder = {
                State.UNCHECKED, State.CHECKED, State.INDETERMINATE};
        private boolean indeterminateEnabled;
        private int checkedColor = 0xff019cc4;
        private int uncheckedColor = Color.TRANSPARENT;
        private int indeterminateColor = 0xff019cc4;
        private int disabledCheckedColor = 0xff9e9e9e;
        private int disabledUncheckedColor = 0xffeeeeee;
        private int disabledIndeterminateColor = 0xffbdbdbd;
        private int checkMarkColor = Color.WHITE;
        private int disabledCheckMarkColor = Color.WHITE;
        private int strokeColor = 0xff656565;
        private int disabledStrokeColor = 0xffbdbdbd;
        private float cornerRadius = 8f;
        private boolean cornerRadiusInPixels;
        private float strokeWidth = 2f;
        private boolean strokeWidthInPixels;
        private float checkMarkWidth = 3f;
        private boolean checkMarkWidthInPixels;
        private float padding = 6f;
        private boolean paddingInPixels;
        private long stateAnimationDuration = DEFAULT_STATE_DURATION;
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
        private CheckBoxImages images;
        private Image.ScaleType imageScaleType = Image.ScaleType.FIT_CENTER;
        private ImageTransition imageTransition = ImageTransition.CROSS_FADE;
        private boolean imageFiltering = true;
        private OnCheckedChangeListener checkedChangeListener;
        private OnStateChangeListener stateChangeListener;
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
        public Builder(
                Context context, String id, CheckBoxImages images,
                Position position, Size size) {
            this(context, id, position, size); configureImages(images);
        }
        public Builder(Context context, String id, CheckBoxImages images, RectF bounds) {
            this(context, id, bounds); configureImages(images);
        }

        public Builder setChecked(boolean value) { state = value ? State.CHECKED : State.UNCHECKED; return this; }
        public Builder setState(State value) { state = Objects.requireNonNull(value); validateBuilderState(); return this; }
        public Builder setIndeterminateEnabled(boolean value) { indeterminateEnabled = value; validateBuilderState(); return this; }
        public Builder setToggleOrder(State... value) { toggleOrder = requireToggleOrder(value); return this; }
        public Builder setCheckedColor(int value) { requireBuilderColorMode(); checkedColor = value; return this; }
        public Builder setUncheckedColor(int value) { requireBuilderColorMode(); uncheckedColor = value; return this; }
        public Builder setIndeterminateColor(int value) { requireBuilderColorMode(); indeterminateColor = value; return this; }
        public Builder setDisabledCheckedColor(int value) { requireBuilderColorMode(); disabledCheckedColor = value; return this; }
        public Builder setDisabledUncheckedColor(int value) { requireBuilderColorMode(); disabledUncheckedColor = value; return this; }
        public Builder setDisabledIndeterminateColor(int value) { requireBuilderColorMode(); disabledIndeterminateColor = value; return this; }
        public Builder setCheckMarkColor(int value) { requireBuilderColorMode(); checkMarkColor = value; return this; }
        public Builder setDisabledCheckMarkColor(int value) { requireBuilderColorMode(); disabledCheckMarkColor = value; return this; }
        public Builder setStrokeColor(int value) { requireBuilderColorMode(); strokeColor = value; return this; }
        public Builder setDisabledStrokeColor(int value) { requireBuilderColorMode(); disabledStrokeColor = value; return this; }
        public Builder setCornerRadius(float value) { requireBuilderColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = false; return this; }
        public Builder setCornerRadiusPx(float value) { requireBuilderColorMode(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = true; return this; }
        public Builder setStrokeWidth(float value) { requireBuilderColorMode(); strokeWidth = requireNonNegative(value, "Stroke width"); strokeWidthInPixels = false; return this; }
        public Builder setStrokeWidthPx(float value) { requireBuilderColorMode(); strokeWidth = requireNonNegative(value, "Stroke width"); strokeWidthInPixels = true; return this; }
        public Builder setCheckMarkWidth(float value) { requireBuilderColorMode(); checkMarkWidth = requirePositive(value, "Check-mark width"); checkMarkWidthInPixels = false; return this; }
        public Builder setCheckMarkWidthPx(float value) { requireBuilderColorMode(); checkMarkWidth = requirePositive(value, "Check-mark width"); checkMarkWidthInPixels = true; return this; }
        public Builder setPadding(float value) { requireBuilderColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = false; return this; }
        public Builder setPaddingPx(float value) { requireBuilderColorMode(); padding = requireNonNegative(value, "Padding"); paddingInPixels = true; return this; }
        public Builder setStateAnimationDuration(long value) { stateAnimationDuration = requireDuration(value, "State animation duration"); return this; }
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
        public Builder setOnStateChangeListener(OnStateChangeListener value) { stateChangeListener = value; return this; }
        public Builder setSoundAction(Runnable value) { soundAction = value; return this; }
        public Builder setHapticAction(Runnable value) { hapticAction = value; return this; }
        @Override public CheckBox build(View hostView) { validateBuilderState(); return new CheckBox(this, hostView); }

        private void configureImages(CheckBoxImages value) {
            images = Objects.requireNonNull(value, "CheckBox images cannot be null.");
            images.validateActive(); renderMode = RenderMode.IMAGE; disabledAlpha = 1f;
        }
        private void validateBuilderState() {
            if (state == State.INDETERMINATE && renderMode == RenderMode.IMAGE
                    && !images.supportsIndeterminate()) {
                throw new IllegalStateException(
                        "Indeterminate state requires indeterminate CheckBox images.");
            }
            if (indeterminateEnabled && renderMode == RenderMode.IMAGE
                    && !images.supportsIndeterminate()) {
                throw new IllegalStateException(
                        "Indeterminate interaction requires indeterminate CheckBox images.");
            }
        }
        private void requireBuilderColorMode() {
            if (renderMode != RenderMode.COLOR) {
                throw new IllegalStateException("This styling API requires COLOR rendering mode.");
            }
        }
        private void requireBuilderImageMode() {
            if (renderMode != RenderMode.IMAGE) {
                throw new IllegalStateException("This API requires IMAGE rendering mode.");
            }
        }
    }
}

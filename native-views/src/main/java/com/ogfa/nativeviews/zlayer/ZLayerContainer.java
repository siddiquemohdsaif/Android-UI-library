package com.ogfa.nativeviews.zlayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.textfield.TextField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A generic Component that owns nested ZLayers in a local coordinate system.
 * Any Component, including another ZLayerContainer, can be placed inside it.
 */
public final class ZLayerContainer implements Component {
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
    public interface OnClickListener { void onClick(String id); }
    public interface OnLongClickListener { void onLongClick(String id); }

    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final ArrayList<ZLayer> layers = new ArrayList<>();
    private final Map<String, ZLayer> layersById = new LinkedHashMap<>();
    private final ZLayer defaultLayer;

    private final ZLayerOwner layerOwner = new ZLayerOwner() {
        @Override public View getHostView() { return hostView; }
        @Override public void registerLayerComponent(Component component) { registerChild(component); }
        @Override public void unregisterLayerComponent(Component component) {
            if (rootHost != null) rootHost.unregisterNestedComponent(component);
        }
        @Override public void invalidateLayer() { invalidate(); }
    };

    private final NestedComponentHost childHost = new NestedComponentHost() {
        @Override public View getHostView() { return hostView; }
        @Override public RectF getComponentBounds() {
            return new RectF(0f, 0f, bounds.width(), bounds.height());
        }
        @Override public void invalidateComponent() { invalidate(); }
        @Override public void postInvalidateComponentOnAnimation() {
            if (owner != null) owner.postInvalidateComponentOnAnimation();
        }
        @Override public boolean requestFocus(TextField field) {
            return rootHost != null && rootHost.requestFocus(field);
        }
        @Override public void clearFocus(TextField field) { if (rootHost != null) rootHost.clearFocus(field); }
        @Override public void restartInput() { if (rootHost != null) rootHost.restartInput(); }
        @Override public void updateSelection(TextField field) { if (rootHost != null) rootHost.updateSelection(field); }
        @Override public void registerNestedComponent(Component component) {
            if (rootHost != null) rootHost.registerNestedComponent(component);
        }
        @Override public void unregisterNestedComponent(Component component) {
            if (rootHost != null) rootHost.unregisterNestedComponent(component);
        }
    };

    private ComponentHost owner;
    private NestedComponentHost rootHost;
    private Component touchTarget;
    private ZLayer touchLayer;
    private boolean blockedTouch;
    private boolean containerPressed;
    private boolean longClickFired;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean clipToBounds;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private boolean released;
    private float alpha = 1f;
    private float dimensionScale = 1f;
    private float translationX;
    private float translationY;
    private float pressedScale = 0.96f;
    private float currentScale = 1f;
    private long pressDuration = 100L;
    private long longClickDelay = 500L;
    private OnClickListener clickListener;
    private OnLongClickListener longClickListener;
    private Runnable soundAction;
    private Runnable hapticAction;
    private ValueAnimator pressAnimator;
    private ValueAnimator movementAnimator;
    private boolean movementCancelled;

    private final Runnable longClickRunnable;

    private ZLayerContainer(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        id = requireId(builder.id);
        longClickRunnable = () -> {
            if (!containerPressed || longClickListener == null || released) return;
            longClickFired = true;
            longClickListener.onLongClick(id);
            runFeedback();
            invalidate();
        };
        clickListener = builder.clickListener;
        longClickListener = builder.longClickListener;
        pressedScale = builder.pressedScale;
        pressDuration = builder.pressDuration;
        longClickDelay = builder.longClickDelay;
        soundAction = builder.soundAction;
        hapticAction = builder.hapticAction;
        alpha = builder.alpha;
        visible = builder.visible;
        enabled = builder.enabled;
        clipToBounds = builder.clipToBounds;
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        if (builder.explicitBounds != null) {
            baseBounds.set(requireBounds(builder.explicitBounds));
        } else {
            baseBounds.set(builder.position.toRectF(hostView, builder.size));
            dimensionScale = builder.position.getScale(hostView);
        }
        bounds.set(baseBounds);
        defaultLayer = addLayerInternal("content");
    }

    public static final class Builder implements ComponentFactory<ZLayerContainer> {
        private final Context context;
        private final String id;
        private Position position;
        private Size size;
        private RectF explicitBounds;
        private OnClickListener clickListener;
        private OnLongClickListener longClickListener;
        private Runnable soundAction;
        private Runnable hapticAction;
        private float pressedScale = 0.96f;
        private long pressDuration = 100L;
        private long longClickDelay = 500L;
        private float alpha = 1f;
        private boolean visible = true;
        private boolean enabled = true;
        private boolean clipToBounds;
        private boolean horizontalCentered;
        private boolean verticalCentered;

        public Builder(Context context, String id, Position position, Size size) {
            this.context = Objects.requireNonNull(context, "Context cannot be null.");
            this.id = requireId(id);
            this.position = Objects.requireNonNull(position, "Position cannot be null.");
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
        }
        public Builder(Context context, String id, RectF bounds) {
            this.context = Objects.requireNonNull(context, "Context cannot be null.");
            this.id = requireId(id);
            explicitBounds = requireBounds(bounds);
        }
        public Builder setOnClickListener(OnClickListener value) { clickListener = value; return this; }
        public Builder setOnLongClickListener(OnLongClickListener value) { longClickListener = value; return this; }
        public Builder setLongClickDelay(long value) { if (value < 0L) throw new IllegalArgumentException("Delay cannot be negative."); longClickDelay = value; return this; }
        public Builder setPressedScale(float value) { pressedScale = requirePressedScale(value); return this; }
        public Builder setPressAnimationDuration(long value) { if (value < 0L) throw new IllegalArgumentException("Duration cannot be negative."); pressDuration = value; return this; }
        public Builder setSoundAction(Runnable value) { soundAction = value; return this; }
        public Builder setHapticAction(Runnable value) { hapticAction = value; return this; }
        public Builder setClipToBounds(boolean value) { clipToBounds = value; return this; }
        public Builder setAlpha(float value) { alpha = requireAlpha(value); return this; }
        public Builder setVisible(boolean value) { visible = value; return this; }
        public Builder setEnabled(boolean value) { enabled = value; return this; }
        public Builder horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder verticalCenter(boolean value) { verticalCentered = value; return this; }
        @Override public ZLayerContainer build(View hostView) { return new ZLayerContainer(this, hostView); }
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() {
        RectF result = new RectF(bounds);
        result.offset(translationX, translationY);
        return result;
    }
    public RectF getLocalBounds() { return new RectF(0f, 0f, bounds.width(), bounds.height()); }
    public float getDimensionScale() { return dimensionScale; }
    public RectF figmaRect(float left, float top, float width, float height) {
        requireFinite(left, "Left"); requireFinite(top, "Top");
        if (!(width > 0f) || !(height > 0f)) throw new IllegalArgumentException("Local size must be positive.");
        return new RectF(left * dimensionScale, top * dimensionScale,
                (left + width) * dimensionScale, (top + height) * dimensionScale);
    }
    public RectF pxRect(float left, float top, float width, float height) {
        requireFinite(left, "Left"); requireFinite(top, "Top");
        if (!(width > 0f) || !(height > 0f)) throw new IllegalArgumentException("Local size must be positive.");
        return new RectF(left, top, left + width, top + height);
    }

    public ZLayer getContentLayer() { ensureActive(); return defaultLayer; }
    public ZLayer addLayer(String layerId) { ensureActive(); return addLayerInternal(layerId); }
    public ZLayer findLayer(String layerId) { return layerId == null ? null : layersById.get(layerId.trim()); }
    public List<ZLayer> getLayers() { return Collections.unmodifiableList(layers); }
    public int getLayerCount() { return layers.size(); }
    public void bringLayerToFront(String layerId) { moveLayer(layerId, layers.size() - 1); }
    public void sendLayerToBack(String layerId) { moveLayer(layerId, 0); }
    public void setLayerIndex(String layerId, int index) { if (index < 0 || index >= layers.size()) throw new IndexOutOfBoundsException("Layer index: " + index); moveLayer(layerId, index); }
    public void moveLayerAbove(String layerId, String referenceId) { moveRelative(layerId, referenceId, true); }
    public void moveLayerBelow(String layerId, String referenceId) { moveRelative(layerId, referenceId, false); }
    public boolean removeLayer(String layerId) {
        ZLayer layer = findLayer(layerId);
        if (layer == null || layer == defaultLayer) return false;
        layer.clear(); layers.remove(layer); layersById.remove(layerId.trim()); invalidate(); return true;
    }
    public Component findComponent(String componentId) {
        for (ZLayer layer : layers) { Component found = layer.find(componentId); if (found != null) return found; }
        return null;
    }
    public <T extends Component> T findComponent(String componentId, Class<T> type) {
        Component found = findComponent(componentId); return type.isInstance(found) ? type.cast(found) : null;
    }
    public boolean moveComponent(String componentId, String targetLayerId) {
        ZLayer target = requireLayer(targetLayerId);
        Component component = findComponent(componentId);
        if (component == null) return false;
        for (ZLayer layer : layers) if (layer.contains(componentId)) {
            layer.detachForMove(component); target.attachMoved(component); invalidate(); return true;
        }
        return false;
    }

    public ZLayerContainer setRegion(Position position, Size size) {
        baseBounds.set(position.toRectF(hostView, size));
        dimensionScale = position.getScale(hostView);
        applyAlignment(); invalidate(); return this;
    }
    public ZLayerContainer setRegion(RectF value) { baseBounds.set(requireBounds(value)); dimensionScale = 1f; applyAlignment(); invalidate(); return this; }
    public ZLayerContainer horizontalCenter(boolean value) { horizontalCentered = value; applyAlignment(); invalidate(); return this; }
    public ZLayerContainer verticalCenter(boolean value) { verticalCentered = value; applyAlignment(); invalidate(); return this; }
    public ZLayerContainer setTranslation(float x, float y) { requireFinite(x, "Translation X"); requireFinite(y, "Translation Y"); translationX = x; translationY = y; invalidate(); return this; }
    public ZLayerContainer resetTranslation() { return setTranslation(0f, 0f); }
    public ZLayerContainer setClipToBounds(boolean value) { clipToBounds = value; invalidate(); return this; }
    public ZLayerContainer setAlpha(float value) { alpha = requireAlpha(value); invalidate(); return this; }
    public float getAlpha() { return alpha; }
    @Override public boolean isVisible() { return visible; }
    public ZLayerContainer setVisible(boolean value) { visible = value; if (!value) cancelTouch(); invalidate(); return this; }
    @Override public boolean isEnabled() { return enabled; }
    public ZLayerContainer setEnabled(boolean value) { enabled = value; if (!value) cancelTouch(); return this; }

    @Override public void draw(Canvas canvas) {
        if (!visible || released || alpha <= 0f) return;
        int save = canvas.save();
        canvas.translate(bounds.left + translationX, bounds.top + translationY);
        float width = bounds.width(); float height = bounds.height();
        canvas.scale(currentScale, currentScale, width / 2f, height / 2f);
        int alphaSave = alpha >= 1f ? canvas.save() : canvas.saveLayerAlpha(
                new RectF(0f, 0f, width, height), Math.round(alpha * 255f));
        if (clipToBounds) canvas.clipRect(0f, 0f, width, height);
        for (ZLayer layer : layers) layer.draw(canvas);
        canvas.restoreToCount(alphaSave);
        canvas.restoreToCount(save);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        MotionEvent local = toLocalEvent(event);
        try { return dispatchLocalTouch(local); }
        finally { local.recycle(); }
    }

    private boolean dispatchLocalTouch(MotionEvent event) {
        boolean inside = new RectF(0f, 0f, bounds.width(), bounds.height())
                .contains(event.getX(), event.getY());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelTouch();
                if (!visible || !enabled || !inside) return false;
                for (int i = layers.size() - 1; i >= 0; i--) {
                    ZLayer layer = layers.get(i);
                    touchTarget = layer.dispatchDown(event);
                    if (touchTarget != null) { touchLayer = layer; return true; }
                    if (layer.isVisible() && layer.isEnabled()
                            && (layer.getTouchPolicy() == ZLayer.TouchPolicy.MODAL
                            || (layer.getTouchPolicy() == ZLayer.TouchPolicy.BLOCK_BELOW
                            && layer.containsPoint(event.getX(), event.getY())))) {
                        blockedTouch = true; return true;
                    }
                }
                if (clickListener == null && longClickListener == null) return false;
                containerPressed = true; longClickFired = false;
                animatePressTo(pressedScale);
                if (longClickListener != null) hostView.postDelayed(longClickRunnable, longClickDelay);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (blockedTouch) return true;
                if (touchTarget != null) return touchLayer.dispatchTo(touchTarget, event);
                if (!containerPressed) return false;
                if (!inside) cancelContainerPress();
                return containerPressed;
            case MotionEvent.ACTION_UP:
                if (blockedTouch) { blockedTouch = false; return true; }
                if (touchTarget != null) {
                    boolean handled = touchLayer.dispatchTo(touchTarget, event);
                    touchTarget = null; touchLayer = null; return handled;
                }
                if (!containerPressed) return false;
                hostView.removeCallbacks(longClickRunnable);
                containerPressed = false; animatePressTo(1f);
                if (inside && !longClickFired && clickListener != null) {
                    clickListener.onClick(id); runFeedback();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                boolean handled = blockedTouch || touchTarget != null || containerPressed;
                cancelTouch(); return handled;
            default: return blockedTouch || touchTarget != null || containerPressed;
        }
    }

    public void animateRegionTo(RectF target, long duration, Interpolator interpolator, Runnable completion) {
        RectF start = new RectF(bounds);
        RectF end = alignTarget(requireBounds(target));
        if (movementAnimator != null) movementAnimator.cancel();
        movementCancelled = false;
        movementAnimator = ValueAnimator.ofFloat(0f, 1f);
        movementAnimator.setDuration(Math.max(0L, duration));
        movementAnimator.setInterpolator(toInterpolator(interpolator));
        movementAnimator.addUpdateListener(value -> {
            float f = (float) value.getAnimatedValue();
            bounds.set(lerp(start.left, end.left, f), lerp(start.top, end.top, f),
                    lerp(start.right, end.right, f), lerp(start.bottom, end.bottom, f));
            baseBounds.set(bounds); invalidate();
        });
        movementAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationCancel(Animator animation) { movementCancelled = true; }
            @Override public void onAnimationEnd(Animator animation) { if (!movementCancelled && completion != null) completion.run(); }
        });
        movementAnimator.start();
    }
    public void animateRegionTo(Position position, Size size, long duration, Interpolator interpolator, Runnable completion) {
        dimensionScale = position.getScale(hostView);
        animateRegionTo(position.toRectF(hostView, size), duration, interpolator, completion);
    }
    public boolean isMoving() { return movementAnimator != null && movementAnimator.isRunning(); }
    public void pauseMovement() { if (isMoving()) movementAnimator.pause(); }
    public void resumeMovement() { if (movementAnimator != null && movementAnimator.isPaused()) movementAnimator.resume(); }
    public void cancelMovement() { if (movementAnimator != null) movementAnimator.cancel(); }
    public void finishMovement() { if (movementAnimator != null) movementAnimator.end(); }

    @Override public void attach(ComponentHost owner) {
        ensureActive();
        if (!(owner instanceof NestedComponentHost)) throw new IllegalArgumentException("ZLayerContainer requires a NestedComponentHost such as ZLayerGroup.");
        if (this.owner != null && this.owner != owner) throw new IllegalStateException("Container already has another host.");
        this.owner = owner; rootHost = (NestedComponentHost) owner; applyAlignment();
        for (ZLayer layer : layers) for (Component component : layer.getComponents()) {
            rootHost.registerNestedComponent(component);
            component.attach(childHost);
        }
    }

    @Override public void release() {
        if (released) return;
        cancelTouch();
        if (pressAnimator != null) pressAnimator.cancel();
        if (movementAnimator != null) movementAnimator.cancel();
        for (ZLayer layer : new ArrayList<>(layers)) layer.clear();
        layers.clear(); layersById.clear(); owner = null; rootHost = null; released = true;
    }

    private void registerChild(Component component) {
        if (rootHost != null) rootHost.registerNestedComponent(component);
        try { component.attach(childHost); }
        catch (RuntimeException error) {
            if (rootHost != null) rootHost.unregisterNestedComponent(component);
            throw error;
        }
    }
    private ZLayer addLayerInternal(String layerId) {
        String normalized = requireId(layerId);
        if (layersById.containsKey(normalized)) throw new IllegalArgumentException("Duplicate container layer ID: " + normalized);
        ZLayer layer = new ZLayer(layerOwner, id + ":" + normalized);
        layers.add(layer); layersById.put(normalized, layer); invalidate(); return layer;
    }
    private ZLayer requireLayer(String id) { ZLayer layer = findLayer(id); if (layer == null) throw new IllegalArgumentException("Unknown container layer: " + id); return layer; }
    private void moveLayer(String id, int index) { ZLayer layer = requireLayer(id); layers.remove(layer); layers.add(Math.max(0, Math.min(index, layers.size())), layer); invalidate(); }
    private void moveRelative(String id, String referenceId, boolean above) { if (id.equals(referenceId)) return; ZLayer layer = requireLayer(id); ZLayer reference = requireLayer(referenceId); layers.remove(layer); layers.add(layers.indexOf(reference) + (above ? 1 : 0), layer); invalidate(); }
    private void applyAlignment() { bounds.set(alignTarget(baseBounds)); }
    private RectF alignTarget(RectF source) {
        RectF result = new RectF(source);
        if (owner != null && (horizontalCentered || verticalCentered)) {
            RectF parent = owner.getComponentBounds();
            if (horizontalCentered) result.offsetTo(parent.centerX() - result.width() / 2f, result.top);
            if (verticalCentered) result.offsetTo(result.left, parent.centerY() - result.height() / 2f);
        }
        return result;
    }
    private MotionEvent toLocalEvent(MotionEvent source) {
        MotionEvent result = MotionEvent.obtain(source);
        float originX = bounds.left + translationX;
        float originY = bounds.top + translationY;
        float x = source.getX() - originX;
        float y = source.getY() - originY;
        if (currentScale != 1f) {
            float cx = bounds.width() / 2f; float cy = bounds.height() / 2f;
            x = (x - cx) / currentScale + cx;
            y = (y - cy) / currentScale + cy;
        }
        result.setLocation(x, y);
        return result;
    }
    private void animatePressTo(float target) {
        if (pressAnimator != null) pressAnimator.cancel();
        pressAnimator = ValueAnimator.ofFloat(currentScale, target);
        pressAnimator.setDuration(pressDuration);
        pressAnimator.setInterpolator(new DecelerateInterpolator());
        pressAnimator.addUpdateListener(value -> { currentScale = (float) value.getAnimatedValue(); invalidate(); });
        pressAnimator.start();
    }
    private void cancelContainerPress() { hostView.removeCallbacks(longClickRunnable); containerPressed = false; longClickFired = false; animatePressTo(1f); }
    private void cancelTouch() {
        blockedTouch = false;
        cancelContainerPress();
        if (touchTarget != null) {
            long now = SystemClock.uptimeMillis();
            MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
            touchLayer.dispatchTo(touchTarget, cancel); cancel.recycle();
            touchTarget = null; touchLayer = null;
        }
    }
    private void runFeedback() { if (soundAction != null) soundAction.run(); if (hapticAction != null) hapticAction.run(); }
    private void invalidate() { if (owner != null) owner.postInvalidateComponentOnAnimation(); }
    private void ensureActive() { if (released) throw new IllegalStateException("Container has been released: " + id); }

    private static String requireId(String value) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("ID cannot be blank."); return value.trim(); }
    private static RectF requireBounds(RectF value) { Objects.requireNonNull(value, "Bounds cannot be null."); if (value.width() <= 0f || value.height() <= 0f) throw new IllegalArgumentException("Bounds must be positive."); return new RectF(value); }
    private static float requireAlpha(float value) { if (!Float.isFinite(value)) throw new IllegalArgumentException("Alpha must be finite."); return Math.max(0f, Math.min(1f, value)); }
    private static float requirePressedScale(float value) { if (!(value > 0f) || value > 1f || !Float.isFinite(value)) throw new IllegalArgumentException("Pressed scale must be in (0, 1]."); return value; }
    private static void requireFinite(float value, String name) { if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite."); }
    private static float lerp(float a, float b, float f) { return a + (b - a) * f; }
    private static TimeInterpolator toInterpolator(Interpolator value) {
        switch (Objects.requireNonNull(value)) {
            case LINEAR: return new LinearInterpolator();
            case EASE_IN: return new AccelerateInterpolator();
            case EASE_OUT: return new DecelerateInterpolator();
            default: return new AccelerateDecelerateInterpolator();
        }
    }
}

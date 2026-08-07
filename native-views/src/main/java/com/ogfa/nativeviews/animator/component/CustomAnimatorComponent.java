package com.ogfa.nativeviews.animator.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.ogfa.nativeviews.animator.component.internal.MovementController;
import com.ogfa.nativeviews.animator.component.internal.PressController;
import com.ogfa.nativeviews.animator.component.layer.BitmapLayer;
import com.ogfa.nativeviews.animator.component.layer.ComponentLayer;
import com.ogfa.nativeviews.audio.NativeViewsSoundPlayer;
import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A ZLayer-owned component that composes ordered bitmap/GIF/Lottie/dynamic/effect layers. */
public final class CustomAnimatorComponent implements Component {
    public enum SoundMode { NONE, NATIVE_VIEWS, CUSTOM }
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
    public interface OnClickListener { void onClick(String id); }
    public interface OnLongClickListener { void onLongClick(String id); }

    private final Context context;
    private final String id;
    private final ArrayList<ComponentLayer> layers = new ArrayList<>();
    private final RectF baseBounds = new RectF();
    private final RectF layoutBounds = new RectF();
    private final RectF policyBounds = new RectF();
    private final RectF visualBounds = new RectF();
    private final PressController pressController;
    private final MovementController movementController = new MovementController();
    private final float figmaScale;

    private ComponentHost owner;
    private BoundsPolicy boundsPolicy;
    private BoundsResolver boundsResolver;
    private OnClickListener clickListener;
    private OnLongClickListener longClickListener;
    private long longClickDelay;
    private float pressedScale;
    private long pressDuration;
    private SoundMode soundMode;
    private Runnable soundAction;
    private Runnable hapticAction;
    private boolean clipToBounds;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private boolean visible = true;
    private boolean enabled = true;
    private float alpha = 1f;
    private boolean pressed;
    private boolean longClickFired;
    private boolean released;

    private final Runnable longClickRunnable;

    private CustomAnimatorComponent(Builder builder, View hostView) {
        context = builder.context.getApplicationContext();
        id = builder.id;
        longClickRunnable = () -> {
            if (!pressed || longClickListener == null || released) return;
            longClickFired = true;
            longClickListener.onLongClick(id);
            runFeedback();
            invalidate();
        };
        baseBounds.set(builder.resolveBounds(hostView));
        layoutBounds.set(baseBounds);
        figmaScale = builder.position == null ? 1f : builder.position.getScale(hostView);
        boundsPolicy = builder.boundsPolicy;
        boundsResolver = builder.boundsResolver;
        clickListener = builder.clickListener;
        longClickListener = builder.longClickListener;
        longClickDelay = builder.longClickDelay;
        pressedScale = builder.pressedScale;
        pressDuration = builder.pressDuration;
        soundMode = builder.soundMode;
        soundAction = builder.soundAction;
        hapticAction = builder.hapticAction;
        clipToBounds = builder.clipToBounds;
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        layers.addAll(builder.layers);
        ensureUniqueLayerIds();
        pressController = new PressController(scale -> invalidate());
        resolveLayoutAndLayers();
        if (soundMode == SoundMode.NATIVE_VIEWS) NativeViewsSoundPlayer.preload(context);
    }

    public static final class Builder implements ComponentFactory<CustomAnimatorComponent> {
        private final Context context;
        private final String id;
        private final ArrayList<ComponentLayer> layers = new ArrayList<>();
        private Position position;
        private Size size;
        private RectF bounds;
        private BoundsPolicy boundsPolicy = BoundsPolicy.DECLARED_REGION;
        private BoundsResolver boundsResolver;
        private OnClickListener clickListener;
        private OnLongClickListener longClickListener;
        private long longClickDelay = 500L;
        private float pressedScale = 0.96f;
        private long pressDuration = 100L;
        private SoundMode soundMode = SoundMode.NONE;
        private Runnable soundAction;
        private Runnable hapticAction;
        private boolean clipToBounds;
        private boolean horizontalCentered;
        private boolean verticalCentered;

        public Builder(Context context, String id, List<ComponentLayer> layers,
                       Position position, Size size) {
            this(context, id, layers);
            this.position = Objects.requireNonNull(position, "Position cannot be null.");
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
        }

        public Builder(Context context, String id, List<ComponentLayer> layers, RectF bounds) {
            this(context, id, layers);
            this.bounds = requireBounds(bounds);
        }

        public Builder(Context context, String id, Bitmap bitmap, Position position, Size size) {
            this(context, id, Collections.singletonList(BitmapLayer.create(
                    id + "_bitmap", bitmap, LayerRegion.matchComponent())), position, size);
        }

        public Builder(Context context, String id, Bitmap bitmap, RectF bounds) {
            this(context, id, Collections.singletonList(BitmapLayer.create(
                    id + "_bitmap", bitmap, LayerRegion.matchComponent())), bounds);
        }

        private Builder(Context context, String id, List<ComponentLayer> sourceLayers) {
            this.context = Objects.requireNonNull(context, "Context cannot be null.");
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Component id cannot be blank.");
            this.id = id;
            if (sourceLayers == null || sourceLayers.isEmpty())
                throw new IllegalArgumentException("At least one layer is required.");
            this.layers.addAll(sourceLayers);
        }

        public Builder setBoundsPolicy(BoundsPolicy policy) { boundsPolicy = Objects.requireNonNull(policy); return this; }
        public Builder setBoundsResolver(BoundsResolver resolver) { boundsPolicy = BoundsPolicy.CUSTOM; boundsResolver = Objects.requireNonNull(resolver); return this; }
        public Builder setClipToBounds(boolean value) { clipToBounds = value; return this; }
        public Builder setClickListener(OnClickListener listener) { clickListener = listener; return this; }
        public Builder setOnLongClickListener(OnLongClickListener listener) { longClickListener = listener; return this; }
        public Builder setLongClickDelay(long value) { if (value < 0) throw new IllegalArgumentException("Delay cannot be negative."); longClickDelay = value; return this; }
        public Builder setPressedScale(float value) { if (value <= 0f || value > 1f) throw new IllegalArgumentException("Pressed scale must be in (0, 1]."); pressedScale = value; return this; }
        public Builder setPressAnimationDuration(long value) { if (value < 0) throw new IllegalArgumentException("Duration cannot be negative."); pressDuration = value; return this; }
        public Builder setSoundMode(SoundMode value) { soundMode = Objects.requireNonNull(value); return this; }
        public Builder setSoundAction(Runnable action) { soundAction = Objects.requireNonNull(action); soundMode = SoundMode.CUSTOM; return this; }
        public Builder setHapticAction(Runnable action) { hapticAction = action; return this; }
        public Builder horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder verticalCenter(boolean value) { verticalCentered = value; return this; }

        @Override public CustomAnimatorComponent build(View hostView) {
            Objects.requireNonNull(hostView, "Host view cannot be null.");
            return new CustomAnimatorComponent(this, hostView);
        }

        private RectF resolveBounds(View hostView) {
            return bounds != null ? new RectF(bounds) : position.toRectF(hostView, size);
        }
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() { return new RectF(policyBounds); }
    public RectF getLayoutBounds() { return new RectF(layoutBounds); }
    public RectF getVisualBounds() { return new RectF(visualBounds); }
    public int getLayerCount() { return layers.size(); }
    public List<ComponentLayer> getLayers() { return Collections.unmodifiableList(layers); }
    public boolean containsLayer(String layerId) { return findLayer(layerId) != null; }
    public ComponentLayer findLayer(String layerId) { for (ComponentLayer layer : layers) if (layer.getId().equals(layerId)) return layer; return null; }
    public <T extends ComponentLayer> T findLayer(String layerId, Class<T> type) { ComponentLayer found = findLayer(layerId); return type.isInstance(found) ? type.cast(found) : null; }

    public CustomAnimatorComponent addLayer(ComponentLayer layer) { return addLayer(layers.size(), layer); }
    public CustomAnimatorComponent addLayer(int index, ComponentLayer layer) {
        Objects.requireNonNull(layer, "Layer cannot be null.");
        if (containsLayer(layer.getId())) throw new IllegalArgumentException("Duplicate layer id: " + layer.getId());
        if (index < 0 || index > layers.size()) throw new IndexOutOfBoundsException("Layer index: " + index);
        layers.add(index, layer); resolveLayoutAndLayers(); invalidate(); return this;
    }
    public boolean removeLayer(String layerId) { ComponentLayer layer = findLayer(layerId); if (layer == null) return false; layers.remove(layer); layer.release(); resolveLayoutAndLayers(); invalidate(); return true; }
    public CustomAnimatorComponent setLayerRegion(String layerId, LayerRegion region) { requireLayer(layerId).setRegion(region); resolveLayoutAndLayers(); invalidate(); return this; }
    public CustomAnimatorComponent setLayerVisible(String layerId, boolean visible) { requireLayer(layerId).setVisible(visible); recomputeBounds(); invalidate(); return this; }
    public CustomAnimatorComponent setLayerAlpha(String layerId, float alpha) { requireLayer(layerId).setAlpha(alpha); invalidate(); return this; }
    public void clearLayers() { for (ComponentLayer layer : layers) layer.release(); layers.clear(); resolveLayoutAndLayers(); invalidate(); }
    public CustomAnimatorComponent bringLayerToFront(String id) { return moveLayer(id, layers.size() - 1); }
    public CustomAnimatorComponent sendLayerToBack(String id) { return moveLayer(id, 0); }
    public CustomAnimatorComponent setLayerIndex(String id, int index) { return moveLayer(id, index); }
    public CustomAnimatorComponent moveLayerAbove(String id, String referenceId) { return moveRelative(id, referenceId, true); }
    public CustomAnimatorComponent moveLayerBelow(String id, String referenceId) { return moveRelative(id, referenceId, false); }

    public CustomAnimatorComponent setRegion(RectF bounds) { baseBounds.set(requireBounds(bounds)); resolveLayoutAndLayers(); invalidate(); return this; }
    public CustomAnimatorComponent setRegion(Position position, Size size) {
        View view = requireHostView();
        baseBounds.set(position.toRectF(view, size)); resolveLayoutAndLayers(); invalidate(); return this;
    }
    public CustomAnimatorComponent horizontalCenter(boolean value) { horizontalCentered = value; resolveLayoutAndLayers(); invalidate(); return this; }
    public CustomAnimatorComponent verticalCenter(boolean value) { verticalCentered = value; resolveLayoutAndLayers(); invalidate(); return this; }
    public CustomAnimatorComponent setBoundsPolicy(BoundsPolicy policy) { boundsPolicy = Objects.requireNonNull(policy); recomputeBounds(); invalidate(); return this; }
    public CustomAnimatorComponent setBoundsResolver(BoundsResolver resolver) { boundsPolicy = BoundsPolicy.CUSTOM; boundsResolver = Objects.requireNonNull(resolver); recomputeBounds(); invalidate(); return this; }
    public CustomAnimatorComponent setClipToBounds(boolean value) { clipToBounds = value; invalidate(); return this; }
    public CustomAnimatorComponent setAlpha(float value) { if (!Float.isFinite(value)) throw new IllegalArgumentException("Alpha must be finite."); alpha = Math.max(0f, Math.min(1f, value)); invalidate(); return this; }
    public float getAlpha() { return alpha; }
    @Override public boolean isVisible() { return visible; }
    public CustomAnimatorComponent setVisible(boolean value) { visible = value; if (!value) cancelPress(); invalidate(); return this; }
    @Override public boolean isEnabled() { return enabled; }
    public CustomAnimatorComponent setEnabled(boolean value) { enabled = value; if (!value) cancelPress(); return this; }

    @Override public void draw(Canvas canvas) {
        if (!visible || alpha <= 0f || released) return;
        int alphaSave = alpha >= 1f
                ? canvas.save()
                : canvas.saveLayerAlpha(visualBounds, Math.round(alpha * 255f));
        float scale = pressController.getScale();
        canvas.scale(scale, scale, policyBounds.centerX(), policyBounds.centerY());
        if (clipToBounds) canvas.clipRect(layoutBounds);
        for (ComponentLayer layer : layers) layer.draw(canvas);
        canvas.restoreToCount(alphaSave);
        if (needsNextFrame()) postInvalidate();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!visible || !enabled || released || (clickListener == null && longClickListener == null)) return false;
        boolean inside = policyBounds.contains(event.getX(), event.getY());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!inside) return false;
                pressed = true; longClickFired = false;
                pressController.animateTo(pressedScale, pressDuration);
                if (longClickListener != null) requireHostView().postDelayed(longClickRunnable, longClickDelay);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!pressed) return false;
                if (!inside) cancelPress();
                return pressed;
            case MotionEvent.ACTION_UP:
                if (!pressed) return false;
                requireHostView().removeCallbacks(longClickRunnable);
                pressed = false;
                pressController.animateTo(1f, pressDuration);
                if (inside && !longClickFired && clickListener != null) { clickListener.onClick(id); runFeedback(); }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (pressed) { cancelPress(); return true; }
                return false;
            default: return pressed;
        }
    }

    public void animateRegionTo(RectF target, long duration, Interpolator interpolator, Runnable completion) {
        RectF end = applyAlignment(requireBounds(target));
        movementController.start(layoutBounds, end, duration, toInterpolator(interpolator),
                bounds -> { baseBounds.set(bounds); layoutBounds.set(bounds); resolveLayersOnly(); invalidate(); },
                () -> { baseBounds.set(end); layoutBounds.set(end); resolveLayersOnly(); if (completion != null) completion.run(); });
    }
    public void animateRegionTo(Position position, Size size, long duration, Interpolator interpolator, Runnable completion) {
        animateRegionTo(position.toRectF(requireHostView(), size), duration, interpolator, completion);
    }
    public boolean isMoving() { return movementController.isRunning(); }
    public void pauseMovement() { movementController.pause(); }
    public void resumeMovement() { movementController.resume(); }
    public void cancelMovement() { movementController.cancel(); }
    public void finishMovement() { movementController.finish(); }

    @Override public void attach(ComponentHost host) { if (owner != null && owner != host) throw new IllegalStateException("Component already has a host."); owner = host; resolveLayoutAndLayers(); }
    @Override public void release() {
        if (released) return;
        released = true;
        cancelPress(); pressController.release(); movementController.cancel();
        for (ComponentLayer layer : layers) layer.release();
        owner = null;
    }

    private boolean needsNextFrame() { if (pressController.isRunning() || movementController.isRunning()) return true; for (ComponentLayer layer : layers) if (layer.isVisible() && layer.needsNextFrame()) return true; return false; }
    private void cancelPress() { if (owner != null) requireHostView().removeCallbacks(longClickRunnable); pressed = false; longClickFired = false; pressController.animateTo(1f, pressDuration); }
    private void runFeedback() {
        if (soundMode == SoundMode.NATIVE_VIEWS) NativeViewsSoundPlayer.playButtonSound(context);
        else if (soundMode == SoundMode.CUSTOM && soundAction != null) soundAction.run();
        if (hapticAction != null) hapticAction.run();
    }
    private void resolveLayoutAndLayers() { layoutBounds.set(applyAlignment(baseBounds)); resolveLayersOnly(); }
    private RectF applyAlignment(RectF source) {
        RectF result = new RectF(source);
        if (owner != null && (horizontalCentered || verticalCentered)) {
            RectF parent = owner.getComponentBounds();
            if (horizontalCentered) result.offsetTo(parent.centerX() - result.width() / 2f, result.top);
            if (verticalCentered) result.offsetTo(result.left, parent.centerY() - result.height() / 2f);
        }
        return result;
    }
    private void resolveLayersOnly() { for (ComponentLayer layer : layers) layer.resolveBounds(layoutBounds, figmaScale); recomputeBounds(); }
    private void recomputeBounds() {
        visualBounds.setEmpty();
        ArrayList<RectF> values = new ArrayList<>();
        for (ComponentLayer layer : layers) if (layer.isVisible()) { RectF rect = layer.getBounds(); values.add(rect); if (visualBounds.isEmpty()) visualBounds.set(rect); else visualBounds.union(rect); }
        if (visualBounds.isEmpty()) visualBounds.set(layoutBounds);
        if (boundsPolicy == BoundsPolicy.DECLARED_REGION || values.isEmpty()) policyBounds.set(layoutBounds);
        else if (boundsPolicy == BoundsPolicy.LAYER_UNION) policyBounds.set(visualBounds);
        else if (boundsPolicy == BoundsPolicy.LARGEST_LAYER) { RectF largest = values.get(0); for (RectF value : values) if (value.width() * value.height() > largest.width() * largest.height()) largest = value; policyBounds.set(largest); }
        else { if (boundsResolver == null) throw new IllegalStateException("CUSTOM bounds policy requires a resolver."); RectF resolved = boundsResolver.resolve(new RectF(layoutBounds), Collections.unmodifiableList(values)); policyBounds.set(requireBounds(resolved)); }
    }
    private CustomAnimatorComponent moveLayer(String id, int index) { if (index < 0 || index >= layers.size()) throw new IndexOutOfBoundsException("Layer index: " + index); ComponentLayer layer = requireLayer(id); layers.remove(layer); layers.add(index, layer); invalidate(); return this; }
    private CustomAnimatorComponent moveRelative(String id, String referenceId, boolean above) { if (id.equals(referenceId)) return this; ComponentLayer layer = requireLayer(id); ComponentLayer reference = requireLayer(referenceId); layers.remove(layer); int index = layers.indexOf(reference) + (above ? 1 : 0); layers.add(index, layer); invalidate(); return this; }
    private ComponentLayer requireLayer(String id) { ComponentLayer layer = findLayer(id); if (layer == null) throw new IllegalArgumentException("Unknown layer id: " + id); return layer; }
    private void ensureUniqueLayerIds() { ArrayList<String> ids = new ArrayList<>(); for (ComponentLayer layer : layers) { if (ids.contains(layer.getId())) throw new IllegalArgumentException("Duplicate layer id: " + layer.getId()); ids.add(layer.getId()); } }
    private View requireHostView() { if (owner == null) throw new IllegalStateException("Component must be attached to a ZLayer first."); return owner.getHostView(); }
    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
    private void postInvalidate() { if (owner != null) owner.postInvalidateComponentOnAnimation(); }
    private static RectF requireBounds(RectF bounds) { Objects.requireNonNull(bounds, "Bounds cannot be null."); if (bounds.width() <= 0f || bounds.height() <= 0f) throw new IllegalArgumentException("Bounds must have positive width and height."); return new RectF(bounds); }
    private static android.animation.TimeInterpolator toInterpolator(Interpolator value) { switch (Objects.requireNonNull(value)) { case LINEAR: return new LinearInterpolator(); case EASE_IN: return new AccelerateInterpolator(); case EASE_OUT: return new DecelerateInterpolator(); default: return new AccelerateDecelerateInterpolator(); } }
}

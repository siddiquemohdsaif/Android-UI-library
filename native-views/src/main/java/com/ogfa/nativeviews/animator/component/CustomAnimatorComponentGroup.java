package com.ogfa.nativeviews.animator.component;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.ogfa.nativeviews.animator.component.layer.ComponentLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns a group of {@link CustomAnimatorComponent} instances for one host {@link View}.
 *
 * <p>The group preserves insertion order for drawing and dispatches touches in reverse
 * order so the last added (visually topmost) component gets the first opportunity to
 * consume an event.</p>
 */
public final class CustomAnimatorComponentGroup implements AutoCloseable {

    private final View hostView;
    private final ArrayList<CustomAnimatorComponent> drawingOrder = new ArrayList<>();
    private final Map<String, CustomAnimatorComponent> componentsById = new HashMap<>();
    private boolean autoInvalidate = true;

    public CustomAnimatorComponentGroup(View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
    }

    /**
     * Builds and adds a component.
     */
    public CustomAnimatorComponent add(CustomAnimatorComponent.Builder builder) {
        Objects.requireNonNull(builder, "CustomAnimatorComponent.Builder cannot be null.");
        return add(builder.build());
    }

    /**
     * Adds a component. IDs must be non-empty and unique within this group.
     */
    public CustomAnimatorComponent add(CustomAnimatorComponent component) {
        Objects.requireNonNull(component, "CustomAnimatorComponent cannot be null.");

        String id = component.getId();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("CustomAnimatorComponent ID cannot be null or empty.");
        }
        if (componentsById.containsKey(id)) {
            throw new IllegalArgumentException(
                    "CustomAnimatorComponentGroup already contains a component with ID: " + id
            );
        }

        drawingOrder.add(component);
        componentsById.put(id, component);
        hostView.invalidate();
        return component;
    }

    public CustomAnimatorComponent find(String id) {
        return componentsById.get(id);
    }

    public boolean contains(String id) {
        return componentsById.containsKey(id);
    }

    /**
     * Clears the component's layers and removes it from the group.
     */
    public boolean remove(String id) {
        CustomAnimatorComponent component = componentsById.remove(id);
        if (component == null) {
            return false;
        }

        drawingOrder.remove(component);
        clearLayers(component);
        hostView.invalidate();
        return true;
    }

    public int size() {
        return drawingOrder.size();
    }

    public boolean isEmpty() {
        return drawingOrder.isEmpty();
    }

    /**
     * Draws all components in insertion order.
     */
    public void draw(Canvas canvas) {
        CustomAnimatorComponent.draw(canvas, drawingOrder);
        scheduleNextFrameIfEnabled();
    }

    /**
     * Draws only components intersecting the host view's local visible rectangle.
     */
    public void drawVisible(Canvas canvas) {
        drawVisible(canvas, hostView);
    }

    /**
     * Draws only components intersecting {@code visibleView}'s local visible rectangle.
     */
    public void drawVisible(Canvas canvas, View visibleView) {
        Objects.requireNonNull(visibleView, "Visible view cannot be null.");
        CustomAnimatorComponent.drawVisible(canvas, drawingOrder, visibleView);
        scheduleNextFrameIfEnabled();
    }

    /**
     * Dispatches touch to components from topmost to bottommost.
     */
    public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");

        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            onScrollChanged();
            return !drawingOrder.isEmpty();
        }

        boolean handled = CustomAnimatorComponent.handleTouch(event, drawingOrder);
        if (handled) {
            hostView.invalidate();
        }
        return handled;
    }

    /**
     * Cancels the pressed state, normally when a parent starts scrolling.
     */
    public void onScrollChanged() {
        CustomAnimatorComponent.handleTouchScrollChanged(drawingOrder);
        hostView.invalidate();
    }

    /**
     * Moves a component and returns false when the ID is not present.
     */
    public boolean animateToPosition(
            String id,
            float targetLeft,
            float targetTop,
            long duration,
            Runnable onComplete
    ) {
        CustomAnimatorComponent component = find(id);
        if (component == null) {
            return false;
        }

        component.animateToPositionWithValueAnimator(
                targetLeft,
                targetTop,
                duration,
                hostView,
                onComplete
        );
        return true;
    }

    /**
     * Controls whether draw methods automatically request the next animation frame.
     *
     * <p>Enabled by default for the simplest integration. Disable it for a completely
     * static group and invalidate the host only when its state changes.</p>
     */
    public CustomAnimatorComponentGroup setAutoInvalidate(boolean enabled) {
        autoInvalidate = enabled;
        return this;
    }

    public boolean isAutoInvalidate() {
        return autoInvalidate;
    }

    /**
     * Clears every layer and removes every component. The group can be reused afterward.
     */
    public void clear() {
        for (CustomAnimatorComponent component : drawingOrder) {
            clearLayers(component);
        }
        drawingOrder.clear();
        componentsById.clear();
        hostView.invalidate();
    }

    /**
     * Lifecycle-oriented alias for {@link #clear()}.
     */
    public void release() {
        clear();
    }

    @Override
    public void close() {
        release();
    }

    private void scheduleNextFrameIfEnabled() {
        if (autoInvalidate && !drawingOrder.isEmpty()) {
            hostView.postInvalidateOnAnimation();
        }
    }

    private static void clearLayers(CustomAnimatorComponent component) {
        for (ComponentLayer viewLayer : component.layers) {
            viewLayer.release();
        }
    }
}

package com.ogfa.nativeviews.button;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns a group of {@link AnimatedButton} instances for one host {@link View}.
 *
 * <p>The group preserves insertion order for drawing and dispatches touches in reverse
 * order so the last added (visually topmost) button gets the first opportunity to
 * consume an event.</p>
 */
public final class AnimatedButtonGroup implements AutoCloseable {

    private final View hostView;
    private final ArrayList<AnimatedButton> drawingOrder = new ArrayList<>();
    private final Map<String, AnimatedButton> buttonsById = new HashMap<>();
    private boolean autoInvalidate = true;

    public AnimatedButtonGroup(View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
    }

    /**
     * Builds and adds a button.
     */
    public AnimatedButton add(AnimatedButton.Builder builder) {
        Objects.requireNonNull(builder, "AnimatedButton.Builder cannot be null.");
        return add(builder.build());
    }

    /**
     * Adds a button. IDs must be non-empty and unique within this group.
     */
    public AnimatedButton add(AnimatedButton button) {
        Objects.requireNonNull(button, "AnimatedButton cannot be null.");

        String id = button.getId();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("AnimatedButton ID cannot be null or empty.");
        }
        if (buttonsById.containsKey(id)) {
            throw new IllegalArgumentException(
                    "AnimatedButtonGroup already contains a button with ID: " + id
            );
        }

        drawingOrder.add(button);
        buttonsById.put(id, button);
        hostView.invalidate();
        return button;
    }

    public AnimatedButton find(String id) {
        return buttonsById.get(id);
    }

    public boolean contains(String id) {
        return buttonsById.containsKey(id);
    }

    /**
     * Clears the button's layers and removes it from the group.
     */
    public boolean remove(String id) {
        AnimatedButton button = buttonsById.remove(id);
        if (button == null) {
            return false;
        }

        drawingOrder.remove(button);
        clearLayers(button);
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
     * Draws all buttons in insertion order.
     */
    public void draw(Canvas canvas) {
        AnimatedButton.Draw(canvas, drawingOrder);
        scheduleNextFrameIfEnabled();
    }

    /**
     * Draws only buttons intersecting the host view's local visible rectangle.
     */
    public void drawVisible(Canvas canvas) {
        drawVisible(canvas, hostView);
    }

    /**
     * Draws only buttons intersecting {@code visibleView}'s local visible rectangle.
     */
    public void drawVisible(Canvas canvas, View visibleView) {
        Objects.requireNonNull(visibleView, "Visible view cannot be null.");
        AnimatedButton.visibleDraw(canvas, drawingOrder, visibleView);
        scheduleNextFrameIfEnabled();
    }

    /**
     * Dispatches touch to buttons from topmost to bottommost.
     */
    public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");

        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            onScrollChanged();
            return !drawingOrder.isEmpty();
        }

        boolean handled = AnimatedButton.HandleTouch(event, drawingOrder);
        if (handled) {
            hostView.invalidate();
        }
        return handled;
    }

    /**
     * Cancels the pressed state, normally when a parent starts scrolling.
     */
    public void onScrollChanged() {
        AnimatedButton.HandleTouchScrollChanged(drawingOrder);
        hostView.invalidate();
    }

    /**
     * Moves a button and returns false when the ID is not present.
     */
    public boolean animateToPosition(
            String id,
            float targetLeft,
            float targetTop,
            long duration,
            Runnable onComplete
    ) {
        AnimatedButton button = find(id);
        if (button == null) {
            return false;
        }

        button.animateToPositionWithValueAnimator(
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
    public AnimatedButtonGroup setAutoInvalidate(boolean enabled) {
        autoInvalidate = enabled;
        return this;
    }

    public boolean isAutoInvalidate() {
        return autoInvalidate;
    }

    /**
     * Clears every layer and removes every button. The group can be reused afterward.
     */
    public void clear() {
        for (AnimatedButton button : drawingOrder) {
            clearLayers(button);
        }
        drawingOrder.clear();
        buttonsById.clear();
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

    private static void clearLayers(AnimatedButton button) {
        for (ViewLayer viewLayer : button.viewLayers) {
            viewLayer.clear();
        }
    }
}

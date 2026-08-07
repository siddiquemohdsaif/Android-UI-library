package com.ogfa.nativeviews.zlayer;

import android.graphics.Canvas;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.BackHandler;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.textfield.TextFieldHost;
import com.ogfa.nativeviews.textfield.TextInputCoordinator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ZLayerGroup implements ComponentHost, TextFieldHost,
        NestedComponentHost,
        ZLayerOwner, AutoCloseable {
    private final View hostView;
    private final ArrayList<ZLayer> layers = new ArrayList<>();
    private final Map<String, ZLayer> layersById = new LinkedHashMap<>();
    private final Map<String, Component> componentsById = new LinkedHashMap<>();
    private final TextInputCoordinator textInput;
    private Component touchTarget;
    private ZLayer touchLayer;
    private boolean blockedGesture;
    private boolean autoInvalidate = true;
    private boolean released;

    public ZLayerGroup(View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        textInput = new TextInputCoordinator(hostView);
    }

    public ZLayer addLayer(String id) {
        ensureActive();
        if (layersById.containsKey(id)) throw new IllegalArgumentException(
                "Duplicate layer ID: " + id);
        ZLayer layer = new ZLayer(this, id);
        layers.add(layer);
        layersById.put(layer.getId(), layer);
        invalidateComponent();
        return layer;
    }

    public ZLayer findLayer(String id) { ensureActive(); return layersById.get(id); }
    public boolean containsLayer(String id) { return findLayer(id) != null; }
    public int getLayerCount() { ensureActive(); return layers.size(); }

    public boolean removeLayer(String id) {
        ensureActive();
        ZLayer layer = layersById.remove(id);
        if (layer == null) return false;
        if (touchTarget != null && layer.contains(touchTarget.getId())) cancelTouch();
        layer.clear();
        layers.remove(layer);
        return true;
    }

    public void bringLayerToFront(String id) { moveLayer(id, layers.size() - 1); }
    public void sendLayerToBack(String id) { moveLayer(id, 0); }
    public int getLayerIndex(String id) { return layers.indexOf(requireLayer(id)); }
    public void setLayerIndex(String id, int index) {
        if (index < 0 || index >= layers.size()) throw new IndexOutOfBoundsException(
                "Layer index: " + index);
        moveLayer(id, index);
    }

    public void moveLayerAbove(String id, String referenceId) {
        ZLayer reference = requireLayer(referenceId);
        ZLayer layer = requireLayer(id);
        layers.remove(layer);
        layers.add(layers.indexOf(reference) + 1, layer);
        invalidateComponent();
    }

    public void moveLayerBelow(String id, String referenceId) {
        ZLayer reference = requireLayer(referenceId);
        ZLayer layer = requireLayer(id);
        layers.remove(layer);
        layers.add(Math.max(0, layers.indexOf(reference)), layer);
        invalidateComponent();
    }

    public Component findComponent(String id) {
        ensureActive();
        return componentsById.get(id);
    }

    public <T extends Component> T findComponent(String id, Class<T> type) {
        Component component = findComponent(id);
        return type.isInstance(component) ? type.cast(component) : null;
    }

    public boolean moveComponent(String id, String targetLayerId) {
        Component component = componentsById.get(id);
        ZLayer target = layersById.get(targetLayerId);
        if (component == null || target == null) return false;
        for (ZLayer layer : layers) {
            if (layer.contains(id)) {
                // Preserve the instance without releasing it.
                layer.detachForMove(component);
                target.attachMoved(component);
                invalidateComponent();
                return true;
            }
        }
        return false;
    }

    public void draw(Canvas canvas) {
        ensureActive();
        for (ZLayer layer : layers) layer.draw(canvas);
        if (textInput.hasFocusedField()) {
            hostView.postInvalidateDelayed(TextField.CURSOR_BLINK_INTERVAL_MS);
        }
        if (autoInvalidate && !componentsById.isEmpty()) {
            hostView.postInvalidateOnAnimation();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        ensureActive();
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            cancelTouch();
            blockedGesture = false;
            for (int i = layers.size() - 1; i >= 0; i--) {
                ZLayer layer = layers.get(i);
                touchTarget = layer.dispatchDown(event);
                if (touchTarget != null) {
                    touchLayer = layer;
                    return true;
                }
                if (layer.isVisible() && layer.isEnabled()
                        && (layer.getTouchPolicy() == ZLayer.TouchPolicy.MODAL
                        || (layer.getTouchPolicy() == ZLayer.TouchPolicy.BLOCK_BELOW
                        && layer.containsPoint(event.getX(), event.getY())))) {
                    blockedGesture = true;
                    return true;
                }
            }
            textInput.clearFocus();
            return false;
        }
        if (blockedGesture) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                blockedGesture = false;
            }
            return true;
        }
        if (touchTarget == null) return false;
        Component target = touchTarget;
        boolean handled = touchLayer == null
                ? target.onTouchEvent(event)
                : touchLayer.dispatchTo(target, event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            touchTarget = null;
            touchLayer = null;
        }
        return handled;
    }

    /** Dispatches Back to the topmost visible, enabled Back-aware component. */
    public boolean onBackPressed() {
        ensureActive();
        for (int layerIndex = layers.size() - 1; layerIndex >= 0; layerIndex--) {
            ZLayer layer = layers.get(layerIndex);
            if (!layer.isVisible() || !layer.isEnabled()) continue;
            java.util.List<Component> components = layer.getComponents();
            for (int index = components.size() - 1; index >= 0; index--) {
                Component component = components.get(index);
                if (component.isVisible() && component.isEnabled()
                        && component instanceof BackHandler
                        && ((BackHandler) component).onBackPressed()) {
                    return true;
                }
            }
        }
        return false;
    }

    public InputConnection onCreateInputConnection(EditorInfo attrs) {
        return textInput.onCreateInputConnection(attrs);
    }
    public boolean onCheckIsTextEditor() { return textInput.hasFocusedField(); }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return textInput.onKeyDown(keyCode, event);
    }
    public TextField getFocusedTextField() { return textInput.getFocusedField(); }
    public ZLayerGroup setAutoInvalidate(boolean value) { autoInvalidate = value; return this; }
    public boolean isAutoInvalidate() { return autoInvalidate; }

    public void clear() {
        ensureActive();
        cancelTouch();
        for (ZLayer layer : new ArrayList<>(layers)) layer.clear();
        layers.clear();
        layersById.clear();
        componentsById.clear();
        textInput.clear();
        invalidateComponent();
    }

    public void release() {
        if (released) return;
        clear();
        released = true;
    }
    @Override public void close() { release(); }
    @Override public View getHostView() { return hostView; }
    @Override public void invalidateComponent() { if (!released) hostView.invalidate(); }
    @Override public void postInvalidateComponentOnAnimation() {
        if (!released) hostView.postInvalidateOnAnimation();
    }
    @Override public boolean requestFocus(TextField field) {
        return textInput.requestFocus(field);
    }
    @Override public void clearFocus(TextField field) { textInput.clearFocus(field); }
    @Override public void restartInput() { textInput.restartInput(); }
    @Override public void updateSelection(TextField field) { textInput.updateSelection(field); }

    @Override
    public void registerLayerComponent(Component component) {
        ensureActive();
        Objects.requireNonNull(component, "Component cannot be null.");
        String id = component.getId();
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException(
                "Component ID cannot be null or blank.");
        if (componentsById.containsKey(id)) throw new IllegalArgumentException(
                "Duplicate component ID: " + id);
        component.attach(this);
        componentsById.put(id, component);
        if (component instanceof TextField) textInput.register((TextField) component);
    }

    @Override
    public void unregisterLayerComponent(Component component) {
        componentsById.remove(component.getId());
        if (component instanceof TextField) textInput.unregister(component.getId());
        if (touchTarget == component) cancelTouch();
    }

    /**
     * Registers a child that is drawn by a composite component.
     */
    public void registerNestedComponent(Component component) {
        ensureActive();
        Objects.requireNonNull(component, "Component cannot be null.");
        String id = component.getId();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Component ID cannot be null or blank."
            );
        }
        if (componentsById.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate component ID: " + id);
        }
        componentsById.put(id, component);
        if (component instanceof TextField) {
            textInput.register((TextField) component);
        }
    }

    public void unregisterNestedComponent(Component component) {
        unregisterLayerComponent(component);
    }

    @Override
    public void invalidateLayer() {
        invalidateComponent();
    }

    private void cancelTouch() {
        blockedGesture = false;
        if (touchTarget != null) {
            long now = android.os.SystemClock.uptimeMillis();
            MotionEvent cancel = MotionEvent.obtain(now, now,
                    MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
            if (touchLayer == null) {
                touchTarget.onTouchEvent(cancel);
            } else {
                touchLayer.dispatchTo(touchTarget, cancel);
            }
            cancel.recycle();
            touchTarget = null;
        }
        touchLayer = null;
    }
    private ZLayer requireLayer(String id) {
        ZLayer layer = layersById.get(id);
        if (layer == null) throw new IllegalArgumentException("Unknown layer: " + id);
        return layer;
    }
    private void moveLayer(String id, int index) {
        ZLayer layer = requireLayer(id);
        layers.remove(layer);
        layers.add(Math.max(0, Math.min(index, layers.size())), layer);
        invalidateComponent();
    }
    private void ensureActive() {
        if (released) throw new IllegalStateException("ZLayerGroup has been released.");
    }
}

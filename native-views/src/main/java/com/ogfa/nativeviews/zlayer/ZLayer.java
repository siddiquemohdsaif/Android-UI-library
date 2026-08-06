package com.ogfa.nativeviews.zlayer;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ZLayer {
    public enum TouchPolicy { PASS_THROUGH, BLOCK_BELOW, MODAL }

    private final ZLayerGroup owner;
    private final String id;
    private final ArrayList<Component> components = new ArrayList<>();
    private boolean visible = true;
    private boolean enabled = true;
    private TouchPolicy touchPolicy = TouchPolicy.PASS_THROUGH;

    ZLayer(ZLayerGroup owner, String id) {
        this.owner = owner;
        this.id = requireId(id);
    }

    public String getId() { return id; }
    public boolean isVisible() { return visible; }
    public boolean isEnabled() { return enabled; }
    public TouchPolicy getTouchPolicy() { return touchPolicy; }

    public ZLayer setVisible(boolean visible) {
        this.visible = visible;
        owner.invalidateComponent();
        return this;
    }

    public ZLayer setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ZLayer setTouchPolicy(TouchPolicy policy) {
        touchPolicy = Objects.requireNonNull(policy, "Touch policy cannot be null.");
        return this;
    }

    public <T extends Component> T add(ComponentFactory<T> factory) {
        return add(Objects.requireNonNull(factory, "Factory cannot be null.")
                .build(owner.getHostView()));
    }

    public <T extends Component> T add(T component) {
        owner.register(this, component);
        components.add(component);
        owner.invalidateComponent();
        return component;
    }

    public Component find(String componentId) {
        for (Component component : components) {
            if (component.getId().equals(componentId)) return component;
        }
        return null;
    }

    public boolean contains(String componentId) { return find(componentId) != null; }
    public int size() { return components.size(); }
    public boolean isEmpty() { return components.isEmpty(); }
    public List<Component> getComponents() {
        return Collections.unmodifiableList(components);
    }

    public boolean remove(String componentId) {
        Component component = find(componentId);
        if (component == null) return false;
        components.remove(component);
        owner.unregister(component);
        component.release();
        owner.invalidateComponent();
        return true;
    }

    public void clear() {
        for (Component component : new ArrayList<>(components)) {
            owner.unregister(component);
            component.release();
        }
        components.clear();
        owner.invalidateComponent();
    }

    public void bringToFront(String id) { moveToIndex(id, components.size() - 1); }
    public void sendToBack(String id) { moveToIndex(id, 0); }

    public void moveAbove(String id, String referenceId) {
        Component reference = require(referenceId);
        Component component = require(id);
        components.remove(component);
        components.add(components.indexOf(reference) + 1, component);
        owner.invalidateComponent();
    }

    public void moveBelow(String id, String referenceId) {
        Component reference = require(referenceId);
        Component component = require(id);
        components.remove(component);
        components.add(Math.max(0, components.indexOf(reference)), component);
        owner.invalidateComponent();
    }

    public void setComponentIndex(String id, int index) {
        if (index < 0 || index >= components.size()) {
            throw new IndexOutOfBoundsException("Component index: " + index);
        }
        moveToIndex(id, index);
    }

    public int getComponentIndex(String id) { return components.indexOf(require(id)); }

    void draw(Canvas canvas) {
        if (!visible) return;
        for (Component component : components) {
            if (component.isVisible()) component.draw(canvas);
        }
    }

    Component dispatchDown(MotionEvent event) {
        if (!visible || !enabled) return null;
        for (int i = components.size() - 1; i >= 0; i--) {
            Component component = components.get(i);
            if (component.isVisible() && component.isEnabled()
                    && component.onTouchEvent(event)) return component;
        }
        return null;
    }

    boolean containsPoint(float x, float y) {
        for (Component component : components) {
            RectF bounds = component.getBounds();
            if (component.isVisible() && bounds.contains(x, y)) return true;
        }
        return false;
    }

    void detachForMove(Component component) {
        components.remove(component);
    }

    void attachMoved(Component component) {
        components.add(component);
    }

    private void moveToIndex(String id, int index) {
        Component component = require(id);
        components.remove(component);
        components.add(Math.max(0, Math.min(index, components.size())), component);
        owner.invalidateComponent();
    }

    private Component require(String id) {
        Component component = find(id);
        if (component == null) throw new IllegalArgumentException(
                "Layer " + this.id + " has no component: " + id);
        return component;
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Layer ID cannot be null or blank.");
        }
        return id.trim();
    }
}

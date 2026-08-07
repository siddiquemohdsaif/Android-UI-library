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

    private final ZLayerOwner owner;
    private final String id;
    private final ArrayList<Component> components = new ArrayList<>();
    private boolean visible = true;
    private boolean enabled = true;
    private float translationX;
    private float translationY;
    private TouchPolicy touchPolicy = TouchPolicy.PASS_THROUGH;

    public ZLayer(ZLayerOwner owner, String id) {
        this.owner = Objects.requireNonNull(owner, "Layer owner cannot be null.");
        this.id = requireId(id);
    }

    public String getId() { return id; }
    public boolean isVisible() { return visible; }
    public boolean isEnabled() { return enabled; }
    public float getTranslationX() {
        return owner.ownsLayerTranslation()
                ? owner.getOwnedLayerTranslationX()
                : translationX;
    }
    public float getTranslationY() {
        return owner.ownsLayerTranslation()
                ? owner.getOwnedLayerTranslationY()
                : translationY;
    }
    public TouchPolicy getTouchPolicy() { return touchPolicy; }

    public ZLayer setVisible(boolean visible) {
        this.visible = visible;
        owner.invalidateLayer();
        return this;
    }

    public ZLayer setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ZLayer setTranslation(float translationX, float translationY) {
        requireFiniteTranslation(translationX, "X");
        requireFiniteTranslation(translationY, "Y");
        if (owner.ownsLayerTranslation()) {
            owner.setOwnedLayerTranslation(translationX, translationY);
            return this;
        }
        this.translationX = translationX;
        this.translationY = translationY;
        owner.invalidateLayer();
        return this;
    }

    public ZLayer setTranslationX(float translationX) {
        return setTranslation(translationX, getTranslationY());
    }

    public ZLayer setTranslationY(float translationY) {
        return setTranslation(getTranslationX(), translationY);
    }

    public ZLayer resetTranslation() {
        return setTranslation(0f, 0f);
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
        if (find(component.getId()) != null) {
            throw new IllegalArgumentException(
                    "Duplicate component ID in layer " + id + ": "
                            + component.getId()
            );
        }
        owner.registerLayerComponent(component);
        components.add(component);
        owner.invalidateLayer();
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
        owner.unregisterLayerComponent(component);
        component.release();
        owner.invalidateLayer();
        return true;
    }

    public void clear() {
        for (Component component : new ArrayList<>(components)) {
            owner.unregisterLayerComponent(component);
            component.release();
        }
        components.clear();
        owner.invalidateLayer();
    }

    public void bringToFront(String id) { moveToIndex(id, components.size() - 1); }
    public void sendToBack(String id) { moveToIndex(id, 0); }

    public void moveAbove(String id, String referenceId) {
        Component reference = require(referenceId);
        Component component = require(id);
        components.remove(component);
        components.add(components.indexOf(reference) + 1, component);
        owner.invalidateLayer();
    }

    public void moveBelow(String id, String referenceId) {
        Component reference = require(referenceId);
        Component component = require(id);
        components.remove(component);
        components.add(Math.max(0, components.indexOf(reference)), component);
        owner.invalidateLayer();
    }

    public void setComponentIndex(String id, int index) {
        if (index < 0 || index >= components.size()) {
            throw new IndexOutOfBoundsException("Component index: " + index);
        }
        moveToIndex(id, index);
    }

    public int getComponentIndex(String id) { return components.indexOf(require(id)); }

    public void draw(Canvas canvas) {
        if (!visible) return;
        int saveCount = canvas.save();
        if (!owner.ownsLayerTranslation()) {
            canvas.translate(translationX, translationY);
        }
        for (Component component : components) {
            if (component.isVisible()) component.draw(canvas);
        }
        canvas.restoreToCount(saveCount);
    }

    public Component dispatchDown(MotionEvent event) {
        if (!visible || !enabled) return null;
        MotionEvent translatedEvent = translatedEvent(event);
        try {
            for (int i = components.size() - 1; i >= 0; i--) {
                Component component = components.get(i);
                if (component.isVisible() && component.isEnabled()
                        && component.onTouchEvent(translatedEvent)) {
                    return component;
                }
            }
        } finally {
            translatedEvent.recycle();
        }
        return null;
    }

    public boolean dispatchTo(Component component, MotionEvent event) {
        Objects.requireNonNull(component, "Component cannot be null.");
        MotionEvent translatedEvent = translatedEvent(event);
        try {
            return component.onTouchEvent(translatedEvent);
        } finally {
            translatedEvent.recycle();
        }
    }

    public boolean containsPoint(float x, float y) {
        float localX = owner.ownsLayerTranslation()
                ? x
                : x - translationX;
        float localY = owner.ownsLayerTranslation()
                ? y
                : y - translationY;
        for (Component component : components) {
            RectF bounds = component.getBounds();
            if (component.isVisible()
                    && bounds.contains(localX, localY)) return true;
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
        owner.invalidateLayer();
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

    private MotionEvent translatedEvent(MotionEvent event) {
        MotionEvent translatedEvent = MotionEvent.obtain(event);
        if (!owner.ownsLayerTranslation()) {
            translatedEvent.offsetLocation(-translationX, -translationY);
        }
        return translatedEvent;
    }

    private static void requireFiniteTranslation(float value, String axis) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Layer translation " + axis + " must be finite."
            );
        }
    }
}

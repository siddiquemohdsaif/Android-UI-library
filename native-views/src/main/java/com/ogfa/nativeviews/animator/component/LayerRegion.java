package com.ogfa.nativeviews.animator.component;

import android.graphics.RectF;

/** Immutable layer bounds relative to its owning CustomAnimatorComponent. */
public final class LayerRegion {
    private enum Unit { MATCH, FIGMA, PX }

    private final Unit unit;
    private final float left;
    private final float top;
    private final float width;
    private final float height;

    private LayerRegion(Unit unit, float left, float top, float width, float height) {
        if (unit != Unit.MATCH && (width <= 0f || height <= 0f
                || !Float.isFinite(left) || !Float.isFinite(top)
                || !Float.isFinite(width) || !Float.isFinite(height))) {
            throw new IllegalArgumentException("LayerRegion values must be finite and size must be positive.");
        }
        this.unit = unit;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public static LayerRegion matchComponent() {
        return new LayerRegion(Unit.MATCH, 0f, 0f, 0f, 0f);
    }

    public static LayerRegion figma(float left, float top, float width, float height) {
        return new LayerRegion(Unit.FIGMA, left, top, width, height);
    }

    public static LayerRegion px(float left, float top, float width, float height) {
        return new LayerRegion(Unit.PX, left, top, width, height);
    }

    public RectF resolve(RectF componentBounds, float figmaScale) {
        if (unit == Unit.MATCH) return new RectF(componentBounds);
        float scale = unit == Unit.FIGMA ? figmaScale : 1f;
        float x = componentBounds.left + left * scale;
        float y = componentBounds.top + top * scale;
        return new RectF(x, y, x + width * scale, y + height * scale);
    }
}

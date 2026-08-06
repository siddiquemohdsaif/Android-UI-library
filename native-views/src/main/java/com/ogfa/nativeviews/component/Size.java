package com.ogfa.nativeviews.component;

import java.util.Objects;

/**
 * Immutable Figma/design-space dimensions used with {@link Position}.
 */
public final class Size {

    private final float width;
    private final float height;

    public Size(float width, float height) {
        this.width = requirePositiveFinite(width, "Width");
        this.height = requirePositiveFinite(height, "Height");
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Size)) {
            return false;
        }
        Size size = (Size) object;
        return Float.compare(width, size.width) == 0
                && Float.compare(height, size.height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "Size{width=" + width + ", height=" + height + '}';
    }

    private static float requirePositiveFinite(float value, String label) {
        if (value <= 0f || !Float.isFinite(value)) {
            throw new IllegalArgumentException(
                    label + " must be positive and finite."
            );
        }
        return value;
    }
}

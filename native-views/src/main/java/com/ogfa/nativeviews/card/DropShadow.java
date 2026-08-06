package com.ogfa.nativeviews.card;

import android.graphics.Color;

import java.util.Objects;

/**
 * Immutable Figma-style drop-shadow measurements.
 */
public final class DropShadow {

    public static final DropShadow DEFAULT = new DropShadow(
            0f,
            4f,
            28f,
            4f,
            Color.argb(5, 0, 0, 0)
    );
    public static final DropShadow NONE = new DropShadow(
            0f,
            0f,
            0f,
            0f,
            Color.TRANSPARENT
    );

    private final float x;
    private final float y;
    private final float blur;
    private final float spread;
    private final int color;

    public DropShadow(
            float x,
            float y,
            float blur,
            float spread,
            int color
    ) {
        this.x = requireFinite(x, "Shadow x");
        this.y = requireFinite(y, "Shadow y");
        this.blur = requireNonNegativeFinite(blur, "Shadow blur");
        this.spread = requireNonNegativeFinite(spread, "Shadow spread");
        this.color = color;
    }

    public static DropShadow defaultShadow() {
        return DEFAULT;
    }

    public static DropShadow none() {
        return NONE;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getBlur() {
        return blur;
    }

    public float getSpread() {
        return spread;
    }

    public int getColor() {
        return color;
    }

    DropShadow scale(float scale) {
        return new DropShadow(
                x * scale,
                y * scale,
                blur * scale,
                spread * scale,
                color
        );
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof DropShadow)) return false;
        DropShadow that = (DropShadow) object;
        return Float.compare(x, that.x) == 0
                && Float.compare(y, that.y) == 0
                && Float.compare(blur, that.blur) == 0
                && Float.compare(spread, that.spread) == 0
                && color == that.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, blur, spread, color);
    }

    @Override
    public String toString() {
        return "DropShadow{x=" + x + ", y=" + y + ", blur=" + blur
                + ", spread=" + spread + ", color=" + color + '}';
    }

    private static float requireFinite(float value, String label) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite.");
        }
        return value;
    }

    private static float requireNonNegativeFinite(
            float value,
            String label
    ) {
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(
                    label + " must be non-negative and finite."
            );
        }
        return value;
    }
}

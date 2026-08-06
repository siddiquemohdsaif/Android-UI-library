package com.ogfa.nativeviews.button;

import java.util.Objects;

/**
 * Immutable spacing that restricts a Button's text region.
 *
 * <p>Values use Figma/design-space units for a {@code Position + Size} button
 * and runtime pixels for a {@code RectF} button.</p>
 */
public final class TextInsets {

    private static final TextInsets NONE = new TextInsets(0f, 0f, 0f, 0f);

    private final float left;
    private final float top;
    private final float right;
    private final float bottom;

    public TextInsets(float left, float top, float right, float bottom) {
        this.left = requireInset(left, "Left");
        this.top = requireInset(top, "Top");
        this.right = requireInset(right, "Right");
        this.bottom = requireInset(bottom, "Bottom");
    }

    public static TextInsets none() {
        return NONE;
    }

    public static TextInsets all(float value) {
        return new TextInsets(value, value, value, value);
    }

    public static TextInsets horizontal(float value) {
        return new TextInsets(value, 0f, value, 0f);
    }

    public static TextInsets vertical(float value) {
        return new TextInsets(0f, value, 0f, value);
    }

    public static TextInsets of(
            float left,
            float top,
            float right,
            float bottom
    ) {
        return new TextInsets(left, top, right, bottom);
    }

    public float getLeft() {
        return left;
    }

    public float getTop() {
        return top;
    }

    public float getRight() {
        return right;
    }

    public float getBottom() {
        return bottom;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof TextInsets)) return false;
        TextInsets that = (TextInsets) object;
        return Float.compare(left, that.left) == 0
                && Float.compare(top, that.top) == 0
                && Float.compare(right, that.right) == 0
                && Float.compare(bottom, that.bottom) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, top, right, bottom);
    }

    @Override
    public String toString() {
        return "TextInsets{left=" + left
                + ", top=" + top
                + ", right=" + right
                + ", bottom=" + bottom + '}';
    }

    private static float requireInset(float value, String name) {
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(
                    name + " text inset must be non-negative and finite."
            );
        }
        return value;
    }
}

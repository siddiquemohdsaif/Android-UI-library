package com.ogfa.nativeviews.component;

import java.util.Objects;

/**
 * Immutable project-wide configuration for converting Figma/design-space
 * measurements into runtime pixels.
 */
public final class FigmaConfig {

    public static final float DEFAULT_REFERENCE_WIDTH = 1080f;

    private static volatile FigmaConfig defaultConfig =
            new FigmaConfig(DEFAULT_REFERENCE_WIDTH);

    private final float referenceWidth;

    public FigmaConfig(float referenceWidth) {
        this.referenceWidth = requirePositiveFinite(
                referenceWidth,
                "Figma reference width"
        );
    }

    public float getReferenceWidth() {
        return referenceWidth;
    }

    public float getScale(float runtimeWidth) {
        return requirePositiveFinite(runtimeWidth, "Runtime width")
                / referenceWidth;
    }

    public float toRuntime(float figmaValue, float runtimeWidth) {
        if (!Float.isFinite(figmaValue)) {
            throw new IllegalArgumentException(
                    "Figma value must be finite."
            );
        }
        return figmaValue * getScale(runtimeWidth);
    }

    public static FigmaConfig getDefault() {
        return defaultConfig;
    }

    /**
     * Sets the configuration captured by subsequently created Positions.
     * Existing Positions and components remain unchanged.
     */
    public static void setDefault(FigmaConfig config) {
        defaultConfig = Objects.requireNonNull(
                config,
                "Default Figma config cannot be null."
        );
    }

    public static void resetDefault() {
        defaultConfig = new FigmaConfig(DEFAULT_REFERENCE_WIDTH);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof FigmaConfig)) return false;
        FigmaConfig that = (FigmaConfig) object;
        return Float.compare(referenceWidth, that.referenceWidth) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(referenceWidth);
    }

    @Override
    public String toString() {
        return "FigmaConfig{referenceWidth=" + referenceWidth + '}';
    }

    private static float requirePositiveFinite(float value, String label) {
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(
                    label + " must be positive and finite."
            );
        }
        return value;
    }
}

package com.ogfa.nativeviews.component;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.View;

import java.util.Objects;

/**
 * Converts Figma-space measurements into a runtime {@link RectF}.
 *
 * <p>All Figma measurements are scaled uniformly from the configured reference width.
 * The default reference width is 1080 px. Create positions only after the host view has
 * been measured, for example from {@code onSizeChanged()}.</p>
 */
public final class Position {

    public enum HorizontalMarginFrom {
        LEFT,
        RIGHT
    }

    public enum VerticalMarginFrom {
        TOP,
        BOTTOM
    }

    private final HorizontalMarginFrom horizontalMarginFrom;
    private final VerticalMarginFrom verticalMarginFrom;
    private final float horizontalMargin;
    private final float verticalMargin;
    private final FigmaConfig figmaConfig;
    private final View hostView;

    /**
     * Creates a position using the current default Figma reference width.
     */
    public Position(
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin
    ) {
        this(
                horizontalMarginFrom,
                verticalMarginFrom,
                horizontalMargin,
                verticalMargin,
                FigmaConfig.getDefault(),
                null
        );
    }

    /**
     * Creates a position using an explicit Figma configuration.
     */
    public Position(
            FigmaConfig figmaConfig,
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin
    ) {
        this(
                horizontalMarginFrom,
                verticalMarginFrom,
                horizontalMargin,
                verticalMargin,
                figmaConfig,
                null
        );
    }

    /**
     * Creates a host-bound position that layer factories can evaluate directly.
     */
    public Position(
            View hostView,
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin
    ) {
        this(
                horizontalMarginFrom,
                verticalMarginFrom,
                horizontalMargin,
                verticalMargin,
                FigmaConfig.getDefault(),
                hostView
        );
    }

    /**
     * Creates a host-bound position with an explicit Figma configuration.
     */
    public Position(
            View hostView,
            FigmaConfig figmaConfig,
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin
    ) {
        this(
                horizontalMarginFrom,
                verticalMarginFrom,
                horizontalMargin,
                verticalMargin,
                figmaConfig,
                hostView
        );
    }

    private Position(
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin,
            FigmaConfig figmaConfig,
            View hostView
    ) {
        requireNonNegativeFinite(horizontalMargin, "Horizontal margin");
        requireNonNegativeFinite(verticalMargin, "Vertical margin");

        this.horizontalMarginFrom = Objects.requireNonNull(
                horizontalMarginFrom, "Horizontal anchor cannot be null.");
        this.verticalMarginFrom = Objects.requireNonNull(
                verticalMarginFrom, "Vertical anchor cannot be null.");
        this.horizontalMargin = horizontalMargin;
        this.verticalMargin = verticalMargin;
        this.figmaConfig = Objects.requireNonNull(
                figmaConfig,
                "Figma config cannot be null."
        );
        this.hostView = hostView;
    }

    public FigmaConfig getFigmaConfig() {
        return figmaConfig;
    }

    public float getScale(View hostView) {
        Objects.requireNonNull(hostView, "Host view cannot be null.");
        return figmaConfig.getScale(hostView.getWidth());
    }

    public float getScale() {
        return getScale(requireHostView());
    }

    /**
     * Converts a Figma/design-space value with this position's reference width.
     */
    public float toRuntimePixels(View hostView, float figmaValue) {
        Objects.requireNonNull(hostView, "Host view cannot be null.");
        requireNonNegativeFinite(figmaValue, "Figma value");
        return figmaConfig.toRuntime(figmaValue, hostView.getWidth());
    }

    /**
     * Converts a Figma/design-space value using the bound host view.
     */
    public float toRuntimePixels(float figmaValue) {
        return toRuntimePixels(requireHostView(), figmaValue);
    }

    /**
     * Uses the bitmap dimensions as the element dimensions in Figma space.
     */
    public RectF toRectF(View hostView, Bitmap bitmap) {
        return toRectF(
                hostView.getWidth(),
                hostView.getHeight(),
                bitmap.getWidth(),
                bitmap.getHeight()
        );
    }

    /**
     * Uses the bound host view and bitmap dimensions.
     */
    public RectF toRectF(Bitmap bitmap) {
        return toRectF(requireHostView(), bitmap);
    }

    /**
     * Converts explicit Figma-space element dimensions using a measured host view.
     */
    public RectF toRectF(
            View hostView,
            float figmaElementWidth,
            float figmaElementHeight
    ) {
        return toRectF(
                hostView.getWidth(),
                hostView.getHeight(),
                figmaElementWidth,
                figmaElementHeight
        );
    }

    /**
     * Uses the bound host view and explicit Figma-space element dimensions.
     */
    public RectF toRectF(float figmaElementWidth, float figmaElementHeight) {
        return toRectF(requireHostView(), figmaElementWidth, figmaElementHeight);
    }

    /**
     * Converts a Figma/design-space size using a measured host view.
     */
    public RectF toRectF(View hostView, Size size) {
        Objects.requireNonNull(size, "Size cannot be null.");
        return toRectF(hostView, size.getWidth(), size.getHeight());
    }

    /**
     * Converts a Figma/design-space size using the bound host view.
     */
    public RectF toRectF(Size size) {
        return toRectF(requireHostView(), size);
    }

    /**
     * Converts Figma-space margins and element dimensions into runtime pixels.
     */
    public RectF toRectF(
            float runtimeWidth,
            float runtimeHeight,
            float figmaElementWidth,
            float figmaElementHeight
    ) {
        requirePositiveFinite(runtimeWidth, "Runtime width");
        requirePositiveFinite(runtimeHeight, "Runtime height");
        requirePositiveFinite(figmaElementWidth, "Element width");
        requirePositiveFinite(figmaElementHeight, "Element height");

        float scale = figmaConfig.getScale(runtimeWidth);
        float scaledHorizontalMargin = horizontalMargin * scale;
        float scaledVerticalMargin = verticalMargin * scale;
        float width = figmaElementWidth * scale;
        float height = figmaElementHeight * scale;

        float left = horizontalMarginFrom == HorizontalMarginFrom.LEFT
                ? scaledHorizontalMargin
                : runtimeWidth - scaledHorizontalMargin - width;
        float top = verticalMarginFrom == VerticalMarginFrom.TOP
                ? scaledVerticalMargin
                : runtimeHeight - scaledVerticalMargin - height;

        return new RectF(left, top, left + width, top + height);
    }

    /**
     * Converts a Figma/design-space size using explicit runtime dimensions.
     */
    public RectF toRectF(
            float runtimeWidth,
            float runtimeHeight,
            Size size
    ) {
        Objects.requireNonNull(size, "Size cannot be null.");
        return toRectF(
                runtimeWidth,
                runtimeHeight,
                size.getWidth(),
                size.getHeight()
        );
    }

    private static void requirePositiveFinite(float value, String name) {
        if (value <= 0f || Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be a positive finite value.");
        }
    }

    private static void requireNonNegativeFinite(float value, String name) {
        if (value < 0f || Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IllegalArgumentException(name + " must be a non-negative finite value.");
        }
    }

    private View requireHostView() {
        if (hostView == null) {
            throw new IllegalStateException(
                    "This Position is not bound to a host View. "
                            + "Pass a View to the Position constructor or toRectF()."
            );
        }
        return hostView;
    }
}

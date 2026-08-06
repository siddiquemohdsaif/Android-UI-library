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

    public static final float INITIAL_FIGMA_REFERENCE_WIDTH = 1080f;

    private static volatile float defaultFigmaReferenceWidth = INITIAL_FIGMA_REFERENCE_WIDTH;

    private final HorizontalMarginFrom horizontalMarginFrom;
    private final VerticalMarginFrom verticalMarginFrom;
    private final float horizontalMargin;
    private final float verticalMargin;
    private final float figmaReferenceWidth;
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
                defaultFigmaReferenceWidth,
                null
        );
    }

    /**
     * Creates a position using a custom Figma reference width.
     */
    public Position(
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin,
            float figmaReferenceWidth
    ) {
        this(
                horizontalMarginFrom,
                verticalMarginFrom,
                horizontalMargin,
                verticalMargin,
                figmaReferenceWidth,
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
                defaultFigmaReferenceWidth,
                hostView
        );
    }

    /**
     * Creates a host-bound position with a custom Figma reference width.
     */
    public Position(
            View hostView,
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin,
            float figmaReferenceWidth
    ) {
        this(
                horizontalMarginFrom,
                verticalMarginFrom,
                horizontalMargin,
                verticalMargin,
                figmaReferenceWidth,
                hostView
        );
    }

    private Position(
            HorizontalMarginFrom horizontalMarginFrom,
            VerticalMarginFrom verticalMarginFrom,
            float horizontalMargin,
            float verticalMargin,
            float figmaReferenceWidth,
            View hostView
    ) {
        requireNonNegativeFinite(horizontalMargin, "Horizontal margin");
        requireNonNegativeFinite(verticalMargin, "Vertical margin");
        requirePositiveFinite(figmaReferenceWidth, "Figma reference width");

        this.horizontalMarginFrom = Objects.requireNonNull(
                horizontalMarginFrom, "Horizontal anchor cannot be null.");
        this.verticalMarginFrom = Objects.requireNonNull(
                verticalMarginFrom, "Vertical anchor cannot be null.");
        this.horizontalMargin = horizontalMargin;
        this.verticalMargin = verticalMargin;
        this.figmaReferenceWidth = figmaReferenceWidth;
        this.hostView = hostView;
    }

    /**
     * Changes the reference width used by constructors that omit {@code figmaReferenceWidth}.
     */
    public static void setDefaultFigmaReferenceWidth(float figmaReferenceWidth) {
        requirePositiveFinite(figmaReferenceWidth, "Figma reference width");
        defaultFigmaReferenceWidth = figmaReferenceWidth;
    }

    public static float getDefaultFigmaReferenceWidth() {
        return defaultFigmaReferenceWidth;
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

        float scale = runtimeWidth / figmaReferenceWidth;
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

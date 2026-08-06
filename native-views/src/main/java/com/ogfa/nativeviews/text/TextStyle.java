package com.ogfa.nativeviews.text;

import android.graphics.Color;
import android.graphics.Typeface;

import java.util.Objects;

/**
 * Immutable styling shared by Canvas {@link Text} components.
 */
public final class TextStyle {

    enum FontSource {
        DEFAULT,
        RESOURCE,
        ASSET,
        TYPEFACE
    }

    enum DimensionUnit {
        REGION,
        PIXELS
    }

    final FontSource fontSource;
    final int fontResourceId;
    final String fontAssetPath;
    final Typeface typeface;
    final float textSize;
    final DimensionUnit textSizeUnit;
    final int textColor;
    final float alpha;
    final float letterSpacing;
    final float lineSpacing;
    final float lineSpacingMultiplier;
    final float horizontalPadding;
    final float verticalPadding;
    final Text.Alignment alignment;
    final Text.VerticalAlignment verticalAlignment;
    final Text.Overflow overflow;
    final int maxLines;
    final boolean wrapEnabled;
    final float shadowRadius;
    final float shadowDx;
    final float shadowDy;
    final int shadowColor;

    private TextStyle(Builder builder) {
        fontSource = builder.fontSource;
        fontResourceId = builder.fontResourceId;
        fontAssetPath = builder.fontAssetPath;
        typeface = builder.typeface;
        textSize = builder.textSize;
        textSizeUnit = builder.textSizeUnit;
        textColor = builder.textColor;
        alpha = builder.alpha;
        letterSpacing = builder.letterSpacing;
        lineSpacing = builder.lineSpacing;
        lineSpacingMultiplier = builder.lineSpacingMultiplier;
        horizontalPadding = builder.horizontalPadding;
        verticalPadding = builder.verticalPadding;
        alignment = builder.alignment;
        verticalAlignment = builder.verticalAlignment;
        overflow = builder.overflow;
        maxLines = builder.maxLines;
        wrapEnabled = builder.wrapEnabled;
        shadowRadius = builder.shadowRadius;
        shadowDx = builder.shadowDx;
        shadowDy = builder.shadowDy;
        shadowColor = builder.shadowColor;
    }

    public static final class Builder {

        private FontSource fontSource = FontSource.DEFAULT;
        private int fontResourceId;
        private String fontAssetPath;
        private Typeface typeface = Typeface.DEFAULT;
        private float textSize = -1f;
        private DimensionUnit textSizeUnit = DimensionUnit.REGION;
        private int textColor = Color.WHITE;
        private float alpha = 1f;
        private float letterSpacing;
        private float lineSpacing;
        private float lineSpacingMultiplier = 1f;
        private float horizontalPadding;
        private float verticalPadding;
        private Text.Alignment alignment = Text.Alignment.START;
        private Text.VerticalAlignment verticalAlignment =
                Text.VerticalAlignment.TOP;
        private Text.Overflow overflow = Text.Overflow.CLIP;
        private int maxLines = Integer.MAX_VALUE;
        private boolean wrapEnabled = true;
        private float shadowRadius;
        private float shadowDx;
        private float shadowDy;
        private int shadowColor = Color.TRANSPARENT;

        public Builder() {
        }

        public Builder(TextStyle style) {
            Objects.requireNonNull(style, "TextStyle cannot be null.");
            fontSource = style.fontSource;
            fontResourceId = style.fontResourceId;
            fontAssetPath = style.fontAssetPath;
            typeface = style.typeface;
            textSize = style.textSize;
            textSizeUnit = style.textSizeUnit;
            textColor = style.textColor;
            alpha = style.alpha;
            letterSpacing = style.letterSpacing;
            lineSpacing = style.lineSpacing;
            lineSpacingMultiplier = style.lineSpacingMultiplier;
            horizontalPadding = style.horizontalPadding;
            verticalPadding = style.verticalPadding;
            alignment = style.alignment;
            verticalAlignment = style.verticalAlignment;
            overflow = style.overflow;
            maxLines = style.maxLines;
            wrapEnabled = style.wrapEnabled;
            shadowRadius = style.shadowRadius;
            shadowDx = style.shadowDx;
            shadowDy = style.shadowDy;
            shadowColor = style.shadowColor;
        }

        public Builder useDefaultFont() {
            fontSource = FontSource.DEFAULT;
            fontResourceId = 0;
            fontAssetPath = null;
            typeface = Typeface.DEFAULT;
            return this;
        }

        public Builder setFont(int fontResourceId) {
            if (fontResourceId == 0) {
                throw new IllegalArgumentException(
                        "Font resource ID cannot be zero."
                );
            }
            fontSource = FontSource.RESOURCE;
            this.fontResourceId = fontResourceId;
            fontAssetPath = null;
            typeface = null;
            return this;
        }

        public Builder setFontAsset(String assetPath) {
            String normalized = requireText(assetPath, "Font asset path");
            fontSource = FontSource.ASSET;
            fontResourceId = 0;
            fontAssetPath = normalized;
            typeface = null;
            return this;
        }

        public Builder setFont(Typeface typeface) {
            fontSource = FontSource.TYPEFACE;
            fontResourceId = 0;
            fontAssetPath = null;
            this.typeface = Objects.requireNonNull(
                    typeface,
                    "Typeface cannot be null."
            );
            return this;
        }

        /**
         * Uses Figma units with Position regions and runtime pixels with RectF.
         */
        public Builder setTextSize(float size) {
            textSize = requirePositiveFinite(size, "Text size");
            textSizeUnit = DimensionUnit.REGION;
            return this;
        }

        public Builder setTextSizePx(float pixels) {
            textSize = requirePositiveFinite(pixels, "Text size");
            textSizeUnit = DimensionUnit.PIXELS;
            return this;
        }

        public Builder setTextColor(int color) {
            textColor = color;
            return this;
        }

        public Builder setAlpha(float alpha) {
            if (!Float.isFinite(alpha) || alpha < 0f || alpha > 1f) {
                throw new IllegalArgumentException(
                        "Alpha must be finite and in the 0..1 range."
                );
            }
            this.alpha = alpha;
            return this;
        }

        public Builder setLetterSpacing(float spacing) {
            letterSpacing = requireNonNegativeFinite(
                    spacing,
                    "Letter spacing"
            );
            return this;
        }

        public Builder setLineSpacing(float spacing) {
            lineSpacing = requireNonNegativeFinite(spacing, "Line spacing");
            return this;
        }

        public Builder setLineSpacingMultiplier(float multiplier) {
            lineSpacingMultiplier = requirePositiveFinite(
                    multiplier,
                    "Line spacing multiplier"
            );
            return this;
        }

        public Builder setPadding(float horizontal, float vertical) {
            horizontalPadding = requireNonNegativeFinite(
                    horizontal,
                    "Horizontal padding"
            );
            verticalPadding = requireNonNegativeFinite(
                    vertical,
                    "Vertical padding"
            );
            return this;
        }

        public Builder setAlignment(Text.Alignment alignment) {
            this.alignment = Objects.requireNonNull(
                    alignment,
                    "Text alignment cannot be null."
            );
            return this;
        }

        public Builder setVerticalAlignment(
                Text.VerticalAlignment alignment
        ) {
            verticalAlignment = Objects.requireNonNull(
                    alignment,
                    "Vertical alignment cannot be null."
            );
            return this;
        }

        public Builder setOverflow(Text.Overflow overflow) {
            this.overflow = Objects.requireNonNull(
                    overflow,
                    "Text overflow cannot be null."
            );
            return this;
        }

        public Builder setMaxLines(int maxLines) {
            if (maxLines <= 0) {
                throw new IllegalArgumentException(
                        "Maximum lines must be greater than zero."
                );
            }
            this.maxLines = maxLines;
            return this;
        }

        public Builder setWrapEnabled(boolean enabled) {
            wrapEnabled = enabled;
            return this;
        }

        public Builder setShadow(
                float radius,
                float dx,
                float dy,
                int color
        ) {
            shadowRadius = requireNonNegativeFinite(radius, "Shadow radius");
            if (!Float.isFinite(dx) || !Float.isFinite(dy)) {
                throw new IllegalArgumentException(
                        "Shadow offsets must be finite."
                );
            }
            shadowDx = dx;
            shadowDy = dy;
            shadowColor = color;
            return this;
        }

        public Builder clearShadow() {
            shadowRadius = 0f;
            shadowDx = 0f;
            shadowDy = 0f;
            shadowColor = Color.TRANSPARENT;
            return this;
        }

        public TextStyle build() {
            return new TextStyle(this);
        }

        private static float requirePositiveFinite(
                float value,
                String label
        ) {
            if (value <= 0f || !Float.isFinite(value)) {
                throw new IllegalArgumentException(
                        label + " must be positive and finite."
                );
            }
            return value;
        }

        private static float requireNonNegativeFinite(
                float value,
                String label
        ) {
            if (value < 0f || !Float.isFinite(value)) {
                throw new IllegalArgumentException(
                        label + " must be non-negative and finite."
                );
            }
            return value;
        }

        private static String requireText(String value, String label) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        label + " cannot be null or blank."
                );
            }
            return value.trim();
        }
    }
}

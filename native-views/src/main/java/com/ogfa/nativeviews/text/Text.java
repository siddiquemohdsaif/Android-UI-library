package com.ogfa.nativeviews.text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.text.internal.TypefaceCache;

import java.util.Objects;

/**
 * A non-editable native Android text component rendered directly on a Canvas.
 *
 * <p>Instances are created through {@link Builder} and owned by a
 * a ZLayer. A region is supplied either as Figma-space
 * {@code Position + Size} or as an explicit runtime {@link RectF}.</p>
 */
public final class Text implements Component {

    public enum Alignment {
        START,
        CENTER,
        END
    }

    public enum VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM
    }

    public enum Overflow {
        CLIP,
        ELLIPSIZE_START,
        ELLIPSIZE_MIDDLE,
        ELLIPSIZE_END
    }

    private final Context context;
    private final View hostView;
    private final String id;
    private final RectF bounds = new RectF();
    private final RectF contentBounds = new RectF();
    private final TextPaint paint = new TextPaint(
            TextPaint.ANTI_ALIAS_FLAG
                    | TextPaint.SUBPIXEL_TEXT_FLAG
                    | TextPaint.DITHER_FLAG
    );

    private ComponentHost owner;
    private CharSequence value;
    private TextStyle style;
    private StaticLayout layout;
    private float dimensionScale;
    private float layoutTop;
    private boolean visible = true;
    private boolean enabled;
    private boolean released;
    private boolean touchCaptured;
    private boolean clickCancelled;
    private OnClickListener clickListener;

    private Text(Builder builder, View hostView) {
        context = builder.context.getApplicationContext();
        this.hostView = Objects.requireNonNull(
                hostView,
                "Host view cannot be null."
        );
        id = requireId(builder.id);
        value = Objects.requireNonNull(builder.value, "Text cannot be null.");
        style = builder.styleBuilder.build();
        enabled = builder.enabled;
        clickListener = builder.clickListener;
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        rebuildLayout();
    }

    public String getId() {
        return id;
    }

    public CharSequence getText() {
        return value;
    }

    public RectF getBounds() {
        return new RectF(bounds);
    }

    public float getMeasuredTextWidth() {
        if (layout == null) {
            return 0f;
        }
        float width = 0f;
        for (int line = 0; line < layout.getLineCount(); line++) {
            width = Math.max(width, layout.getLineWidth(line));
        }
        return Math.min(width, contentBounds.width());
    }

    public float getMeasuredTextHeight() {
        return layout == null ? 0f : Math.min(
                layout.getHeight(),
                contentBounds.height()
        );
    }

    public boolean isVisible() {
        return visible;
    }

    public Text setVisible(boolean visible) {
        ensureActive();
        this.visible = visible;
        invalidate();
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Text setEnabled(boolean enabled) {
        ensureActive();
        this.enabled = enabled;
        return this;
    }

    public boolean isClickable() {
        return clickListener != null;
    }

    public Text setOnClickListener(OnClickListener listener) {
        ensureActive();
        clickListener = listener;
        return this;
    }

    public Text setText(CharSequence text) {
        ensureActive();
        value = Objects.requireNonNull(text, "Text cannot be null.");
        rebuildLayout();
        return this;
    }

    public Text setStyle(TextStyle style) {
        ensureActive();
        this.style = Objects.requireNonNull(style, "TextStyle cannot be null.");
        rebuildLayout();
        return this;
    }

    public Text setTextColor(int color) {
        return updateStyle(new TextStyle.Builder(style).setTextColor(color));
    }

    public Text setAlpha(float alpha) {
        return updateStyle(new TextStyle.Builder(style).setAlpha(alpha));
    }

    public Text setTextSize(float size) {
        return updateStyle(new TextStyle.Builder(style).setTextSize(size));
    }

    public Text setTextSizePx(float pixels) {
        return updateStyle(
                new TextStyle.Builder(style).setTextSizePx(pixels)
        );
    }

    public Text setLetterSpacing(float spacing) {
        return updateStyle(
                new TextStyle.Builder(style).setLetterSpacing(spacing)
        );
    }

    public Text setLetterSpacingPx(float pixels) {
        return updateStyle(
                new TextStyle.Builder(style).setLetterSpacingPx(pixels)
        );
    }

    public Text setLineSpacing(float spacing) {
        return updateStyle(
                new TextStyle.Builder(style).setLineSpacing(spacing)
        );
    }

    public Text setLineSpacingPx(float pixels) {
        return updateStyle(
                new TextStyle.Builder(style).setLineSpacingPx(pixels)
        );
    }

    public Text setPadding(float horizontal, float vertical) {
        return updateStyle(
                new TextStyle.Builder(style).setPadding(horizontal, vertical)
        );
    }

    public Text setPaddingPx(float horizontal, float vertical) {
        return updateStyle(
                new TextStyle.Builder(style).setPaddingPx(
                        horizontal,
                        vertical
                )
        );
    }

    public Text setShadow(float radius, float dx, float dy, int color) {
        return updateStyle(
                new TextStyle.Builder(style).setShadow(radius, dx, dy, color)
        );
    }

    public Text setShadowPx(float radius, float dx, float dy, int color) {
        return updateStyle(
                new TextStyle.Builder(style).setShadowPx(
                        radius,
                        dx,
                        dy,
                        color
                )
        );
    }

    public Text clearShadow() {
        return updateStyle(new TextStyle.Builder(style).clearShadow());
    }

    public Text setFont(int fontResourceId) {
        return updateStyle(
                new TextStyle.Builder(style).setFont(fontResourceId)
        );
    }

    public Text setFontAsset(String assetPath) {
        return updateStyle(
                new TextStyle.Builder(style).setFontAsset(assetPath)
        );
    }

    public Text setFont(Typeface typeface) {
        return updateStyle(new TextStyle.Builder(style).setFont(typeface));
    }

    public Text useDefaultFont() {
        return updateStyle(new TextStyle.Builder(style).useDefaultFont());
    }

    public FontVariation getFontVariation() {
        return style.fontVariation;
    }

    public Text setFontVariations(FontVariation variation) {
        return updateStyle(
                new TextStyle.Builder(style).setFontVariations(variation)
        );
    }

    public Text clearFontVariations() {
        return updateStyle(
                new TextStyle.Builder(style).clearFontVariations()
        );
    }

    public Text setAlignment(Alignment alignment) {
        return updateStyle(
                new TextStyle.Builder(style).setAlignment(alignment)
        );
    }

    public Text setVerticalAlignment(VerticalAlignment alignment) {
        return updateStyle(
                new TextStyle.Builder(style).setVerticalAlignment(alignment)
        );
    }

    public Text setMaxLines(int maxLines) {
        return updateStyle(
                new TextStyle.Builder(style).setMaxLines(maxLines)
        );
    }

    public Text setOverflow(Overflow overflow) {
        return updateStyle(
                new TextStyle.Builder(style).setOverflow(overflow)
        );
    }

    public Text setWrapEnabled(boolean enabled) {
        return updateStyle(
                new TextStyle.Builder(style).setWrapEnabled(enabled)
        );
    }

    public Text setRegion(Position position, Size size) {
        ensureActive();
        resolveRegion(
                Objects.requireNonNull(position, "Position cannot be null."),
                Objects.requireNonNull(size, "Size cannot be null."),
                null
        );
        rebuildLayout();
        return this;
    }

    public Text setRegion(RectF bounds) {
        ensureActive();
        resolveRegion(
                null,
                null,
                new RectF(Objects.requireNonNull(
                        bounds,
                        "Bounds cannot be null."
                ))
        );
        rebuildLayout();
        return this;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        switch (event.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                touchCaptured = acceptsTouch(event.getX(), event.getY());
                clickCancelled = false;
                return touchCaptured;
            case android.view.MotionEvent.ACTION_MOVE:
                if (!touchCaptured) return false;
                if (!acceptsTouch(event.getX(), event.getY())) clickCancelled = true;
                return true;
            case android.view.MotionEvent.ACTION_UP:
                if (!touchCaptured) return false;
                boolean click = !clickCancelled
                        && acceptsTouch(event.getX(), event.getY());
                touchCaptured = false;
                if (click) performClick();
                return true;
            case android.view.MotionEvent.ACTION_CANCEL:
                boolean handled = touchCaptured;
                touchCaptured = false;
                clickCancelled = true;
                return handled;
            default:
                return touchCaptured;
        }
    }

    public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || layout == null) {
            return;
        }
        int saveCount = canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(contentBounds.left, layoutTop);
        layout.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        owner = null;
        layout = null;
    }

    @Override
    public void attach(ComponentHost owner) {
        ensureActive();
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException(
                    "Text already belongs to another component host."
            );
        }
        this.owner = owner;
    }

    private boolean acceptsTouch(float x, float y) {
        return !released
                && visible
                && enabled
                && clickListener != null
                && bounds.contains(x, y);
    }

    private void performClick() {
        if (!released
                && visible
                && enabled
                && clickListener != null) {
            clickListener.onClick(id);
        }
    }

    private Text updateStyle(TextStyle.Builder builder) {
        ensureActive();
        style = builder.build();
        rebuildLayout();
        return this;
    }

    private void resolveRegion(
            Position position,
            Size size,
            RectF explicitBounds
    ) {
        if (explicitBounds != null) {
            requireBounds(explicitBounds);
            bounds.set(explicitBounds);
            dimensionScale = FigmaConfig.getDefault().getScale(
                    hostView.getWidth()
            );
            return;
        }
        Objects.requireNonNull(position, "Position cannot be null.");
        Objects.requireNonNull(size, "Size cannot be null.");
        RectF resolved = position.toRectF(hostView, size);
        requireBounds(resolved);
        bounds.set(resolved);
        dimensionScale = position.getScale(hostView);
    }

    private void rebuildLayout() {
        ensureActive();
        applyPaint();

        float horizontalPadding = scaleDimension(
                style.horizontalPadding,
                style.paddingUnit
        );
        float verticalPadding = scaleDimension(
                style.verticalPadding,
                style.paddingUnit
        );
        contentBounds.set(bounds);
        contentBounds.inset(horizontalPadding, verticalPadding);
        if (contentBounds.width() <= 0f || contentBounds.height() <= 0f) {
            throw new IllegalArgumentException(
                    "Text padding leaves no drawable content region."
            );
        }

        int availableWidth = Math.max(1, (int) Math.floor(
                contentBounds.width()
        ));
        int maxLines = style.wrapEnabled ? style.maxLines : 1;
        if ((style.overflow == Overflow.ELLIPSIZE_START
                || style.overflow == Overflow.ELLIPSIZE_MIDDLE)
                && maxLines != 1) {
            throw new IllegalArgumentException(
                    "Start and middle ellipsizing require maxLines = 1."
            );
        }

        StaticLayout.Builder layoutBuilder = StaticLayout.Builder.obtain(
                        value,
                        0,
                        value.length(),
                        paint,
                        availableWidth
                )
                .setAlignment(toLayoutAlignment(style.alignment))
                .setIncludePad(false)
                .setLineSpacing(
                        scaleDimension(
                                style.lineSpacing,
                                style.lineSpacingUnit
                        ),
                        style.lineSpacingMultiplier
                )
                .setMaxLines(maxLines);

        TextUtils.TruncateAt truncateAt = toTruncateAt(style.overflow);
        if (truncateAt != null) {
            layoutBuilder
                    .setEllipsize(truncateAt)
                    .setEllipsizedWidth(availableWidth);
        }

        layout = layoutBuilder.build();
        switch (style.verticalAlignment) {
            case CENTER:
                layoutTop = contentBounds.top
                        + (contentBounds.height() - layout.getHeight()) / 2f;
                break;
            case BOTTOM:
                layoutTop = contentBounds.bottom - layout.getHeight();
                break;
            case TOP:
            default:
                layoutTop = contentBounds.top;
                break;
        }
        invalidate();
    }

    private void applyPaint() {
        paint.setTypeface(resolveTypeface());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            paint.setFontVariationSettings(
                    style.fontVariation == null
                            ? null
                            : style.fontVariation.toSettings()
            );
        }
        float textSize = style.textSize > 0f
                ? scaleTextSize(style)
                : bounds.height() * 0.5f;
        paint.setTextSize(textSize);
        paint.setColor(style.textColor);
        paint.setAlpha(Math.round(
                Color.alpha(style.textColor) * style.alpha
        ));
        float letterSpacingPx = scaleDimension(
                style.letterSpacing,
                style.letterSpacingUnit
        );
        paint.setLetterSpacing(letterSpacingPx / textSize);
        if (style.shadowRadius > 0f) {
            paint.setShadowLayer(
                    scaleDimension(style.shadowRadius, style.shadowUnit),
                    scaleDimension(style.shadowDx, style.shadowUnit),
                    scaleDimension(style.shadowDy, style.shadowUnit),
                    style.shadowColor
            );
        } else {
            paint.clearShadowLayer();
        }
    }

    private Typeface resolveTypeface() {
        switch (style.fontSource) {
            case RESOURCE:
                return TypefaceCache.fromResource(
                        context,
                        style.fontResourceId
                );
            case ASSET:
                return TypefaceCache.fromAsset(
                        context,
                        style.fontAssetPath
                );
            case TYPEFACE:
                return style.typeface;
            case DEFAULT:
            default:
                return Typeface.DEFAULT;
        }
    }

    private float scaleTextSize(TextStyle style) {
        return style.textSizeUnit == TextStyle.DimensionUnit.PIXELS
                ? style.textSize
                : style.textSize * dimensionScale;
    }

    private float scaleDimension(
            float value,
            TextStyle.DimensionUnit unit
    ) {
        return unit == TextStyle.DimensionUnit.PIXELS
                ? value
                : value * dimensionScale;
    }

    private void invalidate() {
        if (owner != null) {
            owner.postInvalidateComponentOnAnimation();
        }
    }

    private void ensureActive() {
        if (released) {
            throw new IllegalStateException("Text has been released: " + id);
        }
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Text ID cannot be null or blank."
            );
        }
        return id.trim();
    }

    private static void requireBounds(RectF bounds) {
        if (!Float.isFinite(bounds.left)
                || !Float.isFinite(bounds.top)
                || !Float.isFinite(bounds.right)
                || !Float.isFinite(bounds.bottom)
                || bounds.width() <= 0f
                || bounds.height() <= 0f) {
            throw new IllegalArgumentException(
                    "Text bounds must have positive finite dimensions."
            );
        }
    }

    private static Layout.Alignment toLayoutAlignment(Alignment alignment) {
        switch (alignment) {
            case CENTER:
                return Layout.Alignment.ALIGN_CENTER;
            case END:
                return Layout.Alignment.ALIGN_OPPOSITE;
            case START:
            default:
                return Layout.Alignment.ALIGN_NORMAL;
        }
    }

    private static TextUtils.TruncateAt toTruncateAt(Overflow overflow) {
        switch (overflow) {
            case ELLIPSIZE_START:
                return TextUtils.TruncateAt.START;
            case ELLIPSIZE_MIDDLE:
                return TextUtils.TruncateAt.MIDDLE;
            case ELLIPSIZE_END:
                return TextUtils.TruncateAt.END;
            case CLIP:
            default:
                return null;
        }
    }

    public interface OnClickListener {
        void onClick(String id);
    }

    public static final class Builder implements ComponentFactory<Text> {

        private final Context context;
        private final String id;
        private final CharSequence value;
        private final Position position;
        private final Size size;
        private final RectF explicitBounds;
        private TextStyle.Builder styleBuilder = new TextStyle.Builder();
        private boolean enabled = true;
        private OnClickListener clickListener;

        public Builder(
                Context context,
                String id,
                CharSequence text,
                Position position,
                Size size
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            value = Objects.requireNonNull(text, "Text cannot be null.");
            this.position = Objects.requireNonNull(
                    position,
                    "Position cannot be null."
            );
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
            explicitBounds = null;
        }

        public Builder(
                Context context,
                String id,
                CharSequence text,
                RectF bounds
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            value = Objects.requireNonNull(text, "Text cannot be null.");
            explicitBounds = new RectF(Objects.requireNonNull(
                    bounds,
                    "Bounds cannot be null."
            ));
            position = null;
            size = null;
        }

        public Builder setStyle(TextStyle style) {
            styleBuilder = new TextStyle.Builder(
                    Objects.requireNonNull(style, "TextStyle cannot be null.")
            );
            return this;
        }

        public Builder useDefaultFont() {
            styleBuilder.useDefaultFont();
            return this;
        }

        public Builder setFont(int fontResourceId) {
            styleBuilder.setFont(fontResourceId);
            return this;
        }

        public Builder setFontAsset(String assetPath) {
            styleBuilder.setFontAsset(assetPath);
            return this;
        }

        public Builder setFont(Typeface typeface) {
            styleBuilder.setFont(typeface);
            return this;
        }

        public Builder setFontVariations(FontVariation variation) {
            styleBuilder.setFontVariations(variation);
            return this;
        }

        public Builder clearFontVariations() {
            styleBuilder.clearFontVariations();
            return this;
        }

        public Builder setTextSize(float size) {
            styleBuilder.setTextSize(size);
            return this;
        }

        public Builder setTextSizePx(float pixels) {
            styleBuilder.setTextSizePx(pixels);
            return this;
        }

        public Builder setTextColor(int color) {
            styleBuilder.setTextColor(color);
            return this;
        }

        public Builder setAlpha(float alpha) {
            styleBuilder.setAlpha(alpha);
            return this;
        }

        public Builder setLetterSpacing(float spacing) {
            styleBuilder.setLetterSpacing(spacing);
            return this;
        }

        public Builder setLetterSpacingPx(float pixels) {
            styleBuilder.setLetterSpacingPx(pixels);
            return this;
        }

        public Builder setLineSpacing(float spacing) {
            styleBuilder.setLineSpacing(spacing);
            return this;
        }

        public Builder setLineSpacingPx(float pixels) {
            styleBuilder.setLineSpacingPx(pixels);
            return this;
        }

        public Builder setLineSpacingMultiplier(float multiplier) {
            styleBuilder.setLineSpacingMultiplier(multiplier);
            return this;
        }

        public Builder setPadding(float horizontal, float vertical) {
            styleBuilder.setPadding(horizontal, vertical);
            return this;
        }

        public Builder setPaddingPx(float horizontal, float vertical) {
            styleBuilder.setPaddingPx(horizontal, vertical);
            return this;
        }

        public Builder setAlignment(Alignment alignment) {
            styleBuilder.setAlignment(alignment);
            return this;
        }

        public Builder setVerticalAlignment(
                VerticalAlignment alignment
        ) {
            styleBuilder.setVerticalAlignment(alignment);
            return this;
        }

        public Builder setOverflow(Overflow overflow) {
            styleBuilder.setOverflow(overflow);
            return this;
        }

        public Builder setMaxLines(int maxLines) {
            styleBuilder.setMaxLines(maxLines);
            return this;
        }

        public Builder setWrapEnabled(boolean enabled) {
            styleBuilder.setWrapEnabled(enabled);
            return this;
        }

        public Builder setShadow(
                float radius,
                float dx,
                float dy,
                int color
        ) {
            styleBuilder.setShadow(radius, dx, dy, color);
            return this;
        }

        public Builder setShadowPx(
                float radius,
                float dx,
                float dy,
                int color
        ) {
            styleBuilder.setShadowPx(radius, dx, dy, color);
            return this;
        }

        public Builder clearShadow() {
            styleBuilder.clearShadow();
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setOnClickListener(OnClickListener listener) {
            clickListener = listener;
            return this;
        }

        @Override
        public Text build(View hostView) {
            return new Text(this, hostView);
        }
    }
}

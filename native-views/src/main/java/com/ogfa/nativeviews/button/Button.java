package com.ogfa.nativeviews.button;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.text.TextStyle;

import java.util.Objects;

/**
 * An image-backed Canvas button with an optional text label.
 *
 * <p>The Button privately owns its Image and Text children. Add only the Button
 * to a ZLayer; do not add either child separately.</p>
 */
public final class Button implements Component {

    private final Context context;
    private final View hostView;
    private final String id;
    private final RectF bounds = new RectF();
    private final Path clipPath = new Path();
    private final ComponentHost childHost = new ComponentHost() {
        @Override
        public View getHostView() {
            return hostView;
        }

        @Override
        public void invalidateComponent() {
            invalidate();
        }

        @Override
        public void postInvalidateComponentOnAnimation() {
            invalidate();
        }
    };

    private ComponentHost owner;
    private Image image;
    private Text text;
    private Bitmap ownedBackgroundBitmap;
    private FigmaConfig figmaConfig;
    private TextInsets textInsets;
    private boolean textInsetsInPixels;
    private float dimensionScale;
    private float cornerRadius;
    private float resolvedCornerRadius;
    private boolean cornerRadiusInPixels;
    private float alpha;
    private boolean visible;
    private boolean enabled;
    private boolean released;
    private boolean touchCaptured;
    private boolean clickCancelled;
    private OnClickListener clickListener;

    private Button(Builder builder, View hostView) {
        context = builder.context.getApplicationContext();
        this.hostView = Objects.requireNonNull(
                hostView,
                "Host view cannot be null."
        );
        id = requireId(builder.id);
        textInsets = builder.textInsets;
        textInsetsInPixels = builder.textInsetsInPixels;
        cornerRadius = builder.cornerRadius;
        cornerRadiusInPixels = builder.cornerRadiusInPixels;
        alpha = builder.alpha;
        visible = builder.visible;
        enabled = builder.enabled;
        clickListener = builder.clickListener;

        if (builder.suppliedImage != null) {
            image = builder.suppliedImage;
            bounds.set(image.getBounds());
            figmaConfig = image.getFigmaConfig();
            dimensionScale = image.getDimensionScale();
            RectF insetTextBounds = resolveTextBounds(
                    bounds,
                    dimensionScale,
                    textInsets,
                    textInsetsInPixels
            );
            if (builder.suppliedText != null) {
                requireContainedText(bounds, builder.suppliedText.getBounds());
                text = builder.suppliedText;
                if (!TextInsets.none().equals(textInsets)) {
                    applyTextRegion(text, insetTextBounds, dimensionScale);
                }
            }
        } else {
            resolveOwnRegion(
                    builder.position,
                    builder.size,
                    builder.explicitBounds
            );
            RectF textBounds = resolveTextBounds(
                    bounds,
                    dimensionScale,
                    textInsets,
                    textInsetsInPixels
            );
            Bitmap resolvedBitmap = builder.bitmap;
            if (resolvedBitmap == null) {
                ownedBackgroundBitmap = createColorBitmap(
                        Objects.requireNonNull(
                                builder.backgroundColor,
                                "Button background color cannot be null."
                        )
                );
                resolvedBitmap = ownedBackgroundBitmap;
            }
            image = new Image.Builder(
                    context,
                    childId("image"),
                    resolvedBitmap,
                    bounds
            )
                    .setScaleType(builder.imageScaleType)
                    .setFilterBitmap(builder.filterBitmap)
                    .build(hostView);

            if (builder.label != null) {
                text = newTextBuilder(builder.label, textBounds)
                        .setStyle(builder.textStyleBuilder.build())
                        .setEnabled(false)
                        .build(hostView);
            }
        }

        // The composite host gives child invalidation to this Button and prevents
        // a transferred child from also belonging to a ZLayer.
        image.attach(childHost);
        if (text != null) text.attach(childHost);
        rebuildClipPath();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public RectF getBounds() {
        return new RectF(bounds);
    }

    public Image getImage() {
        ensureActive();
        return image;
    }

    public Text getText() {
        ensureActive();
        return text;
    }

    public boolean hasText() {
        return !released && text != null;
    }

    public TextInsets getTextInsets() {
        return textInsets;
    }

    public boolean areTextInsetsInPixels() {
        return textInsetsInPixels;
    }

    public FigmaConfig getFigmaConfig() {
        return figmaConfig;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public float getResolvedCornerRadius() {
        return resolvedCornerRadius;
    }

    public boolean isCornerRadiusInPixels() {
        return cornerRadiusInPixels;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isClickable() {
        return clickListener != null;
    }

    public Button setBitmap(Bitmap bitmap) {
        ensureActive();
        Objects.requireNonNull(bitmap, "Button bitmap cannot be null.");
        if (bitmap != ownedBackgroundBitmap) {
            recycleOwnedBackground();
        }
        image.setBitmap(bitmap);
        return this;
    }

    /**
     * Replaces the image background with a privately owned solid-color bitmap.
     */
    public Button setBackgroundColor(int color) {
        ensureActive();
        if (ownedBackgroundBitmap == null
                || ownedBackgroundBitmap.isRecycled()) {
            ownedBackgroundBitmap = createColorBitmap(color);
            image.setBitmap(ownedBackgroundBitmap);
        } else {
            ownedBackgroundBitmap.eraseColor(color);
        }
        image.setScaleType(Image.ScaleType.FIT_XY);
        invalidate();
        return this;
    }

    /**
     * Changes the label, creating the privately owned Text child when needed.
     */
    public Button setLabel(CharSequence label) {
        ensureActive();
        Objects.requireNonNull(label, "Button label cannot be null.");
        if (text == null) {
            text = newTextBuilder(label, resolveTextBounds())
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setMaxLines(1)
                    .setOverflow(Text.Overflow.ELLIPSIZE_END)
                    .setWrapEnabled(false)
                    .setEnabled(false)
                    .build(hostView);
            text.attach(childHost);
        } else {
            text.setText(label);
        }
        invalidate();
        return this;
    }

    public Button removeText() {
        ensureActive();
        if (text != null) {
            text.release();
            text = null;
            invalidate();
        }
        return this;
    }

    public Button setRegion(Position position, Size size) {
        ensureActive();
        Objects.requireNonNull(position, "Position cannot be null.");
        Objects.requireNonNull(size, "Size cannot be null.");
        RectF resolved = position.toRectF(hostView, size);
        float newDimensionScale = position.getScale(hostView);
        applyRegion(resolved, newDimensionScale);
        figmaConfig = position.getFigmaConfig();
        return this;
    }

    public Button setRegion(RectF bounds) {
        ensureActive();
        FigmaConfig newConfig = FigmaConfig.getDefault();
        applyRegion(new RectF(Objects.requireNonNull(
                bounds,
                "Bounds cannot be null."
        )), newConfig.getScale(hostView.getWidth()));
        figmaConfig = newConfig;
        return this;
    }

    /**
     * Sets label-region insets in Figma/design-space units.
     */
    public Button setTextInsets(TextInsets textInsets) {
        return setTextInsetsInternal(textInsets, false);
    }

    /**
     * Sets exact label-region insets in runtime pixels.
     */
    public Button setTextInsetsPx(TextInsets textInsets) {
        return setTextInsetsInternal(textInsets, true);
    }

    private Button setTextInsetsInternal(
            TextInsets textInsets,
            boolean inPixels
    ) {
        ensureActive();
        TextInsets newInsets = Objects.requireNonNull(
                textInsets,
                "Text insets cannot be null."
        );
        RectF textBounds = resolveTextBounds(
                bounds,
                dimensionScale,
                newInsets,
                inPixels
        );
        this.textInsets = newInsets;
        textInsetsInPixels = inPixels;
        if (text != null) {
            applyTextRegion(text, textBounds, dimensionScale);
        }
        invalidate();
        return this;
    }

    public Button setImageScaleType(Image.ScaleType scaleType) {
        ensureActive();
        image.setScaleType(scaleType);
        return this;
    }

    public Button setFilterBitmap(boolean filterBitmap) {
        ensureActive();
        image.setFilterBitmap(filterBitmap);
        return this;
    }

    /**
     * Uses Figma/design-space units regardless of the region type.
     */
    public Button setCornerRadius(float cornerRadius) {
        ensureActive();
        this.cornerRadius = requireCornerRadius(cornerRadius);
        cornerRadiusInPixels = false;
        rebuildClipPath();
        invalidate();
        return this;
    }

    /**
     * Uses an exact runtime-pixel radius regardless of the region source.
     */
    public Button setCornerRadiusPx(float cornerRadius) {
        ensureActive();
        this.cornerRadius = requireCornerRadius(cornerRadius);
        cornerRadiusInPixels = true;
        rebuildClipPath();
        invalidate();
        return this;
    }

    public Button setTextSize(float size) {
        requireText().setTextSize(size);
        return this;
    }

    public Button setTextSizePx(float pixels) {
        requireText().setTextSizePx(pixels);
        return this;
    }

    public Button setTextLetterSpacing(float spacing) {
        requireText().setLetterSpacing(spacing);
        return this;
    }

    public Button setTextLetterSpacingPx(float pixels) {
        requireText().setLetterSpacingPx(pixels);
        return this;
    }

    public Button setTextLineSpacing(float spacing) {
        requireText().setLineSpacing(spacing);
        return this;
    }

    public Button setTextLineSpacingPx(float pixels) {
        requireText().setLineSpacingPx(pixels);
        return this;
    }

    public Button setTextPadding(float horizontal, float vertical) {
        requireText().setPadding(horizontal, vertical);
        return this;
    }

    public Button setTextPaddingPx(float horizontal, float vertical) {
        requireText().setPaddingPx(horizontal, vertical);
        return this;
    }

    public Button setTextShadow(
            float radius,
            float dx,
            float dy,
            int color
    ) {
        requireText().setShadow(radius, dx, dy, color);
        return this;
    }

    public Button setTextShadowPx(
            float radius,
            float dx,
            float dy,
            int color
    ) {
        requireText().setShadowPx(radius, dx, dy, color);
        return this;
    }

    public Button clearTextShadow() {
        requireText().clearShadow();
        return this;
    }

    public Button setTextColor(int color) {
        requireText().setTextColor(color);
        return this;
    }

    public Button setTextAlpha(float alpha) {
        requireText().setAlpha(alpha);
        return this;
    }

    public Button setTextAlignment(Text.Alignment alignment) {
        requireText().setAlignment(alignment);
        return this;
    }

    public Button setTextVerticalAlignment(Text.VerticalAlignment alignment) {
        requireText().setVerticalAlignment(alignment);
        return this;
    }

    public Button setFont(int fontResourceId) {
        requireText().setFont(fontResourceId);
        return this;
    }

    public Button setFontAsset(String assetPath) {
        requireText().setFontAsset(assetPath);
        return this;
    }

    public Button setFont(Typeface typeface) {
        requireText().setFont(typeface);
        return this;
    }

    public Button useDefaultFont() {
        requireText().useDefaultFont();
        return this;
    }

    public Button setFontVariations(FontVariation variation) {
        requireText().setFontVariations(variation);
        return this;
    }

    public Button clearFontVariations() {
        requireText().clearFontVariations();
        return this;
    }

    public Button setAlpha(float alpha) {
        ensureActive();
        this.alpha = requireAlpha(alpha);
        invalidate();
        return this;
    }

    public Button setVisible(boolean visible) {
        ensureActive();
        this.visible = visible;
        if (!visible) cancelTouch();
        invalidate();
        return this;
    }

    public Button setEnabled(boolean enabled) {
        ensureActive();
        this.enabled = enabled;
        if (!enabled) cancelTouch();
        invalidate();
        return this;
    }

    public Button setOnClickListener(OnClickListener listener) {
        ensureActive();
        clickListener = listener;
        if (listener == null) cancelTouch();
        return this;
    }

    public Button removeOnClickListener() {
        return setOnClickListener(null);
    }

    @Override
    public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || alpha <= 0f) return;

        int saveCount = alpha < 1f
                ? canvas.saveLayerAlpha(bounds, Math.round(alpha * 255f))
                : canvas.save();
        if (resolvedCornerRadius > 0f) {
            canvas.clipPath(clipPath);
        } else {
            canvas.clipRect(bounds);
        }
        image.draw(canvas);
        if (text != null) text.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchCaptured = acceptsTouch(event.getX(), event.getY());
                clickCancelled = false;
                return touchCaptured;
            case MotionEvent.ACTION_MOVE:
                if (!touchCaptured) return false;
                if (!acceptsTouch(event.getX(), event.getY())) {
                    clickCancelled = true;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!touchCaptured) return false;
                boolean click = !clickCancelled
                        && acceptsTouch(event.getX(), event.getY());
                touchCaptured = false;
                if (click) clickListener.onClick(id);
                return true;
            case MotionEvent.ACTION_CANCEL:
                boolean handled = touchCaptured;
                cancelTouch();
                return handled;
            default:
                return touchCaptured;
        }
    }

    @Override
    public void attach(ComponentHost owner) {
        ensureActive();
        Objects.requireNonNull(owner, "Component host cannot be null.");
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException(
                    "Button already belongs to another component host."
            );
        }
        this.owner = owner;
    }

    @Override
    public void release() {
        if (released) return;
        released = true;
        cancelTouch();
        clickListener = null;
        owner = null;
        if (text != null) {
            text.release();
            text = null;
        }
        if (image != null) {
            image.release();
            image = null;
        }
        recycleOwnedBackground();
    }

    private void resolveOwnRegion(
            Position position,
            Size size,
            RectF explicitBounds
    ) {
        if (explicitBounds != null) {
            requireBounds(explicitBounds);
            bounds.set(explicitBounds);
            figmaConfig = FigmaConfig.getDefault();
            dimensionScale = figmaConfig.getScale(hostView.getWidth());
            return;
        }
        Objects.requireNonNull(position, "Position cannot be null.");
        Objects.requireNonNull(size, "Size cannot be null.");
        RectF resolved = position.toRectF(hostView, size);
        requireBounds(resolved);
        bounds.set(resolved);
        figmaConfig = position.getFigmaConfig();
        dimensionScale = position.getScale(hostView);
    }

    private void applyRegion(RectF newBounds, float newDimensionScale) {
        requireBounds(newBounds);
        RectF textBounds = resolveTextBounds(
                newBounds,
                newDimensionScale,
                textInsets,
                textInsetsInPixels
        );
        bounds.set(newBounds);
        dimensionScale = newDimensionScale;
        rebuildClipPath();
        image.setRegion(bounds);
        if (text != null) {
            applyTextRegion(text, textBounds, newDimensionScale);
        }
        invalidate();
    }

    private Text.Builder newTextBuilder(
            CharSequence label,
            RectF textBounds
    ) {
        Position textPosition = designPosition(textBounds, dimensionScale);
        Size textSize = designSize(textBounds, dimensionScale);
        return new Text.Builder(
                context,
                childId("text"),
                label,
                textPosition,
                textSize
        );
    }

    private void applyTextRegion(
            Text target,
            RectF textBounds,
            float scale
    ) {
        target.setRegion(
                designPosition(textBounds, scale),
                designSize(textBounds, scale)
        );
    }

    private Position designPosition(RectF runtimeBounds, float scale) {
        return new Position(
                hostView,
                new FigmaConfig(hostView.getWidth() / scale),
                Position.HorizontalMarginFrom.LEFT,
                Position.VerticalMarginFrom.TOP,
                runtimeBounds.left / scale,
                runtimeBounds.top / scale
        );
    }

    private static Size designSize(RectF runtimeBounds, float scale) {
        return new Size(
                runtimeBounds.width() / scale,
                runtimeBounds.height() / scale
        );
    }

    private RectF resolveTextBounds() {
        return resolveTextBounds(
                bounds,
                dimensionScale,
                textInsets,
                textInsetsInPixels
        );
    }

    private RectF resolveTextBounds(
            RectF buttonBounds,
            float scale,
            TextInsets insets,
            boolean inPixels
    ) {
        float insetScale = inPixels ? 1f : scale;
        float left = buttonBounds.left + insets.getLeft() * insetScale;
        float top = buttonBounds.top + insets.getTop() * insetScale;
        float right = buttonBounds.right - insets.getRight() * insetScale;
        float bottom = buttonBounds.bottom - insets.getBottom() * insetScale;
        RectF result = new RectF(left, top, right, bottom);
        if (result.width() <= 0f || result.height() <= 0f) {
            throw new IllegalArgumentException(
                    "Button \"" + id
                            + "\" text insets leave no drawable text region. "
                            + "Button size: " + buttonBounds.width() + " x "
                            + buttonBounds.height() + "; insets: " + insets
            );
        }
        return result;
    }

    private void rebuildClipPath() {
        float scaledRadius = cornerRadiusInPixels
                ? cornerRadius
                : cornerRadius * dimensionScale;
        resolvedCornerRadius = Math.min(
                scaledRadius,
                Math.min(bounds.width(), bounds.height()) / 2f
        );
        clipPath.reset();
        if (resolvedCornerRadius > 0f) {
            clipPath.addRoundRect(
                    bounds,
                    resolvedCornerRadius,
                    resolvedCornerRadius,
                    Path.Direction.CW
            );
            clipPath.close();
        }
    }

    private Text requireText() {
        ensureActive();
        if (text == null) {
            throw new IllegalStateException(
                    "Button has no Text component: " + id
                            + ". Call setLabel() before configuring text."
            );
        }
        return text;
    }

    private boolean acceptsTouch(float x, float y) {
        return !released
                && visible
                && enabled
                && clickListener != null
                && bounds.contains(x, y);
    }

    private void cancelTouch() {
        touchCaptured = false;
        clickCancelled = true;
    }

    private void invalidate() {
        if (owner != null) owner.postInvalidateComponentOnAnimation();
    }

    private void ensureActive() {
        if (released) {
            throw new IllegalStateException("Button has been released: " + id);
        }
    }

    private String childId(String suffix) {
        return id + ':' + suffix;
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Button ID cannot be null or blank."
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
                    "Button bounds must have positive finite dimensions."
            );
        }
    }

    private static void requireContainedText(
            RectF imageBounds,
            RectF textBounds
    ) {
        if (textBounds.left < imageBounds.left
                || textBounds.top < imageBounds.top
                || textBounds.right > imageBounds.right
                || textBounds.bottom > imageBounds.bottom) {
            throw new IllegalArgumentException(
                    "Button Text bounds must be fully inside Image bounds. "
                            + "Image: " + imageBounds + "; Text: " + textBounds
            );
        }
    }

    private static float requireAlpha(float alpha) {
        if (!Float.isFinite(alpha) || alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException(
                    "Button alpha must be finite and in the 0..1 range."
            );
        }
        return alpha;
    }

    private static Bitmap createColorBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
        );
        bitmap.eraseColor(color);
        return bitmap;
    }

    private void recycleOwnedBackground() {
        if (ownedBackgroundBitmap != null) {
            if (!ownedBackgroundBitmap.isRecycled()) {
                ownedBackgroundBitmap.recycle();
            }
            ownedBackgroundBitmap = null;
        }
    }

    private static float requireCornerRadius(float cornerRadius) {
        if (!Float.isFinite(cornerRadius) || cornerRadius < 0f) {
            throw new IllegalArgumentException(
                    "Button corner radius must be non-negative and finite."
            );
        }
        return cornerRadius;
    }

    public interface OnClickListener {
        void onClick(String id);
    }

    public static final class Builder implements ComponentFactory<Button> {

        private final Context context;
        private final String id;
        private final Image suppliedImage;
        private final Text suppliedText;
        private final Bitmap bitmap;
        private final Integer backgroundColor;
        private final CharSequence label;
        private final Position position;
        private final Size size;
        private final RectF explicitBounds;
        private TextStyle.Builder textStyleBuilder =
                new TextStyle.Builder()
                        .setAlignment(Text.Alignment.CENTER)
                        .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                        .setMaxLines(1)
                        .setOverflow(Text.Overflow.ELLIPSIZE_END)
                        .setWrapEnabled(false);

        private TextInsets textInsets = TextInsets.none();
        private boolean textInsetsInPixels;
        private Image.ScaleType imageScaleType = Image.ScaleType.FIT_XY;
        private boolean filterBitmap = true;
        private float cornerRadius;
        private boolean cornerRadiusInPixels;
        private float alpha = 1f;
        private boolean visible = true;
        private boolean enabled = true;
        private OnClickListener clickListener;

        public Builder(Context context, String id, Image image) {
            this(context, id, image, null);
        }

        public Builder(
                Context context,
                String id,
                Image image,
                Text text
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            suppliedImage = Objects.requireNonNull(
                    image,
                    "Button Image cannot be null."
            );
            suppliedText = text;
            bitmap = null;
            backgroundColor = null;
            label = null;
            position = null;
            size = null;
            explicitBounds = null;
        }

        public Builder(
                Context context,
                String id,
                Bitmap bitmap,
                Position position,
                Size size
        ) {
            this(context, id, bitmap, null, position, size);
        }

        public Builder(
                Context context,
                String id,
                Bitmap bitmap,
                CharSequence text,
                Position position,
                Size size
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.bitmap = Objects.requireNonNull(
                    bitmap,
                    "Button bitmap cannot be null."
            );
            backgroundColor = null;
            label = text;
            this.position = Objects.requireNonNull(
                    position,
                    "Position cannot be null."
            );
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
            suppliedImage = null;
            suppliedText = null;
            explicitBounds = null;
        }

        public Builder(
                Context context,
                String id,
                Bitmap bitmap,
                RectF bounds
        ) {
            this(context, id, bitmap, null, bounds);
        }

        public Builder(
                Context context,
                String id,
                Bitmap bitmap,
                CharSequence text,
                RectF bounds
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.bitmap = Objects.requireNonNull(
                    bitmap,
                    "Button bitmap cannot be null."
            );
            backgroundColor = null;
            label = text;
            explicitBounds = new RectF(Objects.requireNonNull(
                    bounds,
                    "Bounds cannot be null."
            ));
            suppliedImage = null;
            suppliedText = null;
            position = null;
            size = null;
        }

        public Builder(
                Context context,
                String id,
                int backgroundColor,
                Position position,
                Size size
        ) {
            this(context, id, backgroundColor, null, position, size);
        }

        public Builder(
                Context context,
                String id,
                int backgroundColor,
                CharSequence text,
                Position position,
                Size size
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.backgroundColor = backgroundColor;
            label = text;
            this.position = Objects.requireNonNull(
                    position,
                    "Position cannot be null."
            );
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
            suppliedImage = null;
            suppliedText = null;
            bitmap = null;
            explicitBounds = null;
        }

        public Builder(
                Context context,
                String id,
                int backgroundColor,
                RectF bounds
        ) {
            this(context, id, backgroundColor, null, bounds);
        }

        public Builder(
                Context context,
                String id,
                int backgroundColor,
                CharSequence text,
                RectF bounds
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.backgroundColor = backgroundColor;
            label = text;
            explicitBounds = new RectF(Objects.requireNonNull(
                    bounds,
                    "Bounds cannot be null."
            ));
            suppliedImage = null;
            suppliedText = null;
            bitmap = null;
            position = null;
            size = null;
        }

        public Builder setTextInsets(TextInsets textInsets) {
            this.textInsets = Objects.requireNonNull(
                    textInsets,
                    "Text insets cannot be null."
            );
            textInsetsInPixels = false;
            return this;
        }

        public Builder setTextInsetsPx(TextInsets textInsets) {
            this.textInsets = Objects.requireNonNull(
                    textInsets,
                    "Text insets cannot be null."
            );
            textInsetsInPixels = true;
            return this;
        }

        public Builder setImageScaleType(Image.ScaleType scaleType) {
            requireInternalCreation();
            imageScaleType = Objects.requireNonNull(
                    scaleType,
                    "Image scale type cannot be null."
            );
            return this;
        }

        public Builder setFilterBitmap(boolean filterBitmap) {
            requireInternalCreation();
            this.filterBitmap = filterBitmap;
            return this;
        }

        public Builder setCornerRadius(float cornerRadius) {
            this.cornerRadius = requireCornerRadius(cornerRadius);
            cornerRadiusInPixels = false;
            return this;
        }

        public Builder setCornerRadiusPx(float cornerRadius) {
            this.cornerRadius = requireCornerRadius(cornerRadius);
            cornerRadiusInPixels = true;
            return this;
        }

        public Builder setTextStyle(TextStyle style) {
            requireInternalText();
            textStyleBuilder = new TextStyle.Builder(Objects.requireNonNull(
                    style,
                    "TextStyle cannot be null."
            ));
            return this;
        }

        public Builder setTextSize(float size) {
            requireInternalText();
            textStyleBuilder.setTextSize(size);
            return this;
        }

        public Builder setTextSizePx(float pixels) {
            requireInternalText();
            textStyleBuilder.setTextSizePx(pixels);
            return this;
        }

        public Builder setTextLetterSpacing(float spacing) {
            requireInternalText();
            textStyleBuilder.setLetterSpacing(spacing);
            return this;
        }

        public Builder setTextLetterSpacingPx(float pixels) {
            requireInternalText();
            textStyleBuilder.setLetterSpacingPx(pixels);
            return this;
        }

        public Builder setTextLineSpacing(float spacing) {
            requireInternalText();
            textStyleBuilder.setLineSpacing(spacing);
            return this;
        }

        public Builder setTextLineSpacingPx(float pixels) {
            requireInternalText();
            textStyleBuilder.setLineSpacingPx(pixels);
            return this;
        }

        public Builder setTextPadding(float horizontal, float vertical) {
            requireInternalText();
            textStyleBuilder.setPadding(horizontal, vertical);
            return this;
        }

        public Builder setTextPaddingPx(float horizontal, float vertical) {
            requireInternalText();
            textStyleBuilder.setPaddingPx(horizontal, vertical);
            return this;
        }

        public Builder setTextShadow(
                float radius,
                float dx,
                float dy,
                int color
        ) {
            requireInternalText();
            textStyleBuilder.setShadow(radius, dx, dy, color);
            return this;
        }

        public Builder setTextShadowPx(
                float radius,
                float dx,
                float dy,
                int color
        ) {
            requireInternalText();
            textStyleBuilder.setShadowPx(radius, dx, dy, color);
            return this;
        }

        public Builder clearTextShadow() {
            requireInternalText();
            textStyleBuilder.clearShadow();
            return this;
        }

        public Builder setTextColor(int color) {
            requireInternalText();
            textStyleBuilder.setTextColor(color);
            return this;
        }

        public Builder setTextAlpha(float alpha) {
            requireInternalText();
            textStyleBuilder.setAlpha(alpha);
            return this;
        }

        public Builder setTextAlignment(Text.Alignment alignment) {
            requireInternalText();
            textStyleBuilder.setAlignment(alignment);
            return this;
        }

        public Builder setTextVerticalAlignment(
                Text.VerticalAlignment alignment
        ) {
            requireInternalText();
            textStyleBuilder.setVerticalAlignment(alignment);
            return this;
        }

        public Builder useDefaultFont() {
            requireInternalText();
            textStyleBuilder.useDefaultFont();
            return this;
        }

        public Builder setFont(int fontResourceId) {
            requireInternalText();
            textStyleBuilder.setFont(fontResourceId);
            return this;
        }

        public Builder setFontAsset(String assetPath) {
            requireInternalText();
            textStyleBuilder.setFontAsset(assetPath);
            return this;
        }

        public Builder setFont(Typeface typeface) {
            requireInternalText();
            textStyleBuilder.setFont(typeface);
            return this;
        }

        public Builder setFontVariations(FontVariation variation) {
            requireInternalText();
            textStyleBuilder.setFontVariations(variation);
            return this;
        }

        public Builder setAlpha(float alpha) {
            this.alpha = requireAlpha(alpha);
            return this;
        }

        public Builder setVisible(boolean visible) {
            this.visible = visible;
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
        public Button build(View hostView) {
            return new Button(this, hostView);
        }

        private void requireInternalCreation() {
            if (suppliedImage != null) {
                throw new IllegalStateException(
                        "Configure a supplied Image directly before creating "
                                + "the Button."
                );
            }
        }

        private void requireInternalText() {
            requireInternalCreation();
            if (label == null) {
                throw new IllegalStateException(
                        "This Button builder has no text label."
                );
            }
        }
    }
}

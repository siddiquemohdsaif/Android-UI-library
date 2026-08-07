package com.ogfa.nativeviews.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;

import java.util.Objects;

/**
 * A standalone bitmap component rendered directly on a Canvas.
 */
public final class Image implements Component {

    public enum ScaleType {
        FIT_CENTER,
        CENTER_CROP,
        FIT_XY
    }

    private final String id;
    private final View hostView;
    private final RectF bounds = new RectF();
    private final RectF drawBounds = new RectF();
    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG
                    | Paint.DITHER_FLAG
                    | Paint.FILTER_BITMAP_FLAG
    );

    private ComponentHost owner;
    private FigmaConfig figmaConfig;
    private float dimensionScale;
    private Bitmap bitmap;
    private ScaleType scaleType;
    private float alpha;
    private boolean visible;
    private boolean enabled;
    private boolean released;
    private boolean touchCaptured;
    private boolean clickCancelled;
    private OnClickListener clickListener;

    private Image(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(
                hostView,
                "Host view cannot be null."
        );
        id = requireId(builder.id);
        bitmap = requireBitmap(builder.bitmap);
        scaleType = builder.scaleType;
        alpha = builder.alpha;
        visible = builder.visible;
        enabled = builder.enabled;
        clickListener = builder.clickListener;
        paint.setFilterBitmap(builder.filterBitmap);
        paint.setAlpha(Math.round(alpha * 255f));
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        resolveDrawBounds();
    }

    @Override
    public String getId() {
        return id;
    }

    public Bitmap getBitmap() {
        ensureActive();
        return bitmap;
    }

    @Override
    public RectF getBounds() {
        return new RectF(bounds);
    }

    public ScaleType getScaleType() {
        return scaleType;
    }

    public FigmaConfig getFigmaConfig() {
        return figmaConfig;
    }

    public float getDimensionScale() {
        return dimensionScale;
    }

    public float getAlpha() {
        return alpha;
    }

    public boolean isFilterBitmap() {
        return paint.isFilterBitmap();
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

    public Image setBitmap(Bitmap bitmap) {
        ensureActive();
        this.bitmap = requireBitmap(bitmap);
        resolveDrawBounds();
        invalidate();
        return this;
    }

    public Image setRegion(Position position, Size size) {
        ensureActive();
        resolveRegion(
                Objects.requireNonNull(position, "Position cannot be null."),
                Objects.requireNonNull(size, "Size cannot be null."),
                null
        );
        resolveDrawBounds();
        invalidate();
        return this;
    }

    public Image setRegion(RectF bounds) {
        ensureActive();
        resolveRegion(
                null,
                null,
                new RectF(Objects.requireNonNull(
                        bounds,
                        "Bounds cannot be null."
                ))
        );
        resolveDrawBounds();
        invalidate();
        return this;
    }

    public Image setScaleType(ScaleType scaleType) {
        ensureActive();
        this.scaleType = Objects.requireNonNull(
                scaleType,
                "Scale type cannot be null."
        );
        resolveDrawBounds();
        invalidate();
        return this;
    }

    public Image setAlpha(float alpha) {
        ensureActive();
        this.alpha = requireAlpha(alpha);
        paint.setAlpha(Math.round(alpha * 255f));
        invalidate();
        return this;
    }

    public Image setFilterBitmap(boolean filterBitmap) {
        ensureActive();
        paint.setFilterBitmap(filterBitmap);
        invalidate();
        return this;
    }

    public Image setVisible(boolean visible) {
        ensureActive();
        this.visible = visible;
        if (!visible) cancelTouch();
        invalidate();
        return this;
    }

    public Image setEnabled(boolean enabled) {
        ensureActive();
        this.enabled = enabled;
        if (!enabled) cancelTouch();
        return this;
    }

    public Image setOnClickListener(OnClickListener listener) {
        ensureActive();
        clickListener = listener;
        if (listener == null) cancelTouch();
        return this;
    }

    public Image removeOnClickListener() {
        return setOnClickListener(null);
    }

    @Override
    public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released) return;
        requireBitmap(bitmap);

        int saveCount = canvas.save();
        canvas.clipRect(bounds);
        canvas.drawBitmap(bitmap, null, drawBounds, paint);
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
                if (click) performClick();
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
                    "Image already belongs to another component host."
            );
        }
        this.owner = owner;
    }

    @Override
    public void release() {
        if (released) return;
        released = true;
        cancelTouch();
        owner = null;
        clickListener = null;
        bitmap = null;
    }

    private void resolveRegion(
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

    private void resolveDrawBounds() {
        requireBitmap(bitmap);
        if (scaleType == ScaleType.FIT_XY) {
            drawBounds.set(bounds);
            return;
        }

        float scaleX = bounds.width() / bitmap.getWidth();
        float scaleY = bounds.height() / bitmap.getHeight();
        float scale = scaleType == ScaleType.CENTER_CROP
                ? Math.max(scaleX, scaleY)
                : Math.min(scaleX, scaleY);
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        float left = bounds.centerX() - width / 2f;
        float top = bounds.centerY() - height / 2f;
        drawBounds.set(left, top, left + width, top + height);
    }

    private boolean acceptsTouch(float x, float y) {
        return !released
                && visible
                && enabled
                && clickListener != null
                && bounds.contains(x, y);
    }

    private void performClick() {
        if (acceptsTouch(bounds.centerX(), bounds.centerY())) {
            clickListener.onClick(id);
        }
    }

    private void cancelTouch() {
        touchCaptured = false;
        clickCancelled = true;
    }

    private void invalidate() {
        if (owner != null) {
            owner.postInvalidateComponentOnAnimation();
        }
    }

    private void ensureActive() {
        if (released) {
            throw new IllegalStateException("Image has been released: " + id);
        }
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Image ID cannot be null or blank."
            );
        }
        return id.trim();
    }

    private static Bitmap requireBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException("Image bitmap cannot be null.");
        }
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException(
                    "Image bitmap has already been recycled."
            );
        }
        if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            throw new IllegalArgumentException(
                    "Image bitmap must have positive dimensions."
            );
        }
        return bitmap;
    }

    private static void requireBounds(RectF bounds) {
        if (!Float.isFinite(bounds.left)
                || !Float.isFinite(bounds.top)
                || !Float.isFinite(bounds.right)
                || !Float.isFinite(bounds.bottom)
                || bounds.width() <= 0f
                || bounds.height() <= 0f) {
            throw new IllegalArgumentException(
                    "Image bounds must have positive finite dimensions."
            );
        }
    }

    private static float requireAlpha(float alpha) {
        if (!Float.isFinite(alpha) || alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException(
                    "Image alpha must be finite and in the 0..1 range."
            );
        }
        return alpha;
    }

    public interface OnClickListener {
        void onClick(String id);
    }

    public static final class Builder implements ComponentFactory<Image> {

        private final String id;
        private final Bitmap bitmap;
        private final Position position;
        private final Size size;
        private final RectF explicitBounds;

        private ScaleType scaleType = ScaleType.FIT_CENTER;
        private float alpha = 1f;
        private boolean filterBitmap = true;
        private boolean visible = true;
        private boolean enabled = true;
        private OnClickListener clickListener;

        public Builder(
                Context context,
                String id,
                Bitmap bitmap,
                Position position,
                Size size
        ) {
            Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.bitmap = requireBitmap(bitmap);
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
                Bitmap bitmap,
                RectF bounds
        ) {
            Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.bitmap = requireBitmap(bitmap);
            explicitBounds = new RectF(Objects.requireNonNull(
                    bounds,
                    "Bounds cannot be null."
            ));
            position = null;
            size = null;
        }

        public Builder setScaleType(ScaleType scaleType) {
            this.scaleType = Objects.requireNonNull(
                    scaleType,
                    "Scale type cannot be null."
            );
            return this;
        }

        public Builder setAlpha(float alpha) {
            this.alpha = requireAlpha(alpha);
            return this;
        }

        public Builder setFilterBitmap(boolean filterBitmap) {
            this.filterBitmap = filterBitmap;
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
        public Image build(View hostView) {
            return new Image(this, hostView);
        }
    }
}

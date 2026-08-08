package com.ogfa.nativeviews.checkbox.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ogfa.nativeviews.checkbox.CheckBox;
import com.ogfa.nativeviews.checkbox.CheckBoxImages;
import com.ogfa.nativeviews.image.Image;

/** Cross-fade or snap renderer for complete-state CheckBox images. */
public final class ImageCheckBoxRenderer implements CheckBoxRenderer {
    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private CheckBoxImages images;
    private Image.ScaleType scaleType;
    private CheckBox.ImageTransition transition;

    public ImageCheckBoxRenderer(
            CheckBoxImages images,
            Image.ScaleType scaleType,
            CheckBox.ImageTransition transition,
            boolean filtering
    ) {
        this.images = images;
        this.scaleType = scaleType;
        this.transition = transition;
        paint.setFilterBitmap(filtering);
    }

    public void setImages(CheckBoxImages value) { images = value; }
    public void setScaleType(Image.ScaleType value) { scaleType = value; }
    public void setTransition(CheckBox.ImageTransition value) { transition = value; }

    @Override public void draw(Canvas canvas, CheckBoxRenderState state) {
        images.validateActive();
        Bitmap target = images.get(state.state, state.enabled);
        float progress = Math.max(0f, Math.min(1f, state.transitionProgress));
        if (transition == CheckBox.ImageTransition.SNAP
                || state.fromState == state.state || progress >= 1f) {
            drawBitmap(canvas, target, state.bounds, 255);
            return;
        }
        Bitmap source = images.get(state.fromState, state.enabled);
        drawBitmap(canvas, source, state.bounds, Math.round(255f * (1f - progress)));
        drawBitmap(canvas, target, state.bounds, Math.round(255f * progress));
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, RectF bounds, int alpha) {
        if (bitmap.isRecycled()) {
            throw new IllegalStateException("CheckBox image bitmap was recycled by its owner.");
        }
        if (alpha <= 0) return;
        paint.setAlpha(alpha);
        if (scaleType == Image.ScaleType.FIT_XY) {
            canvas.drawBitmap(bitmap, null, bounds, paint);
            return;
        }
        float sx = bounds.width() / bitmap.getWidth();
        float sy = bounds.height() / bitmap.getHeight();
        float scale = scaleType == Image.ScaleType.CENTER_CROP
                ? Math.max(sx, sy) : Math.min(sx, sy);
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        RectF destination = new RectF(
                bounds.centerX() - width / 2f, bounds.centerY() - height / 2f,
                bounds.centerX() + width / 2f, bounds.centerY() + height / 2f);
        int save = canvas.save();
        canvas.clipRect(bounds);
        canvas.drawBitmap(bitmap, null, destination, paint);
        canvas.restoreToCount(save);
    }

    @Override public void setImageFiltering(boolean enabled) { paint.setFilterBitmap(enabled); }
    @Override public void release() { }
}

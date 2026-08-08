package com.ogfa.nativeviews.radiobutton.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.radiobutton.RadioButton;
import com.ogfa.nativeviews.radiobutton.RadioButtonImages;

/** Complete-state image renderer with cross-fade or snap selection changes. */
public final class ImageRadioRenderer implements RadioRenderer {
    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private RadioButtonImages images;
    private Image.ScaleType scaleType;
    private RadioButton.ImageTransition transition;

    public ImageRadioRenderer(
            RadioButtonImages images,
            Image.ScaleType scaleType,
            RadioButton.ImageTransition transition,
            boolean filtering
    ) {
        this.images = images;
        this.scaleType = scaleType;
        this.transition = transition;
        paint.setFilterBitmap(filtering);
    }

    public void setScaleType(Image.ScaleType value) { scaleType = value; }
    public void setTransition(RadioButton.ImageTransition value) { transition = value; }

    @Override public void draw(Canvas canvas, RadioRenderState state) {
        images.validateActive();
        float progress = Math.max(0f, Math.min(1f, state.progress));
        if (transition == RadioButton.ImageTransition.SNAP) {
            drawBitmap(canvas, images.get(progress >= 0.5f, state.enabled), state.bounds, 255);
            return;
        }
        drawBitmap(canvas, images.get(false, state.enabled), state.bounds,
                Math.round(255f * (1f - progress)));
        drawBitmap(canvas, images.get(true, state.enabled), state.bounds,
                Math.round(255f * progress));
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, RectF bounds, int alpha) {
        if (bitmap.isRecycled()) {
            throw new IllegalStateException("RadioButton image was recycled by its owner.");
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

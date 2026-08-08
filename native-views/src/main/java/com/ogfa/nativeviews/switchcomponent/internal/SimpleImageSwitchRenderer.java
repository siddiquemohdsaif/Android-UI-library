package com.ogfa.nativeviews.switchcomponent.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.switchcomponent.Switch;
import com.ogfa.nativeviews.switchcomponent.SwitchImages;

public final class SimpleImageSwitchRenderer implements SwitchRenderer {
    private final SwitchImages images;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Image.ScaleType scaleType;
    private Switch.ImageTransition transition;

    public SimpleImageSwitchRenderer(
            SwitchImages images,
            Image.ScaleType scaleType,
            Switch.ImageTransition transition,
            boolean filterBitmap
    ) {
        this.images = images;
        this.scaleType = scaleType;
        this.transition = transition;
        paint.setFilterBitmap(filterBitmap);
    }

    public void setScaleType(Image.ScaleType value) { scaleType = value; }
    public void setTransition(Switch.ImageTransition value) { transition = value; }

    @Override public void drawTrack(Canvas canvas, SwitchRenderState state) {
        images.validateActive();
        if (!state.enabled) {
            draw(canvas, images.getSwitchDisabled(), state, 255);
        } else if (transition == Switch.ImageTransition.SNAP) {
            draw(canvas, state.progress >= 0.5f ? images.getSwitchOn() : images.getSwitchOff(), state, 255);
        } else {
            draw(canvas, images.getSwitchOff(), state, Math.round(255f * (1f - state.progress)));
            draw(canvas, images.getSwitchOn(), state, Math.round(255f * state.progress));
        }
    }

    private void draw(Canvas canvas, Bitmap bitmap, SwitchRenderState state, int alpha) {
        if (alpha <= 0) return;
        paint.setAlpha(alpha);
        BitmapDrawHelper.draw(canvas, bitmap, state.trackBounds, scaleType, paint);
    }

    @Override public void drawThumb(Canvas canvas, SwitchRenderState state) { }
    @Override public boolean supportsDrag() { return false; }
    @Override public boolean usesSeparateThumb() { return false; }
    @Override public void setFilterBitmap(boolean enabled) { paint.setFilterBitmap(enabled); }
    @Override public void release() { }
}

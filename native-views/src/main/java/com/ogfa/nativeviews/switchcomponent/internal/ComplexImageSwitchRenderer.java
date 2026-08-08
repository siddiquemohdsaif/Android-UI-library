package com.ogfa.nativeviews.switchcomponent.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.switchcomponent.SwitchImages;

public final class ComplexImageSwitchRenderer implements SwitchRenderer {
    private final SwitchImages images;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Image.ScaleType trackScaleType;
    private Image.ScaleType thumbScaleType;

    public ComplexImageSwitchRenderer(
            SwitchImages images,
            Image.ScaleType trackScaleType,
            Image.ScaleType thumbScaleType,
            boolean filterBitmap
    ) {
        this.images = images;
        this.trackScaleType = trackScaleType;
        this.thumbScaleType = thumbScaleType;
        paint.setFilterBitmap(filterBitmap);
    }

    public void setTrackScaleType(Image.ScaleType value) { trackScaleType = value; }
    public void setThumbScaleType(Image.ScaleType value) { thumbScaleType = value; }

    @Override public void drawTrack(Canvas canvas, SwitchRenderState state) {
        images.validateActive();
        if (!state.enabled) {
            draw(canvas, images.getTrackDisabled(), state.trackBounds, trackScaleType, 255);
            return;
        }
        float progress = Math.max(0f, Math.min(1f, state.progress));
        if (progress <= 0f) {
            draw(canvas, images.getTrackOff(), state.trackBounds, trackScaleType, 255);
            return;
        }
        if (progress >= 1f) {
            draw(canvas, images.getTrackOn(), state.trackBounds, trackScaleType, 255);
            return;
        }

        // During movement, split exactly through the thumb center. Using track-width
        // progress here exposes a gap between the texture boundary and the thumb near
        // either endpoint because the thumb itself has a non-zero radius.
        draw(canvas, images.getTrackOff(), state.trackBounds, trackScaleType, 255);
        float boundary = Math.max(
                state.trackBounds.left,
                Math.min(state.trackBounds.right, state.thumbBounds.centerX())
        );
        int save = canvas.save();
        canvas.clipRect(
                state.trackBounds.left,
                state.trackBounds.top,
                boundary,
                state.trackBounds.bottom
        );
        draw(canvas, images.getTrackOn(), state.trackBounds, trackScaleType, 255);
        canvas.restoreToCount(save);
    }

    @Override public void drawThumb(Canvas canvas, SwitchRenderState state) {
        Bitmap bitmap = state.enabled ? images.getThumbEnabled() : images.getThumbDisabled();
        draw(canvas, bitmap, state.thumbBounds, thumbScaleType, 255);
    }

    private void draw(Canvas canvas, Bitmap bitmap, RectF bounds,
                      Image.ScaleType scaleType, int alpha) {
        if (alpha <= 0) return;
        paint.setAlpha(alpha);
        BitmapDrawHelper.draw(canvas, bitmap, bounds, scaleType, paint);
    }

    @Override public boolean supportsDrag() { return true; }
    @Override public boolean usesSeparateThumb() { return true; }
    @Override public void setFilterBitmap(boolean enabled) { paint.setFilterBitmap(enabled); }
    @Override public void release() { }
}

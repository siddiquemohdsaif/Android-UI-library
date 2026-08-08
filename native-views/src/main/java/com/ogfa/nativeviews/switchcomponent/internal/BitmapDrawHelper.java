package com.ogfa.nativeviews.switchcomponent.internal;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ogfa.nativeviews.image.Image;

final class BitmapDrawHelper {
    private BitmapDrawHelper() { }

    static void draw(Canvas canvas, Bitmap bitmap, RectF bounds, Image.ScaleType type, Paint paint) {
        if (bitmap.isRecycled()) throw new IllegalStateException("Switch image bitmap was recycled by its owner.");
        if (type == Image.ScaleType.FIT_XY) {
            canvas.drawBitmap(bitmap, null, bounds, paint);
            return;
        }
        float sx = bounds.width() / bitmap.getWidth();
        float sy = bounds.height() / bitmap.getHeight();
        float scale = type == Image.ScaleType.CENTER_CROP ? Math.max(sx, sy) : Math.min(sx, sy);
        float width = bitmap.getWidth() * scale;
        float height = bitmap.getHeight() * scale;
        RectF destination = new RectF(
                bounds.centerX() - width / 2f,
                bounds.centerY() - height / 2f,
                bounds.centerX() + width / 2f,
                bounds.centerY() + height / 2f
        );
        int save = canvas.save();
        canvas.clipRect(bounds);
        canvas.drawBitmap(bitmap, null, destination, paint);
        canvas.restoreToCount(save);
    }
}

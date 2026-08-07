package com.ogfa.nativeviews.animator.component.layer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import com.ogfa.nativeviews.animator.component.LayerRegion;
import java.util.Objects;

public final class BitmapLayer extends BaseComponentLayer {
    private Bitmap bitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private BitmapLayer(String id, Bitmap bitmap, LayerRegion region) {
        super(id, region);
        this.bitmap = requireBitmap(bitmap);
    }

    public static BitmapLayer create(String id, Bitmap bitmap, LayerRegion region) {
        return new BitmapLayer(id, bitmap, region);
    }

    public Bitmap getBitmap() { return bitmap; }
    public BitmapLayer setBitmap(Bitmap bitmap) { this.bitmap = requireBitmap(bitmap); return this; }

    @Override protected void onDraw(Canvas canvas) {
        if (!bitmap.isRecycled()) canvas.drawBitmap(bitmap, null, getBounds(), paint);
    }

    private static Bitmap requireBitmap(Bitmap bitmap) {
        Objects.requireNonNull(bitmap, "Layer bitmap cannot be null.");
        if (bitmap.isRecycled()) throw new IllegalArgumentException("Layer bitmap is recycled.");
        return bitmap;
    }
}

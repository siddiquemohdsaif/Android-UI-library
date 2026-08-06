package com.ogfa.nativeviews.button;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class BitmapView implements ViewLayer {
    public Bitmap bitmap;
    private RectF rectF;
    private Paint paint = new Paint();

    public BitmapView(Bitmap bitmap, RectF rectF) {
        this.bitmap = bitmap;
        this.rectF = rectF;
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    public static BitmapView get(Bitmap bitmap, RectF rectF){
        return new BitmapView(bitmap, rectF);
    }

    /**
     * Evaluates a host-bound Position using the bitmap's dimensions in Figma space.
     */
    public static BitmapView get(Bitmap bitmap, Position position) {
        return new BitmapView(bitmap, position.toRectF(bitmap));
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, null, rectF, paint);
    }

    @Override
    public void clear() {
        //empty
    }

    @Override
    public void setRect(RectF rectF) {
        this.rectF = rectF;
    }
}

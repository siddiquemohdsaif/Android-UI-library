package com.ogfa.nativeviews.animator.component.layer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ogfa.nativeviews.component.Position;

public class BitmapLayer implements ComponentLayer {
    public Bitmap bitmap;
    private RectF rectF;
    private Paint paint = new Paint();

    public BitmapLayer(Bitmap bitmap, RectF rectF) {
        this.bitmap = bitmap;
        this.rectF = rectF;
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    public static BitmapLayer create(Bitmap bitmap, RectF rectF){
        return new BitmapLayer(bitmap, rectF);
    }

    /**
     * Evaluates a host-bound Position using the bitmap's dimensions in Figma space.
     */
    public static BitmapLayer create(Bitmap bitmap, Position position) {
        return new BitmapLayer(bitmap, position.toRectF(bitmap));
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, null, rectF, paint);
    }

    @Override
    public void release() {
        //empty
    }

    @Override
    public void setBounds(RectF rectF) {
        this.rectF = rectF;
    }
}

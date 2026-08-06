package com.ogfa.nativeviews.button;


import android.graphics.Canvas;
import android.graphics.RectF;

public interface ViewLayer {

    void onDraw(Canvas canvas);
    void clear();
    void setRect(RectF rectF);
}

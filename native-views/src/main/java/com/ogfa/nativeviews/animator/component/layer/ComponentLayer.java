package com.ogfa.nativeviews.animator.component.layer;


import android.graphics.Canvas;
import android.graphics.RectF;

public interface ComponentLayer {

    void draw(Canvas canvas);
    void release();
    void setBounds(RectF rectF);
}

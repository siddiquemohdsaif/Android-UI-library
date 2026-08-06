package com.ogfa.nativeviews.component;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;

public interface Component {
    String getId();
    RectF getBounds();
    void draw(Canvas canvas);
    boolean onTouchEvent(MotionEvent event);
    boolean isVisible();
    boolean isEnabled();
    void attach(ComponentHost host);
    void release();
}

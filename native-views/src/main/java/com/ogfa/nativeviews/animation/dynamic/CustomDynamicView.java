package com.ogfa.nativeviews.animation.dynamic;

import android.graphics.Canvas;
import android.graphics.RectF;

public interface CustomDynamicView {
    void onDraw(Canvas canvas, float progress, RectF rectF);
    long getDurationMillis();
    default void onReset() {}
    default void onRelease() {}

}

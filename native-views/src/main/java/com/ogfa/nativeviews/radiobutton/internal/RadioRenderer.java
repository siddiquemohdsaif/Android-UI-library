package com.ogfa.nativeviews.radiobutton.internal;

import android.graphics.Canvas;

public interface RadioRenderer {
    void draw(Canvas canvas, RadioRenderState state);
    void setImageFiltering(boolean enabled);
    void release();
}

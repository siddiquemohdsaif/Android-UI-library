package com.ogfa.nativeviews.checkbox.internal;

import android.graphics.Canvas;

public interface CheckBoxRenderer {
    void draw(Canvas canvas, CheckBoxRenderState state);
    void setImageFiltering(boolean enabled);
    void release();
}

package com.ogfa.nativeviews.radiobutton.internal;

import android.graphics.RectF;

/** Mutable allocation-free RadioButton draw snapshot. */
public final class RadioRenderState {
    public final RectF bounds = new RectF();
    public float progress;
    public boolean enabled;
    public float ringWidth;
    public float dotSize;
    public float padding;
    public int checkedColor;
    public int uncheckedColor;
    public int dotColor;
    public int backgroundColor;
    public int disabledCheckedColor;
    public int disabledUncheckedColor;
    public int disabledDotColor;
    public int disabledBackgroundColor;
}

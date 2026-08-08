package com.ogfa.nativeviews.switchcomponent.internal;

import android.graphics.RectF;

/** Mutable allocation-free draw snapshot populated by Switch. */
public final class SwitchRenderState {
    public final RectF trackBounds = new RectF();
    public final RectF thumbBounds = new RectF();
    public float progress;
    public boolean enabled;
    public float cornerRadius;
    public float strokeWidth;
    public boolean strokeEnabled;
    public int checkedTrackColor;
    public int uncheckedTrackColor;
    public int disabledCheckedTrackColor;
    public int disabledUncheckedTrackColor;
    public int thumbColor;
    public int disabledCheckedThumbColor;
    public int disabledUncheckedThumbColor;
    public int strokeColor;
    public int disabledStrokeColor;
}

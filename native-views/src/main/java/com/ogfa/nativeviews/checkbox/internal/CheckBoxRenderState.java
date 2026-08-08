package com.ogfa.nativeviews.checkbox.internal;

import android.graphics.RectF;

import com.ogfa.nativeviews.checkbox.CheckBox;

/** Mutable allocation-free draw snapshot populated by CheckBox. */
public final class CheckBoxRenderState {
    public final RectF bounds = new RectF();
    public CheckBox.State fromState;
    public CheckBox.State state;
    public float transitionProgress;
    public boolean enabled;
    public float cornerRadius;
    public float strokeWidth;
    public float markWidth;
    public float padding;
    public int checkedColor;
    public int uncheckedColor;
    public int indeterminateColor;
    public int disabledCheckedColor;
    public int disabledUncheckedColor;
    public int disabledIndeterminateColor;
    public int checkMarkColor;
    public int disabledCheckMarkColor;
    public int strokeColor;
    public int disabledStrokeColor;
}

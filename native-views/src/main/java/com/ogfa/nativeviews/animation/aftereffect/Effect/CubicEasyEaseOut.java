package com.ogfa.nativeviews.animation.aftereffect.Effect;

public class CubicEasyEaseOut implements Interpolator{

    private float startValue;
    private float endValue;

    private CubicEasyEaseOut(float startValue, float endValue) {
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public static CubicEasyEaseOut get(float startValue, float endValue){
        return new CubicEasyEaseOut(startValue, endValue);
    }

    @Override
    public float getInterpolation(float progress) {
        float f = progress - 1;
        float easedProgress = f * f * f + 1; // cubic ease-out
        return startValue + easedProgress * (endValue - startValue);
    }
}
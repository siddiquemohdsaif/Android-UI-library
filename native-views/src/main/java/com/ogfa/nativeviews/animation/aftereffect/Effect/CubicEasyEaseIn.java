package com.ogfa.nativeviews.animation.aftereffect.Effect;

public class CubicEasyEaseIn implements  Interpolator{

    private float startValue;
    private float endValue;

    private CubicEasyEaseIn(float startValue, float endValue) {
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public static CubicEasyEaseIn get(float startValue, float endValue){
        return new CubicEasyEaseIn(startValue, endValue);
    }

    @Override
    public float getInterpolation(float progress) {
        float easedProgress = progress * progress * progress; // cubic ease-in
        return startValue + easedProgress * (endValue - startValue);
    }
}
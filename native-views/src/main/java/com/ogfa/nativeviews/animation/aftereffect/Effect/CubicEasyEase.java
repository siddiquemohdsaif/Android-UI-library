package com.ogfa.nativeviews.animation.aftereffect.Effect;

public class CubicEasyEase implements Interpolator {

    private float startValue;
    private float endValue;

    private CubicEasyEase(float startValue, float endValue) {
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public static CubicEasyEase get(float startValue, float endValue){
        return new CubicEasyEase(startValue, endValue);
    }


    @Override
    public float getInterpolation(float progress) {
        float easedProgress;
        if (progress < 0.5f) {
            easedProgress = 4 * progress * progress * progress; // cubic ease-in
        } else {
            float f = ((2 * progress) - 2);
            easedProgress = 0.5f * f * f * f + 1; // cubic ease-out
        }
        return startValue + easedProgress * (endValue - startValue);
    }
}

package com.ogfa.nativeviews.animation.aftereffect.Effect;

public class Linear implements Interpolator{

    private float startValue;
    private float endValue;

    private Linear(float startValue, float endValue) {
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public static Linear get(float startValue, float endValue){
        return new Linear(startValue, endValue);
    }

    @Override
    public float getInterpolation(float progress){
        return startValue + progress * (endValue - startValue);
    }
}

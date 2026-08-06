package com.ogfa.nativeviews.animation.aftereffect.Effect;

public class Constant implements Interpolator{

    private float constantValue;

    private Constant(float constantValue) {
        this.constantValue = constantValue;
    }

    public static Constant get(float constantValue){
        return new Constant(constantValue);
    }

    @Override
    public float getInterpolation(float progress){
        return constantValue;
    }
}

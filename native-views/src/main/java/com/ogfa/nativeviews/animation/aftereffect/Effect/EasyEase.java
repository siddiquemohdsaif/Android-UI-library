package com.ogfa.nativeviews.animation.aftereffect.Effect;

public class EasyEase implements Interpolator{

    private float startValue;
    private float endValue;

    // Control points for cubic Bezier curve
    private static final float P1_X = 0.42f;
    private static final float P1_Y = 0f;
    private static final float P2_X = 0.58f;
    private static final float P2_Y = 1f;

    private EasyEase(float startValue, float endValue) {
        this.startValue = startValue;
        this.endValue = endValue;
    }

    public static EasyEase get(float startValue, float endValue){
        return new EasyEase(startValue, endValue);
    }

    @Override
    public float getInterpolation(float progress) {
        return startValue + cubicBezier(progress, P1_X, P1_Y, P2_X, P2_Y) * (endValue - startValue);
    }

    private float cubicBezier(float t, float x1, float y1, float x2, float y2) {
        
        float ay = 3 * y1 - 3 * y2 + 1;
        float by = 3 * y2 - 6 * y1;
        float cy = 3 * y1;

        // Calculate the cubic Bezier curve values for y (since x is the input progress)
        float y = ((ay * t + by) * t + cy) * t;
        return y;
    }
}

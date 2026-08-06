package com.ogfa.nativeviews.animation.aftereffect;

import com.ogfa.nativeviews.animation.aftereffect.Effect.Interpolator;

public class KeyFrameAnimationBuilder {

    private float posX;
    private float posY;
    private Interpolator posXInterpolator;
    private Interpolator posYInterpolator;
    private Interpolator scaleXInterpolator;
    private Interpolator scaleYInterpolator;
    private Interpolator rotationInterpolator;
    private Interpolator alphaInterpolator;
    private PathInterpolator pathInterpolator;

    public KeyFrameAnimationBuilder(float posX, float posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public KeyFrameAnimationBuilder setPosXInterpolator(Interpolator posXInterpolator) {
        this.posXInterpolator = posXInterpolator;
        return this;
    }

    public KeyFrameAnimationBuilder setPosYInterpolator(Interpolator posYInterpolator) {
        this.posYInterpolator = posYInterpolator;
        return this;
    }

    public KeyFrameAnimationBuilder setScaleXInterpolator(Interpolator scaleXInterpolator) {
        this.scaleXInterpolator = scaleXInterpolator;
        return this;
    }

    public KeyFrameAnimationBuilder setScaleYInterpolator(Interpolator scaleYInterpolator) {
        this.scaleYInterpolator = scaleYInterpolator;
        return this;
    }

    public KeyFrameAnimationBuilder setRotationInterpolator(Interpolator rotationInterpolator) {
        this.rotationInterpolator = rotationInterpolator;
        return this;
    }

    public KeyFrameAnimationBuilder setAlphaInterpolator(Interpolator alphaInterpolator) {
        this.alphaInterpolator = alphaInterpolator;
        return this;
    }

    public KeyFrameAnimationBuilder setPath(PathInterpolator.Path path, String type) {
        pathInterpolator = new PathInterpolator(path.copy(),type );
        return this;
    }

    public KeyFrameAnimation build() {
        FastBuildKeyFrameAnimation animation = new FastBuildKeyFrameAnimation(posX, posY);

        if (posXInterpolator != null) {
            animation.setPosXInterpolator(posXInterpolator);
        }
        if (posYInterpolator != null) {
            animation.setPosYInterpolator(posYInterpolator);
        }
        if (scaleXInterpolator != null) {
            animation.setScaleXInterpolator(scaleXInterpolator);
        }
        if (scaleYInterpolator != null) {
            animation.setScaleYInterpolator(scaleYInterpolator);
        }
        if (rotationInterpolator != null) {
            animation.setRotationInterpolator(rotationInterpolator);
        }
        if (alphaInterpolator != null) {
            animation.setAlphaInterpolator(alphaInterpolator);
        }
        if (pathInterpolator != null) {
            animation.setPathInterpolator(pathInterpolator);
        }

        return animation;
    }

    private static class FastBuildKeyFrameAnimation extends KeyFrameAnimation {
        public FastBuildKeyFrameAnimation(float posX, float posY) {
            super(posX, posY);
        }
    }
}

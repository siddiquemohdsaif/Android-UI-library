package com.ogfa.nativeviews.animation.aftereffect;

import android.graphics.PointF;

import com.ogfa.nativeviews.animation.aftereffect.Effect.Constant;
import com.ogfa.nativeviews.animation.aftereffect.Effect.Interpolator;

public abstract class KeyFrameAnimation implements KeyFrame {

    private Interpolator posYInterpolator;
    private Interpolator posXInterpolator;
    private Interpolator scaleXInterpolator;
    private Interpolator scaleYInterpolator;
    private Interpolator rotationInterpolator;
    private Interpolator alphaInterpolator;
    private PathInterpolator pathInterpolator;
    public boolean posXInterpolator_Set = false;
    public boolean posYInterpolator_Set = false;
    public boolean scaleXInterpolator_Set = false;
    public boolean scaleYInterpolator_Set = false;
    public boolean rotationInterpolator_Set = false;
    public boolean alphaInterpolator_Set = false;
    public float posX;
    public float posY;

    public KeyFrameAnimation(float posX , float posY){
        //default initialization
        posXInterpolator = Constant.get(posX);
        posYInterpolator = Constant.get(posY);
        scaleXInterpolator = Constant.get(100);
        scaleYInterpolator = Constant.get(100);
        rotationInterpolator = Constant.get(0);
        alphaInterpolator = Constant.get(100);
        this.posX = posX;
        this.posY = posY;
    }

    public Interpolator getPosYInterpolator() {
        return posYInterpolator;
    }

    public void setPosYInterpolator(Interpolator posYInterpolator) {
        this.posYInterpolator = posYInterpolator;
        posYInterpolator_Set = true;
    }

    public Interpolator getPosXInterpolator() {
        return posXInterpolator;
    }

    public void setPosXInterpolator(Interpolator posXInterpolator) {
        this.posXInterpolator = posXInterpolator;
        posXInterpolator_Set = true;
    }

    public Interpolator getScaleXInterpolator() {
        return scaleXInterpolator;
    }

    public void setScaleXInterpolator(Interpolator scaleXInterpolator) {
        this.scaleXInterpolator = scaleXInterpolator;
        scaleXInterpolator_Set = true;
    }

    public Interpolator getScaleYInterpolator() {
        return scaleYInterpolator;
    }

    public void setScaleYInterpolator(Interpolator scaleYInterpolator) {
        this.scaleYInterpolator = scaleYInterpolator;
        scaleYInterpolator_Set = true;
    }

    public Interpolator getRotationInterpolator() {
        return rotationInterpolator;
    }

    public void setRotationInterpolator(Interpolator rotationInterpolator) {
        this.rotationInterpolator = rotationInterpolator;
        rotationInterpolator_Set = true;
    }

    public Interpolator getAlphaInterpolator() {
        return alphaInterpolator;
    }

    public void setAlphaInterpolator(Interpolator alphaInterpolator) {
        float alphaAtStart = alphaInterpolator.getInterpolation(0);
        float alphaAtEnd = alphaInterpolator.getInterpolation(1);

        if (alphaAtStart < 0 || alphaAtStart > 100 || alphaAtEnd < 0 || alphaAtEnd > 100) {
            throw new IllegalArgumentException("Alpha value must be between 0 and 100 at both start and end of progress.");
        }
        this.alphaInterpolator = alphaInterpolator;
        alphaInterpolator_Set = true;
    }

    public void setPathInterpolator(PathInterpolator pathInterpolator){
        pathInterpolator.transform(posXInterpolator, posYInterpolator);
        this.pathInterpolator = pathInterpolator;
        posXInterpolator_Set = true;
        posYInterpolator_Set = true;
    }

    public PathInterpolator getPathInterpolator() {
        return pathInterpolator;
    }

    @Override
    public void getPosition(float progress, float animWindowX, float animWindowY, PointF positionForUpdate) {
        if (pathInterpolator != null){
            pathInterpolator.getPosition(progress, positionForUpdate) ;
        }else {
            positionForUpdate.x = posXInterpolator.getInterpolation(progress);
            positionForUpdate.y = posYInterpolator.getInterpolation(progress);
        }
    }

    @Override
    public void getScale(float progress, float animWindowX, float animWindowY, PointF saleDimensionForUpdate) {
        saleDimensionForUpdate.x = scaleXInterpolator.getInterpolation(progress);
        saleDimensionForUpdate.y = scaleYInterpolator.getInterpolation(progress);
    }

    @Override
    public float getRotation(float progress) {
        return rotationInterpolator.getInterpolation(progress);
    }

    @Override
    public float getAlpha(float progress) {
        return alphaInterpolator.getInterpolation(progress);
    }
}

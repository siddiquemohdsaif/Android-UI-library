package com.ogfa.nativeviews.animation.aftereffect;

import android.graphics.PointF;

public interface KeyFrame {
    void getPosition(float progress, float animWindowX , float animWindowY, PointF positionForUpdate); // position of image
    void getScale(float progress,  float animWindowX , float animWindowY, PointF saleDimensionForUpdate);  // width and height in percentage of image
    float getRotation(float progress);  // 0 - 360 => full rotation of image. it's encoding in degree angle
    float getAlpha(float progress);  // 0 - 1 alpha of image

}

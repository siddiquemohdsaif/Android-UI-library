package com.ogfa.nativeviews.button.internal;

import android.graphics.Canvas;

public class PressAnimation {

    public boolean isDownAnimation;
    public long startTime;
    private long duration;
    private float pivotX;
    private float pivotY;
    private float shrinkScale;

    public PressAnimation(boolean isDownAnimation, long startTime, long duration, float pivotX, float pivotY, float shrinkScale) {
        this.isDownAnimation = isDownAnimation;
        this.startTime = startTime;
        this.duration = duration;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.shrinkScale = shrinkScale;
    }

    public void applyAnimationPressed(Canvas canvas) {
        canvas.save();

        float elapsed = (float) (System.currentTimeMillis() - startTime);
        float progress = Math.min(elapsed / duration, 1.0f);
        float currentShrink;

        if (isDownAnimation) {
            currentShrink = 1 - progress * (1 - shrinkScale); // Shrinks from 1 to shrinkScale
        } else {
            currentShrink = shrinkScale + progress * (1 - shrinkScale); // Grows from shrinkScale to 1
        }

        canvas.scale(currentShrink, currentShrink, pivotX, pivotY);
    }

    public void restoreAnimationPressed(Canvas canvas) {
        canvas.restore();
    }

    public boolean isAnimationFinished() {
        return System.currentTimeMillis() > startTime + duration;
    }
}

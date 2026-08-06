package com.ogfa.nativeviews.animation.dynamic;


import android.graphics.Canvas;
import android.graphics.RectF;

public class DynamicViewAnimator {

    private final CustomDynamicView customDynamicView;
    private long duration;
    private int repeatCount;
    private int currentRepeat;
    private long startTime;
    private long timeElapsed;
    private boolean animationOn;
    private RectF rectF;

    public DynamicViewAnimator(CustomDynamicView customDynamicView, int repeatCount, RectF rectF){
        this.duration = customDynamicView.getDuration();
        this.customDynamicView = customDynamicView;
        this.repeatCount = repeatCount;
        this.rectF = rectF;
        this.startTime = System.currentTimeMillis();
        this.animationOn = true;
        this.currentRepeat = 0;
    }
    public void setRect(RectF newRectF) {
        this.rectF = new RectF(newRectF);
    }

    public void draw(Canvas canvas){

        // find time elapsed
        timeElapsed = System.currentTimeMillis() - startTime;
        if (timeElapsed > duration){
            if (repeatCount == -1){
                restartAnim();
            }else if (currentRepeat < repeatCount){
                restartAnim();
            }else {
                animationOn = false;
                return;
            }
        }



        // draw
        float progress = (float) timeElapsed / duration; // Calculate progress based on timeElapsed and duration
        customDynamicView.onDraw(canvas, progress, rectF);
    }

    private void restartAnim() {
        startTime = System.currentTimeMillis();
        timeElapsed = 0;
        currentRepeat++;
    }

    public boolean isAnimating(){
        return animationOn;
    }
}

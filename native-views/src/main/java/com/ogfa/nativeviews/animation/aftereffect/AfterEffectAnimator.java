package com.ogfa.nativeviews.animation.aftereffect;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.util.ArrayList;

public class AfterEffectAnimator {

    public final boolean loop;
    public ArrayList<Layer> layers;
    public AnimationWindow animationWindow;
    public long duration;
    public long startTime;
    private final Matrix matrix;
    private final Paint paint;

    public AfterEffectAnimator(AnimationWindow animationWindow, ArrayList<Layer> layers,long duration) {
        this.loop = false;
        this.layers = layers;
        this.animationWindow = animationWindow;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.matrix = new Matrix();
        this.paint = new Paint();
    }

    public AfterEffectAnimator(AnimationWindow animationWindow, ArrayList<Layer> layers,long duration, boolean loop) {
        this.loop = loop;
        this.layers = layers;
        this.animationWindow = animationWindow;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.matrix = new Matrix();
        this.paint = new Paint();
    }

    public boolean isAnimationFinished(){
        long currentTime = System.currentTimeMillis();
        if (loop){
            return false;
        }
        return startTime + duration <= currentTime;
    }

    public boolean isAnimationFinished(long pre){
        long currentTime = System.currentTimeMillis();
        if (loop){
            return false;
        }
        return startTime + duration <= currentTime + pre;
    }


    public void onDraw(Canvas canvas) {
        AfterEffectRenderer.render(canvas, this, matrix, paint);
    }

    public static void Draw(Canvas canvas, ArrayList<AfterEffectAnimator> afterEffectAnimatorList) {
        try {
            for (AfterEffectAnimator afterEffectAnimator : afterEffectAnimatorList) {
                afterEffectAnimator.onDraw(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

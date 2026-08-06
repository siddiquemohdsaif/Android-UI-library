package com.ogfa.nativeviews.animator.component.layer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.gif.GIFComposition;
import com.ogfa.nativeviews.animation.gif.GIFViewAnimator;

public class GifLayer implements ComponentLayer {
    private GIFViewAnimator gifViewAnimator = new GIFViewAnimator();

    private GifLayer(String id, GIFComposition composition, RectF rectF) {
        gifViewAnimator.addAnimation(id, composition, rectF,-1);
    }

    private GifLayer(String gifName, RectF rectF) {
        gifViewAnimator.addAnimation(gifName, rectF, -1);
    }

    private GifLayer(Context context, String gifName, RectF rectF) {
        gifViewAnimator.addAnimation(context, gifName, rectF, -1);
    }

    public static GifLayer create(String gifName, RectF rectF){
        return new GifLayer(gifName, rectF);
    }

    /**
     * Uses a preloaded GIF, or loads it once from assets/gif if it is not cached.
     */
    public static GifLayer create(Context context, String gifName, RectF rectF) {
        return new GifLayer(context, gifName, rectF);
    }

    public static GifLayer create(String id, GIFComposition composition, RectF rectF){
        return new GifLayer(id, composition, rectF);
    }

    @Override
    public void draw(Canvas canvas) {
        GIFViewAnimator.Draw(canvas, gifViewAnimator);
    }

    @Override
    public void release() {
    }

    @Override
    public void setBounds(RectF rectF) {
        //
    }
}

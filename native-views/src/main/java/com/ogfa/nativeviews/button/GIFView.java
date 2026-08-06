package com.ogfa.nativeviews.button;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.gif.GIFComposition;
import com.ogfa.nativeviews.animation.gif.GIFViewAnimator;

public class GIFView implements ViewLayer {
    private GIFViewAnimator gifViewAnimator = new GIFViewAnimator();

    private GIFView(String id, GIFComposition composition, RectF rectF) {
        gifViewAnimator.addAnimation(id, composition, rectF,-1);
    }

    private GIFView(String gifName, RectF rectF) {
        gifViewAnimator.addAnimation(gifName, rectF, -1);
    }

    private GIFView(Context context, String gifName, RectF rectF) {
        gifViewAnimator.addAnimation(context, gifName, rectF, -1);
    }

    public static GIFView get(String gifName, RectF rectF){
        return new GIFView(gifName, rectF);
    }

    /**
     * Uses a preloaded GIF, or loads it once from assets/gif if it is not cached.
     */
    public static GIFView get(Context context, String gifName, RectF rectF) {
        return new GIFView(context, gifName, rectF);
    }

    public static GIFView get(String id, GIFComposition composition, RectF rectF){
        return new GIFView(id, composition, rectF);
    }

    @Override
    public void onDraw(Canvas canvas) {
        GIFViewAnimator.Draw(canvas, gifViewAnimator);
    }

    @Override
    public void clear() {
    }

    @Override
    public void setRect(RectF rectF) {
        //
    }
}

package com.ogfa.nativeviews.animator.component.layer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import com.ogfa.nativeviews.animation.gif.GIFComposition;
import com.ogfa.nativeviews.animation.gif.GIFViewAnimator;
import com.ogfa.nativeviews.animator.component.LayerRegion;

public final class GifLayer extends BaseComponentLayer {
    private final GIFViewAnimator animator = new GIFViewAnimator();
    private final String animationId;

    private GifLayer(String layerId, String animationName, GIFComposition composition,
                     Context context, LayerRegion region) {
        super(layerId, region);
        animationId = animationName;
        RectF initial = new RectF(0, 0, 1, 1);
        if (composition != null) animator.addAnimation(animationName, composition, initial, -1);
        else if (context != null) animator.addAnimation(context, animationName, initial, -1);
        else animator.addAnimation(animationName, initial, -1);
    }

    public static GifLayer create(String id, String animationName, LayerRegion region) {
        return new GifLayer(id, animationName, null, null, region);
    }

    public static GifLayer create(Context context, String id, String assetName, LayerRegion region) {
        return new GifLayer(id, assetName, null, context, region);
    }

    public static GifLayer create(String id, GIFComposition composition, LayerRegion region) {
        return new GifLayer(id, id, composition, null, region);
    }

    @Override protected void onDraw(Canvas canvas) { GIFViewAnimator.Draw(canvas, animator); }
    @Override protected void onBoundsChanged(RectF bounds) { animator.setAnimationBounds(animationId, bounds); }
    @Override protected void onRelease() { animator.clear(); }
    @Override public boolean needsNextFrame() { return true; }
}

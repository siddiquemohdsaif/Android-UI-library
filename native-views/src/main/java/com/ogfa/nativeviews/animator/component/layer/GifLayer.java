package com.ogfa.nativeviews.animator.component.layer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import com.ogfa.nativeviews.animation.AnimatorComponent;
import com.ogfa.nativeviews.animation.gif.GIFComposition;
import com.ogfa.nativeviews.animation.gif.GifAnimator;
import com.ogfa.nativeviews.animator.component.LayerRegion;

public final class GifLayer extends BaseComponentLayer {
    private final GifAnimator animator;
    private GifLayer(String id, GifAnimator animator, LayerRegion region) { super(id, region); this.animator = animator; }
    public static GifLayer create(String id, String animationName, LayerRegion region) {
        throw new IllegalStateException("Context-free GIF creation was removed. Use create(context, id, assetName, region).");
    }
    public static GifLayer create(Context context, String id, String assetName, LayerRegion region) {
        return new GifLayer(id, new GifAnimator(context, id + "_playback", assetName,
                new RectF(0, 0, 1, 1), AnimatorComponent.INFINITE), region);
    }
    public static GifLayer create(String id, GIFComposition composition, LayerRegion region) {
        return new GifLayer(id, new GifAnimator(id + "_playback", composition,
                new RectF(0, 0, 1, 1), AnimatorComponent.INFINITE), region);
    }
    @Override protected void onDraw(Canvas canvas) { animator.draw(canvas); }
    @Override protected void onBoundsChanged(RectF bounds) { animator.setRegion(bounds); }
    @Override protected void onRelease() { animator.release(); }
    @Override public boolean needsNextFrame() { return animator.needsNextFrame(); }
}

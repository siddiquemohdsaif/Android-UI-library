package com.ogfa.nativeviews.animator.component.layer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import com.ogfa.nativeviews.animation.LottieViewAnimator;
import com.ogfa.nativeviews.animator.component.LayerRegion;

public final class LottieLayer extends BaseComponentLayer {
    private final LottieViewAnimator animator = new LottieViewAnimator();
    private final String animationId;

    private LottieLayer(Context context, String id, String animationName,
                        LayerRegion region, boolean repeat) {
        super(id, region);
        animationId = animationName;
        animator.addAnimation(context, animationName, 1, 1, 0, 0, repeat ? -1 : 0);
    }

    public static LottieLayer create(Context context, String id, String animationName,
                                     LayerRegion region) {
        return new LottieLayer(context, id, animationName, region, true);
    }

    public static LottieLayer create(Context context, String id, String animationName,
                                     LayerRegion region, boolean repeat) {
        return new LottieLayer(context, id, animationName, region, repeat);
    }

    @Override protected void onDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipRect(getBounds());
        LottieViewAnimator.Draw(canvas, animator);
        canvas.restoreToCount(save);
    }
    @Override protected void onBoundsChanged(RectF bounds) { animator.setAnimationBounds(animationId, bounds); }
    @Override protected void onRelease() { LottieViewAnimator.releaseLottieResources(animator); }
    @Override public boolean needsNextFrame() { return true; }
}

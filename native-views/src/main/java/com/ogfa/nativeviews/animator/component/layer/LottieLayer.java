package com.ogfa.nativeviews.animator.component.layer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import com.ogfa.nativeviews.animation.AnimatorComponent;
import com.ogfa.nativeviews.animation.LottieAnimator;
import com.ogfa.nativeviews.animator.component.LayerRegion;

public final class LottieLayer extends BaseComponentLayer {
    private final LottieAnimator animator;
    private LottieLayer(Context context, String id, String name, LayerRegion region, boolean repeat) {
        super(id, region);
        animator = new LottieAnimator(context, id + "_playback", name,
                new RectF(0, 0, 1, 1), repeat ? AnimatorComponent.INFINITE : 0);
    }
    public static LottieLayer create(Context context, String id, String name, LayerRegion region) { return new LottieLayer(context, id, name, region, true); }
    public static LottieLayer create(Context context, String id, String name, LayerRegion region, boolean repeat) { return new LottieLayer(context, id, name, region, repeat); }
    @Override protected void onDraw(Canvas canvas) { animator.draw(canvas); }
    @Override protected void onBoundsChanged(RectF bounds) { animator.setRegion(bounds); }
    @Override protected void onRelease() { animator.release(); }
    @Override public boolean needsNextFrame() { return animator.needsNextFrame(); }
}

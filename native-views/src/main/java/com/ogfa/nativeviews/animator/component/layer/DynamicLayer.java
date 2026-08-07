package com.ogfa.nativeviews.animator.component.layer;

import android.graphics.Canvas;
import android.graphics.RectF;
import com.ogfa.nativeviews.animation.AnimatorComponent;
import com.ogfa.nativeviews.animation.dynamic.CustomDynamicView;
import com.ogfa.nativeviews.animation.dynamic.DynamicViewAnimator;
import com.ogfa.nativeviews.animator.component.LayerRegion;

public final class DynamicLayer extends BaseComponentLayer {
    private final DynamicViewAnimator animator;
    private DynamicLayer(String id, CustomDynamicView view, LayerRegion region) {
        super(id, region);
        animator = new DynamicViewAnimator(id + "_playback", view,
                AnimatorComponent.INFINITE, new RectF(0, 0, 1, 1));
    }
    public static DynamicLayer create(String id, CustomDynamicView view, LayerRegion region) { return new DynamicLayer(id, view, region); }
    @Override protected void onDraw(Canvas canvas) { animator.draw(canvas); }
    @Override protected void onBoundsChanged(RectF bounds) { animator.setRegion(bounds); }
    @Override protected void onRelease() { animator.release(); }
    @Override public boolean needsNextFrame() { return animator.needsNextFrame(); }
}

package com.ogfa.nativeviews.animator.component.layer;

import android.graphics.Canvas;
import com.ogfa.nativeviews.animation.aftereffect.AfterEffectAnimator;
import com.ogfa.nativeviews.animator.component.LayerRegion;
import java.util.Objects;

public final class AfterEffectLayer extends BaseComponentLayer {
    private final AfterEffectAnimator animator;

    private AfterEffectLayer(String id, AfterEffectAnimator animator, LayerRegion region) {
        super(id, region);
        this.animator = Objects.requireNonNull(animator, "After-effect animator cannot be null.");
    }

    public static AfterEffectLayer create(String id, AfterEffectAnimator animator,
                                          LayerRegion region) {
        return new AfterEffectLayer(id, animator, region);
    }

    @Override protected void onDraw(Canvas canvas) { animator.onDraw(canvas); }
    @Override protected void onRelease() { animator.layers.clear(); }
    @Override public boolean needsNextFrame() { return true; }
}

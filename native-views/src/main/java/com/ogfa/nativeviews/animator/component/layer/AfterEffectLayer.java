package com.ogfa.nativeviews.animator.component.layer;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.aftereffect.AfterEffectAnimator;

public class AfterEffectLayer implements ComponentLayer {
    private final AfterEffectAnimator afterEffectAnimator;

    private AfterEffectLayer(AfterEffectAnimator afterEffectAnimator) {
        this.afterEffectAnimator = afterEffectAnimator;
    }

    public static AfterEffectLayer create(AfterEffectAnimator afterEffectAnimator){
        return new AfterEffectLayer(afterEffectAnimator);
    }

    @Override
    public void draw(Canvas canvas) {
        afterEffectAnimator.onDraw(canvas);
    }

    @Override
    public void release() {
        afterEffectAnimator.layers.clear();
    }
    @Override
    public void setBounds(RectF rectF) {
        //
    }
}

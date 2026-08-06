package com.ogfa.nativeviews.button;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.aftereffect.AfterEffectAnimator;

public class AfterEffectView implements ViewLayer {
    private final AfterEffectAnimator afterEffectAnimator;

    private AfterEffectView(AfterEffectAnimator afterEffectAnimator) {
        this.afterEffectAnimator = afterEffectAnimator;
    }

    public static AfterEffectView get(AfterEffectAnimator afterEffectAnimator){
        return new AfterEffectView(afterEffectAnimator);
    }

    @Override
    public void onDraw(Canvas canvas) {
        afterEffectAnimator.onDraw(canvas);
    }

    @Override
    public void clear() {
        afterEffectAnimator.layers.clear();
    }
    @Override
    public void setRect(RectF rectF) {
        //
    }
}

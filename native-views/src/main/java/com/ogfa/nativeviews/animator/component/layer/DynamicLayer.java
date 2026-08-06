package com.ogfa.nativeviews.animator.component.layer;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.dynamic.CustomDynamicView;
import com.ogfa.nativeviews.animation.dynamic.DynamicViewAnimator;

public class DynamicLayer implements ComponentLayer{

    private DynamicViewAnimator dynamicViewAnimator;

    public DynamicLayer(DynamicViewAnimator dynamicViewAnimator) {
        this.dynamicViewAnimator = dynamicViewAnimator;
    }

    public static DynamicLayer create(CustomDynamicView customDynamicView, RectF rectF){
        return new DynamicLayer(new DynamicViewAnimator(customDynamicView,-1,rectF));
    }

    @Override
    public void draw(Canvas canvas) {
        dynamicViewAnimator.draw(canvas);
    }

    @Override
    public void release() {
        //empty
    }

    @Override
    public void setBounds(RectF rectF) {
        if (dynamicViewAnimator != null) {
            dynamicViewAnimator.setRect(rectF);  // Pass updated rect
        }
    }


}

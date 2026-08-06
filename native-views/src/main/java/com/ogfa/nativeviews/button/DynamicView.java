package com.ogfa.nativeviews.button;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.dynamic.CustomDynamicView;
import com.ogfa.nativeviews.animation.dynamic.DynamicViewAnimator;

public class DynamicView implements ViewLayer{

    private DynamicViewAnimator dynamicViewAnimator;

    public DynamicView(DynamicViewAnimator dynamicViewAnimator) {
        this.dynamicViewAnimator = dynamicViewAnimator;
    }

    public static DynamicView get(CustomDynamicView customDynamicView, RectF rectF){
        return new DynamicView(new DynamicViewAnimator(customDynamicView,-1,rectF));
    }

    @Override
    public void onDraw(Canvas canvas) {
        dynamicViewAnimator.draw(canvas);
    }

    @Override
    public void clear() {
        //empty
    }

    @Override
    public void setRect(RectF rectF) {
        if (dynamicViewAnimator != null) {
            dynamicViewAnimator.setRect(rectF);  // Pass updated rect
        }
    }


}

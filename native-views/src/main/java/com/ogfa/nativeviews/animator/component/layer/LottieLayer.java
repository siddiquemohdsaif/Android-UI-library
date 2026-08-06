package com.ogfa.nativeviews.animator.component.layer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.LottieViewAnimator;
import com.ogfa.nativeviews.component.Position;

public class LottieLayer implements ComponentLayer {
    private LottieViewAnimator lottieViewAnimator = new LottieViewAnimator();

    private LottieLayer(Context context, String lottieName, RectF lottieViewParam, boolean repeat) {
        if (repeat){
            lottieViewAnimator.addAnimation(context, lottieName, (int) (lottieViewParam.right - lottieViewParam.left), (int) (lottieViewParam.bottom - lottieViewParam.top), (int) lottieViewParam.left, (int) lottieViewParam.top, -1);
        } else {
            lottieViewAnimator.addAnimation(context, lottieName, (int) (lottieViewParam.right - lottieViewParam.left), (int) (lottieViewParam.bottom - lottieViewParam.top), (int) lottieViewParam.left, (int) lottieViewParam.top, 0);
        }
    }

    public static LottieLayer create(Context context, String lottieName, RectF rectF,boolean repeat){
            return new LottieLayer(context, lottieName, rectF,repeat);
    }

    public static LottieLayer create(Context context, String lottieName, RectF rectF){
        return new LottieLayer(context, lottieName, rectF,true);
    }

    /**
     * Uses the Lottie composition's intrinsic dimensions in Figma space.
     */
    public static LottieLayer create(Context context, String lottieName, Position position) {
        return create(context, lottieName, position, true);
    }

    /**
     * Uses the Lottie composition's intrinsic dimensions in Figma space.
     */
    public static LottieLayer create(
            Context context,
            String lottieName,
            Position position,
            boolean repeat
    ) {
        Rect bounds = LottieViewAnimator.getCompositionBounds(context, lottieName);
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
            throw new IllegalArgumentException(
                    "Unable to read intrinsic bounds for Lottie animation: " + lottieName
            );
        }
        return new LottieLayer(
                context,
                lottieName,
                position.toRectF(bounds.width(), bounds.height()),
                repeat
        );
    }

    /**
     * Uses another bitmap's dimensions so the Lottie layer can match that bitmap.
     */
    public static LottieLayer create(
            Context context,
            String lottieName,
            Position position,
            Bitmap sizeSource
    ) {
        return new LottieLayer(context, lottieName, position.toRectF(sizeSource), true);
    }

    /**
     * Uses explicit Figma-space dimensions for the Lottie layer.
     */
    public static LottieLayer create(
            Context context,
            String lottieName,
            Position position,
            float figmaWidth,
            float figmaHeight
    ) {
        return new LottieLayer(
                context,
                lottieName,
                position.toRectF(figmaWidth, figmaHeight),
                true
        );
    }

    @Override
    public void draw(Canvas canvas) {
        LottieViewAnimator.Draw(canvas, lottieViewAnimator);
    }

    @Override
    public void release() {
        LottieViewAnimator.releaseLottieResources(lottieViewAnimator);
    }

    @Override
    public void setBounds(RectF rectF) {
        //
    }
}

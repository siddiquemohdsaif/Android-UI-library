package com.ogfa.nativeviews.button;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.ogfa.nativeviews.animation.LottieViewAnimator;

public class LottieView implements ViewLayer {
    private LottieViewAnimator lottieViewAnimator = new LottieViewAnimator();

    private LottieView(Context context, String lottieName, RectF lottieViewParam, boolean repeat) {
        if (repeat){
            lottieViewAnimator.addAnimation(context, lottieName, (int) (lottieViewParam.right - lottieViewParam.left), (int) (lottieViewParam.bottom - lottieViewParam.top), (int) lottieViewParam.left, (int) lottieViewParam.top, -1);
        } else {
            lottieViewAnimator.addAnimation(context, lottieName, (int) (lottieViewParam.right - lottieViewParam.left), (int) (lottieViewParam.bottom - lottieViewParam.top), (int) lottieViewParam.left, (int) lottieViewParam.top, 0);
        }
    }

    public static LottieView get(Context context, String lottieName, RectF rectF,boolean repeat){
            return new LottieView(context, lottieName, rectF,repeat);
    }

    public static LottieView get(Context context, String lottieName, RectF rectF){
        return new LottieView(context, lottieName, rectF,true);
    }

    /**
     * Uses the Lottie composition's intrinsic dimensions in Figma space.
     */
    public static LottieView get(Context context, String lottieName, Position position) {
        return get(context, lottieName, position, true);
    }

    /**
     * Uses the Lottie composition's intrinsic dimensions in Figma space.
     */
    public static LottieView get(
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
        return new LottieView(
                context,
                lottieName,
                position.toRectF(bounds.width(), bounds.height()),
                repeat
        );
    }

    /**
     * Uses another bitmap's dimensions so the Lottie layer can match that bitmap.
     */
    public static LottieView get(
            Context context,
            String lottieName,
            Position position,
            Bitmap sizeSource
    ) {
        return new LottieView(context, lottieName, position.toRectF(sizeSource), true);
    }

    /**
     * Uses explicit Figma-space dimensions for the Lottie layer.
     */
    public static LottieView get(
            Context context,
            String lottieName,
            Position position,
            float figmaWidth,
            float figmaHeight
    ) {
        return new LottieView(
                context,
                lottieName,
                position.toRectF(figmaWidth, figmaHeight),
                true
        );
    }

    @Override
    public void onDraw(Canvas canvas) {
        LottieViewAnimator.Draw(canvas, lottieViewAnimator);
    }

    @Override
    public void clear() {
        LottieViewAnimator.releaseLottieResources(lottieViewAnimator);
    }

    @Override
    public void setRect(RectF rectF) {
        //
    }
}

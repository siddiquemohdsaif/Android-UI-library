package com.ogfa.nativeviews.animator.component.internal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.RectF;

public final class MovementController {
    public interface UpdateListener { void onBounds(RectF bounds); }
    private ValueAnimator animator;
    private boolean cancelled;

    public void start(RectF from, RectF to, long duration, TimeInterpolator interpolator,
                      UpdateListener update, Runnable completion) {
        cancel();
        cancelled = false;
        RectF start = new RectF(from);
        RectF end = new RectF(to);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(Math.max(0L, duration));
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(value -> {
            float f = (float) value.getAnimatedValue();
            update.onBounds(new RectF(
                    lerp(start.left, end.left, f), lerp(start.top, end.top, f),
                    lerp(start.right, end.right, f), lerp(start.bottom, end.bottom, f)));
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) {
                if (!cancelled && completion != null) completion.run();
            }
        });
        animator.start();
    }

    public boolean isRunning() { return animator != null && animator.isRunning(); }
    public void pause() { if (animator != null && animator.isRunning()) animator.pause(); }
    public void resume() { if (animator != null && animator.isPaused()) animator.resume(); }
    public void cancel() { if (animator != null) animator.cancel(); animator = null; }
    public void finish() { if (animator != null) animator.end(); }
    private static float lerp(float a, float b, float f) { return a + (b - a) * f; }
}

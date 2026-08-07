package com.ogfa.nativeviews.animator.component.internal;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

public final class PressController {
    public interface UpdateListener { void onScale(float scale); }
    private final UpdateListener listener;
    private ValueAnimator animator;
    private float scale = 1f;

    public PressController(UpdateListener listener) { this.listener = listener; }

    public void animateTo(float target, long duration) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(scale, target);
        animator.setDuration(Math.max(0L, duration));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(value -> {
            scale = (float) value.getAnimatedValue();
            listener.onScale(scale);
        });
        animator.start();
    }

    public float getScale() { return scale; }
    public boolean isRunning() { return animator != null && animator.isRunning(); }
    public void release() { if (animator != null) animator.cancel(); animator = null; }
}

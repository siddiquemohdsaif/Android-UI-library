package com.ogfa.nativeviews.selection.internal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;

import java.util.Objects;

/** Internal checked-state and progress animator shared by selectable components. */
public final class CheckedStateController {
    public interface Callback {
        void onProgressChanged(float progress);
        void onCheckedChanged(boolean checked, boolean fromUser);
    }

    private final Callback callback;
    private boolean checked;
    private float progress;
    private long duration;
    private TimeInterpolator interpolator;
    private ValueAnimator animator;
    private boolean released;
    private boolean cancelling;

    public CheckedStateController(
            boolean checked,
            long duration,
            TimeInterpolator interpolator,
            Callback callback
    ) {
        this.checked = checked;
        progress = checked ? 1f : 0f;
        this.duration = requireDuration(duration);
        this.interpolator = Objects.requireNonNull(interpolator, "Interpolator cannot be null.");
        this.callback = Objects.requireNonNull(callback, "Callback cannot be null.");
    }

    public boolean isChecked() { return checked; }
    public float getProgress() { return progress; }
    public boolean isAnimating() { return animator != null && animator.isRunning(); }
    public long getDuration() { return duration; }

    public void setDuration(long value) { ensureActive(); duration = requireDuration(value); }
    public void setInterpolator(TimeInterpolator value) {
        ensureActive(); interpolator = Objects.requireNonNull(value, "Interpolator cannot be null.");
    }

    public boolean setChecked(boolean value, boolean animate, boolean fromUser) {
        ensureActive();
        boolean changed = checked != value;
        checked = value;
        if (changed) callback.onCheckedChanged(value, fromUser);
        animateTo(value ? 1f : 0f, animate);
        return changed;
    }

    public void setDragProgress(float value) {
        ensureActive();
        cancelAnimator(false);
        setProgress(clamp(value));
    }

    public boolean commitDrag(boolean value, boolean fromUser) {
        return setChecked(value, true, fromUser);
    }

    public void restoreStable(boolean animate) {
        ensureActive();
        animateTo(checked ? 1f : 0f, animate);
    }

    public void finishAnimation() {
        ensureActive();
        if (animator != null) animator.end();
    }

    public void cancelAnimation() {
        ensureActive();
        cancelAnimator(false);
        setProgress(checked ? 1f : 0f);
    }

    public void release() {
        if (released) return;
        cancelAnimator(false);
        released = true;
    }

    private void animateTo(float target, boolean animate) {
        cancelAnimator(false);
        if (!animate || duration == 0L || Math.abs(progress - target) < 0.0001f) {
            setProgress(target);
            return;
        }
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(value -> setProgress((float) value.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (!cancelling && animator == animation) animator = null;
            }
        });
        animator.start();
    }

    private void cancelAnimator(boolean snap) {
        if (animator != null) {
            ValueAnimator old = animator;
            animator = null;
            cancelling = true;
            old.cancel();
            cancelling = false;
        }
        if (snap) setProgress(checked ? 1f : 0f);
    }

    private void setProgress(float value) {
        progress = clamp(value);
        callback.onProgressChanged(progress);
    }

    private void ensureActive() {
        if (released) throw new IllegalStateException("Checked-state controller has been released.");
    }

    private static long requireDuration(long value) {
        if (value < 0L) throw new IllegalArgumentException("Animation duration cannot be negative.");
        return value;
    }
    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
}

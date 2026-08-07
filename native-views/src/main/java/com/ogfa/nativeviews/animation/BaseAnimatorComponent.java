package com.ogfa.nativeviews.animation;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import java.util.Objects;

public abstract class BaseAnimatorComponent implements AnimatorComponent {
    public interface OnClickListener { void onClick(String id); }

    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private ComponentHost owner;
    private PlaybackState state = PlaybackState.READY;
    private RepeatMode repeatMode;
    private AnimationListener listener;
    private OnClickListener clickListener;
    private int repeatCount;
    private int completedRepeats;
    private float speed;
    private float progress;
    private float alpha;
    private long lastFrameTime;
    private boolean reverse;
    private boolean clipToBounds;
    private boolean horizontalCenter;
    private boolean verticalCenter;
    private boolean progressCallbacks;
    private boolean pauseWhenHidden;
    private boolean pausedByVisibility;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean downInside;
    private boolean released;

    protected BaseAnimatorComponent(BaseAnimatorBuilder<?, ?> builder, View host) {
        this(builder.id, builder.resolveBounds(host), builder.autoPlay, builder.repeatCount,
                builder.repeatMode, builder.speed, builder.initialProgress, builder.clipToBounds,
                builder.horizontalCenter, builder.verticalCenter, builder.alpha,
                builder.progressCallbacks, builder.pauseWhenHidden, builder.listener,
                builder.clickListener);
    }

    protected BaseAnimatorComponent(String id, RectF bounds, boolean autoPlay, int repeatCount) {
        this(id, bounds, autoPlay, repeatCount, RepeatMode.RESTART, 1f, 0f, true,
                false, false, 1f, false, true, null, null);
    }

    private BaseAnimatorComponent(String id, RectF initialBounds, boolean autoPlay,
            int repeatCount, RepeatMode repeatMode, float speed, float progress,
            boolean clip, boolean hCenter, boolean vCenter, float alpha,
            boolean progressCallbacks, boolean pauseWhenHidden, AnimationListener listener,
            OnClickListener clickListener) {
        this.id = id;
        baseBounds.set(BaseAnimatorBuilder.requireRect(initialBounds));
        bounds.set(baseBounds);
        this.repeatCount = repeatCount;
        this.repeatMode = repeatMode;
        this.speed = speed;
        this.progress = progress;
        this.clipToBounds = clip;
        horizontalCenter = hCenter;
        verticalCenter = vCenter;
        this.alpha = alpha;
        this.progressCallbacks = progressCallbacks;
        this.pauseWhenHidden = pauseWhenHidden;
        this.listener = listener;
        this.clickListener = clickListener;
        state = autoPlay ? PlaybackState.PLAYING : PlaybackState.READY;
        lastFrameTime = SystemClock.uptimeMillis();
        if (listener != null) listener.onReady(id);
        if (autoPlay && listener != null) listener.onStarted(id);
    }

    @Override public final String getId() { return id; }
    @Override public final RectF getBounds() { return new RectF(bounds); }
    @Override public final PlaybackState getPlaybackState() { return state; }
    @Override public final float getProgress() { return progress; }
    @Override public final float getSpeed() { return speed; }
    @Override public final int getRepeatCount() { return repeatCount; }
    @Override public final RepeatMode getRepeatMode() { return repeatMode; }

    @Override public final void draw(Canvas canvas) {
        if (!visible || released || alpha <= 0f) return;
        updateProgress();
        int save = alpha >= 1f ? canvas.save() : canvas.saveLayerAlpha(bounds, Math.round(alpha * 255f));
        if (clipToBounds) canvas.clipRect(bounds);
        try { renderFrame(canvas, reverse ? 1f - progress : progress, new RectF(bounds)); }
        catch (RuntimeException error) { state = PlaybackState.FAILED; if (listener != null) listener.onError(id, error); throw error; }
        canvas.restoreToCount(save);
        if (needsNextFrame() && owner != null) owner.postInvalidateComponentOnAnimation();
    }

    private void updateProgress() {
        long now = SystemClock.uptimeMillis();
        if (state != PlaybackState.PLAYING) { lastFrameTime = now; return; }
        long duration = Math.max(1L, getDurationMillis());
        progress += ((now - lastFrameTime) * speed) / duration;
        lastFrameTime = now;
        while (progress >= 1f && state == PlaybackState.PLAYING) {
            if (repeatCount == INFINITE || completedRepeats < repeatCount) {
                progress -= 1f;
                completedRepeats++;
                if (repeatMode == RepeatMode.REVERSE) reverse = !reverse;
                if (listener != null) listener.onRepeated(id, completedRepeats);
                onRepeat();
            } else {
                progress = 1f;
                state = PlaybackState.COMPLETED;
                if (listener != null) listener.onCompleted(id);
                onStop();
            }
        }
        if (progressCallbacks && listener != null) listener.onProgress(id, reverse ? 1f - progress : progress);
    }

    protected abstract long getDurationMillis();
    protected abstract void renderFrame(Canvas canvas, float progress, RectF bounds);
    protected void onRepeat() {}
    protected void onStop() {}
    protected void onReleaseResources() {}

    @Override public final void play() { if (released) return; if (state == PlaybackState.COMPLETED) { progress = 0f; completedRepeats = 0; reverse = false; } state = PlaybackState.PLAYING; lastFrameTime = SystemClock.uptimeMillis(); if (listener != null) listener.onStarted(id); invalidate(); }
    @Override public final void pause() { if (state == PlaybackState.PLAYING) { updateProgress(); state = PlaybackState.PAUSED; if (listener != null) listener.onPaused(id); } }
    @Override public final void resume() { if (state == PlaybackState.PAUSED) play(); }
    @Override public final void restart() { progress = 0f; completedRepeats = 0; reverse = false; onRepeat(); play(); }
    @Override public final void stop() { state = PlaybackState.READY; progress = 0f; completedRepeats = 0; reverse = false; onStop(); invalidate(); }
    @Override public final void seekTo(float value) { progress = BaseAnimatorBuilder.requireProgress(value); lastFrameTime = SystemClock.uptimeMillis(); invalidate(); }
    @Override public final AnimatorComponent setSpeed(float value) { if (!(value > 0f) || !Float.isFinite(value)) throw new IllegalArgumentException("Speed must be positive and finite."); speed = value; return this; }
    @Override public final AnimatorComponent setRepeatCount(int value) { if (value < -1) throw new IllegalArgumentException("Repeat count must be -1 or greater."); repeatCount = value; return this; }
    @Override public final AnimatorComponent setRepeatMode(RepeatMode value) { repeatMode = Objects.requireNonNull(value); return this; }
    @Override public final boolean needsNextFrame() { return visible && state == PlaybackState.PLAYING; }

    public final BaseAnimatorComponent setRegion(RectF value) { baseBounds.set(BaseAnimatorBuilder.requireRect(value)); resolveBounds(); invalidate(); return this; }
    public final BaseAnimatorComponent setRegion(Position position, Size size) { return setRegion(position.toRectF(requireHost(), size)); }
    public final BaseAnimatorComponent horizontalCenter(boolean value) { horizontalCenter = value; resolveBounds(); invalidate(); return this; }
    public final BaseAnimatorComponent verticalCenter(boolean value) { verticalCenter = value; resolveBounds(); invalidate(); return this; }
    public final BaseAnimatorComponent setAlpha(float value) { alpha = BaseAnimatorBuilder.clampAlpha(value); invalidate(); return this; }
    public final float getAlpha() { return alpha; }
    public final BaseAnimatorComponent setClipToBounds(boolean value) { clipToBounds = value; invalidate(); return this; }
    public final BaseAnimatorComponent setOnAnimationListener(AnimationListener value) { listener = value; return this; }
    public final BaseAnimatorComponent setOnClickListener(OnClickListener value) { clickListener = value; return this; }
    @Override public final boolean isVisible() { return visible; }
    public final BaseAnimatorComponent setVisible(boolean value) {
        if (visible && !value && pauseWhenHidden && state == PlaybackState.PLAYING) {
            pause();
            pausedByVisibility = true;
        }
        visible = value;
        if (value && pausedByVisibility) {
            pausedByVisibility = false;
            resume();
        }
        invalidate();
        return this;
    }
    @Override public final boolean isEnabled() { return enabled; }
    public final BaseAnimatorComponent setEnabled(boolean value) { enabled = value; return this; }

    @Override public final boolean onTouchEvent(MotionEvent event) {
        if (!visible || !enabled || clickListener == null) return false;
        boolean inside = bounds.contains(event.getX(), event.getY());
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { downInside = inside; return inside; }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) { if (!inside) downInside = false; return downInside; }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) { boolean click = downInside && inside; downInside = false; if (click) clickListener.onClick(id); return click; }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) { boolean handled = downInside; downInside = false; return handled; }
        return downInside;
    }

    @Override public final void attach(ComponentHost host) { if (owner != null && owner != host) throw new IllegalStateException("Animator already has a host."); owner = host; resolveBounds(); }
    @Override public final void release() { if (released) return; released = true; state = PlaybackState.RELEASED; onReleaseResources(); owner = null; }

    private void resolveBounds() { bounds.set(baseBounds); if (owner != null && (horizontalCenter || verticalCenter)) { RectF parent = owner.getComponentBounds(); if (horizontalCenter) bounds.offsetTo(parent.centerX() - bounds.width() / 2f, bounds.top); if (verticalCenter) bounds.offsetTo(bounds.left, parent.centerY() - bounds.height() / 2f); } onBoundsChanged(new RectF(bounds)); }
    protected void onBoundsChanged(RectF bounds) {}
    private View requireHost() { if (owner == null) throw new IllegalStateException("Animator must be attached first."); return owner.getHostView(); }
    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
}

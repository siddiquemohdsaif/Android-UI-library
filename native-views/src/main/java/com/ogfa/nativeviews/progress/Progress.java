package com.ogfa.nativeviews.progress;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.ogfa.nativeviews.animation.AnimatorComponent;
import com.ogfa.nativeviews.animation.BaseAnimatorComponent;
import com.ogfa.nativeviews.animation.LottieAnimator;
import com.ogfa.nativeviews.animation.PlaybackState;
import com.ogfa.nativeviews.animation.RepeatMode;
import com.ogfa.nativeviews.animation.gif.GifAnimator;
import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.progress.internal.NativeProgressRenderer;
import com.ogfa.nativeviews.progress.internal.ProgressRenderState;

import java.util.Objects;

/** Native linear/circular or cached GIF/Lottie progress component. */
public final class Progress implements Component {
    public enum Style { LINEAR, CIRCULAR, ANIMATION }
    public enum Mode { DETERMINATE, INDETERMINATE }
    public enum AssetPlayback { AUTO_PLAY, FOLLOW_PROGRESS }
    public enum LinearDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM, BOTTOM_TO_TOP }
    public enum CircularDirection { CLOCKWISE, COUNTERCLOCKWISE }
    public enum StrokeCap { BUTT, ROUND, SQUARE }
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

    private static final long DEFAULT_INDETERMINATE_DURATION = 1200L;
    private static final float DEFAULT_SEGMENT_SIZE = 0.32f;
    private static final float DEFAULT_SWEEP_ANGLE = 96f;
    private static final float DEFAULT_DISABLED_ALPHA = 0.65f;

    private final View hostView;
    private final Context context;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final NativeProgressRenderer nativeRenderer = new NativeProgressRenderer();
    private final ProgressRenderState renderState = new ProgressRenderState();

    private ComponentHost owner;
    private FigmaConfig figmaConfig;
    private float dimensionScale;
    private Style style;
    private Mode mode;
    private float progress;
    private boolean completionReported;
    private OnProgressChangedListener progressListener;
    private OnProgressCompleteListener completeListener;
    private ValueAnimator progressAnimator;

    private LinearDirection linearDirection;
    private CircularDirection circularDirection;
    private StrokeCap strokeCap;
    private int trackColor;
    private int progressColor;
    private int disabledTrackColor;
    private int disabledProgressColor;
    private float thickness;
    private boolean thicknessInPixels;
    private float cornerRadius;
    private boolean cornerRadiusInPixels;
    private float padding;
    private boolean paddingInPixels;
    private float resolvedThickness;
    private float resolvedCornerRadius;
    private float resolvedPadding;
    private float startAngle;
    private float indeterminateSegmentSize;
    private float indeterminateSweepAngle;
    private long indeterminateDuration;
    private float indeterminatePhase;
    private ValueAnimator indeterminateAnimator;
    private PlaybackState nativePlaybackState = PlaybackState.READY;

    private ProgressAsset asset;
    private AssetPlayback assetPlayback;
    private BaseAnimatorComponent assetAnimator;
    private LottieAnimator.ImageResolver lottieImageResolver;
    private Image.ScaleType contentScaleType;
    private int repeatCount;
    private RepeatMode repeatMode;
    private float speed;
    private boolean autoPlay;

    private boolean visible;
    private boolean enabled;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private boolean released;
    private float alpha;
    private float disabledAlpha;
    private boolean pauseWhenHidden;
    private boolean pauseWhenDisabled;
    private boolean pausedByVisibility;
    private boolean pausedByDisabled;

    private Progress(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        context = builder.context;
        id = requireId(builder.id);
        style = builder.style;
        mode = builder.mode;
        progress = builder.progress;
        completionReported = progress >= 1f;
        progressListener = builder.progressListener;
        completeListener = builder.completeListener;
        linearDirection = builder.linearDirection;
        circularDirection = builder.circularDirection;
        strokeCap = builder.strokeCap;
        trackColor = builder.trackColor;
        progressColor = builder.progressColor;
        disabledTrackColor = builder.disabledTrackColor;
        disabledProgressColor = builder.disabledProgressColor;
        thickness = builder.thickness;
        thicknessInPixels = builder.thicknessInPixels;
        cornerRadius = builder.cornerRadius;
        cornerRadiusInPixels = builder.cornerRadiusInPixels;
        padding = builder.padding;
        paddingInPixels = builder.paddingInPixels;
        startAngle = builder.startAngle;
        indeterminateSegmentSize = builder.indeterminateSegmentSize;
        indeterminateSweepAngle = builder.indeterminateSweepAngle;
        indeterminateDuration = builder.indeterminateDuration;
        asset = builder.asset;
        assetPlayback = builder.assetPlayback;
        lottieImageResolver = builder.lottieImageResolver;
        contentScaleType = builder.contentScaleType;
        repeatCount = builder.repeatCount;
        repeatMode = builder.repeatMode;
        speed = builder.speed;
        autoPlay = builder.autoPlay;
        visible = builder.visible;
        enabled = builder.enabled;
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        alpha = builder.alpha;
        disabledAlpha = builder.disabledAlpha;
        pauseWhenHidden = builder.pauseWhenHidden;
        pauseWhenDisabled = builder.pauseWhenDisabled;
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        rebuildGeometry();
        if (style == Style.ANIMATION) rebuildAssetAnimator();
        else if (mode == Mode.INDETERMINATE && autoPlay) startNativeIndeterminate(false);
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() { return new RectF(bounds); }
    public FigmaConfig getFigmaConfig() { return figmaConfig; }
    public float getDimensionScale() { return dimensionScale; }
    public Style getStyle() { return style; }
    public Mode getMode() { return mode; }
    public float getProgress() { return progress; }
    public float getProgressPercent() { return progress * 100f; }
    public ProgressAsset getProgressAsset() { return asset; }
    public AssetPlayback getAssetPlayback() { return assetPlayback; }
    public PlaybackState getPlaybackState() {
        return style == Style.ANIMATION && assetAnimator != null
                ? assetAnimator.getPlaybackState() : nativePlaybackState;
    }
    public boolean isPlaying() { return getPlaybackState() == PlaybackState.PLAYING; }
    public boolean needsNextFrame() {
        return (assetAnimator != null && assetAnimator.needsNextFrame())
                || (indeterminateAnimator != null && indeterminateAnimator.isRunning())
                || (progressAnimator != null && progressAnimator.isRunning());
    }
    @Override public boolean isVisible() { return visible; }
    @Override public boolean isEnabled() { return enabled; }
    public float getAlpha() { return alpha; }
    public float getDisabledAlpha() { return disabledAlpha; }
    public float getResolvedThickness() { return resolvedThickness; }
    public float getResolvedCornerRadius() { return resolvedCornerRadius; }
    public float getResolvedPadding() { return resolvedPadding; }

    public Progress setProgress(float value) {
        ensureActive(); cancelProgressAnimator(); setProgressInternal(requireProgress(value)); return this;
    }
    public Progress setProgressPercent(float value) {
        return setProgress(requirePercent(value) / 100f);
    }
    public Progress animateProgressTo(float value, long duration) {
        return animateProgressTo(value, duration, Interpolator.EASE_IN_OUT);
    }
    public Progress animateProgressTo(float value, long duration, Interpolator interpolator) {
        ensureActive(); value = requireProgress(value); requireDuration(duration, "Progress duration");
        cancelProgressAnimator();
        if (duration == 0L || Math.abs(progress - value) < 0.0001f) {
            setProgressInternal(value); return this;
        }
        progressAnimator = ValueAnimator.ofFloat(progress, value);
        progressAnimator.setDuration(duration);
        progressAnimator.setInterpolator(toInterpolator(interpolator));
        progressAnimator.addUpdateListener(animation ->
                setProgressInternal((float) animation.getAnimatedValue()));
        progressAnimator.start(); return this;
    }
    public Progress animateProgressPercentTo(float value, long duration) {
        return animateProgressTo(requirePercent(value) / 100f, duration);
    }

    public Progress setMode(Mode value) {
        ensureNative(); value = Objects.requireNonNull(value, "Progress mode cannot be null.");
        if (mode == value) return this;
        mode = value;
        if (mode == Mode.INDETERMINATE) {
            if (autoPlay && visible && (!pauseWhenDisabled || enabled)) startNativeIndeterminate(false);
        } else stopNativeIndeterminate();
        invalidate(); return this;
    }
    public Progress useNativeLinearRendering() {
        ensureActive(); stopNativeIndeterminate(); releaseAssetAnimator(); style = Style.LINEAR; asset = null;
        mode = Mode.DETERMINATE; nativePlaybackState = PlaybackState.READY;
        rebuildGeometry(); invalidate(); return this;
    }
    public Progress useNativeCircularRendering() {
        ensureActive(); stopNativeIndeterminate(); releaseAssetAnimator(); style = Style.CIRCULAR; asset = null;
        mode = Mode.DETERMINATE; nativePlaybackState = PlaybackState.READY;
        rebuildGeometry(); invalidate(); return this;
    }
    public Progress setProgressAsset(ProgressAsset value) {
        ensureActive(); stopNativeIndeterminate();
        asset = Objects.requireNonNull(value, "Progress asset cannot be null.");
        style = Style.ANIMATION; rebuildAssetAnimator(); invalidate(); return this;
    }

    public Progress setRegion(Position position, Size size) {
        ensureActive(); resolveRegion(Objects.requireNonNull(position), Objects.requireNonNull(size), null);
        applyParentAlignment(); rebuildGeometry(); updateAssetBounds(); invalidate(); return this;
    }
    public Progress setRegion(RectF value) {
        ensureActive(); resolveRegion(null, null, Objects.requireNonNull(value));
        applyParentAlignment(); rebuildGeometry(); updateAssetBounds(); invalidate(); return this;
    }
    public Progress horizontalCenter(boolean value) {
        ensureActive(); horizontalCentered = value; applyParentAlignment();
        rebuildGeometry(); updateAssetBounds(); invalidate(); return this;
    }
    public Progress verticalCenter(boolean value) {
        ensureActive(); verticalCentered = value; applyParentAlignment();
        rebuildGeometry(); updateAssetBounds(); invalidate(); return this;
    }

    public Progress setTrackColor(int value) { ensureNative(); trackColor = value; invalidate(); return this; }
    public Progress setProgressColor(int value) { ensureNative(); progressColor = value; invalidate(); return this; }
    public Progress setDisabledTrackColor(int value) { ensureNative(); disabledTrackColor = value; invalidate(); return this; }
    public Progress setDisabledProgressColor(int value) { ensureNative(); disabledProgressColor = value; invalidate(); return this; }
    public Progress setThickness(float value) { ensureNative(); thickness = requirePositive(value, "Thickness"); thicknessInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Progress setThicknessPx(float value) { ensureNative(); thickness = requirePositive(value, "Thickness"); thicknessInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Progress setCornerRadius(float value) { ensureNative(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Progress setCornerRadiusPx(float value) { ensureNative(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Progress setPadding(float value) { ensureNative(); padding = requireNonNegative(value, "Padding"); paddingInPixels = false; rebuildGeometry(); invalidate(); return this; }
    public Progress setPaddingPx(float value) { ensureNative(); padding = requireNonNegative(value, "Padding"); paddingInPixels = true; rebuildGeometry(); invalidate(); return this; }
    public Progress setLinearDirection(LinearDirection value) { ensureStyle(Style.LINEAR); linearDirection = Objects.requireNonNull(value); rebuildGeometry(); invalidate(); return this; }
    public Progress setCircularDirection(CircularDirection value) { ensureStyle(Style.CIRCULAR); circularDirection = Objects.requireNonNull(value); invalidate(); return this; }
    public Progress setStrokeCap(StrokeCap value) { ensureStyle(Style.CIRCULAR); strokeCap = Objects.requireNonNull(value); invalidate(); return this; }
    public Progress setStartAngle(float value) { ensureStyle(Style.CIRCULAR); startAngle = requireFinite(value, "Start angle"); invalidate(); return this; }
    public Progress setIndeterminateDuration(long value) { ensureNative(); indeterminateDuration = requirePositiveDuration(value, "Indeterminate duration"); if (isPlaying()) startNativeIndeterminate(true); return this; }
    public Progress setIndeterminateSegmentSize(float value) { ensureStyle(Style.LINEAR); indeterminateSegmentSize = requireFractionExclusiveZero(value, "Segment size"); invalidate(); return this; }
    public Progress setIndeterminateSweepAngle(float value) { ensureStyle(Style.CIRCULAR); if (!Float.isFinite(value) || value <= 0f || value > 360f) throw new IllegalArgumentException("Sweep angle must be in (0, 360]."); indeterminateSweepAngle = value; invalidate(); return this; }

    public Progress setContentScaleType(Image.ScaleType value) {
        ensureAsset(); contentScaleType = Objects.requireNonNull(value); updateAssetBounds(); invalidate(); return this;
    }
    public Progress setAssetPlayback(AssetPlayback value) {
        ensureAsset(); assetPlayback = Objects.requireNonNull(value);
        if (assetPlayback == AssetPlayback.FOLLOW_PROGRESS) {
            assetAnimator.pause(); assetAnimator.seekTo(progress);
        } else if (autoPlay && visible && (!pauseWhenDisabled || enabled)) assetAnimator.play();
        invalidate(); return this;
    }
    public Progress setLottieImageResolver(LottieAnimator.ImageResolver value) {
        ensureLottieAsset(); lottieImageResolver = value; rebuildAssetAnimator(); invalidate(); return this;
    }
    public Progress setRepeatCount(int value) { ensureAsset(); if (value < -1) throw new IllegalArgumentException("Repeat count must be -1 or greater."); repeatCount = value; assetAnimator.setRepeatCount(value); return this; }
    public Progress setRepeatMode(RepeatMode value) { ensureAsset(); repeatMode = Objects.requireNonNull(value); assetAnimator.setRepeatMode(value); return this; }
    public Progress setSpeed(float value) { ensureAsset(); speed = requirePositive(value, "Speed"); assetAnimator.setSpeed(value); return this; }

    public Progress play() {
        ensurePlayable();
        if (style == Style.ANIMATION) {
            if (assetPlayback == AssetPlayback.FOLLOW_PROGRESS) throw new IllegalStateException("FOLLOW_PROGRESS assets are controlled by setProgress().");
            assetAnimator.play();
        } else startNativeIndeterminate(false);
        return this;
    }
    public Progress pause() {
        ensurePlayable();
        if (style == Style.ANIMATION) assetAnimator.pause();
        else if (indeterminateAnimator != null) {
            indeterminateAnimator.pause(); nativePlaybackState = PlaybackState.PAUSED;
        }
        return this;
    }
    public Progress resume() {
        ensurePlayable();
        if (style == Style.ANIMATION) assetAnimator.resume();
        else if (indeterminateAnimator != null && indeterminateAnimator.isPaused()) {
            indeterminateAnimator.resume(); nativePlaybackState = PlaybackState.PLAYING;
        } else startNativeIndeterminate(false);
        return this;
    }
    public Progress restart() {
        ensurePlayable();
        if (style == Style.ANIMATION) assetAnimator.restart();
        else { indeterminatePhase = 0f; startNativeIndeterminate(true); }
        return this;
    }
    public Progress stop() {
        ensurePlayable();
        if (style == Style.ANIMATION) assetAnimator.stop();
        else stopNativeIndeterminate();
        return this;
    }

    public Progress setOnProgressChangedListener(OnProgressChangedListener value) { ensureActive(); progressListener = value; return this; }
    public Progress removeOnProgressChangedListener() { return setOnProgressChangedListener(null); }
    public Progress setOnProgressCompleteListener(OnProgressCompleteListener value) { ensureActive(); completeListener = value; return this; }
    public Progress removeOnProgressCompleteListener() { return setOnProgressCompleteListener(null); }
    public Progress setAlpha(float value) { ensureActive(); alpha = requireAlpha(value); invalidate(); return this; }
    public Progress setDisabledAlpha(float value) { ensureActive(); disabledAlpha = requireAlpha(value); invalidate(); return this; }
    public Progress setVisible(boolean value) {
        ensureActive();
        if (visible == value) return this;
        if (visible && !value && pauseWhenHidden && isPlaying()) {
            pause(); pausedByVisibility = true;
        }
        visible = value;
        if (assetAnimator != null) assetAnimator.setVisible(value);
        if (value && pausedByVisibility) { pausedByVisibility = false; resume(); }
        invalidate(); return this;
    }
    public Progress setEnabled(boolean value) {
        ensureActive();
        if (enabled == value) return this;
        if (enabled && !value && pauseWhenDisabled && isPlaying()) {
            pause(); pausedByDisabled = true;
        }
        enabled = value;
        if (assetAnimator != null) assetAnimator.setEnabled(value);
        if (value && pausedByDisabled) { pausedByDisabled = false; resume(); }
        invalidate(); return this;
    }
    public Progress setPauseWhenHidden(boolean value) { ensureActive(); pauseWhenHidden = value; return this; }
    public Progress setPauseWhenDisabled(boolean value) { ensureActive(); pauseWhenDisabled = value; return this; }

    @Override public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || alpha <= 0f) return;
        float effectiveAlpha = alpha * (enabled ? 1f : disabledAlpha);
        int save = effectiveAlpha < 1f
                ? canvas.saveLayerAlpha(bounds, Math.round(255f * effectiveAlpha))
                : canvas.save();
        canvas.clipRect(bounds);
        if (style == Style.ANIMATION) {
            if (assetPlayback == AssetPlayback.FOLLOW_PROGRESS) assetAnimator.seekTo(progress);
            assetAnimator.draw(canvas);
        } else {
            populateRenderState(); nativeRenderer.draw(canvas, renderState);
        }
        canvas.restoreToCount(save);
    }

    @Override public boolean onTouchEvent(MotionEvent event) { return false; }

    @Override public void attach(ComponentHost value) {
        ensureActive(); Objects.requireNonNull(value, "Component host cannot be null.");
        if (owner != null && owner != value) throw new IllegalStateException("Progress already belongs to another host.");
        owner = value; applyParentAlignment(); rebuildGeometry();
        if (assetAnimator != null) { assetAnimator.attach(value); updateAssetBounds(); }
    }

    @Override public void release() {
        if (released) return;
        cancelProgressAnimator(); stopNativeIndeterminate(); releaseAssetAnimator();
        progressListener = null; completeListener = null; owner = null; released = true;
    }

    private void setProgressInternal(float value) {
        float old = progress;
        if (Math.abs(old - value) < 0.000001f) return;
        progress = value;
        if (assetAnimator != null && assetPlayback == AssetPlayback.FOLLOW_PROGRESS) {
            assetAnimator.seekTo(progress);
        }
        if (progressListener != null) progressListener.onProgressChanged(id, progress);
        if (progress >= 1f) {
            if (!completionReported && completeListener != null) completeListener.onProgressComplete(id);
            completionReported = true;
        } else completionReported = false;
        invalidateOnAnimation();
    }

    private void startNativeIndeterminate(boolean restart) {
        ensureNative();
        if (mode != Mode.INDETERMINATE) throw new IllegalStateException("Native playback requires INDETERMINATE mode.");
        if (restart) stopNativeIndeterminate();
        if (indeterminateAnimator != null && indeterminateAnimator.isPaused()) {
            indeterminateAnimator.resume();
            nativePlaybackState = PlaybackState.PLAYING;
            return;
        }
        if (indeterminateAnimator != null && indeterminateAnimator.isRunning()) return;
        indeterminateAnimator = ValueAnimator.ofFloat(indeterminatePhase, 1f);
        indeterminateAnimator.setDuration(Math.max(1L, (long) (indeterminateDuration * (1f - indeterminatePhase))));
        indeterminateAnimator.setInterpolator(new LinearInterpolator());
        indeterminateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        indeterminateAnimator.addUpdateListener(value -> {
            indeterminatePhase = (float) value.getAnimatedValue(); invalidateOnAnimation();
        });
        indeterminateAnimator.start(); nativePlaybackState = PlaybackState.PLAYING;
    }
    private void stopNativeIndeterminate() {
        if (indeterminateAnimator != null) indeterminateAnimator.cancel();
        indeterminateAnimator = null; indeterminatePhase = 0f;
        if (!released) nativePlaybackState = PlaybackState.READY;
        invalidate();
    }

    private void rebuildAssetAnimator() {
        releaseAssetAnimator();
        if (asset == null) throw new IllegalStateException("Animation Progress requires a ProgressAsset.");
        boolean shouldPlay = assetPlayback == AssetPlayback.AUTO_PLAY && autoPlay
                && visible && (!pauseWhenDisabled || enabled);
        RectF initial = new RectF(bounds);
        if (asset.getType() == ProgressAsset.Type.GIF) {
            assetAnimator = new GifAnimator.Builder(
                    context, id + "$progress_asset", asset.getName(), initial)
                    .setAutoPlay(shouldPlay)
                    .setRepeatCount(repeatCount)
                    .setRepeatMode(repeatMode)
                    .setSpeed(speed)
                    .setInitialProgress(assetPlayback == AssetPlayback.FOLLOW_PROGRESS ? progress : 0f)
                    .setPauseWhenHidden(pauseWhenHidden)
                    .build(hostView);
        } else {
            LottieAnimator.Builder builder = new LottieAnimator.Builder(
                    context, id + "$progress_asset", asset.getName(), initial)
                    .setAutoPlay(shouldPlay)
                    .setRepeatCount(repeatCount)
                    .setRepeatMode(repeatMode)
                    .setSpeed(speed)
                    .setInitialProgress(assetPlayback == AssetPlayback.FOLLOW_PROGRESS ? progress : 0f)
                    .setPauseWhenHidden(pauseWhenHidden);
            if (lottieImageResolver != null) builder.setImageResolver(lottieImageResolver);
            assetAnimator = builder.build(hostView);
        }
        assetAnimator.setVisible(visible);
        assetAnimator.setEnabled(enabled);
        if (owner != null) assetAnimator.attach(owner);
        updateAssetBounds();
        if (assetPlayback == AssetPlayback.FOLLOW_PROGRESS) {
            assetAnimator.pause(); assetAnimator.seekTo(progress);
        }
    }

    private void updateAssetBounds() {
        if (assetAnimator == null) return;
        int width;
        int height;
        if (assetAnimator instanceof GifAnimator) {
            width = ((GifAnimator) assetAnimator).getIntrinsicWidth();
            height = ((GifAnimator) assetAnimator).getIntrinsicHeight();
        } else {
            width = ((LottieAnimator) assetAnimator).getIntrinsicWidth();
            height = ((LottieAnimator) assetAnimator).getIntrinsicHeight();
        }
        if (width <= 0 || height <= 0 || contentScaleType == Image.ScaleType.FIT_XY) {
            assetAnimator.setRegion(bounds); return;
        }
        float sx = bounds.width() / width;
        float sy = bounds.height() / height;
        float scale = contentScaleType == Image.ScaleType.CENTER_CROP
                ? Math.max(sx, sy) : Math.min(sx, sy);
        float drawWidth = width * scale;
        float drawHeight = height * scale;
        assetAnimator.setRegion(new RectF(
                bounds.centerX() - drawWidth / 2f, bounds.centerY() - drawHeight / 2f,
                bounds.centerX() + drawWidth / 2f, bounds.centerY() + drawHeight / 2f));
    }

    private void releaseAssetAnimator() {
        if (assetAnimator != null) assetAnimator.release();
        assetAnimator = null;
    }
    private void resolveRegion(Position position, Size size, RectF explicit) {
        if (explicit != null) {
            requireBounds(explicit); baseBounds.set(explicit);
            figmaConfig = FigmaConfig.getDefault(); dimensionScale = figmaConfig.getScale(hostView.getWidth());
        } else {
            Objects.requireNonNull(position, "Position cannot be null.");
            Objects.requireNonNull(size, "Size cannot be null.");
            baseBounds.set(position.toRectF(hostView, size));
            figmaConfig = position.getFigmaConfig(); dimensionScale = position.getScale(hostView);
        }
        bounds.set(baseBounds);
    }
    private void applyParentAlignment() {
        bounds.set(baseBounds);
        if (owner == null) return;
        RectF parent = owner.getComponentBounds();
        if (horizontalCentered) bounds.offsetTo(parent.centerX() - bounds.width() / 2f, bounds.top);
        if (verticalCentered) bounds.offsetTo(bounds.left, parent.centerY() - bounds.height() / 2f);
    }
    private void rebuildGeometry() {
        resolvedThickness = resolve(thickness, thicknessInPixels);
        resolvedPadding = resolve(padding, paddingInPixels);
        resolvedCornerRadius = resolve(cornerRadius, cornerRadiusInPixels);
        float shortest = Math.min(bounds.width(), bounds.height());
        if (resolvedPadding * 2f >= shortest) throw new IllegalArgumentException("Progress padding leaves no drawable region.");
        if (style == Style.CIRCULAR && resolvedThickness + resolvedPadding * 2f > shortest) {
            throw new IllegalArgumentException("Circular Progress thickness does not fit its region.");
        }
        if (style == Style.LINEAR) {
            boolean vertical = linearDirection == LinearDirection.TOP_TO_BOTTOM
                    || linearDirection == LinearDirection.BOTTOM_TO_TOP;
            float crossSize = (vertical ? bounds.width() : bounds.height()) - resolvedPadding * 2f;
            if (resolvedThickness > crossSize) throw new IllegalArgumentException("Linear Progress thickness does not fit its region.");
            resolvedCornerRadius = Math.min(resolvedCornerRadius, resolvedThickness / 2f);
        }
    }
    private void populateRenderState() {
        renderState.bounds.set(bounds);
        renderState.style = style; renderState.mode = mode;
        renderState.linearDirection = linearDirection;
        renderState.circularDirection = circularDirection;
        renderState.strokeCap = strokeCap;
        renderState.progress = progress; renderState.phase = indeterminatePhase;
        renderState.thickness = resolvedThickness;
        renderState.cornerRadius = resolvedCornerRadius;
        renderState.padding = resolvedPadding;
        renderState.segmentSize = indeterminateSegmentSize;
        renderState.sweepAngle = indeterminateSweepAngle;
        renderState.startAngle = startAngle;
        renderState.trackColor = enabled ? trackColor : disabledTrackColor;
        renderState.progressColor = enabled ? progressColor : disabledProgressColor;
    }
    private void cancelProgressAnimator() {
        if (progressAnimator != null) progressAnimator.cancel(); progressAnimator = null;
    }
    private float resolve(float value, boolean pixels) { return pixels ? value : value * dimensionScale; }
    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
    private void invalidateOnAnimation() { if (owner != null) owner.postInvalidateComponentOnAnimation(); }
    private void ensureActive() { if (released) throw new IllegalStateException("Progress has been released: " + id); }
    private void ensureNative() { ensureActive(); if (style == Style.ANIMATION) throw new IllegalStateException("This API requires native Progress rendering."); }
    private void ensureAsset() { ensureActive(); if (style != Style.ANIMATION || assetAnimator == null) throw new IllegalStateException("This API requires GIF or Lottie Progress rendering."); }
    private void ensureLottieAsset() { ensureAsset(); if (asset.getType() != ProgressAsset.Type.LOTTIE) throw new IllegalStateException("Lottie image resolver requires a Lottie Progress asset."); }
    private void ensureStyle(Style expected) { ensureActive(); if (style != expected) throw new IllegalStateException("This API requires " + expected + " Progress style."); }
    private void ensurePlayable() {
        ensureActive();
        if (style != Style.ANIMATION && mode != Mode.INDETERMINATE) {
            throw new IllegalStateException("Playback is available only for indeterminate or asset Progress.");
        }
    }

    public static void preload(Context context, ProgressAsset... assets) {
        Objects.requireNonNull(context, "Context cannot be null.");
        Objects.requireNonNull(assets, "Progress assets cannot be null.");
        for (ProgressAsset asset : assets) {
            Objects.requireNonNull(asset, "Progress asset cannot be null.");
            if (asset.getType() == ProgressAsset.Type.GIF) GifAnimator.preload(context, asset.getName());
            else LottieAnimator.preload(context, asset.getName());
        }
    }
    public static boolean isLoaded(ProgressAsset asset) {
        Objects.requireNonNull(asset, "Progress asset cannot be null.");
        return asset.getType() == ProgressAsset.Type.GIF
                ? GifAnimator.isLoaded(asset.getName()) : LottieAnimator.isLoaded(asset.getName());
    }
    public static void clearCache(ProgressAsset asset) {
        Objects.requireNonNull(asset, "Progress asset cannot be null.");
        if (asset.getType() == ProgressAsset.Type.GIF) GifAnimator.clearCache(asset.getName());
        else LottieAnimator.clearCache(asset.getName());
    }

    private static TimeInterpolator toInterpolator(Interpolator value) {
        switch (Objects.requireNonNull(value)) {
            case LINEAR: return new LinearInterpolator();
            case EASE_IN: return new AccelerateInterpolator();
            case EASE_OUT: return new DecelerateInterpolator();
            default: return new AccelerateDecelerateInterpolator();
        }
    }
    private static String requireId(String value) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Progress ID cannot be blank."); return value.trim(); }
    private static void requireBounds(RectF value) { if (value.width() <= 0f || value.height() <= 0f) throw new IllegalArgumentException("Progress bounds must be positive."); }
    private static float requireProgress(float value) { if (!Float.isFinite(value) || value < 0f || value > 1f) throw new IllegalArgumentException("Progress must be in [0, 1]."); return value; }
    private static float requirePercent(float value) { if (!Float.isFinite(value) || value < 0f || value > 100f) throw new IllegalArgumentException("Progress percent must be in [0, 100]."); return value; }
    private static float requireAlpha(float value) { if (!Float.isFinite(value) || value < 0f || value > 1f) throw new IllegalArgumentException("Alpha must be in [0, 1]."); return value; }
    private static float requireNonNegative(float value, String label) { if (!Float.isFinite(value) || value < 0f) throw new IllegalArgumentException(label + " must be non-negative and finite."); return value; }
    private static float requirePositive(float value, String label) { if (!Float.isFinite(value) || value <= 0f) throw new IllegalArgumentException(label + " must be positive and finite."); return value; }
    private static float requireFinite(float value, String label) { if (!Float.isFinite(value)) throw new IllegalArgumentException(label + " must be finite."); return value; }
    private static float requireFractionExclusiveZero(float value, String label) { if (!Float.isFinite(value) || value <= 0f || value > 1f) throw new IllegalArgumentException(label + " must be in (0, 1]."); return value; }
    private static long requireDuration(long value, String label) { if (value < 0L) throw new IllegalArgumentException(label + " cannot be negative."); return value; }
    private static long requirePositiveDuration(long value, String label) { if (value <= 0L) throw new IllegalArgumentException(label + " must be positive."); return value; }

    public static final class Builder implements ComponentFactory<Progress> {
        private final Context context;
        private final String id;
        private Position position;
        private Size size;
        private RectF explicitBounds;
        private Style style = Style.LINEAR;
        private Mode mode = Mode.DETERMINATE;
        private float progress;
        private LinearDirection linearDirection = LinearDirection.LEFT_TO_RIGHT;
        private CircularDirection circularDirection = CircularDirection.CLOCKWISE;
        private StrokeCap strokeCap = StrokeCap.ROUND;
        private int trackColor = 0xffd9e1e8;
        private int progressColor = 0xff019cc4;
        private int disabledTrackColor = 0xffeeeeee;
        private int disabledProgressColor = 0xff9e9e9e;
        private float thickness = 12f;
        private boolean thicknessInPixels;
        private float cornerRadius = 6f;
        private boolean cornerRadiusInPixels;
        private float padding;
        private boolean paddingInPixels;
        private float startAngle = -90f;
        private float indeterminateSegmentSize = DEFAULT_SEGMENT_SIZE;
        private float indeterminateSweepAngle = DEFAULT_SWEEP_ANGLE;
        private long indeterminateDuration = DEFAULT_INDETERMINATE_DURATION;
        private ProgressAsset asset;
        private AssetPlayback assetPlayback = AssetPlayback.AUTO_PLAY;
        private LottieAnimator.ImageResolver lottieImageResolver;
        private Image.ScaleType contentScaleType = Image.ScaleType.FIT_CENTER;
        private int repeatCount = AnimatorComponent.INFINITE;
        private RepeatMode repeatMode = RepeatMode.RESTART;
        private float speed = 1f;
        private boolean autoPlay = true;
        private boolean visible = true;
        private boolean enabled = true;
        private boolean horizontalCentered;
        private boolean verticalCentered;
        private float alpha = 1f;
        private float disabledAlpha = DEFAULT_DISABLED_ALPHA;
        private boolean pauseWhenHidden = true;
        private boolean pauseWhenDisabled;
        private OnProgressChangedListener progressListener;
        private OnProgressCompleteListener completeListener;

        public Builder(Context context, String id, Position position, Size size) {
            this.context = Objects.requireNonNull(context, "Context cannot be null."); this.id = requireId(id);
            this.position = Objects.requireNonNull(position, "Position cannot be null."); this.size = Objects.requireNonNull(size, "Size cannot be null.");
        }
        public Builder(Context context, String id, RectF bounds) {
            this.context = Objects.requireNonNull(context, "Context cannot be null."); this.id = requireId(id);
            explicitBounds = new RectF(Objects.requireNonNull(bounds, "Bounds cannot be null.")); requireBounds(explicitBounds);
        }
        public Builder(Context context, String id, ProgressAsset asset, Position position, Size size) {
            this(context, id, position, size); configureAsset(asset);
        }
        public Builder(Context context, String id, ProgressAsset asset, RectF bounds) {
            this(context, id, bounds); configureAsset(asset);
        }
        public Builder setStyle(Style value) { style = Objects.requireNonNull(value); if (style == Style.ANIMATION && asset == null) throw new IllegalStateException("Use an asset constructor for ANIMATION style."); return this; }
        public Builder setMode(Mode value) { requireBuilderNative(); mode = Objects.requireNonNull(value); return this; }
        public Builder setProgress(float value) { progress = requireProgress(value); return this; }
        public Builder setProgressPercent(float value) { progress = requirePercent(value) / 100f; return this; }
        public Builder setTrackColor(int value) { requireBuilderNative(); trackColor = value; return this; }
        public Builder setProgressColor(int value) { requireBuilderNative(); progressColor = value; return this; }
        public Builder setDisabledTrackColor(int value) { requireBuilderNative(); disabledTrackColor = value; return this; }
        public Builder setDisabledProgressColor(int value) { requireBuilderNative(); disabledProgressColor = value; return this; }
        public Builder setThickness(float value) { requireBuilderNative(); thickness = requirePositive(value, "Thickness"); thicknessInPixels = false; return this; }
        public Builder setThicknessPx(float value) { requireBuilderNative(); thickness = requirePositive(value, "Thickness"); thicknessInPixels = true; return this; }
        public Builder setCornerRadius(float value) { requireBuilderNative(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = false; return this; }
        public Builder setCornerRadiusPx(float value) { requireBuilderNative(); cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = true; return this; }
        public Builder setPadding(float value) { requireBuilderNative(); padding = requireNonNegative(value, "Padding"); paddingInPixels = false; return this; }
        public Builder setPaddingPx(float value) { requireBuilderNative(); padding = requireNonNegative(value, "Padding"); paddingInPixels = true; return this; }
        public Builder setLinearDirection(LinearDirection value) { requireBuilderStyle(Style.LINEAR); linearDirection = Objects.requireNonNull(value); return this; }
        public Builder setCircularDirection(CircularDirection value) { requireBuilderStyle(Style.CIRCULAR); circularDirection = Objects.requireNonNull(value); return this; }
        public Builder setStrokeCap(StrokeCap value) { requireBuilderStyle(Style.CIRCULAR); strokeCap = Objects.requireNonNull(value); return this; }
        public Builder setStartAngle(float value) { requireBuilderStyle(Style.CIRCULAR); startAngle = requireFinite(value, "Start angle"); return this; }
        public Builder setIndeterminateDuration(long value) { requireBuilderNative(); indeterminateDuration = requirePositiveDuration(value, "Indeterminate duration"); return this; }
        public Builder setIndeterminateSegmentSize(float value) { requireBuilderStyle(Style.LINEAR); indeterminateSegmentSize = requireFractionExclusiveZero(value, "Segment size"); return this; }
        public Builder setIndeterminateSweepAngle(float value) { requireBuilderStyle(Style.CIRCULAR); if (!Float.isFinite(value) || value <= 0f || value > 360f) throw new IllegalArgumentException("Sweep angle must be in (0, 360]."); indeterminateSweepAngle = value; return this; }
        public Builder setAssetPlayback(AssetPlayback value) { requireBuilderAsset(); assetPlayback = Objects.requireNonNull(value); return this; }
        public Builder setContentScaleType(Image.ScaleType value) { requireBuilderAsset(); contentScaleType = Objects.requireNonNull(value); return this; }
        public Builder setLottieImageResolver(LottieAnimator.ImageResolver value) { requireBuilderLottie(); lottieImageResolver = value; return this; }
        public Builder setRepeatCount(int value) { requireBuilderAsset(); if (value < -1) throw new IllegalArgumentException("Repeat count must be -1 or greater."); repeatCount = value; return this; }
        public Builder setRepeatMode(RepeatMode value) { requireBuilderAsset(); repeatMode = Objects.requireNonNull(value); return this; }
        public Builder setSpeed(float value) { requireBuilderAsset(); speed = requirePositive(value, "Speed"); return this; }
        public Builder setAutoPlay(boolean value) { autoPlay = value; return this; }
        public Builder setOnProgressChangedListener(OnProgressChangedListener value) { progressListener = value; return this; }
        public Builder setOnProgressCompleteListener(OnProgressCompleteListener value) { completeListener = value; return this; }
        public Builder setAlpha(float value) { alpha = requireAlpha(value); return this; }
        public Builder setDisabledAlpha(float value) { disabledAlpha = requireAlpha(value); return this; }
        public Builder setVisible(boolean value) { visible = value; return this; }
        public Builder setEnabled(boolean value) { enabled = value; return this; }
        public Builder horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder verticalCenter(boolean value) { verticalCentered = value; return this; }
        public Builder setPauseWhenHidden(boolean value) { pauseWhenHidden = value; return this; }
        public Builder setPauseWhenDisabled(boolean value) { pauseWhenDisabled = value; return this; }
        @Override public Progress build(View hostView) { return new Progress(this, hostView); }

        private void configureAsset(ProgressAsset value) {
            asset = Objects.requireNonNull(value, "Progress asset cannot be null.");
            style = Style.ANIMATION; mode = Mode.INDETERMINATE; disabledAlpha = 0.45f;
        }
        private void requireBuilderNative() { if (style == Style.ANIMATION) throw new IllegalStateException("This API requires native Progress rendering."); }
        private void requireBuilderAsset() { if (style != Style.ANIMATION || asset == null) throw new IllegalStateException("This API requires GIF or Lottie Progress rendering."); }
        private void requireBuilderLottie() { requireBuilderAsset(); if (asset.getType() != ProgressAsset.Type.LOTTIE) throw new IllegalStateException("Lottie image resolver requires a Lottie Progress asset."); }
        private void requireBuilderStyle(Style expected) { if (style != expected) throw new IllegalStateException("This API requires " + expected + " Progress style."); }
    }
}

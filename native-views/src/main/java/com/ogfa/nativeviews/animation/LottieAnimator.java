package com.ogfa.nativeviews.animation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;

/** First-class ZLayer Lottie component driven by the shared animation clock. */
public final class LottieAnimator extends BaseAnimatorComponent {
    public interface ImageResolver { Bitmap resolve(String assetName); }
    private final LottieComposition composition;
    private final LottieDrawable drawable;

    private LottieAnimator(Builder builder, View host) {
        super(builder, host);
        composition = LottieViewAnimator.getOrLoad(builder.context, builder.animationName);
        drawable = LottieViewAnimator.createDrawableForComponent(builder.context, composition);
        if (builder.imageResolver != null) drawable.setImageAssetDelegate(
                asset -> builder.imageResolver.resolve(asset.getFileName()));
    }

    @Override protected long getDurationMillis() { return Math.max(1L, (long) composition.getDuration()); }
    @Override protected void renderFrame(Canvas canvas, float progress, RectF bounds) {
        drawable.setProgress(progress);
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) throw new IllegalStateException("Lottie composition has invalid intrinsic dimensions.");
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / intrinsicWidth, bounds.height() / intrinsicHeight);
        drawable.draw(canvas);
        canvas.restoreToCount(save);
    }
    @Override protected void onReleaseResources() { drawable.stop(); drawable.clearComposition(); }

    public static void preload(Context context, String name) { LottieViewAnimator.preloadAnimations(context, name); }
    public static void preload(Context context, String... names) { LottieViewAnimator.preloadAnimations(context, names); }
    public static boolean isLoaded(String name) { return LottieViewAnimator.isLoaded(name); }
    public static void clearCache(String name) { LottieViewAnimator.clearCache(name); }
    public static void clearCache() { LottieViewAnimator.clearCache(); }
    public static void shutdown() { LottieViewAnimator.shutdown(); }

    public static final class Builder extends BaseAnimatorBuilder<Builder, LottieAnimator> {
        private final String animationName;
        private ImageResolver imageResolver;
        public Builder(Context context, String id, String animationName, Position position, Size size) {
            super(context, id, position, size); this.animationName = requireName(animationName);
        }
        public Builder(Context context, String id, String animationName, RectF bounds) {
            super(context, id, bounds); this.animationName = requireName(animationName);
        }
        public Builder setImageResolver(ImageResolver resolver) { imageResolver = resolver; return this; }
        @Override public LottieAnimator build(View hostView) { return new LottieAnimator(this, hostView); }
        private static String requireName(String value) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Lottie animation name cannot be blank."); return value; }
    }
}

package com.ogfa.nativeviews.animation.gif;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import com.ogfa.nativeviews.animation.BaseAnimatorBuilder;
import com.ogfa.nativeviews.animation.BaseAnimatorComponent;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import java.util.Objects;

/** First-class ZLayer GIF component with shared cached composition data. */
public final class GifAnimator extends BaseAnimatorComponent {
    private final GIFComposition composition;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private GifAnimator(Builder builder, View host) {
        super(builder, host);
        composition = GIFViewAnimator.getOrLoad(builder.getContext(), builder.assetName);
    }

    /** Detached constructor used by CustomAnimatorComponent's GifLayer adapter. */
    public GifAnimator(Context context, String id, String assetName, RectF bounds, int repeatCount) {
        super(id, bounds, true, repeatCount);
        composition = GIFViewAnimator.getOrLoad(context, assetName);
    }

    public GifAnimator(String id, GIFComposition composition, RectF bounds, int repeatCount) {
        super(id, bounds, true, repeatCount);
        this.composition = Objects.requireNonNull(composition, "GIF composition cannot be null.");
    }

    @Override protected long getDurationMillis() { return composition.getDuration(); }
    @Override protected void renderFrame(Canvas canvas, float progress, RectF bounds) {
        composition.draw(canvas, bounds, Math.round(progress * composition.getDuration()), paint);
    }

    public static void preload(Context context, String name) { GIFViewAnimator.preloadAnimations(context, name); }
    public static void preload(Context context, String... names) { GIFViewAnimator.preloadAnimations(context, names); }
    public static boolean isLoaded(String name) { return GIFViewAnimator.isLoaded(name); }
    public static void clearCache(String name) { GIFViewAnimator.clearCache(name); }
    public static void clearCache() { GIFViewAnimator.clearCache(); }
    public static void shutdown() { GIFViewAnimator.shutdown(); }

    public static final class Builder extends BaseAnimatorBuilder<Builder, GifAnimator> {
        private final String assetName;
        public Builder(Context context, String id, String assetName, Position position, Size size) {
            super(context, id, position, size);
            this.assetName = requireName(assetName);
        }
        public Builder(Context context, String id, String assetName, RectF bounds) {
            super(context, id, bounds);
            this.assetName = requireName(assetName);
        }
        @Override public GifAnimator build(View hostView) { return new GifAnimator(this, hostView); }
        private static String requireName(String value) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("GIF asset name cannot be blank."); return value; }
    }
}

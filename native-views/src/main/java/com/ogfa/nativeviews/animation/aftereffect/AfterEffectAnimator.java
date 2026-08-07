package com.ogfa.nativeviews.animation.aftereffect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.View;
import com.ogfa.nativeviews.animation.BaseAnimatorBuilder;
import com.ogfa.nativeviews.animation.BaseAnimatorComponent;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import java.util.ArrayList;
import java.util.Objects;

/** After Effects timeline component with immutable reusable composition data. */
public final class AfterEffectAnimator extends BaseAnimatorComponent {
    private final AfterEffectComposition composition;
    private final Matrix matrix = new Matrix();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final PointF positionScratch = new PointF();
    private final PointF scaleScratch = new PointF();

    private AfterEffectAnimator(Builder builder, View host) {
        super(builder, host);
        composition = builder.composition;
    }

    /** Detached constructor retained for layer composition. */
    public AfterEffectAnimator(AnimationWindow window, ArrayList<Layer> layers, long duration) {
        this(window, layers, duration, false);
    }
    public AfterEffectAnimator(AnimationWindow window, ArrayList<Layer> layers, long duration, boolean loop) {
        super("after_effect_playback", new RectF(window.left, window.top,
                window.left + window.width, window.top + window.height), true, loop ? INFINITE : 0);
        composition = new AfterEffectComposition.Builder(window, duration).addLayers(layers).build();
    }

    @Override protected long getDurationMillis() { return composition.getDurationMillis(); }
    @Override protected void renderFrame(Canvas canvas, float progress, RectF bounds) {
        AnimationWindow window = composition.getWindow();
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / window.width, bounds.height() / window.height);
        canvas.translate(-window.left, -window.top);
        AfterEffectRenderer.render(canvas, composition,
                Math.round(progress * composition.getDurationMillis()), matrix, paint,
                positionScratch, scaleScratch);
        canvas.restoreToCount(save);
    }

    public AfterEffectComposition getComposition() { return composition; }

    public static final class Builder extends BaseAnimatorBuilder<Builder, AfterEffectAnimator> {
        private final AfterEffectComposition composition;
        public Builder(Context context, String id, AfterEffectComposition composition,
                       Position position, Size size) {
            super(context, id, position, size);
            this.composition = Objects.requireNonNull(composition, "Composition cannot be null.");
        }
        public Builder(Context context, String id, AfterEffectComposition composition, RectF bounds) {
            super(context, id, bounds);
            this.composition = Objects.requireNonNull(composition, "Composition cannot be null.");
        }
        @Override public AfterEffectAnimator build(View hostView) { return new AfterEffectAnimator(this, hostView); }
    }
}

package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ogfa.nativeviews.animation.AnimatorComponent;
import com.ogfa.nativeviews.animation.LottieAnimator;
import com.ogfa.nativeviews.animation.RepeatMode;
import com.ogfa.nativeviews.animation.aftereffect.AfterEffectAnimator;
import com.ogfa.nativeviews.animation.aftereffect.AfterEffectComposition;
import com.ogfa.nativeviews.animation.aftereffect.KeyFrameAnimation;
import com.ogfa.nativeviews.animation.aftereffect.KeyFrameAnimationBuilder;
import com.ogfa.nativeviews.animation.aftereffect.Layer;
import com.ogfa.nativeviews.animation.aftereffect.Effect.Linear;
import com.ogfa.nativeviews.animation.dynamic.CustomDynamicView;
import com.ogfa.nativeviews.animation.dynamic.DynamicViewAnimator;
import com.ogfa.nativeviews.animation.gif.GifAnimator;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/** Direct ZLayer smoke test for all four hardened animator components. */
public final class AnimatorComponentsTestActivity extends AppCompatActivity {
    private AnimatorTestView view;
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        GifAnimator.preload(this, "carrom_pass_buy");
        LottieAnimator.preload(this, "win_animation");
        view = new AnimatorTestView(this);
        setContentView(view);
    }
    @Override protected void onDestroy() { if (view != null) view.release(); super.onDestroy(); }

    static final class AnimatorTestView extends View {
        private final ZLayerGroup ui;
        private final ZLayer animations;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean built;

        AnimatorTestView(Context context) {
            super(context);
            ui = new ZLayerGroup(this);
            ui.setAutoInvalidate(false);
            animations = ui.addLayer("standalone_animators");
            setBackgroundColor(0xff08111f);
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w <= 0 || built) return;
            built = true;
            float margin = dp(26);
            float width = w - margin * 2f;

            animations.add(new GifAnimator.Builder(getContext(), "gif", "carrom_pass_buy",
                    new RectF(margin, dp(120), margin + width, dp(120) + width * 0.28f))
                    .setRepeatCount(AnimatorComponent.INFINITE)
                    .setOnClickListener(id -> toggle(id)));

            animations.add(new LottieAnimator.Builder(getContext(), "lottie", "win_animation",
                    new RectF(margin, dp(300), margin + dp(150), dp(450)))
                    .setRepeatCount(AnimatorComponent.INFINITE)
                    .setRepeatMode(RepeatMode.REVERSE)
                    .setOnClickListener(id -> toggle(id)));

            animations.add(new DynamicViewAnimator.Builder(getContext(), "dynamic",
                    new CustomDynamicView() {
                        @Override public void onDraw(Canvas canvas, float progress, RectF bounds) {
                            paint.setColor(0xff00b4d8);
                            canvas.drawCircle(bounds.centerX(), bounds.centerY(),
                                    bounds.width() * (0.16f + 0.30f * progress), paint);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(dp(3));
                            paint.setColor(Color.WHITE);
                            canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() * 0.46f, paint);
                            paint.setStyle(Paint.Style.FILL);
                        }
                        @Override public long getDurationMillis() { return 1000L; }
                    }, new RectF(w - margin - dp(150), dp(300), w - margin, dp(450)))
                    .setRepeatCount(AnimatorComponent.INFINITE)
                    .setRepeatMode(RepeatMode.REVERSE)
                    .setOnClickListener(id -> toggle(id)));

            AfterEffectComposition composition = createAfterEffectComposition();
            float effectTop = dp(500);
            float effectHeight = width * (150f / 740f);
            animations.add(new AfterEffectAnimator.Builder(getContext(), "after_effect", composition,
                    new RectF(margin, effectTop, w - margin, effectTop + effectHeight))
                    .setRepeatCount(AnimatorComponent.INFINITE)
                    .setRepeatMode(RepeatMode.REVERSE)
                    .setOnClickListener(id -> toggle(id)));
        }

        private AfterEffectComposition createAfterEffectComposition() {
            Bitmap dot = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(dot);
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(0xffffb703);
            canvas.drawCircle(40, 40, 38, dotPaint);
            Layer layer = new Layer(dot);
            KeyFrameAnimation animation = new KeyFrameAnimationBuilder(70f, 75f)
                    .setPosXInterpolator(Linear.get(70f, 670f))
                    .setRotationInterpolator(Linear.get(0f, 360f))
                    .build();
            layer.addKeyFrameTimeLineDefinition(animation, 0L, 1600L);
            return new AfterEffectComposition.Builder(new Size(740f, 150f), 1600L)
                    .addLayer(layer)
                    .build();
        }

        private void toggle(String id) {
            AnimatorComponent animator = (AnimatorComponent) animations.find(id);
            if (animator == null) return;
            if (animator.getPlaybackState() == com.ogfa.nativeviews.animation.PlaybackState.PLAYING) animator.pause();
            else animator.resume();
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dp(18));
            canvas.drawText("Standalone Animator Components", getWidth() / 2f, dp(55), paint);
            paint.setColor(0xff9fb3c8);
            paint.setTextSize(dp(14));
            canvas.drawText("GIF • Lottie • Dynamic • After Effects — tap to pause/resume",
                    getWidth() / 2f, dp(84), paint);
            ui.draw(canvas);
        }
        @Override public boolean onTouchEvent(MotionEvent event) { return ui.onTouchEvent(event) || super.onTouchEvent(event); }
        void release() { ui.release(); }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    }
}

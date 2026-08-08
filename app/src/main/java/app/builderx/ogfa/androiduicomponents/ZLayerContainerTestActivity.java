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
import com.ogfa.nativeviews.button.Button;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerContainer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/** Generic nested composition test for arbitrary Canvas components. */
public final class ZLayerContainerTestActivity extends AppCompatActivity {
    private ContainerTestView view;
    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        GifAnimator.preload(this, "carrom_pass_buy");
        LottieAnimator.preload(this, "win_animation");
        view = new ContainerTestView(this);
        setContentView(view);
    }
    @Override protected void onDestroy() { if (view != null) view.release(); super.onDestroy(); }

    static final class ContainerTestView extends View {
        private static final String MAIN = "reward_container";
        private static final String MOVE = "move_container";
        private final ZLayerGroup ui;
        private final ZLayer scene;
        private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ZLayerContainer reward;
        private ZLayerContainer moving;
        private Image rewardBackground;
        private Image rewardLabel;
        private RectF movingStart;
        private boolean built;
        private boolean alternate;
        private boolean movedRight;
        private int clicks;
        private String event = "Tap container background, badge, or MOVE";

        ContainerTestView(Context context) {
            super(context);
            ui = new ZLayerGroup(this);
            ui.setAutoInvalidate(false);
            scene = ui.addLayer("scene");
            setBackgroundColor(0xff0d121f);
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w <= 0 || built) return;
            built = true;
            buildScene(w);
        }

        private void buildScene(int viewWidth) {
            float margin = dp(28);
            RectF rewardBounds = new RectF(margin, dp(150), viewWidth - margin, dp(255));
            reward = scene.add(new ZLayerContainer.Builder(getContext(), MAIN, rewardBounds)
                    .setOnClickListener(id -> updateReward())
                    .setOnLongClickListener(id -> toggleRewardColor())
                    .setPressedScale(0.92f)
                    .setPressAnimationDuration(100L)
                    .setHapticAction(() -> performHapticFeedback(
                            android.view.HapticFeedbackConstants.KEYBOARD_TAP)));

            ZLayer background = reward.getContentLayer();
            ZLayer animation = reward.addLayer("animation");
            ZLayer content = reward.addLayer("content_overlay");

            rewardBackground = background.add(new Image.Builder(
                    getContext(), "reward_background",
                    roundedBitmap(800, 210, 0xff146c94, 0xff90e0ef, 30),
                    reward.getLocalBounds()));

            animation.add(new GifAnimator.Builder(
                    getContext(), "reward_gif", "carrom_pass_buy",
                    reward.pxRect(dp(5), dp(5), dp(42), dp(42)))
                    .setRepeatCount(AnimatorComponent.INFINITE));

            animation.add(new LottieAnimator.Builder(
                    getContext(), "reward_lottie", "win_animation",
                    reward.pxRect(rewardBounds.width() - dp(60), dp(10), dp(48), dp(48)))
                    .setRepeatCount(AnimatorComponent.INFINITE)
                    .setRepeatMode(RepeatMode.REVERSE));

            animation.add(new DynamicViewAnimator.Builder(
                    getContext(), "reward_pulse", new CustomDynamicView() {
                        @Override public void onDraw(Canvas canvas, float progress, RectF bounds) {
                            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            paint.setColor(0x99ffffff);
                            canvas.drawCircle(bounds.centerX(), bounds.centerY(),
                                    bounds.width() * (0.15f + 0.25f * progress), paint);
                        }
                        @Override public long getDurationMillis() { return 900L; }
                    }, reward.pxRect(rewardBounds.width() - dp(24), dp(75), dp(12), dp(12)))
                    .setRepeatCount(AnimatorComponent.INFINITE));

            animation.add(new AfterEffectAnimator.Builder(
                    getContext(), "reward_effect", createEffectComposition(),
                    reward.pxRect(0f, 0f, rewardBounds.width(), rewardBounds.height()))
                    .setRepeatCount(AnimatorComponent.INFINITE));

            content.add(new Image.Builder(getContext(), "reward_icon", iconBitmap(128),
                    reward.pxRect(dp(55), dp(22), dp(62), dp(62))));
            rewardLabel = content.add(new Image.Builder(getContext(), "reward_label",
                    textBitmap("TAP ME • 0", 520, 90, 58f, Color.WHITE),
                    reward.pxRect(dp(140), dp(27), rewardBounds.width() - dp(215), dp(50))));

            content.add(new Button.Builder(getContext(), "badge", 0xffffb703, "!",
                    reward.pxRect(rewardBounds.width() - dp(63), dp(12), dp(52), dp(52)))
                    .setCornerRadiusPx(dp(26))
                    .setOnClickListener(id -> {
                        event = "Nested Button intercepted the topmost touch";
                        invalidate();
                    }));

            movingStart = new RectF(margin, dp(340), margin + dp(190), dp(405));
            moving = scene.add(new ZLayerContainer.Builder(getContext(), MOVE, movingStart)
                    .setOnClickListener(id -> moveContainer())
                    .setPressedScale(0.92f));
            moving.getContentLayer().add(new Image.Builder(getContext(), "move_image",
                    labeledBitmap("MOVE", 420, 140, 0xff2a9d8f), moving.getLocalBounds()));

            scene.add(new Button.Builder(getContext(), "reset", 0xffe76f51, "RESET TEST",
                    new RectF(margin, dp(445), viewWidth - margin, dp(510)))
                    .setCornerRadiusPx(dp(18))
                    .setOnClickListener(id -> resetTest()));
        }

        private AfterEffectComposition createEffectComposition() {
            Bitmap dot = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(dot);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0x88ffffff);
            canvas.drawCircle(18, 18, 17, paint);
            Layer layer = new Layer(dot);
            KeyFrameAnimation motion = new KeyFrameAnimationBuilder(20f, 90f)
                    .setPosXInterpolator(Linear.get(20f, 760f))
                    .setAlphaInterpolator(Linear.get(20f, 100f))
                    .build();
            layer.addKeyFrameTimeLineDefinition(motion, 0L, 1400L);
            return new AfterEffectComposition.Builder(new Size(800f, 210f), 1400L)
                    .addLayer(layer).build();
        }

        private void updateReward() {
            clicks++;
            rewardLabel.setBitmap(textBitmap(
                    "TAP ME • " + clicks, 520, 90, 58f, Color.WHITE));
            event = "Container click: " + clicks;
            invalidate();
        }

        private void toggleRewardColor() {
            alternate = !alternate;
            rewardBackground.setBitmap(roundedBitmap(800, 210,
                    alternate ? 0xff7b2cbf : 0xff146c94, 0xff90e0ef, 30));
            event = "Container long-click changed the shared composition";
            invalidate();
        }

        private void moveContainer() {
            float targetLeft = movedRight
                    ? movingStart.left
                    : getWidth() - dp(28) - movingStart.width();
            movedRight = !movedRight;
            RectF target = new RectF(targetLeft, movingStart.top,
                    targetLeft + movingStart.width(), movingStart.bottom);
            moving.animateRegionTo(target, 650L, ZLayerContainer.Interpolator.EASE_IN_OUT,
                    () -> event = "Container and every nested child moved together");
        }

        private void resetTest() {
            clicks = 0; alternate = false; movedRight = false;
            rewardLabel.setBitmap(textBitmap("TAP ME • 0", 520, 90, 58f, Color.WHITE));
            rewardBackground.setBitmap(roundedBitmap(800, 210, 0xff146c94, 0xff90e0ef, 30));
            moving.setRegion(movingStart);
            event = "Test reset";
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            screenPaint.setTextAlign(Paint.Align.CENTER);
            screenPaint.setColor(Color.WHITE);
            screenPaint.setTextSize(dp(24));
            canvas.drawText("ZLayerContainer", getWidth() / 2f, dp(55), screenPaint);
            screenPaint.setColor(0xffa9bdd6);
            screenPaint.setTextSize(dp(14));
            canvas.drawText("Image • Button • GIF • Lottie • Dynamic • After Effects",
                    getWidth() / 2f, dp(85), screenPaint);
            screenPaint.setColor(0xff80ed99);
            canvas.drawText(event, getWidth() / 2f, dp(118), screenPaint);
            ui.draw(canvas);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }
        void release() { ui.release(); }

        private Bitmap roundedBitmap(int width, int height, int color, int stroke, float radius) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            RectF rect = new RectF(5, 5, width - 5, height - 5);
            paint.setColor(color); canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(10); paint.setColor(stroke);
            canvas.drawRoundRect(rect, radius, radius, paint);
            return bitmap;
        }
        private Bitmap iconBitmap(int size) {
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0xffffd166); canvas.drawCircle(size / 2f, size / 2f, size * .48f, paint);
            paint.setColor(0xff123047); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(size * .09f);
            canvas.drawCircle(size / 2f, size / 2f, size * .30f, paint);
            canvas.drawLine(size * .31f, size * .5f, size * .69f, size * .5f, paint);
            canvas.drawLine(size * .5f, size * .31f, size * .5f, size * .69f, paint);
            return bitmap;
        }
        private Bitmap textBitmap(String text, int width, int height, float size, int color) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color); paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true); paint.setTextSize(size);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText(text, width / 2f, height / 2f - (fm.ascent + fm.descent) / 2f, paint);
            return bitmap;
        }
        private Bitmap labeledBitmap(String text, int width, int height, int color) {
            Bitmap bitmap = roundedBitmap(width, height, color, Color.WHITE, 26);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true); paint.setTextSize(height * .34f);
            Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText(text, width / 2f, height / 2f - (fm.ascent + fm.descent) / 2f, paint);
            return bitmap;
        }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    }
}

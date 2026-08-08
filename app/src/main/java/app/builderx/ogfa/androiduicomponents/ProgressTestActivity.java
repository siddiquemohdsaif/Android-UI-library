package app.builderx.ogfa.androiduicomponents;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.progress.Progress;
import com.ogfa.nativeviews.progress.ProgressAsset;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/** Native determinate/indeterminate and GIF/Lottie Progress integration coverage. */
public final class ProgressTestActivity extends AppCompatActivity {
    private ProgressTestView testView;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        Progress.preload(
                this,
                ProgressAsset.gif("carrom_pass_buy"),
                ProgressAsset.lottie("win_animation")
        );
        testView = new ProgressTestView();
        setContentView(testView);
    }

    @Override protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private final class ProgressTestView extends View {
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer indicators = ui.addLayer("progress_indicators");
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean built;
        private String event = "Determinate target: 82%";

        ProgressTestView() {
            super(ProgressTestActivity.this);
            setBackgroundColor(0xff101522);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || built) return;
            built = true;

            Progress linear = indicators.add(new Progress.Builder(
                    getContext(), "linear_determinate",
                    position(140f, 400f), new Size(800f, 54f))
                    .setStyle(Progress.Style.LINEAR)
                    .setMode(Progress.Mode.DETERMINATE)
                    .setProgress(0.18f)
                    .setThickness(30f)
                    .setCornerRadius(15f)
                    .setOnProgressChangedListener((id, value) -> {
                        event = id + " = " + Math.round(value * 100f) + "%";
                        invalidate();
                    })
                    .setOnProgressCompleteListener(id -> {
                        event = id + " completed";
                        invalidate();
                    }));
            linear.animateProgressTo(0.82f, 1500L, Progress.Interpolator.EASE_IN_OUT);

            indicators.add(new Progress.Builder(
                    getContext(), "circular_indeterminate",
                    position(450f, 700f), new Size(180f, 180f))
                    .setStyle(Progress.Style.CIRCULAR)
                    .setMode(Progress.Mode.INDETERMINATE)
                    .setThickness(14f)
                    .setIndeterminateSweepAngle(110f)
                    .setIndeterminateDuration(900L)
                    .setStrokeCap(Progress.StrokeCap.ROUND));

            indicators.add(new Progress.Builder(
                    getContext(), "gif_progress",
                    ProgressAsset.gif("carrom_pass_buy"),
                    position(120f, 1080f), new Size(360f, 220f))
                    .setAssetPlayback(Progress.AssetPlayback.AUTO_PLAY)
                    .setContentScaleType(Image.ScaleType.FIT_CENTER)
                    .setSpeed(1f));

            indicators.add(new Progress.Builder(
                    getContext(), "lottie_progress",
                    ProgressAsset.lottie("win_animation"),
                    position(620f, 1080f), new Size(260f, 220f))
                    .setAssetPlayback(Progress.AssetPlayback.FOLLOW_PROGRESS)
                    .setProgress(0.58f)
                    .setContentScaleType(Image.ScaleType.FIT_CENTER));

            float barWidth = dp(250f);
            indicators.add(new Progress.Builder(
                    getContext(), "disabled_rect",
                    new RectF(
                            (width - barWidth) / 2f,
                            height - dp(115f),
                            (width + barWidth) / 2f,
                            height - dp(91f)))
                    .setStyle(Progress.Style.LINEAR)
                    .setProgressPercent(64f)
                    .setThicknessPx(dp(12f))
                    .setCornerRadiusPx(dp(6f))
                    .setDisabledAlpha(1f)
                    .setEnabled(false));
        }

        private Position position(float left, float top) {
            return new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    left,
                    top
            );
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setColor(Color.WHITE);
            labelPaint.setTextSize(dp(28f));
            labelPaint.setFakeBoldText(true);
            canvas.drawText("Progress", getWidth() / 2f, dp(60f), labelPaint);
            labelPaint.setFakeBoldText(false);
            labelPaint.setTextSize(dp(14f));
            labelPaint.setColor(0xff8ce99a);
            canvas.drawText(event, getWidth() / 2f, dp(96f), labelPaint);
            labelPaint.setColor(0xffb9c5d8);
            canvas.drawText("Native linear determinate", getWidth() / 2f, fx(350f), labelPaint);
            canvas.drawText("Native circular indeterminate", getWidth() / 2f, fx(650f), labelPaint);
            canvas.drawText("GIF auto-play", fx(300f), fx(1020f), labelPaint);
            canvas.drawText("Lottie follows 58%", fx(750f), fx(1020f), labelPaint);
            canvas.drawText("Disabled RectF + Px", getWidth() / 2f, getHeight() - dp(130f), labelPaint);
            ui.draw(canvas);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        void release() { ui.release(); }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
        private float fx(float value) { return value * getWidth() / 1080f; }
    }
}

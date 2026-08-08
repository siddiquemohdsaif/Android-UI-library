package app.builderx.ogfa.androiduicomponents;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.card.DropShadow;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.switchcomponent.Switch;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/** Manual coverage for tap, drag, region forms, styling, state, and cleanup. */
public final class SwitchTestActivity extends AppCompatActivity {
    private SwitchTestView testView;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        testView = new SwitchTestView();
        setContentView(testView);
    }

    @Override protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private final class SwitchTestView extends View {
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer controls = ui.addLayer("controls");
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean built;
        private int limitedToggleCount;
        private String event = "Tap or drag either enabled switch";

        SwitchTestView() {
            super(SwitchTestActivity.this);
            setBackgroundColor(0xff101522);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || built) return;
            built = true;

            Position top = new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    0f,
                    550f
            );
            controls.add(new Switch.Builder(
                    getContext(), "wifi", top, new Size(220f, 112f))
                    .horizontalCenter(true)
                    .setChecked(true)
                    .setThumbShadowEnabled(true)
                    .setHapticAction(() -> performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP))
                    .setOnCheckedChangeListener((id, checked, fromUser) -> {
                        event = id + " = " + checked + "  fromUser=" + fromUser;
                        invalidate();
                    }));

            Position middle = new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    0f,
                    1050f
            );
            controls.add(new Switch.Builder(
                    getContext(), "notifications", middle, new Size(300f, 128f))
                    .horizontalCenter(true)
                    .setCheckedTrackColor(0xff7c4dff)
                    .setUncheckedTrackColor(0xff465064)
                    .setThumbPadding(8f)
                    .setTrackStroke(4f, 0xffb39ddb)
                    .setThumbShadow(new DropShadow(
                            0f, 5f, 12f, 1f, Color.argb(55, 0, 0, 0)))
                    .setThumbShadowEnabled(true)
                    .setRippleEnabled(true)
                    .setRippleColor(0x44ffffff)
                    .setAnimationDuration(260L)
                    .setAnimationInterpolator(Switch.Interpolator.EASE_IN_OUT)
                    .setOnCheckedChangeListener((id, checked, fromUser) -> {
                        event = id + " = " + checked + "  fromUser=" + fromUser;
                        invalidate();
                    }));

            Position limitedPosition = new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    0f,
                    1450f
            );
            Switch limited = controls.add(new Switch.Builder(
                    getContext(), "three_toggle_limit", limitedPosition,
                    new Size(220f, 112f))
                    .horizontalCenter(true)
                    .setCheckedTrackColor(0xffff8a3d)
                    .setUncheckedTrackColor(0xff465064)
                    .setTrackStroke(3f, 0xffffb27d)
                    .setThumbShadowEnabled(true));
            limited.setDisabledCheckedTrackColor(0xff8d4a21)
                    .setDisabledUncheckedTrackColor(0xff374151)
                    .setDisabledCheckedThumbColor(0xffffd2b3)
                    .setDisabledUncheckedThumbColor(0xff9ca3af)
                    .setDisabledStrokeColor(0xffffb27d)
                    .setDisabledAlpha(1f)
                    .setDisabledThumbShadowEnabled(false);
            limited.setOnCheckedChangeListener((id, checked, fromUser) -> {
                if (!fromUser) return;
                limitedToggleCount++;
                if (limitedToggleCount >= 3) {
                    limited.setEnabled(false);
                    event = id + " disabled after 3 toggles";
                } else {
                    event = id + " toggle " + limitedToggleCount + "/3";
                }
                invalidate();
            });

            float switchWidth = dp(110f);
            float switchHeight = dp(56f);
            RectF runtimeBounds = new RectF(
                    (width - switchWidth) / 2f,
                    height - dp(190f),
                    (width + switchWidth) / 2f,
                    height - dp(190f) + switchHeight
            );
            controls.add(new Switch.Builder(
                    getContext(), "disabled", runtimeBounds)
                    .setChecked(true)
                    .setThumbPaddingPx(dp(4f))
                    .setTrackCornerRadiusPx(dp(28f))
                    .setDisabledCheckedTrackColor(0xff345995)
                    .setDisabledCheckedThumbColor(0xffb8d8ff)
                    .setDisabledAlpha(0.75f)
                    .setEnabled(false));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.WHITE);
            paint.setTextSize(dp(28f));
            paint.setFakeBoldText(true);
            canvas.drawText("Switch", getWidth() / 2f, dp(62f), paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(dp(15f));
            paint.setColor(0xff8ce99a);
            canvas.drawText(event, getWidth() / 2f, dp(100f), paint);
            paint.setColor(0xffb9c5d8);
            canvas.drawText("Default Figma region", getWidth() / 2f, dp(160f), paint);
            canvas.drawText("Custom style + ripple", getWidth() / 2f, dp(330f), paint);
            canvas.drawText("Disables after 3 toggles: " + limitedToggleCount + "/3",
                    getWidth() / 2f, dp(465f), paint);
            canvas.drawText("Disabled RectF + Px styling", getWidth() / 2f,
                    getHeight() - dp(215f), paint);
            ui.draw(canvas);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        void release() { ui.release(); }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    }
}

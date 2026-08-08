package app.builderx.ogfa.androiduicomponents;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.checkbox.CheckBox;
import com.ogfa.nativeviews.checkbox.CheckBoxImages;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import java.util.ArrayList;

/** Manual coverage for color/image, two/three-state, disabled, RectF, and cleanup. */
public final class CheckBoxTestActivity extends AppCompatActivity {
    private CheckBoxTestView testView;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        testView = new CheckBoxTestView();
        setContentView(testView);
    }

    @Override protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private final class CheckBoxTestView extends View {
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer controls = ui.addLayer("checkboxes");
        private final ArrayList<Bitmap> ownedBitmaps = new ArrayList<>();
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean built;
        private String event = "Tap a CheckBox";

        CheckBoxTestView() {
            super(CheckBoxTestActivity.this);
            setBackgroundColor(0xff101522);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || built) return;
            built = true;

            controls.add(new CheckBox.Builder(
                    getContext(), "two_state", position(430f), new Size(96f, 96f))
                    .horizontalCenter(true)
                    .setChecked(false)
                    .setRippleEnabled(true)
                    .setHapticAction(() -> performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP))
                    .setOnCheckedChangeListener((id, checked, fromUser) -> {
                        event = id + " = " + checked + "  fromUser=" + fromUser;
                        invalidate();
                    }));

            controls.add(new CheckBox.Builder(
                    getContext(), "three_state", position(850f), new Size(112f, 112f))
                    .horizontalCenter(true)
                    .setIndeterminateEnabled(true)
                    .setToggleOrder(
                            CheckBox.State.UNCHECKED,
                            CheckBox.State.CHECKED,
                            CheckBox.State.INDETERMINATE)
                    .setCheckedColor(0xff7c4dff)
                    .setIndeterminateColor(0xffff8a3d)
                    .setStrokeColor(0xffb39ddb)
                    .setStrokeWidth(4f)
                    .setCornerRadius(18f)
                    .setCheckMarkWidth(6f)
                    .setRippleEnabled(true)
                    .setOnStateChangeListener((id, value, fromUser) -> {
                        event = id + " = " + value + "  fromUser=" + fromUser;
                        invalidate();
                    }));

            CheckBoxImages imageStates = CheckBoxImages.create(
                    stateBitmap(CheckBox.State.UNCHECKED, false),
                    stateBitmap(CheckBox.State.CHECKED, false),
                    stateBitmap(CheckBox.State.INDETERMINATE, false),
                    stateBitmap(CheckBox.State.UNCHECKED, true),
                    stateBitmap(CheckBox.State.CHECKED, true),
                    stateBitmap(CheckBox.State.INDETERMINATE, true)
            );
            controls.add(new CheckBox.Builder(
                    getContext(), "image_state", imageStates,
                    position(1280f), new Size(128f, 128f))
                    .horizontalCenter(true)
                    .setIndeterminateEnabled(true)
                    .setImageScaleType(Image.ScaleType.FIT_CENTER)
                    .setImageTransition(CheckBox.ImageTransition.CROSS_FADE)
                    .setOnStateChangeListener((id, value, fromUser) -> {
                        event = id + " = " + value + "  bitmap";
                        invalidate();
                    }));

            controls.add(new CheckBox.Builder(
                    getContext(), "disabled_checked", position(1690f), new Size(96f, 96f))
                    .horizontalCenter(true)
                    .setChecked(true)
                    .setDisabledCheckedColor(0xff4b6388)
                    .setDisabledCheckMarkColor(0xffd8e7ff)
                    .setDisabledStrokeColor(0xff8294ae)
                    .setDisabledAlpha(1f)
                    .setEnabled(false));

            float side = dp(48f);
            controls.add(new CheckBox.Builder(
                    getContext(), "runtime_rect",
                    new RectF(dp(32f), height - dp(105f), dp(32f) + side, height - dp(57f)))
                    .setState(CheckBox.State.INDETERMINATE)
                    .setCornerRadiusPx(dp(8f))
                    .setStrokeWidthPx(dp(2f))
                    .setCheckMarkWidthPx(dp(3f))
                    .setPaddingPx(dp(6f)));
        }

        private Position position(float top) {
            return new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    0f,
                    top
            );
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setColor(Color.WHITE);
            labelPaint.setTextSize(dp(28f));
            labelPaint.setFakeBoldText(true);
            canvas.drawText("CheckBox", getWidth() / 2f, dp(60f), labelPaint);
            labelPaint.setFakeBoldText(false);
            labelPaint.setTextSize(dp(14f));
            labelPaint.setColor(0xff8ce99a);
            canvas.drawText(event, getWidth() / 2f, dp(96f), labelPaint);
            labelPaint.setColor(0xffb9c5d8);
            canvas.drawText("Two-state native color", getWidth() / 2f, dp(135f), labelPaint);
            canvas.drawText("Three-state native color", getWidth() / 2f, dp(285f), labelPaint);
            canvas.drawText("Six-state image rendering", getWidth() / 2f, dp(435f), labelPaint);
            canvas.drawText("Disabled checked appearance", getWidth() / 2f, dp(575f), labelPaint);
            labelPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("RectF + Px indeterminate", dp(92f), getHeight() - dp(74f), labelPaint);
            ui.draw(canvas);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        private Bitmap stateBitmap(CheckBox.State state, boolean disabled) {
            Bitmap bitmap = owned(Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888));
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            int fill = disabled ? 0xff596273
                    : state == CheckBox.State.INDETERMINATE ? 0xffff8a3d : 0xff00a6c8;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(state == CheckBox.State.UNCHECKED ? 0xff202a3a : fill);
            canvas.drawRoundRect(new RectF(8f, 8f, 172f, 172f), 34f, 34f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(12f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(disabled ? 0xffb8c1d0 : Color.WHITE);
            if (state == CheckBox.State.CHECKED) {
                Path path = new Path();
                path.moveTo(42f, 92f); path.lineTo(76f, 126f); path.lineTo(142f, 54f);
                canvas.drawPath(path, paint);
            } else if (state == CheckBox.State.INDETERMINATE) {
                canvas.drawLine(48f, 90f, 132f, 90f, paint);
            } else {
                paint.setColor(disabled ? 0xff8490a3 : 0xff91a0b7);
                canvas.drawRoundRect(new RectF(14f, 14f, 166f, 166f), 28f, 28f, paint);
            }
            return bitmap;
        }

        private Bitmap owned(Bitmap bitmap) { ownedBitmaps.add(bitmap); return bitmap; }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

        void release() {
            ui.release();
            for (Bitmap bitmap : ownedBitmaps) {
                if (!bitmap.isRecycled()) bitmap.recycle();
            }
            ownedBitmaps.clear();
        }
    }
}

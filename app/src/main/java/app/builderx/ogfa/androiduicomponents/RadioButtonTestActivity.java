package app.builderx.ogfa.androiduicomponents;

import android.graphics.Bitmap;
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

import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.radiobutton.RadioButton;
import com.ogfa.nativeviews.radiobutton.RadioButtonImages;
import com.ogfa.nativeviews.radiobutton.RadioSelection;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import java.util.ArrayList;

/** Manual coverage for grouped color/image selection, disabled state, and standalone use. */
public final class RadioButtonTestActivity extends AppCompatActivity {
    private RadioButtonTestView testView;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        testView = new RadioButtonTestView();
        setContentView(testView);
    }

    @Override protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private final class RadioButtonTestView extends View {
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer controls = ui.addLayer("radio_buttons");
        private final RadioSelection quality = new RadioSelection("quality")
                .setSelectionRequired(true)
                .setOnSelectionChangedListener((group, oldId, newId, fromUser) -> {
                    event = group + ": " + oldId + " → " + newId
                            + "  fromUser=" + fromUser;
                    invalidate();
                });
        private final RadioSelection theme = new RadioSelection("theme")
                .setSelectionRequired(true)
                .setOnSelectionChangedListener((group, oldId, newId, fromUser) -> {
                    event = group + ": " + oldId + " → " + newId
                            + "  fromUser=" + fromUser;
                    invalidate();
                });
        private final ArrayList<Bitmap> ownedBitmaps = new ArrayList<>();
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean built;
        private String event = "Select an option";

        RadioButtonTestView() {
            super(RadioButtonTestActivity.this);
            setBackgroundColor(0xff101522);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || built) return;
            built = true;

            addColorOption("quality_low", 170f, false, 0xff019cc4);
            addColorOption("quality_medium", 490f, true, 0xff7c4dff);
            addColorOption("quality_high", 810f, false, 0xffff8a3d);

            RadioButtonImages imageStates = RadioButtonImages.create(
                    imageState(false, false),
                    imageState(true, false),
                    imageState(false, true),
                    imageState(true, true)
            );
            controls.add(new RadioButton.Builder(
                    getContext(), "theme_light", imageStates,
                    position(280f, 1080f), new Size(132f, 132f))
                    .setSelection(theme)
                    .setChecked(true)
                    .setImageScaleType(Image.ScaleType.FIT_CENTER)
                    .setImageTransition(RadioButton.ImageTransition.CROSS_FADE));
            controls.add(new RadioButton.Builder(
                    getContext(), "theme_dark", imageStates,
                    position(670f, 1080f), new Size(132f, 132f))
                    .setSelection(theme)
                    .setImageScaleType(Image.ScaleType.FIT_CENTER)
                    .setImageTransition(RadioButton.ImageTransition.CROSS_FADE));

            controls.add(new RadioButton.Builder(
                    getContext(), "disabled_selected",
                    position(490f, 1570f), new Size(104f, 104f))
                    .setChecked(true)
                    .setDisabledCheckedColor(0xff526b91)
                    .setDisabledDotColor(0xffd7e6ff)
                    .setDisabledAlpha(1f)
                    .setEnabled(false));

            float side = dp(48f);
            controls.add(new RadioButton.Builder(
                    getContext(), "standalone_rect",
                    new RectF(dp(32f), height - dp(105f),
                            dp(32f) + side, height - dp(57f)))
                    .setRingWidthPx(dp(3f))
                    .setDotSizePx(dp(21f))
                    .setPaddingPx(dp(3f))
                    .setRippleEnabled(true)
                    .setOnClickListener(id -> {
                        event = id + " clicked";
                        invalidate();
                    }));
        }

        private void addColorOption(String id, float left, boolean checked, int color) {
            controls.add(new RadioButton.Builder(
                    getContext(), id, position(left, 520f), new Size(104f, 104f))
                    .setSelection(quality)
                    .setChecked(checked)
                    .setCheckedColor(color)
                    .setDotColor(color)
                    .setRingWidth(5f)
                    .setDotSize(46f)
                    .setPadding(4f)
                    .setRippleEnabled(true)
                    .setHapticAction(() -> performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP)));
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
            canvas.drawText("RadioButton", getWidth() / 2f, dp(60f), labelPaint);
            labelPaint.setFakeBoldText(false);
            labelPaint.setTextSize(dp(13f));
            labelPaint.setColor(0xff8ce99a);
            canvas.drawText(event, getWidth() / 2f, dp(96f), labelPaint);
            labelPaint.setTextSize(dp(15f));
            labelPaint.setColor(0xffb9c5d8);
            canvas.drawText("Required native-color selection", getWidth() / 2f, fx(390f), labelPaint);
            canvas.drawText("Low", fx(222f), fx(480f), labelPaint);
            canvas.drawText("Medium", fx(542f), fx(480f), labelPaint);
            canvas.drawText("High", fx(862f), fx(480f), labelPaint);
            canvas.drawText("Required complete-image selection", getWidth() / 2f, fx(990f), labelPaint);
            canvas.drawText("Disabled selected", getWidth() / 2f, fx(1480f), labelPaint);
            labelPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Standalone RectF + Px", dp(92f), getHeight() - dp(74f), labelPaint);
            ui.draw(canvas);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        private Bitmap imageState(boolean checked, boolean disabled) {
            Bitmap bitmap = owned(Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888));
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(disabled ? 0xff4f5868 : 0xff202b3d);
            canvas.drawCircle(90f, 90f, 82f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(14f);
            paint.setColor(disabled ? 0xff8793a6 : checked ? 0xff00b8d4 : 0xff9cacbf);
            canvas.drawCircle(90f, 90f, 68f, paint);
            if (checked) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(disabled ? 0xffb8c1cf : 0xff00b8d4);
                canvas.drawCircle(90f, 90f, 36f, paint);
            }
            return bitmap;
        }

        private Bitmap owned(Bitmap bitmap) { ownedBitmaps.add(bitmap); return bitmap; }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
        private float fx(float value) { return value * getWidth() / 1080f; }

        void release() {
            ui.release(); quality.release(); theme.release();
            for (Bitmap bitmap : ownedBitmaps) if (!bitmap.isRecycled()) bitmap.recycle();
            ownedBitmaps.clear();
        }
    }
}

package app.builderx.ogfa.androiduicomponents;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.switchcomponent.Switch;
import com.ogfa.nativeviews.switchcomponent.SwitchImages;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import java.util.ArrayList;

/** Visual and interaction coverage for complex and simple image rendering modes. */
public final class SwitchImageModeTestActivity extends AppCompatActivity {
    private ImageModeView testView;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        testView = new ImageModeView();
        setContentView(testView);
    }

    @Override protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private final class ImageModeView extends View {
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer controls = ui.addLayer("image_switches");
        private final ArrayList<Bitmap> ownedBitmaps = new ArrayList<>();
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean built;
        private String event = "Drag complex switches; tap the simple switch";

        ImageModeView() {
            super(SwitchImageModeTestActivity.this);
            setBackgroundColor(0xff0f1726);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || built) return;
            built = true;

            SwitchImages complex = SwitchImages.complex(
                    resourceBitmap(R.drawable.switch_complex_track_on),
                    resourceBitmap(R.drawable.switch_complex_track_off),
                    resourceBitmap(R.drawable.switch_complex_track_disabled),
                    resourceBitmap(R.drawable.switch_complex_thumb_enabled),
                    resourceBitmap(R.drawable.switch_complex_thumb_disabled)
            );
            SwitchImages simple = SwitchImages.simple(
                    fullSwitchTexture(true, false),
                    fullSwitchTexture(false, false),
                    fullSwitchTexture(true, true)
            );

            Switch complexSwitch = controls.add(new Switch.Builder(
                    getContext(), "complex_image",
                    complex, position(410f), new Size(254f, 50f))
                    .horizontalCenter(true)
                    .setChecked(true)
                    .setThumbPadding(0f)
                    .setTrackImageScaleType(Image.ScaleType.FIT_XY)
                    .setThumbImageScaleType(Image.ScaleType.FIT_CENTER)
                    .setOnCheckedChangeListener((id, checked, fromUser) -> {
                        event = "Complex: " + checked + "  fromUser=" + fromUser;
                        invalidate();
                    }));

            controls.add(new Switch.Builder(
                    getContext(), "simple_image",
                    simple, position(980f), new Size(420f, 160f))
                    .horizontalCenter(true)
                    .setImageTransition(Switch.ImageTransition.CROSS_FADE)
                    .setSwitchImageScaleType(Image.ScaleType.FIT_XY)
                    .setOnCheckedChangeListener((id, checked, fromUser) -> {
                        event = "Simple: " + checked + "  tap-only";
                        invalidate();
                    }));

            controls.add(new Switch.Builder(
                    getContext(), "complex_disabled",
                    complex, position(1510f), new Size(254f, 50f))
                    .horizontalCenter(true)
                    .setChecked(false)
                    .setThumbPadding(0f)
                    .setEnabled(false));

            controls.add(new Switch.Builder(
                    getContext(), "simple_disabled",
                    simple, position(1930f), new Size(420f, 160f))
                    .horizontalCenter(true)
                    .setChecked(true)
                    .setEnabled(false));
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
            labelPaint.setTextSize(dp(27f));
            labelPaint.setFakeBoldText(true);
            canvas.drawText("Switch Image Rendering", getWidth() / 2f, dp(60f), labelPaint);
            labelPaint.setFakeBoldText(false);
            labelPaint.setTextSize(dp(14f));
            labelPaint.setColor(0xff8ce99a);
            canvas.drawText(event, getWidth() / 2f, dp(98f), labelPaint);
            labelPaint.setColor(0xffb9c5d8);
            canvas.drawText("Complex: separate track + moving thumb", getWidth() / 2f, dp(132f), labelPaint);
            canvas.drawText("Simple: complete on/off cross-fade", getWidth() / 2f, dp(322f), labelPaint);
            canvas.drawText("Complex disabled assets", getWidth() / 2f, dp(498f), labelPaint);
            canvas.drawText("Simple disabled asset", getWidth() / 2f, dp(638f), labelPaint);
            ui.draw(canvas);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        private Bitmap trackTexture(int start, int end, String label) {
            Bitmap bitmap = owned(Bitmap.createBitmap(520, 200, Bitmap.Config.ARGB_8888));
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new LinearGradient(0f, 0f, 520f, 200f,
                    start, end, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(new RectF(2f, 2f, 518f, 198f), 98f, 98f, paint);
            paint.setShader(null);
            paint.setColor(0x33ffffff);
            paint.setStrokeWidth(10f);
            for (int x = -100; x < 620; x += 55) canvas.drawLine(x, 200f, x + 170f, 0f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(52f);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(label, 260f, 118f, paint);
            return bitmap;
        }

        private Bitmap fullSwitchTexture(boolean checked, boolean disabled) {
            int start = disabled ? 0xff555a64 : checked ? 0xff4a148c : 0xff263238;
            int end = disabled ? 0xff777c86 : checked ? 0xffab47bc : 0xff546e7a;
            Bitmap bitmap = trackTexture(start, end, disabled ? "DISABLED" : checked ? "ON" : "OFF");
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(disabled ? 0xffb9bdc5 : 0xfffff4c2);
            canvas.drawCircle(checked ? 420f : 100f, 100f, 78f, paint);
            return bitmap;
        }

        private Bitmap resourceBitmap(int drawableId) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableId);
            if (bitmap == null) {
                throw new IllegalStateException(
                        "Unable to decode complex switch drawable resource " + drawableId
                );
            }
            return owned(bitmap);
        }

        private Bitmap owned(Bitmap bitmap) { ownedBitmaps.add(bitmap); return bitmap; }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

        void release() {
            ui.release();
            for (Bitmap bitmap : ownedBitmaps) if (!bitmap.isRecycled()) bitmap.recycle();
            ownedBitmaps.clear();
        }
    }
}

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

import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/**
 * Standalone rendering and touch test for the Image component.
 */
public final class ImageTestActivity extends AppCompatActivity {

    private ImageTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new ImageTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private static final class ImageTestView extends View {

        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer images = ui.addLayer("images");
        private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Bitmap wideBitmap;
        private Bitmap alternateBitmap;
        private boolean initialized;
        private String status = "TAP AN IMAGE";
        private int clickCount;

        ImageTestView(Context context) {
            super(context);
            setBackgroundColor(0xff0d121f);
            setClickable(true);

            guidePaint.setStyle(Paint.Style.STROKE);
            guidePaint.setStrokeWidth(dp(1));
            guidePaint.setColor(0xff35556f);
            labelPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onSizeChanged(
                int width,
                int height,
                int oldWidth,
                int oldHeight
        ) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width <= 0 || height <= 0) return;
            if (!initialized || width != oldWidth || height != oldHeight) {
                createImages();
                initialized = true;
            }
        }

        private void createImages() {
            images.clear();
            wideBitmap = createTestBitmap(
                    800,
                    300,
                    0xff0077b6,
                    0xffffb703,
                    "WIDE BITMAP"
            );
            alternateBitmap = createTestBitmap(
                    800,
                    300,
                    0xff6a4c93,
                    0xff8ac926,
                    "NEW BITMAP"
            );

            Position topPosition = position(90f, 350f);
            Size imageSize = new Size(900f, 300f);
            Image top = images.add(new Image.Builder(
                    getContext(),
                    "fit_center",
                    wideBitmap,
                    topPosition,
                    imageSize
            )
                    .setScaleType(Image.ScaleType.FIT_CENTER)
                    .setOnClickListener(this::onImageClick));

            Position middlePosition = position(90f, 800f);
            images.add(new Image.Builder(
                    getContext(),
                    "center_crop",
                    wideBitmap,
                    middlePosition,
                    imageSize
            )
                    .setScaleType(Image.ScaleType.CENTER_CROP)
                    .setOnClickListener(this::onImageClick));

            RectF bottomBounds = position(90f, 1250f)
                    .toRectF(this, imageSize);
            images.add(new Image.Builder(
                    getContext(),
                    "fit_xy",
                    wideBitmap,
                    bottomBounds
            )
                    .setScaleType(Image.ScaleType.FIT_XY)
                    .setAlpha(0.72f)
                    .setOnClickListener(this::onImageClick));

            runRuntimeApiAssertions(top, topPosition, imageSize);
        }

        private void runRuntimeApiAssertions(
                Image image,
                Position position,
                Size size
        ) {
            if (!"fit_center".equals(image.getId())
                    || image.getBitmap() != wideBitmap
                    || image.getBounds().isEmpty()
                    || image.getScaleType() != Image.ScaleType.FIT_CENTER) {
                throw new AssertionError("Image getters returned invalid state.");
            }

            RectF originalBounds = image.getBounds();
            image.setBitmap(alternateBitmap);
            if (image.getBitmap() != alternateBitmap) {
                throw new AssertionError("Image bitmap was not replaced.");
            }
            image.setBitmap(wideBitmap);

            image.setRegion(new RectF(originalBounds));
            image.setRegion(position, size);
            image.setScaleType(Image.ScaleType.CENTER_CROP);
            image.setScaleType(Image.ScaleType.FIT_CENTER);
            image.setAlpha(0.7f).setAlpha(1f);

            image.setVisible(false).setVisible(true);
            image.setEnabled(false).setEnabled(true);
            image.removeOnClickListener();
            if (image.isClickable()) {
                throw new AssertionError("Image click listener was not removed.");
            }
            image.setOnClickListener(this::onImageClick);
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

        private void onImageClick(String id) {
            clickCount++;
            Image image = ui.findComponent(id, Image.class);
            if (image == null) {
                throw new AssertionError("Clicked Image was not found: " + id);
            }
            Image.ScaleType next;
            switch (image.getScaleType()) {
                case FIT_CENTER:
                    next = Image.ScaleType.CENTER_CROP;
                    break;
                case CENTER_CROP:
                    next = Image.ScaleType.FIT_XY;
                    break;
                case FIT_XY:
                default:
                    next = Image.ScaleType.FIT_CENTER;
                    break;
            }
            image.setScaleType(next);
            status = id + " CLICK " + clickCount + " -> " + next;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ui.draw(canvas);

            for (String id : new String[]{
                    "fit_center",
                    "center_crop",
                    "fit_xy"
            }) {
                Image image = ui.findComponent(id, Image.class);
                if (image != null) canvas.drawRect(image.getBounds(), guidePaint);
            }

            labelPaint.setColor(Color.WHITE);
            labelPaint.setTextSize(dp(22));
            labelPaint.setFakeBoldText(true);
            canvas.drawText("IMAGE COMPONENT", getWidth() / 2f, dp(48), labelPaint);

            labelPaint.setColor(0xff90e0ef);
            labelPaint.setTextSize(dp(13));
            canvas.drawText(status, getWidth() / 2f, dp(82), labelPaint);

            labelPaint.setFakeBoldText(false);
            labelPaint.setColor(0xffa9bdd6);
            labelPaint.setTextSize(dp(14));
            drawScaleLabel(canvas, "FIT_CENTER", "fit_center");
            drawScaleLabel(canvas, "CENTER_CROP", "center_crop");
            drawScaleLabel(canvas, "FIT_XY + ALPHA", "fit_xy");
        }

        private void drawScaleLabel(Canvas canvas, String label, String id) {
            Image image = ui.findComponent(id, Image.class);
            if (image == null) return;
            canvas.drawText(
                    label,
                    image.getBounds().centerX(),
                    image.getBounds().top - dp(9),
                    labelPaint
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        void release() {
            ui.release();
            if (wideBitmap != null && !wideBitmap.isRecycled()) {
                wideBitmap.recycle();
            }
            if (alternateBitmap != null && !alternateBitmap.isRecycled()) {
                alternateBitmap.recycle();
            }
        }

        private Bitmap createTestBitmap(
                int width,
                int height,
                int leftColor,
                int rightColor,
                String label
        ) {
            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(leftColor);
            canvas.drawRect(0f, 0f, width / 2f, height, paint);
            paint.setColor(rightColor);
            canvas.drawRect(width / 2f, 0f, width, height, paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(62f);
            paint.setFakeBoldText(true);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float baseline = height / 2f
                    - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(label, width / 2f, baseline, paint);
            return bitmap;
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}

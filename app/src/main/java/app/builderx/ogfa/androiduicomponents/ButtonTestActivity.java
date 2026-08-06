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

import com.ogfa.nativeviews.button.Button;
import com.ogfa.nativeviews.button.TextInsets;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/**
 * Visual and runtime API test for the composite Button component.
 */
public final class ButtonTestActivity extends AppCompatActivity {

    private ButtonTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new ButtonTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private static final class ButtonTestView extends View {

        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer buttons = ui.addLayer("buttons");
        private final Paint headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint boundsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Bitmap blueBackground;
        private Bitmap orangeBackground;
        private Bitmap iconBackground;
        private boolean initialized;
        private int clickCount;
        private String status = "TAP A BUTTON";

        ButtonTestView(Context context) {
            super(context);
            setBackgroundColor(0xff0d121f);
            setClickable(true);
            headingPaint.setTextAlign(Paint.Align.CENTER);
            boundsPaint.setStyle(Paint.Style.STROKE);
            boundsPaint.setStrokeWidth(dp(1));
            boundsPaint.setColor(0xff35556f);
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
                createButtons();
                initialized = true;
            }
        }

        private void createButtons() {
            buttons.clear();
            recycleBitmaps();
            verifyFigmaConfiguration();

            blueBackground = createBackground(900, 240, 0xff0057b8, 36f);
            orangeBackground = createBackground(900, 240, 0xffef7b24, 36f);
            iconBackground = createBackground(240, 240, 0xff6a4c93, 52f);

            Button play = buttons.add(new Button.Builder(
                    getContext(),
                    "play_button",
                    blueBackground,
                    "PLAY NOW",
                    position(90f, 320f),
                    new Size(900f, 240f)
            )
                    .setTextInsets(TextInsets.of(40f, 20f, 40f, 20f))
                    .setCornerRadius(60f)
                    .setTextSize(72f)
                    .setTextColor(Color.WHITE)
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.BOLD)
                    .setOnClickListener(this::onButtonClick));

            // Image-only overload. setLabel() later proves that Text can be
            // created lazily and remains privately owned by the Button.
            Button icon = buttons.add(new Button.Builder(
                    getContext(),
                    "icon_button",
                    iconBackground,
                    position(420f, 680f),
                    new Size(240f, 240f)
            )
                    .setImageScaleType(Image.ScaleType.FIT_XY)
                    .setCornerRadius(120f)
                    .setOnClickListener(this::onButtonClick));
            icon.setLabel("+")
                    .setTextSize(110f)
                    .setTextColor(Color.WHITE);

            RectF suppliedBounds = position(90f, 1040f)
                    .toRectF(this, new Size(900f, 240f));
            Image suppliedImage = new Image.Builder(
                    getContext(),
                    "supplied_background",
                    orangeBackground,
                    suppliedBounds
            )
                    .setScaleType(Image.ScaleType.FIT_XY)
                    .build(this);
            RectF suppliedTextBounds = new RectF(suppliedBounds);
            suppliedTextBounds.inset(dp(28), dp(8));
            Text suppliedText = new Text.Builder(
                    getContext(),
                    "supplied_label",
                    "SUPPLIED COMPONENTS",
                    suppliedTextBounds
            )
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setTextSizePx(dp(24))
                    .setFont(NativeFonts.MONTSERRAT)
                    .setFontVariations(FontVariation.SEMI_BOLD)
                    .build(this);
            buttons.add(new Button.Builder(
                    getContext(),
                    "supplied_button",
                    suppliedImage,
                    suppliedText
            )
                    .setCornerRadiusPx(dp(28))
                    .setOnClickListener(this::onButtonClick));

            RectF runtimeBounds = position(90f, 1400f)
                    .toRectF(this, new Size(900f, 240f));
            buttons.add(new Button.Builder(
                    getContext(),
                    "rect_button",
                    blueBackground,
                    "RECTF + 55% ALPHA",
                    runtimeBounds
            )
                    .setTextInsets(TextInsets.horizontal(dp(24)))
                    .setCornerRadiusPx(dp(24))
                    .setTextSizePx(dp(22))
                    .setAlpha(0.55f)
                    .setOnClickListener(this::onButtonClick));

            runRuntimeAssertions(play);
            verifyCornerRadiusScaling();
        }

        private void verifyFigmaConfiguration() {
            if (FigmaConfig.getDefault().getReferenceWidth() != 1080f) {
                throw new AssertionError(
                        "Application FigmaConfig was not installed."
                );
            }

            FigmaConfig custom = new FigmaConfig(540f);
            Position customPosition = new Position(
                    this,
                    custom,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    10f,
                    20f
            );
            float expectedScale = getWidth() / 540f;
            RectF resolved = customPosition.toRectF(
                    this,
                    new Size(100f, 50f)
            );
            if (customPosition.getFigmaConfig() != custom
                    || Math.abs(customPosition.getScale() - expectedScale)
                    > 0.0001f
                    || Math.abs(resolved.width() - 100f * expectedScale)
                    > 0.01f
                    || Math.abs(resolved.height() - 50f * expectedScale)
                    > 0.01f) {
                throw new AssertionError(
                        "Explicit FigmaConfig did not control Position scale."
                );
            }
        }

        private void verifyCornerRadiusScaling() {
            FigmaConfig custom = new FigmaConfig(540f);
            Position scaledPosition = new Position(
                    this,
                    custom,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    0f,
                    0f
            );
            Button scaled = new Button.Builder(
                    getContext(),
                    "radius_scaling_assertion",
                    blueBackground,
                    scaledPosition,
                    new Size(200f, 200f)
            )
                    .setCornerRadius(36f)
                    .build(this);

            float expected = 36f * getWidth() / 540f;
            if (Math.abs(scaled.getCornerRadius() - 36f) > 0.0001f
                    || Math.abs(scaled.getResolvedCornerRadius() - expected)
                    > 0.01f
                    || scaled.isCornerRadiusInPixels()) {
                throw new AssertionError(
                        "Figma corner radius was not scaled correctly."
                );
            }

            scaled.setRegion(new RectF(0f, 0f, 200f, 200f));
            if (Math.abs(scaled.getResolvedCornerRadius() - 36f) > 0.01f) {
                throw new AssertionError(
                        "RectF region did not resolve radius in runtime pixels."
                );
            }
            scaled.setRegion(
                    scaledPosition,
                    new Size(200f, 200f)
            );
            if (Math.abs(scaled.getResolvedCornerRadius() - expected) > 0.01f) {
                throw new AssertionError(
                        "Position region did not recalculate scaled radius."
                );
            }

            scaled.setCornerRadiusPx(36f);
            if (Math.abs(scaled.getResolvedCornerRadius() - 36f) > 0.01f
                    || !scaled.isCornerRadiusInPixels()) {
                throw new AssertionError(
                        "Runtime-pixel corner radius was unexpectedly scaled."
                );
            }
            scaled.release();
        }

        private void runRuntimeAssertions(Button button) {
            if (!button.hasText()
                    || button.getText() == null
                    || button.getImage() == null
                    || button.getBounds().isEmpty()
                    || button.getAlpha() != 1f
                    || !button.isClickable()) {
                throw new AssertionError("Button getters returned invalid state.");
            }

            RectF original = button.getBounds();
            button.setRegion(new RectF(original))
                    .setRegion(position(90f, 320f), new Size(900f, 240f))
                    .setTextInsets(TextInsets.of(40f, 20f, 40f, 20f))
                    .setImageScaleType(Image.ScaleType.FIT_XY)
                    .setFilterBitmap(true)
                    .setCornerRadius(60f)
                    .setAlpha(0.8f)
                    .setAlpha(1f)
                    .setVisible(false)
                    .setVisible(true)
                    .setEnabled(false)
                    .setEnabled(true);
            if (button.getCornerRadius() != 60f
                    || button.getResolvedCornerRadius() <= 0f
                    || button.isCornerRadiusInPixels()) {
                throw new AssertionError(
                        "Button corner-radius getters returned invalid state."
                );
            }

            int[] syntheticClicks = {0};
            button.setOnClickListener(id -> syntheticClicks[0]++);
            float centerX = button.getBounds().centerX();
            float centerY = button.getBounds().centerY();
            dispatch(button, MotionEvent.ACTION_DOWN, centerX, centerY);
            dispatch(button, MotionEvent.ACTION_UP, centerX, centerY);
            if (syntheticClicks[0] != 1) {
                throw new AssertionError("Button did not dispatch one valid click.");
            }

            dispatch(button, MotionEvent.ACTION_DOWN, centerX, centerY);
            dispatch(
                    button,
                    MotionEvent.ACTION_MOVE,
                    button.getBounds().right + 1f,
                    centerY
            );
            dispatch(button, MotionEvent.ACTION_UP, centerX, centerY);
            if (syntheticClicks[0] != 1) {
                throw new AssertionError(
                        "Button click was not cancelled after moving outside."
                );
            }
            button.setOnClickListener(this::onButtonClick);
        }

        private static void dispatch(
                Button button,
                int action,
                float x,
                float y
        ) {
            long now = android.os.SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(
                    now,
                    now,
                    action,
                    x,
                    y,
                    0
            );
            button.onTouchEvent(event);
            event.recycle();
        }

        private void onButtonClick(String id) {
            clickCount++;
            Button button = ui.findComponent(id, Button.class);
            if (button == null) {
                throw new AssertionError("Clicked Button was not found: " + id);
            }
            status = id + " CLICK " + clickCount;
            if (button.hasText()) {
                button.setLabel("CLICKED " + clickCount);
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ui.draw(canvas);

            for (String id : new String[]{
                    "play_button",
                    "icon_button",
                    "supplied_button",
                    "rect_button"
            }) {
                Button button = ui.findComponent(id, Button.class);
                if (button != null) canvas.drawRect(button.getBounds(), boundsPaint);
            }

            headingPaint.setColor(Color.WHITE);
            headingPaint.setTextSize(dp(22));
            headingPaint.setFakeBoldText(true);
            canvas.drawText(
                    "BUTTON COMPONENT",
                    getWidth() / 2f,
                    dp(48),
                    headingPaint
            );
            headingPaint.setColor(0xff90e0ef);
            headingPaint.setTextSize(dp(13));
            canvas.drawText(status, getWidth() / 2f, dp(82), headingPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        void release() {
            ui.release();
            recycleBitmaps();
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

        private Bitmap createBackground(
                int width,
                int height,
                int color,
                float radius
        ) {
            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            canvas.drawRoundRect(
                    new RectF(0f, 0f, width, height),
                    radius,
                    radius,
                    paint
            );
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8f);
            paint.setColor(0x66ffffff);
            canvas.drawRoundRect(
                    new RectF(4f, 4f, width - 4f, height - 4f),
                    radius,
                    radius,
                    paint
            );
            return bitmap;
        }

        private void recycleBitmaps() {
            recycle(blueBackground);
            recycle(orangeBackground);
            recycle(iconBackground);
            blueBackground = null;
            orangeBackground = null;
            iconBackground = null;
        }

        private static void recycle(Bitmap bitmap) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}

package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
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
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;
import com.ogfa.nativeviews.text.TextStyle;

/**
 * Standalone playground for the direct-Canvas Text component.
 *
 * <p>Launch with:
 * {@code adb shell am start -n
 * app.builderx.ogfa.androiduicomponents/.TextTestActivity}</p>
 */
public final class TextTestActivity extends AppCompatActivity {

    private TextTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new TextTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) {
            testView.release();
        }
        super.onDestroy();
    }

    public static final class TextTestView extends View {

        private static final String RUNTIME_TEXT_ID = "runtime_text";

        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer textLayer = ui.addLayer("text");
        private final Paint regionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private int updateCount;
        private boolean initialized;

        public TextTestView(Context context) {
            super(context);
            verifyBundledFonts(context);
            setBackgroundColor(0xff0d121f);
            setClickable(true);
            regionPaint.setStyle(Paint.Style.STROKE);
            regionPaint.setStrokeWidth(dp(1));
            regionPaint.setColor(0x5568d8ff);
        }

        private static void verifyBundledFonts(Context context) {
            int[] fonts = {
                    NativeFonts.INTER,
                    NativeFonts.INTER_ITALIC,
                    NativeFonts.MONTSERRAT,
                    NativeFonts.MONTSERRAT_ITALIC,
                    NativeFonts.ROBOTO,
                    NativeFonts.ROBOTO_ITALIC,
                    NativeFonts.LILITA_ONE
            };
            for (int font : fonts) {
                if (NativeFonts.load(context, font) == null) {
                    throw new AssertionError(
                            "Bundled font could not be loaded: " + font
                    );
                }
            }
        }

        @Override
        protected void onSizeChanged(
                int width,
                int height,
                int oldWidth,
                int oldHeight
        ) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width > 0 && height > 0
                    && (!initialized
                    || width != oldWidth
                    || height != oldHeight)) {
                createTexts();
                initialized = true;
            }
        }

        private void createTexts() {
            textLayer.clear();

            TextStyle titleStyle = new TextStyle.Builder()
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.BOLD)
                    .setTextSize(72f)
                    .setTextColor(Color.WHITE)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setShadow(4f, 0f, 3f, 0xaa000000)
                    .build();

            Position titlePosition = new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    54f,
                    90f
            );
            textLayer.add(new Text.Builder(
                    getContext(),
                    "title",
                    "NATIVE TEXT",
                    titlePosition,
                    new Size(972f, 140f)
            ).setStyle(titleStyle));

            Position bodyPosition = new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    90f,
                    330f
            );
            textLayer.add(new Text.Builder(
                    getContext(),
                    "body",
                    "This paragraph is rendered directly with Android "
                            + "StaticLayout. It checks wrapping, center alignment, "
                            + "Unicode shaping, and a maximum of four lines.",
                    bodyPosition,
                    new Size(900f, 420f)
            )
                    .setFont(NativeFonts.INTER_ITALIC)
                    .setFontVariations(FontVariation.BOLD)
                    .setTextSize(44f)
                    .setTextColor(0xffb9d8ef)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setLineSpacing(10f)
                    .setMaxLines(4)
                    .setOverflow(Text.Overflow.ELLIPSIZE_END)
                    .setPadding(24f, 18f));

            float margin = dp(24);
            float bottom = getHeight() - dp(48);
            RectF runtimeBounds = new RectF(
                    margin,
                    bottom - dp(96),
                    getWidth() - margin,
                    bottom
            );
            textLayer.add(new Text.Builder(
                    getContext(),
                    RUNTIME_TEXT_ID,
                    "RECTF REGION • TAP TO UPDATE",
                    runtimeBounds
            )
                    .setFont(NativeFonts.LILITA_ONE)
                    // Lilita One is not variable; this must safely render normally.
                    .setFontVariations(FontVariation.BOLD)
                    .setTextSizePx(dp(25))
                    .setTextColor(0xffffd166)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setWrapEnabled(false)
                    .setMaxLines(1)
                    .setOverflow(Text.Overflow.ELLIPSIZE_END)
                    .setPadding(dp(12), dp(8))
                    .setOnClickListener(id -> updateRuntimeText()));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawRegion(canvas, "title");
            drawRegion(canvas, "body");
            drawRegion(canvas, RUNTIME_TEXT_ID);
            ui.draw(canvas);
        }

        private void drawRegion(Canvas canvas, String id) {
            Text text = (Text) textLayer.find(id);
            if (text != null) {
                canvas.drawRect(text.getBounds(), regionPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean handled = ui.onTouchEvent(event);
            if (handled && event.getActionMasked() == MotionEvent.ACTION_UP) {
                performClick();
            }
            return handled || super.onTouchEvent(event);
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        private void updateRuntimeText() {
            updateCount++;
            Text runtimeText = (Text) textLayer.find(RUNTIME_TEXT_ID);
            if (runtimeText != null) {
                runtimeText
                        .setText("RUNTIME UPDATE " + updateCount)
                        .setTextColor(
                                updateCount % 2 == 0
                                        ? 0xffffd166
                                        : 0xff90e0ef
                        );
            }
        }

        public void release() {
            ui.release();
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}

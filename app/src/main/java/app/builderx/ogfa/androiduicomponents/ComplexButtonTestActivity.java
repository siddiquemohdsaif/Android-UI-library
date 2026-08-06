package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
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

import com.ogfa.nativeviews.animation.gif.GIFViewAnimator;
import com.ogfa.nativeviews.button.AnimatedButton;
import com.ogfa.nativeviews.button.AnimatedButtonGroup;
import com.ogfa.nativeviews.button.BitmapView;
import com.ogfa.nativeviews.button.GIFView;
import com.ogfa.nativeviews.button.ViewLayer;

import java.util.ArrayList;

/**
 * Standalone playground for AnimatedButton.
 *
 * Launch from Android Studio, or with:
 * adb shell am start -n app.builderx.ogfa.androiduicomponents/.ComplexButtonTestActivity
 */
public class ComplexButtonTestActivity extends AppCompatActivity {

    private ComplexButtonTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GIFViewAnimator.preloadAnimations(this, "carrom_pass_buy");
        testView = new ComplexButtonTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) {
            testView.release();
        }
        super.onDestroy();
    }

    /** Custom Canvas view that owns, draws, and dispatches touches to the buttons. */
    public static final class ComplexButtonTestView extends View
            implements AnimatedButton.OnClickListener,
            AnimatedButton.OnLongClickListener {

        private static final String COMPLEX_BUTTON = "complex_button";
        private static final String BADGE_BUTTON = "badge_button";
        private static final String MOVE_BUTTON = "move_button";
        private static final String RESET_BUTTON = "reset_button";
        private static final String GIF_BUTTON = "gif_button";

        private final AnimatedButtonGroup buttons;
        private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private BitmapView complexBackgroundLayer;
        private BitmapView complexLabelLayer;
        private RectF movingStartRect;

        private int clickCount;
        private int badgeClickCount;
        private boolean alternateColor;
        private boolean movedRight;
        private boolean initialized;
        private String eventMessage = "No event yet";

        public ComplexButtonTestView(Context context) {
            super(context);
            buttons = new AnimatedButtonGroup(this);
            setBackgroundColor(Color.rgb(13, 18, 31));
            setFocusable(true);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width > 0 && height > 0 && (!initialized || width != oldWidth)) {
                rebuildButtons(width);
                initialized = true;
            }
        }

        private void rebuildButtons(int viewWidth) {
            buttons.clear();

            float margin = dp(28);
            float mainTop = dp(150);
            float mainHeight = dp(92);
            RectF mainRect = new RectF(margin, mainTop, viewWidth - margin, mainTop + mainHeight);

            ArrayList<ViewLayer> mainLayers = new ArrayList<>();
            complexBackgroundLayer = BitmapView.get(
                    roundedBitmap(600, 180, alternateColor ? 0xff7b2cbf : 0xff146c94,
                            0xff90e0ef, 32), mainRect);
            mainLayers.add(complexBackgroundLayer);

            float iconSize = dp(54);
            RectF iconRect = new RectF(
                    mainRect.left + dp(18),
                    mainRect.centerY() - iconSize / 2f,
                    mainRect.left + dp(18) + iconSize,
                    mainRect.centerY() + iconSize / 2f);
            mainLayers.add(BitmapView.get(iconBitmap(128), iconRect));

            RectF labelRect = new RectF(
                    iconRect.right + dp(12), mainRect.top + dp(21),
                    mainRect.right - dp(52), mainRect.bottom - dp(21));
            complexLabelLayer = BitmapView.get(
                    textBitmap("TAP ME  •  " + clickCount, 520, 90, dp(20), Color.WHITE),
                    labelRect);
            mainLayers.add(complexLabelLayer);

            buttons.add(new AnimatedButton.Builder(
                    getContext(), COMPLEX_BUTTON, mainLayers, mainRect)
                    .setClickListener(this)
                    .setOnLongClickListener(this, true)
                    .setShrink(0.90f)
                    .setProxySoundPlay(this::performTestFeedback));

            // This separate button overlaps the main button and is added later. It demonstrates
            // that HandleTouch checks the visually topmost (last-added) button first.
            float badgeSize = dp(48);
            RectF badgeRect = new RectF(
                    mainRect.right - badgeSize - dp(8), mainRect.top + dp(8),
                    mainRect.right - dp(8), mainRect.top + dp(8) + badgeSize);
            ArrayList<ViewLayer> badgeLayers = new ArrayList<>();
            badgeLayers.add(BitmapView.get(
                    circleTextBitmap("!", 120, 0xffffb703, 0xff4a2c00), badgeRect));
            buttons.add(new AnimatedButton.Builder(
                    getContext(), BADGE_BUTTON, badgeLayers, badgeRect)
                    .setClickListener(this)
                    .setShrink(0.78f)
                    .setProxySoundPlay(this::performTestFeedback));

            float actionTop = mainRect.bottom + dp(76);
            float actionWidth = Math.min(dp(178), viewWidth - margin * 2);
            movingStartRect = new RectF(margin, actionTop, margin + actionWidth, actionTop + dp(60));
            ArrayList<ViewLayer> moveLayers = new ArrayList<>();
            moveLayers.add(BitmapView.get(
                    labeledButtonBitmap("MOVE", 420, 140, 0xff2a9d8f), movingStartRect));
            buttons.add(new AnimatedButton.Builder(
                    getContext(), MOVE_BUTTON, moveLayers, movingStartRect)
                    .setClickListener(this)
                    .setShrink(0.93f)
                    .setProxySoundPlay(this::performTestFeedback));

            RectF resetRect = new RectF(
                    margin, actionTop + dp(92), viewWidth - margin, actionTop + dp(152));
            ArrayList<ViewLayer> resetLayers = new ArrayList<>();
            resetLayers.add(BitmapView.get(
                    labeledButtonBitmap("RESET TEST", 700, 140, 0xffe76f51), resetRect));
            buttons.add(new AnimatedButton.Builder(
                    getContext(), RESET_BUTTON, resetLayers, resetRect)
                    .setClickListener(this)
                    .setShrink(0.96f));

            float gifWidth = viewWidth - margin * 2f;
            float gifHeight = gifWidth * (95f / 340f);
            RectF gifRect = new RectF(
                    margin,
                    resetRect.bottom + dp(28),
                    viewWidth - margin,
                    resetRect.bottom + dp(28) + gifHeight
            );

            ArrayList<ViewLayer> gifLayers = new ArrayList<>();
            if (GIFViewAnimator.isLoaded("carrom_pass_buy")) {
                gifLayers.add(GIFView.get("carrom_pass_buy", gifRect));
            } else {
                // Preload is asynchronous. This checks assets/gif once and fills the
                // same cache when layout happens before background preload completes.
                gifLayers.add(GIFView.get(
                        getContext(),
                        "carrom_pass_buy.gif",
                        gifRect
                ));
            }

            buttons.add(new AnimatedButton.Builder(
                    getContext(), GIF_BUTTON, gifLayers, gifRect)
                    .setClickListener(this)
                    .setShrink(0.96f)
                    .setProxySoundPlay(this::performTestFeedback));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawScreenText(canvas);
            buttons.draw(canvas);
        }

        private void drawScreenText(Canvas canvas) {
            screenPaint.setColor(Color.WHITE);
            screenPaint.setTextAlign(Paint.Align.CENTER);
            screenPaint.setFakeBoldText(true);
            screenPaint.setTextSize(dp(25));
            canvas.drawText("Complex Button Playground", getWidth() / 2f, dp(52), screenPaint);

            screenPaint.setFakeBoldText(false);
            screenPaint.setColor(0xffa9bdd6);
            screenPaint.setTextSize(dp(14));
            canvas.drawText("Tap main • hold 500 ms • tap yellow badge", getWidth() / 2f,
                    dp(82), screenPaint);
            canvas.drawText("MOVE tests ValueAnimator + hitbox updates", getWidth() / 2f,
                    dp(105), screenPaint);

            screenPaint.setColor(0xff80ed99);
            screenPaint.setTextSize(dp(15));
            canvas.drawText(eventMessage, getWidth() / 2f, dp(130), screenPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return buttons.onTouchEvent(event)
                    || super.onTouchEvent(event);
        }

        @Override
        public void onClick(String id) {
            switch (id) {
                case COMPLEX_BUTTON:
                    clickCount++;
                    complexLabelLayer.bitmap = textBitmap(
                            "TAP ME  •  " + clickCount, 520, 90, dp(20), Color.WHITE);
                    eventMessage = "Main click received: " + clickCount;
                    break;

                case BADGE_BUTTON:
                    badgeClickCount++;
                    eventMessage = "Top badge intercepted touch: " + badgeClickCount;
                    break;

                case MOVE_BUTTON:
                    animateMovingButton();
                    break;

                case RESET_BUTTON:
                    clickCount = 0;
                    badgeClickCount = 0;
                    alternateColor = false;
                    movedRight = false;
                    eventMessage = "State reset; buttons recreated";
                    rebuildButtons(getWidth());
                    break;

                case GIF_BUTTON:
                    eventMessage = "GIF layer is loaded, animating, and touchable";
                    break;

            }
            invalidate();
        }

        @Override
        public void onLongClick(String id) {
            if (!COMPLEX_BUTTON.equals(id)) {
                return;
            }
            alternateColor = !alternateColor;
            complexBackgroundLayer.bitmap = roundedBitmap(
                    600, 180, alternateColor ? 0xff7b2cbf : 0xff146c94,
                    0xff90e0ef, 32);
            eventMessage = "Long click released; background toggled";
            invalidate();
        }

        private void animateMovingButton() {
            float targetLeft = movedRight
                    ? movingStartRect.left
                    : getWidth() - dp(28) - movingStartRect.width();
            movedRight = !movedRight;
            eventMessage = "MOVE animation started; tap it at its new position";
            buttons.animateToPosition(
                    MOVE_BUTTON, targetLeft, movingStartRect.top, 650,
                    () -> eventMessage = "MOVE completed; hitbox moved with it");
        }

        private void performTestFeedback() {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }

        public void release() {
            buttons.release();
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }

        private Bitmap roundedBitmap(int width, int height, int color, int strokeColor,
                                     float radius) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            RectF rect = new RectF(5, 5, width - 5, height - 5);
            paint.setColor(color);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            paint.setColor(strokeColor);
            canvas.drawRoundRect(rect, radius, radius, paint);
            return bitmap;
        }

        private Bitmap iconBitmap(int size) {
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0xffffd166);
            canvas.drawCircle(size / 2f, size / 2f, size * 0.48f, paint);
            paint.setColor(0xff123047);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.09f);
            canvas.drawCircle(size / 2f, size / 2f, size * 0.30f, paint);
            canvas.drawLine(size * 0.31f, size * 0.50f, size * 0.69f, size * 0.50f, paint);
            canvas.drawLine(size * 0.50f, size * 0.31f, size * 0.50f, size * 0.69f, paint);
            return bitmap;
        }

        private Bitmap textBitmap(String text, int width, int height, float textSize,
                                  int textColor) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(textColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(textSize / getResources().getDisplayMetrics().density * 3.2f);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float baseline = height / 2f - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(text, width / 2f, baseline, paint);
            return bitmap;
        }

        private Bitmap circleTextBitmap(String text, int size, int color, int textColor) {
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            canvas.drawCircle(size / 2f, size / 2f, size * 0.47f, paint);
            paint.setColor(textColor);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(size * 0.60f);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            canvas.drawText(text, size / 2f,
                    size / 2f - (metrics.ascent + metrics.descent) / 2f, paint);
            return bitmap;
        }

        private Bitmap labeledButtonBitmap(String text, int width, int height, int color) {
            Bitmap bitmap = roundedBitmap(width, height, color, 0xffffffff, 26);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(true);
            paint.setTextSize(height * 0.34f);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            canvas.drawText(text, width / 2f,
                    height / 2f - (metrics.ascent + metrics.descent) / 2f, paint);
            return bitmap;
        }

    }
}

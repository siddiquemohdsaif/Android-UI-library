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
import com.ogfa.nativeviews.animator.component.CustomAnimatorComponent;
import com.ogfa.nativeviews.animator.component.CustomAnimatorComponentGroup;
import com.ogfa.nativeviews.animator.component.layer.BitmapLayer;
import com.ogfa.nativeviews.animator.component.layer.ComponentLayer;
import com.ogfa.nativeviews.animator.component.layer.GifLayer;

import java.util.ArrayList;

/**
 * Standalone playground for CustomAnimatorComponent.
 *
 * Launch from Android Studio, or with:
 * adb shell am start -n app.builderx.ogfa.androiduicomponents/.CustomAnimatorComponentTestActivity
 */
public class CustomAnimatorComponentTestActivity extends AppCompatActivity {

    private CustomAnimatorComponentTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GIFViewAnimator.preloadAnimations(this, "carrom_pass_buy");
        testView = new CustomAnimatorComponentTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) {
            testView.release();
        }
        super.onDestroy();
    }

    /** Custom Canvas view that owns, draws, and dispatches touches to the components. */
    public static final class CustomAnimatorComponentTestView extends View
            implements CustomAnimatorComponent.OnClickListener,
            CustomAnimatorComponent.OnLongClickListener {

        private static final String MAIN_COMPONENT = "complex_button";
        private static final String BADGE_COMPONENT = "badge_button";
        private static final String MOVE_COMPONENT = "move_button";
        private static final String RESET_COMPONENT = "reset_button";
        private static final String GIF_COMPONENT = "gif_button";

        private final CustomAnimatorComponentGroup components;
        private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private BitmapLayer complexBackgroundLayer;
        private BitmapLayer complexLabelLayer;
        private RectF movingStartRect;

        private int clickCount;
        private int badgeClickCount;
        private boolean alternateColor;
        private boolean movedRight;
        private boolean initialized;
        private String eventMessage = "No event yet";

        public CustomAnimatorComponentTestView(Context context) {
            super(context);
            components = new CustomAnimatorComponentGroup(this);
            setBackgroundColor(Color.rgb(13, 18, 31));
            setFocusable(true);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width > 0 && height > 0 && (!initialized || width != oldWidth)) {
                rebuildComponents(width);
                initialized = true;
            }
        }

        private void rebuildComponents(int viewWidth) {
            components.clear();

            float margin = dp(28);
            float mainTop = dp(150);
            float mainHeight = dp(92);
            RectF mainRect = new RectF(margin, mainTop, viewWidth - margin, mainTop + mainHeight);

            ArrayList<ComponentLayer> mainLayers = new ArrayList<>();
            complexBackgroundLayer = BitmapLayer.create(
                    roundedBitmap(600, 180, alternateColor ? 0xff7b2cbf : 0xff146c94,
                            0xff90e0ef, 32), mainRect);
            mainLayers.add(complexBackgroundLayer);

            float iconSize = dp(54);
            RectF iconRect = new RectF(
                    mainRect.left + dp(18),
                    mainRect.centerY() - iconSize / 2f,
                    mainRect.left + dp(18) + iconSize,
                    mainRect.centerY() + iconSize / 2f);
            mainLayers.add(BitmapLayer.create(iconBitmap(128), iconRect));

            RectF labelRect = new RectF(
                    iconRect.right + dp(12), mainRect.top + dp(21),
                    mainRect.right - dp(52), mainRect.bottom - dp(21));
            complexLabelLayer = BitmapLayer.create(
                    textBitmap("TAP ME  •  " + clickCount, 520, 90, dp(20), Color.WHITE),
                    labelRect);
            mainLayers.add(complexLabelLayer);

            components.add(new CustomAnimatorComponent.Builder(
                    getContext(), MAIN_COMPONENT, mainLayers, mainRect)
                    .setClickListener(this)
                    .setOnLongClickListener(this, true)
                    .setPressScale(0.90f)
                    .setSoundAction(this::performTestFeedback));

            // This separate component overlaps the main component and is added later. It demonstrates
            // that handleTouch checks the visually topmost (last-added) component first.
            float badgeSize = dp(48);
            RectF badgeRect = new RectF(
                    mainRect.right - badgeSize - dp(8), mainRect.top + dp(8),
                    mainRect.right - dp(8), mainRect.top + dp(8) + badgeSize);
            ArrayList<ComponentLayer> badgeLayers = new ArrayList<>();
            badgeLayers.add(BitmapLayer.create(
                    circleTextBitmap("!", 120, 0xffffb703, 0xff4a2c00), badgeRect));
            components.add(new CustomAnimatorComponent.Builder(
                    getContext(), BADGE_COMPONENT, badgeLayers, badgeRect)
                    .setClickListener(this)
                    .setPressScale(0.78f)
                    .setSoundAction(this::performTestFeedback));

            float actionTop = mainRect.bottom + dp(76);
            float actionWidth = Math.min(dp(178), viewWidth - margin * 2);
            movingStartRect = new RectF(margin, actionTop, margin + actionWidth, actionTop + dp(60));
            ArrayList<ComponentLayer> moveLayers = new ArrayList<>();
            moveLayers.add(BitmapLayer.create(
                    labeledComponentBitmap("MOVE", 420, 140, 0xff2a9d8f), movingStartRect));
            components.add(new CustomAnimatorComponent.Builder(
                    getContext(), MOVE_COMPONENT, moveLayers, movingStartRect)
                    .setClickListener(this)
                    .setPressScale(0.93f)
                    .setSoundAction(this::performTestFeedback));

            RectF resetRect = new RectF(
                    margin, actionTop + dp(92), viewWidth - margin, actionTop + dp(152));
            ArrayList<ComponentLayer> resetLayers = new ArrayList<>();
            resetLayers.add(BitmapLayer.create(
                    labeledComponentBitmap("RESET TEST", 700, 140, 0xffe76f51), resetRect));
            components.add(new CustomAnimatorComponent.Builder(
                    getContext(), RESET_COMPONENT, resetLayers, resetRect)
                    .setClickListener(this)
                    .setPressScale(0.96f));

            float gifWidth = viewWidth - margin * 2f;
            float gifHeight = gifWidth * (95f / 340f);
            RectF gifRect = new RectF(
                    margin,
                    resetRect.bottom + dp(28),
                    viewWidth - margin,
                    resetRect.bottom + dp(28) + gifHeight
            );

            ArrayList<ComponentLayer> gifLayers = new ArrayList<>();
            if (GIFViewAnimator.isLoaded("carrom_pass_buy")) {
                gifLayers.add(GifLayer.create("carrom_pass_buy", gifRect));
            } else {
                // Preload is asynchronous. This checks assets/gif once and fills the
                // same cache when layout happens before background preload completes.
                gifLayers.add(GifLayer.create(
                        getContext(),
                        "carrom_pass_buy.gif",
                        gifRect
                ));
            }

            components.add(new CustomAnimatorComponent.Builder(
                    getContext(), GIF_COMPONENT, gifLayers, gifRect)
                    .setClickListener(this)
                    .setPressScale(0.96f)
                    .setSoundAction(this::performTestFeedback));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawScreenText(canvas);
            components.draw(canvas);
        }

        private void drawScreenText(Canvas canvas) {
            screenPaint.setColor(Color.WHITE);
            screenPaint.setTextAlign(Paint.Align.CENTER);
            screenPaint.setFakeBoldText(true);
            screenPaint.setTextSize(dp(25));
            canvas.drawText("Custom Animator Component", getWidth() / 2f, dp(52), screenPaint);

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
            return components.onTouchEvent(event)
                    || super.onTouchEvent(event);
        }

        @Override
        public void onClick(String id) {
            switch (id) {
                case MAIN_COMPONENT:
                    clickCount++;
                    complexLabelLayer.bitmap = textBitmap(
                            "TAP ME  •  " + clickCount, 520, 90, dp(20), Color.WHITE);
                    eventMessage = "Main click received: " + clickCount;
                    break;

                case BADGE_COMPONENT:
                    badgeClickCount++;
                    eventMessage = "Top badge intercepted touch: " + badgeClickCount;
                    break;

                case MOVE_COMPONENT:
                    animateMovingComponent();
                    break;

                case RESET_COMPONENT:
                    clickCount = 0;
                    badgeClickCount = 0;
                    alternateColor = false;
                    movedRight = false;
                    eventMessage = "State reset; components recreated";
                    rebuildComponents(getWidth());
                    break;

                case GIF_COMPONENT:
                    eventMessage = "GIF layer is loaded, animating, and touchable";
                    break;

            }
            invalidate();
        }

        @Override
        public void onLongClick(String id) {
            if (!MAIN_COMPONENT.equals(id)) {
                return;
            }
            alternateColor = !alternateColor;
            complexBackgroundLayer.bitmap = roundedBitmap(
                    600, 180, alternateColor ? 0xff7b2cbf : 0xff146c94,
                    0xff90e0ef, 32);
            eventMessage = "Long click released; background toggled";
            invalidate();
        }

        private void animateMovingComponent() {
            float targetLeft = movedRight
                    ? movingStartRect.left
                    : getWidth() - dp(28) - movingStartRect.width();
            movedRight = !movedRight;
            eventMessage = "MOVE animation started; tap it at its new position";
            components.animateToPosition(
                    MOVE_COMPONENT, targetLeft, movingStartRect.top, 650,
                    () -> eventMessage = "MOVE completed; hitbox moved with it");
        }

        private void performTestFeedback() {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }

        public void release() {
            components.release();
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

        private Bitmap labeledComponentBitmap(String text, int width, int height, int color) {
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

package app.builderx.ogfa.androiduicomponents;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import java.util.List;

/**
 * Standalone playground for TextField.
 *
 * Launch with:
 * adb shell am start -n
 * app.builderx.ogfa.androiduicomponents/.TextFieldTestActivity
 */
public final class TextFieldTestActivity extends AppCompatActivity {

    private TextFieldTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new TextFieldTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) {
            testView.release();
        }
        super.onDestroy();
    }

    /** Canvas host responsible for drawing and forwarding input to the text fields. */
    public static final class TextFieldTestView extends View {

        private static final String TOP_FIELD = "top_field";
        private static final String MIDDLE_FIELD = "middle_field";
        private static final String BOTTOM_FIELD = "bottom_field";
        private static final long PAN_ANIMATION_DURATION_MS = 220L;

        private final ZLayerGroup ui;
        private final ZLayer fieldLayer;
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect visibleWindow = new Rect();
        private final int[] locationOnScreen = new int[2];
        private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener =
                this::updateImeInsetFromVisibleWindow;

        private boolean initialized;
        private int imeInsetBottom;
        private float canvasTranslationY;
        private float panTargetY;
        private ValueAnimator panAnimator;
        private String eventMessage =
                "Tap or drag in any field; NEXT changes focus";

        public TextFieldTestView(Context context) {
            super(context);
            ui = new ZLayerGroup(this);
            fieldLayer = ui.addLayer("fields");
            setBackgroundColor(0xff0d121f);
            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
            getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
            setOnApplyWindowInsetsListener((view, insets) -> {
                updateImeInsetFromWindowInsets(insets);
                return insets;
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setWindowInsetsAnimationCallback(
                        new WindowInsetsAnimation.Callback(
                                WindowInsetsAnimation.Callback
                                        .DISPATCH_MODE_CONTINUE_ON_SUBTREE
                        ) {
                            @Override
                            public WindowInsets onProgress(
                                    WindowInsets insets,
                                    List<WindowInsetsAnimation> runningAnimations
                            ) {
                                updateImeInsetFromWindowInsets(insets);
                                return insets;
                            }
                        }
                );
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            requestApplyInsets();
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            if (width > 0 && height > 0 && (!initialized || width != oldWidth)) {
                createTextFields();
                initialized = true;
            }
        }

        private void createTextFields() {
            fieldLayer.clear();

            float margin = dp(40);
            float fieldHeight = dp(58);
            float right = getWidth() - margin;

            addTestField(
                    TOP_FIELD,
                    "Top field",
                    "Top",
                    new RectF(
                            margin,
                            dp(150),
                            right,
                            dp(150) + fieldHeight
                    ),
                    EditorInfo.IME_ACTION_NEXT
            );

            float middleTop = (getHeight() - fieldHeight) / 2f;
            addTestField(
                    MIDDLE_FIELD,
                    "Middle field",
                    "Middle",
                    new RectF(
                            margin,
                            middleTop,
                            right,
                            middleTop + fieldHeight
                    ),
                    EditorInfo.IME_ACTION_NEXT
            );

            float bottom = getHeight() - dp(70);
            addTestField(
                    BOTTOM_FIELD,
                    "Bottom field",
                    "Bottom",
                    new RectF(
                            margin,
                            bottom - fieldHeight,
                            right,
                            bottom
                    ),
                    EditorInfo.IME_ACTION_DONE
            );
        }

        private void addTestField(
                String id,
                String hint,
                String initialText,
                RectF bounds,
                int imeAction
        ) {
            fieldLayer.add(new TextField.Builder(
                    getContext(),
                    id,
                    bounds
            )
                    .setHint(hint)
                    .setText(initialText)
                    .setMaxLength(40)
                    .setInputType(
                            InputType.TYPE_CLASS_TEXT
                                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    )
                    .setImeOptions(imeAction)
                    .setTextColor(Color.WHITE)
                    .setHintColor(0x88ffffff)
                    .setCursorColor(0xffffd166)
                    .setSelectionColor(0x6690e0ef)
                    .setBackgroundColor(0xff182840, 0xff203a5c)
                    .setStrokeColor(0xff54708f, 0xff90e0ef)
                    .setTextSize(dp(22))
                    .setPadding(dp(14), dp(6))
                    .setCornerRadius(dp(12))
                    .setStrokeWidth(dp(2))
                    .setOnFocusChangedListener((fieldId, focused) -> {
                        if (focused) {
                            eventMessage = "Focused: " + fieldId;
                        }
                        post(this::updateCanvasTranslation);
                        invalidate();
                    })
                    .setOnTextChangedListener((fieldId, text) -> {
                        eventMessage = fieldId + ": " + text;
                        invalidate();
                    })
                    .setOnEditorActionListener((fieldId, action) -> {
                        TextField field = (TextField) fieldLayer.find(fieldId);
                        eventMessage = "Submitted: "
                                + (field == null ? "" : field.getText());
                        invalidate();
                        return false;
                    }));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int saveCount = canvas.save();
            canvas.translate(0f, canvasTranslationY);
            drawLabels(canvas);
            ui.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        private void drawLabels(Canvas canvas) {
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setColor(Color.WHITE);
            labelPaint.setFakeBoldText(true);
            labelPaint.setTextSize(dp(28));
            canvas.drawText(
                    "TextField Test",
                    getWidth() / 2f,
                    dp(72),
                    labelPaint
            );

            labelPaint.setFakeBoldText(false);
            labelPaint.setColor(0xffa9bdd6);
            labelPaint.setTextSize(dp(15));
            canvas.drawText(
                    eventMessage,
                    getWidth() / 2f,
                    dp(112),
                    labelPaint
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            MotionEvent translatedEvent = MotionEvent.obtain(event);
            translatedEvent.offsetLocation(0f, -canvasTranslationY);
            boolean handled = ui.onTouchEvent(translatedEvent);
            translatedEvent.recycle();
            return handled || super.onTouchEvent(event);
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return ui.onCheckIsTextEditor();
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection inputConnection =
                    ui.onCreateInputConnection(outAttrs);
            return inputConnection != null
                    ? inputConnection
                    : super.onCreateInputConnection(outAttrs);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return ui.onKeyDown(keyCode, event)
                    || super.onKeyDown(keyCode, event);
        }

        private void updateImeInsetFromWindowInsets(WindowInsets insets) {
            int inset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                inset = insets.getInsets(WindowInsets.Type.ime()).bottom;
            } else {
                int systemBottom = insets.getSystemWindowInsetBottom();
                int stableBottom = insets.getStableInsetBottom();
                inset = Math.max(0, systemBottom - stableBottom);
            }
            setImeInsetBottom(inset);
        }

        private void updateImeInsetFromVisibleWindow() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsets rootInsets = getRootWindowInsets();
                if (rootInsets != null) {
                    updateImeInsetFromWindowInsets(rootInsets);
                }
                return;
            }
            getWindowVisibleDisplayFrame(visibleWindow);
            getLocationOnScreen(locationOnScreen);
            int visibleBottomInView = visibleWindow.bottom - locationOnScreen[1];
            int obscuredHeight = Math.max(0, getHeight() - visibleBottomInView);
            int detectedInset = obscuredHeight > getHeight() * 0.15f
                    ? obscuredHeight
                    : 0;
            setImeInsetBottom(detectedInset);
        }

        private void setImeInsetBottom(int insetBottom) {
            int safeInset = Math.max(0, insetBottom);
            if (imeInsetBottom == safeInset) {
                return;
            }
            imeInsetBottom = safeInset;
            post(this::updateCanvasTranslation);
        }

        private void updateCanvasTranslation() {
            TextField focusedField = ui.getFocusedTextField();
            float target = 0f;
            if (focusedField != null && imeInsetBottom > 0) {
                RectF fieldBounds = focusedField.getBounds();
                float visibleBottom = getHeight() - imeInsetBottom - dp(24);
                target = Math.min(0f, visibleBottom - fieldBounds.bottom);
                target = Math.max(target, dp(24) - fieldBounds.top);
                eventMessage = "Focused: " + focusedField.getId()
                        + "  |  pan " + Math.round(target)
                        + " px";
            }
            animateCanvasTranslation(target);
        }

        private void animateCanvasTranslation(float target) {
            if (Math.abs(panTargetY - target) < 0.5f
                    && (panAnimator == null || panAnimator.isRunning())) {
                return;
            }
            panTargetY = target;
            if (panAnimator != null) {
                panAnimator.cancel();
            }
            if (Math.abs(canvasTranslationY - target) < 0.5f) {
                canvasTranslationY = target;
                invalidate();
                return;
            }
            panAnimator = ValueAnimator.ofFloat(canvasTranslationY, target);
            panAnimator.setDuration(PAN_ANIMATION_DURATION_MS);
            panAnimator.setInterpolator(new DecelerateInterpolator());
            panAnimator.addUpdateListener(animator -> {
                canvasTranslationY = (float) animator.getAnimatedValue();
                invalidate();
            });
            panAnimator.start();
        }

        public void release() {
            if (getViewTreeObserver().isAlive()) {
                getViewTreeObserver().removeOnGlobalLayoutListener(
                        globalLayoutListener
                );
            }
            if (panAnimator != null) {
                panAnimator.cancel();
                panAnimator = null;
            }
            ui.release();
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}

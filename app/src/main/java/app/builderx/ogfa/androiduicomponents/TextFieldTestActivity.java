package app.builderx.ogfa.androiduicomponents;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
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

import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.text.FontVariation;
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
        private static final String COMPOSER_FIELD = "message_composer";
        private static final int COMPOSER_MAX_VISIBLE_LINES = 5;
        private static final long PAN_ANIMATION_DURATION_MS = 220L;

        private final ZLayerGroup ui;
        private final ZLayer fieldLayer;
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint composerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path composerIconPath = new Path();
        private final RectF composerPanelBounds = new RectF();
        private final RectF composerFieldBounds = new RectF();
        private final RectF composerAddBounds = new RectF();
        private final RectF composerActionBounds = new RectF();
        private final Rect visibleWindow = new Rect();
        private final int[] locationOnScreen = new int[2];
        private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener =
                this::updateImeInsetFromVisibleWindow;

        private boolean initialized;
        private int imeInsetBottom;
        private float canvasTranslationY;
        private float panTargetY;
        private ValueAnimator panAnimator;
        private TextField composerField;
        private int composerLineCount = 1;
        private boolean composerAddGesture;
        private boolean composerActionGesture;
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
            removeCallbacks(composerGeometryUpdate);
            composerField = null;
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
                    "",
                    new RectF(
                            margin,
                            bottom - fieldHeight,
                            right,
                            bottom
                    ),
                    EditorInfo.IME_ACTION_DONE
            );

            addMessageComposer();
        }

        /** Mirrors the adaptive message field used by NativeGenieView. */
        private void addMessageComposer() {
            composerLineCount = 1;
            updateComposerBounds(1);
            composerField = fieldLayer.add(new TextField.Builder(
                    getContext(),
                    COMPOSER_FIELD,
                    new RectF(composerFieldBounds)
            )
                    .setHint("Message Genie")
                    .setMaxLength(500)
                    .setInputType(
                            InputType.TYPE_CLASS_TEXT
                                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    )
                    .setImeOptions(
                            EditorInfo.IME_ACTION_SEND
                                    | EditorInfo.IME_FLAG_NO_EXTRACT_UI
                                    | EditorInfo.IME_FLAG_NO_FULLSCREEN
                    )
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.REGULAR)
                    .setTextSizePx(dp(18))
                    .setTextColor(Color.WHITE)
                    .setHintColor(0xff9d9da6)
                    .setCursorColor(0xff22b573)
                    .setSelectionColor(0x5522b573)
                    .setBackgroundColor(Color.TRANSPARENT, Color.TRANSPARENT)
                    .setStrokeColor(Color.TRANSPARENT, Color.TRANSPARENT)
                    .setStrokeWidthPx(0f)
                    .setPaddingPx(0f, 0f)
                    .setOnFocusChangedListener((fieldId, focused) -> {
                        if (focused) eventMessage = "Focused: Genie composer";
                        post(this::updateCanvasTranslation);
                        invalidate();
                    })
                    .setOnTextChangedListener((fieldId, text) -> {
                        eventMessage = text.trim().isEmpty()
                                ? "Genie composer is empty"
                                : "Composer: " + text;
                        removeCallbacks(composerGeometryUpdate);
                        post(composerGeometryUpdate);
                        invalidate();
                    })
                    .setOnEditorActionListener((fieldId, action) -> {
                        if (action != EditorInfo.IME_ACTION_SEND) return false;
                        submitComposer();
                        return true;
                    }));
        }

        private final Runnable composerGeometryUpdate = () -> {
            TextField field = composerField;
            if (field == null) return;
            int lines = calculateComposerLines(field.getText());
            if (lines != composerLineCount) {
                composerLineCount = lines;
                updateComposerBounds(lines);
                field.setRegion(new RectF(composerFieldBounds));
                post(this::updateCanvasTranslation);
            }
            invalidate();
        };

        private int calculateComposerLines(String text) {
            if (text == null || text.isEmpty() || getWidth() <= 0) return 1;
            if (measureComposerLines(text, composerSingleLineWidth()) <= 1) return 1;
            return Math.max(2, Math.min(
                    COMPOSER_MAX_VISIBLE_LINES,
                    measureComposerLines(text, composerExpandedWidth())
            ));
        }

        private int measureComposerLines(String text, float width) {
            TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(dp(18));
            TextField field = composerField;
            if (field != null) paint.setTypeface(field.getTypeface());
            StaticLayout layout = StaticLayout.Builder.obtain(
                            text, 0, text.length(), paint,
                            Math.max(1, (int) Math.floor(width)))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .build();
            return Math.max(1, layout.getLineCount());
        }

        private void updateComposerBounds(int lines) {
            float panelLeft = dp(24);
            float panelRight = getWidth() - dp(24);
            float panelBottom = getHeight() - dp(155);
            float controlSize = dp(44);
            float controlBottom = panelBottom - dp(14);
            float controlTop = controlBottom - controlSize;

            composerAddBounds.set(
                    panelLeft + dp(12), controlTop,
                    panelLeft + dp(12) + controlSize, controlBottom
            );
            composerActionBounds.set(
                    panelRight - dp(12) - controlSize, controlTop,
                    panelRight - dp(12), controlBottom
            );

            if (lines <= 1) {
                float panelTop = panelBottom - dp(72);
                composerPanelBounds.set(panelLeft, panelTop, panelRight, panelBottom);
                composerFieldBounds.set(
                        panelLeft + dp(66), panelTop + dp(9),
                        panelRight - dp(66), panelBottom - dp(9)
                );
                return;
            }

            float textHeight = composerTextHeight(lines);
            float textBottom = panelBottom - dp(66);
            float textTop = textBottom - textHeight;
            composerPanelBounds.set(
                    panelLeft, textTop - dp(12), panelRight, panelBottom
            );
            composerFieldBounds.set(
                    panelLeft + dp(20), textTop,
                    panelRight - dp(20), textBottom
            );
        }

        private float composerTextHeight(int lines) {
            TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(dp(18));
            TextField field = composerField;
            if (field != null) paint.setTypeface(field.getTypeface());
            StringBuilder probe = new StringBuilder("Ag");
            for (int line = 1; line < lines; line++) probe.append("\nAg");
            StaticLayout layout = StaticLayout.Builder.obtain(
                            probe, 0, probe.length(), paint,
                            Math.max(1, (int) composerExpandedWidth()))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .build();
            return layout.getHeight() + dp(2);
        }

        private float composerSingleLineWidth() {
            return Math.max(1f, composerPanelBounds.width() - dp(132));
        }

        private float composerExpandedWidth() {
            return Math.max(1f, getWidth() - dp(88));
        }

        private void addTestField(
                String id,
                String hint,
                String initialText,
                RectF bounds,
                int imeAction
        ) {
            TextField field = fieldLayer.add(new TextField.Builder(
                    getContext(),
                    id,
                    bounds
            )
                    .setFont(NativeFonts.INTER_ITALIC)
                    .setFontVariations(FontVariation.BOLD)
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
                    .setTextSize(66f)
                    .setPadding(42f, 18f)
                    .setCornerRadius(36f)
                    .setStrokeWidth(6f)
                    .horizontalCenter(false)
                    .verticalCenter(false)
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
                        TextField submittedField =
                                (TextField) fieldLayer.find(fieldId);
                        eventMessage = "Submitted: "
                                + (submittedField == null
                                ? ""
                                : submittedField.getText());
                        invalidate();
                        return false;
                    }));

            RectF originalBounds = field.getBounds();
            field.horizontalCenter(true).verticalCenter(true);
            RectF centeredBounds = field.getBounds();
            if (!field.isHorizontalCentered()
                    || !field.isVerticalCentered()
                    || Math.abs(centeredBounds.centerX() - getWidth() / 2f)
                    > 0.01f
                    || Math.abs(centeredBounds.centerY() - getHeight() / 2f)
                    > 0.01f) {
                throw new AssertionError(
                        "TextField centering was not applied."
                );
            }
            field.horizontalCenter(false).verticalCenter(false);
            if (!field.getBounds().equals(originalBounds)) {
                throw new AssertionError(
                        "TextField did not restore its original region."
                );
            }

            // Exercise runtime font changes, then restore the displayed state.
            if (field.getTypeface() == null
                    || field.getFontVariation() != FontVariation.BOLD) {
                throw new AssertionError(
                        "TextField font configuration was not applied."
                );
            }
            field.clearFontVariations();
            if (field.getFontVariation() != null) {
                throw new AssertionError(
                        "TextField font variation was not cleared."
                );
            }
            field.setFont(NativeFonts.INTER_ITALIC)
                    .setFontVariations(FontVariation.BOLD);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int saveCount = canvas.save();
            canvas.translate(0f, canvasTranslationY);
            drawLabels(canvas);
            drawComposerChrome(canvas);
            ui.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        private void drawComposerChrome(Canvas canvas) {
            if (composerField == null) return;

            float radius = dp(28);
            composerPaint.setStyle(Paint.Style.FILL);
            composerPaint.setColor(0xff1c1c1f);
            canvas.drawRoundRect(composerPanelBounds, radius, radius, composerPaint);
            composerPaint.setStyle(Paint.Style.STROKE);
            composerPaint.setStrokeWidth(dp(1));
            composerPaint.setColor(0xff3e3e44);
            RectF strokeBounds = new RectF(composerPanelBounds);
            strokeBounds.inset(dp(1), dp(1));
            canvas.drawRoundRect(strokeBounds, radius - dp(1), radius - dp(1), composerPaint);

            composerPaint.setStyle(Paint.Style.FILL);
            composerPaint.setColor(0xff29292e);
            canvas.drawOval(composerAddBounds, composerPaint);
            canvas.drawOval(composerActionBounds, composerPaint);

            composerPaint.setStyle(Paint.Style.STROKE);
            composerPaint.setStrokeCap(Paint.Cap.ROUND);
            composerPaint.setStrokeWidth(dp(2));
            composerPaint.setColor(0xffd8d8dd);
            float addCx = composerAddBounds.centerX();
            float addCy = composerAddBounds.centerY();
            float plusRadius = dp(7);
            canvas.drawLine(addCx - plusRadius, addCy,
                    addCx + plusRadius, addCy, composerPaint);
            canvas.drawLine(addCx, addCy - plusRadius,
                    addCx, addCy + plusRadius, composerPaint);

            composerPaint.setStyle(Paint.Style.FILL);
            composerPaint.setColor(0xff22b573);
            if (composerField.getText().trim().isEmpty()) {
                float cx = composerActionBounds.centerX();
                float cy = composerActionBounds.centerY();
                composerPaint.setStyle(Paint.Style.STROKE);
                composerPaint.setStrokeWidth(dp(2));
                canvas.drawRoundRect(
                        cx - dp(4), cy - dp(9),
                        cx + dp(4), cy + dp(4),
                        dp(4), dp(4), composerPaint
                );
                canvas.drawArc(
                        cx - dp(9), cy - dp(4),
                        cx + dp(9), cy + dp(9),
                        0f, 180f, false, composerPaint
                );
                canvas.drawLine(cx, cy + dp(9), cx, cy + dp(13), composerPaint);
            } else {
                composerIconPath.reset();
                composerIconPath.moveTo(
                        composerActionBounds.centerX() - dp(8),
                        composerActionBounds.centerY() - dp(9)
                );
                composerIconPath.lineTo(
                        composerActionBounds.centerX() + dp(11),
                        composerActionBounds.centerY()
                );
                composerIconPath.lineTo(
                        composerActionBounds.centerX() - dp(8),
                        composerActionBounds.centerY() + dp(9)
                );
                composerIconPath.close();
                canvas.drawPath(composerIconPath, composerPaint);
            }
            composerPaint.setStrokeCap(Paint.Cap.BUTT);
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
            boolean handled = handleComposerControlTouch(translatedEvent)
                    || ui.onTouchEvent(translatedEvent);
            translatedEvent.recycle();
            return handled || super.onTouchEvent(event);
        }

        private boolean handleComposerControlTouch(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                composerAddGesture = composerAddBounds.contains(
                        event.getX(), event.getY());
                composerActionGesture = composerActionBounds.contains(
                        event.getX(), event.getY());
                return composerAddGesture || composerActionGesture;
            }
            if (!composerAddGesture && !composerActionGesture) return false;
            if (action == MotionEvent.ACTION_UP) {
                if (composerAddGesture && composerAddBounds.contains(
                        event.getX(), event.getY())) {
                    eventMessage = "Genie attachment action";
                    invalidate();
                } else if (composerActionGesture && composerActionBounds.contains(
                        event.getX(), event.getY())) {
                    submitComposer();
                }
                composerAddGesture = false;
                composerActionGesture = false;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                composerAddGesture = false;
                composerActionGesture = false;
            }
            return true;
        }

        private void submitComposer() {
            TextField field = composerField;
            if (field == null) return;
            String message = field.getText().trim();
            if (message.isEmpty()) {
                eventMessage = "Genie voice action";
            } else {
                field.clear();
                eventMessage = "Sent: " + message;
            }
            field.clearFocus();
            post(composerGeometryUpdate);
            invalidate();
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
            removeCallbacks(composerGeometryUpdate);
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

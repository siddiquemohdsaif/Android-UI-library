package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.animator.component.CustomAnimatorComponent;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/**
 * Mixed-component integration test for ZLayerGroup and ZLayer.
 */
public final class ZLayerTestActivity extends AppCompatActivity {

    private ZLayerTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new ZLayerTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) {
            testView.release();
        }
        super.onDestroy();
    }

    public static final class ZLayerTestView extends View {

        private static final String BACKGROUND_LAYER = "background";
        private static final String CONTENT_LAYER = "content";
        private static final String EFFECTS_LAYER = "effects";
        private static final String DIALOG_LAYER = "dialog";

        private static final String STATUS_ID = "content_status";
        private static final String FIELD_ID = "content_player_name";
        private static final String BUTTON_ID = "content_open_dialog";
        private static final String DIALOG_TITLE_ID = "dialog_title";
        private static final String DIALOG_CLOSE_ID = "dialog_close";

        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer background = ui.addLayer(BACKGROUND_LAYER);
        private final ZLayer content = ui.addLayer(CONTENT_LAYER);
        private final ZLayer effects = ui.addLayer(EFFECTS_LAYER);
        private final ZLayer dialog = ui.addLayer(DIALOG_LAYER);
        private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private boolean initialized;
        private int backgroundClicks;
        private int dialogOpenCount;

        public ZLayerTestView(Context context) {
            super(context);
            setBackgroundColor(0xff0d121f);
            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);

            effects.setTouchPolicy(ZLayer.TouchPolicy.PASS_THROUGH);
            dialog.setTouchPolicy(ZLayer.TouchPolicy.MODAL);
            dialog.setVisible(false);

            runArchitectureAssertions();
        }

        private void runArchitectureAssertions() {
            ui.sendLayerToBack(BACKGROUND_LAYER);
            ui.bringLayerToFront(DIALOG_LAYER);
            ui.moveLayerAbove(EFFECTS_LAYER, CONTENT_LAYER);
            ui.moveLayerBelow(CONTENT_LAYER, EFFECTS_LAYER);

            if (ui.getLayerIndex(BACKGROUND_LAYER) != 0) {
                throw new AssertionError("Background layer must remain at index 0.");
            }
            if (ui.findLayer(CONTENT_LAYER) != content) {
                throw new AssertionError("Layer lookup returned the wrong instance.");
            }

            boolean duplicateRejected = false;
            try {
                ui.addLayer(CONTENT_LAYER);
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
            }
            if (!duplicateRejected) {
                throw new AssertionError("Duplicate layer IDs must be rejected.");
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
                    && (!initialized || width != oldWidth || height != oldHeight)) {
                buildScene();
                initialized = true;
            }
        }

        private void buildScene() {
            background.clear();
            content.clear();
            effects.clear();
            dialog.clear();

            background.add(new Text.Builder(
                    getContext(),
                    "background_tap_target",
                    "BACKGROUND — TAP HERE",
                    new RectF(dp(24), dp(100), getWidth() - dp(24), dp(180))
            )
                    .setTextSizePx(dp(24))
                    .setTextColor(0xff8ecae6)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setOnClickListener(id -> {
                        backgroundClicks++;
                        updateStatus("Background click " + backgroundClicks);
                    }));

            Position statusPosition = new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    70f,
                    250f
            );
            content.add(new Text.Builder(
                    getContext(),
                    STATUS_ID,
                    "ARCHITECTURE ASSERTIONS PASSED",
                    statusPosition,
                    new Size(940f, 100f)
            )
                    .setFont(NativeFonts.LILITA_ONE)
                    .setTextSize(46f)
                    .setTextColor(0xff90e0ef)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER));

            Position fieldPosition = new Position(
                    this,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    140f,
                    430f
            );
            content.add(new TextField.Builder(
                    getContext(),
                    FIELD_ID,
                    fieldPosition,
                    new Size(800f, 120f)
            )
                    .setHint("Tap to test shared IME")
                    .setText("ZLayer")
                    .setInputType(InputType.TYPE_CLASS_TEXT)
                    .setImeOptions(EditorInfo.IME_ACTION_DONE)
                    .setTextColor(Color.WHITE)
                    .setHintColor(0x88ffffff)
                    .setCursorColor(0xffffd166)
                    .setBackgroundColor(0xff182840, 0xff203a5c)
                    .setStrokeColor(0xff54708f, 0xff90e0ef));

            RectF buttonBounds = new RectF(
                    dp(70),
                    dp(330),
                    getWidth() - dp(70),
                    dp(420)
            );
            content.add(new CustomAnimatorComponent.Builder(
                    getContext(),
                    BUTTON_ID,
                    createButtonBitmap(800, 180),
                    buttonBounds
            )
                    .setClickListener(id -> showDialog())
                    .setPressScale(0.94f)
                    .setSoundAction(() -> {
                    }));

            effects.add(new Text.Builder(
                    getContext(),
                    "effects_pass_through_label",
                    "EFFECT LAYER: PASS THROUGH",
                    new RectF(dp(80), dp(445), getWidth() - dp(80), dp(490))
            )
                    .setTextSizePx(dp(14))
                    .setTextColor(0x99ffffff)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER));

            buildDialog();
            verifyComponentOperations();
        }

        private void buildDialog() {
            RectF titleBounds = new RectF(
                    dp(35),
                    dp(180),
                    getWidth() - dp(35),
                    dp(390)
            );
            dialog.add(new Text.Builder(
                    getContext(),
                    DIALOG_TITLE_ID,
                    "MODAL DIALOG\nUNDERLYING TOUCH IS BLOCKED",
                    titleBounds
            )
                    .setFont(NativeFonts.LILITA_ONE)
                    .setTextSizePx(dp(30))
                    .setTextColor(Color.WHITE)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setMaxLines(2));

            dialog.add(new Text.Builder(
                    getContext(),
                    DIALOG_CLOSE_ID,
                    "CLOSE DIALOG",
                    new RectF(dp(90), dp(430), getWidth() - dp(90), dp(520))
            )
                    .setFont(NativeFonts.LILITA_ONE)
                    .setTextSizePx(dp(28))
                    .setTextColor(0xffffd166)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setOnClickListener(id -> {
                        dialog.setVisible(false);
                        updateStatus("Modal closed; background enabled");
                    }));
        }

        private void verifyComponentOperations() {
            if (ui.findComponent(STATUS_ID, Text.class) == null) {
                throw new AssertionError("Typed global component lookup failed.");
            }

            content.bringToFront(STATUS_ID);
            content.sendToBack(STATUS_ID);
            content.bringToFront(STATUS_ID);

            if (!ui.moveComponent(STATUS_ID, EFFECTS_LAYER)
                    || ui.findComponent(STATUS_ID) == null
                    || !ui.moveComponent(STATUS_ID, CONTENT_LAYER)) {
                throw new AssertionError("Cross-layer component movement failed.");
            }

            boolean duplicateRejected = false;
            try {
                background.add(new Text.Builder(
                        getContext(),
                        STATUS_ID,
                        "DUPLICATE",
                        new RectF(0f, 0f, 10f, 10f)
                ));
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
            }
            if (!duplicateRejected) {
                throw new AssertionError("Duplicate component IDs must be rejected.");
            }
        }

        private void showDialog() {
            dialogOpenCount++;
            dialog.setVisible(true);
            ui.bringLayerToFront(DIALOG_LAYER);
            updateStatus("Modal opened " + dialogOpenCount + " time(s)");
        }

        private void updateStatus(String value) {
            Text status = ui.findComponent(STATUS_ID, Text.class);
            if (status != null) {
                status.setText(value);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ui.draw(canvas);

            guidePaint.setStyle(Paint.Style.STROKE);
            guidePaint.setStrokeWidth(dp(1));
            guidePaint.setColor(0x445ec8e5);
            canvas.drawRect(
                    dp(24),
                    dp(100),
                    getWidth() - dp(24),
                    dp(180),
                    guidePaint
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        @Override
        public boolean onCheckIsTextEditor() {
            return ui.onCheckIsTextEditor();
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection connection = ui.onCreateInputConnection(outAttrs);
            return connection != null
                    ? connection
                    : super.onCreateInputConnection(outAttrs);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            return ui.onKeyDown(keyCode, event)
                    || super.onKeyDown(keyCode, event);
        }

        public void release() {
            ui.release();
        }

        private Bitmap createButtonBitmap(int width, int height) {
            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0xff146c94);
            canvas.drawRoundRect(
                    0f,
                    0f,
                    width,
                    height,
                    34f,
                    34f,
                    paint
            );
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(64f);
            paint.setFakeBoldText(true);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float baseline = height / 2f
                    - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText("OPEN MODAL", width / 2f, baseline, paint);
            return bitmap;
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}

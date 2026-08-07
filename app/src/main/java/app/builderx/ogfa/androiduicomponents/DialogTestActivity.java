package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.button.Button;
import com.ogfa.nativeviews.card.DropShadow;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.dialog.Dialog;
import com.ogfa.nativeviews.dialog.DialogTransition;
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/** Modal draw, nested touch, outside-dismiss, Back, and transition test. */
public final class DialogTestActivity extends AppCompatActivity {
    private DialogTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new DialogTestView(this);
        setContentView(testView);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!testView.onBackPressed()) {
                    testView.showDialog("BACK REOPENED DIALOG");
                }
            }
        });
    }

    @Override protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private static final class DialogTestView extends View {
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer dialogLayer = ui.addLayer("dialogs");
        private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Dialog dialog;
        private boolean initialized;
        private String status = "DIALOG TEST";

        DialogTestView(Context context) {
            super(context);
            setClickable(true);
            setBackgroundColor(0xff16697a);
            titlePaint.setColor(Color.WHITE);
            titlePaint.setTextAlign(Paint.Align.CENTER);
            titlePaint.setTextSize(dp(28));
            bodyPaint.setColor(0xffb9e3ea);
            bodyPaint.setTextAlign(Paint.Align.CENTER);
            bodyPaint.setTextSize(dp(17));
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w > 0 && h > 0 && (!initialized || w != oldw || h != oldh)) {
                createDialog();
                initialized = true;
            }
        }

        private void createDialog() {
            dialogLayer.clear();
            dialog = dialogLayer.add(new Dialog.Builder(
                    getContext(),
                    "confirmation",
                    new Position(
                            this,
                            Position.HorizontalMarginFrom.LEFT,
                            Position.VerticalMarginFrom.TOP,
                            0f,
                            0f
                    ),
                    new Size(850f, 650f)
            )
                    .horizontalCenter(true)
                    .verticalCenter(true)
                    .setBackgroundColor(Color.WHITE)
                    .setCornerRadius(48f)
                    .setDropShadow(new DropShadow(
                            0f, 10f, 36f, 4f,
                            Color.argb(35, 0, 0, 0)
                    ))
                    .setDimColor(Color.BLACK)
                    .setDimAlpha(0.58f)
                    .setOutsideTouchPolicy(Dialog.OutsideTouchPolicy.DISMISS)
                    .setDismissOnBackPressed(true)
                    .setEnterTransition(DialogTransition.fadeScale(240L, 0.88f))
                    .setExitTransition(DialogTransition.fadeScale(190L, 0.88f))
                    .setOnShowListener(id -> {
                        status = "SHOWN: " + id;
                        invalidate();
                    })
                    .setOnDismissListener((id, reason) -> {
                        status = "DISMISSED: " + reason + " — TAP TO REOPEN";
                        invalidate();
                    })
                    .setContent((instance, content, scope) -> {
                        content.add(new Text.Builder(
                                getContext(), scope.id("title"),
                                "NATIVE DIALOG",
                                scope.rect(60f, 55f, 730f, 100f)
                        )
                                .setFont(NativeFonts.INTER)
                                .setFontVariations(FontVariation.BOLD)
                                .setTextSizePx(scope.px(48f))
                                .setTextColor(0xff102a43)
                                .setAlignment(Text.Alignment.CENTER)
                                .setVerticalAlignment(Text.VerticalAlignment.CENTER));

                        content.add(new Text.Builder(
                                getContext(), scope.id("message"),
                                "This content is composed from native Canvas components. "
                                        + "Tap outside or press Back to dismiss.",
                                scope.rect(80f, 175f, 690f, 155f)
                        )
                                .setFont(NativeFonts.INTER)
                                .setTextSizePx(scope.px(30f))
                                .setTextColor(0xff526777)
                                .setAlignment(Text.Alignment.CENTER)
                                .setVerticalAlignment(Text.VerticalAlignment.CENTER));

                        content.add(new Button.Builder(
                                getContext(), scope.id("confirm"),
                                0xff009fc8,
                                "CONFIRM",
                                scope.rect(105f, 405f, 640f, 125f)
                        )
                                .setCornerRadiusPx(scope.px(28f))
                                .setTextSizePx(scope.px(36f))
                                .setTextColor(Color.WHITE)
                                .setFont(NativeFonts.INTER)
                                .setFontVariations(FontVariation.SEMI_BOLD)
                                .setRippleEnabled(true)
                                .setOnClickListener(id ->
                                        instance.dismiss(Dialog.DismissReason.ACTION)));
                    }));
            dialog.show();

            if (dialog.getBounds().isEmpty()
                    || dialog.find("title", Text.class) == null
                    || dialog.find("confirm", Button.class) == null) {
                throw new AssertionError("Dialog content or lookup failed.");
            }
        }

        void showDialog(String message) {
            status = message;
            dialog.show();
            invalidate();
        }

        boolean onBackPressed() { return ui.onBackPressed(); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawText("BACKGROUND CONTENT", getWidth() / 2f, dp(150), titlePaint);
            canvas.drawText("The modal must block touches below it.",
                    getWidth() / 2f, dp(190), bodyPaint);
            canvas.drawText(status, getWidth() / 2f, getHeight() - dp(90), bodyPaint);
            ui.draw(canvas);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (ui.onTouchEvent(event)) return true;
            if (event.getActionMasked() == MotionEvent.ACTION_UP && dialog.isHidden()) {
                showDialog("REOPENING");
            }
            return true;
        }

        void release() { ui.release(); }
        private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    }
}

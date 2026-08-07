package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.button.Button;
import com.ogfa.nativeviews.card.Card;
import com.ogfa.nativeviews.card.DropShadow;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

/**
 * Visual, nested-touch, and runtime API test for Card.
 */
public final class CardTestActivity extends AppCompatActivity {

    private CardTestView testView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        testView = new CardTestView(this);
        setContentView(testView);
    }

    @Override
    protected void onDestroy() {
        if (testView != null) testView.release();
        super.onDestroy();
    }

    private static final class CardTestView extends View {

        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer cards = ui.addLayer("cards");
        private final Paint headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private Bitmap avatarBitmap;
        private Bitmap buttonBitmap;
        private Bitmap cardBitmap;
        private boolean initialized;
        private String status = "NESTED COMPONENTS ARE INTERACTIVE";

        CardTestView(Context context) {
            super(context);
            setBackgroundColor(0xffedf2f7);
            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
            headingPaint.setTextAlign(Paint.Align.CENTER);
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
                createCards();
                initialized = true;
            }
        }

        private void createCards() {
            cards.clear();
            recycleBitmaps();
            avatarBitmap = createCircleBitmap(180, 0xff6c63ff);
            buttonBitmap = createRoundedBitmap(420, 120, 0xff111827, 32f);
            cardBitmap = createGradientBitmap(900, 430);

            Card profile = cards.add(new Card.Builder(
                    getContext(),
                    "profile_card",
                    position(90f, 300f),
                    new Size(900f, 700f)
            )
                    .setBackgroundColor(Color.WHITE)
                    .setCornerRadius(48f)
                    .setDropShadow(new DropShadow(
                            0f,
                            12f,
                            32f,
                            4f,
                            0x40000000
                    )));
            profile.getContentLayer().setTouchPolicy(
                    ZLayer.TouchPolicy.BLOCK_BELOW
            );

            profile.getContentLayer().add(new Text.Builder(
                    getContext(),
                    "profile_title",
                    "PLAYER PROFILE",
                    position(140f, 350f),
                    new Size(800f, 90f)
            )
                    .setTextSize(48f)
                    .setTextColor(0xff111827)
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.BOLD)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER));

            profile.getContentLayer().add(new Image.Builder(
                    getContext(),
                    "profile_avatar",
                    avatarBitmap,
                    position(150f, 480f),
                    new Size(180f, 180f)
            )
                    .setScaleType(Image.ScaleType.FIT_XY));

            TextField nameField = profile.getContentLayer().add(
                    new TextField.Builder(
                            getContext(),
                            "profile_name_field",
                            position(380f, 490f),
                            new Size(560f, 120f)
                    )
                            .setHint("Player name")
                            .setText("PLAYER ONE")
                            .setTextSize(60f)
                            .setTextColor(0xff111827)
                            .setHintColor(0xff7a8494)
                            .setCursorColor(0xff6c63ff)
                            .setCursorWidth(6f)
                            .setBackgroundColor(0xfff1f5f9, 0xffffffff)
                            .setStrokeColor(0xffcbd5e1, 0xff6c63ff)
                            .setCornerRadius(42f)
                            .setFont(NativeFonts.INTER)
            );

            profile.getContentLayer().add(new Button.Builder(
                    getContext(),
                    "profile_action",
                    buttonBitmap,
                    "SAVE PROFILE",
                    position(330f, 760f),
                    new Size(420f, 120f)
            )
                    .setCornerRadius(32f)
                    .setTextSize(36f)
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.SEMI_BOLD)
                    .setOnClickListener(id -> {
                        status = "NESTED BUTTON CLICKED";
                        invalidate();
                    }));

            verifyTranslatedContentLayer(profile);

            Card imageCard = cards.add(new Card.Builder(
                    getContext(),
                    "image_card",
                    position(90f, 1160f),
                    new Size(900f, 430f)
            )
                    .setBackgroundImage(cardBitmap)
                    .setBackgroundScaleType(Image.ScaleType.CENTER_CROP)
                    .setCornerRadius(60f)
                    .setDropShadowPx(new DropShadow(
                            0f,
                            dp(8),
                            dp(24),
                            dp(3),
                            0x55000000
                    )));
            imageCard.getContentLayer().add(new Text.Builder(
                    getContext(),
                    "image_card_title",
                    "IMAGE BACKGROUND",
                    position(140f, 1300f),
                    new Size(800f, 100f)
            )
                    .setTextSize(54f)
                    .setTextColor(Color.WHITE)
                    .setFont(NativeFonts.MONTSERRAT)
                    .setFontVariations(FontVariation.BOLD)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER));

            verifyCard(profile, imageCard, nameField);
        }

        private void verifyCard(
                Card profile,
                Card imageCard,
                TextField nameField
        ) {
            if (profile.getBackgroundType() != Card.BackgroundType.COLOR
                    || profile.getBackgroundColor() != Color.WHITE
                    || profile.getContentLayer().size() != 4
                    || profile.getVisualBounds().equals(profile.getBounds())
                    || profile.getResolvedCornerRadius() <= 0f
                    || profile.getResolvedDropShadow() == null) {
                throw new AssertionError("Color Card state is invalid.");
            }
            if (ui.findComponent("profile_title", Text.class) == null
                    || ui.findComponent("profile_avatar", Image.class) == null
                    || ui.findComponent("profile_action", Button.class) == null
                    || ui.findComponent(
                            "profile_name_field",
                            TextField.class
                    ) != nameField) {
                throw new AssertionError(
                        "Nested Card children were not globally registered."
                );
            }
            if (imageCard.getBackgroundType() != Card.BackgroundType.IMAGE
                    || imageCard.getBackgroundImage() != cardBitmap
                    || !imageCard.isDropShadowInPixels()) {
                throw new AssertionError("Image Card state is invalid.");
            }

            RectF original = imageCard.getBounds();
            imageCard.setRegion(new RectF(original))
                    .setRegion(
                            position(90f, 1160f),
                            new Size(900f, 430f)
                    )
                    .setCornerRadiusPx(dp(24))
                    .setCornerRadius(60f)
                    .setAlpha(0.8f)
                    .setAlpha(1f)
                    .setVisible(false)
                    .setVisible(true)
                    .setEnabled(false)
                    .setEnabled(true);

            DropShadow pixelShadow = imageCard.getDropShadow();
            imageCard.removeDropShadow();
            if (imageCard.getDropShadow() != null
                    || !imageCard.getVisualBounds().equals(
                            imageCard.getBounds()
                    )) {
                throw new AssertionError("Card shadow was not removed.");
            }
            imageCard.resetDefaultDropShadow();
            if (!DropShadow.DEFAULT.equals(imageCard.getDropShadow())
                    || imageCard.isDropShadowInPixels()) {
                throw new AssertionError("Default Card shadow was not restored.");
            }
            imageCard.setDropShadowPx(pixelShadow);

            imageCard.setBackgroundColor(Color.WHITE);
            if (imageCard.getBackgroundType() != Card.BackgroundType.COLOR
                    || imageCard.getBackgroundColor() != Color.WHITE) {
                throw new AssertionError(
                        "Card did not switch to a color background."
                );
            }
            imageCard.setBackgroundImage(cardBitmap);

            Card defaults = new Card.Builder(
                    getContext(),
                    "card_defaults_assertion",
                    new RectF(0f, 0f, 200f, 100f)
            ).build(this);
            if (defaults.getBackgroundType() != Card.BackgroundType.COLOR
                    || defaults.getBackgroundColor() != Color.WHITE
                    || !DropShadow.DEFAULT.equals(defaults.getDropShadow())) {
                throw new AssertionError("Card defaults are invalid.");
            }
            defaults.release();
        }

        private void verifyTranslatedContentLayer(Card card) {
            ZLayer translated = card.addContentLayer("translated_test");
            RectF originalCardBounds = card.getBounds();
            RectF cardBounds = new RectF(originalCardBounds);
            RectF buttonBounds = new RectF(
                    cardBounds.left + dp(30),
                    cardBounds.top + dp(30),
                    cardBounds.left + dp(90),
                    cardBounds.top + dp(70)
            );
            int[] clicks = {0};
            translated.add(new Button.Builder(
                    getContext(),
                    "translated_touch_probe",
                    Color.TRANSPARENT,
                    buttonBounds
            )
                    .setPressedScale(1f)
                    .setPressAnimationDuration(0L)
                    .setOnClickListener(id -> clicks[0]++));

            translated.setTranslation(12f, -24f);
            RectF translatedCardBounds = card.getBounds();
            if (card.findContentLayer("translated_test") != translated
                    || card.getContentLayers().size() != 2
                    || translated.getTranslationX() != 12f
                    || translated.getTranslationY() != -24f
                    || Math.abs(
                    translatedCardBounds.left
                            - (originalCardBounds.left + 12f)
            ) > 0.0001f
                    || Math.abs(
                    translatedCardBounds.top
                            - (originalCardBounds.top - 24f)
            ) > 0.0001f) {
                throw new AssertionError(
                        "Card-owned layer did not translate its Card owner."
                );
            }

            dispatch(
                    card,
                    MotionEvent.ACTION_DOWN,
                    buttonBounds.centerX() + 12f,
                    buttonBounds.centerY() - 24f
            );
            dispatch(
                    card,
                    MotionEvent.ACTION_UP,
                    buttonBounds.centerX() + 12f,
                    buttonBounds.centerY() - 24f
            );
            if (clicks[0] != 1) {
                throw new AssertionError(
                        "Translated Card layer did not map touch coordinates."
                );
            }
            translated.clear();
            translated.resetTranslation();
            if (!card.getBounds().equals(originalCardBounds)) {
                throw new AssertionError(
                        "Card translation did not reset with its layer."
                );
            }
        }

        private static void dispatch(
                Card card,
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
            card.onTouchEvent(event);
            event.recycle();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ui.draw(canvas);

            headingPaint.setColor(0xff111827);
            headingPaint.setTextSize(dp(22));
            headingPaint.setFakeBoldText(true);
            canvas.drawText("CARD COMPONENT", getWidth() / 2f, dp(48), headingPaint);
            headingPaint.setColor(0xff526174);
            headingPaint.setTextSize(dp(13));
            canvas.drawText(status, getWidth() / 2f, dp(82), headingPaint);
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

        private Bitmap createCircleBitmap(int size, int color) {
            Bitmap bitmap = Bitmap.createBitmap(
                    size,
                    size,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(size * 0.38f);
            paint.setFakeBoldText(true);
            canvas.drawText("P1", size / 2f, size * 0.62f, paint);
            return bitmap;
        }

        private Bitmap createRoundedBitmap(
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
            return bitmap;
        }

        private Bitmap createGradientBitmap(int width, int height) {
            Bitmap bitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new LinearGradient(
                    0f,
                    0f,
                    width,
                    height,
                    0xff4f46e5,
                    0xff0ea5e9,
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRect(0f, 0f, width, height, paint);
            return bitmap;
        }

        private void recycleBitmaps() {
            recycle(avatarBitmap);
            recycle(buttonBitmap);
            recycle(cardBitmap);
            avatarBitmap = null;
            buttonBitmap = null;
            cardBitmap = null;
        }

        private static void recycle(Bitmap bitmap) {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}

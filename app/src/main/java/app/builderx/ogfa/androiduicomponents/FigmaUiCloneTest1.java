package app.builderx.ogfa.androiduicomponents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ogfa.nativeviews.button.Button;
import com.ogfa.nativeviews.card.Card;
import com.ogfa.nativeviews.card.DropShadow;
import com.ogfa.nativeviews.component.FigmaConfig;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.font.NativeFonts;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.text.FontVariation;
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import java.io.IOException;
import java.io.InputStream;

/**
 * First incremental clone test for the extracted PingGo Figma login page.
 *
 * <p>This step intentionally renders only the background and bottom legal
 * terms asset in one ZLayer.</p>
 */
public final class FigmaUiCloneTest1 extends AppCompatActivity {

    private static final float FIGMA_REFERENCE_WIDTH = 852f;

    private FigmaUiCloneView cloneView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        cloneView = new FigmaUiCloneView(this);
        setContentView(cloneView);
    }

    @SuppressWarnings("deprecation")
    private void configureSystemBars() {
        Window window = getWindow();
        int systemBarColor = Color.parseColor("#EBF1F7");
        window.setStatusBarColor(systemBarColor);
        window.setNavigationBarColor(systemBarColor);

        int systemUiFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            systemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            systemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(systemUiFlags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (cloneView != null) {
            cloneView.release();
            cloneView = null;
        }
        super.onDestroy();
    }

    private static final class FigmaUiCloneView extends View {

        private static final String ASSET_ROOT =
                "figma/figma_login_page_1/";

        private final FigmaConfig figmaConfig =
                new FigmaConfig(FIGMA_REFERENCE_WIDTH);
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer backgroundLayer = ui.addLayer("background");
        private final ZLayer cardLayer = ui.addLayer("card");

        private Bitmap backgroundBitmap;
        private Bitmap logoBitmap;
        private Bitmap brandNameBitmap;
        private Bitmap cardTitleBitmap;
        private Bitmap cardDescriptionBitmap;
        private Bitmap securityLockBitmap;
        private Bitmap securityMessageBitmap;
        private Bitmap nextButtonBitmap;
        private boolean initialized;

        FigmaUiCloneView(Context context) {
            super(context);
            setBackgroundColor(0xffffffff);
            setClickable(true);
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
                buildBackgroundLayer();
                initialized = true;
            }
        }

        private void buildBackgroundLayer() {
            cardLayer.clear();
            backgroundLayer.clear();
            recycleBitmaps();

            backgroundBitmap = loadBitmap("background.webp");
            logoBitmap = loadBitmap("logo.webp");
            brandNameBitmap = loadBitmap("brand_name.webp");
            cardTitleBitmap = loadBitmap("card_title.webp");
            cardDescriptionBitmap = loadBitmap("card_description.webp");
            securityLockBitmap = loadBitmap("security_lock_icon.webp");
            securityMessageBitmap = loadBitmap("security_message.webp");
            nextButtonBitmap = loadBitmap("next_button.png");

            Position backgroundPosition = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    0f,
                    0f
            );
            backgroundLayer.add(new Image.Builder(
                    getContext(),
                    "background",
                    backgroundBitmap,
                    backgroundPosition,
                    new Size(852f, 1846f)
            ).setScaleType(Image.ScaleType.FIT_XY));

            Position logoPosition = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    326f,
                    230f
            );
            backgroundLayer.add(new Image.Builder(
                    getContext(),
                    "logo",
                    logoBitmap,
                    logoPosition,
                    new Size(200f, 200f)
            ).setScaleType(Image.ScaleType.FIT_XY));

            Position brandNamePosition = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    265f,
                    426f
            );
            backgroundLayer.add(new Image.Builder(
                    getContext(),
                    "brand_name",
                    brandNameBitmap,
                    brandNamePosition,
                    new Size(321f, 99f)
            ).setScaleType(Image.ScaleType.FIT_XY));

            addCenteredTagline(
                    "tagline_primary",
                    "HD Video Calls. Crystal Clear.",
                    556f,
                    390f,
                    Color.parseColor("#323232")
            );
            addCenteredTagline(
                    "tagline_secondary",
                    "Fast. Secure. Private.",
                    606f,
                    281f,
                    Color.parseColor("#009FC8")
            );

            Position legalTermsPosition = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.BOTTOM,
                    0f,
                    57f
            );
            backgroundLayer.add(new Text.Builder(
                    getContext(),
                    "legal_terms",
                    createLegalTermsText(),
                    legalTermsPosition,
                    new Size(400f, 70f)
            )
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.REGULAR)
                    .setTextSize(23f)
                    .setLineHeightPercent(130.2f)
                    .setLetterSpacingPercent(0f)
                    .setTextColor(Color.parseColor("#656565"))
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .horizontalCenter(true)
                    .setMaxLines(2));

            Position cardPosition = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.BOTTOM,
                    0f,
                    163f
            );
            Card phoneNumberCard = cardLayer.add(new Card.Builder(
                    getContext(),
                    "phone_number_card",
                    cardPosition,
                    new Size(742f, 849f)
            )
                    .setBackgroundColor(Color.WHITE)
                    .setCornerRadius(50f)
                    .horizontalCenter(true)
                    .setDropShadow(new DropShadow(
                            0f,
                            4f,
                            28f,
                            4f,
                            Color.argb(13, 0, 0, 0)
                    )));

            addCenteredCardImage(
                    phoneNumberCard,
                    "card_title",
                    cardTitleBitmap,
                    902f,
                    529f,
                    46f
            );
            addCenteredCardImage(
                    phoneNumberCard,
                    "card_description",
                    cardDescriptionBitmap,
                    799f,
                    398f,
                    76f
            );
            addCardImage(
                    phoneNumberCard,
                    "security_lock_icon",
                    securityLockBitmap,
                    165f,
                    426.591f,
                    25f,
                    28.409f
            );
            addCardImage(
                    phoneNumberCard,
                    "security_message",
                    securityMessageBitmap,
                    204f,
                    425f,
                    465f,
                    33f
            );
            addCardRelativeButton(
                    phoneNumberCard,
                    "next_button",
                    nextButtonBitmap,
                    1f,
                    621f,
                    740f,
                    148f
            );
        }

        private void addCardRelativeButton(
                Card card,
                String id,
                Bitmap bitmap,
                float relativeLeft,
                float relativeTop,
                float width,
                float height
        ) {
            float scale = figmaConfig.getScale(getWidth());
            RectF cardBounds = card.getBounds();
            float left = cardBounds.left + relativeLeft * scale;
            float top = cardBounds.top + relativeTop * scale;
            RectF bounds = new RectF(
                    left,
                    top,
                    left + width * scale,
                    top + height * scale
            );
            card.getContentLayer().add(new Button.Builder(
                    getContext(),
                    id,
                    bitmap,
                    bounds
            )
                    .setImageScaleType(Image.ScaleType.FIT_XY)
                    .setRippleEnabled(true)
                    .setRippleColor(0x33ffffff)
                    .setRippleDuration(320L)
                    .setRippleOrigin(Button.RippleOrigin.TOUCH)
                    .setOnClickListener(buttonId -> Toast.makeText(
                            getContext(),
                            "Next clicked",
                            Toast.LENGTH_SHORT
                    ).show()));
        }

        private void addCardImage(
                Card card,
                String id,
                Bitmap bitmap,
                float left,
                float bottomMargin,
                float width,
                float height
        ) {
            Position position = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.BOTTOM,
                    left,
                    bottomMargin
            );
            card.getContentLayer().add(new Image.Builder(
                    getContext(),
                    id,
                    bitmap,
                    position,
                    new Size(width, height)
            ).setScaleType(Image.ScaleType.FIT_XY));
        }

        private void addCenteredCardImage(
                Card card,
                String id,
                Bitmap bitmap,
                float bottomMargin,
                float width,
                float height
        ) {
            Position position = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.BOTTOM,
                    0f,
                    bottomMargin
            );
            card.getContentLayer().add(new Image.Builder(
                    getContext(),
                    id,
                    bitmap,
                    position,
                    new Size(width, height)
            )
                    .setScaleType(Image.ScaleType.FIT_XY)
                    .horizontalCenter(true));
        }

        private void addCenteredTagline(
                String id,
                String value,
                float top,
                float width,
                int color
        ) {
            Position position = new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    0f,
                    top
            );
            backgroundLayer.add(new Text.Builder(
                    getContext(),
                    id,
                    value,
                    position,
                    new Size(width, 31f)
            )
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.REGULAR)
                    .setTextSize(28f)
                    .setLineHeightPercent(100f)
                    .setLetterSpacingPercent(0f)
                    .setTextColor(color)
                    .setAlignment(Text.Alignment.CENTER)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .horizontalCenter(true)
                    .setWrapEnabled(false));
        }

        private SpannableString createLegalTermsText() {
            String value = "By continuing, you agree to our\n"
                    + "Terms of Service and Privacy Policy.";
            SpannableString text = new SpannableString(value);
            int accentColor = Color.parseColor("#019CC4");
            applyColor(text, "Terms of Service", accentColor);
            applyColor(text, "Privacy Policy", accentColor);
            return text;
        }

        private void applyColor(
                SpannableString text,
                String phrase,
                int color
        ) {
            int start = text.toString().indexOf(phrase);
            if (start < 0) return;
            text.setSpan(
                    new ForegroundColorSpan(color),
                    start,
                    start + phrase.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        private Bitmap loadBitmap(String fileName) {
            String assetPath = ASSET_ROOT + fileName;
            try (InputStream input =
                         getContext().getAssets().open(assetPath)) {
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                if (bitmap == null) {
                    throw new IllegalStateException(
                            "Unable to decode Figma asset: " + assetPath
                    );
                }
                return bitmap;
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Unable to load Figma asset: " + assetPath,
                        exception
                );
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            ui.draw(canvas);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return ui.onTouchEvent(event) || super.onTouchEvent(event);
        }

        void release() {
            ui.release();
            recycleBitmaps();
            initialized = false;
        }

        private void recycleBitmaps() {
            if (backgroundBitmap != null) {
                if (!backgroundBitmap.isRecycled()) {
                    backgroundBitmap.recycle();
                }
                backgroundBitmap = null;
            }
            if (logoBitmap != null) {
                if (!logoBitmap.isRecycled()) {
                    logoBitmap.recycle();
                }
                logoBitmap = null;
            }
            if (brandNameBitmap != null) {
                if (!brandNameBitmap.isRecycled()) {
                    brandNameBitmap.recycle();
                }
                brandNameBitmap = null;
            }
            if (cardTitleBitmap != null) {
                if (!cardTitleBitmap.isRecycled()) {
                    cardTitleBitmap.recycle();
                }
                cardTitleBitmap = null;
            }
            if (cardDescriptionBitmap != null) {
                if (!cardDescriptionBitmap.isRecycled()) {
                    cardDescriptionBitmap.recycle();
                }
                cardDescriptionBitmap = null;
            }
            if (securityLockBitmap != null) {
                if (!securityLockBitmap.isRecycled()) {
                    securityLockBitmap.recycle();
                }
                securityLockBitmap = null;
            }
            if (securityMessageBitmap != null) {
                if (!securityMessageBitmap.isRecycled()) {
                    securityMessageBitmap.recycle();
                }
                securityMessageBitmap = null;
            }
            if (nextButtonBitmap != null) {
                if (!nextButtonBitmap.isRecycled()) {
                    nextButtonBitmap.recycle();
                }
                nextButtonBitmap = null;
            }
        }
    }
}

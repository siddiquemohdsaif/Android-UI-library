package app.builderx.ogfa.androiduicomponents;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
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
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Native Canvas clone test for the extracted PingGo Figma login page.
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
        private static final long PAN_ANIMATION_DURATION_MS = 220L;

        private final FigmaConfig figmaConfig =
                new FigmaConfig(FIGMA_REFERENCE_WIDTH);
        private final ZLayerGroup ui = new ZLayerGroup(this);
        private final ZLayer backgroundLayer = ui.addLayer("background");
        private final ZLayer cardLayer = ui.addLayer("card");
        private final Rect visibleWindow = new Rect();
        private final int[] locationOnScreen = new int[2];
        private final ViewTreeObserver.OnGlobalLayoutListener
                globalLayoutListener = this::updateImeInsetFromVisibleWindow;

        private Bitmap backgroundBitmap;
        private Bitmap logoBitmap;
        private Bitmap brandNameBitmap;
        private Bitmap cardTitleBitmap;
        private Bitmap cardDescriptionBitmap;
        private Bitmap securityLockBitmap;
        private Bitmap securityMessageBitmap;
        private Bitmap nextButtonBitmap;
        private Bitmap phoneLabelBackgroundBitmap;
        private Bitmap phoneDividerBitmap;
        private ZLayer cardContentLayer;
        private boolean initialized;
        private int imeInsetBottom;
        private float phoneFieldTranslationY;
        private float panTargetY;
        private ValueAnimator panAnimator;

        FigmaUiCloneView(Context context) {
            super(context);
            setBackgroundColor(0xffffffff);
            setClickable(true);
            setFocusable(true);
            setFocusableInTouchMode(true);
            getViewTreeObserver().addOnGlobalLayoutListener(
                    globalLayoutListener
            );
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
                                    List<WindowInsetsAnimation>
                                            runningAnimations
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
            cardContentLayer = null;
            recycleBitmaps();

            backgroundBitmap = loadBitmap("background.webp");
            logoBitmap = loadBitmap("logo.webp");
            brandNameBitmap = loadBitmap("brand_name.webp");
            cardTitleBitmap = loadBitmap("card_title.webp");
            cardDescriptionBitmap = loadBitmap("card_description.webp");
            securityLockBitmap = loadBitmap("security_lock_icon.webp");
            securityMessageBitmap = loadBitmap("security_message.webp");
            nextButtonBitmap = loadBitmap("next_button.png");
            phoneLabelBackgroundBitmap = createColorBitmap(Color.WHITE);
            phoneDividerBitmap = createColorBitmap(
                    Color.parseColor("#DDE3EA")
            );

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

            cardContentLayer = phoneNumberCard.getContentLayer();

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
            addPhoneNumberField(phoneNumberCard, cardContentLayer);
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

        private void addPhoneNumberField(Card card, ZLayer fieldLayer) {
            float cardDesignLeft = card.getBounds().left
                    / figmaConfig.getScale(getWidth());
            float cardDesignTop = card.getBounds().top
                    / figmaConfig.getScale(getWidth());

            Position fieldPosition = absoluteCardPosition(
                    cardDesignLeft,
                    cardDesignTop,
                    50f,
                    405f
            );
            fieldLayer.add(new TextField.Builder(
                    getContext(),
                    "phone_number",
                    fieldPosition,
                    new Size(642f, 116f)
            )
//                    .setText("98765 43210")
                    .setHint("98765 43210")
                    .setMaxLength(14)
                    .setInputType(InputType.TYPE_CLASS_PHONE)
                    .setImeOptions(EditorInfo.IME_ACTION_DONE)
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.REGULAR)
                    .setTextSize(29f)
                    .setTextColor(Color.parseColor("#000E1A"))
                    .setHintColor(Color.parseColor("#757575"))
                    .setCursorColor(Color.parseColor("#019CC4"))
                    .setCursorWidth(4f)
                    .setSelectionColor(0x443b9cff)
                    .setBackgroundColor(Color.WHITE, Color.WHITE)
                    .setStrokeColor(
                            Color.parseColor("#DDE3EA"),
                            Color.parseColor("#019CC4")
                    )
                    .setStrokeWidth(3.5f)
                    .setCornerRadius(20f)
                    .setPadding(155f, 18f)
                    .setOnFocusChangedListener((id, focused) ->
                            post(this::updateCanvasTranslation)));

            fieldLayer.add(new Image.Builder(
                    getContext(),
                    "phone_number_divider",
                    phoneDividerBitmap,
                    absoluteCardPosition(
                            cardDesignLeft,
                            cardDesignTop,
                            154f,
                            441f
                    ),
                    new Size(3f, 44f)
            ).setScaleType(Image.ScaleType.FIT_XY));

            fieldLayer.add(new Text.Builder(
                    getContext(),
                    "phone_country_code",
                    " +91",
                    absoluteCardPosition(
                            cardDesignLeft,
                            cardDesignTop,
                            70f,
                            429f
                    ),
                    new Size(75f, 68f)
            )
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.REGULAR)
                    .setTextSize(29f)
                    .setTextColor(Color.parseColor("#000E1A"))
                    .setAlignment(Text.Alignment.START)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setWrapEnabled(false));

            fieldLayer.add(new Image.Builder(
                    getContext(),
                    "phone_number_label_background",
                    phoneLabelBackgroundBitmap,
                    absoluteCardPosition(
                            cardDesignLeft,
                            cardDesignTop,
                            70f,
                            391f
                    ),
                    new Size(180f, 30f)
            ).setScaleType(Image.ScaleType.FIT_XY));

            fieldLayer.add(new Text.Builder(
                    getContext(),
                    "phone_number_label",
                    "Phone number",
                    absoluteCardPosition(
                            cardDesignLeft,
                            cardDesignTop,
                            78f,
                            386f
                    ),
                    new Size(185f, 40f)
            )
                    .setFont(NativeFonts.INTER)
                    .setFontVariations(FontVariation.REGULAR)
                    .setTextSize(24f)
                    .setTextColor(Color.parseColor("#019CC4"))
                    .setAlignment(Text.Alignment.START)
                    .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                    .setWrapEnabled(false));
        }

        private Position absoluteCardPosition(
                float cardDesignLeft,
                float cardDesignTop,
                float relativeLeft,
                float relativeTop
        ) {
            return new Position(
                    this,
                    figmaConfig,
                    Position.HorizontalMarginFrom.LEFT,
                    Position.VerticalMarginFrom.TOP,
                    cardDesignLeft + relativeLeft,
                    cardDesignTop + relativeTop
            );
        }

        private static Bitmap createColorBitmap(int color) {
            Bitmap bitmap = Bitmap.createBitmap(
                    1,
                    1,
                    Bitmap.Config.ARGB_8888
            );
            bitmap.eraseColor(color);
            return bitmap;
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
            int visibleBottomInView = visibleWindow.bottom
                    - locationOnScreen[1];
            int obscuredHeight = Math.max(
                    0,
                    getHeight() - visibleBottomInView
            );
            int detectedInset = obscuredHeight > getHeight() * 0.15f
                    ? obscuredHeight
                    : 0;
            setImeInsetBottom(detectedInset);
        }

        private void setImeInsetBottom(int insetBottom) {
            int safeInset = Math.max(0, insetBottom);
            if (imeInsetBottom == safeInset) return;
            imeInsetBottom = safeInset;
            post(this::updateCanvasTranslation);
        }

        private void updateCanvasTranslation() {
            TextField focusedField = ui.getFocusedTextField();
            float target = 0f;
            if (focusedField != null && imeInsetBottom > 0) {
                RectF fieldBounds = focusedField.getBounds();
                float safeGap = 24f * figmaConfig.getScale(getWidth());
                float visibleBottom = getHeight()
                        - imeInsetBottom
                        - safeGap;
                target = Math.min(0f, visibleBottom - fieldBounds.bottom);
                target = Math.max(target, safeGap - fieldBounds.top);
            }
            animateCanvasTranslation(target);
        }

        private void animateCanvasTranslation(float target) {
            if (Math.abs(panTargetY - target) < 0.5f
                    && (panAnimator == null || panAnimator.isRunning())) {
                return;
            }
            panTargetY = target;
            if (panAnimator != null) panAnimator.cancel();
            if (Math.abs(phoneFieldTranslationY - target) < 0.5f) {
                phoneFieldTranslationY = target;
                if (cardContentLayer != null) {
                    cardContentLayer.setTranslationY(target);
                }
                invalidate();
                return;
            }
            panAnimator = ValueAnimator.ofFloat(
                    phoneFieldTranslationY,
                    target
            );
            panAnimator.setDuration(PAN_ANIMATION_DURATION_MS);
            panAnimator.setInterpolator(new DecelerateInterpolator());
            panAnimator.addUpdateListener(animation -> {
                phoneFieldTranslationY =
                        (float) animation.getAnimatedValue();
                if (cardContentLayer != null) {
                    cardContentLayer.setTranslationY(
                            phoneFieldTranslationY
                    );
                }
            });
            panAnimator.start();
        }

        void release() {
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
            if (phoneLabelBackgroundBitmap != null) {
                if (!phoneLabelBackgroundBitmap.isRecycled()) {
                    phoneLabelBackgroundBitmap.recycle();
                }
                phoneLabelBackgroundBitmap = null;
            }
            if (phoneDividerBitmap != null) {
                if (!phoneDividerBitmap.isRecycled()) {
                    phoneDividerBitmap.recycle();
                }
                phoneDividerBitmap = null;
            }
        }
    }
}

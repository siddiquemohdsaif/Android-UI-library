# Native Views

Native Android Canvas UI components published under `com.ogfa.nativeviews`.

## Add to an Android project

Copy `native-views-release.aar` into:

```text
app/libs/native-views-release.aar
```

Then add:

```groovy
dependencies {
    implementation files('libs/native-views-release.aar')

    // AndroidX is intentionally not embedded in the AAR.
    implementation 'androidx.appcompat:appcompat:1.7.1'
}
```

Lottie, Android GIF Drawable, ReLinker, and Okio are embedded. Do not add Lottie or
Android GIF Drawable again. AppCompat and the rest of AndroidX are supplied by the
parent application so Gradle can select versions compatible with the app.

To rebuild the AAR from this repository:

```powershell
.\gradlew.bat :native-views-fat:assembleRelease
```

Output:

```text
native-views-fat/build/outputs/aar/native-views-release.aar
```

## AnimatedButton overview

`AnimatedButton` is not an Android XML `Button`. It is a controller that draws one or
more ordered `ViewLayer` objects on the `Canvas` of a host custom `View`.

It provides:

- bitmap, Lottie, GIF, dynamic Canvas, and After Effects layers;
- click and long-click hit testing;
- press/release scale animation;
- Figma-to-runtime position conversion;
- multiple overlapping buttons with topmost-first touch dispatch;
- visibility filtering for scrollable or clipped views;
- animated movement with synchronized touch bounds.

The host `View` owns one `AnimatedButtonGroup`. The group stores buttons, preserves
drawing order, dispatches touch, schedules animation frames, performs lookup, and
releases layer resources.

## Minimal bitmap button

Create buttons after the host view has a measured size. `onSizeChanged()` is a
convenient place:

```java
public final class GameCanvasView extends View {

    private final AnimatedButtonGroup buttons;
    private Bitmap playBitmap;

    public GameCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        buttons = new AnimatedButtonGroup(this);
        setClickable(true);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        buttons.clear();

        Position position = new Position(
                this,
                Position.HorizontalMarginFrom.LEFT,
                Position.VerticalMarginFrom.TOP,
                48f,
                450f
        );

        buttons.add(new AnimatedButton.Builder(
                getContext(),
                "play",
                playBitmap,
                position
        )
                .setClickListener(id -> startGame())
                .setShrink(0.92f)
                .setProxySoundPlay(() ->
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        buttons.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return buttons.onTouchEvent(event)
                || super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        buttons.release();
        super.onDetachedFromWindow();
    }
}
```

The bitmap convenience constructor creates the `BitmapView` layer internally.

## Layered button

Layers are drawn in list order. The first layer is the background and the last layer
appears on top.

```java
Position position = new Position(
        this,
        Position.HorizontalMarginFrom.RIGHT,
        Position.VerticalMarginFrom.TOP,
        200f,
        500f
);

ArrayList<ViewLayer> layers = new ArrayList<>();
layers.add(BitmapView.get(freeCoinBitmap, position));
layers.add(LottieView.get(
        getContext(),
        "emoji_lottie_1",
        position,
        freeCoinBitmap
));

buttons.add(new AnimatedButton.Builder(
        getContext(),
        "free_coin",
        layers,
        position
)
        .setClickListener(id -> openFreeCoinDialog())
        .setProxySoundPlay(this::playPopupSound));
```

For `Builder(..., layers, position)`, the largest `BitmapView` by rendered area becomes
the button's touch bounds. At least one `BitmapView` is required. Use an explicit
`RectF` builder when the button has no bitmap layer.

## AnimatedButtonGroup API

Create one group for each host custom `View`:

```java
private final AnimatedButtonGroup buttons;

public GameCanvasView(Context context, AttributeSet attrs) {
    super(context, attrs);
    buttons = new AnimatedButtonGroup(this);
}
```

Build and add in one call:

```java
AnimatedButton button = buttons.add(
        new AnimatedButton.Builder(context, "play", bitmap, position)
                .setClickListener(id -> startGame())
);
```

You can also add an already-built button:

```java
buttons.add(button);
```

IDs must be non-null, non-empty, and unique inside the group. Adding a duplicate ID
throws `IllegalArgumentException`.

The complete group API:

```java
buttons.add(builder);                 // Builds, adds, and returns the button
buttons.add(button);                  // Adds and returns an existing button

AnimatedButton play = buttons.find("play");
boolean exists = buttons.contains("play");
boolean removed = buttons.remove("play");

int count = buttons.size();
boolean empty = buttons.isEmpty();

buttons.draw(canvas);
buttons.drawVisible(canvas);
buttons.drawVisible(canvas, scrollView);

boolean consumed = buttons.onTouchEvent(event);
buttons.onScrollChanged();

boolean started = buttons.animateToPosition(
        "play", targetLeft, targetTop, 350L, onComplete
);

buttons.clear();                       // Releases layers; group remains reusable
buttons.release();                     // Lifecycle-oriented alias for clear()
buttons.close();                       // AutoCloseable alias for release()
```

`draw()` automatically calls `postInvalidateOnAnimation()` while the group is non-empty,
so animated layers and press effects continue rendering without host-view boilerplate.
For a completely static group, disable automatic frame scheduling:

```java
buttons.setAutoInvalidate(false);
boolean enabled = buttons.isAutoInvalidate();
```

With automatic invalidation disabled, call `invalidate()` on the host whenever button
state changes.

## Builder API

### Explicit runtime bounds

Use already-calculated Android pixel coordinates:

```java
RectF bounds = new RectF(left, top, right, bottom);

// One bitmap layer is created internally.
new AnimatedButton.Builder(context, "play", playBitmap, bounds);

// Multiple custom layers.
new AnimatedButton.Builder(context, "play", layers, bounds);
```

### Position-derived bounds

Use Figma-space margins and dimensions:

```java
new AnimatedButton.Builder(context, "play", playBitmap, position);
new AnimatedButton.Builder(context, "play", layers, position);
```

The bitmap overload derives bounds from that bitmap. The layered overload selects the
largest bitmap layer.

### Separate DynamicView movement bounds

```java
new AnimatedButton.Builder(
        context,
        "moving_button",
        layers,
        buttonBounds,
        dynamicLayerBounds
);
```

`buttonBounds` controls drawing and hit testing. During
`animateToPositionWithValueAnimator()`, `dynamicLayerBounds` is passed only to
`DynamicView` layers while the regular button bounds are passed to other layers.

### Builder options

```java
new AnimatedButton.Builder(context, "shop", bitmap, position)
        // Enables normal clicks.
        .setClickListener(id -> openShop())

        // Stores a click listener but explicitly enables/disables click handling.
        .setClickListener(id -> openShop(), true)

        // A press longer than 500 ms calls this listener.
        .setOnLongClickListener(id -> showShopHelp(), true)

        // Pressed scale. Default is 0.96f.
        .setShrink(0.90f)

        // Replaces the library's default button sound.
        .setProxySoundPlay(() -> soundPool.play(clickSound, 1f, 1f, 1, 0, 1f))
        .build();
```

Do not call both `setClickListener` overloads in real code; the example shows the two
available forms. When long-click is enabled, also provide a normal click listener
because a short press is dispatched to `OnClickListener`.

Recommended shrink values are between `0f` and `1f`. `1f` produces no visible shrink.

### Default button sound

When `setProxySoundPlay()` is not set, `AnimatedButton` uses the bundled:

```text
nativeviews/audio/sfx/g_button.mp3
```

The first default-sound button starts an asynchronous `SoundPool` preload. The sound is
loaded only once per process and reused by every button. If the first click arrives while
loading is still in progress, it plays immediately after loading completes.

Optional direct controls:

```java
import com.ogfa.nativeviews.audio.NativeViewsSoundPlayer;

NativeViewsSoundPlayer.preload(context);
boolean ready = NativeViewsSoundPlayer.isButtonSoundLoaded();
NativeViewsSoundPlayer.playButtonSound(context);

// Optional application-wide replacement.
NativeViewsSoundPlayer.setButtonSoundOverride(() -> appSoundManager.playButton());

// Return to the bundled sound.
NativeViewsSoundPlayer.setButtonSoundOverride(null);

// Normally needed only when the application is shutting down.
NativeViewsSoundPlayer.release();
```

A per-button `setProxySoundPlay()` callback takes priority over the bundled default.

## Position API

`Position` scales Figma measurements using:

```text
scale = runtime view width / Figma reference width
```

The default Figma reference width is `1080f`. Horizontal and vertical margins, element
width, and element height are all multiplied by the same scale.

### Host-bound position

Host-bound positions can be passed directly to layer factories:

```java
Position position = new Position(
        hostView,
        Position.HorizontalMarginFrom.RIGHT,
        Position.VerticalMarginFrom.BOTTOM,
        80f,
        120f
);
```

With a custom reference width:

```java
Position position = new Position(
        hostView,
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        120f,
        300f,
        1440f
);
```

### Unbound position

An unbound position is useful when the host view will be supplied later:

```java
Position position = new Position(
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        48f,
        450f
);

RectF bitmapBounds = position.toRectF(hostView, bitmap);
RectF customBounds = position.toRectF(hostView, 320f, 180f);
```

With a custom reference width:

```java
Position position = new Position(
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        48f,
        450f,
        1440f
);
```

### Global reference width

```java
Position.setDefaultFigmaReferenceWidth(1440f);
float currentReference = Position.getDefaultFigmaReferenceWidth();
```

This changes only constructors that do not receive an explicit reference width.

### RectF conversion overloads

```java
// Bound host + bitmap dimensions.
RectF a = position.toRectF(bitmap);

// Explicit host + bitmap dimensions.
RectF b = position.toRectF(hostView, bitmap);

// Bound host + explicit Figma element dimensions.
RectF c = position.toRectF(320f, 180f);

// Explicit host + explicit Figma element dimensions.
RectF d = position.toRectF(hostView, 320f, 180f);

// No View object; useful in layout calculations and tests.
RectF e = position.toRectF(
        runtimeWidth,
        runtimeHeight,
        figmaElementWidth,
        figmaElementHeight
);
```

Host width and height must be greater than zero, so evaluate positions after measurement.

## ViewLayer API

Every layer implements:

```java
public interface ViewLayer {
    void onDraw(Canvas canvas);
    void clear();
    void setRect(RectF rectF);
}
```

- `onDraw()` renders the layer.
- `clear()` releases or clears owned resources.
- `setRect()` receives new bounds when a button moves.

You can create a custom layer:

```java
public final class BadgeLayer implements ViewLayer {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF bounds;

    public BadgeLayer(RectF bounds) {
        this.bounds = new RectF(bounds);
        paint.setColor(Color.RED);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawOval(bounds, paint);
    }

    @Override
    public void clear() {
        // Release layer-owned resources here.
    }

    @Override
    public void setRect(RectF rectF) {
        bounds = new RectF(rectF);
    }
}
```

## BitmapView

```java
BitmapView layer1 = BitmapView.get(bitmap, bounds);
BitmapView layer2 = BitmapView.get(bitmap, position);

// Equivalent public constructor:
BitmapView layer3 = new BitmapView(bitmap, bounds);
```

The `Position` overload treats the bitmap's pixel width and height as its Figma-space
dimensions. `setRect()` updates the bitmap's rendered bounds.

## LottieView

Place JSON animations in:

```text
app/src/main/assets/lottie/json/animation_id.json
```

If a composition references external images, put them in the parent application's
assets. The resolver checks the path from the JSON first, followed by:

```text
app/src/main/assets/images/
app/src/main/assets/lottie/images/
```

Preload one animation asynchronously. Names work with or without `.json`:

```java
LottieViewAnimator.preloadAnimations(context, "loading");
LottieViewAnimator.preloadAnimations(context, "loading.json");
```

Preload multiple animations in one call:

```java
LottieViewAnimator.preloadAnimations(
        context,
        "loading",
        "reward.json",
        "celebration"
);
```

Check whether an animation has finished preloading:

```java
boolean ready = LottieViewAnimator.isLoaded("loading.json");
```

Create a layer:

```java
// Explicit bounds; repeats forever by default.
LottieView.get(context, "loading", bounds);

// Explicit repeat behavior: true = forever, false = play once.
LottieView.get(context, "loading", bounds, false);

// Position + intrinsic composition dimensions.
LottieView.get(context, "loading", position);
LottieView.get(context, "loading", position, false);

// Position + dimensions copied from a bitmap.
LottieView.get(context, "loading", position, buttonBitmap);

// Position + explicit Figma dimensions.
LottieView.get(context, "loading", position, 240f, 240f);
```

All overloads use the same normalized animation ID and shared load operation. A layer
uses the memory cache first, waits for an existing preload when one is in flight, or
loads once from `assets/lottie/json/` when no preload was started. Missing JSON,
invalid JSON, and unresolved external images produce a runtime exception that includes
the animation or image name and searched asset paths.

Call `buttons.release()` when the host is detached. This clears each layer's animation
source list and drawable resources.

Current behavior: `LottieView.setRect()` does not relocate its drawable. If a button will
use `animateToPositionWithValueAnimator()`, use a custom movable layer or update the
library implementation before expecting a Lottie layer to follow the button.

## GIFView

Place application-owned GIF files under:

```text
app/src/main/assets/gif/
```

Preload one GIF asynchronously:

```java
GIFViewAnimator.preloadAnimations(context, "reward");

// ".gif" is also accepted.
GIFViewAnimator.preloadAnimations(context, "reward.gif");
```

Preload multiple GIFs:

```java
GIFViewAnimator.preloadAnimations(
        context,
        "reward",
        "celebration",
        "daily_bonus.gif"
);
```

Check the cache and create a preloaded layer:

```java
boolean ready = GIFViewAnimator.isLoaded("reward");
ViewLayer gifLayer = GIFView.get("reward", gifBounds);
```

If the GIF was not explicitly preloaded, use the context overload:

```java
ViewLayer gifLayer = GIFView.get(
        context,
        "reward",
        gifBounds
);
```

It checks the cache first and otherwise loads `assets/gif/reward.gif` from disk once.
If the asset is absent or invalid, it throws `IllegalStateException` with the attempted
asset path. The no-context overload throws when the requested GIF is not already cached.

An application-created composition is also supported:

```java
GIFComposition composition =
        GIFComposition.fromAssetSync(context, "gif/reward.gif");

ViewLayer gifLayer =
        GIFView.get("reward", composition, gifBounds);
```

Current behavior: GIF layers loop indefinitely and `GIFView.setRect()` does not relocate
them during button movement.

## DynamicView

`DynamicView` renders application-defined Canvas content with progress from `0f` to `1f`:

```java
CustomDynamicView pulse = new CustomDynamicView() {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    public void onDraw(Canvas canvas, float progress, RectF rectF) {
        paint.setAlpha((int) (255f * (1f - progress)));
        canvas.drawCircle(
                rectF.centerX(),
                rectF.centerY(),
                rectF.width() * 0.5f * progress,
                paint
        );
    }

    @Override
    public long getDuration() {
        return 700L;
    }
};

ViewLayer pulseLayer = DynamicView.get(pulse, pulseBounds);
```

`DynamicView.get()` repeats indefinitely. Its `setRect()` implementation updates the
animation bounds, so it follows `animateToPositionWithValueAnimator()`.

For direct control:

```java
DynamicViewAnimator animator =
        new DynamicViewAnimator(pulse, -1, pulseBounds);

DynamicView layer = new DynamicView(animator);
boolean running = animator.isAnimating();
animator.setRect(newBounds);
```

`repeatCount == -1` means infinite repetition.

## AfterEffectView

Wrap an existing `AfterEffectAnimator` as a button layer:

```java
AfterEffectAnimator effectAnimator = new AfterEffectAnimator(
        animationWindow,
        effectLayers,
        1_000L,
        true
);

ViewLayer effectLayer = AfterEffectView.get(effectAnimator);
```

Calling `clear()` empties the animator's layer list. `setRect()` currently does nothing,
so an After Effects layer does not automatically follow button movement.

## Drawing and visibility API

Draw every button:

```java
buttons.draw(canvas);
```

Draw only buttons intersecting a view's local visible rectangle:

```java
buttons.drawVisible(canvas, scrollView);
```

Draw one button directly:

```java
button.onDraw(canvas);
```

`AnimatedButtonGroup` requests the next frame automatically unless auto-invalidation is
disabled.

## Touch API

Forward events for a collection:

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
    return buttons.onTouchEvent(event)
            || super.onTouchEvent(event);
}
```

Buttons are checked from the end of the list to the beginning, so the last added
overlapping button receives touch first.

Forward to one button:

```java
boolean consumed = button.onTouchEvent(event);
```

Cancel pressed state when a parent scroll begins:

```java
buttons.onScrollChanged();
```

`ACTION_CANCEL` is handled by the group and clears pressed state. The underlying button
handles `ACTION_DOWN`, `ACTION_MOVE`, and `ACTION_UP`. Moving outside the button before
release also cancels pressed state.

## Collection and lookup API

```java
buttons.add(button);

AnimatedButton found = buttons.find("free_coin");
boolean exists = buttons.contains("free_coin");

boolean removed = buttons.remove("free_coin");

buttons.clear();
buttons.release();
```

`remove()` clears every layer owned by the removed button. `find()` returns `null` when
no match exists.

## Read button state

```java
String id = button.getId();
int initialLeft = button.getLeft();
int initialTop = button.getTop();
RectF currentBounds = new RectF(button.rectF);
ArrayList<ViewLayer> layers = button.viewLayers;
```

`getLeft()` and `getTop()` are the integer coordinates captured at build time.
`rectF` contains the current bounds and changes during movement.

## Move a button

```java
boolean started = buttons.animateToPosition(
        "free_coin",
        targetLeft,
        targetTop,
        350L,
        () -> Log.d("AnimatedButton", "Movement complete")
);
```

Parameters:

- `targetLeft`, `targetTop`: destination in runtime pixels;
- `duration`: animation time in milliseconds;
- `onComplete`: invoked when movement finishes; may be `null`.

The group supplies its host view to the underlying animation. The method returns `false`
when the ID is not present. Movement uses linear interpolation and updates drawing
bounds, pressed bounds, and the touch region on every frame.

Layer movement depends on `ViewLayer.setRect()`:

- `BitmapView`: moves;
- `DynamicView`: moves;
- `LottieView`: currently does not move;
- `GIFView`: currently does not move;
- `AfterEffectView`: currently does not move;
- custom layers: move when their `setRect()` implementation updates their bounds.

## Public AnimatedButton API summary

| API | Purpose |
|---|---|
| `Builder(context, id, layers, RectF)` | Layered button with explicit bounds |
| `Builder(context, id, bitmap, RectF)` | Simple bitmap button with explicit bounds |
| `Builder(context, id, layers, Position)` | Layered button bounded by its largest bitmap |
| `Builder(context, id, bitmap, Position)` | Simple position-based bitmap button |
| `Builder(context, id, layers, RectF, dynamicRectF)` | Separate movement bounds for dynamic layers |
| `setClickListener(listener)` | Set and enable click handling |
| `setClickListener(listener, enabled)` | Set listener and explicit click state |
| `setOnLongClickListener(listener, enabled)` | Configure presses longer than 500 ms |
| `setShrink(scale)` | Configure pressed scale; default `0.96f` |
| `setProxySoundPlay(runnable)` | Replace default click sound |
| `build()` | Create the button |
| `viewLayers` | Public ordered layer list |
| `rectF` | Public current drawing and touch bounds |
| `getId()` | Return the button ID |
| `getLeft()`, `getTop()` | Return initial integer position |
| `onDraw(canvas)` | Draw one button |
| `onTouchEvent(event)` | Handle touch for one button |
| `animateToPositionWithValueAnimator(...)` | Animate position and touch bounds |
| `Draw(canvas, buttons)` | Draw a collection |
| `visibleDraw(canvas, buttons, view)` | Draw the visible collection subset |
| `getVisible(source, output, view)` | Collect visible buttons |
| `HandleTouch(event, buttons)` | Dispatch touch in reverse drawing order |
| `HandleTouchScrollChanged(buttons)` | Cancel pressed state during scrolling |
| `addButton(list, button)` | Add a button |
| `removeButton(id, list)` | Clear and remove a button |
| `findButtonById(id, list)` | Find a button or return `null` |
| `releaseLottieResources(list)` | Call `clear()` on all layers |

Callback contracts:

```java
AnimatedButton.OnClickListener clickListener =
        id -> Log.d("Button", "Clicked: " + id);

AnimatedButton.OnLongClickListener longClickListener =
        id -> Log.d("Button", "Long clicked: " + id);
```

---

## Text bitmap generation and composition

These APIs only generate or compose `Bitmap` objects. They do not maintain a separate
drawing, touch, or animation system. Use the resulting bitmap directly on a `Canvas`,
inside a `BitmapView`, or as the bitmap of an `AnimatedButton`.

### Bitmap-font characters

These utilities use a `Map<String, Bitmap>` containing one bitmap per character:

```java
Bitmap text = TextMakerEngine.generateTextBitmap(
        glyphs, "PLAYER 1", 72
);

Bitmap spaced = TextMakerEngine.generateTextBitmapWithSpacing(
        glyphs, "PLAYER 1", 72, 4
);

ArrayList<String> lines = TextMakerEngine.textLineEvaluator(
        glyphs, message, 72, 4, 600
);

Bitmap composed = TextWriter.writeTextToBitmap(
        background, textElements
);

TextWriter.writeTextToBitmapByOverWrite(
        mutableBackground, textElements
);

Bitmap withImages = ImageWriter.writeImage(
        background, imageElements
);
```

Make the generated image clickable by passing it to `AnimatedButton`:

```java
Bitmap textImage = TextMakerEngine.generateTextBitmapWithSpacing(
        glyphs,
        "PLAYER 1",
        72,
        4
);

buttons.add(new AnimatedButton.Builder(
        getContext(),
        "profile_name",
        textImage,
        position
)
        .setClickListener(id -> openProfile())
        .setShrink(0.92f));
```

The button group now owns drawing, press animation, touch handling, sound, movement,
and cleanup. No second text-animator collection is required.

`TextWriter.writeTextToBitmapWithSpacing()` and
`writeTextToBitmapWithSpacingWidthReduction()` are also available. Alpha arguments use
the `0f..1f` range and are clamped. Invalid dimensions, recycled bitmaps, or text with
no characters in the supplied glyph map produce clear runtime exceptions.

### Android font-resource generation and composition

`TextWriterNative` renders ordinary Android `R.font` resources. Despite its historical
name, it does not use JNI. It returns a normal bitmap that can be drawn directly or
passed to `AnimatedButton`.

Place the font in the consuming application:

```text
app/src/main/res/font/game_font.ttf
```

Render centered text onto a copied background:

```java
ArrayList<TextWriterNative.ElementWriter> writers = new ArrayList<>();
writers.add(new TextWriterNative.ElementWriter(
        R.font.game_font,
        "PLAY NOW",
        background.getWidth() / 2f,
        24f,
        72f,
        TextWriterNative.LineType.MIDDLE,
        1f,
        "#FFFFFF"
));

Bitmap result = TextWriterNative.writeTextToBitmap(
        context,
        background,
        writers
);

buttons.add(new AnimatedButton.Builder(
        context,
        "play",
        result,
        position
)
        .setClickListener(id -> startGame()));
```

Available operations:

```java
// Return a new bitmap.
TextWriterNative.writeTextToBitmap(context, background, writers);
TextWriterNative.writeTextToBitmapWithSpacing(context, background, spacedWriters);

// Modify an existing mutable bitmap.
TextWriterNative.writeTextToBitmapByOverWrite(context, mutableBitmap, writers);
TextWriterNative.writeTextToBitmapWithSpacingByOverWrite(
        context, mutableBitmap, spacedWriters
);

// Preserve the left/right edge caps while reducing background width.
TextWriterNative.writeTextToBitmapWithSpacingWidthReduction(
        context, background, spacedWriters
);

// Return matching [top, middle, bottom] sections with a minimum width.
TextWriterNative.writeTextToBitmapWithSpacingWidthReductionWithLimit(
        context, minWidth, middle, top, bottom, spacedWriters
);
```

`TextMakerEngineNative.generateTextBitmap()`,
`generateTextBitmapWithSpacing()`, and `textLineEvaluator()` are available for direct
font rendering and line wrapping. The SDK embeds the middle-removal logic and does not
depend on Carrom's `BitmapUtil` or `BoardProperty`. AndroidX Core is compile-only and
remains supplied by the parent application through AppCompat.

---

## NativeTextField

`NativeTextField` is a single-line, Canvas-rendered editor. It uses Android native font
resources and communicates with the Android keyboard through `InputConnection`.
`NativeTextFieldGroup` owns field order, focus, touch dispatch, keyboard visibility,
cursor blinking, IME composition, and cleanup.

The field renders text directly with `TextPaint`; it does not regenerate a bitmap after
each keystroke. Tapping positions the cursor at the nearest character, and dragging
horizontally continuously moves the cursor while keeping it visible in long text.

### Font selection

Font configuration is optional. If no font method is called, the field uses Android's
`Typeface.DEFAULT`:

```java
textFields.add(
        new NativeTextField.Builder(context, "player_name", bounds)
                .setHint("Player name")
);
```

The default can also be selected explicitly:

```java
.useDefaultFont()
// or
.setFont(Typeface.DEFAULT)
```

For a custom font, store it in the consuming application:

Store the font in the consuming application:

```text
app/src/main/res/font/game_font.ttf
```

Pass it using the generated resource ID:

```java
.setFont(R.font.game_font)
```

### Create a field

Create the group once for the host custom view:

```java
private final NativeTextFieldGroup textFields;

public GameCanvasView(Context context, AttributeSet attrs) {
    super(context, attrs);
    textFields = new NativeTextFieldGroup(this);
}
```

Create fields after the host has a measured size:

```java
@Override
protected void onSizeChanged(
        int width,
        int height,
        int oldWidth,
        int oldHeight
) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    textFields.clear();

    Position position = new Position(
            this,
            Position.HorizontalMarginFrom.LEFT,
            Position.VerticalMarginFrom.TOP,
            180f,
            500f
    );

    NativeTextField playerName = textFields.add(
            new NativeTextField.Builder(
                    getContext(),
                    "player_name",
                    position,
                    720f,
                    120f
            )
                    .setFont(R.font.game_font)
                    .setHint("Enter player name")
                    .setText("Player")
                    .setMaxLength(20)
                    .setInputType(
                            InputType.TYPE_CLASS_TEXT
                                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    )
                    .setImeOptions(EditorInfo.IME_ACTION_DONE)
                    .setTextColor(Color.WHITE)
                    .setHintColor(0x88ffffff)
                    .setCursorColor(0xffffd166)
                    .setSelectionColor(0x6690e0ef)
                    .setBackgroundColor(0xff182840, 0xff203a5c)
                    .setStrokeColor(0xff54708f, 0xff90e0ef)
                    .setOnTextChangedListener((id, text) ->
                            savePlayerName(text))
                    .setOnEditorActionListener((id, action) -> {
                        startGame();
                        return true;
                    })
    );
}
```

The `Position` constructor uses Figma-space width and height. Explicit runtime bounds
are also supported:

```java
new NativeTextField.Builder(context, "search", new RectF(left, top, right, bottom))
```

### Connect the host view

Drawing and touch:

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    buttons.draw(canvas);
    textFields.draw(canvas);
}

@Override
public boolean onTouchEvent(MotionEvent event) {
    return textFields.onTouchEvent(event)
            || buttons.onTouchEvent(event)
            || super.onTouchEvent(event);
}
```

Android keyboard connection:

```java
@Override
public boolean onCheckIsTextEditor() {
    return textFields.hasFocusedField();
}

@Override
public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    InputConnection connection =
            textFields.onCreateInputConnection(outAttrs);
    return connection != null
            ? connection
            : super.onCreateInputConnection(outAttrs);
}

@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    return textFields.onKeyDown(keyCode, event)
            || super.onKeyDown(keyCode, event);
}
```

Lifecycle cleanup:

```java
@Override
protected void onDetachedFromWindow() {
    textFields.release();
    buttons.release();
    super.onDetachedFromWindow();
}
```

### Field API

Builder configuration:

| API | Purpose |
|---|---|
| No font call | Use Android's `Typeface.DEFAULT` |
| `useDefaultFont()` | Explicitly select Android's default typeface |
| `setFont(Typeface)` | Use an Android `Typeface` instance |
| `setFont(R.font.name)` | Load an Android font resource |
| `setHint(text)` | Set empty-field hint |
| `setText(text)` | Set initial text |
| `setMaxLength(length)` | Enforce maximum UTF-16 length |
| `setInputType(flags)` | Configure keyboard type |
| `setImeOptions(flags)` | Configure Done, Next, Search, Send, or Go |
| `setPassword(value)` | Mask rendered text |
| `setTextColor(color)` | Set entered-text color |
| `setHintColor(color)` | Set hint color |
| `setCursorColor(color)` | Set cursor color |
| `setSelectionColor(color)` | Set selection highlight |
| `setBackgroundColor(normal, focused)` | Configure background states |
| `setStrokeColor(normal, focused)` | Configure border states |
| `setTextSize(px)` | Set runtime text size |
| `setPadding(horizontalPx, verticalPx)` | Set internal padding |
| `setCornerRadius(px)` | Set background radius |
| `setStrokeWidth(px)` | Set border width |
| `setCursorWidth(px)` | Set cursor width |
| `setEnabled(value)` | Enable or disable editing |
| `setOnTextChangedListener(listener)` | Observe committed/composing changes |
| `setOnEditorActionListener(listener)` | Handle keyboard actions |
| `setOnFocusChangedListener(listener)` | Observe focus changes |

Runtime operations:

```java
playerName.getText();
playerName.setText("Player 2");
playerName.clear();
playerName.setSelection(0, playerName.getText().length());
playerName.requestFocus();
playerName.clearFocus();
playerName.setPassword(true);
playerName.setEnabled(false);

textFields.find("player_name");
textFields.remove("player_name");
textFields.clearFocus();
textFields.clear();
```

The input connection supports committed text, composing text, cursor selection,
surrounding-text deletion, clipboard copy/cut/paste, hardware keys, IME actions, and
focus-next behavior.

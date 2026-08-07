# Button

`Button` is an image-backed Canvas component with an optional native `Text`
label. It is one component in `ZLayer`, one draw unit, and one touch target.

```text
Button
├── Image (required, drawn first)
└── Text  (optional, drawn above Image)
```

The button privately owns both children. Child IDs such as `play:image` and
`play:text` are implementation details and are not registered in
`ZLayerGroup`. Never add a transferred child separately.

## Recommended creation

Create the image and text internally:

```java
Button play = content.add(new Button.Builder(
        getContext(),
        "play",
        playBitmap,
        "PLAY NOW",
        position,
        new Size(900f, 240f)
)
        .setTextInsets(TextInsets.of(40f, 20f, 40f, 20f))
        .setCornerRadius(36f)
        .setTextSize(72f)
        .setTextColor(Color.WHITE)
        .setFont(NativeFonts.INTER)
        .setFontVariations(FontVariation.BOLD)
        .setPressedScale(0.92f)
        .setPressAnimationDuration(100L)
        .setOnClickListener(id -> startGame()));
```

`Position + Size` values and unsuffixed visual dimensions are Figma-space
measurements. They scale from the captured `FigmaConfig`.

Every Button shrinks by 8% while pressed by default. In other words, the
default pressed scale is `0.92f` with a `100 ms` down/up animation. Override
either value when a different interaction is needed:

```java
.setPressedScale(0.88f)
.setPressAnimationDuration(140L)
```

Use `1f` to disable shrinking while retaining normal click handling.

Ripple is separate from shrinking and is disabled by default. Enable the
bounded Android-style wave explicitly:

```java
.setRippleEnabled(true)
.setRippleColor(0x33ffffff)
.setRippleDuration(320L)
.setRippleOrigin(Button.RippleOrigin.TOUCH)
.setRippleRadiusAuto()
```

The wave is drawn above the background Image and below the Text. It is clipped
by the Button region and rounded corners. Automatic radius reaches the
farthest corner from the selected origin. A released or cancelled ripple
finishes with a fade, and rapid presses may overlap naturally.

Use a fixed radius when required by the design:

```java
.setRippleRadius(180f)       // Figma units
.setRippleRadiusPx(180f)     // exact runtime pixels
.setRippleOrigin(Button.RippleOrigin.CENTER)
```

Use runtime-pixel bounds instead:

```java
Button play = content.add(new Button.Builder(
        getContext(),
        "play",
        playBitmap,
        "PLAY NOW",
        new RectF(40f, 300f, 360f, 420f)
));
```

`RectF` defines exact runtime bounds, but it does not change styling units.
Unsuffixed styling methods still use Figma units. Use methods ending in `Px`
for exact runtime pixels.

## Solid-color background

A Button can create and privately own its background without a bitmap:

```java
Button play = content.add(new Button.Builder(
        context,
        "play",
        0xff0057b8,
        "PLAY",
        position,
        new Size(900f, 240f)
));
```

Color-background overloads also support `RectF` and image-only buttons.

## Image-only button

```java
Button icon = content.add(new Button.Builder(
        getContext(),
        "settings",
        settingsBitmap,
        position,
        new Size(120f, 120f)
)
        .setImageScaleType(Image.ScaleType.FIT_XY)
        .setOnClickListener(id -> openSettings()));
```

A label can be created later:

```java
icon.setLabel("!")
        .setTextSize(64f)
        .setTextColor(Color.WHITE);

icon.removeText();
```

## Supply existing components

Use this path for independently configured child visuals:

```java
Image background = new Image.Builder(
        context,
        "temporary_background_id",
        bitmap,
        bounds
)
        .setScaleType(Image.ScaleType.CENTER_CROP)
        .build(hostView);

Text label = new Text.Builder(
        context,
        "temporary_label_id",
        "CUSTOM",
        textBounds
)
        .setAlignment(Text.Alignment.CENTER)
        .setVerticalAlignment(Text.VerticalAlignment.CENTER)
        .build(hostView);

Button custom = content.add(new Button.Builder(
        context,
        "custom",
        background,
        label
)
        .setOnClickListener(id -> openCustomScreen()));
```

The Image determines the complete button bounds. Supplied Text bounds must be
fully contained by the Image bounds. Creating the Button transfers both
components to its private host; adding either child to `ZLayer` before or after
the transfer is invalid.

Image-only supplied-component construction is also supported:

```java
new Button.Builder(context, "custom_icon", background);
```

Configure supplied children directly before constructing the button.
Image/text builder styling methods deliberately reject supplied-child mode so
configuration precedence cannot be ambiguous.

## TextInsets

Insets reduce the label's usable region without moving or resizing the button:

```java
TextInsets.none();
TextInsets.all(12f);
TextInsets.horizontal(20f);
TextInsets.vertical(10f);
TextInsets.of(16f, 10f, 16f, 8f);
```

For a `100 x 100` button and top inset `10`, the label region is `100 x 90`:

```text
button = [left=0, top=0, right=100, bottom=100]
text   = [left=0, top=10, right=100, bottom=100]
```

The internal calculation is:

```java
textLeft   = button.left   + leftInset;
textTop    = button.top    + topInset;
textRight  = button.right  - rightInset;
textBottom = button.bottom - bottomInset;
```

Insets that leave zero or negative width/height throw a descriptive
`IllegalArgumentException`.

The receiving method selects the unit:

```java
.setTextInsets(TextInsets.all(20f))   // Figma units
.setTextInsetsPx(TextInsets.all(20f)) // exact runtime pixels
```

## Complete construction API

```java
// Supplied Image, with optional supplied Text
new Button.Builder(context, id, image);
new Button.Builder(context, id, image, text);

// Internally created Image, with optional internally created Text
new Button.Builder(context, id, bitmap, position, size);
new Button.Builder(context, id, bitmap, label, position, size);
new Button.Builder(context, id, bitmap, rectF);
new Button.Builder(context, id, bitmap, label, rectF);

// Internally created solid-color Image
new Button.Builder(context, id, color, position, size);
new Button.Builder(context, id, color, label, position, size);
new Button.Builder(context, id, color, rectF);
new Button.Builder(context, id, color, label, rectF);
```

Internal visual configuration:

```java
.setTextInsets(insets)
.setTextInsetsPx(insets)
.setImageScaleType(Image.ScaleType.FIT_XY)
.setFilterBitmap(true)
.setTextStyle(textStyle)
.setTextSize(72f)
.setTextSizePx(48f)
.setTextLetterSpacing(2f)
.setTextLetterSpacingPx(2f)
.setTextLineSpacing(8f)
.setTextLineSpacingPx(8f)
.setTextPadding(12f, 6f)
.setTextPaddingPx(12f, 6f)
.setTextShadow(4f, 0f, 3f, 0xaa000000)
.setTextShadowPx(4f, 0f, 3f, 0xaa000000)
.clearTextShadow()
.setTextColor(Color.WHITE)
.setTextAlpha(0.8f)
.setTextAlignment(Text.Alignment.CENTER)
.setTextVerticalAlignment(Text.VerticalAlignment.CENTER)
.useDefaultFont()
.setFont(R.font.game_font)
.setFont(NativeFonts.INTER)
.setFontAsset("fonts/game_font.ttf")
.setFont(typeface)
.setFontVariations(FontVariation.BOLD)
```

Button configuration:

```java
.horizontalCenter(true)
.verticalCenter(true)
.setCornerRadius(36f)
.setCornerRadiusPx(24f)
.setPressedScale(0.92f)
.setPressAnimationDuration(100L)
.setRippleEnabled(true)
.setRippleColor(0x33ffffff)
.setRippleDuration(320L)
.setRippleOrigin(Button.RippleOrigin.TOUCH)
.setRippleRadiusAuto()
.setAlpha(0.8f)
.setVisible(true)
.setEnabled(true)
.setOnClickListener(id -> handleClick(id))
```

Component alpha multiplies the already configured Image and Text alpha without
overwriting either child value.

Centering moves the complete Button composite—including its Image, Text,
clipping path, and touch region—inside the owning ZLayer. The axes are
independent. A root layer uses the host view; a Card content layer uses the
Card. Disabling an axis restores the source `Position` or `RectF`.

## Corner radius

The radius clips the complete Button composite, including both Image and Text:

```java
.setCornerRadius(36f)
```

`36f` is always a Figma-space value converted with the Button's captured
`FigmaConfig`, including when the region is a `RectF`.

```java
.setCornerRadiusPx(36f)
```

The `Px` form always remains exactly 36 runtime pixels. The resolved radius is
limited to half the shortest button side, so oversized values safely form a
pill or circle. `setRegion()` automatically recalculates the radius. The
rounded clipping `Path` is cached and rebuilt only after a region or radius
change.

## Runtime API

```java
button.getId();
button.getBounds();
button.getImage();
button.getText();       // null for image-only
button.hasText();
button.getTextInsets();
button.areTextInsetsInPixels();
button.getFigmaConfig();
button.getAlpha();
button.getPressedScale();
button.getCurrentPressedScale();
button.getPressAnimationDuration();
button.isPressed();
button.isRippleEnabled();
button.getRippleColor();
button.getRippleDuration();
button.getRippleOrigin();
button.getRippleRadius();
button.getResolvedRippleRadius();
button.isRippleRadiusAuto();
button.isRippleRadiusInPixels();
button.getCornerRadius();
button.getResolvedCornerRadius();
button.isCornerRadiusInPixels();
button.isVisible();
button.isEnabled();
button.isClickable();
button.isHorizontalCentered();
button.isVerticalCentered();

button.setBitmap(newBitmap);
button.setBackgroundColor(0xff0057b8);
button.setLabel("CONTINUE");
button.removeText();

button.setRegion(position, size);
button.setRegion(rectF);
button.setHorizontalCenter(true);
button.setVerticalCenter(true);
button.horizontalCenter(false);
button.verticalCenter(false);
button.setTextInsets(TextInsets.all(12f));
button.setTextInsetsPx(TextInsets.all(12f));

button.setImageScaleType(Image.ScaleType.CENTER_CROP);
button.setFilterBitmap(true);
button.setCornerRadius(36f);
button.setCornerRadiusPx(24f);
button.setPressedScale(0.92f);
button.setPressAnimationDuration(100L);
button.setRippleEnabled(true);
button.setRippleColor(0x33ffffff);
button.setRippleDuration(320L);
button.setRippleOrigin(Button.RippleOrigin.TOUCH);
button.setRippleRadius(180f);
button.setRippleRadiusPx(180f);
button.setRippleRadiusAuto();

button.setTextSize(60f);
button.setTextSizePx(44f);
button.setTextLetterSpacing(2f);
button.setTextLetterSpacingPx(2f);
button.setTextLineSpacing(8f);
button.setTextLineSpacingPx(8f);
button.setTextPadding(12f, 6f);
button.setTextPaddingPx(12f, 6f);
button.setTextShadow(4f, 0f, 3f, 0xaa000000);
button.setTextShadowPx(4f, 0f, 3f, 0xaa000000);
button.clearTextShadow();
button.setTextColor(Color.WHITE);
button.setTextAlpha(0.75f);
button.setTextAlignment(Text.Alignment.CENTER);
button.setTextVerticalAlignment(Text.VerticalAlignment.CENTER);
button.setFont(NativeFonts.INTER);
button.setFontAsset("fonts/game_font.ttf");
button.setFont(typeface);
button.useDefaultFont();
button.setFontVariations(FontVariation.SEMI_BOLD);
button.clearFontVariations();

button.setAlpha(0.7f);
button.setVisible(false);
button.setEnabled(false);
button.setOnClickListener(listener);
button.removeOnClickListener();
```

Text-specific runtime methods require a label. Call `setLabel()` first for an
image-only button.

Changing the button region always assigns the complete region to Image and the
inset-adjusted region to Text.

## Drawing, touch, and lifecycle

`ZLayerGroup` handles all three:

```java
private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer content = ui.addLayer("content");

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
}
```

A click fires only when down and up happen inside the button. `ACTION_DOWN`
animates the complete Button—Image, Text, and clipping—toward its pressed
scale around the Button center. `ACTION_UP`, moving outside, disabling,
hiding, removing the listener, or receiving `ACTION_CANCEL` animates it back
to `1f`. Moving outside permanently cancels that gesture. Hidden, disabled,
or listener-free buttons ignore touch.

Releasing the button releases its private components but never recycles the
caller-provided Bitmap.

## Validation

The API rejects:

- blank button IDs;
- null, recycled, or dimensionless bitmaps;
- invalid or empty regions;
- negative or non-finite insets;
- insets that consume the text region;
- supplied Text outside supplied Image;
- child components already owned by a different host;
- alpha outside `0..1`;
- pressed scale outside `(0, 1]`;
- negative press-animation duration;
- non-positive or non-finite ripple radius;
- negative ripple duration;
- text runtime styling when the button currently has no Text.

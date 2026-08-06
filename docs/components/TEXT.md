# Text

`Text` is a non-editable Android Canvas component rendered directly with
`TextPaint` and `StaticLayout`. It supports native font shaping, multiline layout,
alignment, ellipsizing, reusable styles, runtime updates, and an optional lightweight
click callback without creating a bitmap.

Package:

```java
import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;
import com.ogfa.nativeviews.text.TextStyle;
```

## Region contract

Every `Text` requires one of the SDK's two region forms:

```java
// Figma/design-space coordinates
new Text.Builder(context, id, value, position, size);

// Final runtime-pixel coordinates
new Text.Builder(context, id, value, rectF);
```

`Position` defines anchors and margins. `Size` defines width and height:

```java
Position position = new Position(
        hostView,
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        90f,
        160f
);

Size size = new Size(900f, 120f);
```

With the default Figma reference width of `1080`, margins, width, height, text size,
padding, spacing, and shadow dimensions are multiplied by:

```text
scale = runtime host width / 1080
```

Device height does not determine the scale. It is only used when resolving a
bottom-anchored position.

With a `RectF` region, dimensional style values are runtime pixels.
`setTextSizePx()` always uses exact runtime pixels regardless of region type.

## Minimal usage

Create one group for the custom host view:

```java
private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer texts = ui.addLayer("text");
```

Build text after the view has a measured size:

```java
Text title = texts.add(
        new Text.Builder(
                getContext(),
                "screen_title",
                "CHOOSE PLAYER",
                position,
                new Size(900f, 120f)
        )
                .setTextSize(72f)
                .setTextColor(Color.WHITE)
                .setAlignment(Text.Alignment.CENTER)
                .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                .setFont(R.font.game_font)
);
```

Draw the group:

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ui.draw(canvas);
}
```

Release it with the host:

```java
@Override
protected void onDetachedFromWindow() {
    ui.release();
    super.onDetachedFromWindow();
}
```

## Complete host example

```java
public final class GameCanvasView extends View {

    private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer texts = ui.addLayer("text");

    public GameCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        texts.clear();

        Position position = new Position(
                this,
                Position.HorizontalMarginFrom.LEFT,
                Position.VerticalMarginFrom.TOP,
                90f,
                160f
        );

        texts.add(
                new Text.Builder(
                        getContext(),
                        "title",
                        "NATIVE TEXT",
                        position,
                        new Size(900f, 120f)
                )
                        .setTextSize(72f)
                        .setTextColor(Color.WHITE)
                        .setAlignment(Text.Alignment.CENTER)
                        .setVerticalAlignment(
                                Text.VerticalAlignment.CENTER
                        )
                        .setOnClickListener(id -> openTitle())
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ui.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return ui.onTouchEvent(event)
                || super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        ui.release();
        super.onDetachedFromWindow();
    }
}
```

Touch forwarding is required only when at least one text has an
`OnClickListener`.

## Explicit RectF

```java
Text status = texts.add(
        new Text.Builder(
                getContext(),
                "status",
                "READY",
                new RectF(60f, 240f, 1020f, 360f)
        )
                .setTextSizePx(54f)
                .setAlignment(Text.Alignment.CENTER)
                .setVerticalAlignment(Text.VerticalAlignment.CENTER)
);
```

Both constructor paths resolve to the same internal `RectF`, used for clipping,
alignment, drawing, and hit testing.

## Fonts

Android default:

```java
.useDefaultFont()
```

Font bundled inside Native Views:

```java
import com.ogfa.nativeviews.font.NativeFonts;

.setFont(NativeFonts.LILITA_ONE)
```

Bundled choices include Inter, Montserrat, Roboto, their italic variants, and
Lilita One. The consuming application does not need to copy these files into
`res/font`. See [FONTS.md](../utilities/FONTS.md).

Named variable-font weight:

```java
.setFont(NativeFonts.INTER)
.setFontVariations(FontVariation.SEMI_BOLD)
```

Available presets are `THIN`, `EXTRA_LIGHT`, `LIGHT`, `REGULAR`, `MEDIUM`,
`SEMI_BOLD`, `BOLD`, `EXTRA_BOLD`, and `BLACK`. Unsupported variation axes are
ignored, so ordinary fonts render normally. API 24 and 25 also retain normal
font rendering.

Android font resource:

```java
.setFont(R.font.game_font)
```

Font in the consuming application's assets:

```java
.setFontAsset("fonts/game_font.ttf")
```

Existing typeface:

```java
.setFont(Typeface.create(
        "sans-serif-medium",
        Typeface.NORMAL
))
```

Resource and asset typefaces are cached process-wide. Missing or invalid fonts throw
an `IllegalArgumentException` that identifies the resource ID or asset path.

## Alignment and overflow

Horizontal alignment:

```java
Text.Alignment.START
Text.Alignment.CENTER
Text.Alignment.END
```

Vertical alignment:

```java
Text.VerticalAlignment.TOP
Text.VerticalAlignment.CENTER
Text.VerticalAlignment.BOTTOM
```

Overflow:

```java
Text.Overflow.CLIP
Text.Overflow.ELLIPSIZE_START
Text.Overflow.ELLIPSIZE_MIDDLE
Text.Overflow.ELLIPSIZE_END
```

Start and middle ellipsizing require `maxLines = 1`. Invalid multiline
combinations throw `IllegalArgumentException`.

Multiline example:

```java
Text description = texts.add(
        new Text.Builder(
                getContext(),
                "description",
                message,
                position,
                new Size(760f, 260f)
        )
                .setTextSize(46f)
                .setTextColor(0xffb9d8ef)
                .setAlignment(Text.Alignment.CENTER)
                .setVerticalAlignment(Text.VerticalAlignment.CENTER)
                .setLineSpacing(10f)
                .setMaxLines(3)
                .setWrapEnabled(true)
                .setOverflow(Text.Overflow.ELLIPSIZE_END)
);
```

## Styling

All builder styling methods:

| API | Purpose |
|---|---|
| `useDefaultFont()` | Select `Typeface.DEFAULT` |
| `setFont(int)` | Load an Android `R.font` resource |
| `setFontAsset(path)` | Load a font from application assets |
| `setFont(Typeface)` | Use an existing typeface |
| `setFontVariations(FontVariation)` | Select a named variable-font weight |
| `clearFontVariations()` | Return to the font's default variation |
| `setTextSize(value)` | Region-space text size |
| `setTextSizePx(px)` | Exact runtime-pixel text size |
| `setTextColor(color)` | Set ARGB text color |
| `setAlpha(alpha)` | Set additional alpha in the `0..1` range |
| `setLetterSpacing(value)` | Add character spacing |
| `setLineSpacing(value)` | Add line spacing |
| `setLineSpacingMultiplier(value)` | Multiply normal line spacing |
| `setPadding(horizontal, vertical)` | Inset the content region |
| `setAlignment(value)` | Horizontal alignment |
| `setVerticalAlignment(value)` | Vertical alignment |
| `setOverflow(value)` | Clip or ellipsize overflow |
| `setMaxLines(count)` | Limit rendered line count |
| `setWrapEnabled(enabled)` | Enable wrapping; false forces one line |
| `setShadow(radius, dx, dy, color)` | Configure text shadow |
| `clearShadow()` | Remove text shadow |
| `setStyle(style)` | Apply a reusable immutable style |
| `setEnabled(enabled)` | Enable or disable click handling |
| `setOnClickListener(listener)` | Add optional click behavior |

## Reusable TextStyle

```java
TextStyle heading = new TextStyle.Builder()
        .setFont(R.font.game_font)
        .setTextSize(72f)
        .setTextColor(Color.WHITE)
        .setAlignment(Text.Alignment.CENTER)
        .setVerticalAlignment(Text.VerticalAlignment.CENTER)
        .setShadow(4f, 0f, 3f, 0xaa000000)
        .build();
```

Apply it:

```java
texts.add(
        new Text.Builder(
                getContext(),
                "game_over",
                "GAME OVER",
                position,
                new Size(900f, 120f)
        ).setStyle(heading)
);
```

Create a modified copy:

```java
TextStyle warning = new TextStyle.Builder(heading)
        .setTextColor(Color.RED)
        .build();
```

## Runtime API

Read state:

```java
text.getId();
text.getText();
text.getBounds();
text.getMeasuredTextWidth();
text.getMeasuredTextHeight();
text.isVisible();
text.isEnabled();
text.isClickable();
```

Update content and appearance:

```java
text.setText("12,500");
text.setTextColor(Color.GREEN);
text.setAlpha(0.8f);
text.setTextSize(64f);
text.setTextSizePx(48f);
text.setFont(R.font.game_font);
text.setFontAsset("fonts/game_font.ttf");
text.setFont(typeface);
text.setFontVariations(FontVariation.BOLD);
text.clearFontVariations();
text.useDefaultFont();
text.setAlignment(Text.Alignment.END);
text.setVerticalAlignment(Text.VerticalAlignment.BOTTOM);
text.setMaxLines(2);
text.setOverflow(Text.Overflow.ELLIPSIZE_END);
text.setWrapEnabled(true);
text.setStyle(style);
text.setVisible(true);
```

Update the region:

```java
text.setRegion(position, new Size(500f, 100f));
text.setRegion(new RectF(left, top, right, bottom));
```

Layout-affecting updates rebuild `StaticLayout` and invalidate the host
automatically.

## Optional click callback

```java
Text link = texts.add(
        new Text.Builder(
                getContext(),
                "privacy_policy",
                "Privacy Policy",
                position,
                new Size(420f, 80f)
        )
                .setTextSize(42f)
                .setOnClickListener(id -> openPrivacyPolicy())
);
```

Click behavior:

- reverse drawing-order dispatch gives the topmost overlapping text priority;
- down and up must occur inside the same resolved region;
- moving outside permanently cancels the current gesture;
- `ACTION_CANCEL` cancels the gesture;
- hidden, disabled, and listener-free text are ignored.

Change interaction at runtime:

```java
text.setEnabled(false);
text.setOnClickListener(listener);
text.setOnClickListener(null);
```

Click handling intentionally adds no sound, haptic, press scale, long-click, or
animation. Use the future `Button` component when those behaviors are required.

## ZLayer API

```java
Text added = texts.add(builder);
Text found = texts.find("id");

boolean exists = texts.contains("id");
boolean removed = texts.remove("id");

int count = texts.size();
boolean empty = texts.isEmpty();

ui.draw(canvas);
boolean consumed = ui.onTouchEvent(event);

texts.clear();   // Releases children; group remains reusable.
ui.release(); // Releases children and closes the group.
texts.close();   // AutoCloseable alias.
```

IDs must be non-null, non-blank, and unique within a group. Duplicate IDs throw
`IllegalArgumentException`. Calling group APIs after `release()` throws
`IllegalStateException`.

## Validation and lifecycle

- Region dimensions must be positive and finite.
- Figma `Size` dimensions must be positive and finite.
- Padding cannot consume the entire drawable region.
- Alpha must be within `0..1`.
- Text size and line-spacing multiplier must be positive.
- Spacing and padding cannot be negative.
- Released `Text` instances reject further mutations.
- `clear()` is appropriate when rebuilding components in `onSizeChanged()`.
- `release()` is appropriate when the host is detached or destroyed.

## Test activity

The sample application contains:

```text
app.builderx.ogfa.androiduicomponents.TextTestActivity
```

Launch it with:

```powershell
adb shell am start -n `
  app.builderx.ogfa.androiduicomponents/.TextTestActivity
```

It covers `Position + Size`, `RectF`, resource/default fonts, wrapping, ellipsizing,
alignment, shadows, runtime mutation, click dispatch, and move-out cancellation.

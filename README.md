# Native Views

Native Views is an Android Canvas UI SDK published under:

```text
com.ogfa.nativeviews
```

It is designed for custom `View` and game-style Canvas interfaces that need
Figma-based positioning, native Android text/input, layered animation, deterministic
drawing order, touch dispatch, and explicit resource cleanup without XML layouts.

This README covers SDK integration, architecture, component status, build commands,
and documentation navigation. Each implemented component has its own detailed guide
with its complete API and examples.

## Requirements

```text
Minimum Android SDK: 24
Java source level:    11
Default Figma width:  1080
```

The parent Android application supplies AndroidX. The fat AAR intentionally does not
embed AppCompat or other AndroidX classes.

## Add the fat AAR

Copy:

```text
native-views-release.aar
```

into:

```text
app/libs/native-views-release.aar
```

Add it to the consuming application's `build.gradle`:

```groovy
dependencies {
    implementation files('libs/native-views-release.aar')

    // AndroidX is owned by the parent application.
    implementation 'androidx.appcompat:appcompat:1.7.1'
}
```

The fat AAR embeds:

- Lottie `6.0.0`;
- Android GIF Drawable `1.2.28`;
- ReLinker;
- Okio required by the embedded animation dependencies.

Do not declare Lottie or Android GIF Drawable again when consuming the fat AAR.

## Consume the Gradle module

When this repository is included as a multi-module project:

```groovy
dependencies {
    implementation project(':native-views')
    implementation 'androidx.appcompat:appcompat:1.7.1'
}
```

The normal `native-views` module declares its animation libraries through Gradle.
Use `native-views-fat` only to build the portable all-in-one AAR.

## Component documentation

### Implemented UI components

| Component | Purpose | Detailed guide |
|---|---|---|
| `Text` | Native non-editable Canvas text with optional click | [TEXT.md](docs/components/TEXT.md) |
| `TextField` | Native Canvas editor and Android IME bridge | [TEXT_FIELD.md](docs/components/TEXT_FIELD.md) |
| `Button` | Image-backed composite with optional native text | [BUTTON.md](docs/components/BUTTON.md) |
| `Image` | Standalone Canvas bitmap with scaling and optional click | [IMAGE.md](docs/components/IMAGE.md) |
| `Card` | Rounded color/image container with nested ZLayer and outside shadow | [CARD.md](docs/components/CARD.md) |
| `ComponentList` | Virtualized vertical/horizontal list with reusable layered items | [COMPONENT_LIST.md](docs/components/COMPONENT_LIST.md) |
| `CustomAnimatorComponent` | Generic five-layer animated Canvas element | [CUSTOM_ANIMATOR_COMPONENT.md](docs/components/CUSTOM_ANIMATOR_COMPONENT.md) |
| `ZLayerGroup` / `ZLayer` | Mixed-component scene, z-order, touch, and IME owner | [Z_LAYER.md](docs/components/Z_LAYER.md) |

### Supporting utilities

| Utility | Purpose | Detailed guide |
|---|---|---|
| Bundled fonts | Inter, Montserrat, Roboto, and Lilita One resources | [FONTS.md](docs/utilities/FONTS.md) |
| Bitmap text utilities | Bitmap-font and Android-font bitmap generation/composition | [TEXT_BITMAP_UTILITIES.md](docs/utilities/TEXT_BITMAP_UTILITIES.md) |
| `Position` and `Size` | Figma-to-runtime region conversion | Covered by each component guide |
| `FigmaConfig` | App-wide Figma reference width and conversion scale | [FIGMA_CONFIG.md](docs/utilities/FIGMA_CONFIG.md) |
| `NativeViewsSoundPlayer` | Bundled component-click sound | Covered by `CustomAnimatorComponent` |

### Existing APIs awaiting final hardening

These classes exist in the SDK, but their final public names or contracts are still on
the roadmap. Their dedicated component guides will be added when hardening is
complete:

| Roadmap component | Current implementation |
|---|---|
| `AfterEffectAnimator` | `animation.aftereffect.AfterEffectAnimator` |
| `DynamicViewAnimator` | `animation.dynamic.DynamicViewAnimator` |
| `LottieViewAnimator` | `animation.LottieViewAnimator` |
| `GifAnimator` | Currently `animation.gif.GIFViewAnimator` |

### Planned primary components

```text
Dialog
```

### Planned secondary components

```text
Switch
CheckBox
RadioButton
Progress
```

See [GOAL_BLUEPRINT.md](GOAL_BLUEPRINT.md) for the architecture, requirements, delivery
order, and definition of done.

## Shared region model

Every visual component targets the same two region forms:

```java
// Figma/design-space layout
new Component.Builder(..., position, size);

// Direct runtime-pixel layout
new Component.Builder(..., rectF);
```

The region form does not change styling units. Unsuffixed dimensional styling
methods always use Figma/design units, while methods ending in `Px` always use
exact runtime pixels:

```java
.setPadding(24f, 12f)       // Figma units
.setPaddingPx(24f, 12f)     // exact runtime pixels
```

`Position` defines horizontal/vertical anchors and Figma margins:

```java
Position position = new Position(
        hostView,
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        20f,
        90f
);
```

`Size` defines Figma element dimensions:

```java
Size size = new Size(100f, 50f);
```

The uniform scale is derived only from the measured host width:

```text
scale = runtime host width / Figma reference width
```

With the default reference width of `1080`, a `1440`-wide host produces:

```text
scale  = 1440 / 1080 = 1.3333
left   = 20  × 1.3333 = 26.67
top    = 90  × 1.3333 = 120
width  = 100 × 1.3333 = 133.33
height = 50  × 1.3333 = 66.67
```

Resolved bounds:

```java
RectF(
        26.67f,
        120f,
        160f,
        186.67f
);
```

The runtime device height does not affect scale. It is used only to resolve a
bottom-anchored position.

Direct conversion:

```java
RectF bounds = position.toRectF(size);
RectF bounds = position.toRectF(hostView, size);
```

Configure the application reference width when the design file uses another
frame width:

```java
FigmaConfig.setDefault(new FigmaConfig(1440f));
```

`Position` captures the current immutable configuration. Components using an
explicit `RectF` capture the current default configuration for their unsuffixed
styling dimensions. All Figma measurements use the same width-derived scale. See
[FIGMA_CONFIG.md](docs/utilities/FIGMA_CONFIG.md).

## Shared Canvas lifecycle

A `ZLayerGroup` is created once with the host:

```java
private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer content = ui.addLayer("content");
```

Create or rebuild components after measurement:

```java
@Override
protected void onSizeChanged(
        int width,
        int height,
        int oldWidth,
        int oldHeight
) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    content.clear();
    // Add components here.
}
```

Draw:

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ui.draw(canvas);
}
```

Forward touch when the group contains interactive elements:

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
    return ui.onTouchEvent(event)
            || super.onTouchEvent(event);
}
```

Release with the host:

```java
@Override
protected void onDetachedFromWindow() {
    ui.release();
    super.onDetachedFromWindow();
}
```

Read the component-specific guides because animated components and TextFields have
additional lifecycle and input responsibilities managed by `ZLayerGroup`.

## Build the SDK

Build the normal library AAR:

```powershell
.\gradlew.bat :native-views:assembleRelease
```

Output:

```text
native-views/build/outputs/aar/native-views-release.aar
```

Build the fat AAR:

```powershell
.\gradlew.bat :native-views-fat:assembleRelease
```

Output:

```text
native-views-fat/build/outputs/aar/native-views-release.aar
```

## Validate as an external AAR consumer

Copy the exact generated fat AAR into the sample app:

```powershell
Copy-Item `
  native-views-fat/build/outputs/aar/native-views-release.aar `
  app/libs/native-views-release.aar `
  -Force
```

Compile the app against that copied AAR:

```powershell
.\gradlew.bat :app:assembleDebug
```

This validates the same packaging path used by a separate parent project.

## Generated artifacts

Fat AAR:

```text
native-views-fat/build/outputs/aar/native-views-release.aar
```

Sample APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Optional release copies may also be placed in:

```text
artifacts/
```

## Sample test activities

| Activity | Coverage |
|---|---|
| `TextTestActivity` | Both region paths, all bundled font resources, alignment, wrapping, ellipsis, style, runtime update, click cancellation |
| `TextFieldTestActivity` | IME, focus, cursor taps/dragging, selection, three screen positions, keyboard avoidance |
| `ImageTestActivity` | Both region paths, all scale types, bitmap replacement, runtime API, alpha, click, cleanup |
| `CustomAnimatorComponentTestActivity` | Bitmap, Lottie, GIF, dynamic and After Effects layers, touch, movement, cleanup |
| `ZLayerTestActivity` | Mixed components, ordering, unique IDs, cross-layer movement, modal touch blocking, shared IME |

Example launch:

```powershell
adb shell am start -n `
  app.builderx.ogfa.androiduicomponents/.TextTestActivity
```

## Documentation convention

Every completed UI component must have:

```text
docs/components/COMPONENT_NAME.md
```

Its component guide contains:

- purpose and responsibility;
- packages and imports;
- every supported region constructor;
- complete host integration;
- builder and runtime API tables;
- callbacks and touch behavior;
- lifecycle and failure behavior;
- practical examples;
- dedicated test activity.

The root README remains the SDK overview, integration guide, component index, build
guide, and documentation entry point.

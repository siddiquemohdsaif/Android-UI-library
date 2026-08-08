# Switch

`Switch` is a native Canvas selectable component with an animated track and thumb. It
supports tapping, continuous thumb dragging, programmatic state changes, and direct use
in `ZLayer`, `Card`, `Dialog`, `ComponentList`, or `ZLayerContainer`.

It supports three rendering modes: native color drawing, complex images with separate
track/thumb assets, and simple complete-state images.

```java
import com.ogfa.nativeviews.switchcomponent.Switch;
```

## Creation

Figma-space region:

```java
Switch sound = layer.add(new Switch.Builder(
        getContext(),
        "sound",
        position,
        new Size(120f, 64f)
)
        .setChecked(true)
        .setOnCheckedChangeListener((id, checked, fromUser) ->
                saveSoundEnabled(checked)));
```

Runtime-pixel region:

```java
Switch sound = layer.add(new Switch.Builder(
        getContext(),
        "sound",
        new RectF(40f, 200f, 160f, 264f)
));
```

`Position + Size` uses Figma space. `RectF` uses runtime pixels. Unsuffixed visual
measurements always remain Figma values; matching `Px` methods use exact runtime pixels.

## Complex image rendering

Complex mode requires five caller-owned bitmaps and preserves continuous thumb dragging:

```java
SwitchImages images = SwitchImages.complex(
        trackOn,
        trackOff,
        trackDisabled,
        thumbEnabled,
        thumbDisabled
);

Switch textured = layer.add(new Switch.Builder(
        context,
        "textured",
        images,
        position,
        new Size(220f, 112f)
)
        .setThumbPadding(6f)
        .setTrackImageScaleType(Image.ScaleType.FIT_XY)
        .setThumbImageScaleType(Image.ScaleType.FIT_CENTER)
        .setImageFiltering(true));
```

At exact 0% or 100%, the renderer draws the complete unchecked or checked track.
Between those endpoints, the unchecked track is drawn first and the checked track is
clipped precisely at `thumbBounds.centerX()`. Both textures remain opaque and meet
under the moving thumb without exposing a gap. When disabled, the renderer
uses `trackDisabled` and `thumbDisabled`; the thumb position continues to communicate
the stable checked state.

## Simple image rendering

Simple mode requires three complete switch images:

```java
SwitchImages images = SwitchImages.simple(
        switchOn,
        switchOff,
        switchDisabled
);

Switch textured = layer.add(new Switch.Builder(
        context,
        "simple_textured",
        images,
        position,
        new Size(220f, 112f)
)
        .setImageTransition(Switch.ImageTransition.CROSS_FADE)
        .setSwitchImageScaleType(Image.ScaleType.FIT_XY));
```

Transitions:

```java
Switch.ImageTransition.CROSS_FADE
Switch.ImageTransition.SNAP
```

Simple mode is tap-only because its thumb is embedded in the complete bitmap. Enabling
dragging throws a clear exception instead of rendering two overlapping cross-faded
thumbs:

```java
simple.setDragEnabled(true); // throws IllegalStateException
```

## Image ownership and runtime modes

The library validates all required images for null, recycling, and dimensions. It never
recycles caller-owned bitmaps. Recycling an active source in caller code produces a clear
rendering exception.

```java
textured.getRenderMode();
textured.getSwitchImages();
textured.isDragEnabled();
textured.isImageFilteringEnabled();

textured.setSwitchImages(otherImages);
textured.useColorRendering();

textured.setTrackImageScaleType(Image.ScaleType.FIT_XY);   // complex
textured.setThumbImageScaleType(Image.ScaleType.FIT_CENTER); // complex
textured.setSwitchImageScaleType(Image.ScaleType.FIT_XY);  // simple
textured.setImageFiltering(true);
textured.setImageTransition(Switch.ImageTransition.SNAP);   // simple
textured.setDragEnabled(false);
```

Changing rendering mode cancels the current gesture and animation, preserves the stable
checked value, validates the new sources, and rebuilds rendering geometry. Complex and
color modes default to drag enabled; simple mode defaults to drag disabled.

Image modes default to `disabledAlpha = 1f` because their disabled assets already contain
the intended appearance. An explicit `setDisabledAlpha()` always wins.

Color-only APIs throw while an image renderer is active. Thumb geometry and shadow APIs
remain available in complex mode, but throw in simple mode because it has no separate
thumb. This prevents silently ignored configuration.

## State

```java
sound.isChecked();
sound.setChecked(true);             // animated, fromUser=false
sound.setCheckedImmediately(false); // immediate, fromUser=false
sound.toggle();
sound.toggleImmediately();

sound.setOnCheckedChangeListener((id, checked, fromUser) -> { });
sound.removeOnCheckedChangeListener();
```

The callback runs only when the stable value changes. `fromUser` is true for a
successful tap or drag and false for programmatic changes. Sound and haptic actions run
only for user-originated changes.

## Touch behavior

- A tap inside the region toggles the value.
- A horizontal drag moves the thumb continuously.
- Releasing at or beyond the midpoint checks the switch; releasing below it unchecks.
- Android touch slop separates taps from drags.
- Releasing a tap outside or receiving `ACTION_CANCEL` restores the original state.
- Hidden and disabled switches ignore input.

## Track and thumb styling

```java
.setCheckedTrackColor(0xff019cc4)
.setUncheckedTrackColor(0xffb8c0c8)
.setDisabledTrackColor(0xffd5d9dd)
.setThumbColor(Color.WHITE)
.setDisabledThumbColor(0xffeeeeee)

.setTrackCornerRadius(32f)
.setTrackCornerRadiusPx(32f)
.setTrackCornerRadiusAuto()

.setThumbPadding(4f)
.setThumbPaddingPx(4f)
.setThumbSize(56f)
.setThumbSizePx(56f)
.setThumbSizeAuto()

.setTrackStroke(2f, 0xff7f8a94)
.setTrackStrokePx(2f, 0xff7f8a94)
.setTrackStrokeEnabled(false)
```

Automatic radius is half the track height. Automatic thumb size is the track height
minus twice the resolved padding. Invalid combinations fail immediately instead of
drawing outside the region.

## Disabled appearance

Disabled rendering is fully configurable. Convenience setters apply one color to both
checked and unchecked disabled states:

```java
.setDisabledTrackColor(0xff707784)
.setDisabledThumbColor(0xffa5aab3)
```

Use state-specific colors when a disabled switch must still communicate its value:

```java
.setDisabledCheckedTrackColor(0xff345995)
.setDisabledUncheckedTrackColor(0xff596170)
.setDisabledCheckedThumbColor(0xffb8d8ff)
.setDisabledUncheckedThumbColor(0xffc6cad1)
.setDisabledStrokeColor(0xff8491a6)
.setDisabledAlpha(0.75f)
.setDisabledThumbShadowEnabled(false)
```

All methods have matching runtime setters. `disabledAlpha` is multiplied by the normal
component alpha and defaults to `0.45f`. Set it to `1f` to render the exact disabled
colors without additional fading. The regular stroke color is reused until an explicit
disabled stroke color is supplied. Disabled shadow visibility is independent from the
normal `setThumbShadowEnabled()` setting.

```java
sound.getDisabledAlpha();
sound.getDisabledCheckedTrackColor();
sound.getDisabledUncheckedTrackColor();
sound.getDisabledCheckedThumbColor();
sound.getDisabledUncheckedThumbColor();
sound.getDisabledStrokeColor();
sound.isDisabledThumbShadowEnabled();
```

Styling never changes disabled interaction: touch, ripple, user sound, and user haptic
feedback remain unavailable while `setEnabled(false)` is active. Programmatic checked
state updates remain supported and animate between the configured disabled colors.

## Thumb shadow

The immutable Card `DropShadow` type is reused:

```java
.setThumbShadow(new DropShadow(
        0f, 2f, 6f, 0f, Color.argb(35, 0, 0, 0)))
.setThumbShadowEnabled(true)
```

`setThumbShadow()` uses Figma measurements. `setThumbShadowPx()` treats all shadow
measurements as exact runtime pixels. Shadow is disabled by default.

## Animation, ripple, and feedback

```java
.setAnimationDuration(180L)
.setAnimationInterpolator(Switch.Interpolator.EASE_OUT)

.setRippleEnabled(true) // disabled by default
.setRippleColor(0x22000000)
.setRippleDuration(240L)

.setSoundAction(this::playToggleSound)
.setHapticAction(() -> performHapticFeedback(
        HapticFeedbackConstants.KEYBOARD_TAP))
```

Available interpolators are `LINEAR`, `EASE_IN`, `EASE_OUT`, and `EASE_IN_OUT`.

Runtime animation control:

```java
sound.isAnimating();
sound.getThumbProgress(); // 0f..1f
sound.finishAnimation();
sound.cancelAnimation();  // snaps to stable checked state
```

## Region and component state

```java
sound.getId();
sound.getBounds();
sound.getFigmaConfig();
sound.getDimensionScale();

sound.setRegion(position, size);
sound.setRegion(rectF);
sound.horizontalCenter(true);
sound.verticalCenter(true);

sound.setAlpha(0.8f);
sound.setVisible(false);
sound.setEnabled(false);
```

Region changes rebuild track, thumb travel, radius, padding, stroke, shadow, ripple,
and touch geometry.

## Shared selectable contract

`Switch` implements `SelectableComponent`. `CheckBox` and `RadioButton` will use the
same `OnCheckedChangeListener` and checked-state controller, keeping callback and
animation semantics consistent across selectable controls.

## Validation and lifecycle

The component rejects blank IDs, invalid bounds, negative dimensions or durations,
invalid alpha, excessive thumb dimensions, and use after release. The owning
`ZLayerGroup` normally releases it:

```java
@Override protected void onDetachedFromWindow() {
    ui.release();
    super.onDetachedFromWindow();
}
```

Release cancels state and ripple animators, restores touch state, removes callbacks,
and is idempotent.

Dedicated coverage: `SwitchTestActivity` and `SwitchImageModeTestActivity`.

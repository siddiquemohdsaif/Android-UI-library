# Switch

`Switch` is a native Canvas selectable component with an animated track and thumb. It
supports tapping, continuous thumb dragging, programmatic state changes, and direct use
in `ZLayer`, `Card`, `Dialog`, `ComponentList`, or `ZLayerContainer`.

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

Dedicated coverage: `SwitchTestActivity`.

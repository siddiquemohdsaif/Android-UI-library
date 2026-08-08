# CheckBox

`com.ogfa.nativeviews.checkbox.CheckBox` is a native Canvas selectable component for
two-state and optional three-state input. It is added directly to a `ZLayer`; it is not
an Android XML `CheckBox` and does not own a separate component group.

## Basic native-color usage

```java
CheckBox remember = controls.add(
        new CheckBox.Builder(
                getContext(),
                "remember_me",
                new Position(
                        this,
                        Position.HorizontalMarginFrom.LEFT,
                        Position.VerticalMarginFrom.TOP,
                        80f,
                        640f
                ),
                new Size(48f, 48f)
        )
                .setChecked(false)
                .setCheckedColor(0xff019cc4)
                .setUncheckedColor(Color.TRANSPARENT)
                .setCheckMarkColor(Color.WHITE)
                .setStrokeColor(0xff656565)
                .setStrokeWidth(2f)
                .setCornerRadius(8f)
                .setOnCheckedChangeListener((id, checked, fromUser) ->
                        saveRememberMe(checked))
);
```

The runtime-pixel region overload is:

```java
new CheckBox.Builder(context, "remember_me", new RectF(left, top, right, bottom));
```

`horizontalCenter(true)` and `verticalCenter(true)` align the complete region inside
its owning ZLayer or nested component host.

## Checked and indeterminate states

```java
public enum State {
    UNCHECKED,
    CHECKED,
    INDETERMINATE
}
```

Normal interaction alternates between unchecked and checked. Enable three-state
cycling explicitly:

```java
new CheckBox.Builder(context, "selection", position, size)
        .setIndeterminateEnabled(true)
        .setToggleOrder(
                CheckBox.State.UNCHECKED,
                CheckBox.State.CHECKED,
                CheckBox.State.INDETERMINATE
        )
        .setOnStateChangeListener((id, state, fromUser) -> {
            // Receives all three states.
        });
```

`OnCheckedChangeListener` remains available through the shared `SelectableComponent`
contract. It reports `true` only for `CHECKED`. Use `OnStateChangeListener` when the
distinction between unchecked and indeterminate matters.

```java
checkBox.setChecked(true);                       // Animated, listener notified
checkBox.setChecked(true, false);                // Animated, listener suppressed
checkBox.setCheckedImmediately(false);           // No animation
checkBox.setState(CheckBox.State.INDETERMINATE);
checkBox.setStateImmediately(CheckBox.State.CHECKED);
checkBox.toggle();
checkBox.toggleImmediately();
```

Callbacks receive `fromUser=true` only for a completed touch gesture. Moving or
cancelling outside the original component cancels the click.

## Image rendering

Four images provide a two-state CheckBox:

```java
CheckBoxImages images = CheckBoxImages.create(
        uncheckedBitmap,
        checkedBitmap,
        disabledUncheckedBitmap,
        disabledCheckedBitmap
);
```

Six images enable all states:

```java
CheckBoxImages images = CheckBoxImages.create(
        uncheckedBitmap,
        checkedBitmap,
        indeterminateBitmap,
        disabledUncheckedBitmap,
        disabledCheckedBitmap,
        disabledIndeterminateBitmap
);

CheckBox textured = controls.add(
        new CheckBox.Builder(
                getContext(), "textured", images, position, new Size(64f, 64f))
                .setIndeterminateEnabled(true)
                .setImageScaleType(Image.ScaleType.FIT_CENTER)
                .setImageTransition(CheckBox.ImageTransition.CROSS_FADE)
                .setImageFiltering(true)
);
```

`SNAP` switches complete images without blending. Invalid, zero-sized, or recycled
bitmaps fail clearly. The SDK never recycles supplied images: the caller or cache that
created them owns their lifetime, allowing safe sharing across CheckBoxes.

```java
checkBox.setCheckBoxImages(images);
checkBox.setImageScaleType(Image.ScaleType.FIT_XY);
checkBox.setImageTransition(CheckBox.ImageTransition.SNAP);
checkBox.setImageFiltering(false);
checkBox.useColorRendering();
```

Image sets without indeterminate images reject indeterminate state and interaction.

## Figma and pixel styling

Normal numeric methods use the component's `FigmaConfig` scale:

```java
.setCornerRadius(8f)
.setStrokeWidth(2f)
.setCheckMarkWidth(3f)
.setPadding(6f)
```

Explicit runtime-pixel alternatives bypass Figma scaling:

```java
.setCornerRadiusPx(8f)
.setStrokeWidthPx(2f)
.setCheckMarkWidthPx(3f)
.setPaddingPx(6f)
```

Resolved values are available from `getResolvedCornerRadius()`,
`getResolvedStrokeWidth()`, `getResolvedCheckMarkWidth()`, and
`getResolvedPadding()`.

Complete color-state control:

```java
.setCheckedColor(checked)
.setUncheckedColor(unchecked)
.setIndeterminateColor(indeterminate)
.setCheckMarkColor(mark)
.setStrokeColor(stroke)
.setDisabledCheckedColor(disabledChecked)
.setDisabledUncheckedColor(disabledUnchecked)
.setDisabledIndeterminateColor(disabledIndeterminate)
.setDisabledCheckMarkColor(disabledMark)
.setDisabledStrokeColor(disabledStroke)
.setDisabledAlpha(0.65f)
```

Disabled state preserves the logical value:

```java
checkBox.setChecked(true, false);
checkBox.setEnabled(false); // Disabled checked

checkBox.setChecked(false, false);
checkBox.setEnabled(false); // Disabled unchecked
```

## Animation and feedback

Defaults are a 160 ms state transition, `0.92f` pressed scale, 100 ms press animation,
and disabled ripple.

```java
.setStateAnimationDuration(160L)
.setAnimationInterpolator(CheckBox.Interpolator.EASE_OUT)
.setPressedScale(0.92f)
.setPressAnimationDuration(100L)
.setRippleEnabled(true)
.setRippleColor(0x33019cc4)
.setRippleDuration(240L)
.setSoundAction(this::playClick)
.setHapticAction(() -> performHapticFeedback(
        HapticFeedbackConstants.KEYBOARD_TAP))
```

```java
checkBox.finishAnimation();
checkBox.cancelAnimation();
checkBox.isAnimating();
```

## Runtime API

```java
checkBox.getId();
checkBox.getBounds();
checkBox.getState();
checkBox.isChecked();
checkBox.isIndeterminate();
checkBox.isEnabled();
checkBox.isVisible();
checkBox.getRenderMode();
checkBox.getCheckBoxImages();

checkBox.setRegion(position, size);
checkBox.setRegion(rectF);
checkBox.horizontalCenter(true);
checkBox.verticalCenter(true);
checkBox.setAlpha(0.8f);
checkBox.setVisible(false);
checkBox.setEnabled(false);

checkBox.setOnCheckedChangeListener(listener);
checkBox.removeOnCheckedChangeListener();
checkBox.setOnStateChangeListener(listener);
checkBox.removeOnStateChangeListener();
```

## Host integration and cleanup

```java
private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer controls = ui.addLayer("controls");

@Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ui.draw(canvas);
}

@Override public boolean onTouchEvent(MotionEvent event) {
    return ui.onTouchEvent(event) || super.onTouchEvent(event);
}

void release() {
    ui.release();
}
```

`release()` is idempotent and cancels state, press, and ripple animators. It removes
callbacks but leaves caller-owned bitmaps untouched. Keep labels as separate `Text`
components in the same ZLayer so their region and styling remain independent.

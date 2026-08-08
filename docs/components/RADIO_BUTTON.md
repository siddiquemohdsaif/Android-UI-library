# RadioButton

`RadioButton` is a native Canvas selectable component. `RadioSelection` is its optional
non-visual mutual-exclusion controller. Buttons remain ordinary components in any
`ZLayer`; the selection does not draw, own a ViewGroup, or dispatch touch.

## Required grouped selection

```java
RadioSelection quality = new RadioSelection("video_quality")
        .setSelectionRequired(true)
        .setOnSelectionChangedListener((groupId, oldId, newId, fromUser) ->
                saveQuality(newId));

RadioButton low = controls.add(
        new RadioButton.Builder(context, "quality_low", lowPosition, new Size(48f, 48f))
                .setSelection(quality));

RadioButton medium = controls.add(
        new RadioButton.Builder(context, "quality_medium", mediumPosition, new Size(48f, 48f))
                .setSelection(quality)
                .setChecked(true));

RadioButton high = controls.add(
        new RadioButton.Builder(context, "quality_high", highPosition, new Size(48f, 48f))
                .setSelection(quality));
```

Selecting a new button unchecks the previous button before the group emits one
transaction callback. Selecting an already-selected button keeps it selected.

Each selection requires unique button IDs. Registering multiple initially checked
buttons throws clearly. When required selection already has buttons but no selected
button, enabling the requirement selects the first enabled button. Disabling or
releasing the selected button selects the first available enabled replacement.

## Regions and standalone use

Both standard region forms are supported:

```java
new RadioButton.Builder(context, "option", position, new Size(48f, 48f));
new RadioButton.Builder(context, "option", new RectF(left, top, right, bottom));
```

```java
.horizontalCenter(true)
.verticalCenter(true)
```

Rectangular regions draw a circle centered using the shortest side; the complete
region remains touchable. Without a `RadioSelection`, touch selects the standalone
button but touching it again does not unselect it. Programmatic code can clear it.

## Native color rendering

```java
new RadioButton.Builder(context, "high", position, new Size(48f, 48f))
        .setCheckedColor(0xff019cc4)
        .setUncheckedColor(0xff656565)
        .setDotColor(0xff019cc4)
        .setBackgroundColor(Color.TRANSPARENT)
        .setRingWidth(3f)
        .setDotSize(22f)
        .setPadding(3f);
```

Normal numeric methods use Figma units. Runtime-pixel alternatives are:

```java
.setRingWidthPx(3f)
.setDotSizePx(22f)
.setPaddingPx(3f)
```

Inspect resolved geometry with `getResolvedRingWidth()`, `getResolvedDotSize()`, and
`getResolvedPadding()`. Invalid padding, ring width, or dot geometry throws before
drawing.

Disabled appearance is independent:

```java
.setDisabledCheckedColor(0xff9e9e9e)
.setDisabledUncheckedColor(0xffbdbdbd)
.setDisabledDotColor(0xffeeeeee)
.setDisabledBackgroundColor(Color.TRANSPARENT)
.setDisabledAlpha(0.65f)
```

## Complete-state image rendering

```java
RadioButtonImages images = RadioButtonImages.create(
        uncheckedBitmap,
        checkedBitmap,
        disabledUncheckedBitmap,
        disabledCheckedBitmap
);

RadioButton textured = controls.add(
        new RadioButton.Builder(
                context, "textured", images, position, new Size(64f, 64f))
                .setSelection(selection)
                .setImageScaleType(Image.ScaleType.FIT_CENTER)
                .setImageTransition(RadioButton.ImageTransition.CROSS_FADE)
                .setImageFiltering(true));
```

`SNAP` is also available. Images are validated before use. The SDK never recycles
caller-provided bitmaps; the creator/cache owns their lifetime and may share them.

```java
radio.setRadioButtonImages(images);
radio.setImageScaleType(Image.ScaleType.FIT_XY);
radio.setImageTransition(RadioButton.ImageTransition.SNAP);
radio.setImageFiltering(false);
radio.useColorRendering();
```

## RadioSelection API

```java
selection.getId();
selection.getSelectedId();
selection.getSelectedButton();
selection.hasSelection();
selection.isSelectionRequired();

selection.select("quality_high");
selection.select("quality_high", false); // Suppress state/group callbacks
selection.selectImmediately("quality_low");
selection.clearSelection();              // Rejected when selection is required
selection.clearSelection(false);

selection.find("quality_high");
selection.contains("quality_high");
selection.getButtons();                  // Unmodifiable snapshot

selection.setSelectionRequired(true);
selection.setOnSelectionChangedListener(listener);
selection.removeOnSelectionChangedListener();
selection.release();
```

## Button state and callbacks

```java
radio.select();
radio.setChecked(true);
radio.setChecked(true, false);            // Suppress callbacks
radio.setCheckedImmediately(false);
radio.toggle();

radio.setSelection(selection);
radio.removeFromSelection();
radio.getSelection();

radio.setOnCheckedChangeListener((id, checked, fromUser) -> { });
radio.setOnClickListener(id -> { });
```

Touch always requests `checked=true`. `OnRadioClickListener` still fires when the
already-selected option is clicked. `OnCheckedChangeListener` fires only for actual
state changes. A user selection reports `fromUser=true` for both the old unchecked and
new checked button callbacks, followed by one group callback.

Moving outside or cancelling the gesture prevents selection. Hidden and disabled
buttons ignore touch.

## Animation and feedback

The default selection transition scales the inner dot and blends the ring over 160 ms.
Press scale defaults to `0.92f` over 100 ms; ripple is off.

```java
.setSelectionAnimationDuration(160L)
.setAnimationInterpolator(RadioButton.Interpolator.EASE_OUT)
.setPressedScale(0.92f)
.setPressAnimationDuration(100L)
.setRippleEnabled(true)
.setRippleColor(0x33019cc4)
.setRippleDuration(240L)
.setSoundAction(this::playClick)
.setHapticAction(this::performRadioHaptic)
```

```java
radio.finishAnimation();
radio.cancelAnimation();
radio.isAnimating();
radio.getSelectionProgress();
```

## Runtime component API

```java
radio.getId();
radio.getBounds();
radio.isChecked();
radio.isEnabled();
radio.isVisible();
radio.getRenderMode();

radio.setRegion(position, size);
radio.setRegion(rectF);
radio.horizontalCenter(true);
radio.verticalCenter(true);
radio.setAlpha(0.8f);
radio.setVisible(false);
radio.setEnabled(false);
```

`ZLayerGroup` owns drawing, touch dispatch, and component release. Release the
non-visual selections after the UI:

```java
ui.release();
quality.release();
```

Keep option labels as separate `Text` components. A transparent `Button` may cover an
entire option row and call `radio.select()` when a larger touch target is required.

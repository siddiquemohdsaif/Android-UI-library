# CustomAnimatorComponent

`CustomAnimatorComponent` is one Canvas component made from an ordered stack of bitmap,
GIF, Lottie, dynamic, and After Effects layers. `ZLayerGroup` owns drawing, topmost touch
dispatch, invalidation, and cleanup; no separate component group or static list helpers are
needed.

## Declared region

Every component has an explicit region. Bitmap dimensions are never used to guess it.

```java
CustomAnimatorComponent component = layer.add(
        new CustomAnimatorComponent.Builder(
                getContext(),
                "reward",
                layers,
                position,
                new Size(420f, 160f)
        )
);

// Runtime-pixel alternative
new CustomAnimatorComponent.Builder(getContext(), "reward", layers, rectF);
```

A single bitmap can be supplied directly. The builder creates a match-component
`BitmapLayer` internally:

```java
CustomAnimatorComponent play = layer.add(
        new CustomAnimatorComponent.Builder(
                getContext(), "play", playBitmap, position, new Size(420f, 160f)
        )
                .setClickListener(id -> startGame())
                .setPressedScale(0.92f)
                .setPressAnimationDuration(100L)
);
```

## Relative layer regions

Layer regions are relative to the component, so moving or resizing the component moves all
layers together.

```java
ArrayList<ComponentLayer> layers = new ArrayList<>();

layers.add(BitmapLayer.create(
        "background", backgroundBitmap, LayerRegion.matchComponent()
));

layers.add(BitmapLayer.create(
        "icon", iconBitmap, LayerRegion.figma(24f, 20f, 96f, 96f)
));

layers.add(GifLayer.create(
        getContext(), "glow", "reward_glow.gif",
        LayerRegion.figma(0f, 0f, 420f, 160f)
));

layers.add(LottieLayer.create(
        getContext(), "sparkles", "reward_sparkles",
        LayerRegion.figma(280f, 8f, 120f, 120f)
));

layers.add(DynamicLayer.create(
        "counter", customDynamicView, LayerRegion.figma(140f, 45f, 120f, 60f)
));

layers.add(AfterEffectLayer.create(
        "effect", afterEffectAnimator, LayerRegion.matchComponent()
));
```

`LayerRegion.figma(...)` scales from the component's `Position`/`FigmaConfig` reference.
`LayerRegion.px(...)` uses runtime pixels. `LayerRegion.matchComponent()` fills the declared
component region.

## Layer API

Every layer has a unique ID inside its component.

```java
component.getLayerCount();
component.getLayers();                 // unmodifiable
component.containsLayer("icon");
component.findLayer("icon");
component.findLayer("icon", BitmapLayer.class);

component.addLayer(layer);
component.addLayer(1, layer);
component.removeLayer("icon");         // releases that layer
component.clearLayers();                // releases all layers

component.bringLayerToFront("sparkles");
component.sendLayerToBack("background");
component.moveLayerAbove("icon", "background");
component.moveLayerBelow("glow", "icon");
component.setLayerIndex("icon", 2);

component.setLayerRegion("icon", LayerRegion.figma(30f, 20f, 90f, 90f));
component.setLayerVisible("glow", false);
component.setLayerAlpha("sparkles", 0.65f);
```

`BitmapLayer` does not own or recycle its bitmap:

```java
BitmapLayer icon = component.findLayer("icon", BitmapLayer.class);
icon.getBitmap();
icon.setBitmap(updatedBitmap);
```

## Bounds and clipping

The declared layout region, visual union, and interaction bounds are separate:

```java
component.getLayoutBounds();
component.getVisualBounds();
component.getBounds();          // active BoundsPolicy result
```

Policies:

```java
BoundsPolicy.DECLARED_REGION    // default
BoundsPolicy.LAYER_UNION
BoundsPolicy.LARGEST_LAYER
BoundsPolicy.CUSTOM
```

```java
new CustomAnimatorComponent.Builder(context, id, layers, position, size)
        .setBoundsPolicy(BoundsPolicy.LAYER_UNION)
        .setClipToBounds(true);

// Setting a resolver selects CUSTOM automatically.
.setBoundsResolver((declared, layerBounds) -> calculatedRect);
```

Clipping is independent of hit bounds. It is off by default, allowing effects to render
outside the declared region.

## Optional interaction

Without a click or long-click listener, the component is display-only and touch passes to
the component below it.

```java
new CustomAnimatorComponent.Builder(context, id, layers, rectF)
        .setClickListener(clickedId -> openReward())
        .setOnLongClickListener(clickedId -> showDetails())
        .setLongClickDelay(500L)
        .setPressedScale(0.92f)
        .setPressAnimationDuration(100L);
```

Down must begin inside the component. Moving outside cancels the gesture. Click fires only
when up remains inside; long click fires once after its delay. Press animation uses
`ValueAnimator`, not polling threads.

Sound and haptics are both opt-in:

```java
.setSoundMode(CustomAnimatorComponent.SoundMode.NATIVE_VIEWS)

.setSoundAction(this::playGameSound)  // selects CUSTOM sound mode
.setHapticAction(() ->
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP))

.setSoundMode(CustomAnimatorComponent.SoundMode.NONE) // default
```

## Position animation

All layer rectangles and the touch bounds resolve on every movement frame. GIF and Lottie
animations retain their current playback instead of being recreated.

```java
component.animateRegionTo(
        targetPosition,
        new Size(420f, 160f),
        650L,
        CustomAnimatorComponent.Interpolator.EASE_IN_OUT,
        () -> onMoveFinished()
);

component.animateRegionTo(targetRectF, 650L,
        CustomAnimatorComponent.Interpolator.LINEAR, null);

component.isMoving();
component.pauseMovement();
component.resumeMovement();
component.cancelMovement();
component.finishMovement();
```

## General state and alignment

```java
component.setRegion(position, size);
component.setRegion(rectF);
component.horizontalCenter(true);
component.verticalCenter(true);
component.setAlpha(0.7f);
component.setVisible(false);
component.setEnabled(false);
```

Builder centering is also available with `.horizontalCenter(true)` and
`.verticalCenter(true)`. Centering uses the owning `ZLayer`/composite component bounds.

## Lifecycle

```java
private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer content = ui.addLayer("content");

@Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ui.draw(canvas);
}

@Override public boolean onTouchEvent(MotionEvent event) {
    return ui.onTouchEvent(event) || super.onTouchEvent(event);
}

@Override protected void onDetachedFromWindow() {
    ui.release();
    super.onDetachedFromWindow();
}
```

Release is idempotent. Animator resources are stopped/cleared, but caller-owned bitmaps are
never recycled.

# ZLayerContainer

`ZLayerContainer` is a bounded, movable component that owns nested `ZLayer`s. Use it
when several ordinary SDK components must behave as one unit. It replaces the removed
five-type `CustomAnimatorComponent`; its children can be any `Component`, including
`Text`, `TextField`, `Button`, `Image`, `Card`, animator components, or another
`ZLayerContainer`.

## Create a container

Every container supports the normal region forms:

```java
ZLayerContainer panel = scene.add(
        new ZLayerContainer.Builder(
                getContext(),
                "reward_panel",
                position,
                new Size(600f, 220f)
        )
                .setClipToBounds(true)
);

ZLayerContainer runtimePanel = scene.add(
        new ZLayerContainer.Builder(
                getContext(),
                "runtime_panel",
                new RectF(40f, 180f, 740f, 520f)
        )
);
```

`Position + Size` uses Figma-space dimensions. `RectF` uses runtime pixels.

## Add nested layers and components

The built-in `content` layer is available immediately. Additional layers render in
insertion order, bottom to top:

```java
ZLayer background = panel.getContentLayer();
ZLayer animation = panel.addLayer("animation");
ZLayer foreground = panel.addLayer("foreground");

background.add(new Image.Builder(
        getContext(),
        "panel_background",
        backgroundBitmap,
        panel.getLocalBounds()
));

animation.add(new GifAnimator.Builder(
        getContext(),
        "sparkles",
        "sparkles",
        panel.figmaRect(20f, 20f, 120f, 120f)
));

foreground.add(new Button.Builder(
        getContext(),
        "claim",
        Color.BLUE,
        "CLAIM",
        panel.figmaRect(180f, 130f, 240f, 70f)
));
```

Child bounds are local to the container. Use:

```java
panel.getLocalBounds();                 // [0, 0, width, height]
panel.figmaRect(20f, 30f, 100f, 50f);  // local Figma units
panel.pxRect(20f, 30f, 100f, 50f);     // local runtime pixels
```

Moving or scaling the container transforms every nested layer together. With
`setClipToBounds(true)`, child drawing is clipped at the container edge.

## Ordering and lookup

```java
panel.bringLayerToFront("foreground");
panel.sendLayerToBack("animation");
panel.moveLayerAbove("animation", "content");
panel.moveLayerBelow("foreground", "animation");
panel.setLayerIndex("foreground", 2);

panel.findLayer("animation");
panel.findComponent("claim");
panel.findComponent("claim", Button.class);
panel.moveComponent("claim", "foreground");
panel.removeLayer("animation");
```

Layer IDs must be unique inside a container; component IDs remain unique throughout
the root `ZLayerGroup`, including nested containers.

## Interaction

Children receive touch from topmost to bottommost. An interactive child captures the
gesture before the container. A container listener handles touches on its otherwise
unconsumed surface:

```java
new ZLayerContainer.Builder(context, "reward", position, size)
        .setOnClickListener(id -> openReward())
        .setOnLongClickListener(id -> inspectReward())
        .setLongClickDelay(500L)
        .setPressedScale(0.92f)
        .setPressAnimationDuration(100L)
        .setSoundAction(this::playClick)
        .setHapticAction(this::performClickHaptic);
```

Without click listeners, the container itself is display-only; its children remain
interactive.

## Runtime API

```java
panel.getId();
panel.getBounds();
panel.getLocalBounds();
panel.getDimensionScale();

panel.setRegion(position, size);
panel.setRegion(rectF);
panel.horizontalCenter(true);
panel.verticalCenter(true);
panel.setTranslation(12f, 20f);
panel.resetTranslation();
panel.setClipToBounds(true);
panel.setAlpha(0.8f);
panel.setVisible(true);
panel.setEnabled(true);
```

Region movement keeps drawing and touch bounds synchronized:

```java
panel.animateRegionTo(
        destination,
        650L,
        ZLayerContainer.Interpolator.EASE_IN_OUT,
        () -> onMoveFinished()
);

panel.pauseMovement();
panel.resumeMovement();
panel.cancelMovement();
panel.finishMovement();
```

## Lifecycle

Add the container to a normal `ZLayer`; its owning `ZLayerGroup` handles drawing,
touch dispatch, invalidation, nested IME registration, and release:

```java
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

Calling `release()` is idempotent and releases every nested child.

## Migration from the removed API

Replace each old specialized layer with the corresponding ordinary component:

| Removed type | Use inside a nested `ZLayer` |
|---|---|
| `BitmapLayer` | `Image` |
| `GifLayer` | `GifAnimator` |
| `LottieLayer` | `LottieAnimator` |
| `DynamicLayer` | `DynamicViewAnimator` |
| `AfterEffectLayer` | `AfterEffectAnimator` |

This removes duplicate layer adapters, bounds policies, and playback ownership. Each
child now keeps its own public API and lifecycle while `ZLayerContainer` provides only
generic composition.

# CustomAnimatorComponent

`CustomAnimatorComponent` is a generic layered Canvas element. It can combine bitmap,
GIF, Lottie, dynamic Canvas, and After Effects content in one region, with optional
button-like interaction and movement.

It is not an Android XML `Button`. The future primary `Button` will provide a simpler
API for ordinary text, icon, bitmap, and background buttons.

Packages:

```java
import com.ogfa.nativeviews.animator.component.CustomAnimatorComponent;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;
import com.ogfa.nativeviews.animator.component.layer.ComponentLayer;
```

## Supported layers

Layers draw in list order: the first is at the back and the last is on top.

```text
BitmapLayer
GifLayer
LottieLayer
DynamicLayer
AfterEffectLayer
```

Every layer implements:

```java
public interface ComponentLayer {
    void draw(Canvas canvas);
    void release();
    void setBounds(RectF bounds);
}
```

## Minimal bitmap component

```java
private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer components = ui.addLayer("components");
```

```java
Position position = new Position(
        this,
        Position.HorizontalMarginFrom.LEFT,
        Position.VerticalMarginFrom.TOP,
        48f,
        450f
);

CustomAnimatorComponent play = components.add(
        new CustomAnimatorComponent.Builder(
                getContext(),
                "play",
                playBitmap,
                position
        )
                .setClickListener(id -> startGame())
                .setPressScale(0.92f)
);
```

The bitmap constructor creates `BitmapLayer` internally. Its bitmap dimensions are
treated as Figma-space dimensions.

## Host integration

```java
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
```

The group automatically calls `postInvalidateOnAnimation()` while non-empty so
animated layers and press effects continue rendering.

Disable this for a completely static group:

```java
components.setAutoInvalidate(false);
boolean enabled = components.isAutoInvalidate();
```

## Builder regions

Explicit runtime bounds:

```java
new CustomAnimatorComponent.Builder(
        context,
        "play",
        bitmap,
        new RectF(left, top, right, bottom)
);
```

```java
new CustomAnimatorComponent.Builder(
        context,
        "play",
        layers,
        bounds
);
```

Position-derived bitmap bounds:

```java
new CustomAnimatorComponent.Builder(
        context,
        "play",
        bitmap,
        position
);
```

Position-derived layered bounds:

```java
new CustomAnimatorComponent.Builder(
        context,
        "free_coin",
        layers,
        position
);
```

The layered `Position` overload selects the largest `BitmapLayer` by rendered area.
At least one bitmap layer is therefore required. Use explicit bounds when a layered
component has no bitmap.

The planned shared region API will add `Position + Size` overloads and remove the
requirement to infer layered bounds from a bitmap.

Separate movement bounds for `DynamicLayer`:

```java
new CustomAnimatorComponent.Builder(
        context,
        "moving_component",
        layers,
        componentBounds,
        dynamicLayerBounds
);
```

## Layer creation

### BitmapLayer

```java
BitmapLayer bitmapLayer = BitmapLayer.create(bitmap, bounds);
BitmapLayer positioned = BitmapLayer.create(bitmap, position);
```

Canvas stretches the bitmap into the resolved region. Keep assets in `drawable-nodpi`
or assets when bitmap dimensions represent Figma dimensions.

### LottieLayer

Cached animation:

```java
LottieLayer lottie = LottieLayer.create(
        "win_animation",
        bounds
);
```

Asset fallback:

```java
LottieLayer lottie = LottieLayer.create(
        context,
        "win_animation.json",
        bounds
);
```

Position plus bitmap-sized bounds:

```java
LottieLayer lottie = LottieLayer.create(
        context,
        "win_animation",
        position,
        referenceBitmap
);
```

### GifLayer

Cached animation:

```java
GifLayer gif = GifLayer.create("carrom_pass_buy", bounds);
```

Asset fallback:

```java
GifLayer gif = GifLayer.create(
        context,
        "carrom_pass_buy.gif",
        bounds
);
```

### DynamicLayer

```java
DynamicLayer dynamic = DynamicLayer.create(
        customDynamicView,
        bounds
);
```

The supplied dynamic view draws into the component's Canvas lifecycle.

### AfterEffectLayer

```java
AfterEffectLayer effect = AfterEffectLayer.create(
        afterEffectAnimator,
        bounds
);
```

The layer delegates frame drawing and cleanup to the After Effects animation system.

## Layered example

```java
ArrayList<ComponentLayer> layers = new ArrayList<>();
layers.add(BitmapLayer.create(background, bounds));
layers.add(GifLayer.create("reward_glow", bounds));
layers.add(LottieLayer.create("reward_icon", iconBounds));

components.add(
        new CustomAnimatorComponent.Builder(
                getContext(),
                "reward",
                layers,
                bounds
        )
                .setClickListener(id -> claimReward())
                .setPressScale(0.92f)
                .setSoundAction(this::playRewardSound)
);
```

## Interaction

Normal click:

```java
.setClickListener(id -> openShop())
```

Explicit click enable state:

```java
.setClickListener(id -> openShop(), true)
```

Long click:

```java
.setOnLongClickListener(id -> showShopHelp(), true)
```

Long press is detected after 500 ms. When long-click is enabled, also provide a normal
click listener for shorter presses.

Pressed scale:

```java
.setPressScale(0.90f)
```

The default is `0.96f`; `1f` disables the visible shrink.

Custom sound or haptic:

```java
.setSoundAction(() ->
        performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP
        ))
```

## Default button sound

If no sound action is supplied, the component uses:

```text
nativeviews/audio/sfx/g_button.mp3
```

The SDK preloads it asynchronously once per process and reuses the `SoundPool` entry.

Optional global controls:

```java
NativeViewsSoundPlayer.preload(context);
NativeViewsSoundPlayer.isButtonSoundLoaded();
NativeViewsSoundPlayer.playButtonSound(context);

NativeViewsSoundPlayer.setButtonSoundOverride(
        () -> appSoundManager.playButton()
);

NativeViewsSoundPlayer.setButtonSoundOverride(null);
NativeViewsSoundPlayer.release();
```

A component-specific `setSoundAction()` has priority over the global/default sound.

## Group API

```java
CustomAnimatorComponent added = components.add(builder);
components.add(existingComponent);

CustomAnimatorComponent found = components.find("play");
components.contains("play");
components.remove("play");

components.size();
components.isEmpty();

ui.draw(canvas);
components.drawVisible(canvas);
components.drawVisible(canvas, scrollView);

ui.onTouchEvent(event);
components.onScrollChanged();

components.clear();
ui.release();
components.close();
```

IDs must be unique. The group draws from first to last and dispatches touch from last
to first, giving the topmost overlapping component priority.

`onScrollChanged()` cancels active pressed states to prevent accidental clicks while
scrolling.

## Visibility drawing

Draw only components intersecting the host:

```java
components.drawVisible(canvas);
```

Or use another view as the visible viewport:

```java
components.drawVisible(canvas, scrollView);
```

## Movement

```java
components.animateToPosition(
        "reward",
        targetLeft,
        targetTop,
        350L,
        this::onMovementFinished
);
```

Direct component API:

```java
component.animateToPositionWithValueAnimator(
        targetLeft,
        targetTop,
        350L,
        hostView,
        onComplete
);
```

Drawing and touch bounds move together. `BitmapLayer` and `DynamicLayer` currently
follow bounds changes. Lottie, GIF, and After Effects layer movement remains part of
their API-hardening work.

## Component state and public API

```java
component.getId();
component.getBounds();
component.getLeft();
component.getTop();
component.draw(canvas);
component.onTouchEvent(event);
```

Builder summary:

| API | Purpose |
|---|---|
| `Builder(context, id, bitmap, RectF)` | Bitmap component with runtime bounds |
| `Builder(context, id, layers, RectF)` | Layered component with runtime bounds |
| `Builder(context, id, bitmap, Position)` | Bitmap component with Figma placement |
| `Builder(context, id, layers, Position)` | Bounds from largest bitmap layer |
| `Builder(..., bounds, dynamicBounds)` | Separate dynamic movement bounds |
| `setClickListener(listener)` | Enable normal clicks |
| `setClickListener(listener, enabled)` | Store listener with explicit state |
| `setOnLongClickListener(listener, enabled)` | Enable long-click |
| `setPressScale(scale)` | Configure pressed scale |
| `setSoundAction(action)` | Replace default sound |
| `build()` | Create without adding to a group |

## Manual collection API

`ZLayerGroup` is recommended. Static compatibility helpers also
exist for manual `ArrayList<CustomAnimatorComponent>` ownership:

```java
CustomAnimatorComponent.draw(canvas, list);
CustomAnimatorComponent.drawVisible(canvas, list, view);
CustomAnimatorComponent.getVisible(source, output, view);
CustomAnimatorComponent.handleTouch(event, list);
CustomAnimatorComponent.handleTouchScrollChanged(list);
CustomAnimatorComponent.addComponent(list, component);
CustomAnimatorComponent.removeComponent(id, list);
CustomAnimatorComponent.findComponentById(id, list);
CustomAnimatorComponent.releaseResources(list);
```

## Custom layer

```java
public final class BadgeLayer implements ComponentLayer {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    public BadgeLayer(RectF bounds) {
        this.bounds.set(bounds);
        paint.setColor(Color.RED);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawOval(bounds, paint);
    }

    @Override
    public void setBounds(RectF bounds) {
        this.bounds.set(bounds);
    }

    @Override
    public void release() {
    }
}
```

## Test activity

```text
app.builderx.ogfa.androiduicomponents.CustomAnimatorComponentTestActivity
```

It exercises bitmap, Lottie, GIF, dynamic, and After Effects layers, drawing, touch,
press effects, movement, and cleanup.

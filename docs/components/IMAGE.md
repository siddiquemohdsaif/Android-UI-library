# Image

`Image` is a standalone Canvas bitmap component. It is independent from
`CustomAnimatorComponent` and its `BitmapLayer`; use `Image` when a bitmap only
needs normal rendering, z-order, runtime updates, and optional click behavior.

```java
import com.ogfa.nativeviews.image.Image;
```

## Position and Size

```java
Image avatar = content.add(
        new Image.Builder(
                getContext(),
                "player_avatar",
                avatarBitmap,
                position,
                new Size(240f, 240f)
        )
                .setScaleType(Image.ScaleType.CENTER_CROP)
                .setOnClickListener(id -> openPlayerProfile())
);
```

`Position` and `Size` are Figma-space values scaled from the configured reference
width.

## Runtime RectF

```java
Image logo = content.add(
        new Image.Builder(
                getContext(),
                "game_logo",
                logoBitmap,
                new RectF(40f, 80f, 440f, 280f)
        )
                .setScaleType(Image.ScaleType.FIT_CENTER)
);
```

`RectF` values are used directly as runtime pixels.

## Scale types

| Scale type | Behavior |
|---|---|
| `FIT_CENTER` | Preserve aspect ratio and show the complete bitmap |
| `CENTER_CROP` | Preserve aspect ratio, fill the region, and crop overflow |
| `FIT_XY` | Stretch the bitmap to exactly fill the region |

`FIT_CENTER` is the default.

## Builder API

```java
.setScaleType(Image.ScaleType.CENTER_CROP)
.setAlpha(0.8f)
.setFilterBitmap(true)
.setVisible(true)
.setEnabled(true)
.setOnClickListener(listener)
```

Alpha must be in the `0f..1f` range. Bitmap filtering is enabled by default.

## Runtime API

Read state:

```java
image.getId();
image.getBitmap();
image.getBounds();
image.getScaleType();
image.getFigmaConfig();
image.getDimensionScale();
image.getAlpha();
image.isFilterBitmap();
image.isVisible();
image.isEnabled();
image.isClickable();
```

Update the bitmap and region:

```java
image.setBitmap(newBitmap);
image.setRegion(position, size);
image.setRegion(rectF);
```

Update rendering:

```java
image.setScaleType(Image.ScaleType.CENTER_CROP);
image.setAlpha(0.7f);
image.setFilterBitmap(true);
```

Update state and click behavior:

```java
image.setVisible(false);
image.setEnabled(false);
image.setOnClickListener(listener);
image.removeOnClickListener();
```

All mutating methods return the same `Image` instance for chaining.

## Click behavior

Click behavior matches `Text`:

- Down and up must occur inside the resolved image region.
- Moving outside cancels the click.
- `ACTION_CANCEL` cancels the click.
- Hidden, disabled, and non-clickable images ignore touch.
- The entire rectangular region is clickable, including transparent pixels.
- `ZLayer` dispatches to the topmost component first.
- No sound, haptic, scaling, or animation is applied automatically.

```java
image.setOnClickListener(id -> openDetails(id));
```

## Drawing and touch delegation

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
```

## Bitmap ownership

`Image` does not recycle the supplied bitmap. A bitmap may therefore be shared
between components. The application remains responsible for recycling a bitmap
after every component using it has been released.

Null, recycled, or zero-dimension bitmaps produce clear exceptions.

```java
ui.release();

if (!bitmap.isRecycled()) {
    bitmap.recycle();
}
```

## Test activity

```powershell
adb shell am start -n `
  app.builderx.ogfa.androiduicomponents/.ImageTestActivity
```

The activity covers both region paths, all scale types, alpha, bitmap
replacement, every runtime setter, ZLayer lookup, clicks, and cleanup.

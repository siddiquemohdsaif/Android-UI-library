# Card

`Card` is a composite Canvas component with a background, rounded clipping,
an outside drop shadow, and one privately owned content `ZLayer`.

```text
Card
├── DropShadow
├── color or bitmap background
└── content ZLayer
    └── any number of mixed Components
```

Children are drawn only by Card, but their IDs and nested `TextField` input
ownership are registered with the root `ZLayerGroup`.

## Create a color Card

```java
Card card = screen.add(new Card.Builder(
        context,
        "profile_card",
        position,
        new Size(900f, 620f)
)
        .setBackgroundColor(Color.WHITE)
        .setCornerRadius(36f)
        .setDropShadow(new DropShadow(
                0f,
                4f,
                28f,
                4f,
                Color.argb(5, 0, 0, 0)
        )));
```

White and the shown `DropShadow` are defaults:

```java
Card card = screen.add(new Card.Builder(
        context,
        "profile_card",
        position,
        new Size(900f, 620f)
));
```

Explicit runtime bounds:

```java
new Card.Builder(context, "profile_card", runtimeRectF);
```

## Add mixed content

```java
ZLayer content = card.getContentLayer();

content.add(new Text.Builder(...));
content.add(new Image.Builder(...));
content.add(new TextField.Builder(...));
content.add(new Button.Builder(...));
```

The content layer uses the complete card region as its clip. Child regions use
the normal screen `Position + Size` or `RectF` APIs and must be placed over the
Card. Rounded Card clipping prevents their pixels from escaping.

Rendering follows insertion order; touch follows reverse order. The root can
find nested children:

```java
ui.findComponent("profile_title", Text.class);
ui.findComponent("profile_name", TextField.class);
```

Use a blocking policy when empty Card space must consume touch:

```java
card.getContentLayer().setTouchPolicy(
        ZLayer.TouchPolicy.BLOCK_BELOW
);
```

## Image background

```java
Card imageCard = screen.add(new Card.Builder(
        context,
        "reward_card",
        position,
        new Size(900f, 500f)
)
        .setBackgroundImage(bitmap)
        .setBackgroundScaleType(Image.ScaleType.CENTER_CROP)
        .setCornerRadius(40f));
```

Runtime switching:

```java
card.setBackgroundColor(0xffffffff);
card.setBackgroundImage(bitmap);
card.setBackgroundScaleType(Image.ScaleType.FIT_XY);
card.setFilterBitmap(true);
```

Card never recycles caller-provided bitmaps.

## Corner radius

```java
.setCornerRadius(36f)
.setCornerRadiusPx(36f)
```

Normal radius is Figma-scaled with `Position + Size` and runtime pixels with
`RectF`. The `Px` form is always runtime pixels. It is clamped to half the
shortest side.

```java
card.getCornerRadius();
card.getResolvedCornerRadius();
card.isCornerRadiusInPixels();
```

## DropShadow

```java
DropShadow shadow = new DropShadow(
        0f,         // x offset
        12f,        // y offset
        32f,        // blur
        4f,         // spread
        0x40000000  // ARGB
);
```

Normal measurements follow the Card region scale:

```java
.setDropShadow(shadow)
```

Always-runtime measurements:

```java
.setDropShadowPx(shadow)
```

Management:

```java
card.getDropShadow();
card.getResolvedDropShadow();
card.isDropShadowInPixels();

card.setDropShadow(shadow);
card.setDropShadowPx(shadow);
card.removeDropShadow();
card.resetDefaultDropShadow();
```

The shadow renders outside `getBounds()`. Its complete drawing region is:

```java
card.getVisualBounds();
```

Touch uses Card bounds and never the shadow bounds.

`Color.argb(5, 0, 0, 0)` is an alpha value of 5/255. For approximately 5%
opacity, use `Color.argb(13, 0, 0, 0)`.

## Builder API

```java
new Card.Builder(context, id, position, size);
new Card.Builder(context, id, rectF);

.setBackgroundColor(color)
.setBackgroundImage(bitmap)
.setBackgroundScaleType(scaleType)
.setFilterBitmap(true)

.setCornerRadius(radius)
.setCornerRadiusPx(radius)

.setDropShadow(shadow)
.setDropShadowPx(shadow)
.removeDropShadow()

.setAlpha(alpha)
.setVisible(visible)
.setEnabled(enabled)
```

## Runtime API

```java
card.getId();
card.getBounds();
card.getVisualBounds();
card.getContentBounds();
card.getContentLayer();

card.getBackgroundType();
card.getBackgroundColor();
card.getBackgroundImage();
card.getBackgroundScaleType();

card.getCornerRadius();
card.getResolvedCornerRadius();
card.isCornerRadiusInPixels();

card.getDropShadow();
card.getResolvedDropShadow();
card.isDropShadowInPixels();

card.setRegion(position, size);
card.setRegion(rectF);
card.setAlpha(0.8f);
card.setVisible(false);
card.setEnabled(false);
```

Changing Card bounds does not rewrite arbitrary child regions. Rebuild or
update the child regions when moving an already populated Card.

## Drawing and touch

The draw order is:

```text
outside shadow
rounded Card clip
background
content components
```

On touch, Card asks its content layer for the topmost child. Card retains that
child as the gesture target through move, up, or cancel. Nested `TextField`
instances delegate focus and IME operations to the root `ZLayerGroup`.

Release the root normally:

```java
ui.release();
```

This releases the Card and its complete content layer without recycling
caller-owned bitmaps.

# Card

`Card` is a composite Canvas component with a background, rounded clipping,
an outside drop shadow, and an ordered stack of privately owned content
`ZLayer`s.

```text
Card
├── DropShadow
├── color or bitmap background
└── content ZLayers (bottom to top)
    └── any number of mixed Components per layer
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

## Parent-relative centering

Center the complete Card region inside its owning ZLayer without calculating
Figma margins:

```java
Card card = screen.add(new Card.Builder(
        context,
        "centered_card",
        new Position(
                hostView,
                figmaConfig,
                Position.HorizontalMarginFrom.LEFT,
                Position.VerticalMarginFrom.BOTTOM,
                0f,
                163f
        ),
        new Size(742f, 849f)
)
        .horizontalCenter(true));
```

Both axes are supported:

```java
.horizontalCenter(true)
.verticalCenter(true)
```

A root ZLayer centers against the full host view. A Card nested in another
Card's content ZLayer centers against the parent Card. Centering keeps the
declared Card size; it only replaces placement on the enabled axis. Disabling
an axis restores the corresponding position resolved from the original
`Position` or `RectF`:

```java
card.setHorizontalCenter(false);
card.setVerticalCenter(false);
```

The background, rounded clip, content layer, touch region, and outside shadow
all move with the Card.

## Add mixed content

```java
ZLayer content = card.getContentLayer();

content.add(new Text.Builder(...));
content.add(new Image.Builder(...));
content.add(new TextField.Builder(...));
content.add(new Button.Builder(...));
```

`getContentLayer()` returns the default layer for compact Cards. Add dedicated
layers when one group needs independent ordering, touch policy, or visibility:

```java
ZLayer labels = card.getContentLayer();
ZLayer fields = card.addContentLayer("fields");
ZLayer actions = card.addContentLayer("actions");

fields.add(new TextField.Builder(...));

card.findContentLayer("fields");
card.getContentLayers();
```

New Card content layers are drawn above earlier layers and checked first for
touch. All layers remain clipped by the Card's rounded region. Component IDs
remain globally unique in the root `ZLayerGroup`.

### Card-owned layer translation

A Card and all of its content layers form one composite. Calling translation
on any Card-owned layer moves the complete Card—not only that layer:

```java
card.getContentLayer().setTranslationY(keyboardOffsetPx);
```

This moves the shadow, background, rounded clip, default content layer, and all
additional content layers together. Root layers outside the Card remain fixed.
Every Card-owned layer reports the same owner translation through
`getTranslationX()` and `getTranslationY()`.

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

.horizontalCenter(enabled)
.verticalCenter(enabled)

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
card.addContentLayer("fields");
card.findContentLayer("fields");
card.getContentLayers();

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

card.isHorizontalCentered();
card.isVerticalCentered();
card.setHorizontalCenter(true);
card.setVerticalCenter(true);
card.horizontalCenter(false);
card.verticalCenter(false);

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
content layers from bottom to top
```

On touch, Card asks its content layers from top to bottom for the topmost child.
Card retains both that layer and child as the gesture target through move, up,
or cancel, applying the Card owner's inverse translation throughout. Nested `TextField`
instances delegate focus and IME operations to the root `ZLayerGroup`.

Release the root normally:

```java
ui.release();
```

This releases the Card and all its content layers without recycling
caller-owned bitmaps.

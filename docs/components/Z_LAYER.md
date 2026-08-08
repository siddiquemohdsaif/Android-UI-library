# ZLayerGroup and ZLayer

`ZLayerGroup` is the single Canvas scene owner. It replaces separate groups for each
component type.

```text
ZLayerGroup
└── ZLayer (bottom to top)
    └── Component (bottom to top)
```

## Create and populate

```java
private final ZLayerGroup ui = new ZLayerGroup(this);
private final ZLayer content = ui.addLayer("content");
private final ZLayer dialog = ui.addLayer("dialog");

Text title = content.add(new Text.Builder(...));
TextField field = content.add(new TextField.Builder(...));
ZLayerContainer reward =
        content.add(new ZLayerContainer.Builder(...));
```

Builders implement `ComponentFactory<T>`. Component IDs are globally unique.

## Host integration

```java
@Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ui.draw(canvas);
}

@Override public boolean onTouchEvent(MotionEvent event) {
    return ui.onTouchEvent(event) || super.onTouchEvent(event);
}

@Override public boolean onCheckIsTextEditor() {
    return ui.onCheckIsTextEditor();
}

@Override public InputConnection onCreateInputConnection(EditorInfo attrs) {
    InputConnection result = ui.onCreateInputConnection(attrs);
    return result != null ? result : super.onCreateInputConnection(attrs);
}

@Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    return ui.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
}

@Override protected void onDetachedFromWindow() {
    ui.release();
    super.onDetachedFromWindow();
}
```

## Ordering and lookup

New layers/components go on top. Drawing is bottom-to-top; touch is top-to-bottom and
the `ACTION_DOWN` target captures the rest of its gesture.

```java
ui.bringLayerToFront("dialog");
ui.sendLayerToBack("background");
ui.moveLayerAbove("effects", "content");
ui.moveLayerBelow("content", "dialog");
ui.setLayerIndex("content", 1);

content.bringToFront("title");
content.sendToBack("background");
content.moveAbove("title", "image");
content.moveBelow("image", "title");
content.setComponentIndex("title", 2);

ui.findLayer("content");
ui.findComponent("title");
ui.findComponent("title", Text.class);
ui.moveComponent("title", "dialog");
```

## Touch policies

```java
ZLayer.TouchPolicy.PASS_THROUGH
ZLayer.TouchPolicy.BLOCK_BELOW
ZLayer.TouchPolicy.MODAL
```

- `PASS_THROUGH`: continue to lower layers if no child handles down.
- `BLOCK_BELOW`: block lower layers inside any visible child region.
- `MODAL`: block all lower layers while visible and enabled.

```java
dialog.setTouchPolicy(ZLayer.TouchPolicy.MODAL);
```

## Layer translation

Translate a root-owned layer without moving sibling layers:

```java
formLayer.setTranslationY(-240f);

formLayer.setTranslationX(40f);
formLayer.setTranslation(40f, -240f);
formLayer.resetTranslation();
```

Layer translation uses runtime pixels because it represents transient render
state such as keyboard avoidance, scrolling, or drag motion. Drawing and hit
testing use the same translation. The layer automatically applies inverse
coordinates to the complete captured gesture, including move, up, and cancel,
so callers must forward the original `MotionEvent` unchanged.

```java
ui.onTouchEvent(event); // Do not manually offset for ZLayer translation.
```

Card-owned layers are the deliberate exception. A Card is one visual and touch
composite, so translating any of its content layers delegates translation to
the Card owner. The background, shadow, rounded clip, and every content layer
move together:

```java
card.getContentLayer().setTranslationY(-240f);
```

Sibling root layers outside the Card remain fixed.

## Shared contract

```java
public interface Component {
    String getId();
    RectF getBounds();
    void draw(Canvas canvas);
    boolean onTouchEvent(MotionEvent event);
    boolean isVisible();
    boolean isEnabled();
    void attach(ComponentHost host);
    void release();
}
```

`Text`, `TextField`, `Image`, `Button`, `Card`, all animator components, and
`ZLayerContainer` implement it. Every future visual component must implement
`Component`/`ComponentFactory` and support
`Position + Size` and `RectF`.

## State and lifecycle

```java
layer.setVisible(false); // Not drawn or touched.
layer.setEnabled(false); // Drawn, but not touched.
layer.setTouchPolicy(ZLayer.TouchPolicy.MODAL);
layer.setTranslationY(-240f);

layer.getTranslationX();
layer.getTranslationY();
layer.resetTranslation();

layer.find("title");
layer.contains("title");
layer.remove("title");
layer.size();
layer.isEmpty();
layer.getComponents();

ui.setAutoInvalidate(true);
ui.getFocusedTextField();

layer.clear();
ui.clear();
ui.release();
ui.close();
```

There are no compatibility wrappers. Type-specific component groups were deleted.

## Test activity

```text
app.builderx.ogfa.androiduicomponents.ZLayerTestActivity
```

It combines multiple component types in one scene and
verifies unique IDs, typed lookup, layer/component ordering, cross-layer movement,
pass-through effects, modal blocking, callbacks, shared IME, and cleanup.

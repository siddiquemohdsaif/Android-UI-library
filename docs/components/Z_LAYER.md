# ZLayerGroup and ZLayer

`ZLayerGroup` is the single Canvas scene owner. It replaces `TextGroup`,
`TextFieldGroup`, and `CustomAnimatorComponentGroup`.

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
CustomAnimatorComponent reward =
        content.add(new CustomAnimatorComponent.Builder(...));
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

`Text`, `TextField`, and `CustomAnimatorComponent` implement it. Every future visual
component must implement `Component`/`ComponentFactory` and support
`Position + Size` and `RectF`.

## State and lifecycle

```java
layer.setVisible(false); // Not drawn or touched.
layer.setEnabled(false); // Drawn, but not touched.
layer.setTouchPolicy(ZLayer.TouchPolicy.MODAL);

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

There are no compatibility wrappers. `TextGroup`, `TextFieldGroup`, and
`CustomAnimatorComponentGroup` were deleted.

## Test activity

```text
app.builderx.ogfa.androiduicomponents.ZLayerTestActivity
```

It combines `Text`, `TextField`, and `CustomAnimatorComponent` in one scene and
verifies unique IDs, typed lookup, layer/component ordering, cross-layer movement,
pass-through effects, modal blocking, callbacks, shared IME, and cleanup.

# ComponentList

`ComponentList<T>` is a virtualized, Canvas-native scrolling component. Each reusable
item owns ordered `ZLayer`s and may contain any Native Views component, including
`Text`, `Image`, `Button`, `Card`, and `TextField`.

It is added directly to a normal `ZLayer`; no separate list group is required.

## Create a list

Every list supports the shared region forms:

```java
// Figma-space region.
new ComponentList.Builder<Player>(
        context,
        "players",
        position,
        new Size(972f, 1450f)
);

// Direct runtime-pixel region.
new ComponentList.Builder<Player>(
        context,
        "players",
        new RectF(left, top, right, bottom)
);
```

Add the configured builder to a layer:

```java
ComponentList<Player> players = content.add(
        new ComponentList.Builder<Player>(
                getContext(),
                "players",
                position,
                new Size(972f, 1450f)
        )
                .setOrientation(ComponentList.Orientation.VERTICAL)
                .setItemSize(180f)
                .setItemSpacing(20f)
                .setPadding(12f)
                .setAdapter(adapter)
                .setOnItemClickListener((list, player, position) ->
                        openPlayer(player))
);
```

`setItemSize()` is the item height for a vertical list and item width for a
horizontal list. Items fill the other axis unless `setItemCrossSize()` is supplied.

## Adapter and reusable items

The adapter owns the data and separates one-time component creation from repeated
data binding:

```java
final class PlayerAdapter extends ComponentList.Adapter<Player> {
    private final List<Player> values;

    PlayerAdapter(List<Player> values) {
        this.values = values;
    }

    @Override public int getItemCount() {
        return values.size();
    }

    @Override public Player getItem(int position) {
        return values.get(position);
    }

    @Override public long getItemId(int position) {
        return values.get(position).id;
    }

    @Override
    public void onCreateItem(ComponentList.Item item, int viewType) {
        ComponentList.ItemScope scope = item.getScope();
        ZLayer background = item.addLayer("background");
        ZLayer content = item.addLayer("content");

        background.add(new Card.Builder(
                context,
                scope.id("surface"),
                scope.rect(0f, 0f, scope.width(), scope.height())
        )
                .setBackgroundColor(Color.WHITE)
                .setCornerRadiusPx(scope.px(24f))
                .removeDropShadow());

        content.add(new Text.Builder(
                context,
                scope.id("name"),
                "",
                scope.rect(32f, 20f, scope.width() - 64f, 60f)
        )
                .setTextSizePx(scope.px(32f))
                .setTextColor(Color.BLACK));
    }

    @Override
    public void onBindItem(
            ComponentList.Item item,
            Player player,
            int position
    ) {
        item.find("name", Text.class).setText(player.name);
    }
}
```

`onCreateItem()` runs only when the list needs another holder. `onBindItem()` runs
whenever that holder represents a different adapter position. Do not capture the
initial data position in component callbacks; obtain the current position through
`item.getPosition()`.

Optional adapter methods:

```java
adapter.getItemViewType(position);
adapter.onItemRecycled(item);
```

Different view types use independent reuse pools.

## Item-local positioning

An item is drawn in a translated local coordinate space. The scope converts local
Figma measurements to runtime bounds:

```java
RectF bounds = scope.rect(
        20f,  // left inside item
        15f,  // top inside item
        300f, // width
        80f   // height
);

float itemWidth = scope.width();   // Figma-space width
float itemHeight = scope.height(); // Figma-space height
float runtimeValue = scope.px(24f);
```

Because `scope.rect()` returns an explicit runtime `RectF`, use the component's `Px`
setter for other Figma values converted through `scope.px()`:

```java
.setTextSizePx(scope.px(32f))
.setCornerRadiusPx(scope.px(24f))
```

The list translates drawing and touch events automatically. Item code never adds the
list's screen position or current scroll offset.

## IDs and lookup

Every nested component must use `scope.id()`:

```java
scope.id("name");
scope.id("avatar");
```

The resulting holder-scoped ID is globally unique. Local item lookup remains short:

```java
item.find("name");
item.find("name", Text.class);
item.findLayer("content");
```

Look up visible data by stable adapter ID:

```java
ComponentList.Item item = list.findVisibleItem(player.id);

Text name = list.findVisibleComponent(
        player.id,
        "name",
        Text.class
);
```

Off-screen data has no active visible item and returns `null`.

## Fixed and variable item sizes

Fixed primary-axis size:

```java
.setItemSize(180f)
```

Variable Figma-space sizes:

```java
.setItemSizeProvider((player, position) ->
        player.expanded ? 280f : 180f)
```

Optional fixed cross-axis size:

```java
.setItemCrossSize(800f)
```

## Vertical and horizontal lists

```java
.setOrientation(ComponentList.Orientation.VERTICAL)
.setOrientation(ComponentList.Orientation.HORIZONTAL)
```

For a vertical list, vertical gestures scroll and fling. For a horizontal list, the
same behavior uses horizontal gestures.

## Touch behavior

The list first offers `ACTION_DOWN` to the topmost child under the finger. If movement
crosses Android's touch slop, it sends `ACTION_CANCEL` to that child and takes control
of the gesture for scrolling. This prevents accidental child clicks while swiping.

Whole-item callbacks:

```java
list.setOnItemClickListener((source, player, position) ->
        openPlayer(player));

list.setOnItemLongClickListener((source, player, position) -> {
    showPlayerActions(player);
    return true;
});
```

A child that handles the gesture takes priority over the whole-item click. Long-click
is scheduled only when no child captured the initial press.

## Scrolling

```java
list.scrollToPosition(10);
list.smoothScrollToPosition(10);

list.scrollBy(0f, 120f);
list.smoothScrollBy(0f, 120f);
list.stopScroll();

float offset = list.getScrollOffset();
int first = list.getFirstVisiblePosition();
int last = list.getLastVisiblePosition();

boolean canGoBack = list.canScrollBackward();
boolean canGoForward = list.canScrollForward();
```

Builder and runtime controls:

```java
.setScrollEnabled(true)
.setFlingEnabled(true)
.setOverscrollEnabled(false)
.setClipToBounds(true)
.setInitialPosition(0)
```

Clipping is enabled by default. Overscroll is disabled by default; content remains
clamped to the viewport edges.

## Spacing and padding

Figma-aware defaults:

```java
list.setItemSpacing(16f);
list.setPadding(20f);
list.setPadding(20f, 16f, 20f, 16f);
```

Exact runtime pixels:

```java
list.setItemSpacingPx(16f);
list.setPaddingPx(20f);
list.setPaddingPx(20f, 16f, 20f, 16f);
```

## Region, alignment, and state

```java
list.setRegion(position, size);
list.setRegion(rectF);

list.horizontalCenter(true);
list.verticalCenter(true);

list.setAlpha(0.8f);
list.setVisible(true);
list.setEnabled(true);
```

## Data updates

After changing adapter-owned data, use either the adapter convenience methods or the
matching list methods:

```java
adapter.notifyDataSetChanged();
adapter.notifyItemChanged(position);
adapter.notifyItemInserted(position);
adapter.notifyItemRemoved(position);
adapter.notifyItemMoved(from, to);
adapter.notifyItemRangeChanged(start, count);
```

Stable IDs from `getItemId()` allow visible data lookup to remain independent from
holder reuse.

## Host integration and release

The host custom view uses the same `ZLayerGroup` bridge as other components:

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ui.draw(canvas);
}

@Override
public boolean onTouchEvent(MotionEvent event) {
    return ui.onTouchEvent(event) || super.onTouchEvent(event);
}

@Override
protected void onDetachedFromWindow() {
    ui.release();
    super.onDetachedFromWindow();
}
```

Release stops an active fling, cancels touch, unregisters nested components, releases
all visible and pooled holders, and detaches the adapter.


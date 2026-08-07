package com.ogfa.nativeviews.list;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.NestedComponentHost;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerOwner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A virtualized, Canvas-native scrolling list whose reusable items contain ZLayers.
 */
public final class ComponentList<T> implements Component {

    public enum Orientation { VERTICAL, HORIZONTAL }

    public interface ItemSizeProvider<T> {
        float getItemSize(T item, int position);
    }

    public interface OnItemClickListener<T> {
        void onItemClick(ComponentList<T> list, T item, int position);
    }

    public interface OnItemLongClickListener<T> {
        boolean onItemLongClick(ComponentList<T> list, T item, int position);
    }

    /** Data and reusable-item binding contract. */
    public abstract static class Adapter<T> {
        private ComponentList<T> owner;

        public abstract int getItemCount();
        public abstract T getItem(int position);

        public long getItemId(int position) { return position; }
        public int getItemViewType(int position) { return 0; }
        public abstract void onCreateItem(Item item, int viewType);
        public abstract void onBindItem(Item item, T value, int position);
        public void onItemRecycled(Item item) { }

        public final void notifyDataSetChanged() {
            if (owner != null) owner.notifyDataSetChanged();
        }

        public final void notifyItemChanged(int position) {
            if (owner != null) owner.notifyItemChanged(position);
        }

        public final void notifyItemInserted(int position) {
            if (owner != null) owner.notifyItemInserted(position);
        }

        public final void notifyItemRemoved(int position) {
            if (owner != null) owner.notifyItemRemoved(position);
        }

        public final void notifyItemMoved(int fromPosition, int toPosition) {
            if (owner != null) owner.notifyItemMoved(fromPosition, toPosition);
        }

        public final void notifyItemRangeChanged(int start, int count) {
            if (owner != null) owner.notifyItemRangeChanged(start, count);
        }
    }

    /** A reusable item container. Components are addressed using local IDs. */
    public static final class Item {
        private final ComponentList<?> list;
        private final int holderNumber;
        private final int viewType;
        private final ArrayList<ZLayer> layers = new ArrayList<>();
        private final Map<String, ZLayer> layersByLocalId = new LinkedHashMap<>();
        private final Map<String, Component> componentsByLocalId = new LinkedHashMap<>();
        private final ItemScope scope;
        private int position = -1;
        private long itemId;
        private float originX;
        private float originY;
        private float width;
        private float height;
        private Component touchTarget;
        private ZLayer touchLayer;
        private final NestedComponentHost componentHost = new NestedComponentHost() {
            @Override public View getHostView() { return list.hostView; }
            @Override public RectF getComponentBounds() {
                return new RectF(0f, 0f, width, height);
            }
            @Override public void invalidateComponent() { list.invalidate(); }
            @Override public void postInvalidateComponentOnAnimation() {
                list.invalidateOnAnimation();
            }
            @Override public boolean requestFocus(TextField field) {
                return list.rootHost != null && list.rootHost.requestFocus(field);
            }
            @Override public void clearFocus(TextField field) {
                if (list.rootHost != null) list.rootHost.clearFocus(field);
            }
            @Override public void restartInput() {
                if (list.rootHost != null) list.rootHost.restartInput();
            }
            @Override public void updateSelection(TextField field) {
                if (list.rootHost != null) list.rootHost.updateSelection(field);
            }
            @Override public void registerNestedComponent(Component component) {
                if (list.rootHost != null) list.rootHost.registerNestedComponent(component);
            }
            @Override public void unregisterNestedComponent(Component component) {
                if (list.rootHost != null) list.rootHost.unregisterNestedComponent(component);
            }
        };

        private Item(ComponentList<?> list, int holderNumber, int viewType) {
            this.list = list;
            this.holderNumber = holderNumber;
            this.viewType = viewType;
            scope = new ItemScope(this);
        }

        public int getPosition() { return position; }
        public long getItemId() { return itemId; }
        public int getViewType() { return viewType; }
        public ItemScope getScope() { return scope; }

        public ZLayer addLayer(String localId) {
            String normalized = requireId(localId, "Item layer ID");
            if (layersByLocalId.containsKey(normalized)) {
                throw new IllegalArgumentException("Duplicate item layer ID: " + normalized);
            }
            ZLayer layer = new ZLayer(list.itemLayerOwner,
                    list.id + "/holder_" + holderNumber + "/layer_" + normalized);
            layers.add(layer);
            layersByLocalId.put(normalized, layer);
            return layer;
        }

        public ZLayer findLayer(String localId) { return layersByLocalId.get(localId); }

        public Component find(String localId) { return componentsByLocalId.get(localId); }

        public <C extends Component> C find(String localId, Class<C> type) {
            Component component = find(localId);
            return type.isInstance(component) ? type.cast(component) : null;
        }

        public List<ZLayer> getLayers() {
            return Collections.unmodifiableList(layers);
        }

        private void draw(Canvas canvas) {
            int save = canvas.save();
            canvas.translate(originX, originY);
            for (ZLayer layer : layers) layer.draw(canvas);
            canvas.restoreToCount(save);
        }

        private Component dispatchDown(MotionEvent event) {
            MotionEvent local = translatedEvent(event);
            try {
                for (int index = layers.size() - 1; index >= 0; index--) {
                    ZLayer layer = layers.get(index);
                    Component target = layer.dispatchDown(local);
                    if (target != null) {
                        touchTarget = target;
                        touchLayer = layer;
                        return target;
                    }
                }
                return null;
            } finally {
                local.recycle();
            }
        }

        private boolean dispatchToTarget(MotionEvent event) {
            if (touchTarget == null) return false;
            MotionEvent local = translatedEvent(event);
            try {
                return touchLayer == null
                        ? touchTarget.onTouchEvent(local)
                        : touchLayer.dispatchTo(touchTarget, local);
            } finally {
                local.recycle();
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    touchTarget = null;
                    touchLayer = null;
                }
            }
        }

        private void cancelTouch(MotionEvent source) {
            if (touchTarget == null) return;
            MotionEvent cancel = MotionEvent.obtain(source);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            dispatchToTarget(cancel);
            cancel.recycle();
        }

        private MotionEvent translatedEvent(MotionEvent event) {
            MotionEvent translated = MotionEvent.obtain(event);
            translated.offsetLocation(-originX, -originY);
            return translated;
        }

        private boolean contains(float x, float y) {
            return x >= originX && x <= originX + width
                    && y >= originY && y <= originY + height;
        }

        private void release() {
            for (ZLayer layer : new ArrayList<>(layers)) layer.clear();
            layers.clear();
            layersByLocalId.clear();
            componentsByLocalId.clear();
            touchTarget = null;
            touchLayer = null;
        }
    }

    /** Item-local Figma conversion, ID namespacing, and component registration. */
    public static final class ItemScope {
        private final Item item;

        private ItemScope(Item item) { this.item = item; }

        public Context getContext() { return item.list.hostView.getContext(); }
        public View getHostView() { return item.list.hostView; }
        public int getPosition() { return item.position; }
        public long getItemId() { return item.itemId; }

        public String id(String localId) {
            String normalized = requireId(localId, "Item component ID");
            return item.list.id + "/holder_" + item.holderNumber + "/" + normalized;
        }

        public float width() { return item.width / item.list.dimensionScale; }
        public float height() { return item.height / item.list.dimensionScale; }

        /** Converts a Figma-space value using the list's active reference scale. */
        public float px(float figmaValue) {
            return requireNonNegative(figmaValue, "Figma value")
                    * item.list.dimensionScale;
        }

        /** Returns item-local runtime bounds from Figma-space values. */
        public RectF rect(float left, float top, float width, float height) {
            requireNonNegative(left, "Item left");
            requireNonNegative(top, "Item top");
            requirePositive(width, "Item width");
            requirePositive(height, "Item height");
            float scale = item.list.dimensionScale;
            return new RectF(left * scale, top * scale,
                    (left + width) * scale, (top + height) * scale);
        }

        public RectF getBounds() { return new RectF(0f, 0f, item.width, item.height); }
    }

    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final OverScroller scroller;
    private final int touchSlop;
    private final int minimumFlingVelocity;
    private final int maximumFlingVelocity;
    private final LinkedHashMap<Integer, Item> visibleItems = new LinkedHashMap<>();
    private final Map<Integer, ArrayDeque<Item>> recycledItems = new HashMap<>();
    private final ArrayList<Float> itemStarts = new ArrayList<>();
    private final ArrayList<Float> itemSizes = new ArrayList<>();

    private final ZLayerOwner itemLayerOwner = new ZLayerOwner() {
        @Override public View getHostView() { return hostView; }

        @Override public void registerLayerComponent(Component component) {
            registerItemComponent(component);
        }

        @Override public void unregisterLayerComponent(Component component) {
            unregisterItemComponent(component);
        }

        @Override public void invalidateLayer() { invalidate(); }
    };

    private Adapter<T> adapter;
    private Orientation orientation;
    private ItemSizeProvider<T> itemSizeProvider;
    private float itemSize;
    private float itemCrossSize;
    private float itemSpacing;
    private boolean itemSpacingInPixels;
    private final float[] padding = new float[4];
    private boolean paddingInPixels;
    private boolean scrollEnabled;
    private boolean flingEnabled;
    private boolean overscrollEnabled;
    private boolean clipToBounds;
    private boolean visible;
    private boolean enabled;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private float alpha;
    private float dimensionScale;
    private float scrollOffset;
    private float contentLength;
    private int initialPosition;
    private int nextHolderNumber;
    private ComponentHost owner;
    private NestedComponentHost rootHost;
    private Item bindingItem;
    private Item touchItem;
    private boolean childHandledDown;
    private boolean dragging;
    private float downPrimary;
    private float lastPrimary;
    private VelocityTracker velocityTracker;
    private OnItemClickListener<T> itemClickListener;
    private OnItemLongClickListener<T> itemLongClickListener;
    private boolean longClickTriggered;
    private final Runnable longClickRunnable = () -> {
        Item item = touchItem;
        if (item == null || dragging || childHandledDown || itemLongClickListener == null
                || item.position < 0 || item.position >= adapter.getItemCount()) return;
        longClickTriggered = itemLongClickListener.onItemLongClick(
                this, adapter.getItem(item.position), item.position);
    };
    private boolean released;

    private ComponentList(Builder<T> builder, View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        id = requireId(builder.id, "ComponentList ID");
        adapter = Objects.requireNonNull(builder.adapter, "Adapter cannot be null.");
        orientation = builder.orientation;
        itemSizeProvider = builder.itemSizeProvider;
        itemSize = builder.itemSize;
        itemCrossSize = builder.itemCrossSize;
        itemSpacing = builder.itemSpacing;
        itemSpacingInPixels = builder.itemSpacingInPixels;
        System.arraycopy(builder.padding, 0, padding, 0, padding.length);
        paddingInPixels = builder.paddingInPixels;
        scrollEnabled = builder.scrollEnabled;
        flingEnabled = builder.flingEnabled;
        overscrollEnabled = builder.overscrollEnabled;
        clipToBounds = builder.clipToBounds;
        visible = builder.visible;
        enabled = builder.enabled;
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        alpha = builder.alpha;
        initialPosition = builder.initialPosition;
        itemClickListener = builder.itemClickListener;
        itemLongClickListener = builder.itemLongClickListener;
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        ViewConfiguration configuration = ViewConfiguration.get(hostView.getContext());
        touchSlop = configuration.getScaledTouchSlop();
        minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        scroller = new OverScroller(hostView.getContext());
        adapter.owner = this;
        rebuildLayout();
        if (initialPosition > 0) scrollToPosition(initialPosition);
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() { return new RectF(bounds); }
    public Orientation getOrientation() { return orientation; }
    public Adapter<T> getAdapter() { return adapter; }
    public float getScrollOffset() { return scrollOffset; }
    public boolean isScrollEnabled() { return scrollEnabled; }
    public boolean isFlingEnabled() { return flingEnabled; }
    public boolean isOverscrollEnabled() { return overscrollEnabled; }
    public boolean isClipToBounds() { return clipToBounds; }
    public float getAlpha() { return alpha; }
    @Override public boolean isVisible() { return visible; }
    @Override public boolean isEnabled() { return enabled; }

    public ComponentList<T> setAdapter(Adapter<T> value) {
        ensureActive();
        Objects.requireNonNull(value, "Adapter cannot be null.");
        if (adapter == value) return this;
        clearHolders();
        adapter.owner = null;
        adapter = value;
        adapter.owner = this;
        scrollOffset = 0f;
        rebuildLayout();
        invalidate();
        return this;
    }

    public ComponentList<T> setRegion(Position position, Size size) {
        ensureActive();
        RectF resolved = Objects.requireNonNull(position, "Position cannot be null.")
                .toRectF(hostView, Objects.requireNonNull(size, "Size cannot be null."));
        baseBounds.set(resolved);
        dimensionScale = position.getScale(hostView);
        applyParentAlignment();
        rebuildLayout();
        return this;
    }

    public ComponentList<T> setRegion(RectF value) {
        ensureActive();
        requireBounds(value);
        baseBounds.set(value);
        dimensionScale = 1f;
        applyParentAlignment();
        rebuildLayout();
        return this;
    }

    public ComponentList<T> horizontalCenter(boolean value) {
        horizontalCentered = value;
        applyParentAlignment();
        rebuildLayout();
        return this;
    }

    public ComponentList<T> verticalCenter(boolean value) {
        verticalCentered = value;
        applyParentAlignment();
        rebuildLayout();
        return this;
    }

    public ComponentList<T> setAlpha(float value) {
        alpha = requireAlpha(value);
        invalidate();
        return this;
    }

    public ComponentList<T> setVisible(boolean value) {
        visible = value;
        if (!value) cancelGesture(null);
        invalidate();
        return this;
    }

    public ComponentList<T> setEnabled(boolean value) {
        enabled = value;
        if (!value) cancelGesture(null);
        return this;
    }

    public ComponentList<T> setScrollEnabled(boolean value) {
        scrollEnabled = value;
        if (!value) stopScroll();
        return this;
    }

    public ComponentList<T> setFlingEnabled(boolean value) {
        flingEnabled = value;
        if (!value) stopScroll();
        return this;
    }

    public ComponentList<T> setOverscrollEnabled(boolean value) {
        overscrollEnabled = value;
        return this;
    }

    public ComponentList<T> setClipToBounds(boolean value) {
        clipToBounds = value;
        invalidate();
        return this;
    }

    public ComponentList<T> setItemSpacing(float value) {
        itemSpacing = requireNonNegative(value, "Item spacing");
        itemSpacingInPixels = false;
        rebuildLayout();
        return this;
    }

    public ComponentList<T> setItemSpacingPx(float value) {
        itemSpacing = requireNonNegative(value, "Item spacing");
        itemSpacingInPixels = true;
        rebuildLayout();
        return this;
    }

    public ComponentList<T> setPadding(float all) {
        return setPadding(all, all, all, all);
    }

    public ComponentList<T> setPadding(float left, float top, float right, float bottom) {
        setPaddingValues(left, top, right, bottom);
        paddingInPixels = false;
        rebuildLayout();
        return this;
    }

    public ComponentList<T> setPaddingPx(float all) {
        return setPaddingPx(all, all, all, all);
    }

    public ComponentList<T> setPaddingPx(float left, float top, float right, float bottom) {
        setPaddingValues(left, top, right, bottom);
        paddingInPixels = true;
        rebuildLayout();
        return this;
    }

    public ComponentList<T> setOnItemClickListener(OnItemClickListener<T> listener) {
        itemClickListener = listener;
        return this;
    }

    public ComponentList<T> setOnItemLongClickListener(OnItemLongClickListener<T> listener) {
        itemLongClickListener = listener;
        return this;
    }

    @Override
    public void draw(Canvas canvas) {
        ensureActive();
        if (!visible || bounds.isEmpty()) return;
        updateFling();
        ensureVisibleItems();
        int save = canvas.save();
        if (clipToBounds) canvas.clipRect(bounds);
        if (alpha < 1f) {
            canvas.saveLayerAlpha(bounds, Math.round(alpha * 255f));
        }
        for (Item item : visibleItems.values()) item.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ensureActive();
        if (!visible || !enabled) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!bounds.contains(event.getX(), event.getY())) return false;
            stopScroll();
            obtainVelocityTracker();
            velocityTracker.addMovement(event);
            dragging = false;
            downPrimary = lastPrimary = primary(event);
            ensureVisibleItems();
            touchItem = findItemAt(event.getX(), event.getY());
            childHandledDown = touchItem != null && touchItem.dispatchDown(event) != null;
            longClickTriggered = false;
            if (touchItem != null && !childHandledDown && itemLongClickListener != null) {
                hostView.postDelayed(longClickRunnable,
                        ViewConfiguration.getLongPressTimeout());
            }
            return true;
        }
        if (velocityTracker != null) velocityTracker.addMovement(event);
        if (action == MotionEvent.ACTION_MOVE) {
            float current = primary(event);
            if (scrollEnabled && !dragging && Math.abs(current - downPrimary) > touchSlop) {
                dragging = true;
                hostView.removeCallbacks(longClickRunnable);
                if (touchItem != null) touchItem.cancelTouch(event);
            }
            if (dragging) {
                setDraggedScrollOffset(scrollOffset + lastPrimary - current);
                lastPrimary = current;
                return true;
            }
            if (touchItem != null && childHandledDown) {
                touchItem.dispatchToTarget(event);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (dragging) {
                if (!settleOverscroll()) startFling();
            } else if (touchItem != null) {
                boolean childHandled = childHandledDown && touchItem.dispatchToTarget(event);
                if (!childHandled && !longClickTriggered
                        && touchItem.contains(event.getX(), event.getY())
                        && itemClickListener != null && touchItem.position >= 0
                        && touchItem.position < adapter.getItemCount()) {
                    itemClickListener.onItemClick(this,
                            adapter.getItem(touchItem.position), touchItem.position);
                }
            }
            finishGesture();
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            cancelGesture(event);
            return true;
        }
        return touchItem != null || dragging;
    }

    public void scrollToPosition(int position) {
        requireAdapterPosition(position);
        stopScroll();
        setScrollOffsetInternal(itemStarts.get(position) - leadingPadding());
    }

    public void smoothScrollToPosition(int position) {
        requireAdapterPosition(position);
        smoothScrollBy(itemStarts.get(position) - leadingPadding() - scrollOffset);
    }

    public void scrollBy(float dx, float dy) {
        setScrollOffsetInternal(scrollOffset + (orientation == Orientation.VERTICAL ? dy : dx));
    }

    public void smoothScrollBy(float dx, float dy) {
        smoothScrollBy(orientation == Orientation.VERTICAL ? dy : dx);
    }

    private void smoothScrollBy(float distance) {
        int start = Math.round(scrollOffset);
        int delta = Math.round(clampOffset(scrollOffset + distance) - scrollOffset);
        if (orientation == Orientation.VERTICAL) scroller.startScroll(0, start, 0, delta);
        else scroller.startScroll(start, 0, delta, 0);
        invalidateOnAnimation();
    }

    public void stopScroll() {
        if (!scroller.isFinished()) scroller.abortAnimation();
    }

    public boolean canScrollBackward() { return scrollOffset > 0f; }
    public boolean canScrollForward() { return scrollOffset < maxScrollOffset(); }

    public int getFirstVisiblePosition() {
        ensureVisibleItems();
        return visibleItems.isEmpty() ? -1 : visibleItems.keySet().iterator().next();
    }

    public int getLastVisiblePosition() {
        ensureVisibleItems();
        int result = -1;
        for (int position : visibleItems.keySet()) result = position;
        return result;
    }

    public Item findVisibleItem(long itemId) {
        ensureVisibleItems();
        for (Item item : visibleItems.values()) if (item.itemId == itemId) return item;
        return null;
    }

    public Component findVisibleComponent(long itemId, String localId) {
        Item item = findVisibleItem(itemId);
        return item == null ? null : item.find(localId);
    }

    public <C extends Component> C findVisibleComponent(
            long itemId, String localId, Class<C> type) {
        Component component = findVisibleComponent(itemId, localId);
        return type.isInstance(component) ? type.cast(component) : null;
    }

    public void notifyDataSetChanged() {
        ensureActive();
        recycleAllVisible();
        rebuildLayout();
        invalidate();
    }

    public void notifyItemChanged(int position) {
        requireAdapterPosition(position);
        Item item = visibleItems.get(position);
        if (item != null) bind(item, position);
        rebuildLayout();
        invalidate();
    }

    public void notifyItemInserted(int position) { notifyStructureChanged(position, true); }
    public void notifyItemRemoved(int position) { notifyStructureChanged(position, false); }
    public void notifyItemMoved(int fromPosition, int toPosition) {
        requireAdapterPosition(fromPosition);
        requireAdapterPosition(toPosition);
        notifyDataSetChanged();
    }

    public void notifyItemRangeChanged(int start, int count) {
        if (start < 0 || count < 0 || start + count > adapter.getItemCount()) {
            throw new IndexOutOfBoundsException("Invalid changed range.");
        }
        for (int position = start; position < start + count; position++) {
            Item item = visibleItems.get(position);
            if (item != null) bind(item, position);
        }
        rebuildLayout();
        invalidate();
    }

    @Override
    public void attach(ComponentHost owner) {
        ensureActive();
        if (!(owner instanceof NestedComponentHost)) {
            throw new IllegalArgumentException(
                    "ComponentList requires a NestedComponentHost such as ZLayerGroup.");
        }
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException("ComponentList already belongs to another host.");
        }
        this.owner = owner;
        rootHost = (NestedComponentHost) owner;
        applyParentAlignment();
        for (Item item : allHolders()) {
            for (ZLayer layer : item.layers) {
                for (Component component : layer.getComponents()) {
                    rootHost.registerNestedComponent(component);
                    component.attach(item.componentHost);
                }
            }
        }
        rebuildLayout();
    }

    @Override
    public void release() {
        if (released) return;
        cancelGesture(null);
        stopScroll();
        clearHolders();
        if (adapter != null) adapter.owner = null;
        owner = null;
        rootHost = null;
        released = true;
    }

    private void resolveRegion(Position position, Size size, RectF explicitBounds) {
        if (explicitBounds != null) {
            requireBounds(explicitBounds);
            baseBounds.set(explicitBounds);
            dimensionScale = 1f;
        } else {
            Objects.requireNonNull(position, "Position cannot be null.");
            baseBounds.set(position.toRectF(hostView,
                    Objects.requireNonNull(size, "Size cannot be null.")));
            dimensionScale = position.getScale(hostView);
        }
        applyParentAlignment();
    }

    private void applyParentAlignment() {
        bounds.set(baseBounds);
        if (!horizontalCentered && !verticalCentered) return;
        RectF parent = owner == null
                ? new RectF(0f, 0f, hostView.getWidth(), hostView.getHeight())
                : owner.getComponentBounds();
        if (horizontalCentered) bounds.offsetTo(parent.centerX() - bounds.width() / 2f, bounds.top);
        if (verticalCentered) bounds.offsetTo(bounds.left, parent.centerY() - bounds.height() / 2f);
    }

    private void rebuildLayout() {
        itemStarts.clear();
        itemSizes.clear();
        float cursor = leadingPadding();
        int count = adapter.getItemCount();
        float spacing = resolvedSpacing();
        for (int position = 0; position < count; position++) {
            float figmaSize = itemSizeProvider == null
                    ? itemSize
                    : requirePositive(itemSizeProvider.getItemSize(
                            adapter.getItem(position), position), "Item size");
            float resolvedSize = figmaSize * dimensionScale;
            itemStarts.add(cursor);
            itemSizes.add(resolvedSize);
            cursor += resolvedSize;
            if (position + 1 < count) cursor += spacing;
        }
        contentLength = cursor + trailingPadding();
        scrollOffset = clampOffset(scrollOffset);
        layoutVisibleItems();
        invalidate();
    }

    private void ensureVisibleItems() {
        if (released || adapter.getItemCount() == 0) {
            recycleAllVisible();
            return;
        }
        float viewportStart = scrollOffset;
        float viewportEnd = scrollOffset + viewportLength();
        ArrayList<Integer> needed = new ArrayList<>();
        for (int position = 0; position < itemStarts.size(); position++) {
            float start = itemStarts.get(position);
            float end = start + itemSizes.get(position);
            if (end >= viewportStart && start <= viewportEnd) needed.add(position);
        }
        for (int position : new ArrayList<>(visibleItems.keySet())) {
            Item item = visibleItems.get(position);
            if (!needed.contains(position) && item != touchItem) recycle(position);
        }
        for (int position : needed) {
            if (!visibleItems.containsKey(position)) obtain(position);
        }
        ArrayList<Map.Entry<Integer, Item>> ordered = new ArrayList<>(visibleItems.entrySet());
        ordered.sort(Comparator.comparingInt(Map.Entry::getKey));
        visibleItems.clear();
        for (Map.Entry<Integer, Item> entry : ordered) visibleItems.put(entry.getKey(), entry.getValue());
        layoutVisibleItems();
    }

    private void layoutVisibleItems() {
        float leftPadding = resolvedPadding(0);
        float topPadding = resolvedPadding(1);
        float rightPadding = resolvedPadding(2);
        float bottomPadding = resolvedPadding(3);
        for (Map.Entry<Integer, Item> entry : visibleItems.entrySet()) {
            int position = entry.getKey();
            if (position >= itemStarts.size()) continue;
            layoutItem(entry.getValue(), position, leftPadding, topPadding,
                    rightPadding, bottomPadding);
        }
    }

    private void layoutItem(Item item, int position) {
        layoutItem(item, position, resolvedPadding(0), resolvedPadding(1),
                resolvedPadding(2), resolvedPadding(3));
    }

    private void layoutItem(Item item, int position, float leftPadding, float topPadding,
                            float rightPadding, float bottomPadding) {
        float primaryStart = itemStarts.get(position) - scrollOffset;
        if (orientation == Orientation.VERTICAL) {
            item.originX = bounds.left + leftPadding;
            item.originY = bounds.top + primaryStart;
            item.width = itemCrossSize > 0f
                    ? itemCrossSize * dimensionScale
                    : Math.max(0f, bounds.width() - leftPadding - rightPadding);
            item.height = itemSizes.get(position);
        } else {
            item.originX = bounds.left + primaryStart;
            item.originY = bounds.top + topPadding;
            item.width = itemSizes.get(position);
            item.height = itemCrossSize > 0f
                    ? itemCrossSize * dimensionScale
                    : Math.max(0f, bounds.height() - topPadding - bottomPadding);
        }
    }

    private void obtain(int position) {
        int type = adapter.getItemViewType(position);
        ArrayDeque<Item> pool = recycledItems.get(type);
        Item item = pool == null ? null : pool.pollFirst();
        if (item == null) {
            item = new Item(this, nextHolderNumber++, type);
            layoutItem(item, position);
            bindingItem = item;
            try {
                adapter.onCreateItem(item, type);
            } finally {
                bindingItem = null;
            }
        } else {
            layoutItem(item, position);
        }
        bind(item, position);
        visibleItems.put(position, item);
    }

    private void bind(Item item, int position) {
        item.position = position;
        item.itemId = adapter.getItemId(position);
        bindingItem = item;
        try {
            adapter.onBindItem(item, adapter.getItem(position), position);
        } finally {
            bindingItem = null;
        }
    }

    private void recycle(int position) {
        Item item = visibleItems.remove(position);
        if (item == null) return;
        for (Component component : item.componentsByLocalId.values()) {
            if (component instanceof TextField && ((TextField) component).isFocused()
                    && rootHost != null) {
                rootHost.clearFocus((TextField) component);
            }
        }
        adapter.onItemRecycled(item);
        item.position = -1;
        recycledItems.computeIfAbsent(item.viewType, ignored -> new ArrayDeque<>()).addLast(item);
    }

    private void recycleAllVisible() {
        for (int position : new ArrayList<>(visibleItems.keySet())) recycle(position);
    }

    private void clearHolders() {
        ArrayList<Item> holders = allHolders();
        visibleItems.clear();
        recycledItems.clear();
        for (Item item : holders) item.release();
    }

    private ArrayList<Item> allHolders() {
        ArrayList<Item> result = new ArrayList<>(visibleItems.values());
        for (ArrayDeque<Item> pool : recycledItems.values()) result.addAll(pool);
        return result;
    }

    private void registerItemComponent(Component component) {
        if (bindingItem == null) {
            throw new IllegalStateException("Item components may only be added in onCreateItem().");
        }
        String prefix = id + "/holder_" + bindingItem.holderNumber + "/";
        if (!component.getId().startsWith(prefix)) {
            throw new IllegalArgumentException(
                    "Item component IDs must be created with ItemScope.id().");
        }
        String localId = component.getId().substring(prefix.length());
        if (bindingItem.componentsByLocalId.containsKey(localId)) {
            throw new IllegalArgumentException("Duplicate item component ID: " + localId);
        }
        if (rootHost != null) rootHost.registerNestedComponent(component);
        try {
            component.attach(bindingItem.componentHost);
            bindingItem.componentsByLocalId.put(localId, component);
        } catch (RuntimeException exception) {
            if (rootHost != null) rootHost.unregisterNestedComponent(component);
            throw exception;
        }
    }

    private void unregisterItemComponent(Component component) {
        if (rootHost != null) rootHost.unregisterNestedComponent(component);
        for (Item item : allHolders()) {
            item.componentsByLocalId.values().remove(component);
        }
    }

    private Item findItemAt(float x, float y) {
        ArrayList<Item> items = new ArrayList<>(visibleItems.values());
        for (int index = items.size() - 1; index >= 0; index--) {
            if (items.get(index).contains(x, y)) return items.get(index);
        }
        return null;
    }

    private void startFling() {
        if (!flingEnabled || velocityTracker == null) return;
        velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
        float velocity = orientation == Orientation.VERTICAL
                ? velocityTracker.getYVelocity()
                : velocityTracker.getXVelocity();
        if (Math.abs(velocity) < minimumFlingVelocity) return;
        int start = Math.round(scrollOffset);
        int max = Math.round(maxScrollOffset());
        if (orientation == Orientation.VERTICAL) {
            scroller.fling(0, start, 0, Math.round(-velocity), 0, 0, 0, max);
        } else {
            scroller.fling(start, 0, Math.round(-velocity), 0, 0, max, 0, 0);
        }
        invalidateOnAnimation();
    }

    private void updateFling() {
        if (!scroller.computeScrollOffset()) return;
        scrollOffset = orientation == Orientation.VERTICAL
                ? scroller.getCurrY()
                : scroller.getCurrX();
        layoutVisibleItems();
        invalidateOnAnimation();
    }

    private void setScrollOffsetInternal(float value) {
        scrollOffset = clampOffset(value);
        ensureVisibleItems();
        invalidate();
    }

    private void setDraggedScrollOffset(float value) {
        if (!overscrollEnabled) {
            setScrollOffsetInternal(value);
            return;
        }
        float max = maxScrollOffset();
        float limit = viewportLength() * 0.15f;
        if (value < 0f) value *= 0.35f;
        else if (value > max) value = max + (value - max) * 0.35f;
        scrollOffset = Math.max(-limit, Math.min(value, max + limit));
        ensureVisibleItems();
        invalidate();
    }

    private boolean settleOverscroll() {
        float destination;
        if (scrollOffset < 0f) destination = 0f;
        else if (scrollOffset > maxScrollOffset()) destination = maxScrollOffset();
        else return false;
        int start = Math.round(scrollOffset);
        int delta = Math.round(destination - scrollOffset);
        if (orientation == Orientation.VERTICAL) scroller.startScroll(0, start, 0, delta, 180);
        else scroller.startScroll(start, 0, delta, 0, 180);
        invalidateOnAnimation();
        return true;
    }

    private float clampOffset(float value) {
        return Math.max(0f, Math.min(value, maxScrollOffset()));
    }

    private float maxScrollOffset() {
        return Math.max(0f, contentLength - viewportLength());
    }

    private float viewportLength() {
        return orientation == Orientation.VERTICAL ? bounds.height() : bounds.width();
    }

    private float leadingPadding() {
        return orientation == Orientation.VERTICAL ? resolvedPadding(1) : resolvedPadding(0);
    }

    private float trailingPadding() {
        return orientation == Orientation.VERTICAL ? resolvedPadding(3) : resolvedPadding(2);
    }

    private float resolvedPadding(int index) {
        return padding[index] * (paddingInPixels ? 1f : dimensionScale);
    }

    private float resolvedSpacing() {
        return itemSpacing * (itemSpacingInPixels ? 1f : dimensionScale);
    }

    private float primary(MotionEvent event) {
        return orientation == Orientation.VERTICAL ? event.getY() : event.getX();
    }

    private void obtainVelocityTracker() {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        else velocityTracker.clear();
    }

    private void finishGesture() {
        hostView.removeCallbacks(longClickRunnable);
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
        touchItem = null;
        childHandledDown = false;
        dragging = false;
        longClickTriggered = false;
    }

    private void cancelGesture(MotionEvent event) {
        if (touchItem != null && event != null) touchItem.cancelTouch(event);
        finishGesture();
    }

    private void notifyStructureChanged(int position, boolean inserted) {
        int count = adapter.getItemCount();
        boolean invalid = inserted
                ? position < 0 || position >= count
                : position < 0 || position > count;
        if (invalid) {
            throw new IndexOutOfBoundsException("Invalid changed position: " + position);
        }
        recycleAllVisible();
        rebuildLayout();
        invalidate();
    }

    private void requireAdapterPosition(int position) {
        if (position < 0 || position >= adapter.getItemCount()) {
            throw new IndexOutOfBoundsException("Adapter position: " + position);
        }
    }

    private void setPaddingValues(float left, float top, float right, float bottom) {
        padding[0] = requireNonNegative(left, "Left padding");
        padding[1] = requireNonNegative(top, "Top padding");
        padding[2] = requireNonNegative(right, "Right padding");
        padding[3] = requireNonNegative(bottom, "Bottom padding");
    }

    private void invalidate() { if (owner != null) owner.invalidateComponent(); }
    private void invalidateOnAnimation() {
        if (owner != null) owner.postInvalidateComponentOnAnimation();
        else hostView.postInvalidateOnAnimation();
    }

    private void ensureActive() {
        if (released) throw new IllegalStateException("ComponentList has been released.");
    }

    private static String requireId(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be null or blank.");
        }
        return value.trim();
    }

    private static void requireBounds(RectF value) {
        Objects.requireNonNull(value, "Bounds cannot be null.");
        if (!Float.isFinite(value.left) || !Float.isFinite(value.top)
                || !Float.isFinite(value.right) || !Float.isFinite(value.bottom)
                || value.width() <= 0f || value.height() <= 0f) {
            throw new IllegalArgumentException("Bounds must be finite and non-empty.");
        }
    }

    private static float requirePositive(float value, String label) {
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(label + " must be positive and finite.");
        }
        return value;
    }

    private static float requireNonNegative(float value, String label) {
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(label + " must be non-negative and finite.");
        }
        return value;
    }

    private static float requireAlpha(float value) {
        if (!Float.isFinite(value) || value < 0f || value > 1f) {
            throw new IllegalArgumentException("Alpha must be in the 0..1 range.");
        }
        return value;
    }

    /** Builder supporting either Figma Position + Size or runtime RectF bounds. */
    public static final class Builder<T> implements ComponentFactory<ComponentList<T>> {
        private final String id;
        private final Position position;
        private final Size size;
        private final RectF explicitBounds;
        private Adapter<T> adapter;
        private Orientation orientation = Orientation.VERTICAL;
        private ItemSizeProvider<T> itemSizeProvider;
        private float itemSize;
        private float itemCrossSize;
        private float itemSpacing;
        private boolean itemSpacingInPixels;
        private final float[] padding = new float[4];
        private boolean paddingInPixels;
        private boolean scrollEnabled = true;
        private boolean flingEnabled = true;
        private boolean overscrollEnabled;
        private boolean clipToBounds = true;
        private boolean visible = true;
        private boolean enabled = true;
        private boolean horizontalCentered;
        private boolean verticalCentered;
        private float alpha = 1f;
        private int initialPosition;
        private OnItemClickListener<T> itemClickListener;
        private OnItemLongClickListener<T> itemLongClickListener;

        public Builder(Context context, String id, Position position, Size size) {
            Objects.requireNonNull(context, "Context cannot be null.");
            this.id = id;
            this.position = Objects.requireNonNull(position, "Position cannot be null.");
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
            explicitBounds = null;
        }

        public Builder(Context context, String id, RectF bounds) {
            Objects.requireNonNull(context, "Context cannot be null.");
            this.id = id;
            explicitBounds = new RectF(Objects.requireNonNull(bounds, "Bounds cannot be null."));
            position = null;
            size = null;
        }

        public Builder<T> setAdapter(Adapter<T> value) { adapter = value; return this; }
        public Builder<T> setOrientation(Orientation value) {
            orientation = Objects.requireNonNull(value, "Orientation cannot be null.");
            return this;
        }
        public Builder<T> setItemSize(float value) {
            itemSize = requirePositive(value, "Item size");
            itemSizeProvider = null;
            return this;
        }
        public Builder<T> setItemSizeProvider(ItemSizeProvider<T> value) {
            itemSizeProvider = Objects.requireNonNull(value, "Item size provider cannot be null.");
            return this;
        }
        public Builder<T> setItemCrossSize(float value) {
            itemCrossSize = requirePositive(value, "Item cross size"); return this;
        }
        public Builder<T> setItemSpacing(float value) {
            itemSpacing = requireNonNegative(value, "Item spacing");
            itemSpacingInPixels = false; return this;
        }
        public Builder<T> setItemSpacingPx(float value) {
            itemSpacing = requireNonNegative(value, "Item spacing");
            itemSpacingInPixels = true; return this;
        }
        public Builder<T> setPadding(float all) { return setPadding(all, all, all, all); }
        public Builder<T> setPadding(float left, float top, float right, float bottom) {
            setBuilderPadding(left, top, right, bottom); paddingInPixels = false; return this;
        }
        public Builder<T> setPaddingPx(float all) { return setPaddingPx(all, all, all, all); }
        public Builder<T> setPaddingPx(float left, float top, float right, float bottom) {
            setBuilderPadding(left, top, right, bottom); paddingInPixels = true; return this;
        }
        public Builder<T> setScrollEnabled(boolean value) { scrollEnabled = value; return this; }
        public Builder<T> setFlingEnabled(boolean value) { flingEnabled = value; return this; }
        public Builder<T> setOverscrollEnabled(boolean value) { overscrollEnabled = value; return this; }
        public Builder<T> setClipToBounds(boolean value) { clipToBounds = value; return this; }
        public Builder<T> setInitialPosition(int value) {
            if (value < 0) throw new IllegalArgumentException("Initial position cannot be negative.");
            initialPosition = value; return this;
        }
        public Builder<T> setAlpha(float value) { alpha = requireAlpha(value); return this; }
        public Builder<T> setVisible(boolean value) { visible = value; return this; }
        public Builder<T> setEnabled(boolean value) { enabled = value; return this; }
        public Builder<T> horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder<T> verticalCenter(boolean value) { verticalCentered = value; return this; }
        public Builder<T> setOnItemClickListener(OnItemClickListener<T> value) {
            itemClickListener = value; return this;
        }
        public Builder<T> setOnItemLongClickListener(OnItemLongClickListener<T> value) {
            itemLongClickListener = value; return this;
        }

        @Override
        public ComponentList<T> build(View hostView) {
            if (adapter == null) throw new IllegalStateException("ComponentList adapter is required.");
            if (itemSizeProvider == null && itemSize <= 0f) {
                throw new IllegalStateException("setItemSize() or setItemSizeProvider() is required.");
            }
            if (initialPosition >= adapter.getItemCount() && adapter.getItemCount() > 0) {
                throw new IndexOutOfBoundsException("Initial position: " + initialPosition);
            }
            return new ComponentList<>(this, hostView);
        }

        private void setBuilderPadding(float left, float top, float right, float bottom) {
            padding[0] = requireNonNegative(left, "Left padding");
            padding[1] = requireNonNegative(top, "Top padding");
            padding[2] = requireNonNegative(right, "Right padding");
            padding[3] = requireNonNegative(bottom, "Bottom padding");
        }
    }
}

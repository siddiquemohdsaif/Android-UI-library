package com.ogfa.nativeviews.list;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ComponentList<T> implements Component {
   private final View hostView;
   private final String id;
   private final RectF baseBounds = new RectF();
   private final RectF bounds = new RectF();
   private final OverScroller scroller;
   private final int touchSlop;
   private final int minimumFlingVelocity;
   private final int maximumFlingVelocity;
   private final LinkedHashMap<Integer, Item> visibleItems = new LinkedHashMap();
   private final Map<Integer, ArrayDeque<Item>> recycledItems = new HashMap();
   private final ArrayList<Float> itemStarts = new ArrayList();
   private final ArrayList<Float> itemSizes = new ArrayList();
   private final ZLayerOwner itemLayerOwner = new ZLayerOwner() {
      public View getHostView() {
         return ComponentList.this.hostView;
      }

      public void registerLayerComponent(Component var1) {
         ComponentList.this.registerItemComponent(var1);
      }

      public void unregisterLayerComponent(Component var1) {
         ComponentList.this.unregisterItemComponent(var1);
      }

      public void invalidateLayer() {
         ComponentList.this.invalidate();
      }
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
   private boolean scrollBarEnabled;
   private final Paint scrollBarPaint = new Paint(1);
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
      Item var1 = this.touchItem;
      if (var1 != null && !this.dragging && !this.childHandledDown && this.itemLongClickListener != null && var1.position >= 0 && var1.position < this.adapter.getItemCount()) {
         this.longClickTriggered = this.itemLongClickListener.onItemLongClick(this, this.adapter.getItem(var1.position), var1.position);
      }
   };
   private boolean released;

   private ComponentList(Builder<T> var1, View var2) {
      this.hostView = (View)Objects.requireNonNull(var2, "Host view cannot be null.");
      this.id = requireId(var1.id, "ComponentList ID");
      this.adapter = (Adapter)Objects.requireNonNull(var1.adapter, "Adapter cannot be null.");
      this.orientation = var1.orientation;
      this.itemSizeProvider = var1.itemSizeProvider;
      this.itemSize = var1.itemSize;
      this.itemCrossSize = var1.itemCrossSize;
      this.itemSpacing = var1.itemSpacing;
      this.itemSpacingInPixels = var1.itemSpacingInPixels;
      System.arraycopy(var1.padding, 0, this.padding, 0, this.padding.length);
      this.paddingInPixels = var1.paddingInPixels;
      this.scrollEnabled = var1.scrollEnabled;
      this.flingEnabled = var1.flingEnabled;
      this.overscrollEnabled = var1.overscrollEnabled;
      this.clipToBounds = var1.clipToBounds;
      this.scrollBarEnabled = var1.scrollBarEnabled;
      this.visible = var1.visible;
      this.enabled = var1.enabled;
      this.horizontalCentered = var1.horizontalCentered;
      this.verticalCentered = var1.verticalCentered;
      this.alpha = var1.alpha;
      this.initialPosition = var1.initialPosition;
      this.itemClickListener = var1.itemClickListener;
      this.itemLongClickListener = var1.itemLongClickListener;
      this.resolveRegion(var1.position, var1.size, var1.explicitBounds);
      ViewConfiguration var3 = ViewConfiguration.get(var2.getContext());
      this.touchSlop = var3.getScaledTouchSlop();
      this.minimumFlingVelocity = var3.getScaledMinimumFlingVelocity();
      this.maximumFlingVelocity = var3.getScaledMaximumFlingVelocity();
      this.scroller = new OverScroller(var2.getContext());
      this.adapter.owner = this;
      this.rebuildLayout();
      if (this.initialPosition > 0) {
         this.scrollToPosition(this.initialPosition);
      }

   }

   public String getId() {
      return this.id;
   }

   public RectF getBounds() {
      return new RectF(this.bounds);
   }

   public Orientation getOrientation() {
      return this.orientation;
   }

   public Adapter<T> getAdapter() {
      return this.adapter;
   }

   public float getScrollOffset() {
      return this.scrollOffset;
   }

   public boolean isScrollEnabled() {
      return this.scrollEnabled;
   }

   public boolean isFlingEnabled() {
      return this.flingEnabled;
   }

   public boolean isOverscrollEnabled() {
      return this.overscrollEnabled;
   }

   public boolean isClipToBounds() {
      return this.clipToBounds;
   }

   public float getAlpha() {
      return this.alpha;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public ComponentList<T> setAdapter(Adapter<T> var1) {
      this.ensureActive();
      Objects.requireNonNull(var1, "Adapter cannot be null.");
      if (this.adapter == var1) {
         return this;
      } else {
         this.clearHolders();
         this.adapter.owner = null;
         this.adapter = var1;
         this.adapter.owner = this;
         this.scrollOffset = 0.0F;
         this.rebuildLayout();
         this.invalidate();
         return this;
      }
   }

   public ComponentList<T> setRegion(Position var1, Size var2) {
      this.ensureActive();
      RectF var3 = ((Position)Objects.requireNonNull(var1, "Position cannot be null.")).toRectF(this.hostView, (Size)Objects.requireNonNull(var2, "Size cannot be null."));
      this.baseBounds.set(var3);
      this.dimensionScale = var1.getScale(this.hostView);
      this.applyParentAlignment();
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> setRegion(RectF var1) {
      this.ensureActive();
      requireBounds(var1);
      this.baseBounds.set(var1);
      this.dimensionScale = 1.0F;
      this.applyParentAlignment();
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> horizontalCenter(boolean var1) {
      this.horizontalCentered = var1;
      this.applyParentAlignment();
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> verticalCenter(boolean var1) {
      this.verticalCentered = var1;
      this.applyParentAlignment();
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> setAlpha(float var1) {
      this.alpha = requireAlpha(var1);
      this.invalidate();
      return this;
   }

   public ComponentList<T> setVisible(boolean var1) {
      this.visible = var1;
      if (!var1) {
         this.cancelGesture((MotionEvent)null);
      }

      this.invalidate();
      return this;
   }

   public ComponentList<T> setEnabled(boolean var1) {
      this.enabled = var1;
      if (!var1) {
         this.cancelGesture((MotionEvent)null);
      }

      return this;
   }

   public ComponentList<T> setScrollEnabled(boolean var1) {
      this.scrollEnabled = var1;
      if (!var1) {
         this.stopScroll();
      }

      return this;
   }

   public ComponentList<T> setFlingEnabled(boolean var1) {
      this.flingEnabled = var1;
      if (!var1) {
         this.stopScroll();
      }

      return this;
   }

   public ComponentList<T> setOverscrollEnabled(boolean var1) {
      this.overscrollEnabled = var1;
      return this;
   }

   public ComponentList<T> setClipToBounds(boolean var1) {
      this.clipToBounds = var1;
      this.invalidate();
      return this;
   }

   public ComponentList<T> setScrollBarEnabled(boolean var1) {
      this.scrollBarEnabled = var1;
      this.invalidate();
      return this;
   }

   public ComponentList<T> setItemSpacing(float var1) {
      this.itemSpacing = requireNonNegative(var1, "Item spacing");
      this.itemSpacingInPixels = false;
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> setItemSpacingPx(float var1) {
      this.itemSpacing = requireNonNegative(var1, "Item spacing");
      this.itemSpacingInPixels = true;
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> setPadding(float var1) {
      return this.setPadding(var1, var1, var1, var1);
   }

   public ComponentList<T> setPadding(float var1, float var2, float var3, float var4) {
      this.setPaddingValues(var1, var2, var3, var4);
      this.paddingInPixels = false;
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> setPaddingPx(float var1) {
      return this.setPaddingPx(var1, var1, var1, var1);
   }

   public ComponentList<T> setPaddingPx(float var1, float var2, float var3, float var4) {
      this.setPaddingValues(var1, var2, var3, var4);
      this.paddingInPixels = true;
      this.rebuildLayout();
      return this;
   }

   public ComponentList<T> setOnItemClickListener(OnItemClickListener<T> var1) {
      this.itemClickListener = var1;
      return this;
   }

   public ComponentList<T> setOnItemLongClickListener(OnItemLongClickListener<T> var1) {
      this.itemLongClickListener = var1;
      return this;
   }

   public void draw(Canvas var1) {
      this.ensureActive();
      if (this.visible && !this.bounds.isEmpty()) {
         this.updateFling();
         this.ensureVisibleItems();
         int var2 = var1.save();
         if (this.clipToBounds) {
            var1.clipRect(this.bounds);
         }

         if (this.alpha < 1.0F) {
            var1.saveLayerAlpha(this.bounds, Math.round(this.alpha * 255.0F));
         }

         Iterator var3 = this.visibleItems.values().iterator();

         while(var3.hasNext()) {
            Item var4 = (Item)var3.next();
            var4.draw(var1);
         }

         this.drawScrollBar(var1);
         var1.restoreToCount(var2);
      }
   }

   private void drawScrollBar(Canvas var1) {
      float var2 = this.maxScrollOffset();
      if (this.scrollBarEnabled && !(var2 <= 0.0F)) {
         float var3 = this.viewportLength();
         float var4 = var3 + var2;
         float var5 = Math.max(3.0F, this.dimensionScale * 3.0F);
         float var6 = Math.max(3.0F, this.dimensionScale * 5.0F);
         float var7 = Math.max(1.0F, var3 - var5 * 2.0F);
         float var8 = Math.max(Math.max(28.0F, this.dimensionScale * 36.0F), var7 * var3 / var4);
         var8 = Math.min(var7, var8);
         float var9 = Math.max(0.0F, Math.min(1.0F, this.scrollOffset / var2));
         this.scrollBarPaint.setColor(-1721996411);
         float var10;
         float var11;
         if (this.orientation == ComponentList.Orientation.VERTICAL) {
            var10 = this.bounds.right - var5 - var6;
            var11 = this.bounds.top + var5 + (var7 - var8) * var9;
            var1.drawRoundRect(var10, var11, var10 + var6, var11 + var8, var6 / 2.0F, var6 / 2.0F, this.scrollBarPaint);
         } else {
            var10 = this.bounds.left + var5 + (var7 - var8) * var9;
            var11 = this.bounds.bottom - var5 - var6;
            var1.drawRoundRect(var10, var11, var10 + var8, var11 + var6, var6 / 2.0F, var6 / 2.0F, this.scrollBarPaint);
         }

      }
   }

   public boolean onTouchEvent(MotionEvent var1) {
      this.ensureActive();
      if (this.visible && this.enabled) {
         int var2 = var1.getActionMasked();
         if (var2 == 0) {
            if (!this.bounds.contains(var1.getX(), var1.getY())) {
               return false;
            } else {
               this.stopScroll();
               this.obtainVelocityTracker();
               this.velocityTracker.addMovement(var1);
               this.dragging = false;
               this.downPrimary = this.lastPrimary = this.primary(var1);
               this.ensureVisibleItems();
               this.touchItem = this.findItemAt(var1.getX(), var1.getY());
               this.childHandledDown = this.touchItem != null && this.touchItem.dispatchDown(var1) != null;
               this.longClickTriggered = false;
               if (this.touchItem != null && !this.childHandledDown && this.itemLongClickListener != null) {
                  this.hostView.postDelayed(this.longClickRunnable, (long)ViewConfiguration.getLongPressTimeout());
               }

               return true;
            }
         } else {
            if (this.velocityTracker != null) {
               this.velocityTracker.addMovement(var1);
            }

            if (var2 == 2) {
               float var4 = this.primary(var1);
               if (this.scrollEnabled && !this.dragging && Math.abs(var4 - this.downPrimary) > (float)this.touchSlop) {
                  this.dragging = true;
                  this.hostView.removeCallbacks(this.longClickRunnable);
                  if (this.touchItem != null) {
                     this.touchItem.cancelTouch(var1);
                  }
               }

               if (this.dragging) {
                  this.setDraggedScrollOffset(this.scrollOffset + this.lastPrimary - var4);
                  this.lastPrimary = var4;
                  return true;
               } else {
                  if (this.touchItem != null && this.childHandledDown) {
                     this.touchItem.dispatchToTarget(var1);
                  }

                  return true;
               }
            } else if (var2 == 1) {
               if (this.dragging) {
                  if (!this.settleOverscroll()) {
                     this.startFling();
                  }
               } else if (this.touchItem != null) {
                  boolean var3 = this.childHandledDown && this.touchItem.dispatchToTarget(var1);
                  if (!var3 && !this.longClickTriggered && this.touchItem.contains(var1.getX(), var1.getY()) && this.itemClickListener != null && this.touchItem.position >= 0 && this.touchItem.position < this.adapter.getItemCount()) {
                     this.itemClickListener.onItemClick(this, this.adapter.getItem(this.touchItem.position), this.touchItem.position);
                  }
               }

               this.finishGesture();
               return true;
            } else if (var2 == 3) {
               this.cancelGesture(var1);
               return true;
            } else {
               return this.touchItem != null || this.dragging;
            }
         }
      } else {
         return false;
      }
   }

   public void scrollToPosition(int var1) {
      this.requireAdapterPosition(var1);
      this.stopScroll();
      this.setScrollOffsetInternal((Float)this.itemStarts.get(var1) - this.leadingPadding());
   }

   public void smoothScrollToPosition(int var1) {
      this.requireAdapterPosition(var1);
      this.smoothScrollBy((Float)this.itemStarts.get(var1) - this.leadingPadding() - this.scrollOffset);
   }

   public void scrollBy(float var1, float var2) {
      this.setScrollOffsetInternal(this.scrollOffset + (this.orientation == ComponentList.Orientation.VERTICAL ? var2 : var1));
   }

   public void smoothScrollBy(float var1, float var2) {
      this.smoothScrollBy(this.orientation == ComponentList.Orientation.VERTICAL ? var2 : var1);
   }

   private void smoothScrollBy(float var1) {
      int var2 = Math.round(this.scrollOffset);
      int var3 = Math.round(this.clampOffset(this.scrollOffset + var1) - this.scrollOffset);
      if (this.orientation == ComponentList.Orientation.VERTICAL) {
         this.scroller.startScroll(0, var2, 0, var3);
      } else {
         this.scroller.startScroll(var2, 0, var3, 0);
      }

      this.invalidateOnAnimation();
   }

   public void stopScroll() {
      if (!this.scroller.isFinished()) {
         this.scroller.abortAnimation();
      }

   }

   public boolean canScrollBackward() {
      return this.scrollOffset > 0.0F;
   }

   public boolean canScrollForward() {
      return this.scrollOffset < this.maxScrollOffset();
   }

   public int getFirstVisiblePosition() {
      this.ensureVisibleItems();
      return this.visibleItems.isEmpty() ? -1 : (Integer)this.visibleItems.keySet().iterator().next();
   }

   public int getLastVisiblePosition() {
      this.ensureVisibleItems();
      int var1 = -1;

      int var3;
      for(Iterator var2 = this.visibleItems.keySet().iterator(); var2.hasNext(); var1 = var3) {
         var3 = (Integer)var2.next();
      }

      return var1;
   }

   public Item findVisibleItem(long var1) {
      this.ensureVisibleItems();
      Iterator var3 = this.visibleItems.values().iterator();

      Item var4;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         var4 = (Item)var3.next();
      } while(var4.itemId != var1);

      return var4;
   }

   public Component findVisibleComponent(long var1, String var3) {
      Item var4 = this.findVisibleItem(var1);
      return var4 == null ? null : var4.find(var3);
   }

   public <C extends Component> C findVisibleComponent(long var1, String var3, Class<C> var4) {
      Component var5 = this.findVisibleComponent(var1, var3);
      return var4.isInstance(var5) ? var4.cast(var5) : null;
   }

   public void notifyDataSetChanged() {
      this.ensureActive();
      this.recycleAllVisible();
      this.rebuildLayout();
      this.invalidate();
   }

   public void notifyItemChanged(int var1) {
      this.requireAdapterPosition(var1);
      Item var2 = (Item)this.visibleItems.get(var1);
      if (var2 != null) {
         this.bind(var2, var1);
      }

      this.rebuildLayout();
      this.invalidate();
   }

   public void notifyItemInserted(int var1) {
      this.notifyStructureChanged(var1, true);
   }

   public void notifyItemRangeInserted(int var1, int var2) {
      this.ensureActive();
      int var3 = this.adapter.getItemCount();
      if (var1 >= 0 && var2 >= 0 && var1 + var2 <= var3) {
         if (var2 != 0) {
            LinkedHashMap var4 = new LinkedHashMap();

            int var7;
            Item var8;
            for(Iterator var5 = this.visibleItems.entrySet().iterator(); var5.hasNext(); var4.put(var7, var8)) {
               Map.Entry var6 = (Map.Entry)var5.next();
               var7 = (Integer)var6.getKey();
               var8 = (Item)var6.getValue();
               if (var7 >= var1) {
                  var7 += var2;
                  var8.position = var7;
               }
            }

            this.visibleItems.clear();
            this.visibleItems.putAll(var4);
            this.rebuildLayout();
            this.invalidate();
         }
      } else {
         throw new IndexOutOfBoundsException("Invalid inserted range.");
      }
   }

   public void notifyItemRemoved(int var1) {
      this.notifyStructureChanged(var1, false);
   }

   public void notifyItemMoved(int var1, int var2) {
      this.requireAdapterPosition(var1);
      this.requireAdapterPosition(var2);
      this.notifyDataSetChanged();
   }

   public void notifyItemRangeChanged(int var1, int var2) {
      if (var1 >= 0 && var2 >= 0 && var1 + var2 <= this.adapter.getItemCount()) {
         for(int var3 = var1; var3 < var1 + var2; ++var3) {
            Item var4 = (Item)this.visibleItems.get(var3);
            if (var4 != null) {
               this.bind(var4, var3);
            }
         }

         this.rebuildLayout();
         this.invalidate();
      } else {
         throw new IndexOutOfBoundsException("Invalid changed range.");
      }
   }

   public void attach(ComponentHost var1) {
      this.ensureActive();
      if (!(var1 instanceof NestedComponentHost)) {
         throw new IllegalArgumentException("ComponentList requires a NestedComponentHost such as ZLayerGroup.");
      } else if (this.owner != null && this.owner != var1) {
         throw new IllegalStateException("ComponentList already belongs to another host.");
      } else {
         this.owner = var1;
         this.rootHost = (NestedComponentHost)var1;
         this.applyParentAlignment();
         Iterator var2 = this.allHolders().iterator();

         while(var2.hasNext()) {
            Item var3 = (Item)var2.next();
            Iterator var4 = var3.layers.iterator();

            while(var4.hasNext()) {
               ZLayer var5 = (ZLayer)var4.next();
               Iterator var6 = var5.getComponents().iterator();

               while(var6.hasNext()) {
                  Component var7 = (Component)var6.next();
                  this.rootHost.registerNestedComponent(var7);
                  var7.attach(var3.componentHost);
               }
            }
         }

         this.rebuildLayout();
      }
   }

   public void release() {
      if (!this.released) {
         this.cancelGesture((MotionEvent)null);
         this.stopScroll();
         this.clearHolders();
         if (this.adapter != null) {
            this.adapter.owner = null;
         }

         this.owner = null;
         this.rootHost = null;
         this.released = true;
      }
   }

   private void resolveRegion(Position var1, Size var2, RectF var3) {
      if (var3 != null) {
         requireBounds(var3);
         this.baseBounds.set(var3);
         this.dimensionScale = 1.0F;
      } else {
         Objects.requireNonNull(var1, "Position cannot be null.");
         this.baseBounds.set(var1.toRectF(this.hostView, (Size)Objects.requireNonNull(var2, "Size cannot be null.")));
         this.dimensionScale = var1.getScale(this.hostView);
      }

      this.applyParentAlignment();
   }

   private void applyParentAlignment() {
      this.bounds.set(this.baseBounds);
      if (this.horizontalCentered || this.verticalCentered) {
         RectF var1 = this.owner == null ? new RectF(0.0F, 0.0F, (float)this.hostView.getWidth(), (float)this.hostView.getHeight()) : this.owner.getComponentBounds();
         if (this.horizontalCentered) {
            this.bounds.offsetTo(var1.centerX() - this.bounds.width() / 2.0F, this.bounds.top);
         }

         if (this.verticalCentered) {
            this.bounds.offsetTo(this.bounds.left, var1.centerY() - this.bounds.height() / 2.0F);
         }

      }
   }

   private void rebuildLayout() {
      this.itemStarts.clear();
      this.itemSizes.clear();
      float var1 = this.leadingPadding();
      int var2 = this.adapter.getItemCount();
      float var3 = this.resolvedSpacing();

      for(int var4 = 0; var4 < var2; ++var4) {
         float var5 = this.itemSizeProvider == null ? this.itemSize : requirePositive(this.itemSizeProvider.getItemSize(this.adapter.getItem(var4), var4), "Item size");
         float var6 = var5 * this.dimensionScale;
         this.itemStarts.add(var1);
         this.itemSizes.add(var6);
         var1 += var6;
         if (var4 + 1 < var2) {
            var1 += var3;
         }
      }

      this.contentLength = var1 + this.trailingPadding();
      this.scrollOffset = this.clampOffset(this.scrollOffset);
      this.layoutVisibleItems();
      this.invalidate();
   }

   private void ensureVisibleItems() {
      if (!this.released && this.adapter.getItemCount() != 0) {
         float var1 = this.scrollOffset;
         float var2 = this.scrollOffset + this.viewportLength();
         ArrayList var3 = new ArrayList();

         for(int var4 = 0; var4 < this.itemStarts.size(); ++var4) {
            float var5 = (Float)this.itemStarts.get(var4);
            float var6 = var5 + (Float)this.itemSizes.get(var4);
            if (var6 >= var1 && var5 <= var2) {
               var3.add(var4);
            }
         }

         Iterator var7 = (new ArrayList(this.visibleItems.keySet())).iterator();

         int var8;
         while(var7.hasNext()) {
            var8 = (Integer)var7.next();
            Item var11 = (Item)this.visibleItems.get(var8);
            if (!var3.contains(var8) && var11 != this.touchItem) {
               this.recycle(var8);
            }
         }

         var7 = var3.iterator();

         while(var7.hasNext()) {
            var8 = (Integer)var7.next();
            if (!this.visibleItems.containsKey(var8)) {
               this.obtain(var8);
            }
         }

         ArrayList<Map.Entry<Integer, Item>> var9 = new ArrayList<>(this.visibleItems.entrySet());
         var9.sort(Comparator.comparingInt(Map.Entry::getKey));
         this.visibleItems.clear();
         Iterator var10 = var9.iterator();

         while(var10.hasNext()) {
            Map.Entry var12 = (Map.Entry)var10.next();
            this.visibleItems.put((Integer)var12.getKey(), (Item)var12.getValue());
         }

         this.layoutVisibleItems();
      } else {
         this.recycleAllVisible();
      }
   }

   private void layoutVisibleItems() {
      float var1 = this.resolvedPadding(0);
      float var2 = this.resolvedPadding(1);
      float var3 = this.resolvedPadding(2);
      float var4 = this.resolvedPadding(3);
      Iterator var5 = this.visibleItems.entrySet().iterator();

      while(var5.hasNext()) {
         Map.Entry var6 = (Map.Entry)var5.next();
         int var7 = (Integer)var6.getKey();
         if (var7 < this.itemStarts.size()) {
            this.layoutItem((Item)var6.getValue(), var7, var1, var2, var3, var4);
         }
      }

   }

   private void layoutItem(Item var1, int var2) {
      this.layoutItem(var1, var2, this.resolvedPadding(0), this.resolvedPadding(1), this.resolvedPadding(2), this.resolvedPadding(3));
   }

   private void layoutItem(Item var1, int var2, float var3, float var4, float var5, float var6) {
      float var7 = (Float)this.itemStarts.get(var2) - this.scrollOffset;
      if (this.orientation == ComponentList.Orientation.VERTICAL) {
         var1.originX = this.bounds.left + var3;
         var1.originY = this.bounds.top + var7;
         var1.width = this.itemCrossSize > 0.0F ? this.itemCrossSize * this.dimensionScale : Math.max(0.0F, this.bounds.width() - var3 - var5);
         var1.height = (Float)this.itemSizes.get(var2);
      } else {
         var1.originX = this.bounds.left + var7;
         var1.originY = this.bounds.top + var4;
         var1.width = (Float)this.itemSizes.get(var2);
         var1.height = this.itemCrossSize > 0.0F ? this.itemCrossSize * this.dimensionScale : Math.max(0.0F, this.bounds.height() - var4 - var6);
      }

   }

   private void obtain(int var1) {
      int var2 = this.adapter.getItemViewType(var1);
      ArrayDeque var3 = (ArrayDeque)this.recycledItems.get(var2);
      Item var4 = var3 == null ? null : (Item)var3.pollFirst();
      if (var4 == null) {
         var4 = new Item(this, this.nextHolderNumber++, var2);
         this.layoutItem(var4, var1);
         this.bindingItem = var4;

         try {
            this.adapter.onCreateItem(var4, var2);
         } finally {
            this.bindingItem = null;
         }
      } else {
         this.layoutItem(var4, var1);
      }

      this.bind(var4, var1);
      this.visibleItems.put(var1, var4);
   }

   private void bind(Item var1, int var2) {
      var1.position = var2;
      var1.itemId = this.adapter.getItemId(var2);
      this.bindingItem = var1;

      try {
         this.adapter.onBindItem(var1, this.adapter.getItem(var2), var2);
      } finally {
         this.bindingItem = null;
      }

   }

   private void recycle(int var1) {
      Item var2 = (Item)this.visibleItems.remove(var1);
      if (var2 != null) {
         Iterator var3 = var2.componentsByLocalId.values().iterator();

         while(var3.hasNext()) {
            Component var4 = (Component)var3.next();
            if (var4 instanceof TextField && ((TextField)var4).isFocused() && this.rootHost != null) {
               this.rootHost.clearFocus((TextField)var4);
            }
         }

         this.adapter.onItemRecycled(var2);
         var2.position = -1;
         ((ArrayDeque)this.recycledItems.computeIfAbsent(var2.viewType, (var0) -> {
            return new ArrayDeque();
         })).addLast(var2);
      }
   }

   private void recycleAllVisible() {
      Iterator var1 = (new ArrayList(this.visibleItems.keySet())).iterator();

      while(var1.hasNext()) {
         int var2 = (Integer)var1.next();
         this.recycle(var2);
      }

   }

   private void clearHolders() {
      ArrayList var1 = this.allHolders();
      this.visibleItems.clear();
      this.recycledItems.clear();
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         Item var3 = (Item)var2.next();
         var3.release();
      }

   }

   private ArrayList<Item> allHolders() {
      ArrayList var1 = new ArrayList(this.visibleItems.values());
      Iterator var2 = this.recycledItems.values().iterator();

      while(var2.hasNext()) {
         ArrayDeque var3 = (ArrayDeque)var2.next();
         var1.addAll(var3);
      }

      return var1;
   }

   private void registerItemComponent(Component var1) {
      if (this.bindingItem == null) {
         throw new IllegalStateException("Item components may only be added in onCreateItem().");
      } else {
         String var2 = this.id + "/holder_" + this.bindingItem.holderNumber + "/";
         if (!var1.getId().startsWith(var2)) {
            throw new IllegalArgumentException("Item component IDs must be created with ItemScope.id().");
         } else {
            String var3 = var1.getId().substring(var2.length());
            if (this.bindingItem.componentsByLocalId.containsKey(var3)) {
               throw new IllegalArgumentException("Duplicate item component ID: " + var3);
            } else {
               if (this.rootHost != null) {
                  this.rootHost.registerNestedComponent(var1);
               }

               try {
                  var1.attach(this.bindingItem.componentHost);
                  this.bindingItem.componentsByLocalId.put(var3, var1);
               } catch (RuntimeException var5) {
                  if (this.rootHost != null) {
                     this.rootHost.unregisterNestedComponent(var1);
                  }

                  throw var5;
               }
            }
         }
      }
   }

   private void unregisterItemComponent(Component var1) {
      if (this.rootHost != null) {
         this.rootHost.unregisterNestedComponent(var1);
      }

      Iterator var2 = this.allHolders().iterator();

      while(var2.hasNext()) {
         Item var3 = (Item)var2.next();
         var3.componentsByLocalId.values().remove(var1);
      }

   }

   private Item findItemAt(float var1, float var2) {
      ArrayList var3 = new ArrayList(this.visibleItems.values());

      for(int var4 = var3.size() - 1; var4 >= 0; --var4) {
         if (((Item)var3.get(var4)).contains(var1, var2)) {
            return (Item)var3.get(var4);
         }
      }

      return null;
   }

   private void startFling() {
      if (this.flingEnabled && this.velocityTracker != null) {
         this.velocityTracker.computeCurrentVelocity(1000, (float)this.maximumFlingVelocity);
         float var1 = this.orientation == ComponentList.Orientation.VERTICAL ? this.velocityTracker.getYVelocity() : this.velocityTracker.getXVelocity();
         if (!(Math.abs(var1) < (float)this.minimumFlingVelocity)) {
            int var2 = Math.round(this.scrollOffset);
            int var3 = Math.round(this.maxScrollOffset());
            if (this.orientation == ComponentList.Orientation.VERTICAL) {
               this.scroller.fling(0, var2, 0, Math.round(-var1), 0, 0, 0, var3);
            } else {
               this.scroller.fling(var2, 0, Math.round(-var1), 0, 0, var3, 0, 0);
            }

            this.invalidateOnAnimation();
         }
      }
   }

   private void updateFling() {
      if (this.scroller.computeScrollOffset()) {
         this.scrollOffset = this.orientation == ComponentList.Orientation.VERTICAL ? (float)this.scroller.getCurrY() : (float)this.scroller.getCurrX();
         this.layoutVisibleItems();
         this.invalidateOnAnimation();
      }
   }

   private void setScrollOffsetInternal(float var1) {
      this.scrollOffset = this.clampOffset(var1);
      this.ensureVisibleItems();
      this.invalidate();
   }

   private void setDraggedScrollOffset(float var1) {
      if (!this.overscrollEnabled) {
         this.setScrollOffsetInternal(var1);
      } else {
         float var2 = this.maxScrollOffset();
         float var3 = this.viewportLength() * 0.15F;
         if (var1 < 0.0F) {
            var1 *= 0.35F;
         } else if (var1 > var2) {
            var1 = var2 + (var1 - var2) * 0.35F;
         }

         this.scrollOffset = Math.max(-var3, Math.min(var1, var2 + var3));
         this.ensureVisibleItems();
         this.invalidate();
      }
   }

   private boolean settleOverscroll() {
      float var1;
      if (this.scrollOffset < 0.0F) {
         var1 = 0.0F;
      } else {
         if (!(this.scrollOffset > this.maxScrollOffset())) {
            return false;
         }

         var1 = this.maxScrollOffset();
      }

      int var2 = Math.round(this.scrollOffset);
      int var3 = Math.round(var1 - this.scrollOffset);
      if (this.orientation == ComponentList.Orientation.VERTICAL) {
         this.scroller.startScroll(0, var2, 0, var3, 180);
      } else {
         this.scroller.startScroll(var2, 0, var3, 0, 180);
      }

      this.invalidateOnAnimation();
      return true;
   }

   private float clampOffset(float var1) {
      return Math.max(0.0F, Math.min(var1, this.maxScrollOffset()));
   }

   private float maxScrollOffset() {
      return Math.max(0.0F, this.contentLength - this.viewportLength());
   }

   private float viewportLength() {
      return this.orientation == ComponentList.Orientation.VERTICAL ? this.bounds.height() : this.bounds.width();
   }

   private float leadingPadding() {
      return this.orientation == ComponentList.Orientation.VERTICAL ? this.resolvedPadding(1) : this.resolvedPadding(0);
   }

   private float trailingPadding() {
      return this.orientation == ComponentList.Orientation.VERTICAL ? this.resolvedPadding(3) : this.resolvedPadding(2);
   }

   private float resolvedPadding(int var1) {
      return this.padding[var1] * (this.paddingInPixels ? 1.0F : this.dimensionScale);
   }

   private float resolvedSpacing() {
      return this.itemSpacing * (this.itemSpacingInPixels ? 1.0F : this.dimensionScale);
   }

   private float primary(MotionEvent var1) {
      return this.orientation == ComponentList.Orientation.VERTICAL ? var1.getY() : var1.getX();
   }

   private void obtainVelocityTracker() {
      if (this.velocityTracker == null) {
         this.velocityTracker = VelocityTracker.obtain();
      } else {
         this.velocityTracker.clear();
      }

   }

   private void finishGesture() {
      this.hostView.removeCallbacks(this.longClickRunnable);
      if (this.velocityTracker != null) {
         this.velocityTracker.recycle();
         this.velocityTracker = null;
      }

      this.touchItem = null;
      this.childHandledDown = false;
      this.dragging = false;
      this.longClickTriggered = false;
   }

   private void cancelGesture(MotionEvent var1) {
      if (this.touchItem != null && var1 != null) {
         this.touchItem.cancelTouch(var1);
      }

      this.finishGesture();
   }

   private void notifyStructureChanged(int var1, boolean var2) {
      int var3 = this.adapter.getItemCount();
      boolean var4 = var2 ? var1 < 0 || var1 >= var3 : var1 < 0 || var1 > var3;
      if (var4) {
         throw new IndexOutOfBoundsException("Invalid changed position: " + var1);
      } else {
         this.recycleAllVisible();
         this.rebuildLayout();
         this.invalidate();
      }
   }

   private void requireAdapterPosition(int var1) {
      if (var1 < 0 || var1 >= this.adapter.getItemCount()) {
         throw new IndexOutOfBoundsException("Adapter position: " + var1);
      }
   }

   private void setPaddingValues(float var1, float var2, float var3, float var4) {
      this.padding[0] = requireNonNegative(var1, "Left padding");
      this.padding[1] = requireNonNegative(var2, "Top padding");
      this.padding[2] = requireNonNegative(var3, "Right padding");
      this.padding[3] = requireNonNegative(var4, "Bottom padding");
   }

   private void invalidate() {
      if (this.owner != null) {
         this.owner.invalidateComponent();
      }

   }

   private void invalidateOnAnimation() {
      if (this.owner != null) {
         this.owner.postInvalidateComponentOnAnimation();
      } else {
         this.hostView.postInvalidateOnAnimation();
      }

   }

   private void ensureActive() {
      if (this.released) {
         throw new IllegalStateException("ComponentList has been released.");
      }
   }

   private static String requireId(String var0, String var1) {
      if (var0 != null && !var0.trim().isEmpty()) {
         return var0.trim();
      } else {
         throw new IllegalArgumentException(var1 + " cannot be null or blank.");
      }
   }

   private static void requireBounds(RectF var0) {
      Objects.requireNonNull(var0, "Bounds cannot be null.");
      if (!Float.isFinite(var0.left) || !Float.isFinite(var0.top) || !Float.isFinite(var0.right) || !Float.isFinite(var0.bottom) || var0.width() <= 0.0F || var0.height() <= 0.0F) {
         throw new IllegalArgumentException("Bounds must be finite and non-empty.");
      }
   }

   private static float requirePositive(float var0, String var1) {
      if (Float.isFinite(var0) && !(var0 <= 0.0F)) {
         return var0;
      } else {
         throw new IllegalArgumentException(var1 + " must be positive and finite.");
      }
   }

   private static float requireNonNegative(float var0, String var1) {
      if (Float.isFinite(var0) && !(var0 < 0.0F)) {
         return var0;
      } else {
         throw new IllegalArgumentException(var1 + " must be non-negative and finite.");
      }
   }

   private static float requireAlpha(float var0) {
      if (Float.isFinite(var0) && !(var0 < 0.0F) && !(var0 > 1.0F)) {
         return var0;
      } else {
         throw new IllegalArgumentException("Alpha must be in the 0..1 range.");
      }
   }

   public static final class Builder<T> implements ComponentFactory<ComponentList<T>> {
      private final String id;
      private final Position position;
      private final Size size;
      private final RectF explicitBounds;
      private Adapter<T> adapter;
      private Orientation orientation;
      private ItemSizeProvider<T> itemSizeProvider;
      private float itemSize;
      private float itemCrossSize;
      private float itemSpacing;
      private boolean itemSpacingInPixels;
      private final float[] padding;
      private boolean paddingInPixels;
      private boolean scrollEnabled;
      private boolean flingEnabled;
      private boolean overscrollEnabled;
      private boolean clipToBounds;
      private boolean scrollBarEnabled;
      private boolean visible;
      private boolean enabled;
      private boolean horizontalCentered;
      private boolean verticalCentered;
      private float alpha;
      private int initialPosition;
      private OnItemClickListener<T> itemClickListener;
      private OnItemLongClickListener<T> itemLongClickListener;

      public Builder(Context var1, String var2, Position var3, Size var4) {
         this.orientation = ComponentList.Orientation.VERTICAL;
         this.padding = new float[4];
         this.scrollEnabled = true;
         this.flingEnabled = true;
         this.clipToBounds = true;
         this.visible = true;
         this.enabled = true;
         this.alpha = 1.0F;
         Objects.requireNonNull(var1, "Context cannot be null.");
         this.id = var2;
         this.position = (Position)Objects.requireNonNull(var3, "Position cannot be null.");
         this.size = (Size)Objects.requireNonNull(var4, "Size cannot be null.");
         this.explicitBounds = null;
      }

      public Builder(Context var1, String var2, RectF var3) {
         this.orientation = ComponentList.Orientation.VERTICAL;
         this.padding = new float[4];
         this.scrollEnabled = true;
         this.flingEnabled = true;
         this.clipToBounds = true;
         this.visible = true;
         this.enabled = true;
         this.alpha = 1.0F;
         Objects.requireNonNull(var1, "Context cannot be null.");
         this.id = var2;
         this.explicitBounds = new RectF((RectF)Objects.requireNonNull(var3, "Bounds cannot be null."));
         this.position = null;
         this.size = null;
      }

      public Builder<T> setAdapter(Adapter<T> var1) {
         this.adapter = var1;
         return this;
      }

      public Builder<T> setOrientation(Orientation var1) {
         this.orientation = (Orientation)Objects.requireNonNull(var1, "Orientation cannot be null.");
         return this;
      }

      public Builder<T> setItemSize(float var1) {
         this.itemSize = ComponentList.requirePositive(var1, "Item size");
         this.itemSizeProvider = null;
         return this;
      }

      public Builder<T> setItemSizeProvider(ItemSizeProvider<T> var1) {
         this.itemSizeProvider = (ItemSizeProvider)Objects.requireNonNull(var1, "Item size provider cannot be null.");
         return this;
      }

      public Builder<T> setItemCrossSize(float var1) {
         this.itemCrossSize = ComponentList.requirePositive(var1, "Item cross size");
         return this;
      }

      public Builder<T> setItemSpacing(float var1) {
         this.itemSpacing = ComponentList.requireNonNegative(var1, "Item spacing");
         this.itemSpacingInPixels = false;
         return this;
      }

      public Builder<T> setItemSpacingPx(float var1) {
         this.itemSpacing = ComponentList.requireNonNegative(var1, "Item spacing");
         this.itemSpacingInPixels = true;
         return this;
      }

      public Builder<T> setPadding(float var1) {
         return this.setPadding(var1, var1, var1, var1);
      }

      public Builder<T> setPadding(float var1, float var2, float var3, float var4) {
         this.setBuilderPadding(var1, var2, var3, var4);
         this.paddingInPixels = false;
         return this;
      }

      public Builder<T> setPaddingPx(float var1) {
         return this.setPaddingPx(var1, var1, var1, var1);
      }

      public Builder<T> setPaddingPx(float var1, float var2, float var3, float var4) {
         this.setBuilderPadding(var1, var2, var3, var4);
         this.paddingInPixels = true;
         return this;
      }

      public Builder<T> setScrollEnabled(boolean var1) {
         this.scrollEnabled = var1;
         return this;
      }

      public Builder<T> setFlingEnabled(boolean var1) {
         this.flingEnabled = var1;
         return this;
      }

      public Builder<T> setOverscrollEnabled(boolean var1) {
         this.overscrollEnabled = var1;
         return this;
      }

      public Builder<T> setClipToBounds(boolean var1) {
         this.clipToBounds = var1;
         return this;
      }

      public Builder<T> setScrollBarEnabled(boolean var1) {
         this.scrollBarEnabled = var1;
         return this;
      }

      public Builder<T> setInitialPosition(int var1) {
         if (var1 < 0) {
            throw new IllegalArgumentException("Initial position cannot be negative.");
         } else {
            this.initialPosition = var1;
            return this;
         }
      }

      public Builder<T> setAlpha(float var1) {
         this.alpha = ComponentList.requireAlpha(var1);
         return this;
      }

      public Builder<T> setVisible(boolean var1) {
         this.visible = var1;
         return this;
      }

      public Builder<T> setEnabled(boolean var1) {
         this.enabled = var1;
         return this;
      }

      public Builder<T> horizontalCenter(boolean var1) {
         this.horizontalCentered = var1;
         return this;
      }

      public Builder<T> verticalCenter(boolean var1) {
         this.verticalCentered = var1;
         return this;
      }

      public Builder<T> setOnItemClickListener(OnItemClickListener<T> var1) {
         this.itemClickListener = var1;
         return this;
      }

      public Builder<T> setOnItemLongClickListener(OnItemLongClickListener<T> var1) {
         this.itemLongClickListener = var1;
         return this;
      }

      public ComponentList<T> build(View var1) {
         if (this.adapter == null) {
            throw new IllegalStateException("ComponentList adapter is required.");
         } else if (this.itemSizeProvider == null && this.itemSize <= 0.0F) {
            throw new IllegalStateException("setItemSize() or setItemSizeProvider() is required.");
         } else if (this.initialPosition >= this.adapter.getItemCount() && this.adapter.getItemCount() > 0) {
            throw new IndexOutOfBoundsException("Initial position: " + this.initialPosition);
         } else {
            return new ComponentList(this, var1);
         }
      }

      private void setBuilderPadding(float var1, float var2, float var3, float var4) {
         this.padding[0] = ComponentList.requireNonNegative(var1, "Left padding");
         this.padding[1] = ComponentList.requireNonNegative(var2, "Top padding");
         this.padding[2] = ComponentList.requireNonNegative(var3, "Right padding");
         this.padding[3] = ComponentList.requireNonNegative(var4, "Bottom padding");
      }

   }

   public abstract static class Adapter<T> {
      private ComponentList<T> owner;

      public abstract int getItemCount();

      public abstract T getItem(int var1);

      public long getItemId(int var1) {
         return (long)var1;
      }

      public int getItemViewType(int var1) {
         return 0;
      }

      public abstract void onCreateItem(Item var1, int var2);

      public abstract void onBindItem(Item var1, T var2, int var3);

      public void onItemRecycled(Item var1) {
      }

      public final void notifyDataSetChanged() {
         if (this.owner != null) {
            this.owner.notifyDataSetChanged();
         }

      }

      public final void notifyItemChanged(int var1) {
         if (this.owner != null) {
            this.owner.notifyItemChanged(var1);
         }

      }

      public final void notifyItemInserted(int var1) {
         if (this.owner != null) {
            this.owner.notifyItemInserted(var1);
         }

      }

      public final void notifyItemRangeInserted(int var1, int var2) {
         if (this.owner != null) {
            this.owner.notifyItemRangeInserted(var1, var2);
         }

      }

      public final void notifyItemRemoved(int var1) {
         if (this.owner != null) {
            this.owner.notifyItemRemoved(var1);
         }

      }

      public final void notifyItemMoved(int var1, int var2) {
         if (this.owner != null) {
            this.owner.notifyItemMoved(var1, var2);
         }

      }

      public final void notifyItemRangeChanged(int var1, int var2) {
         if (this.owner != null) {
            this.owner.notifyItemRangeChanged(var1, var2);
         }

      }
   }

   public static enum Orientation {
      VERTICAL,
      HORIZONTAL;

      // $FF: synthetic method
      private static Orientation[] $values() {
         return new Orientation[]{VERTICAL, HORIZONTAL};
      }
   }

   public interface ItemSizeProvider<T> {
      float getItemSize(T var1, int var2);
   }

   public interface OnItemClickListener<T> {
      void onItemClick(ComponentList<T> var1, T var2, int var3);
   }

   public interface OnItemLongClickListener<T> {
      boolean onItemLongClick(ComponentList<T> var1, T var2, int var3);
   }

   public static final class Item {
      private final ComponentList<?> list;
      private final int holderNumber;
      private final int viewType;
      private final ArrayList<ZLayer> layers = new ArrayList();
      private final Map<String, ZLayer> layersByLocalId = new LinkedHashMap();
      private final Map<String, Component> componentsByLocalId = new LinkedHashMap();
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
         public View getHostView() {
            return Item.this.list.hostView;
         }

         public RectF getComponentBounds() {
            return new RectF(0.0F, 0.0F, Item.this.width, Item.this.height);
         }

         public void invalidateComponent() {
            Item.this.list.invalidate();
         }

         public void postInvalidateComponentOnAnimation() {
            Item.this.list.invalidateOnAnimation();
         }

         public boolean requestFocus(TextField var1) {
            return Item.this.list.rootHost != null && Item.this.list.rootHost.requestFocus(var1);
         }

         public void clearFocus(TextField var1) {
            if (Item.this.list.rootHost != null) {
               Item.this.list.rootHost.clearFocus(var1);
            }

         }

         public void restartInput() {
            if (Item.this.list.rootHost != null) {
               Item.this.list.rootHost.restartInput();
            }

         }

         public void updateSelection(TextField var1) {
            if (Item.this.list.rootHost != null) {
               Item.this.list.rootHost.updateSelection(var1);
            }

         }

          @Override public void showSelectionActionMode(TextField field) {
             if (Item.this.list.rootHost != null) {
                Item.this.list.rootHost.showSelectionActionMode(field);
             }
          }

          @Override public void showInsertionActionMode(TextField field) {
             if (Item.this.list.rootHost != null) {
                Item.this.list.rootHost.showInsertionActionMode(field);
             }
          }

         public void registerNestedComponent(Component var1) {
            if (Item.this.list.rootHost != null) {
               Item.this.list.rootHost.registerNestedComponent(var1);
            }

         }

         public void unregisterNestedComponent(Component var1) {
            if (Item.this.list.rootHost != null) {
               Item.this.list.rootHost.unregisterNestedComponent(var1);
            }

         }
      };

      private Item(ComponentList<?> var1, int var2, int var3) {
         this.list = var1;
         this.holderNumber = var2;
         this.viewType = var3;
         this.scope = new ItemScope(this);
      }

      public int getPosition() {
         return this.position;
      }

      public long getItemId() {
         return this.itemId;
      }

      public int getViewType() {
         return this.viewType;
      }

      public ItemScope getScope() {
         return this.scope;
      }

      public ZLayer addLayer(String var1) {
         String var2 = ComponentList.requireId(var1, "Item layer ID");
         if (this.layersByLocalId.containsKey(var2)) {
            throw new IllegalArgumentException("Duplicate item layer ID: " + var2);
         } else {
            ZLayer var3 = new ZLayer(this.list.itemLayerOwner, this.list.id + "/holder_" + this.holderNumber + "/layer_" + var2);
            this.layers.add(var3);
            this.layersByLocalId.put(var2, var3);
            return var3;
         }
      }

      public ZLayer findLayer(String var1) {
         return (ZLayer)this.layersByLocalId.get(var1);
      }

      public Component find(String var1) {
         return (Component)this.componentsByLocalId.get(var1);
      }

      public <C extends Component> C find(String var1, Class<C> var2) {
         Component var3 = this.find(var1);
         return var2.isInstance(var3) ? var2.cast(var3) : null;
      }

      public List<ZLayer> getLayers() {
         return Collections.unmodifiableList(this.layers);
      }

      private void draw(Canvas var1) {
         int var2 = var1.save();
         var1.translate(this.originX, this.originY);
         Iterator var3 = this.layers.iterator();

         while(var3.hasNext()) {
            ZLayer var4 = (ZLayer)var3.next();
            var4.draw(var1);
         }

         var1.restoreToCount(var2);
      }

      private Component dispatchDown(MotionEvent var1) {
         MotionEvent var2 = this.translatedEvent(var1);

         Object var10;
         try {
            for(int var3 = this.layers.size() - 1; var3 >= 0; --var3) {
               ZLayer var4 = (ZLayer)this.layers.get(var3);
               Component var5 = var4.dispatchDown(var2);
               if (var5 != null) {
                  this.touchTarget = var5;
                  this.touchLayer = var4;
                  Component var6 = var5;
                  return var6;
               }
            }

            var10 = null;
         } finally {
            var2.recycle();
         }

         return (Component)var10;
      }

      private boolean dispatchToTarget(MotionEvent var1) {
         if (this.touchTarget == null) {
            return false;
         } else {
            MotionEvent var2 = this.translatedEvent(var1);
            boolean var8 = false;

            boolean var3;
            try {
               var8 = true;
               var3 = this.touchLayer == null ? this.touchTarget.onTouchEvent(var2) : this.touchLayer.dispatchTo(this.touchTarget, var2);
               var8 = false;
            } finally {
               if (var8) {
                  var2.recycle();
                  int var6 = var1.getActionMasked();
                  if (var6 == 1 || var6 == 3) {
                     this.touchTarget = null;
                     this.touchLayer = null;
                  }

               }
            }

            var2.recycle();
            int var4 = var1.getActionMasked();
            if (var4 == 1 || var4 == 3) {
               this.touchTarget = null;
               this.touchLayer = null;
            }

            return var3;
         }
      }

      private void cancelTouch(MotionEvent var1) {
         if (this.touchTarget != null) {
            MotionEvent var2 = MotionEvent.obtain(var1);
            var2.setAction(3);
            this.dispatchToTarget(var2);
            var2.recycle();
         }
      }

      private MotionEvent translatedEvent(MotionEvent var1) {
         MotionEvent var2 = MotionEvent.obtain(var1);
         var2.offsetLocation(-this.originX, -this.originY);
         return var2;
      }

      private boolean contains(float var1, float var2) {
         return var1 >= this.originX && var1 <= this.originX + this.width && var2 >= this.originY && var2 <= this.originY + this.height;
      }

      private void release() {
         Iterator var1 = (new ArrayList(this.layers)).iterator();

         while(var1.hasNext()) {
            ZLayer var2 = (ZLayer)var1.next();
            var2.clear();
         }

         this.layers.clear();
         this.layersByLocalId.clear();
         this.componentsByLocalId.clear();
         this.touchTarget = null;
         this.touchLayer = null;
      }
   }

   public static final class ItemScope {
      private final Item item;

      private ItemScope(Item var1) {
         this.item = var1;
      }

      public Context getContext() {
         return this.item.list.hostView.getContext();
      }

      public View getHostView() {
         return this.item.list.hostView;
      }

      public int getPosition() {
         return this.item.position;
      }

      public long getItemId() {
         return this.item.itemId;
      }

      public String id(String var1) {
         String var2 = ComponentList.requireId(var1, "Item component ID");
         return this.item.list.id + "/holder_" + this.item.holderNumber + "/" + var2;
      }

      public float width() {
         return this.item.width / this.item.list.dimensionScale;
      }

      public float height() {
         return this.item.height / this.item.list.dimensionScale;
      }

      public float px(float var1) {
         return ComponentList.requireNonNegative(var1, "Figma value") * this.item.list.dimensionScale;
      }

      public RectF rect(float var1, float var2, float var3, float var4) {
         ComponentList.requireNonNegative(var1, "Item left");
         ComponentList.requireNonNegative(var2, "Item top");
         ComponentList.requirePositive(var3, "Item width");
         ComponentList.requirePositive(var4, "Item height");
         float var5 = this.item.list.dimensionScale;
         return new RectF(var1 * var5, var2 * var5, (var1 + var3) * var5, (var2 + var4) * var5);
      }

      public RectF getBounds() {
         return new RectF(0.0F, 0.0F, this.item.width, this.item.height);
      }
   }
}

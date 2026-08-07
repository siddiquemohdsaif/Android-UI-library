package com.ogfa.nativeviews.card;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.NestedComponentHost;
import com.ogfa.nativeviews.zlayer.ZLayer;
import com.ogfa.nativeviews.zlayer.ZLayerOwner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A rounded background and drop-shadow container with nested ZLayers.
 */
public final class Card implements Component {

    public enum BackgroundType {
        COLOR,
        IMAGE
    }

    private final View hostView;
    private final String id;
    private final RectF baseBounds = new RectF();
    private final RectF bounds = new RectF();
    private final RectF visualBounds = new RectF();
    private final RectF backgroundDrawBounds = new RectF();
    private final RectF shadowBounds = new RectF();
    private final Path clipPath = new Path();
    private final Paint backgroundPaint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
    );
    private final Paint bitmapPaint = new Paint(
            Paint.ANTI_ALIAS_FLAG
                    | Paint.DITHER_FLAG
                    | Paint.FILTER_BITMAP_FLAG
    );
    private final Paint shadowPaint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
    );

    private final ZLayerOwner contentOwner = new ZLayerOwner() {
        @Override
        public View getHostView() {
            return hostView;
        }

        @Override
        public void registerLayerComponent(Component component) {
            registerChild(component);
        }

        @Override
        public void unregisterLayerComponent(Component component) {
            if (rootHost != null) {
                rootHost.unregisterNestedComponent(component);
            }
        }

        @Override
        public void invalidateLayer() {
            invalidate();
        }

        @Override
        public boolean ownsLayerTranslation() {
            return true;
        }

        @Override
        public void setOwnedLayerTranslation(float x, float y) {
            translationX = x;
            translationY = y;
            invalidate();
        }

        @Override
        public float getOwnedLayerTranslationX() {
            return translationX;
        }

        @Override
        public float getOwnedLayerTranslationY() {
            return translationY;
        }
    };

    private final NestedComponentHost childHost = new NestedComponentHost() {
        @Override
        public View getHostView() {
            return hostView;
        }

        @Override
        public RectF getComponentBounds() {
            return new RectF(bounds);
        }

        @Override
        public void invalidateComponent() {
            invalidate();
        }

        @Override
        public void postInvalidateComponentOnAnimation() {
            invalidate();
        }

        @Override
        public boolean requestFocus(TextField field) {
            return rootHost != null && rootHost.requestFocus(field);
        }

        @Override
        public void clearFocus(TextField field) {
            if (rootHost != null) rootHost.clearFocus(field);
        }

        @Override
        public void restartInput() {
            if (rootHost != null) rootHost.restartInput();
        }

        @Override
        public void updateSelection(TextField field) {
            if (rootHost != null) rootHost.updateSelection(field);
        }

        @Override
        public void registerNestedComponent(Component component) {
            if (rootHost != null) {
                rootHost.registerNestedComponent(component);
            }
        }

        @Override
        public void unregisterNestedComponent(Component component) {
            if (rootHost != null) {
                rootHost.unregisterNestedComponent(component);
            }
        }
    };

    private final ArrayList<ZLayer> contentLayers = new ArrayList<>();
    private final Map<String, ZLayer> contentLayersById =
            new LinkedHashMap<>();
    private final ZLayer contentLayer;

    private ComponentHost owner;
    private NestedComponentHost rootHost;
    private Component touchTarget;
    private ZLayer touchLayer;
    private boolean blockedTouch;
    private BackgroundType backgroundType;
    private int backgroundColor;
    private Bitmap backgroundImage;
    private Image.ScaleType backgroundScaleType;
    private float cornerRadius;
    private float resolvedCornerRadius;
    private boolean cornerRadiusInPixels;
    private DropShadow dropShadow;
    private DropShadow resolvedDropShadow;
    private boolean dropShadowInPixels;
    private float dimensionScale;
    private float alpha;
    private boolean visible;
    private boolean enabled;
    private boolean horizontalCentered;
    private boolean verticalCentered;
    private float translationX;
    private float translationY;
    private boolean released;

    private Card(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(
                hostView,
                "Host view cannot be null."
        );
        id = requireId(builder.id);
        contentLayer = addContentLayerInternal("content");
        backgroundType = builder.backgroundType;
        backgroundColor = builder.backgroundColor;
        backgroundImage = builder.backgroundImage;
        backgroundScaleType = builder.backgroundScaleType;
        bitmapPaint.setFilterBitmap(builder.filterBitmap);
        cornerRadius = builder.cornerRadius;
        cornerRadiusInPixels = builder.cornerRadiusInPixels;
        dropShadow = builder.dropShadow;
        dropShadowInPixels = builder.dropShadowInPixels;
        alpha = builder.alpha;
        visible = builder.visible;
        enabled = builder.enabled;
        horizontalCentered = builder.horizontalCentered;
        verticalCentered = builder.verticalCentered;
        resolveRegion(builder.position, builder.size, builder.explicitBounds);
        rebuildGeometry();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public RectF getBounds() {
        return offsetCopy(bounds, translationX, translationY);
    }

    public RectF getVisualBounds() {
        return offsetCopy(visualBounds, translationX, translationY);
    }

    /**
     * Returns the complete clipping region owned by the content ZLayer.
     */
    public RectF getContentBounds() {
        return offsetCopy(bounds, translationX, translationY);
    }

    public ZLayer getContentLayer() {
        ensureActive();
        return contentLayer;
    }

    public ZLayer addContentLayer(String layerId) {
        ensureActive();
        return addContentLayerInternal(layerId);
    }

    public ZLayer findContentLayer(String layerId) {
        ensureActive();
        if (layerId == null) return null;
        return contentLayersById.get(layerId.trim());
    }

    public List<ZLayer> getContentLayers() {
        ensureActive();
        return Collections.unmodifiableList(contentLayers);
    }

    public void bringContentLayerToFront(String layerId) {
        moveContentLayer(layerId, contentLayers.size() - 1);
    }

    public void sendContentLayerToBack(String layerId) {
        moveContentLayer(layerId, 0);
    }

    public void moveContentLayerAbove(String layerId, String referenceId) {
        ZLayer layer = requireContentLayer(layerId);
        ZLayer reference = requireContentLayer(referenceId);
        contentLayers.remove(layer);
        contentLayers.add(contentLayers.indexOf(reference) + 1, layer);
        invalidate();
    }

    public void moveContentLayerBelow(String layerId, String referenceId) {
        ZLayer layer = requireContentLayer(layerId);
        ZLayer reference = requireContentLayer(referenceId);
        contentLayers.remove(layer);
        contentLayers.add(Math.max(0, contentLayers.indexOf(reference)), layer);
        invalidate();
    }

    public void setContentLayerIndex(String layerId, int index) {
        if (index < 0 || index >= contentLayers.size()) {
            throw new IndexOutOfBoundsException("Content layer index: " + index);
        }
        moveContentLayer(layerId, index);
    }

    public float getTranslationX() { return translationX; }
    public float getTranslationY() { return translationY; }

    public Card setTranslation(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) {
            throw new IllegalArgumentException("Card translation must be finite.");
        }
        translationX = x;
        translationY = y;
        invalidate();
        return this;
    }

    public Card setTranslationX(float value) { return setTranslation(value, translationY); }
    public Card setTranslationY(float value) { return setTranslation(translationX, value); }
    public Card resetTranslation() { return setTranslation(0f, 0f); }

    public BackgroundType getBackgroundType() {
        return backgroundType;
    }

    public int getBackgroundColor() {
        ensureActive();
        if (backgroundType != BackgroundType.COLOR) {
            throw new IllegalStateException(
                    "Card currently has an image background: " + id
            );
        }
        return backgroundColor;
    }

    public Bitmap getBackgroundImage() {
        ensureActive();
        return backgroundType == BackgroundType.IMAGE
                ? backgroundImage
                : null;
    }

    public Image.ScaleType getBackgroundScaleType() {
        return backgroundScaleType;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public float getResolvedCornerRadius() {
        return resolvedCornerRadius;
    }

    public boolean isCornerRadiusInPixels() {
        return cornerRadiusInPixels;
    }

    public DropShadow getDropShadow() {
        return dropShadow;
    }

    public DropShadow getResolvedDropShadow() {
        return resolvedDropShadow;
    }

    public boolean isDropShadowInPixels() {
        return dropShadowInPixels;
    }

    public float getAlpha() {
        return alpha;
    }

    public boolean isHorizontalCentered() {
        return horizontalCentered;
    }

    public boolean isVerticalCentered() {
        return verticalCentered;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Card setBackgroundColor(int color) {
        ensureActive();
        backgroundType = BackgroundType.COLOR;
        backgroundColor = color;
        backgroundImage = null;
        invalidate();
        return this;
    }

    public Card setBackgroundImage(Bitmap bitmap) {
        ensureActive();
        backgroundImage = requireBitmap(bitmap);
        backgroundType = BackgroundType.IMAGE;
        resolveBackgroundDrawBounds();
        invalidate();
        return this;
    }

    public Card setBackgroundScaleType(Image.ScaleType scaleType) {
        ensureActive();
        backgroundScaleType = Objects.requireNonNull(
                scaleType,
                "Background scale type cannot be null."
        );
        resolveBackgroundDrawBounds();
        invalidate();
        return this;
    }

    public Card setFilterBitmap(boolean filterBitmap) {
        ensureActive();
        bitmapPaint.setFilterBitmap(filterBitmap);
        invalidate();
        return this;
    }

    public Card setCornerRadius(float radius) {
        ensureActive();
        cornerRadius = requireNonNegativeFinite(radius, "Corner radius");
        cornerRadiusInPixels = false;
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card setCornerRadiusPx(float radius) {
        ensureActive();
        cornerRadius = requireNonNegativeFinite(radius, "Corner radius");
        cornerRadiusInPixels = true;
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card setDropShadow(DropShadow shadow) {
        ensureActive();
        dropShadow = Objects.requireNonNull(
                shadow,
                "Drop shadow cannot be null."
        );
        dropShadowInPixels = false;
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card setDropShadowPx(DropShadow shadow) {
        ensureActive();
        dropShadow = Objects.requireNonNull(
                shadow,
                "Drop shadow cannot be null."
        );
        dropShadowInPixels = true;
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card removeDropShadow() {
        ensureActive();
        dropShadow = null;
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card resetDefaultDropShadow() {
        return setDropShadow(DropShadow.DEFAULT);
    }

    public Card setHorizontalCenter(boolean enabled) {
        ensureActive();
        if (horizontalCentered == enabled) return this;
        horizontalCentered = enabled;
        applyParentAlignment();
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card horizontalCenter(boolean enabled) {
        return setHorizontalCenter(enabled);
    }

    public Card setVerticalCenter(boolean enabled) {
        ensureActive();
        if (verticalCentered == enabled) return this;
        verticalCentered = enabled;
        applyParentAlignment();
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card verticalCenter(boolean enabled) {
        return setVerticalCenter(enabled);
    }

    public Card setRegion(Position position, Size size) {
        ensureActive();
        Objects.requireNonNull(position, "Position cannot be null.");
        Objects.requireNonNull(size, "Size cannot be null.");
        RectF resolved = position.toRectF(hostView, size);
        requireBounds(resolved);
        baseBounds.set(resolved);
        dimensionScale = position.getScale(hostView);
        applyParentAlignment();
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card setRegion(RectF bounds) {
        ensureActive();
        RectF copy = new RectF(Objects.requireNonNull(
                bounds,
                "Bounds cannot be null."
        ));
        requireBounds(copy);
        baseBounds.set(copy);
        dimensionScale = 1f;
        applyParentAlignment();
        rebuildGeometry();
        invalidate();
        return this;
    }

    public Card setAlpha(float alpha) {
        ensureActive();
        this.alpha = requireAlpha(alpha);
        invalidate();
        return this;
    }

    public Card setVisible(boolean visible) {
        ensureActive();
        this.visible = visible;
        if (!visible) cancelTouch();
        invalidate();
        return this;
    }

    public Card setEnabled(boolean enabled) {
        ensureActive();
        this.enabled = enabled;
        if (!enabled) cancelTouch();
        invalidate();
        return this;
    }

    @Override
    public void draw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        if (!visible || released || alpha <= 0f) return;

        int translationSave = canvas.save();
        canvas.translate(translationX, translationY);
        int compositeSave = alpha < 1f
                ? canvas.saveLayerAlpha(
                        visualBounds,
                        Math.round(alpha * 255f)
                )
                : canvas.save();

        drawShadow(canvas);

        int cardSave = canvas.save();
        if (resolvedCornerRadius > 0f) {
            canvas.clipPath(clipPath);
        } else {
            canvas.clipRect(bounds);
        }
        drawBackground(canvas);
        for (ZLayer layer : contentLayers) layer.draw(canvas);
        canvas.restoreToCount(cardSave);
        canvas.restoreToCount(compositeSave);
        canvas.restoreToCount(translationSave);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        MotionEvent localEvent = MotionEvent.obtain(event);
        localEvent.offsetLocation(-translationX, -translationY);
        try {
            return onLocalTouchEvent(localEvent);
        } finally {
            localEvent.recycle();
        }
    }

    private boolean onLocalTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelTouch();
                if (!visible || !enabled
                        || !bounds.contains(event.getX(), event.getY())) {
                    return false;
                }
                for (int i = contentLayers.size() - 1; i >= 0; i--) {
                    ZLayer layer = contentLayers.get(i);
                    touchTarget = layer.dispatchDown(event);
                    if (touchTarget != null) {
                        touchLayer = layer;
                        return true;
                    }
                    if (layer.isVisible() && layer.isEnabled()
                            && (layer.getTouchPolicy()
                            == ZLayer.TouchPolicy.MODAL
                            || (layer.getTouchPolicy()
                            == ZLayer.TouchPolicy.BLOCK_BELOW
                            && layer.containsPoint(
                            event.getX(),
                            event.getY()
                    )))) {
                        blockedTouch = true;
                        return true;
                    }
                }
                return blockedTouch;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (blockedTouch) {
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        blockedTouch = false;
                    }
                    return true;
                }
                if (touchTarget == null) return false;
                boolean handled = touchLayer == null
                        ? touchTarget.onTouchEvent(event)
                        : touchLayer.dispatchTo(touchTarget, event);
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    touchTarget = null;
                    touchLayer = null;
                }
                return handled;
            case MotionEvent.ACTION_CANCEL:
                boolean wasHandling = blockedTouch || touchTarget != null;
                cancelTouch();
                return wasHandling;
            default:
                return blockedTouch || touchTarget != null;
        }
    }

    @Override
    public void attach(ComponentHost owner) {
        ensureActive();
        if (!(owner instanceof NestedComponentHost)) {
            throw new IllegalArgumentException(
                    "Card requires a NestedComponentHost such as ZLayerGroup."
            );
        }
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException(
                    "Card already belongs to another component host."
            );
        }
        this.owner = owner;
        rootHost = (NestedComponentHost) owner;
        applyParentAlignment();
        rebuildGeometry();

        ArrayList<Component> registered = new ArrayList<>();
        try {
            for (ZLayer layer : contentLayers) {
                for (Component component : layer.getComponents()) {
                    rootHost.registerNestedComponent(component);
                    registered.add(component);
                    component.attach(childHost);
                }
            }
        } catch (RuntimeException exception) {
            for (Component component : registered) {
                rootHost.unregisterNestedComponent(component);
            }
            rootHost = null;
            this.owner = null;
            throw exception;
        }
    }

    @Override
    public void release() {
        if (released) return;
        cancelTouch();
        for (ZLayer layer : new ArrayList<>(contentLayers)) {
            layer.clear();
        }
        contentLayers.clear();
        contentLayersById.clear();
        released = true;
        owner = null;
        rootHost = null;
        backgroundImage = null;
        shadowPaint.setMaskFilter(null);
    }

    private void registerChild(Component component) {
        Objects.requireNonNull(component, "Component cannot be null.");
        if (rootHost != null) {
            rootHost.registerNestedComponent(component);
        }
        try {
            component.attach(childHost);
        } catch (RuntimeException exception) {
            if (rootHost != null) {
                rootHost.unregisterNestedComponent(component);
            }
            throw exception;
        }
    }

    private ZLayer addContentLayerInternal(String layerId) {
        String normalizedId = requireLayerId(layerId);
        if (contentLayersById.containsKey(normalizedId)) {
            throw new IllegalArgumentException(
                    "Duplicate Card content layer ID: " + normalizedId
            );
        }
        ZLayer layer = new ZLayer(
                contentOwner,
                id + ":" + normalizedId
        );
        contentLayers.add(layer);
        contentLayersById.put(normalizedId, layer);
        invalidate();
        return layer;
    }

    private ZLayer requireContentLayer(String layerId) {
        ZLayer layer = findContentLayer(layerId);
        if (layer == null) {
            throw new IllegalArgumentException("Unknown Card content layer: " + layerId);
        }
        return layer;
    }

    private void moveContentLayer(String layerId, int index) {
        ZLayer layer = requireContentLayer(layerId);
        contentLayers.remove(layer);
        contentLayers.add(Math.max(0, Math.min(index, contentLayers.size())), layer);
        invalidate();
    }

    private void resolveRegion(
            Position position,
            Size size,
            RectF explicitBounds
    ) {
        if (explicitBounds != null) {
            requireBounds(explicitBounds);
            baseBounds.set(explicitBounds);
            dimensionScale = 1f;
            applyParentAlignment();
            return;
        }
        Objects.requireNonNull(position, "Position cannot be null.");
        Objects.requireNonNull(size, "Size cannot be null.");
        RectF resolved = position.toRectF(hostView, size);
        requireBounds(resolved);
        baseBounds.set(resolved);
        dimensionScale = position.getScale(hostView);
        applyParentAlignment();
    }

    private void applyParentAlignment() {
        bounds.set(baseBounds);
        if ((!horizontalCentered && !verticalCentered)
                || baseBounds.isEmpty()) {
            return;
        }
        RectF parentBounds = owner == null
                ? new RectF(0f, 0f, hostView.getWidth(), hostView.getHeight())
                : owner.getComponentBounds();
        if (horizontalCentered) {
            float width = baseBounds.width();
            bounds.left = parentBounds.centerX() - width / 2f;
            bounds.right = bounds.left + width;
        }
        if (verticalCentered) {
            float height = baseBounds.height();
            bounds.top = parentBounds.centerY() - height / 2f;
            bounds.bottom = bounds.top + height;
        }
    }

    private void rebuildGeometry() {
        resolvedCornerRadius = Math.min(
                (cornerRadiusInPixels
                        ? cornerRadius
                        : cornerRadius * dimensionScale),
                Math.min(bounds.width(), bounds.height()) / 2f
        );
        clipPath.reset();
        if (resolvedCornerRadius > 0f) {
            clipPath.addRoundRect(
                    bounds,
                    resolvedCornerRadius,
                    resolvedCornerRadius,
                    Path.Direction.CW
            );
            clipPath.close();
        }

        resolvedDropShadow = dropShadow == null
                ? null
                : (dropShadowInPixels
                        ? dropShadow
                        : dropShadow.scale(dimensionScale));
        resolveShadowGeometry();
        resolveBackgroundDrawBounds();
    }

    private void resolveShadowGeometry() {
        if (resolvedDropShadow == null
                || Color.alpha(resolvedDropShadow.getColor()) == 0) {
            shadowBounds.setEmpty();
            visualBounds.set(bounds);
            shadowPaint.setMaskFilter(null);
            return;
        }

        float spread = resolvedDropShadow.getSpread();
        shadowBounds.set(
                bounds.left - spread + resolvedDropShadow.getX(),
                bounds.top - spread + resolvedDropShadow.getY(),
                bounds.right + spread + resolvedDropShadow.getX(),
                bounds.bottom + spread + resolvedDropShadow.getY()
        );
        float blur = resolvedDropShadow.getBlur();
        visualBounds.set(
                Math.min(bounds.left, shadowBounds.left - blur),
                Math.min(bounds.top, shadowBounds.top - blur),
                Math.max(bounds.right, shadowBounds.right + blur),
                Math.max(bounds.bottom, shadowBounds.bottom + blur)
        );
        shadowPaint.setColor(resolvedDropShadow.getColor());
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setMaskFilter(blur > 0f
                ? new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
                : null);
    }

    private void resolveBackgroundDrawBounds() {
        if (backgroundType != BackgroundType.IMAGE
                || backgroundImage == null) {
            backgroundDrawBounds.set(bounds);
            return;
        }
        requireBitmap(backgroundImage);
        if (backgroundScaleType == Image.ScaleType.FIT_XY) {
            backgroundDrawBounds.set(bounds);
            return;
        }
        float scaleX = bounds.width() / backgroundImage.getWidth();
        float scaleY = bounds.height() / backgroundImage.getHeight();
        float scale = backgroundScaleType == Image.ScaleType.CENTER_CROP
                ? Math.max(scaleX, scaleY)
                : Math.min(scaleX, scaleY);
        float width = backgroundImage.getWidth() * scale;
        float height = backgroundImage.getHeight() * scale;
        backgroundDrawBounds.set(
                bounds.centerX() - width / 2f,
                bounds.centerY() - height / 2f,
                bounds.centerX() + width / 2f,
                bounds.centerY() + height / 2f
        );
    }

    private void drawShadow(Canvas canvas) {
        if (resolvedDropShadow == null || shadowBounds.isEmpty()) return;
        float shadowRadius = Math.max(
                0f,
                resolvedCornerRadius + resolvedDropShadow.getSpread()
        );
        canvas.drawRoundRect(
                shadowBounds,
                shadowRadius,
                shadowRadius,
                shadowPaint
        );
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundType == BackgroundType.COLOR) {
            backgroundPaint.setColor(backgroundColor);
            canvas.drawRect(bounds, backgroundPaint);
            return;
        }
        requireBitmap(backgroundImage);
        canvas.drawBitmap(
                backgroundImage,
                null,
                backgroundDrawBounds,
                bitmapPaint
        );
    }

    private void cancelTouch() {
        blockedTouch = false;
        if (touchTarget != null) {
            long now = android.os.SystemClock.uptimeMillis();
            MotionEvent cancel = MotionEvent.obtain(
                    now,
                    now,
                    MotionEvent.ACTION_CANCEL,
                    0f,
                    0f,
                    0
            );
            if (touchLayer == null) {
                touchTarget.onTouchEvent(cancel);
            } else {
                touchLayer.dispatchTo(touchTarget, cancel);
            }
            cancel.recycle();
            touchTarget = null;
        }
        touchLayer = null;
    }

    private void invalidate() {
        if (owner != null) owner.postInvalidateComponentOnAnimation();
    }

    private void ensureActive() {
        if (released) {
            throw new IllegalStateException("Card has been released: " + id);
        }
    }

    private static String requireId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Card ID cannot be null or blank."
            );
        }
        return id.trim();
    }

    private static String requireLayerId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Card content layer ID cannot be null or blank."
            );
        }
        return id.trim();
    }

    private static RectF offsetCopy(RectF source, float dx, float dy) {
        RectF result = new RectF(source);
        result.offset(dx, dy);
        return result;
    }

    private static Bitmap requireBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            throw new IllegalArgumentException(
                    "Card background bitmap cannot be null."
            );
        }
        if (bitmap.isRecycled()
                || bitmap.getWidth() <= 0
                || bitmap.getHeight() <= 0) {
            throw new IllegalArgumentException(
                    "Card background bitmap must be active and dimensioned."
            );
        }
        return bitmap;
    }

    private static void requireBounds(RectF bounds) {
        if (!Float.isFinite(bounds.left)
                || !Float.isFinite(bounds.top)
                || !Float.isFinite(bounds.right)
                || !Float.isFinite(bounds.bottom)
                || bounds.width() <= 0f
                || bounds.height() <= 0f) {
            throw new IllegalArgumentException(
                    "Card bounds must have positive finite dimensions."
            );
        }
    }

    private static float requireNonNegativeFinite(
            float value,
            String label
    ) {
        if (!Float.isFinite(value) || value < 0f) {
            throw new IllegalArgumentException(
                    label + " must be non-negative and finite."
            );
        }
        return value;
    }

    private static float requireAlpha(float alpha) {
        if (!Float.isFinite(alpha) || alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException(
                    "Card alpha must be finite and in the 0..1 range."
            );
        }
        return alpha;
    }

    public static final class Builder implements ComponentFactory<Card> {

        private final Context context;
        private final String id;
        private final Position position;
        private final Size size;
        private final RectF explicitBounds;
        private BackgroundType backgroundType = BackgroundType.COLOR;
        private int backgroundColor = Color.WHITE;
        private Bitmap backgroundImage;
        private Image.ScaleType backgroundScaleType =
                Image.ScaleType.CENTER_CROP;
        private boolean filterBitmap = true;
        private float cornerRadius;
        private boolean cornerRadiusInPixels;
        private DropShadow dropShadow = DropShadow.DEFAULT;
        private boolean dropShadowInPixels;
        private float alpha = 1f;
        private boolean visible = true;
        private boolean enabled = true;
        private boolean horizontalCentered;
        private boolean verticalCentered;

        public Builder(
                Context context,
                String id,
                Position position,
                Size size
        ) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            this.position = Objects.requireNonNull(
                    position,
                    "Position cannot be null."
            );
            this.size = Objects.requireNonNull(size, "Size cannot be null.");
            explicitBounds = null;
        }

        public Builder(Context context, String id, RectF bounds) {
            this.context = Objects.requireNonNull(
                    context,
                    "Context cannot be null."
            );
            this.id = id;
            explicitBounds = new RectF(Objects.requireNonNull(
                    bounds,
                    "Bounds cannot be null."
            ));
            position = null;
            size = null;
        }

        public Builder setBackgroundColor(int color) {
            backgroundType = BackgroundType.COLOR;
            backgroundColor = color;
            backgroundImage = null;
            return this;
        }

        public Builder setBackgroundImage(Bitmap bitmap) {
            backgroundImage = requireBitmap(bitmap);
            backgroundType = BackgroundType.IMAGE;
            return this;
        }

        public Builder setBackgroundScaleType(Image.ScaleType scaleType) {
            backgroundScaleType = Objects.requireNonNull(
                    scaleType,
                    "Background scale type cannot be null."
            );
            return this;
        }

        public Builder setFilterBitmap(boolean filterBitmap) {
            this.filterBitmap = filterBitmap;
            return this;
        }

        public Builder setCornerRadius(float radius) {
            cornerRadius = requireNonNegativeFinite(
                    radius,
                    "Corner radius"
            );
            cornerRadiusInPixels = false;
            return this;
        }

        public Builder setCornerRadiusPx(float radius) {
            cornerRadius = requireNonNegativeFinite(
                    radius,
                    "Corner radius"
            );
            cornerRadiusInPixels = true;
            return this;
        }

        public Builder setDropShadow(DropShadow shadow) {
            dropShadow = Objects.requireNonNull(
                    shadow,
                    "Drop shadow cannot be null."
            );
            dropShadowInPixels = false;
            return this;
        }

        public Builder setDropShadowPx(DropShadow shadow) {
            dropShadow = Objects.requireNonNull(
                    shadow,
                    "Drop shadow cannot be null."
            );
            dropShadowInPixels = true;
            return this;
        }

        public Builder removeDropShadow() {
            dropShadow = null;
            return this;
        }

        public Builder setAlpha(float alpha) {
            this.alpha = requireAlpha(alpha);
            return this;
        }

        public Builder setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setHorizontalCenter(boolean enabled) {
            horizontalCentered = enabled;
            return this;
        }

        public Builder horizontalCenter(boolean enabled) {
            return setHorizontalCenter(enabled);
        }

        public Builder setVerticalCenter(boolean enabled) {
            verticalCentered = enabled;
            return this;
        }

        public Builder verticalCenter(boolean enabled) {
            return setVerticalCenter(enabled);
        }

        @Override
        public Card build(View hostView) {
            return new Card(this, hostView);
        }
    }
}

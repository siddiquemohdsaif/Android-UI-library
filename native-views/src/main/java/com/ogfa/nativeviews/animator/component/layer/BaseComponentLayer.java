package com.ogfa.nativeviews.animator.component.layer;

import android.graphics.Canvas;
import android.graphics.RectF;
import com.ogfa.nativeviews.animator.component.LayerRegion;
import java.util.Objects;

/** Common visibility, alpha, region resolution and idempotent cleanup for layers. */
public abstract class BaseComponentLayer implements ComponentLayer {
    private final String id;
    private LayerRegion region;
    private final RectF bounds = new RectF();
    private boolean visible = true;
    private float alpha = 1f;
    private boolean released;

    protected BaseComponentLayer(String id, LayerRegion region) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Layer id cannot be blank.");
        this.id = id;
        this.region = Objects.requireNonNull(region, "Layer region cannot be null.");
    }

    @Override public final String getId() { return id; }
    @Override public final LayerRegion getRegion() { return region; }
    @Override public final RectF getBounds() { return new RectF(bounds); }
    @Override public final boolean isVisible() { return visible; }
    @Override public final float getAlpha() { return alpha; }

    @Override public final ComponentLayer setRegion(LayerRegion region) {
        this.region = Objects.requireNonNull(region, "Layer region cannot be null.");
        return this;
    }

    @Override public final ComponentLayer setVisible(boolean visible) { this.visible = visible; return this; }
    @Override public final ComponentLayer setAlpha(float alpha) {
        if (!Float.isFinite(alpha)) throw new IllegalArgumentException("Alpha must be finite.");
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        return this;
    }

    @Override public final void resolveBounds(RectF componentBounds, float figmaScale) {
        RectF resolved = region.resolve(componentBounds, figmaScale);
        if (!bounds.equals(resolved)) {
            bounds.set(resolved);
            onBoundsChanged(new RectF(resolved));
        }
    }

    @Override public final void draw(Canvas canvas) {
        if (!visible || alpha <= 0f || released) return;
        if (alpha >= 1f) {
            onDraw(canvas);
            return;
        }
        int save = canvas.saveLayerAlpha(bounds, Math.round(alpha * 255f));
        onDraw(canvas);
        canvas.restoreToCount(save);
    }

    @Override public final void release() {
        if (released) return;
        released = true;
        onRelease();
    }

    protected abstract void onDraw(Canvas canvas);
    protected void onBoundsChanged(RectF bounds) {}
    protected void onRelease() {}
    @Override public boolean needsNextFrame() { return false; }
}

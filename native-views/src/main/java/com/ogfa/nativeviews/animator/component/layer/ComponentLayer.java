package com.ogfa.nativeviews.animator.component.layer;


import android.graphics.Canvas;
import android.graphics.RectF;
import com.ogfa.nativeviews.animator.component.LayerRegion;

public interface ComponentLayer {
    String getId();
    LayerRegion getRegion();
    RectF getBounds();
    ComponentLayer setRegion(LayerRegion region);
    void draw(Canvas canvas);
    void release();
    void resolveBounds(RectF componentBounds, float figmaScale);
    boolean isVisible();
    ComponentLayer setVisible(boolean visible);
    float getAlpha();
    ComponentLayer setAlpha(float alpha);
    boolean needsNextFrame();
}

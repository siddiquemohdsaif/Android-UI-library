package com.ogfa.nativeviews.component;

import android.graphics.RectF;
import android.view.View;

public interface ComponentHost {
    View getHostView();

    /**
     * Returns the runtime region components should align within.
     *
     * <p>A root ZLayer uses the full host view. Composite components can
     * override this to expose their own content region.</p>
     */
    default RectF getComponentBounds() {
        View view = getHostView();
        return new RectF(0f, 0f, view.getWidth(), view.getHeight());
    }

    void invalidateComponent();
    void postInvalidateComponentOnAnimation();
}

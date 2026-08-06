package com.ogfa.nativeviews.component;

import android.view.View;

public interface ComponentHost {
    View getHostView();
    void invalidateComponent();
    void postInvalidateComponentOnAnimation();
}

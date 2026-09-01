package com.ogfa.nativeviews.zlayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

/** ViewGroup host that consumes the first outside tap used to dismiss a native TextField. */
public abstract class ZLayerViewGroup extends ViewGroup {
    private final ZLayerGroup nativeLayers = new ZLayerGroup(this);

    protected ZLayerViewGroup(Context context) {
        super(context);
    }

    protected ZLayerViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected ZLayerViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected final ZLayerGroup getNativeLayers() {
        return nativeLayers;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (nativeLayers.onPreDispatchTouchEvent(event)) return true;
        return super.dispatchTouchEvent(event);
    }
}

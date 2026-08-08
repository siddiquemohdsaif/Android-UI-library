package com.ogfa.nativeviews.switchcomponent.internal;

import android.graphics.Canvas;

public interface SwitchRenderer {
    void drawTrack(Canvas canvas, SwitchRenderState state);
    void drawThumb(Canvas canvas, SwitchRenderState state);
    boolean supportsDrag();
    boolean usesSeparateThumb();
    void setFilterBitmap(boolean enabled);
    void release();
}

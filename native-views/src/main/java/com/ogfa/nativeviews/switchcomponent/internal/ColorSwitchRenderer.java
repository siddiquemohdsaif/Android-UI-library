package com.ogfa.nativeviews.switchcomponent.internal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public final class ColorSwitchRenderer implements SwitchRenderer {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF strokeBounds = new RectF();

    @Override public void drawTrack(Canvas canvas, SwitchRenderState state) {
        trackPaint.setColor(state.enabled
                ? blend(state.uncheckedTrackColor, state.checkedTrackColor, state.progress)
                : blend(state.disabledUncheckedTrackColor, state.disabledCheckedTrackColor, state.progress));
        canvas.drawRoundRect(state.trackBounds, state.cornerRadius, state.cornerRadius, trackPaint);
        if (state.strokeEnabled && state.strokeWidth > 0f) {
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(state.strokeWidth);
            strokePaint.setColor(state.enabled ? state.strokeColor : state.disabledStrokeColor);
            strokeBounds.set(state.trackBounds);
            strokeBounds.inset(state.strokeWidth / 2f, state.strokeWidth / 2f);
            float radius = Math.max(0f, state.cornerRadius - state.strokeWidth / 2f);
            canvas.drawRoundRect(strokeBounds, radius, radius, strokePaint);
        }
    }

    @Override public void drawThumb(Canvas canvas, SwitchRenderState state) {
        thumbPaint.setColor(state.enabled
                ? state.thumbColor
                : blend(state.disabledUncheckedThumbColor,
                        state.disabledCheckedThumbColor, state.progress));
        canvas.drawOval(state.thumbBounds, thumbPaint);
    }

    @Override public boolean supportsDrag() { return true; }
    @Override public boolean usesSeparateThumb() { return true; }
    @Override public void setFilterBitmap(boolean enabled) { }
    @Override public void release() { }

    private static int blend(int start, int end, float fraction) {
        int a = Math.round(Color.alpha(start) + (Color.alpha(end) - Color.alpha(start)) * fraction);
        int r = Math.round(Color.red(start) + (Color.red(end) - Color.red(start)) * fraction);
        int g = Math.round(Color.green(start) + (Color.green(end) - Color.green(start)) * fraction);
        int b = Math.round(Color.blue(start) + (Color.blue(end) - Color.blue(start)) * fraction);
        return Color.argb(a, r, g, b);
    }
}

package com.ogfa.nativeviews.radiobutton.internal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Native centered circle, ring, and animated inner-dot renderer. */
public final class ColorRadioRenderer implements RadioRenderer {
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override public void draw(Canvas canvas, RadioRenderState state) {
        float side = Math.min(state.bounds.width(), state.bounds.height());
        float radius = side / 2f - state.padding;
        float centerX = state.bounds.centerX();
        float centerY = state.bounds.centerY();
        float progress = clamp(state.progress);

        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(state.enabled
                ? state.backgroundColor : state.disabledBackgroundColor);
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(state.ringWidth);
        int unchecked = state.enabled ? state.uncheckedColor : state.disabledUncheckedColor;
        int checked = state.enabled ? state.checkedColor : state.disabledCheckedColor;
        ringPaint.setColor(blend(unchecked, checked, progress));
        canvas.drawCircle(centerX, centerY,
                Math.max(0f, radius - state.ringWidth / 2f), ringPaint);

        if (progress > 0f) {
            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(state.enabled ? state.dotColor : state.disabledDotColor);
            canvas.drawCircle(centerX, centerY, state.dotSize * progress / 2f, dotPaint);
        }
    }

    private static int blend(int start, int end, float fraction) {
        fraction = clamp(fraction);
        return Color.argb(
                Math.round(Color.alpha(start) + (Color.alpha(end) - Color.alpha(start)) * fraction),
                Math.round(Color.red(start) + (Color.red(end) - Color.red(start)) * fraction),
                Math.round(Color.green(start) + (Color.green(end) - Color.green(start)) * fraction),
                Math.round(Color.blue(start) + (Color.blue(end) - Color.blue(start)) * fraction));
    }
    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
    @Override public void setImageFiltering(boolean enabled) { }
    @Override public void release() { }
}

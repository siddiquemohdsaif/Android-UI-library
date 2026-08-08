package com.ogfa.nativeviews.checkbox.internal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;

import com.ogfa.nativeviews.checkbox.CheckBox;

/** Allocation-conscious native box/check/dash renderer. */
public final class ColorCheckBoxRenderer implements CheckBoxRenderer {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF strokeBounds = new RectF();
    private final Path checkPath = new Path();
    private final Path visibleCheckPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();

    @Override public void draw(Canvas canvas, CheckBoxRenderState state) {
        float fraction = clamp(state.transitionProgress);
        int fromColor = backgroundColor(state, state.fromState);
        int toColor = backgroundColor(state, state.state);
        fillPaint.setColor(blend(fromColor, toColor, fraction));
        canvas.drawRoundRect(state.bounds, state.cornerRadius, state.cornerRadius, fillPaint);

        if (state.strokeWidth > 0f) {
            strokeBounds.set(state.bounds);
            strokeBounds.inset(state.strokeWidth / 2f, state.strokeWidth / 2f);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(state.strokeWidth);
            strokePaint.setColor(state.enabled ? state.strokeColor : state.disabledStrokeColor);
            float radius = Math.max(0f, state.cornerRadius - state.strokeWidth / 2f);
            canvas.drawRoundRect(strokeBounds, radius, radius, strokePaint);
        }

        if (state.fromState != state.state && state.fromState != CheckBox.State.UNCHECKED) {
            drawMark(canvas, state, state.fromState, 1f - fraction, 1f - fraction);
        }
        if (state.state != CheckBox.State.UNCHECKED) {
            drawMark(canvas, state, state.state, fraction, 1f);
        }
    }

    private void drawMark(
            Canvas canvas,
            CheckBoxRenderState state,
            CheckBox.State markState,
            float reveal,
            float alpha
    ) {
        if (reveal <= 0f || alpha <= 0f) return;
        markPaint.setStyle(Paint.Style.STROKE);
        markPaint.setStrokeCap(Paint.Cap.ROUND);
        markPaint.setStrokeJoin(Paint.Join.ROUND);
        markPaint.setStrokeWidth(state.markWidth);
        int color = state.enabled ? state.checkMarkColor : state.disabledCheckMarkColor;
        markPaint.setColor((color & 0x00ffffff) |
                (Math.round(Color.alpha(color) * clamp(alpha)) << 24));

        RectF inner = new RectF(state.bounds);
        inner.inset(state.padding, state.padding);
        if (inner.width() <= 0f || inner.height() <= 0f) return;

        if (markState == CheckBox.State.INDETERMINATE) {
            float half = inner.width() * 0.38f * clamp(reveal);
            canvas.drawLine(inner.centerX() - half, inner.centerY(),
                    inner.centerX() + half, inner.centerY(), markPaint);
            return;
        }

        checkPath.reset();
        checkPath.moveTo(inner.left + inner.width() * 0.08f, inner.top + inner.height() * 0.52f);
        checkPath.lineTo(inner.left + inner.width() * 0.38f, inner.top + inner.height() * 0.82f);
        checkPath.lineTo(inner.left + inner.width() * 0.94f, inner.top + inner.height() * 0.18f);
        pathMeasure.setPath(checkPath, false);
        visibleCheckPath.reset();
        pathMeasure.getSegment(0f, pathMeasure.getLength() * clamp(reveal),
                visibleCheckPath, true);
        canvas.drawPath(visibleCheckPath, markPaint);
    }

    private static int backgroundColor(CheckBoxRenderState state, CheckBox.State value) {
        if (state.enabled) {
            switch (value) {
                case CHECKED: return state.checkedColor;
                case INDETERMINATE: return state.indeterminateColor;
                default: return state.uncheckedColor;
            }
        }
        switch (value) {
            case CHECKED: return state.disabledCheckedColor;
            case INDETERMINATE: return state.disabledIndeterminateColor;
            default: return state.disabledUncheckedColor;
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

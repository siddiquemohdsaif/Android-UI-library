package com.ogfa.nativeviews.progress.internal;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ogfa.nativeviews.progress.Progress;

/** Native linear and circular determinate/indeterminate renderer. */
public final class NativeProgressRenderer {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final RectF segment = new RectF();
    private final RectF circle = new RectF();

    public void draw(Canvas canvas, ProgressRenderState state) {
        trackPaint.setColor(state.trackColor);
        progressPaint.setColor(state.progressColor);
        if (state.style == Progress.Style.CIRCULAR) drawCircular(canvas, state);
        else drawLinear(canvas, state);
    }

    private void drawLinear(Canvas canvas, ProgressRenderState state) {
        // Circular rendering uses stroke paints; reset them when the same Progress
        // instance switches back to a native linear renderer at runtime.
        trackPaint.setStyle(Paint.Style.FILL);
        progressPaint.setStyle(Paint.Style.FILL);
        boolean vertical = state.linearDirection == Progress.LinearDirection.TOP_TO_BOTTOM
                || state.linearDirection == Progress.LinearDirection.BOTTOM_TO_TOP;
        track.set(state.bounds);
        track.inset(state.padding, state.padding);
        if (vertical) {
            float half = state.thickness / 2f;
            track.left = track.centerX() - half;
            track.right = track.centerX() + half;
        } else {
            float half = state.thickness / 2f;
            track.top = track.centerY() - half;
            track.bottom = track.centerY() + half;
        }
        canvas.drawRoundRect(track, state.cornerRadius, state.cornerRadius, trackPaint);

        if (state.mode == Progress.Mode.DETERMINATE) {
            drawLinearRange(canvas, state, 0f, state.progress, vertical);
            return;
        }
        float start = state.phase;
        float end = start + state.segmentSize;
        if (end <= 1f) drawLinearRange(canvas, state, start, end, vertical);
        else {
            drawLinearRange(canvas, state, start, 1f, vertical);
            drawLinearRange(canvas, state, 0f, end - 1f, vertical);
        }
    }

    private void drawLinearRange(
            Canvas canvas, ProgressRenderState state,
            float startFraction, float endFraction, boolean vertical) {
        startFraction = clamp(startFraction);
        endFraction = clamp(endFraction);
        if (endFraction <= startFraction) return;
        segment.set(track);
        boolean reverse = state.linearDirection == Progress.LinearDirection.RIGHT_TO_LEFT
                || state.linearDirection == Progress.LinearDirection.BOTTOM_TO_TOP;
        if (vertical) {
            float length = track.height();
            if (reverse) {
                segment.top = track.bottom - length * endFraction;
                segment.bottom = track.bottom - length * startFraction;
            } else {
                segment.top = track.top + length * startFraction;
                segment.bottom = track.top + length * endFraction;
            }
        } else {
            float length = track.width();
            if (reverse) {
                segment.left = track.right - length * endFraction;
                segment.right = track.right - length * startFraction;
            } else {
                segment.left = track.left + length * startFraction;
                segment.right = track.left + length * endFraction;
            }
        }
        canvas.drawRoundRect(segment, state.cornerRadius, state.cornerRadius, progressPaint);
    }

    private void drawCircular(Canvas canvas, ProgressRenderState state) {
        circle.set(state.bounds);
        float side = Math.min(circle.width(), circle.height());
        circle.set(
                circle.centerX() - side / 2f + state.padding + state.thickness / 2f,
                circle.centerY() - side / 2f + state.padding + state.thickness / 2f,
                circle.centerX() + side / 2f - state.padding - state.thickness / 2f,
                circle.centerY() + side / 2f - state.padding - state.thickness / 2f);
        Paint.Cap cap = state.strokeCap == Progress.StrokeCap.ROUND
                ? Paint.Cap.ROUND : state.strokeCap == Progress.StrokeCap.SQUARE
                ? Paint.Cap.SQUARE : Paint.Cap.BUTT;
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(state.thickness);
        trackPaint.setStrokeCap(cap);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(state.thickness);
        progressPaint.setStrokeCap(cap);
        canvas.drawArc(circle, 0f, 360f, false, trackPaint);
        float direction = state.circularDirection == Progress.CircularDirection.CLOCKWISE ? 1f : -1f;
        float start = state.startAngle;
        float sweep;
        if (state.mode == Progress.Mode.DETERMINATE) {
            sweep = 360f * state.progress * direction;
        } else {
            start += 360f * state.phase * direction;
            sweep = state.sweepAngle * direction;
        }
        canvas.drawArc(circle, start, sweep, false, progressPaint);
    }

    private static float clamp(float value) { return Math.max(0f, Math.min(1f, value)); }
}

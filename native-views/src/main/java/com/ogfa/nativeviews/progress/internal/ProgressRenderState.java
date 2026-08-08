package com.ogfa.nativeviews.progress.internal;

import android.graphics.RectF;

import com.ogfa.nativeviews.progress.Progress;

public final class ProgressRenderState {
    public final RectF bounds = new RectF();
    public Progress.Style style;
    public Progress.Mode mode;
    public Progress.LinearDirection linearDirection;
    public Progress.CircularDirection circularDirection;
    public Progress.StrokeCap strokeCap;
    public float progress;
    public float phase;
    public float thickness;
    public float cornerRadius;
    public float padding;
    public float segmentSize;
    public float sweepAngle;
    public float startAngle;
    public int trackColor;
    public int progressColor;
}

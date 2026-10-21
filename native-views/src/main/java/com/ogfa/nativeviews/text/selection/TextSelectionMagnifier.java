package com.ogfa.nativeviews.text.selection;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.View;

/** The TextField lens geometry and render pass shared by both components. */
public final class TextSelectionMagnifier {
    public static final long TOOLBAR_DELAY_MS = 100L;

    private TextSelectionMagnifier() {}

    public interface ContentDrawer {
        void draw(Canvas canvas);
    }

    public static void draw(Canvas canvas, View hostView, Matrix drawMatrix,
                            PointF source, RectF lineBounds, int backgroundColor,
                            float textSize, int markerColor, boolean showMarker,
                            ContentDrawer contentDrawer) {
        if (hostView.getWidth() <= 0 || hostView.getHeight() <= 0) return;
        float[] mapped = {source.x, source.y};
        drawMatrix.mapPoints(mapped);
        float density = hostView.getResources().getDisplayMetrics().density;
        float margin = 4f * density;
        float width = Math.min(124f * density,
                Math.max(1f, hostView.getWidth() - margin * 2f));
        float height = Math.min(52f * density,
                Math.max(1f, hostView.getHeight() - margin * 2f));
        float halfWidth = width / 2f;
        float halfHeight = height / 2f;
        float centerX = Math.max(halfWidth + margin,
                Math.min(mapped[0], hostView.getWidth() - halfWidth - margin));
        float centerY = mapped[1] - 47f * density;
        if (centerY - halfHeight < margin) centerY = mapped[1] + 47f * density;
        centerY = Math.max(halfHeight + margin,
                Math.min(centerY, hostView.getHeight() - halfHeight - margin));

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF lens = new RectF(centerX - halfWidth, centerY - halfHeight,
                centerX + halfWidth, centerY + halfHeight);
        int save = canvas.save();
        canvas.setMatrix(new Matrix());
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(lens, halfHeight, halfHeight, paint);
        Path clip = new Path();
        clip.addRoundRect(lens, halfHeight, halfHeight, Path.Direction.CW);
        canvas.clipPath(clip);
        canvas.translate(centerX, centerY);
        canvas.scale(1.65f, 1.65f);
        canvas.translate(-mapped[0], -mapped[1]);
        canvas.concat(drawMatrix);
        canvas.clipRect(lineBounds);
        contentDrawer.draw(canvas);
        if (showMarker) {
            paint.setColor(markerColor);
            float markerHalfHeight = textSize * 0.55f;
            canvas.drawRect(source.x - density * 0.7f,
                    source.y - markerHalfHeight,
                    source.x + density * 0.7f,
                    source.y + markerHalfHeight, paint);
        }
        canvas.restoreToCount(save);
        int outlineSave = canvas.save();
        canvas.setMatrix(new Matrix());
        paint.setColor(0x88666666);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(density);
        canvas.drawRoundRect(lens, halfHeight, halfHeight, paint);
        canvas.restoreToCount(outlineSave);
    }
}

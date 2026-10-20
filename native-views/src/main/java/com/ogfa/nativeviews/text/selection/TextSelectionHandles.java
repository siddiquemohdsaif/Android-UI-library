package com.ogfa.nativeviews.text.selection;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Android themed selection handles and hit geometry shared by both text components. */
public final class TextSelectionHandles {
    private final Drawable[] drawables = new Drawable[3];
    private final PointF[] hotspots = new PointF[3];

    public TextSelectionHandles(Context context, int color) {
        TypedArray handles = context.obtainStyledAttributes(new int[]{
                android.R.attr.textSelectHandleLeft,
                android.R.attr.textSelectHandleRight,
                android.R.attr.textSelectHandle
        });
        try {
            for (int type = 1; type <= 3; type++) {
                drawables[type - 1] = tint(handles.getDrawable(type - 1), color);
                hotspots[type - 1] = findHotspot(drawables[type - 1], type);
            }
        } finally {
            handles.recycle();
        }
    }

    public RectF drawingBounds(RectF bounds, float radius) {
        int fallback = Math.round(radius * 2f);
        int horizontal = fallback;
        int vertical = fallback;
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                horizontal = Math.max(horizontal, Math.max(1, drawable.getIntrinsicWidth()));
                vertical = Math.max(vertical, Math.max(1, drawable.getIntrinsicHeight()));
            }
        }
        RectF padded = new RectF(bounds);
        padded.inset(-horizontal, -vertical);
        return padded;
    }

    public RectF touchBounds(RectF bounds, float radius, int touchSlop) {
        RectF padded = drawingBounds(bounds, radius);
        padded.inset(-Math.max(0, touchSlop), -Math.max(0, touchSlop));
        return padded;
    }

    public boolean hit(float x, float y, PointF endpoint, int type,
                       float radius, int touchSlop) {
        if (endpoint == null) return false;
        Rect handle = bounds(endpoint, type, radius);
        float inset = Math.max(0, touchSlop);
        return x >= handle.left - inset && x <= handle.right + inset
                && y >= handle.top - inset && y <= handle.bottom + inset;
    }

    public void draw(Canvas canvas, PointF endpoint, int type, RectF bounds,
                     float radius, int color, Paint fallbackPaint) {
        if (endpoint == null) return;
        Rect handle = bounds(endpoint, type, radius);
        int save = canvas.save();
        canvas.clipRect(drawingBounds(bounds, radius));
        Drawable drawable = drawables[type - 1];
        if (drawable != null) {
            drawable.setBounds(handle);
            drawable.draw(canvas);
        } else {
            fallbackPaint.setColor(color);
            fallbackPaint.setStyle(Paint.Style.FILL);
            float centerX = handle.exactCenterX();
            canvas.drawRect(centerX - 1f, handle.top, centerX + 1f,
                    handle.centerY(), fallbackPaint);
            canvas.drawRoundRect(handle.left, handle.centerY(), handle.right,
                    handle.bottom, handle.width() * 0.25f,
                    handle.width() * 0.25f, fallbackPaint);
        }
        canvas.restoreToCount(save);
    }

    private Rect bounds(PointF endpoint, int type, float radius) {
        Drawable drawable = drawables[type - 1];
        int fallback = Math.round(radius * 2f);
        int width = drawable == null ? fallback : Math.max(1, drawable.getIntrinsicWidth());
        int height = drawable == null ? fallback : Math.max(1, drawable.getIntrinsicHeight());
        PointF hotspot = hotspots[type - 1];
        int left = Math.round(endpoint.x - (drawable == null ? width / 2f : hotspot.x));
        int top = Math.round(endpoint.y - hotspot.y);
        return new Rect(left, top, left + width, top + height);
    }

    private static Drawable tint(Drawable drawable, int color) {
        if (drawable == null) return null;
        Drawable tinted = drawable.mutate();
        tinted.setTint(color);
        return tinted;
    }

    private static PointF findHotspot(Drawable drawable, int type) {
        if (drawable == null) return new PointF(0.5f, 0f);
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        float fallbackX = type == 1 ? width : type == 2 ? 0f : width / 2f;
        if (width <= 0 || height <= 0 || width > 512 || height > 512) {
            return new PointF(fallbackX, 0f);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Rect original = new Rect(drawable.getBounds());
        try {
            drawable.setBounds(0, 0, width, height);
            drawable.draw(new Canvas(bitmap));
            for (int y = 0; y < height; y++) {
                int first = -1;
                int last = -1;
                for (int x = 0; x < width; x++) {
                    if ((bitmap.getPixel(x, y) >>> 24) >= 64) {
                        if (first < 0) first = x;
                        last = x;
                    }
                }
                if (first >= 0) {
                    float x = type == 1 ? last + 1f
                            : type == 2 ? first : (first + last + 1f) / 2f;
                    return new PointF(x, y);
                }
            }
            return new PointF(fallbackX, 0f);
        } finally {
            drawable.setBounds(original);
            bitmap.recycle();
        }
    }
}

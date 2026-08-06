package com.ogfa.nativeviews.text;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Composes bitmap elements onto another bitmap.
 */
public final class ImageWriter {

    private ImageWriter() {
    }

    public static Bitmap writeImage(
            Bitmap bitmap,
            ArrayList<ElementWriter> elementWriters
    ) {
        requireBitmap(bitmap, "Source bitmap");
        Bitmap result = Bitmap.createBitmap(
                bitmap.getWidth(),
                bitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        new Canvas(result).drawBitmap(bitmap, 0f, 0f, null);
        writeImageByOverwrite(result, elementWriters);
        return result;
    }

    public static void writeImageByOverwrite(
            Bitmap resultBitmap,
            ArrayList<ElementWriter> elementWriters
    ) {
        requireBitmap(resultBitmap, "Destination bitmap");
        Objects.requireNonNull(elementWriters, "Element writers cannot be null.");
        for (ElementWriter writer : elementWriters) {
            draw(resultBitmap, Objects.requireNonNull(writer));
        }
    }

    private static void draw(Bitmap destination, ElementWriter writer) {
        requireBitmap(writer.bitmap, "Element bitmap");
        float left = writer.left;
        if (writer.type == LineType.END) {
            left -= writer.bitmap.getWidth();
        } else if (writer.type == LineType.MIDDLE) {
            left -= writer.bitmap.getWidth() / 2f;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
                | Paint.FILTER_BITMAP_FLAG);
        paint.setAlpha(TextWriter.alphaToByte(writer.alpha));
        new Canvas(destination).drawBitmap(writer.bitmap, left, writer.top, paint);
    }

    private static void requireBitmap(Bitmap bitmap, String label) {
        Objects.requireNonNull(bitmap, label + " cannot be null.");
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException(label + " has been recycled.");
        }
    }

    public static class ElementWriter {
        public Bitmap bitmap;
        public float left;
        public float top;
        public LineType type;
        public float alpha;

        public ElementWriter(
                Bitmap bitmap,
                float left,
                float top,
                LineType type,
                float alpha
        ) {
            this.bitmap = bitmap;
            this.left = left;
            this.top = top;
            this.type = Objects.requireNonNull(type, "Line type cannot be null.");
            this.alpha = alpha;
        }
    }

    public enum LineType {
        START,
        MIDDLE,
        END
    }
}

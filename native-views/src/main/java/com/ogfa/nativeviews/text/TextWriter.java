package com.ogfa.nativeviews.text;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Composes bitmap-font text onto another bitmap.
 */
public final class TextWriter {

    private TextWriter() {
    }

    public static Bitmap writeTextToBitmap(
            Bitmap bitmap,
            ArrayList<ElementWriter> elementWriters
    ) {
        Bitmap result = copy(bitmap);
        writeTextToBitmapByOverWrite(result, elementWriters);
        return result;
    }

    public static void writeTextToBitmapByOverWrite(
            Bitmap resultBitmap,
            ArrayList<ElementWriter> elementWriters
    ) {
        requireBitmap(resultBitmap, "Destination bitmap");
        Objects.requireNonNull(elementWriters, "Element writers cannot be null.");
        for (ElementWriter writer : elementWriters) {
            draw(resultBitmap, Objects.requireNonNull(writer), false);
        }
    }

    public static Bitmap writeTextToBitmapWithSpacing(
            Bitmap bitmap,
            ArrayList<ElementWriterWithSpacing> elementWriters
    ) {
        Bitmap result = copy(bitmap);
        Objects.requireNonNull(elementWriters, "Element writers cannot be null.");
        for (ElementWriterWithSpacing writer : elementWriters) {
            draw(result, Objects.requireNonNull(writer), true);
        }
        return result;
    }

    public static Bitmap writeTextToBitmapWithSpacingWidthReduction(
            Bitmap bitmap,
            ArrayList<ElementWriterWithSpacing> elementWriters
    ) {
        Bitmap result = copy(bitmap);
        Objects.requireNonNull(elementWriters, "Element writers cannot be null.");
        for (ElementWriterWithSpacing writer : elementWriters) {
            Bitmap textBitmap = createText(writer, true);
            int targetWidth = textBitmap.getWidth() + 2 * textBitmap.getHeight();
            if (result.getWidth() > targetWidth) {
                result = removeMiddle(result, result.getWidth() - targetWidth);
            }
            drawBitmap(result, textBitmap, writer.left, writer.top,
                    writer.type, writer.alpha);
        }
        return result;
    }

    private static void draw(
            Bitmap destination,
            ElementWriter writer,
            boolean ignored
    ) {
        Bitmap textBitmap = TextMakerEngine.generateTextBitmap(
                writer.textMap,
                writer.text,
                Math.round(writer.lineHeight)
        );
        drawBitmap(destination, textBitmap, writer.left, writer.top,
                writer.type, writer.alpha);
    }

    private static void draw(
            Bitmap destination,
            ElementWriterWithSpacing writer,
            boolean withSpacing
    ) {
        Bitmap textBitmap = createText(writer, withSpacing);
        drawBitmap(destination, textBitmap, writer.left, writer.top,
                writer.type, writer.alpha);
    }

    private static Bitmap createText(
            ElementWriterWithSpacing writer,
            boolean withSpacing
    ) {
        if (withSpacing) {
            return TextMakerEngine.generateTextBitmapWithSpacing(
                    writer.textMap,
                    writer.text,
                    Math.round(writer.lineHeight),
                    writer.spaceInPx
            );
        }
        return TextMakerEngine.generateTextBitmap(
                writer.textMap,
                writer.text,
                Math.round(writer.lineHeight)
        );
    }

    private static void drawBitmap(
            Bitmap destination,
            Bitmap textBitmap,
            float anchorX,
            float top,
            LineType lineType,
            float alpha
    ) {
        float left = anchoredLeft(anchorX, textBitmap.getWidth(), lineType);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
                | Paint.FILTER_BITMAP_FLAG);
        paint.setAlpha(alphaToByte(alpha));
        new Canvas(destination).drawBitmap(textBitmap, left, top, paint);
    }

    private static float anchoredLeft(
            float anchorX,
            int width,
            LineType lineType
    ) {
        Objects.requireNonNull(lineType, "Line type cannot be null.");
        if (lineType == LineType.END) {
            return anchorX - width;
        }
        if (lineType == LineType.MIDDLE) {
            return anchorX - width / 2f;
        }
        return anchorX;
    }

    static int alphaToByte(float alpha) {
        return Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
    }

    private static Bitmap copy(Bitmap bitmap) {
        requireBitmap(bitmap, "Source bitmap");
        Bitmap result = Bitmap.createBitmap(
                bitmap.getWidth(),
                bitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        new Canvas(result).drawBitmap(bitmap, 0f, 0f, null);
        return result;
    }

    private static Bitmap removeMiddle(Bitmap bitmap, int removeWidth) {
        if (removeWidth <= 0 || removeWidth >= bitmap.getWidth()) {
            return bitmap;
        }
        int leftWidth = (bitmap.getWidth() - removeWidth) / 2;
        int rightStart = leftWidth + removeWidth;
        Bitmap result = Bitmap.createBitmap(
                bitmap.getWidth() - removeWidth,
                bitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(
                bitmap,
                new Rect(0, 0, leftWidth, bitmap.getHeight()),
                new Rect(0, 0, leftWidth, bitmap.getHeight()),
                null
        );
        canvas.drawBitmap(
                bitmap,
                new Rect(
                        rightStart,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight()
                ),
                new Rect(
                        leftWidth,
                        0,
                        result.getWidth(),
                        result.getHeight()
                ),
                null
        );
        return result;
    }

    private static void requireBitmap(Bitmap bitmap, String label) {
        Objects.requireNonNull(bitmap, label + " cannot be null.");
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException(label + " has been recycled.");
        }
    }

    public static class ElementWriter {
        public Map<String, Bitmap> textMap;
        public String text;
        public float left;
        public float top;
        public float lineHeight;
        public LineType type;
        public float alpha;

        public ElementWriter(
                Map<String, Bitmap> textMap,
                String text,
                float left,
                float top,
                float lineHeight,
                LineType type,
                float alpha
        ) {
            this.textMap = textMap;
            this.text = text;
            this.left = left;
            this.top = top;
            this.lineHeight = lineHeight;
            this.type = type;
            this.alpha = alpha;
        }
    }

    public static class ElementWriterWithSpacing extends ElementWriter {
        public int spaceInPx;

        public ElementWriterWithSpacing(
                Map<String, Bitmap> textMap,
                String text,
                float left,
                float top,
                float lineHeight,
                LineType type,
                float alpha,
                int spaceInPx
        ) {
            super(textMap, text, left, top, lineHeight, type, alpha);
            this.spaceInPx = spaceInPx;
        }
    }

    public enum LineType {
        START,
        MIDDLE,
        END
    }
}

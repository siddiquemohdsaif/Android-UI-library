package com.ogfa.nativeviews.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Writes text rendered from Android font resources onto bitmaps.
 *
 * <p>"Native" distinguishes this API from the bitmap-glyph {@link TextWriter};
 * it does not use JNI.</p>
 */
public final class TextWriterNative {

    private TextWriterNative() {
    }

    public static Bitmap writeTextToBitmap(
            Context context,
            Bitmap bitmap,
            ArrayList<ElementWriter> elementWriters
    ) {
        Bitmap result = copy(bitmap);
        writeTextToBitmapByOverWrite(context, result, elementWriters);
        return result;
    }

    public static void writeTextToBitmapByOverWrite(
            Context context,
            Bitmap resultBitmap,
            ArrayList<ElementWriter> elementWriters
    ) {
        requireMutableBitmap(resultBitmap, "Destination bitmap");
        requireList(elementWriters);
        for (ElementWriter writer : elementWriters) {
            draw(context, resultBitmap, Objects.requireNonNull(
                    writer,
                    "Element writer cannot be null."
            ));
        }
    }

    public static void writeTextToBitmapWithSpacingByOverWrite(
            Context context,
            Bitmap bitmap,
            ArrayList<ElementWriterWithSpacing> elementWriters
    ) {
        requireMutableBitmap(bitmap, "Destination bitmap");
        requireList(elementWriters);
        for (ElementWriterWithSpacing writer : elementWriters) {
            draw(context, bitmap, Objects.requireNonNull(
                    writer,
                    "Element writer cannot be null."
            ));
        }
    }

    public static Bitmap writeTextToBitmapWithSpacing(
            Context context,
            Bitmap bitmap,
            ArrayList<ElementWriterWithSpacing> elementWriters
    ) {
        Bitmap result = copy(bitmap);
        writeTextToBitmapWithSpacingByOverWrite(context, result, elementWriters);
        return result;
    }

    /**
     * Shrinks the copied background to text width plus one text-height of padding
     * on each side, then draws the text.
     */
    public static Bitmap writeTextToBitmapWithSpacingWidthReduction(
            Context context,
            Bitmap bitmap,
            ArrayList<ElementWriterWithSpacing> elementWriters
    ) {
        Bitmap result = copy(bitmap);
        requireList(elementWriters);
        for (ElementWriterWithSpacing writer : elementWriters) {
            result = drawWithWidthReduction(
                    context,
                    result,
                    Objects.requireNonNull(
                            writer,
                            "Element writer cannot be null."
                    ),
                    0
            );
        }
        return result;
    }

    /**
     * Returns matching {@code [top, middle, bottom]} bitmaps. The middle section
     * contains the rendered text and every section has the same final width.
     */
    public static Bitmap[] writeTextToBitmapWithSpacingWidthReductionWithLimit(
            Context context,
            int minWidth,
            Bitmap bitmap,
            Bitmap top,
            Bitmap bottom,
            ArrayList<ElementWriterWithSpacing> elementWriters
    ) {
        if (minWidth <= 0) {
            throw new IllegalArgumentException("Minimum width must be greater than zero.");
        }
        requireBitmap(top, "Top bitmap");
        requireBitmap(bottom, "Bottom bitmap");

        Bitmap middle = copy(bitmap);
        requireList(elementWriters);
        for (ElementWriterWithSpacing writer : elementWriters) {
            middle = drawWithWidthReduction(
                    context,
                    middle,
                    Objects.requireNonNull(
                            writer,
                            "Element writer cannot be null."
                    ),
                    minWidth
            );
        }

        int finalWidth = middle.getWidth();
        if (top.getWidth() < finalWidth || bottom.getWidth() < finalWidth) {
            middle.recycle();
            throw new IllegalArgumentException(
                    "Top and bottom bitmaps must be at least " + finalWidth
                            + "px wide."
            );
        }

        return new Bitmap[]{
                shrinkToWidth(top, finalWidth),
                middle,
                shrinkToWidth(bottom, finalWidth)
        };
    }

    private static void draw(
            Context context,
            Bitmap destination,
            ElementWriter writer
    ) {
        requireWriter(
                writer.text,
                writer.lineHeight,
                writer.type,
                writer.alpha
        );
        Bitmap textBitmap = TextMakerEngineNative.generateTextBitmap(
                context,
                writer.fontId,
                writer.text,
                Math.round(writer.lineHeight),
                writer.color
        );
        drawGeneratedText(
                destination,
                textBitmap,
                writer.left,
                writer.top,
                writer.type,
                writer.alpha
        );
    }

    private static void draw(
            Context context,
            Bitmap destination,
            ElementWriterWithSpacing writer
    ) {
        requireWriter(
                writer.text,
                writer.lineHeight,
                writer.type,
                writer.alpha
        );
        Bitmap textBitmap = createSpacedText(context, writer);
        drawGeneratedText(
                destination,
                textBitmap,
                writer.left,
                writer.top,
                writer.type,
                writer.alpha
        );
    }

    private static Bitmap drawWithWidthReduction(
            Context context,
            Bitmap destination,
            ElementWriterWithSpacing writer,
            int minWidth
    ) {
        requireWriter(
                writer.text,
                writer.lineHeight,
                writer.type,
                writer.alpha
        );
        Bitmap textBitmap = createSpacedText(context, writer);
        int targetWidth = Math.max(
                minWidth,
                textBitmap.getWidth() + 2 * textBitmap.getHeight()
        );

        Bitmap resized = shrinkToWidth(
                destination,
                Math.min(destination.getWidth(), targetWidth)
        );
        if (resized != destination) {
            destination.recycle();
            destination = resized;
        }

        drawGeneratedText(
                destination,
                textBitmap,
                writer.left,
                writer.top,
                writer.type,
                writer.alpha
        );
        return destination;
    }

    private static Bitmap createSpacedText(
            Context context,
            ElementWriterWithSpacing writer
    ) {
        return TextMakerEngineNative.generateTextBitmapWithSpacing(
                context,
                writer.fontId,
                writer.text,
                writer.color,
                Math.round(writer.lineHeight),
                writer.spaceInPx
        );
    }

    private static void drawGeneratedText(
            Bitmap destination,
            Bitmap textBitmap,
            float anchorX,
            float top,
            LineType lineType,
            float alpha
    ) {
        float left = anchorX;
        if (lineType == LineType.END) {
            left -= textBitmap.getWidth();
        } else if (lineType == LineType.MIDDLE) {
            left -= textBitmap.getWidth() / 2f;
        }

        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
                        | Paint.FILTER_BITMAP_FLAG
        );
        paint.setAlpha(alphaToByte(alpha));
        new Canvas(destination).drawBitmap(textBitmap, left, top, paint);
        textBitmap.recycle();
    }

    private static Bitmap copy(Bitmap source) {
        requireBitmap(source, "Source bitmap");
        Bitmap result = Bitmap.createBitmap(
                source.getWidth(),
                source.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        new Canvas(result).drawBitmap(source, 0f, 0f, null);
        return result;
    }

    /**
     * Removes pixels from the horizontal middle while retaining both edge caps.
     */
    private static Bitmap shrinkToWidth(Bitmap source, int targetWidth) {
        requireBitmap(source, "Bitmap");
        if (targetWidth <= 0) {
            throw new IllegalArgumentException("Target width must be greater than zero.");
        }
        if (targetWidth >= source.getWidth()) {
            return source;
        }

        int leftWidth = targetWidth / 2;
        int rightWidth = targetWidth - leftWidth;
        int rightSourceStart = source.getWidth() - rightWidth;

        Bitmap result = Bitmap.createBitmap(
                targetWidth,
                source.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(
                source,
                new Rect(0, 0, leftWidth, source.getHeight()),
                new Rect(0, 0, leftWidth, result.getHeight()),
                null
        );
        canvas.drawBitmap(
                source,
                new Rect(
                        rightSourceStart,
                        0,
                        rightSourceStart + rightWidth,
                        source.getHeight()
                ),
                new Rect(
                        leftWidth,
                        0,
                        leftWidth + rightWidth,
                        result.getHeight()
                ),
                null
        );
        return result;
    }

    private static int alphaToByte(float alpha) {
        return Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
    }

    private static void requireWriter(
            String text,
            float lineHeight,
            LineType lineType,
            float alpha
    ) {
        Objects.requireNonNull(text, "Text cannot be null.");
        Objects.requireNonNull(lineType, "Line type cannot be null.");
        if (lineHeight <= 0f) {
            throw new IllegalArgumentException("Line height must be greater than zero.");
        }
        if (!Float.isFinite(alpha)) {
            throw new IllegalArgumentException("Alpha must be a finite value.");
        }
    }

    private static void requireBitmap(Bitmap bitmap, String label) {
        Objects.requireNonNull(bitmap, label + " cannot be null.");
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException(label + " has been recycled.");
        }
        if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            throw new IllegalArgumentException(label + " has invalid dimensions.");
        }
    }

    private static void requireMutableBitmap(Bitmap bitmap, String label) {
        requireBitmap(bitmap, label);
        if (!bitmap.isMutable()) {
            throw new IllegalArgumentException(label + " must be mutable.");
        }
    }

    private static void requireList(ArrayList<?> writers) {
        Objects.requireNonNull(writers, "Element writers cannot be null.");
    }

    public static class ElementWriter {
        public int fontId;
        public String text;
        public float left;
        public float top;
        public float lineHeight;
        public LineType type;
        public int color = Color.parseColor("#4A4A4A");
        public float alpha;

        public ElementWriter(
                int fontId,
                String text,
                float left,
                float top,
                float lineHeight,
                LineType type,
                float alpha
        ) {
            this.fontId = fontId;
            this.text = text;
            this.left = left;
            this.top = top;
            this.lineHeight = lineHeight;
            this.type = type;
            this.alpha = alpha;
        }

        public ElementWriter(
                int fontId,
                String text,
                float left,
                float top,
                float lineHeight,
                LineType type,
                float alpha,
                String colorHex
        ) {
            this(fontId, text, left, top, lineHeight, type, alpha);
            color = parseColor(colorHex);
        }
    }

    public static class ElementWriterWithSpacing {
        public int fontId;
        public String text;
        public float left;
        public float top;
        public float lineHeight;
        public LineType type;
        public float alpha;
        public int color = Color.parseColor("#4A4A4A");
        public int spaceInPx;

        public ElementWriterWithSpacing(
                int fontId,
                String text,
                float left,
                float top,
                float lineHeight,
                LineType type,
                float alpha,
                int spaceInPx
        ) {
            this.fontId = fontId;
            this.text = text;
            this.left = left;
            this.top = top;
            this.lineHeight = lineHeight;
            this.type = type;
            this.alpha = alpha;
            this.spaceInPx = spaceInPx;
        }

        public ElementWriterWithSpacing(
                int fontId,
                String text,
                float left,
                float top,
                float lineHeight,
                LineType type,
                float alpha,
                int spaceInPx,
                String colorHex
        ) {
            this(
                    fontId,
                    text,
                    left,
                    top,
                    lineHeight,
                    type,
                    alpha,
                    spaceInPx
            );
            color = parseColor(colorHex);
        }
    }

    private static int parseColor(String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) {
            throw new IllegalArgumentException("Color cannot be null or empty.");
        }
        try {
            return Color.parseColor(colorHex);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid color value: " + colorHex,
                    exception
            );
        }
    }

    public enum LineType {
        START,
        MIDDLE,
        END
    }
}

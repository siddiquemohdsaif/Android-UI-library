package com.ogfa.nativeviews.text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Renders ordinary Android font resources into bitmaps.
 *
 * <p>The historical "Native" suffix distinguishes this renderer from
 * {@link TextMakerEngine}, which requires a bitmap for each character. It does not
 * use JNI or native code.</p>
 */
public final class TextMakerEngineNative {

    private TextMakerEngineNative() {
    }

    public static Bitmap generateTextBitmap(
            Context context,
            int fontId,
            String text,
            int lineHeightPx,
            int color
    ) {
        return generate(
                requireTypeface(context, fontId),
                text,
                lineHeightPx,
                color,
                0
        );
    }

    public static Bitmap generateTextBitmapWithSpacing(
            Context context,
            int fontId,
            String text,
            int color,
            int lineHeightPx,
            int spacingInPx
    ) {
        if (spacingInPx < 0) {
            throw new IllegalArgumentException("Character spacing cannot be negative.");
        }
        return generate(
                requireTypeface(context, fontId),
                text,
                lineHeightPx,
                color,
                spacingInPx
        );
    }

    public static ArrayList<String> textLineEvaluator(
            Context context,
            int fontId,
            String text,
            int lineHeightPx,
            int spacingInPx,
            int lineLengthInPx
    ) {
        Objects.requireNonNull(text, "Text cannot be null.");
        if (lineHeightPx <= 0 || lineLengthInPx <= 0 || spacingInPx < 0) {
            throw new IllegalArgumentException(
                    "Line height/length must be positive and spacing cannot be negative."
            );
        }

        Paint paint = createPaint(
                requireTypeface(context, fontId),
                lineHeightPx,
                0xff000000
        );
        ArrayList<String> lines = new ArrayList<>();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        for (String word : trimmed.split("\\s+")) {
            String candidate = currentLine.length() == 0
                    ? word
                    : currentLine + " " + word;
            if (measureText(paint, candidate, spacingInPx) <= lineLengthInPx) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }

            appendLongWord(
                    lines,
                    currentLine,
                    word,
                    paint,
                    spacingInPx,
                    lineLengthInPx
            );
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private static Bitmap generate(
            Typeface typeface,
            String text,
            int lineHeightPx,
            int color,
            int spacingInPx
    ) {
        Objects.requireNonNull(text, "Text cannot be null.");
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty.");
        }
        if (lineHeightPx <= 0) {
            throw new IllegalArgumentException("Line height must be greater than zero.");
        }

        Paint paint = createPaint(typeface, lineHeightPx, color);
        int width = Math.max(1, Math.round(measureText(paint, text, spacingInPx)));
        Bitmap output = Bitmap.createBitmap(
                width,
                lineHeightPx,
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(output);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = (lineHeightPx - (metrics.descent - metrics.ascent)) / 2f
                - metrics.ascent;

        float x = 0f;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            canvas.drawText(character, x, baseline, paint);
            x += paint.measureText(character) + spacingInPx;
            offset += Character.charCount(codePoint);
        }
        return output;
    }

    private static Paint createPaint(
            Typeface typeface,
            int lineHeightPx,
            int color
    ) {
        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
                        | Paint.SUBPIXEL_TEXT_FLAG
        );
        paint.setTypeface(typeface);
        paint.setColor(color);

        // Scale once from font metrics instead of incrementing text size one pixel
        // at a time as the original implementation did.
        paint.setTextSize(lineHeightPx);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float measuredHeight = metrics.descent - metrics.ascent;
        if (measuredHeight <= 0f) {
            throw new IllegalStateException("Font has invalid vertical metrics.");
        }
        paint.setTextSize(lineHeightPx * lineHeightPx / measuredHeight);
        return paint;
    }

    private static float measureText(
            Paint paint,
            String text,
            int spacingInPx
    ) {
        float width = 0f;
        int characterCount = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            width += paint.measureText(new String(Character.toChars(codePoint)));
            characterCount++;
            offset += Character.charCount(codePoint);
        }
        return width + Math.max(0, characterCount - 1) * spacingInPx;
    }

    private static void appendLongWord(
            ArrayList<String> lines,
            StringBuilder currentLine,
            String word,
            Paint paint,
            int spacingInPx,
            int lineLengthInPx
    ) {
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            String candidate = currentLine + character;
            if (currentLine.length() > 0
                    && measureText(paint, candidate, spacingInPx)
                    > lineLengthInPx) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }
            currentLine.append(character);
            offset += Character.charCount(codePoint);
        }
    }

    private static Typeface requireTypeface(Context context, int fontId) {
        Objects.requireNonNull(context, "Context cannot be null.");
        if (fontId == 0) {
            throw new IllegalArgumentException("Font resource ID cannot be zero.");
        }
        try {
            Typeface typeface = ResourcesCompat.getFont(context, fontId);
            if (typeface == null) {
                throw new IllegalArgumentException(
                        "Unable to load font resource ID: " + fontId
                );
            }
            return typeface;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Unable to load font resource ID: " + fontId,
                    exception
            );
        }
    }
}

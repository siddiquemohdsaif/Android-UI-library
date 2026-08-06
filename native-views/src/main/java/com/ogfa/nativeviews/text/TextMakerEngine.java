package com.ogfa.nativeviews.text;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a text bitmap from a map containing one bitmap per character.
 */
public final class TextMakerEngine {

    private TextMakerEngine() {
    }

    public static Bitmap generateTextBitmap(
            Map<String, Bitmap> textMap,
            String text,
            int lineHeightPx
    ) {
        return generate(textMap, text, lineHeightPx, 0);
    }

    public static Bitmap generateTextBitmapWithSpacing(
            Map<String, Bitmap> textMap,
            String text,
            int lineHeightPx,
            int spacingInPx
    ) {
        if (spacingInPx < 0) {
            throw new IllegalArgumentException("Character spacing cannot be negative.");
        }
        return generate(textMap, text, lineHeightPx, spacingInPx);
    }

    private static Bitmap generate(
            Map<String, Bitmap> textMap,
            String text,
            int lineHeightPx,
            int spacingInPx
    ) {
        Objects.requireNonNull(textMap, "Text bitmap map cannot be null.");
        Objects.requireNonNull(text, "Text cannot be null.");
        if (lineHeightPx <= 0) {
            throw new IllegalArgumentException("Line height must be greater than zero.");
        }

        int sourceWidth = 0;
        int sourceHeight = 0;
        int mappedCharacters = 0;
        for (int i = 0; i < text.length(); i++) {
            Bitmap character = textMap.get(String.valueOf(text.charAt(i)));
            if (character == null) {
                continue;
            }
            requireUsableCharacter(character, text.charAt(i));
            sourceWidth += character.getWidth();
            sourceHeight = Math.max(sourceHeight, character.getHeight());
            mappedCharacters++;
        }

        if (mappedCharacters == 0) {
            throw new IllegalArgumentException(
                    "Text contains no characters present in the bitmap map: " + text
            );
        }

        sourceWidth += spacingInPx * Math.max(0, mappedCharacters - 1);
        float scale = (float) lineHeightPx / sourceHeight;
        int outputWidth = Math.max(1, Math.round(sourceWidth * scale));
        Bitmap output = Bitmap.createBitmap(
                outputWidth,
                lineHeightPx,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG
                | Paint.FILTER_BITMAP_FLAG);
        Rect source = new Rect();
        RectF destination = new RectF();
        int currentSourceX = 0;

        for (int i = 0; i < text.length(); i++) {
            Bitmap character = textMap.get(String.valueOf(text.charAt(i)));
            if (character == null) {
                continue;
            }
            source.set(0, 0, character.getWidth(), character.getHeight());
            destination.set(
                    currentSourceX * scale,
                    0f,
                    (currentSourceX + character.getWidth()) * scale,
                    lineHeightPx
            );
            canvas.drawBitmap(character, source, destination, paint);
            currentSourceX += character.getWidth() + spacingInPx;
        }
        return output;
    }

    public static ArrayList<String> textLineEvaluator(
            Map<String, Bitmap> textMap,
            String text,
            int lineHeightPx,
            int spacingInPx,
            int lineLengthInPixel
    ) {
        Objects.requireNonNull(textMap, "Text bitmap map cannot be null.");
        Objects.requireNonNull(text, "Text cannot be null.");
        if (lineHeightPx <= 0 || lineLengthInPixel <= 0 || spacingInPx < 0) {
            throw new IllegalArgumentException(
                    "Line height/length must be positive and spacing cannot be negative."
            );
        }

        ArrayList<String> lines = new ArrayList<>();
        String[] words = text.trim().isEmpty()
                ? new String[0]
                : text.trim().split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        float currentLineWidth = 0f;

        TextPaint fallbackPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        fallbackPaint.setTextSize(lineHeightPx);
        fallbackPaint.setColor(Color.BLACK);
        float spaceWidth = fallbackPaint.measureText(" ");

        for (String word : words) {
            float wordWidth = measureWord(
                    textMap,
                    word,
                    lineHeightPx,
                    spacingInPx,
                    fallbackPaint
            );
            float requiredWidth = wordWidth
                    + (currentLine.length() == 0 ? 0f : spaceWidth);

            if (currentLine.length() > 0
                    && currentLineWidth + requiredWidth > lineLengthInPixel) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLineWidth = 0f;
                requiredWidth = wordWidth;
            }

            if (currentLine.length() > 0) {
                currentLine.append(' ');
            }
            currentLine.append(word);
            currentLineWidth += requiredWidth;
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private static float measureWord(
            Map<String, Bitmap> textMap,
            String word,
            int lineHeightPx,
            int spacingInPx,
            TextPaint fallbackPaint
    ) {
        float width = 0f;
        for (int i = 0; i < word.length(); i++) {
            Bitmap character = textMap.get(String.valueOf(word.charAt(i)));
            if (character == null) {
                width += fallbackPaint.measureText(
                        String.valueOf(word.charAt(i))
                );
            } else {
                requireUsableCharacter(character, word.charAt(i));
                width += character.getWidth()
                        * ((float) lineHeightPx / character.getHeight());
            }
            if (i < word.length() - 1) {
                width += spacingInPx;
            }
        }
        return width;
    }

    private static void requireUsableCharacter(Bitmap bitmap, char character) {
        if (bitmap.isRecycled()
                || bitmap.getWidth() <= 0
                || bitmap.getHeight() <= 0) {
            throw new IllegalArgumentException(
                    "Bitmap for character '" + character + "' is unusable."
            );
        }
    }
}

package com.ogfa.nativeviews.textfield;

import java.text.BreakIterator;
import java.util.Locale;

/** Locale-aware text boundaries shared by touch selection and keyboard navigation. */
final class TextBoundaryHelper {
    private TextBoundaryHelper() {}

    static int previousGrapheme(CharSequence text, int offset) {
        String value = text == null ? "" : text.toString();
        int target = Math.max(0, Math.min(offset, value.length()));
        int previous = 0;
        int current = 0;
        while (current < target) {
            previous = current;
            current = nextGrapheme(value, current);
        }
        return previous;
    }

    static int nextGrapheme(CharSequence text, int offset) {
        String value = text == null ? "" : text.toString();
        int length = value.length();
        int current = Math.max(0, Math.min(offset, length));
        if (current >= length) return length;

        int first = value.codePointAt(current);
        current += Character.charCount(first);
        if (isRegionalIndicator(first) && current < length) {
            int next = value.codePointAt(current);
            if (isRegionalIndicator(next)) current += Character.charCount(next);
        }

        while (current < length) {
            int next = value.codePointAt(current);
            if (isGraphemeExtender(next)) {
                current += Character.charCount(next);
                continue;
            }
            if (next == 0x200D) {
                current += Character.charCount(next);
                if (current < length) {
                    int joined = value.codePointAt(current);
                    current += Character.charCount(joined);
                }
                continue;
            }
            break;
        }
        return current;
    }

    static int[] wordAt(CharSequence text, int offset) {
        String value = text == null ? "" : text.toString();
        if (value.isEmpty()) return new int[]{0, 0};

        int probe = Math.max(0, Math.min(offset, value.length()));
        if (probe == value.length()) probe = previousGrapheme(value, probe);

        BreakIterator words = BreakIterator.getWordInstance(Locale.getDefault());
        words.setText(value);
        int start = words.preceding(Math.min(value.length(), probe + 1));
        int end = words.following(probe);
        if (start == BreakIterator.DONE) start = 0;
        if (end == BreakIterator.DONE) end = value.length();

        // Whitespace is not a useful selection target. Prefer the word immediately
        // before it, then the word after it, matching typical editor behavior.
        if (!containsWordCharacter(value, start, end)) {
            int previous = words.preceding(start);
            if (previous != BreakIterator.DONE && containsWordCharacter(value, previous, start)) {
                return new int[]{previous, start};
            }
            int following = words.following(end);
            if (following != BreakIterator.DONE && containsWordCharacter(value, end, following)) {
                return new int[]{end, following};
            }
        }
        return new int[]{start, end};
    }

    private static boolean isGraphemeExtender(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || codePoint >= 0xFE00 && codePoint <= 0xFE0F
                || codePoint >= 0xE0100 && codePoint <= 0xE01EF
                || codePoint >= 0x1F3FB && codePoint <= 0x1F3FF;
    }

    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    private static boolean containsWordCharacter(String value, int start, int end) {
        for (int offset = Math.max(0, start); offset < Math.min(value.length(), end); ) {
            int codePoint = value.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) return true;
            offset += Character.charCount(codePoint);
        }
        return false;
    }
}

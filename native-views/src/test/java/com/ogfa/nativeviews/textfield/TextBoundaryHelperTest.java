package com.ogfa.nativeviews.textfield;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TextBoundaryHelperTest {
    @Test public void selectsWordsAroundWhitespace() {
        assertArrayEquals(new int[]{0, 5}, TextBoundaryHelper.wordAt("hello world", 1));
        assertArrayEquals(new int[]{0, 5}, TextBoundaryHelper.wordAt("hello world", 5));
        assertArrayEquals(new int[]{6, 11}, TextBoundaryHelper.wordAt("hello world", 8));
    }

    @Test public void graphemeNavigationDoesNotSplitSurrogatePairs() {
        String value = "A\uD83D\uDE00B";
        assertEquals(3, TextBoundaryHelper.nextGrapheme(value, 1));
        assertEquals(1, TextBoundaryHelper.previousGrapheme(value, 3));
    }

    @Test public void graphemeNavigationKeepsCombiningAndJoinedEmojiTogether() {
        assertEquals(2, TextBoundaryHelper.nextGrapheme("e\u0301x", 0));
        String family = "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67";
        assertEquals(family.length(), TextBoundaryHelper.nextGrapheme(family, 0));
        assertEquals(0, TextBoundaryHelper.previousGrapheme(family, family.length()));
    }
}

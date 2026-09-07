package com.ogfa.nativeviews.zlayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ImeTranslationProfileTest {
    @Test
    public void resizedMiuiHostRemovesDuplicateKeyboardDistance() {
        assertEquals(-130f, ImeTranslationProfile.normalize(
                -1030f, 900, 440, true), 0f);
    }

    @Test
    public void fullHeightMotorolaHostKeepsRequestedTranslation() {
        assertEquals(-130f, ImeTranslationProfile.normalize(
                -130f, 0, 440, true), 0f);
    }

    @Test
    public void navigationBarGapIsNotTreatedAsKeyboardResize() {
        assertEquals(-130f, ImeTranslationProfile.normalize(
                -130f, 130, 440, true), 0f);
    }

    @Test
    public void translationIsNeverChangedWithoutFocusedTextField() {
        assertEquals(-1030f, ImeTranslationProfile.normalize(
                -1030f, 900, 440, false), 0f);
    }

    @Test
    public void correctionNeverMovesLayerDownward() {
        assertEquals(0f, ImeTranslationProfile.normalize(
                -100f, 900, 440, true), 0f);
    }
}

package com.ogfa.nativeviews.zlayer;

/** Internal decision model for avoiding duplicate IME translation after adjustResize. */
final class ImeTranslationProfile {
    private ImeTranslationProfile() {
    }

    static float normalize(float requestedTranslationY, int obscuredBelowHost,
                           int keyboardThreshold, boolean hasFocusedTextField) {
        if (!hasFocusedTextField || requestedTranslationY >= 0f
                || obscuredBelowHost < keyboardThreshold) {
            return requestedTranslationY;
        }
        // The caller already subtracted the IME inset. If adjustResize also
        // shortened the host, add that duplicated portion back while never
        // turning an upward translation into a downward translation.
        return Math.min(0f, requestedTranslationY + obscuredBelowHost);
    }
}

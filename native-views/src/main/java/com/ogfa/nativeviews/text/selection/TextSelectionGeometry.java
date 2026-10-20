package com.ogfa.nativeviews.text.selection;

/** The text and touch coordinates needed by shared selection gestures. */
public interface TextSelectionGeometry {
    CharSequence selectionText();

    int selectionOffsetAt(float x, float y);

    void applySelection(int anchor, int extent);
}

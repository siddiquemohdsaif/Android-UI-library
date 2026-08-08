package com.ogfa.nativeviews.radiobutton;

/** Receives one callback for each completed RadioSelection transaction. */
@FunctionalInterface
public interface OnSelectionChangedListener {
    void onSelectionChanged(
            String selectionId,
            String oldSelectedId,
            String newSelectedId,
            boolean fromUser
    );
}

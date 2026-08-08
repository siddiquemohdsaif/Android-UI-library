package com.ogfa.nativeviews.radiobutton;

/** Receives completed RadioButton clicks, including clicks on an already-selected item. */
@FunctionalInterface
public interface OnRadioClickListener {
    void onClick(String id);
}

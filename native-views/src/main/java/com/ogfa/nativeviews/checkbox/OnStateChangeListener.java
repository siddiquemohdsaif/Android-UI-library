package com.ogfa.nativeviews.checkbox;

/** Receives complete CheckBox state changes, including indeterminate. */
@FunctionalInterface
public interface OnStateChangeListener {
    void onStateChanged(String id, CheckBox.State state, boolean fromUser);
}

package com.ogfa.nativeviews.selection;

/** Receives real checked-state changes from selectable Canvas components. */
@FunctionalInterface
public interface OnCheckedChangeListener {
    void onCheckedChanged(String id, boolean checked, boolean fromUser);
}

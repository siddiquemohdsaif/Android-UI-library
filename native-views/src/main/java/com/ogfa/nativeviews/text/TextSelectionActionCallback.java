package com.ogfa.nativeviews.text;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

/** Customizes the read-only selection toolbar of a {@link Text}. */
public interface TextSelectionActionCallback {
    default void onCreateActionMode(Text text, ActionMode mode, Menu menu) {}

    default void onPrepareActionMode(Text text, ActionMode mode, Menu menu) {}

    default boolean onActionItemClicked(Text text, ActionMode mode, MenuItem item) {
        return false;
    }
}

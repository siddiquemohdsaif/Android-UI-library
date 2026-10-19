package com.ogfa.nativeviews.textfield;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

/** Allows an application to adjust or handle the contextual text-selection menu. */
public interface TextSelectionActionCallback {
    default void onCreateActionMode(TextField field, ActionMode mode, Menu menu) {}

    default void onPrepareActionMode(TextField field, ActionMode mode, Menu menu) {}

    default boolean onActionItemClicked(TextField field, ActionMode mode, MenuItem item) {
        return false;
    }
}

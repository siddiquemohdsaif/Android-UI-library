package com.ogfa.nativeviews.textfield;

import com.ogfa.nativeviews.component.ComponentHost;

public interface TextFieldHost extends ComponentHost {
   boolean requestFocus(TextField var1);

   void clearFocus(TextField var1);

   void restartInput();

   void updateSelection(TextField var1);

   default void showSelectionActionMode(TextField field) {
   }

   default void showInsertionActionMode(TextField field) {
   }
}

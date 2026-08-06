package com.ogfa.nativeviews.textfield;

import com.ogfa.nativeviews.component.ComponentHost;

public interface TextFieldHost extends ComponentHost {
    boolean requestFocus(TextField field);
    void clearFocus(TextField field);
    void restartInput();
    void updateSelection(TextField field);
}

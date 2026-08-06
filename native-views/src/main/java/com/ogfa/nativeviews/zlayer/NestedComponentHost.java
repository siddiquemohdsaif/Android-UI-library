package com.ogfa.nativeviews.zlayer;

import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.textfield.TextFieldHost;

/**
 * Root services delegated through composite components.
 */
public interface NestedComponentHost extends TextFieldHost {
    void registerNestedComponent(Component component);
    void unregisterNestedComponent(Component component);
}

package com.ogfa.nativeviews.component;

import android.view.View;

public interface ComponentFactory<T extends Component> {
    T build(View hostView);
}

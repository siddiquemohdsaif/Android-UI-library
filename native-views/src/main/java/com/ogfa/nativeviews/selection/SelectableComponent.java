package com.ogfa.nativeviews.selection;

import com.ogfa.nativeviews.component.Component;

/** Shared state contract for Switch, CheckBox, and RadioButton. */
public interface SelectableComponent extends Component {
    boolean isChecked();
    SelectableComponent setChecked(boolean checked);
    SelectableComponent setCheckedImmediately(boolean checked);
    SelectableComponent toggle();
    SelectableComponent toggleImmediately();
    SelectableComponent setOnCheckedChangeListener(OnCheckedChangeListener listener);
    SelectableComponent removeOnCheckedChangeListener();
}

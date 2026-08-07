package com.ogfa.nativeviews.zlayer;

import android.view.View;

import com.ogfa.nativeviews.component.Component;

/**
 * Internal ownership contract that allows ZLayer to be used by a root
 * ZLayerGroup or by a composite component such as Card.
 */
public interface ZLayerOwner {
    View getHostView();
    void registerLayerComponent(Component component);
    void unregisterLayerComponent(Component component);
    void invalidateLayer();

    default boolean ownsLayerTranslation() {
        return false;
    }

    default void setOwnedLayerTranslation(float x, float y) {
        throw new UnsupportedOperationException(
                "This layer owner does not own translation."
        );
    }

    default float getOwnedLayerTranslationX() {
        return 0f;
    }

    default float getOwnedLayerTranslationY() {
        return 0f;
    }
}

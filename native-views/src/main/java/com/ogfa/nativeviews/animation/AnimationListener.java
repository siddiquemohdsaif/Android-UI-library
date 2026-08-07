package com.ogfa.nativeviews.animation;

public interface AnimationListener {
    default void onReady(String id) {}
    default void onStarted(String id) {}
    default void onProgress(String id, float progress) {}
    default void onRepeated(String id, int completedRepeats) {}
    default void onPaused(String id) {}
    default void onCompleted(String id) {}
    default void onError(String id, RuntimeException error) {}
}

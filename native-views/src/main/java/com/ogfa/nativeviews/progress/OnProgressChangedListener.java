package com.ogfa.nativeviews.progress;

@FunctionalInterface
public interface OnProgressChangedListener {
    void onProgressChanged(String id, float progress);
}

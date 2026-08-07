package com.ogfa.nativeviews.component;

/** Implemented by visible components that can consume system Back navigation. */
public interface BackHandler {
    boolean onBackPressed();
}

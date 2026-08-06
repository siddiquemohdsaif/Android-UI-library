package com.ogfa.nativeviews.text.internal;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide cache for immutable Typeface instances.
 */
public final class TypefaceCache {

    private static final Map<String, Typeface> CACHE =
            new ConcurrentHashMap<>();

    private TypefaceCache() {
    }

    public static Typeface fromResource(Context context, int resourceId) {
        String key = "resource:"
                + context.getPackageName()
                + ':'
                + resourceId;
        Typeface cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Typeface loaded = ResourcesCompat.getFont(context, resourceId);
            if (loaded == null) {
                throw new IllegalArgumentException(
                        "Font resource could not be loaded: " + resourceId
                );
            }
            Typeface previous = CACHE.putIfAbsent(key, loaded);
            return previous != null ? previous : loaded;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid or missing font resource: " + resourceId,
                    exception
            );
        }
    }

    public static Typeface fromAsset(Context context, String assetPath) {
        String key = "asset:"
                + context.getPackageName()
                + ':'
                + assetPath;
        Typeface cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Typeface loaded = Typeface.createFromAsset(
                    context.getAssets(),
                    assetPath
            );
            Typeface previous = CACHE.putIfAbsent(key, loaded);
            return previous != null ? previous : loaded;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid or missing font asset: " + assetPath,
                    exception
            );
        }
    }
}

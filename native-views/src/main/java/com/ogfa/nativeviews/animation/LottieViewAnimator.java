package com.ogfa.nativeviews.animation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;
import android.view.View;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.LottieImageAsset;
import com.airbnb.lottie.LottieResult;
import com.ogfa.nativeviews.internal.util.BackgroundRunner;
import com.ogfa.nativeviews.internal.util.LogManager.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

final class LottieViewAnimator {

    private static final String TAG = "LottieViewAnimator";
    private static final String JSON_ASSET_DIRECTORY = "lottie/json/";

    public final ArrayList<Animation> animations = new ArrayList<>();

    private static final ConcurrentHashMap<String, LottieComposition>
            preloadedAnimations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FutureTask<LottieComposition>>
            loadingAnimations = new ConcurrentHashMap<>();

    /**
     * Preloads one Lottie JSON file from assets/lottie/json.
     */
    public static void preloadAnimations(Context context, String animationName) {
        preloadAnimations(context, new String[]{animationName});
    }

    /**
     * Preloads multiple Lottie JSON files from assets/lottie/json.
     */
    public static void preloadAnimations(Context context, String... animationNames) {
        requireContext(context);
        if (animationNames == null || animationNames.length == 0) {
            throw new IllegalArgumentException(
                    "At least one Lottie animation name is required."
            );
        }

        // Validate the complete request before starting asynchronous work.
        for (String animationName : animationNames) {
            requireAssetExists(context, assetPath(animationName), animationName);
        }
        for (String animationName : animationNames) {
            preloadAnimation(context, animationName);
        }
    }

    public static boolean isLoaded(String animationName) {
        return preloadedAnimations.containsKey(normalizeId(animationName));
    }

    public static void clearCache(String animationName) {
        preloadedAnimations.remove(normalizeId(animationName));
    }

    public static void clearCache() {
        preloadedAnimations.clear();
    }

    /**
     * Compatibility wrapper for callers that explicitly requested synchronous loading.
     */
    public static void preloadAnimationSync(String animationName, Context context) {
        getOrLoad(context, animationName);
    }

    /**
     * Returns a cached composition, waits for its in-flight preload, or loads it from
     * assets/lottie/json on the calling thread.
     */
    public static LottieComposition getOrLoad(
            Context context,
            String animationName
    ) {
        requireContext(context);

        String id = normalizeId(animationName);
        LottieComposition cached = preloadedAnimations.get(id);
        if (cached != null) {
            return cached;
        }

        String path = assetPath(animationName);
        FutureTask<LottieComposition> newTask =
                createLoadTask(context, animationName, path);
        FutureTask<LottieComposition> loadTask =
                loadingAnimations.putIfAbsent(id, newTask);
        boolean ownsLoadTask = loadTask == null;

        if (ownsLoadTask) {
            loadTask = newTask;
            loadTask.run();
        }

        try {
            LottieComposition loaded =
                    awaitLoad(loadTask, animationName, path);
            LottieComposition existing =
                    preloadedAnimations.putIfAbsent(id, loaded);
            return existing == null ? loaded : existing;
        } finally {
            if (ownsLoadTask) {
                loadingAnimations.remove(id, loadTask);
            }
        }
    }

    public static Rect getCompositionBounds(Context context, String animationName) {
        LottieComposition composition = getOrLoad(context, animationName);
        Rect bounds = composition.getBounds();
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
            throw new IllegalStateException(
                    "Lottie animation '" + animationName
                            + "' has invalid composition bounds."
            );
        }
        return new Rect(bounds);
    }

    private static void preloadAnimation(Context context, String animationName) {
        String id = normalizeId(animationName);
        if (preloadedAnimations.containsKey(id)) {
            return;
        }

        String path = assetPath(animationName);
        FutureTask<LottieComposition> loadTask =
                createLoadTask(context, animationName, path);
        if (loadingAnimations.putIfAbsent(id, loadTask) != null) {
            return;
        }

        BackgroundRunner.run(() -> {
            loadTask.run();
            try {
                LottieComposition loaded =
                        awaitLoad(loadTask, animationName, path);
                preloadedAnimations.putIfAbsent(id, loaded);
            } catch (IllegalStateException exception) {
                Log.e(TAG, exception.getMessage(), exception);
            } finally {
                loadingAnimations.remove(id, loadTask);
            }
        });
    }

    private static FutureTask<LottieComposition> createLoadTask(
            Context context,
            String animationName,
            String path
    ) {
        Context applicationContext = context.getApplicationContext();
        Context assetContext =
                applicationContext == null ? context : applicationContext;
        return new FutureTask<>(
                () -> loadCompositionSync(assetContext, animationName, path)
        );
    }

    private static LottieComposition loadCompositionSync(
            Context context,
            String animationName,
            String path
    ) {
        LottieResult<LottieComposition> result =
                LottieCompositionFactory.fromAssetSync(context, path);
        LottieComposition composition = result.getValue();
        if (composition != null) {
            return composition;
        }

        Throwable exception = result.getException();
        throw new IllegalStateException(
                "Lottie animation '" + animationName
                        + "' could not be parsed from assets/" + path + ".",
                exception
        );
    }

    private static LottieComposition awaitLoad(
            FutureTask<LottieComposition> loadTask,
            String animationName,
            String path
    ) {
        try {
            return loadTask.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while loading Lottie animation '"
                            + animationName + "'.",
                    exception
            );
        } catch (ExecutionException | RuntimeException exception) {
            Throwable cause = exception instanceof ExecutionException
                    ? exception.getCause()
                    : exception;
            throw new IllegalStateException(
                    "Lottie animation '" + animationName
                            + "' was not preloaded and could not be loaded from assets/"
                            + path + ".",
                    cause
            );
        }
    }

    private static void requireContext(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
    }

    private static String normalizeId(String animationName) {
        if (animationName == null) {
            throw new IllegalArgumentException(
                    "Lottie animation name cannot be null."
            );
        }

        String normalized = animationName.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith(JSON_ASSET_DIRECTORY)) {
            normalized = normalized.substring(JSON_ASSET_DIRECTORY.length());
        }
        if (normalized.toLowerCase(Locale.ROOT).endsWith(".json")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Lottie animation name cannot be empty."
            );
        }
        return normalized;
    }

    private static String assetPath(String animationName) {
        return JSON_ASSET_DIRECTORY + normalizeId(animationName) + ".json";
    }

    private static void requireAssetExists(
            Context context,
            String path,
            String animationName
    ) {
        try (InputStream ignored = context.getAssets().open(path)) {
            // Opening is enough to validate the asset before async preload begins.
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Lottie animation '" + animationName
                            + "' was not found at assets/" + path + ".",
                    exception
            );
        }
    }

    public void addAnimation(
            Context context,
            String id,
            int width,
            int height,
            int x,
            int y,
            int repeatCount
    ) {
        addAnimation(
                context,
                id,
                width,
                height,
                x,
                y,
                repeatCount,
                0f,
                null
        );
    }

    public void addAnimation(
            Context context,
            String id,
            int width,
            int height,
            int x,
            int y,
            int repeatCount,
            float angle
    ) {
        addAnimation(
                context,
                id,
                width,
                height,
                x,
                y,
                repeatCount,
                angle,
                null
        );
    }

    public void addAnimation(
            Context context,
            String id,
            int width,
            int height,
            int x,
            int y,
            int repeatCount,
            Callback callback
    ) {
        addAnimation(
                context,
                id,
                width,
                height,
                x,
                y,
                repeatCount,
                0f,
                callback
        );
    }

    public void addAnimation(
            Context context,
            String id,
            int width,
            int height,
            int x,
            int y,
            int repeatCount,
            float angle,
            Callback callback
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Lottie animation width and height must be greater than zero."
            );
        }

        String normalizedId = normalizeId(id);
        LottieComposition composition = getOrLoad(context, id);
        LottieDrawable drawable = createDrawable(
                context,
                composition,
                repeatCount
        );
        animations.add(
                new Animation(
                        normalizedId,
                        drawable,
                        width,
                        height,
                        x,
                        y,
                        angle,
                        callback
                )
        );
    }

    private static LottieDrawable createDrawable(
            Context context,
            LottieComposition composition,
            int repeatCount
    ) {
        Context applicationContext = context.getApplicationContext();
        Context assetContext =
                applicationContext == null ? context : applicationContext;

        LottieDrawable drawable = new LottieDrawable();
        drawable.setImageAssetDelegate(
                asset -> resolveImageAsset(assetContext, asset)
        );
        drawable.setComposition(composition);
        drawable.setRepeatCount(repeatCount);
        drawable.playAnimation();
        return drawable;
    }

    public static LottieDrawable createDrawableForComponent(
            Context context,
            LottieComposition composition
    ) {
        LottieDrawable drawable = createDrawable(context, composition, 0);
        drawable.stop();
        drawable.setProgress(0f);
        return drawable;
    }

    private static Bitmap resolveImageAsset(
            Context context,
            LottieImageAsset asset
    ) {
        String fileName = asset.getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Lottie image asset has no file name."
            );
        }

        String value = fileName.trim();
        if (value.startsWith("data:")) {
            return decodeBase64Image(value);
        }

        Set<String> candidates = new LinkedHashSet<>();
        String directory = asset.getDirName();
        if (directory != null && !directory.trim().isEmpty()) {
            addImageAssetCandidate(candidates, directory + "/" + value);
        }
        addImageAssetCandidate(candidates, value);
        addImageAssetCandidate(candidates, "images/" + value);
        addImageAssetCandidate(candidates, "lottie/images/" + value);

        IOException lastException = null;
        for (String candidate : candidates) {
            try (InputStream inputStream =
                         context.getAssets().open(candidate)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    return bitmap;
                }
            } catch (IOException exception) {
                lastException = exception;
            }
        }

        throw new IllegalStateException(
                "Unable to resolve Lottie image asset '" + fileName
                        + "'. Tried: " + candidates,
                lastException
        );
    }

    private static void addImageAssetCandidate(
            Set<String> candidates,
            String path
    ) {
        String normalized = path.trim().replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith("assets/")) {
            normalized = normalized.substring(7);
        }
        if (!normalized.isEmpty()) {
            candidates.add(normalized);
        }
    }

    private static Bitmap decodeBase64Image(String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0 || commaIndex == dataUrl.length() - 1) {
            throw new IllegalStateException(
                    "Invalid Base64 Lottie image data URL."
            );
        }

        try {
            byte[] decoded = Base64.decode(
                    dataUrl.substring(commaIndex + 1),
                    Base64.DEFAULT
            );
            Bitmap bitmap = BitmapFactory.decodeByteArray(
                    decoded,
                    0,
                    decoded.length
            );
            if (bitmap == null) {
                throw new IllegalStateException(
                        "Unable to decode Base64 Lottie image."
                );
            }
            return bitmap;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Invalid Base64 Lottie image.",
                    exception
            );
        }
    }

    public static boolean isLottieAnimationExists(
            String animationName,
            Context context
    ) {
        requireContext(context);
        try (InputStream ignored =
                     context.getAssets().open(assetPath(animationName))) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public void removeAnimation(String id) {
        String normalizedId = normalizeId(id);
        Iterator<Animation> iterator = animations.iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            if (animation.id.equals(normalizedId)) {
                animation.dispose(false);
                iterator.remove();
            }
        }
    }

    public void setAnimationBounds(String id, RectF bounds) {
        String normalizedId = normalizeId(id);
        for (Animation animation : animations) {
            if (animation.id.equals(normalizedId)) {
                animation.width = Math.max(1, Math.round(bounds.width()));
                animation.height = Math.max(1, Math.round(bounds.height()));
                animation.x = Math.round(bounds.left);
                animation.y = Math.round(bounds.top);
            }
        }
    }

    public static void releaseLottieResources(
            LottieViewAnimator lottieViewAnimator
    ) {
        for (Animation animation : lottieViewAnimator.animations) {
            animation.dispose(false);
        }
        lottieViewAnimator.animations.clear();
    }

    public static void Draw(
            Canvas canvas,
            LottieViewAnimator lottieViewAnimator
    ) {
        removeFinishedAnimations(lottieViewAnimator.animations);
        drawActiveAnimations(canvas, lottieViewAnimator.animations);
    }

    public static void Draw(
            Canvas canvas,
            ArrayList<Animation> animations
    ) {
        removeFinishedAnimations(animations);
        drawActiveAnimations(canvas, animations);
    }

    public static void visibleDraw(
            Canvas canvas,
            LottieViewAnimator lottieViewAnimator,
            View visibleView
    ) {
        // Cleanup must operate on the owning list, not on a temporary visible list.
        removeFinishedAnimations(lottieViewAnimator.animations);
        ArrayList<Animation> visibleAnimations = new ArrayList<>();
        getVisible(lottieViewAnimator, visibleAnimations, visibleView);
        drawActiveAnimations(canvas, visibleAnimations);
    }

    public static void getVisible(
            LottieViewAnimator lottieViewAnimator,
            ArrayList<Animation> output,
            View visibleView
    ) {
        output.clear();

        Rect visibleRect = new Rect();
        Rect animationRect = new Rect();
        visibleView.getLocalVisibleRect(visibleRect);

        for (Animation animation : lottieViewAnimator.animations) {
            animationRect.set(
                    animation.x,
                    animation.y,
                    animation.x + animation.width,
                    animation.y + animation.height
            );
            if (Rect.intersects(visibleRect, animationRect)) {
                output.add(animation);
            }
        }
    }

    private static void removeFinishedAnimations(
            ArrayList<Animation> animations
    ) {
        Iterator<Animation> iterator = animations.iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            if (animation.isFinished()) {
                animation.dispose(true);
                iterator.remove();
            }
        }
    }

    private static void drawActiveAnimations(
            Canvas canvas,
            ArrayList<Animation> animations
    ) {
        for (Animation animation : animations) {
            int intrinsicWidth = animation.drawable.getIntrinsicWidth();
            int intrinsicHeight = animation.drawable.getIntrinsicHeight();
            if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                throw new IllegalStateException(
                        "Lottie animation '" + animation.id
                                + "' has invalid intrinsic bounds."
                );
            }

            int saveCount = canvas.save();
            float scaleX =
                    (float) animation.width / intrinsicWidth;
            float scaleY =
                    (float) animation.height / intrinsicHeight;
            canvas.translate(animation.x, animation.y);
            canvas.rotate(
                    animation.angle,
                    animation.width / 2f,
                    animation.height / 2f
            );
            canvas.scale(scaleX, scaleY);
            animation.drawable.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    public static final class Animation {
        private final Callback callback;
        private final String id;
        public final LottieDrawable drawable;
        private int width;
        private int height;
        private int x;
        private int y;
        private final float angle;
        private boolean callbackDispatched;
        private boolean disposed;

        private Animation(
                String id,
                LottieDrawable drawable,
                int width,
                int height,
                int x,
                int y,
                float angle,
                Callback callback
        ) {
            this.id = id;
            this.drawable = drawable;
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.callback = callback;
        }

        private boolean isFinished() {
            return !drawable.isAnimating()
                    && drawable.getRepeatCount() != LottieDrawable.INFINITE;
        }

        private void dispose(boolean dispatchCallback) {
            if (!disposed) {
                drawable.stop();
                drawable.clearComposition();
                disposed = true;
            }
            if (dispatchCallback
                    && !callbackDispatched
                    && callback != null) {
                callbackDispatched = true;
                callback.onFinish();
            }
        }
    }

    public static void shutdown() {
        for (FutureTask<LottieComposition> loadTask
                : loadingAnimations.values()) {
            loadTask.cancel(true);
        }
        loadingAnimations.clear();
        preloadedAnimations.clear();
    }

    public interface Callback {
        void onFinish();
    }
}

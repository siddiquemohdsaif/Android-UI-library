package com.ogfa.nativeviews.animation.gif;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;


import com.ogfa.nativeviews.internal.util.LogManager.Log;
import com.ogfa.nativeviews.internal.util.BackgroundRunner;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.io.IOException;
import java.io.InputStream;

final class GIFViewAnimator {

    public ArrayList<Animation> animations = new ArrayList<>();
    private static final ConcurrentHashMap<String, GIFComposition> preloadedAnimations =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FutureTask<GIFComposition>>
            loadingAnimations = new ConcurrentHashMap<>();

    /**
     * Preloads one GIF from assets/gif. The name may include or omit ".gif".
     */
    public static void preloadAnimations(Context context, String animationName) {
        preloadAnimations(context, new String[]{animationName});
    }

    /**
     * Preloads multiple GIFs from assets/gif.
     */
    public static void preloadAnimations(Context context, String... animationNames) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        if (animationNames == null || animationNames.length == 0) {
            throw new IllegalArgumentException("At least one GIF name is required.");
        }

        // Validate the entire request before starting any asynchronous work.
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

    public static void shutdown() {
        for (FutureTask<GIFComposition> task : loadingAnimations.values()) task.cancel(true);
        loadingAnimations.clear();
        preloadedAnimations.clear();
    }

    private static void preloadAnimation(Context context, String animationName) {
        String id = normalizeId(animationName);
        String path = assetPath(animationName);

        if (preloadedAnimations.containsKey(id)) {
            return;
        }

        FutureTask<GIFComposition> loadTask =
                createLoadTask(context, path);
        if (loadingAnimations.putIfAbsent(id, loadTask) != null) {
            return;
        }

        BackgroundRunner.run(() -> {
            loadTask.run();
            try {
                GIFComposition loaded =
                        awaitLoad(loadTask, animationName, path);
                preloadedAnimations.putIfAbsent(id, loaded);
            } catch (IllegalStateException exception) {
                Log.e("GIFViewAnimator", exception.getMessage());
            } finally {
                loadingAnimations.remove(id, loadTask);
            }
        });
    }

    /**
     * Returns a cached composition or checks assets/gif and loads it once.
     */
    public static GIFComposition getOrLoad(Context context, String animationName) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }

        String id = normalizeId(animationName);
        GIFComposition cached = preloadedAnimations.get(id);
        if (cached != null) {
            return cached;
        }

        String path = assetPath(animationName);
        FutureTask<GIFComposition> newTask =
                createLoadTask(context, path);
        FutureTask<GIFComposition> loadTask =
                loadingAnimations.putIfAbsent(id, newTask);
        boolean ownsLoadTask = loadTask == null;

        if (ownsLoadTask) {
            loadTask = newTask;
            loadTask.run();
        }

        try {
            GIFComposition loaded =
                    awaitLoad(loadTask, animationName, path);
            GIFComposition existing =
                    preloadedAnimations.putIfAbsent(id, loaded);
            return existing == null ? loaded : existing;
        } finally {
            if (ownsLoadTask) {
                loadingAnimations.remove(id, loadTask);
            }
        }
    }

    private static FutureTask<GIFComposition> createLoadTask(
            Context context,
            String path
    ) {
        Context applicationContext = context.getApplicationContext();
        Context assetContext =
                applicationContext == null ? context : applicationContext;
        return new FutureTask<>(
                () -> GIFComposition.fromAssetSync(assetContext, path)
        );
    }

    private static GIFComposition awaitLoad(
            FutureTask<GIFComposition> loadTask,
            String animationName,
            String path
    ) {
        try {
            return loadTask.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while loading GIF '" + animationName + "'.",
                    exception
            );
        } catch (ExecutionException | RuntimeException exception) {
            Throwable cause = exception instanceof ExecutionException
                    ? exception.getCause()
                    : exception;
            throw new IllegalStateException(
                    "GIF '" + animationName + "' was not preloaded and could not be "
                            + "loaded from assets/" + path + ".",
                    cause
            );
        }
    }

    private static GIFComposition requirePreloaded(String animationName) {
        String id = normalizeId(animationName);
        GIFComposition composition = preloadedAnimations.get(id);
        if (composition == null) {
            throw new IllegalStateException(
                    "GIF '" + animationName + "' is not preloaded. Call "
                            + "GifAnimator.preload(context, \"" + id
                            + "\") or build GifAnimator with a Context."
            );
        }
        return composition;
    }

    private static String normalizeId(String animationName) {
        if (animationName == null) {
            throw new IllegalArgumentException("GIF name cannot be null.");
        }

        String normalized = animationName.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("gif/")) {
            normalized = normalized.substring(4);
        }
        if (normalized.toLowerCase().endsWith(".gif")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("GIF name cannot be empty.");
        }
        return normalized;
    }

    private static String assetPath(String animationName) {
        return "gif/" + normalizeId(animationName) + ".gif";
    }

    private static void requireAssetExists(
            Context context,
            String assetPath,
            String animationName
    ) {
        try (InputStream ignored = context.getAssets().open(assetPath)) {
            // Opening is enough to validate the asset before async preload starts.
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "GIF '" + animationName + "' was not found at assets/" + assetPath + ".",
                    exception
            );
        }
    }

    public static void visibleDraw(Canvas canvas, GIFViewAnimator gifViewAnimator, View view) {
        try {

            ArrayList<Animation> animationsVisible = new ArrayList<>();
            getVisible(gifViewAnimator,animationsVisible,view);
            Draw(canvas,animationsVisible);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getVisible(GIFViewAnimator gifViewAnimator, ArrayList<Animation> animationsVisible, View view) {
        try {
            animationsVisible.clear();

            // Get the visible rectangle of the ScrollView
            Rect scrollViewVisibleRect = new Rect();
            Rect animationRect = new Rect();

            view.getLocalVisibleRect(scrollViewVisibleRect);

            Iterator<Animation> iterator = gifViewAnimator.animations.iterator();

            while (iterator.hasNext()) {
                Animation animation = iterator.next();

                // Define the rect for the current animation based on its x, y, width, and height
                animation.rectF.round(animationRect);

                // Check if the animation's rect intersects with the ScrollView's visible rect
                if (Rect.intersects(scrollViewVisibleRect, animationRect)) {
                    animationsVisible.add(animation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAnimation(String id, RectF rectF, int repeatCount) {
        GIFComposition composition = requirePreloaded(id);
        animations.add(new Animation(normalizeId(id), composition, rectF, repeatCount));
    }

    public void addAnimation(
            Context context,
            String id,
            RectF rectF,
            int repeatCount
    ) {
        GIFComposition composition = getOrLoad(context, id);
        animations.add(new Animation(normalizeId(id), composition, rectF, repeatCount));
    }

    public void addAnimation(String id, GIFComposition composition, RectF rectF, int repeatCount) {
        if (composition != null) {
            animations.add(new Animation(id, composition, rectF , repeatCount));
        } else {
            Log.e("GIFViewAnimator", "Animation with id " + id + " not found.");
        }
    }

    public void removeAnimation(String id) {
        Iterator<Animation> iterator = animations.iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            if (animation.id.equals(id)) {
                iterator.remove();
            }
        }
    }

    public void setAnimationBounds(String id, RectF bounds) {
        String normalized = normalizeId(id);
        for (Animation animation : animations) {
            if (animation.id.equals(normalized)) animation.rectF.set(bounds);
        }
    }

    public void clear() {
        animations.clear();
    }

    public static void Draw(Canvas canvas, GIFViewAnimator gifViewAnimator) {
        Draw(canvas,gifViewAnimator.animations);
    }

    public static void Draw(Canvas canvas, ArrayList<GIFViewAnimator.Animation> animations) {
        Iterator<GIFViewAnimator.Animation> iterator = animations.iterator();
        while (iterator.hasNext()) {
            GIFViewAnimator.Animation animation = iterator.next();

            //if the animation is finished and not set to repeat infinitely, remove it
            if (!animation.isAnimating()) {
                iterator.remove();
                continue;
            }

            animation.draw(canvas);
        }
    }


    public static class Animation {
        String id;
        public GIFComposition gifComposition;
        private RectF rectF;
        private Paint paint = new Paint();
        private int repeatCount;
        private int currentRepeat;
        private long startTime;
        private long timeElapsed;
        public boolean animationOn;

        Animation(String id, GIFComposition gifComposition, RectF rectF, int repeatCount) {
            this.id = id;
            this.gifComposition = gifComposition;
            this.rectF = rectF;
            this.repeatCount = repeatCount;
            startTime = System.currentTimeMillis();
            this.animationOn = true;
            this.currentRepeat = 0;
        }

        public void draw(Canvas canvas) {
            // find time elapsed
            timeElapsed = System.currentTimeMillis() - startTime;
            if (timeElapsed > gifComposition.getDuration()){
                if (repeatCount == -1){
                    restartAnim();
                }else if (currentRepeat < repeatCount){
                    restartAnim();
                }else {
                    animationOn = false;
                    return;
                }
            }

            // draw
            gifComposition.draw(canvas, rectF, timeElapsed, paint);
        }

        private void restartAnim() {
            startTime = System.currentTimeMillis();
            timeElapsed = 0;
            currentRepeat++;
        }

        public boolean isAnimating(){
            return animationOn;
        }
    }

}

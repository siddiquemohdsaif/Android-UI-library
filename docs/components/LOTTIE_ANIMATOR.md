# LottieAnimator

`LottieAnimator` renders `assets/lottie/json/*.json` directly in a `ZLayer`.

```java
LottieAnimator success = layer.add(
        new LottieAnimator.Builder(
                getContext(), "success", "success", position, new Size(360f, 360f)
        )
                .setAutoPlay(true)
                .setRepeatCount(0)
                .setSpeed(1f)
);
```

Names with and without `.json` resolve to the same cache entry. Preloading shares one
in-flight load and disk fallback produces a clear runtime exception for missing or invalid
JSON.

```java
LottieAnimator.preload(context, "success");
LottieAnimator.preload(context, "success", "loading", "confetti");
LottieAnimator.isLoaded("success");
LottieAnimator.clearCache("success");
LottieAnimator.clearCache();
LottieAnimator.shutdown();
```

The default external-image resolver checks the animation directory, root asset name,
`assets/images`, and `assets/lottie/images`. A builder can replace it:

```java
.setImageResolver(assetName -> bitmapProvider.get(assetName))
```

All playback, callbacks, layout, centering, visibility, alpha, clipping, and click methods
are identical to those documented in [GifAnimator](GIF_ANIMATOR.md#shared-animator-api).
`LottieLayer` uses this same component internally and retains playback while its parent
moves.

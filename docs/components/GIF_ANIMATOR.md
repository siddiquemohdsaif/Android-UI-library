# GifAnimator

`GifAnimator` renders `assets/gif/*.gif` directly inside a `ZLayer`.

```java
GifAnimator gif = layer.add(
        new GifAnimator.Builder(
                getContext(), "reward", "reward.gif", position, new Size(420f, 180f)
        )
                .setAutoPlay(true)
                .setRepeatCount(AnimatorComponent.INFINITE)
                .setRepeatMode(RepeatMode.RESTART)
                .setSpeed(1f)
                .setOnClickListener(id -> toggleAnimation())
);
```

The name may include or omit `.gif`. Loading is cache-first, shares one in-flight task per
asset, and falls back to `assets/gif/<name>.gif` when it was not preloaded.

```java
GifAnimator.preload(context, "reward");
GifAnimator.preload(context, "reward", "loading", "celebration");
GifAnimator.isLoaded("reward");
GifAnimator.clearCache("reward");
GifAnimator.clearCache();
GifAnimator.shutdown();
```

Runtime playback and layout use the [shared animator API](#shared-animator-api). For a layer
inside a composition, add `GifAnimator` directly to a `ZLayerContainer` layer.

## Shared animator API

```java
gif.play();
gif.pause();
gif.resume();
gif.restart();
gif.stop();
gif.seekTo(0.5f);

gif.setSpeed(1.5f);
gif.setRepeatCount(3);
gif.setRepeatMode(RepeatMode.REVERSE);
gif.setRegion(position, size);
gif.setRegion(rectF);
gif.horizontalCenter(true);
gif.verticalCenter(true);
gif.setAlpha(0.7f);
gif.setVisible(false);
gif.setEnabled(false);
gif.setClipToBounds(true);
```

`getPlaybackState()`, `getProgress()`, `getSpeed()`, `getRepeatCount()`,
`getRepeatMode()`, and `needsNextFrame()` expose current state. Release is idempotent and
cached compositions are not owned by individual components.

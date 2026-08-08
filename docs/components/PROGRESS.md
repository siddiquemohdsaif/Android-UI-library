# Progress

`Progress` is one non-interactive ZLayer component with native linear/circular
rendering or an internal cached GIF/Lottie animator delegate. Asset delegates are not
separate ZLayer children: the Progress ID, region, visibility, playback, and release
lifecycle control the complete indicator.

## Native determinate indicators

```java
Progress upload = layer.add(
        new Progress.Builder(context, "upload", position, new Size(640f, 24f))
                .setStyle(Progress.Style.LINEAR)
                .setMode(Progress.Mode.DETERMINATE)
                .setProgress(0.65f)
                .setTrackColor(0xffd9e1e8)
                .setProgressColor(0xff019cc4)
                .setThickness(24f)
                .setCornerRadius(12f));
```

```java
Progress circular = layer.add(
        new Progress.Builder(context, "download", position, new Size(120f, 120f))
                .setStyle(Progress.Style.CIRCULAR)
                .setMode(Progress.Mode.DETERMINATE)
                .setProgressPercent(72f)
                .setThickness(10f)
                .setStartAngle(-90f)
                .setCircularDirection(Progress.CircularDirection.CLOCKWISE)
                .setStrokeCap(Progress.StrokeCap.ROUND));
```

`setProgress()` accepts `0f..1f`; `setProgressPercent()` accepts `0f..100f`.

## Native indeterminate indicators

```java
new Progress.Builder(context, "linear_loading", position, size)
        .setStyle(Progress.Style.LINEAR)
        .setMode(Progress.Mode.INDETERMINATE)
        .setIndeterminateDuration(1200L)
        .setIndeterminateSegmentSize(0.32f)
        .setLinearDirection(Progress.LinearDirection.LEFT_TO_RIGHT);
```

```java
new Progress.Builder(context, "circular_loading", position, size)
        .setStyle(Progress.Style.CIRCULAR)
        .setMode(Progress.Mode.INDETERMINATE)
        .setIndeterminateDuration(900L)
        .setIndeterminateSweepAngle(96f)
        .setCircularDirection(Progress.CircularDirection.CLOCKWISE);
```

Linear directions include left/right and top/bottom. Circular direction can be
clockwise or counterclockwise. Indeterminate playback starts automatically by default.

## GIF and Lottie indicators

GIF files resolve under `assets/gif`; Lottie JSON resolves under
`assets/lottie/json`. Extension names are optional.

```java
Progress gif = layer.add(
        new Progress.Builder(
                context, "gif_loading", ProgressAsset.gif("loading"),
                position, new Size(160f, 160f))
                .setAssetPlayback(Progress.AssetPlayback.AUTO_PLAY)
                .setRepeatCount(AnimatorComponent.INFINITE)
                .setSpeed(1f)
                .setContentScaleType(Image.ScaleType.FIT_CENTER));
```

```java
Progress lottie = layer.add(
        new Progress.Builder(
                context, "lottie_loading", ProgressAsset.lottie("loading"),
                position, new Size(180f, 180f))
                .setAssetPlayback(Progress.AssetPlayback.AUTO_PLAY)
                .setLottieImageResolver(assetName -> bitmaps.get(assetName)));
```

`FIT_XY`, `FIT_CENTER`, and `CENTER_CROP` are supported using the GIF/Lottie intrinsic
dimensions. Progress clips the delegate to its own region.

## Follow-progress animation assets

```java
Progress animated = layer.add(
        new Progress.Builder(
                context, "upload_animation",
                ProgressAsset.lottie("upload_progress"), position, size)
                .setAssetPlayback(Progress.AssetPlayback.FOLLOW_PROGRESS)
                .setProgress(0f));

animated.setProgress(0.5f); // Seeks the Lottie timeline to 50%.
```

`FOLLOW_PROGRESS` pauses automatic playback and seeks GIF/Lottie frames using the
logical Progress value. `play()` is deliberately rejected in this mode.

## Preload and cache

```java
Progress.preload(
        context,
        ProgressAsset.gif("loading"),
        ProgressAsset.lottie("upload_progress")
);

Progress.isLoaded(ProgressAsset.gif("loading"));
Progress.clearCache(ProgressAsset.gif("loading"));
```

This delegates to the existing cache-first GIF and Lottie loaders, including shared
in-flight loading, normalized names, asset fallback, and clear invalid/missing errors.

## Regions and Figma/Px styling

```java
new Progress.Builder(context, "loading", position, new Size(640f, 24f));
new Progress.Builder(context, "loading", new RectF(left, top, right, bottom));
```

Asset constructors support both region forms. Parent alignment is available through
`horizontalCenter(true)` and `verticalCenter(true)`.

Normal numeric styling uses Figma units:

```java
.setThickness(12f)
.setCornerRadius(6f)
.setPadding(4f)
```

Runtime-pixel forms:

```java
.setThicknessPx(12f)
.setCornerRadiusPx(6f)
.setPaddingPx(4f)
```

Resolved geometry is exposed through `getResolvedThickness()`,
`getResolvedCornerRadius()`, and `getResolvedPadding()`. Angles and normalized
fractions are never Figma-scaled.

## Updating and completion

```java
progress.setProgress(0.75f);
progress.setProgressPercent(75f);
progress.animateProgressTo(0.90f, 300L);
progress.animateProgressTo(1f, 500L, Progress.Interpolator.EASE_IN_OUT);
progress.animateProgressPercentTo(80f, 300L);
```

A newer progress animation cancels the previous one and starts from the currently
displayed value.

```java
.setOnProgressChangedListener((id, value) -> updateLabel(value))
.setOnProgressCompleteListener(id -> finishUpload())
```

Completion fires once when crossing from below 1 to 1. Dropping below 1 rearms it.

## Playback

Indeterminate native and auto-playing asset indicators support:

```java
progress.play();
progress.pause();
progress.resume();
progress.restart();
progress.stop();
progress.isPlaying();
progress.getPlaybackState();
progress.needsNextFrame();
```

Static determinate native indicators reject playback methods with a mode-specific
exception.

## Disabled and visibility behavior

```java
.setDisabledTrackColor(0xffeeeeee)
.setDisabledProgressColor(0xff9e9e9e)
.setDisabledAlpha(0.65f)
.setPauseWhenHidden(true)
.setPauseWhenDisabled(false)
```

GIF/Lottie disabled appearance uses component alpha. Hiding pauses by default and
showing resumes from the retained frame.

## Runtime switching

```java
progress.setMode(Progress.Mode.INDETERMINATE);
progress.setProgressAsset(ProgressAsset.gif("loading"));
progress.setAssetPlayback(Progress.AssetPlayback.FOLLOW_PROGRESS);
progress.useNativeLinearRendering();
progress.useNativeCircularRendering();

progress.setRegion(position, size);
progress.setRegion(rectF);
progress.setAlpha(0.8f);
progress.setVisible(false);
progress.setEnabled(false);
```

Progress returns `false` from touch handling. Labels, values, retry actions, and cancel
buttons remain separate components. `ZLayerGroup.release()` releases internal native
animators and GIF/Lottie delegates without clearing shared composition caches.

# DynamicViewAnimator

`DynamicViewAnimator` converts a programmatic Canvas animation into a first-class ZLayer
component.

```java
DynamicViewAnimator pulse = layer.add(
        new DynamicViewAnimator.Builder(
                getContext(),
                "pulse",
                new CustomDynamicView() {
                    @Override
                    public void onDraw(Canvas canvas, float progress, RectF bounds) {
                        canvas.drawCircle(
                                bounds.centerX(), bounds.centerY(),
                                bounds.width() * progress, paint
                        );
                    }

                    @Override public long getDurationMillis() { return 900L; }
                    @Override public void onReset() {}
                    @Override public void onRelease() {}
                },
                position,
                new Size(180f, 180f)
        )
                .setRepeatCount(AnimatorComponent.INFINITE)
                .setRepeatMode(RepeatMode.REVERSE)
);
```

`progress` is always in `0f..1f`; `bounds` is the resolved runtime region. Timing uses
`SystemClock.uptimeMillis()`, so wall-clock changes cannot jump the animation. Duration must
be positive.

All playback, callbacks, layout, centering, visibility, alpha, clipping, and click methods
are identical to [GifAnimator](GIF_ANIMATOR.md#shared-animator-api). Add
`DynamicViewAnimator` directly to a root or container-owned `ZLayer`.

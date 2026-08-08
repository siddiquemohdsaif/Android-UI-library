# AfterEffectAnimator

`AfterEffectAnimator` renders an immutable reusable After Effects composition directly in a
`ZLayer`. Runtime playback state is kept outside the composition.

```java
AfterEffectComposition composition =
        new AfterEffectComposition.Builder(new Size(1080f, 1920f), 2400L)
                .addLayer(backgroundLayer)
                .addLayer(characterLayer)
                .addLayer(particleLayer)
                .build();

AfterEffectAnimator intro = layer.add(
        new AfterEffectAnimator.Builder(
                getContext(), "intro", composition, position, new Size(742f, 849f)
        )
                .setAutoPlay(true)
                .setRepeatCount(0)
);
```

The design window is scaled uniformly into the component's resolved Canvas region. Layer
definitions remain in design coordinates:

```java
Layer layer = new Layer(bitmap);
KeyFrameAnimation motion = new KeyFrameAnimationBuilder(70f, 75f)
        .setPosXInterpolator(Linear.get(70f, 670f))
        .setRotationInterpolator(Linear.get(0f, 360f))
        .build();
layer.addKeyFrameTimeLineDefinition(motion, 0L, 1600L);
```

The composition exposes an unmodifiable ordered layer list. Renderer scratch objects belong
to each animator instance, removing the former global mutable renderer state. All playback,
callbacks, layout, centering, visibility, alpha, clipping, and click methods match
[GifAnimator](GIF_ANIMATOR.md#shared-animator-api).

Add `AfterEffectAnimator` directly to a root or container-owned `ZLayer`.

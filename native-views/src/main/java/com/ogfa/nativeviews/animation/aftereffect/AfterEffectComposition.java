package com.ogfa.nativeviews.animation.aftereffect;

import com.ogfa.nativeviews.component.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable reusable After Effects design window, duration and ordered layer list. */
public final class AfterEffectComposition {
    private final AnimationWindow window;
    private final List<Layer> layers;
    private final long durationMillis;

    private AfterEffectComposition(Builder builder) {
        window = builder.window;
        layers = Collections.unmodifiableList(new ArrayList<>(builder.layers));
        durationMillis = builder.durationMillis;
    }
    public AnimationWindow getWindow() { return window; }
    public List<Layer> getLayers() { return layers; }
    public long getDurationMillis() { return durationMillis; }

    public static final class Builder {
        private final AnimationWindow window;
        private final long durationMillis;
        private final ArrayList<Layer> layers = new ArrayList<>();
        public Builder(Size designSize, long durationMillis) {
            Objects.requireNonNull(designSize, "Design size cannot be null.");
            window = new AnimationWindow(designSize.getWidth(), designSize.getHeight(), 0f, 0f);
            if (durationMillis <= 0L) throw new IllegalArgumentException("Duration must be positive.");
            this.durationMillis = durationMillis;
        }
        public Builder(AnimationWindow window, long durationMillis) {
            this.window = Objects.requireNonNull(window, "Animation window cannot be null.");
            if (durationMillis <= 0L) throw new IllegalArgumentException("Duration must be positive.");
            this.durationMillis = durationMillis;
        }
        public Builder addLayer(Layer layer) { layers.add(Objects.requireNonNull(layer, "Layer cannot be null.")); return this; }
        public Builder addLayers(List<Layer> values) { for (Layer layer : values) addLayer(layer); return this; }
        public AfterEffectComposition build() { return new AfterEffectComposition(this); }
    }
}

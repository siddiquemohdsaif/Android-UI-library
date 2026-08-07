package com.ogfa.nativeviews.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable leaf or composite enter/exit transition used by {@link Dialog}. */
public final class DialogTransition {
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }
    public enum PlayMode { LEAF, PARALLEL, SEQUENCE }

    static final class Transform {
        float alpha = 1f;
        float scale = 1f;
        float translationXFigma;
        float translationYFigma;
        float translationXPx;
        float translationYPx;

        void combine(Transform other) {
            // Minimum prevents two simultaneous fades from being multiplied into
            // an unintentionally faster fade while still honoring the strongest one.
            alpha = Math.min(alpha, other.alpha);
            scale *= other.scale;
            translationXFigma += other.translationXFigma;
            translationYFigma += other.translationYFigma;
            translationXPx += other.translationXPx;
            translationYPx += other.translationYPx;
        }
    }

    private final PlayMode playMode;
    private final long duration;
    private final float effectAlpha;
    private final float effectScale;
    private final float translationX;
    private final float translationY;
    private final boolean translationXInPixels;
    private final boolean translationYInPixels;
    private final Interpolator interpolator;
    private final List<DialogTransition> children;

    private DialogTransition(Builder builder) {
        playMode = PlayMode.LEAF;
        duration = builder.duration;
        effectAlpha = builder.effectAlpha;
        effectScale = builder.effectScale;
        translationX = builder.translationX;
        translationY = builder.translationY;
        translationXInPixels = builder.translationXInPixels;
        translationYInPixels = builder.translationYInPixels;
        interpolator = builder.interpolator;
        children = Collections.emptyList();
    }

    private DialogTransition(PlayMode playMode, DialogTransition... values) {
        if (playMode == PlayMode.LEAF) {
            throw new IllegalArgumentException("Composite play mode cannot be LEAF.");
        }
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("At least one transition is required.");
        }
        ArrayList<DialogTransition> copy = new ArrayList<>(values.length);
        long resolvedDuration = playMode == PlayMode.PARALLEL ? 0L : 0L;
        for (DialogTransition value : values) {
            DialogTransition transition = Objects.requireNonNull(
                    value, "Transition cannot be null.");
            copy.add(transition);
            if (playMode == PlayMode.PARALLEL) {
                resolvedDuration = Math.max(resolvedDuration, transition.duration);
            } else {
                try {
                    resolvedDuration = Math.addExact(resolvedDuration, transition.duration);
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("Transition duration overflow.", exception);
                }
            }
        }
        this.playMode = playMode;
        duration = resolvedDuration;
        effectAlpha = 1f;
        effectScale = 1f;
        translationX = 0f;
        translationY = 0f;
        translationXInPixels = false;
        translationYInPixels = false;
        interpolator = Interpolator.LINEAR;
        children = Collections.unmodifiableList(copy);
    }

    public static DialogTransition none() {
        return new Builder().setDuration(0L).build();
    }

    public static DialogTransition fade(long duration) {
        return new Builder().setDuration(duration).setEffectAlpha(0f).build();
    }

    public static DialogTransition scale(long duration, float effectScale) {
        return new Builder().setDuration(duration).setEffectScale(effectScale).build();
    }

    public static DialogTransition fadeScale(long duration, float effectScale) {
        return new Builder().setDuration(duration).setEffectAlpha(0f)
                .setEffectScale(effectScale).build();
    }

    public static DialogTransition slideFromBottom(long duration, float distance) {
        return new Builder().setDuration(duration).setEffectAlpha(0f)
                .setTranslationY(distance).build();
    }

    public static DialogTransition slideFromTop(long duration, float distance) {
        return new Builder().setDuration(duration).setEffectAlpha(0f)
                .setTranslationY(-distance).build();
    }

    public static DialogTransition slideFromLeft(long duration, float distance) {
        return new Builder().setDuration(duration).setEffectAlpha(0f)
                .setTranslationX(-distance).build();
    }

    public static DialogTransition slideFromRight(long duration, float distance) {
        return new Builder().setDuration(duration).setEffectAlpha(0f)
                .setTranslationX(distance).build();
    }

    public static DialogTransition parallel(DialogTransition... transitions) {
        return new DialogTransition(PlayMode.PARALLEL, transitions);
    }

    public static DialogTransition sequence(DialogTransition... transitions) {
        return new DialogTransition(PlayMode.SEQUENCE, transitions);
    }

    public PlayMode getPlayMode() { return playMode; }
    public List<DialogTransition> getChildren() { return children; }
    public long getDuration() { return duration; }
    public float getEffectAlpha() { return effectAlpha; }
    public float getEffectScale() { return effectScale; }
    public float getTranslationX() { return translationX; }
    public float getTranslationY() { return translationY; }
    public boolean isTranslationInPixels() {
        return translationXInPixels && translationYInPixels;
    }
    public boolean isTranslationXInPixels() { return translationXInPixels; }
    public boolean isTranslationYInPixels() { return translationYInPixels; }
    public Interpolator getInterpolator() { return interpolator; }

    Transform sample(float normalizedProgress) {
        float progress = Math.max(0f, Math.min(1f, normalizedProgress));
        long elapsed = duration == 0L ? 0L : Math.round(duration * progress);
        return sampleElapsed(elapsed, duration == 0L || progress >= 1f);
    }

    private Transform sampleElapsed(long elapsed, boolean forceComplete) {
        if (playMode == PlayMode.LEAF) {
            float raw = forceComplete || duration == 0L
                    ? 1f
                    : Math.max(0f, Math.min(1f, elapsed / (float) duration));
            float value = interpolate(raw);
            Transform result = new Transform();
            result.alpha = 1f + (effectAlpha - 1f) * value;
            result.scale = 1f + (effectScale - 1f) * value;
            if (translationXInPixels) result.translationXPx = translationX * value;
            else result.translationXFigma = translationX * value;
            if (translationYInPixels) result.translationYPx = translationY * value;
            else result.translationYFigma = translationY * value;
            return result;
        }

        Transform result = new Transform();
        if (playMode == PlayMode.PARALLEL) {
            for (DialogTransition child : children) {
                long childElapsed = Math.min(elapsed, child.duration);
                result.combine(child.sampleElapsed(
                        childElapsed,
                        forceComplete || elapsed >= child.duration
                ));
            }
            return result;
        }

        long remaining = elapsed;
        for (DialogTransition child : children) {
            if (forceComplete || remaining >= child.duration) {
                result.combine(child.sampleElapsed(child.duration, true));
                remaining = Math.max(0L, remaining - child.duration);
            } else {
                result.combine(child.sampleElapsed(remaining, false));
                break;
            }
        }
        return result;
    }

    private float interpolate(float value) {
        switch (interpolator) {
            case EASE_IN: return value * value;
            case EASE_OUT: return 1f - (1f - value) * (1f - value);
            case EASE_IN_OUT:
                return value < 0.5f
                        ? 2f * value * value
                        : 1f - (float) Math.pow(-2f * value + 2f, 2f) / 2f;
            case LINEAR:
            default: return value;
        }
    }

    public static final class Builder {
        private long duration = 220L;
        private float effectAlpha = 1f;
        private float effectScale = 1f;
        private float translationX;
        private float translationY;
        private boolean translationXInPixels;
        private boolean translationYInPixels;
        private Interpolator interpolator = Interpolator.EASE_OUT;

        public Builder setDuration(long value) {
            if (value < 0L) throw new IllegalArgumentException("Duration cannot be negative.");
            duration = value; return this;
        }
        public Builder setEffectAlpha(float value) {
            if (!Float.isFinite(value) || value < 0f || value > 1f) {
                throw new IllegalArgumentException("Effect alpha must be in 0..1.");
            }
            effectAlpha = value; return this;
        }
        public Builder setEffectScale(float value) {
            if (!Float.isFinite(value) || value <= 0f) {
                throw new IllegalArgumentException("Effect scale must be positive.");
            }
            effectScale = value; return this;
        }
        public Builder setTranslationX(float value) {
            requireFinite(value); translationX = value;
            translationXInPixels = false; return this;
        }
        public Builder setTranslationY(float value) {
            requireFinite(value); translationY = value;
            translationYInPixels = false; return this;
        }
        public Builder setTranslationXPx(float value) {
            requireFinite(value); translationX = value;
            translationXInPixels = true; return this;
        }
        public Builder setTranslationYPx(float value) {
            requireFinite(value); translationY = value;
            translationYInPixels = true; return this;
        }
        public Builder setInterpolator(Interpolator value) {
            interpolator = Objects.requireNonNull(value); return this;
        }
        public DialogTransition build() { return new DialogTransition(this); }
        private static void requireFinite(float value) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Translation must be finite.");
            }
        }
    }
}

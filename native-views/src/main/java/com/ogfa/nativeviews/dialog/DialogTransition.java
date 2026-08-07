package com.ogfa.nativeviews.dialog;

/** Immutable enter/exit transform used by {@link Dialog}. */
public final class DialogTransition {
    public enum Interpolator { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

    private final long duration;
    private final float effectAlpha;
    private final float effectScale;
    private final float translationX;
    private final float translationY;
    private final boolean translationInPixels;
    private final Interpolator interpolator;

    private DialogTransition(Builder builder) {
        duration = builder.duration;
        effectAlpha = builder.effectAlpha;
        effectScale = builder.effectScale;
        translationX = builder.translationX;
        translationY = builder.translationY;
        translationInPixels = builder.translationInPixels;
        interpolator = builder.interpolator;
    }

    public static DialogTransition none() {
        return new Builder().setDuration(0L).build();
    }

    public static DialogTransition fade(long duration) {
        return new Builder().setDuration(duration).setEffectAlpha(0f).build();
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

    public long getDuration() { return duration; }
    public float getEffectAlpha() { return effectAlpha; }
    public float getEffectScale() { return effectScale; }
    public float getTranslationX() { return translationX; }
    public float getTranslationY() { return translationY; }
    public boolean isTranslationInPixels() { return translationInPixels; }
    public Interpolator getInterpolator() { return interpolator; }

    float interpolate(float value) {
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
        private boolean translationInPixels;
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
            requireFinite(value); translationX = value; translationInPixels = false; return this;
        }
        public Builder setTranslationY(float value) {
            requireFinite(value); translationY = value; translationInPixels = false; return this;
        }
        public Builder setTranslationXPx(float value) {
            requireFinite(value); translationX = value; translationInPixels = true; return this;
        }
        public Builder setTranslationYPx(float value) {
            requireFinite(value); translationY = value; translationInPixels = true; return this;
        }
        public Builder setInterpolator(Interpolator value) {
            interpolator = java.util.Objects.requireNonNull(value); return this;
        }
        public DialogTransition build() { return new DialogTransition(this); }
        private static void requireFinite(float value) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Translation must be finite.");
        }
    }
}

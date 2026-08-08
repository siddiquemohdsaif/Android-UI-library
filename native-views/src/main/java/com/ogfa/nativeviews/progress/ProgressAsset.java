package com.ogfa.nativeviews.progress;

/** Immutable GIF or Lottie source used by Progress animation rendering. */
public final class ProgressAsset {
    public enum Type { GIF, LOTTIE }

    private final Type type;
    private final String name;

    private ProgressAsset(Type type, String name) {
        this.type = type;
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Progress asset name cannot be blank.");
        }
        this.name = name.trim();
    }

    public static ProgressAsset gif(String name) {
        return new ProgressAsset(Type.GIF, name);
    }

    public static ProgressAsset lottie(String name) {
        return new ProgressAsset(Type.LOTTIE, name);
    }

    public Type getType() { return type; }
    public String getName() { return name; }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof ProgressAsset)) return false;
        ProgressAsset other = (ProgressAsset) value;
        return type == other.type && normalizedName().equals(other.normalizedName());
    }

    @Override public int hashCode() { return 31 * type.hashCode() + normalizedName().hashCode(); }

    private String normalizedName() {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        String suffix = type == Type.GIF ? ".gif" : ".json";
        return lower.endsWith(suffix) ? lower.substring(0, lower.length() - suffix.length()) : lower;
    }
}

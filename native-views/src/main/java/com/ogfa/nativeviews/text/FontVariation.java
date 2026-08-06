package com.ogfa.nativeviews.text;

/**
 * Named weight presets for variable fonts.
 *
 * <p>On Android API 26 and newer the preset is applied to the OpenType
 * {@code wght} axis. Ordinary fonts and fonts without that axis continue to
 * render normally. Android API 24 and 25 retain the font's normal rendering.</p>
 */
public enum FontVariation {

    THIN(100),
    EXTRA_LIGHT(200),
    LIGHT(300),
    REGULAR(400),
    MEDIUM(500),
    SEMI_BOLD(600),
    BOLD(700),
    EXTRA_BOLD(800),
    BLACK(900);

    private final int weight;

    FontVariation(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public String toSettings() {
        return "'wght' " + weight;
    }
}

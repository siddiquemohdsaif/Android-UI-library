package com.ogfa.nativeviews.switchcomponent;

import android.graphics.Bitmap;

import java.util.Objects;

/** Immutable caller-owned bitmap set for image-rendered switches. */
public final class SwitchImages {
    public enum Mode { COMPLEX, SIMPLE }

    private final Mode mode;
    private final Bitmap trackOn;
    private final Bitmap trackOff;
    private final Bitmap trackDisabled;
    private final Bitmap thumbEnabled;
    private final Bitmap thumbDisabled;
    private final Bitmap switchOn;
    private final Bitmap switchOff;
    private final Bitmap switchDisabled;

    private SwitchImages(
            Mode mode,
            Bitmap trackOn,
            Bitmap trackOff,
            Bitmap trackDisabled,
            Bitmap thumbEnabled,
            Bitmap thumbDisabled,
            Bitmap switchOn,
            Bitmap switchOff,
            Bitmap switchDisabled
    ) {
        this.mode = mode;
        this.trackOn = trackOn;
        this.trackOff = trackOff;
        this.trackDisabled = trackDisabled;
        this.thumbEnabled = thumbEnabled;
        this.thumbDisabled = thumbDisabled;
        this.switchOn = switchOn;
        this.switchOff = switchOff;
        this.switchDisabled = switchDisabled;
        validateActive();
    }

    public static SwitchImages complex(
            Bitmap trackOn,
            Bitmap trackOff,
            Bitmap trackDisabled,
            Bitmap thumbEnabled,
            Bitmap thumbDisabled
    ) {
        return new SwitchImages(
                Mode.COMPLEX,
                requireBitmap(trackOn, "Track-on bitmap"),
                requireBitmap(trackOff, "Track-off bitmap"),
                requireBitmap(trackDisabled, "Disabled-track bitmap"),
                requireBitmap(thumbEnabled, "Enabled-thumb bitmap"),
                requireBitmap(thumbDisabled, "Disabled-thumb bitmap"),
                null, null, null
        );
    }

    public static SwitchImages simple(
            Bitmap switchOn,
            Bitmap switchOff,
            Bitmap switchDisabled
    ) {
        return new SwitchImages(
                Mode.SIMPLE,
                null, null, null, null, null,
                requireBitmap(switchOn, "Switch-on bitmap"),
                requireBitmap(switchOff, "Switch-off bitmap"),
                requireBitmap(switchDisabled, "Disabled-switch bitmap")
        );
    }

    public Mode getMode() { return mode; }
    public Bitmap getTrackOn() { return trackOn; }
    public Bitmap getTrackOff() { return trackOff; }
    public Bitmap getTrackDisabled() { return trackDisabled; }
    public Bitmap getThumbEnabled() { return thumbEnabled; }
    public Bitmap getThumbDisabled() { return thumbDisabled; }
    public Bitmap getSwitchOn() { return switchOn; }
    public Bitmap getSwitchOff() { return switchOff; }
    public Bitmap getSwitchDisabled() { return switchDisabled; }

    /** Revalidates caller-owned bitmaps before rendering or runtime replacement. */
    public void validateActive() {
        if (mode == Mode.COMPLEX) {
            requireBitmap(trackOn, "Track-on bitmap");
            requireBitmap(trackOff, "Track-off bitmap");
            requireBitmap(trackDisabled, "Disabled-track bitmap");
            requireBitmap(thumbEnabled, "Enabled-thumb bitmap");
            requireBitmap(thumbDisabled, "Disabled-thumb bitmap");
        } else {
            requireBitmap(switchOn, "Switch-on bitmap");
            requireBitmap(switchOff, "Switch-off bitmap");
            requireBitmap(switchDisabled, "Disabled-switch bitmap");
        }
    }

    private static Bitmap requireBitmap(Bitmap bitmap, String label) {
        Objects.requireNonNull(bitmap, label + " cannot be null.");
        if (bitmap.isRecycled()) throw new IllegalArgumentException(label + " cannot be recycled.");
        if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            throw new IllegalArgumentException(label + " must have positive dimensions.");
        }
        return bitmap;
    }
}

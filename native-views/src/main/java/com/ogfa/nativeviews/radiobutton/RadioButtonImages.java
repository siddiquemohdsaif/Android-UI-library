package com.ogfa.nativeviews.radiobutton;

import android.graphics.Bitmap;

import java.util.Objects;

/** Immutable caller-owned complete-state images for RadioButton. */
public final class RadioButtonImages {
    private final Bitmap unchecked;
    private final Bitmap checked;
    private final Bitmap disabledUnchecked;
    private final Bitmap disabledChecked;

    private RadioButtonImages(
            Bitmap unchecked,
            Bitmap checked,
            Bitmap disabledUnchecked,
            Bitmap disabledChecked
    ) {
        this.unchecked = requireBitmap(unchecked, "Unchecked bitmap");
        this.checked = requireBitmap(checked, "Checked bitmap");
        this.disabledUnchecked = requireBitmap(disabledUnchecked, "Disabled-unchecked bitmap");
        this.disabledChecked = requireBitmap(disabledChecked, "Disabled-checked bitmap");
    }

    public static RadioButtonImages create(
            Bitmap unchecked,
            Bitmap checked,
            Bitmap disabledUnchecked,
            Bitmap disabledChecked
    ) {
        return new RadioButtonImages(
                unchecked, checked, disabledUnchecked, disabledChecked);
    }

    public Bitmap getUnchecked() { return unchecked; }
    public Bitmap getChecked() { return checked; }
    public Bitmap getDisabledUnchecked() { return disabledUnchecked; }
    public Bitmap getDisabledChecked() { return disabledChecked; }

    public Bitmap get(boolean checked, boolean enabled) {
        validateActive();
        if (enabled) return checked ? this.checked : unchecked;
        return checked ? disabledChecked : disabledUnchecked;
    }

    public void validateActive() {
        requireBitmap(unchecked, "Unchecked bitmap");
        requireBitmap(checked, "Checked bitmap");
        requireBitmap(disabledUnchecked, "Disabled-unchecked bitmap");
        requireBitmap(disabledChecked, "Disabled-checked bitmap");
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

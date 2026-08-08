package com.ogfa.nativeviews.checkbox;

import android.graphics.Bitmap;

import java.util.Objects;

/** Immutable caller-owned bitmap set for image-rendered CheckBoxes. */
public final class CheckBoxImages {
    private final Bitmap unchecked;
    private final Bitmap checked;
    private final Bitmap indeterminate;
    private final Bitmap disabledUnchecked;
    private final Bitmap disabledChecked;
    private final Bitmap disabledIndeterminate;

    private CheckBoxImages(
            Bitmap unchecked,
            Bitmap checked,
            Bitmap indeterminate,
            Bitmap disabledUnchecked,
            Bitmap disabledChecked,
            Bitmap disabledIndeterminate
    ) {
        this.unchecked = requireBitmap(unchecked, "Unchecked bitmap");
        this.checked = requireBitmap(checked, "Checked bitmap");
        this.indeterminate = optionalBitmap(indeterminate, "Indeterminate bitmap");
        this.disabledUnchecked = requireBitmap(disabledUnchecked, "Disabled-unchecked bitmap");
        this.disabledChecked = requireBitmap(disabledChecked, "Disabled-checked bitmap");
        this.disabledIndeterminate = optionalBitmap(
                disabledIndeterminate, "Disabled-indeterminate bitmap");
        if ((this.indeterminate == null) != (this.disabledIndeterminate == null)) {
            throw new IllegalArgumentException(
                    "Indeterminate and disabled-indeterminate bitmaps must both be provided or both omitted."
            );
        }
    }

    public static CheckBoxImages create(
            Bitmap unchecked,
            Bitmap checked,
            Bitmap disabledUnchecked,
            Bitmap disabledChecked
    ) {
        return new CheckBoxImages(
                unchecked, checked, null, disabledUnchecked, disabledChecked, null);
    }

    public static CheckBoxImages create(
            Bitmap unchecked,
            Bitmap checked,
            Bitmap indeterminate,
            Bitmap disabledUnchecked,
            Bitmap disabledChecked,
            Bitmap disabledIndeterminate
    ) {
        return new CheckBoxImages(
                unchecked, checked, indeterminate,
                disabledUnchecked, disabledChecked, disabledIndeterminate);
    }

    public Bitmap getUnchecked() { return unchecked; }
    public Bitmap getChecked() { return checked; }
    public Bitmap getIndeterminate() { return indeterminate; }
    public Bitmap getDisabledUnchecked() { return disabledUnchecked; }
    public Bitmap getDisabledChecked() { return disabledChecked; }
    public Bitmap getDisabledIndeterminate() { return disabledIndeterminate; }
    public boolean supportsIndeterminate() { return indeterminate != null; }

    public Bitmap get(CheckBox.State state, boolean enabled) {
        validateActive();
        switch (state) {
            case CHECKED: return enabled ? checked : disabledChecked;
            case INDETERMINATE:
                if (!supportsIndeterminate()) {
                    throw new IllegalStateException(
                            "This CheckBoxImages set does not provide indeterminate images.");
                }
                return enabled ? indeterminate : disabledIndeterminate;
            default: return enabled ? unchecked : disabledUnchecked;
        }
    }

    /** Revalidates caller-owned bitmaps before every render or runtime replacement. */
    public void validateActive() {
        requireBitmap(unchecked, "Unchecked bitmap");
        requireBitmap(checked, "Checked bitmap");
        requireBitmap(disabledUnchecked, "Disabled-unchecked bitmap");
        requireBitmap(disabledChecked, "Disabled-checked bitmap");
        if (supportsIndeterminate()) {
            requireBitmap(indeterminate, "Indeterminate bitmap");
            requireBitmap(disabledIndeterminate, "Disabled-indeterminate bitmap");
        }
    }

    private static Bitmap optionalBitmap(Bitmap bitmap, String label) {
        return bitmap == null ? null : requireBitmap(bitmap, label);
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

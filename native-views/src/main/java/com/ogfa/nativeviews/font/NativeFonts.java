package com.ogfa.nativeviews.font;

import android.content.Context;
import android.graphics.Typeface;

import com.ogfa.nativeviews.R;
import com.ogfa.nativeviews.text.internal.TypefaceCache;

import java.util.Objects;

/**
 * Font resources shipped with Native Views.
 *
 * <p>Pass one of these resource IDs to APIs such as
 * {@code Text.Builder.setFont(int)} or {@code TextField.Builder.setFont(int)}.
 * The consuming application does not need to copy the font into its own
 * {@code res/font} directory.</p>
 */
public final class NativeFonts {

    public static final int INTER = R.font.nativeviews_inter_variable;
    public static final int INTER_ITALIC =
            R.font.nativeviews_inter_italic_variable;

    public static final int MONTSERRAT =
            R.font.nativeviews_montserrat_variable;
    public static final int MONTSERRAT_ITALIC =
            R.font.nativeviews_montserrat_italic_variable;

    public static final int ROBOTO = R.font.nativeviews_roboto_variable;
    public static final int ROBOTO_ITALIC =
            R.font.nativeviews_roboto_italic_variable;

    public static final int LILITA_ONE =
            R.font.nativeviews_lilita_one_regular;

    private NativeFonts() {
    }

    /**
     * Loads and process-caches a bundled or application-owned font resource.
     */
    public static Typeface load(Context context, int fontResourceId) {
        return TypefaceCache.fromResource(
                Objects.requireNonNull(context, "Context cannot be null."),
                fontResourceId
        );
    }

    /**
     * Loads a font and derives an Android NORMAL, BOLD, ITALIC, or BOLD_ITALIC
     * style from it.
     */
    public static Typeface load(
            Context context,
            int fontResourceId,
            int style
    ) {
        if (style < Typeface.NORMAL || style > Typeface.BOLD_ITALIC) {
            throw new IllegalArgumentException(
                    "Typeface style must be NORMAL, BOLD, ITALIC, or BOLD_ITALIC."
            );
        }
        return Typeface.create(load(context, fontResourceId), style);
    }
}

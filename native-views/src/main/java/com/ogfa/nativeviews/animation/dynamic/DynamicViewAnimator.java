package com.ogfa.nativeviews.animation.dynamic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import com.ogfa.nativeviews.animation.BaseAnimatorBuilder;
import com.ogfa.nativeviews.animation.BaseAnimatorComponent;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import java.util.Objects;

/** Programmatic Canvas animation usable directly in a ZLayer. */
public final class DynamicViewAnimator extends BaseAnimatorComponent {
    private final CustomDynamicView dynamicView;

    private DynamicViewAnimator(Builder builder, View host) {
        super(builder, host);
        dynamicView = Objects.requireNonNull(builder.dynamicView, "Dynamic view cannot be null.");
        requireDuration(dynamicView);
    }

    @Override protected long getDurationMillis() { return dynamicView.getDurationMillis(); }
    @Override protected void renderFrame(Canvas canvas, float progress, RectF bounds) { dynamicView.onDraw(canvas, progress, bounds); }
    @Override protected void onRepeat() { dynamicView.onReset(); }
    @Override protected void onStop() { dynamicView.onReset(); }
    @Override protected void onReleaseResources() { dynamicView.onRelease(); }

    public static final class Builder extends BaseAnimatorBuilder<Builder, DynamicViewAnimator> {
        private final CustomDynamicView dynamicView;
        public Builder(Context context, String id, CustomDynamicView view, Position position, Size size) {
            super(context, id, position, size); dynamicView = Objects.requireNonNull(view, "Dynamic view cannot be null.");
        }
        public Builder(Context context, String id, CustomDynamicView view, RectF bounds) {
            super(context, id, bounds); dynamicView = Objects.requireNonNull(view, "Dynamic view cannot be null.");
        }
        @Override public DynamicViewAnimator build(View hostView) { return new DynamicViewAnimator(this, hostView); }
    }

    private static void requireDuration(CustomDynamicView view) { if (view.getDurationMillis() <= 0L) throw new IllegalArgumentException("Dynamic duration must be positive."); }
}

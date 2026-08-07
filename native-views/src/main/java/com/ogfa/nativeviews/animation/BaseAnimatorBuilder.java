package com.ogfa.nativeviews.animation;

import android.content.Context;
import android.graphics.RectF;
import android.view.View;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import java.util.Objects;

public abstract class BaseAnimatorBuilder<B extends BaseAnimatorBuilder<B, T>, T extends BaseAnimatorComponent>
        implements ComponentFactory<T> {
    protected final Context context;
    protected final String id;
    protected final Position position;
    protected final Size size;
    protected final RectF rect;
    protected boolean autoPlay = true;
    protected int repeatCount;
    protected RepeatMode repeatMode = RepeatMode.RESTART;
    protected float speed = 1f;
    protected float initialProgress;
    protected boolean clipToBounds = true;
    protected boolean horizontalCenter;
    protected boolean verticalCenter;
    protected float alpha = 1f;
    protected boolean progressCallbacks;
    protected boolean pauseWhenHidden = true;
    protected AnimationListener listener;
    protected BaseAnimatorComponent.OnClickListener clickListener;

    protected BaseAnimatorBuilder(Context context, String id, Position position, Size size) {
        this.context = Objects.requireNonNull(context, "Context cannot be null.");
        this.id = requireId(id);
        this.position = Objects.requireNonNull(position, "Position cannot be null.");
        this.size = Objects.requireNonNull(size, "Size cannot be null.");
        rect = null;
    }

    protected BaseAnimatorBuilder(Context context, String id, RectF rect) {
        this.context = Objects.requireNonNull(context, "Context cannot be null.");
        this.id = requireId(id);
        this.rect = requireRect(rect);
        position = null;
        size = null;
    }

    @SuppressWarnings("unchecked") protected final B self() { return (B) this; }
    public B setAutoPlay(boolean value) { autoPlay = value; return self(); }
    public B setRepeatCount(int value) { if (value < -1) throw new IllegalArgumentException("Repeat count must be -1 or greater."); repeatCount = value; return self(); }
    public B setRepeatMode(RepeatMode value) { repeatMode = Objects.requireNonNull(value); return self(); }
    public B setSpeed(float value) { if (!(value > 0f) || !Float.isFinite(value)) throw new IllegalArgumentException("Speed must be positive and finite."); speed = value; return self(); }
    public B setInitialProgress(float value) { initialProgress = requireProgress(value); return self(); }
    public B setClipToBounds(boolean value) { clipToBounds = value; return self(); }
    public B horizontalCenter(boolean value) { horizontalCenter = value; return self(); }
    public B verticalCenter(boolean value) { verticalCenter = value; return self(); }
    public B setAlpha(float value) { alpha = clampAlpha(value); return self(); }
    public B setOnAnimationListener(AnimationListener value) { listener = value; return self(); }
    public B setProgressCallbacksEnabled(boolean value) { progressCallbacks = value; return self(); }
    public B setPauseWhenHidden(boolean value) { pauseWhenHidden = value; return self(); }
    public B setOnClickListener(BaseAnimatorComponent.OnClickListener value) { clickListener = value; return self(); }

    protected final RectF resolveBounds(View host) {
        return rect != null ? new RectF(rect) : position.toRectF(host, size);
    }

    public final Context getContext() { return context; }

    private static String requireId(String value) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Animator id cannot be blank."); return value; }
    static RectF requireRect(RectF value) { Objects.requireNonNull(value, "Bounds cannot be null."); if (value.width() <= 0f || value.height() <= 0f) throw new IllegalArgumentException("Bounds must be positive."); return new RectF(value); }
    static float requireProgress(float value) { if (!Float.isFinite(value) || value < 0f || value > 1f) throw new IllegalArgumentException("Progress must be in [0, 1]."); return value; }
    static float clampAlpha(float value) { if (!Float.isFinite(value)) throw new IllegalArgumentException("Alpha must be finite."); return Math.max(0f, Math.min(1f, value)); }
}

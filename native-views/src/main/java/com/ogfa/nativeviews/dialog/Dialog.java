package com.ogfa.nativeviews.dialog;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.ogfa.nativeviews.button.Button;
import com.ogfa.nativeviews.card.Card;
import com.ogfa.nativeviews.card.DropShadow;
import com.ogfa.nativeviews.component.BackHandler;
import com.ogfa.nativeviews.component.Component;
import com.ogfa.nativeviews.component.ComponentFactory;
import com.ogfa.nativeviews.component.ComponentHost;
import com.ogfa.nativeviews.component.Position;
import com.ogfa.nativeviews.component.Size;
import com.ogfa.nativeviews.image.Image;
import com.ogfa.nativeviews.zlayer.NestedComponentHost;
import com.ogfa.nativeviews.zlayer.ZLayer;

import java.util.List;
import java.util.Objects;

/** Canvas-native modal overlay backed by a layered {@link Card} surface. */
public final class Dialog implements Component, BackHandler {
    public enum OutsideTouchPolicy { IGNORE, DISMISS }
    public enum DismissReason {
        PROGRAMMATIC, OUTSIDE_TOUCH, BACK_PRESSED, ACTION, HOST_RELEASED
    }
    private enum State { HIDDEN, ENTERING, SHOWN, EXITING }

    public interface OnShowListener { void onShow(String id); }
    public interface OnDismissListener {
        void onDismiss(String id, DismissReason reason);
    }
    public interface ContentBuilder {
        void build(Dialog dialog, ZLayer content, Scope scope);
    }

    /** Dialog-relative Figma conversion and safe nested IDs. */
    public static final class Scope {
        private final Dialog dialog;
        private Scope(Dialog dialog) { this.dialog = dialog; }
        public Context getContext() { return dialog.hostView.getContext(); }
        public View getHostView() { return dialog.hostView; }
        public String id(String localId) {
            return dialog.id + "/" + requireId(localId, "Dialog component ID");
        }
        public float width() { return dialog.surface.getBounds().width() / dialog.dimensionScale; }
        public float height() { return dialog.surface.getBounds().height() / dialog.dimensionScale; }
        public float px(float value) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Figma value must be finite.");
            return value * dialog.dimensionScale;
        }
        public RectF rect(float left, float top, float width, float height) {
            requireNonNegative(left, "Left"); requireNonNegative(top, "Top");
            requirePositive(width, "Width"); requirePositive(height, "Height");
            RectF parent = dialog.surface.getBounds();
            float scale = dialog.dimensionScale;
            return new RectF(parent.left + left * scale, parent.top + top * scale,
                    parent.left + (left + width) * scale,
                    parent.top + (top + height) * scale);
        }
        public RectF getBounds() { return dialog.surface.getContentBounds(); }
    }

    private final View hostView;
    private final String id;
    private final Card surface;
    private final Scope scope;
    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF overlayBounds = new RectF();
    private ComponentHost owner;
    private State state = State.HIDDEN;
    private OutsideTouchPolicy outsideTouchPolicy;
    private boolean dismissOnBackPressed;
    private boolean dimEnabled;
    private int dimColor;
    private float dimAlpha;
    private float alpha;
    private float surfaceAlpha;
    private boolean contentEnabled;
    private boolean interactiveDuringTransition;
    private float dimensionScale;
    private DialogTransition enterTransition;
    private DialogTransition exitTransition;
    private ValueAnimator animator;
    private float transitionProgress;
    private boolean insideGesture;
    private boolean outsideGesture;
    private DismissReason pendingDismissReason = DismissReason.PROGRAMMATIC;
    private OnShowListener showListener;
    private OnDismissListener dismissListener;
    private boolean released;

    private Dialog(Builder builder, View hostView) {
        this.hostView = Objects.requireNonNull(hostView, "Host view cannot be null.");
        id = requireId(builder.id, "Dialog ID");
        dimensionScale = builder.position == null ? 1f : builder.position.getScale(hostView);
        Card.Builder surfaceBuilder = builder.explicitBounds == null
                ? new Card.Builder(hostView.getContext(), id + "/surface",
                        builder.position, builder.size)
                : new Card.Builder(hostView.getContext(), id + "/surface",
                        builder.explicitBounds);
        surfaceBuilder.setBackgroundColor(builder.backgroundColor)
                .setCornerRadius(builder.cornerRadius)
                .setAlpha(builder.surfaceAlpha)
                .setVisible(true)
                .setEnabled(true)
                .horizontalCenter(builder.horizontalCentered)
                .verticalCenter(builder.verticalCentered);
        if (builder.backgroundImage != null) {
            surfaceBuilder.setBackgroundImage(builder.backgroundImage)
                    .setBackgroundScaleType(builder.backgroundScaleType);
        }
        if (builder.dropShadow == null) surfaceBuilder.removeDropShadow();
        else if (builder.dropShadowInPixels) surfaceBuilder.setDropShadowPx(builder.dropShadow);
        else surfaceBuilder.setDropShadow(builder.dropShadow);
        if (builder.cornerRadiusInPixels) surfaceBuilder.setCornerRadiusPx(builder.cornerRadius);
        surface = surfaceBuilder.build(hostView);
        scope = new Scope(this);
        outsideTouchPolicy = builder.outsideTouchPolicy;
        dismissOnBackPressed = builder.dismissOnBackPressed;
        dimEnabled = builder.dimEnabled;
        dimColor = builder.dimColor;
        dimAlpha = builder.dimAlpha;
        alpha = builder.alpha;
        surfaceAlpha = builder.surfaceAlpha;
        contentEnabled = builder.enabled;
        interactiveDuringTransition = builder.interactiveDuringTransition;
        enterTransition = builder.enterTransition;
        exitTransition = builder.exitTransition;
        showListener = builder.showListener;
        dismissListener = builder.dismissListener;
        if (builder.contentBuilder != null) {
            builder.contentBuilder.build(this, getContentLayer(), scope);
        }
        if (builder.initiallyShown) showImmediately();
    }

    @Override public String getId() { return id; }
    @Override public RectF getBounds() { return surface.getBounds(); }
    public RectF getVisualBounds() { return surface.getVisualBounds(); }
    public Scope getScope() { return scope; }
    public Card getSurface() { return surface; }
    public ZLayer getContentLayer() { return surface.getContentLayer(); }
    public ZLayer addContentLayer(String id) { return surface.addContentLayer(id); }
    public ZLayer findContentLayer(String id) { return surface.findContentLayer(id); }
    public List<ZLayer> getContentLayers() { return surface.getContentLayers(); }
    public void bringContentLayerToFront(String id) { surface.bringContentLayerToFront(id); }
    public void sendContentLayerToBack(String id) { surface.sendContentLayerToBack(id); }
    public void moveContentLayerAbove(String id, String referenceId) {
        surface.moveContentLayerAbove(id, referenceId);
    }
    public void moveContentLayerBelow(String id, String referenceId) {
        surface.moveContentLayerBelow(id, referenceId);
    }
    public void setContentLayerIndex(String id, int index) {
        surface.setContentLayerIndex(id, index);
    }

    public Component find(String localId) {
        String fullId = localId.startsWith(id + "/") ? localId : scope.id(localId);
        for (ZLayer layer : surface.getContentLayers()) {
            Component component = layer.find(fullId);
            if (component != null) return component;
        }
        return null;
    }
    public <T extends Component> T find(String localId, Class<T> type) {
        Component component = find(localId);
        return type.isInstance(component) ? type.cast(component) : null;
    }

    public boolean isShowing() { return state != State.HIDDEN; }
    public boolean isHidden() { return state == State.HIDDEN; }
    public boolean isEntering() { return state == State.ENTERING; }
    public boolean isExiting() { return state == State.EXITING; }
    public boolean isAnimating() { return animator != null && animator.isRunning(); }
    @Override public boolean isVisible() { return !released && state != State.HIDDEN; }
    /** A showing Dialog stays dispatch-enabled so disabled content remains modal. */
    @Override public boolean isEnabled() { return isVisible(); }
    public boolean isContentEnabled() { return contentEnabled; }

    public Dialog show() {
        ensureActive();
        if (state == State.SHOWN || state == State.ENTERING) return this;
        startTransition(true, null);
        return this;
    }
    public Dialog showImmediately() {
        ensureActive(); cancelAnimator();
        state = State.SHOWN; transitionProgress = 1f;
        surface.setVisible(true).setEnabled(contentEnabled);
        invalidate();
        if (showListener != null) showListener.onShow(id);
        return this;
    }
    public Dialog dismiss() { return dismiss(DismissReason.PROGRAMMATIC); }
    public Dialog dismiss(DismissReason reason) {
        ensureActive();
        if (state == State.HIDDEN || state == State.EXITING) return this;
        startTransition(false, Objects.requireNonNull(reason));
        return this;
    }
    public Dialog dismissImmediately() { return dismissImmediately(DismissReason.PROGRAMMATIC); }
    public Dialog dismissImmediately(DismissReason reason) {
        ensureActive();
        if (state == State.HIDDEN) return this;
        cancelAnimator(); state = State.HIDDEN; transitionProgress = 0f;
        insideGesture = outsideGesture = false;
        surface.setVisible(false);
        invalidate();
        if (dismissListener != null) dismissListener.onDismiss(id, reason);
        return this;
    }
    public Dialog toggle() { return isShowing() ? dismiss() : show(); }

    @Override
    public boolean onBackPressed() {
        if (!isShowing()) return false;
        if (dismissOnBackPressed && state == State.SHOWN) {
            dismiss(DismissReason.BACK_PRESSED);
        }
        return true;
    }

    public Dialog setRegion(Position position, Size size) {
        dimensionScale = Objects.requireNonNull(position).getScale(hostView);
        surface.setRegion(position, Objects.requireNonNull(size)); invalidate(); return this;
    }
    public Dialog setRegion(RectF bounds) {
        dimensionScale = 1f; surface.setRegion(bounds); invalidate(); return this;
    }
    public Dialog horizontalCenter(boolean value) { surface.horizontalCenter(value); return this; }
    public Dialog verticalCenter(boolean value) { surface.verticalCenter(value); return this; }
    public Dialog setBackgroundColor(int value) { surface.setBackgroundColor(value); return this; }
    public Dialog setBackgroundImage(Bitmap value) { surface.setBackgroundImage(value); return this; }
    public Dialog setBackgroundScaleType(Image.ScaleType value) {
        surface.setBackgroundScaleType(value); return this;
    }
    public Dialog setCornerRadius(float value) { surface.setCornerRadius(value); return this; }
    public Dialog setCornerRadiusPx(float value) { surface.setCornerRadiusPx(value); return this; }
    public Dialog setDropShadow(DropShadow value) { surface.setDropShadow(value); return this; }
    public Dialog setDropShadowPx(DropShadow value) { surface.setDropShadowPx(value); return this; }
    public Dialog removeDropShadow() { surface.removeDropShadow(); return this; }
    public Dialog setSurfaceAlpha(float value) {
        surfaceAlpha = requireAlpha(value); surface.setAlpha(surfaceAlpha); return this;
    }
    public Dialog setDimEnabled(boolean value) { dimEnabled = value; invalidate(); return this; }
    public Dialog setDimColor(int value) { dimColor = value; invalidate(); return this; }
    public Dialog setDimAlpha(float value) { dimAlpha = requireAlpha(value); invalidate(); return this; }
    public Dialog setOutsideTouchPolicy(OutsideTouchPolicy value) {
        outsideTouchPolicy = Objects.requireNonNull(value); return this;
    }
    public Dialog setDismissOnBackPressed(boolean value) { dismissOnBackPressed = value; return this; }
    public Dialog setEnterTransition(DialogTransition value) {
        enterTransition = Objects.requireNonNull(value); return this;
    }
    public Dialog setExitTransition(DialogTransition value) {
        exitTransition = Objects.requireNonNull(value); return this;
    }
    public Dialog setInteractiveDuringTransition(boolean value) {
        interactiveDuringTransition = value; return this;
    }
    public Dialog setOnShowListener(OnShowListener value) { showListener = value; return this; }
    public Dialog setOnDismissListener(OnDismissListener value) { dismissListener = value; return this; }
    public Dialog setTranslation(float x, float y) { surface.setTranslation(x, y); return this; }
    public Dialog setTranslationX(float value) { surface.setTranslationX(value); return this; }
    public Dialog setTranslationY(float value) { surface.setTranslationY(value); return this; }
    public Dialog resetTranslation() { surface.resetTranslation(); return this; }
    public float getTranslationX() { return surface.getTranslationX(); }
    public float getTranslationY() { return surface.getTranslationY(); }

    public Dialog setAlpha(float value) { alpha = requireAlpha(value); invalidate(); return this; }
    public float getAlpha() { return alpha; }
    public Dialog setEnabled(boolean value) {
        contentEnabled = value; surface.setEnabled(value); return this;
    }
    public Dialog setVisible(boolean value) {
        return value ? showImmediately() : dismissImmediately();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isVisible() || alpha <= 0f) return;
        updateOverlayBounds();
        DialogTransition transition = state == State.EXITING ? exitTransition : enterTransition;
        float timeline = transition.interpolate(transitionProgress);
        float effect = state == State.EXITING ? timeline : 1f - timeline;
        float visibleFraction = 1f - effect;
        if (dimEnabled) {
            dimPaint.setColor(dimColor);
            dimPaint.setAlpha(Math.round(Color.alpha(dimColor) * dimAlpha * alpha * visibleFraction));
            canvas.drawRect(overlayBounds, dimPaint);
        }
        float surfaceVisualAlpha = (1f + (transition.getEffectAlpha() - 1f) * effect) * alpha;
        if (surfaceVisualAlpha <= 0f) return;
        RectF surfaceBounds = surface.getBounds();
        float scale = 1f + (transition.getEffectScale() - 1f) * effect;
        float transitionScale = transition.isTranslationInPixels() ? 1f : dimensionScale;
        float tx = transition.getTranslationX() * transitionScale * effect;
        float ty = transition.getTranslationY() * transitionScale * effect;
        int alphaSave = canvas.saveLayerAlpha(overlayBounds,
                Math.round(surfaceVisualAlpha * 255f));
        canvas.translate(tx, ty);
        canvas.scale(scale, scale, surfaceBounds.centerX(), surfaceBounds.centerY());
        surface.draw(canvas);
        canvas.restoreToCount(alphaSave);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isShowing()) return false;
        if (!contentEnabled || (isAnimating() && !interactiveDuringTransition)) return true;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                insideGesture = surface.getBounds().contains(event.getX(), event.getY());
                outsideGesture = !insideGesture;
                if (insideGesture) surface.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (insideGesture) surface.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_UP:
                if (insideGesture) surface.onTouchEvent(event);
                if (outsideGesture && outsideTouchPolicy == OutsideTouchPolicy.DISMISS) {
                    dismiss(DismissReason.OUTSIDE_TOUCH);
                }
                insideGesture = outsideGesture = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (insideGesture) surface.onTouchEvent(event);
                insideGesture = outsideGesture = false;
                return true;
            default: return true;
        }
    }

    @Override
    public void attach(ComponentHost owner) {
        ensureActive();
        if (!(owner instanceof NestedComponentHost)) {
            throw new IllegalArgumentException("Dialog requires a NestedComponentHost such as ZLayerGroup.");
        }
        if (this.owner != null && this.owner != owner) {
            throw new IllegalStateException("Dialog already belongs to another host.");
        }
        this.owner = owner;
        surface.attach(owner);
        updateOverlayBounds();
    }

    @Override
    public void release() {
        if (released) return;
        boolean notify = isShowing();
        cancelAnimator();
        state = State.HIDDEN;
        surface.release();
        released = true;
        owner = null;
        if (notify && dismissListener != null) {
            dismissListener.onDismiss(id, DismissReason.HOST_RELEASED);
        }
    }

    private void startTransition(boolean entering, DismissReason reason) {
        cancelAnimator();
        DialogTransition transition = entering ? enterTransition : exitTransition;
        if (entering) {
            state = State.ENTERING; surface.setVisible(true).setEnabled(contentEnabled);
        } else {
            state = State.EXITING; pendingDismissReason = reason;
        }
        transitionProgress = 0f;
        if (transition.getDuration() == 0L) {
            finishTransition(entering); return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(transition.getDuration());
        animator.addUpdateListener(value -> {
            transitionProgress = (float) value.getAnimatedValue(); invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator animation) {
                cancelled = true;
            }
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (!cancelled) finishTransition(entering);
            }
        });
        animator.start(); invalidate();
    }

    private void finishTransition(boolean entering) {
        animator = null;
        if (entering) {
            state = State.SHOWN; transitionProgress = 1f;
            if (showListener != null) showListener.onShow(id);
        } else {
            state = State.HIDDEN; transitionProgress = 0f;
            surface.setVisible(false);
            if (dismissListener != null) dismissListener.onDismiss(id, pendingDismissReason);
        }
        invalidate();
    }
    private void cancelAnimator() {
        if (animator != null) { animator.cancel(); animator = null; }
    }
    private void updateOverlayBounds() {
        if (owner != null) overlayBounds.set(owner.getComponentBounds());
        else overlayBounds.set(0f, 0f, hostView.getWidth(), hostView.getHeight());
    }
    private void invalidate() {
        if (owner != null) owner.postInvalidateComponentOnAnimation();
        else hostView.postInvalidateOnAnimation();
    }
    private void ensureActive() {
        if (released) throw new IllegalStateException("Dialog has been released.");
    }

    private static String requireId(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be null or blank.");
        }
        return value.trim();
    }
    private static float requireAlpha(float value) {
        if (!Float.isFinite(value) || value < 0f || value > 1f) {
            throw new IllegalArgumentException("Alpha must be in 0..1.");
        }
        return value;
    }
    private static float requirePositive(float value, String label) {
        if (!Float.isFinite(value) || value <= 0f) throw new IllegalArgumentException(label + " must be positive.");
        return value;
    }
    private static float requireNonNegative(float value, String label) {
        if (!Float.isFinite(value) || value < 0f) throw new IllegalArgumentException(label + " cannot be negative.");
        return value;
    }

    public static final class Builder implements ComponentFactory<Dialog> {
        private final String id;
        private final Position position;
        private final Size size;
        private final RectF explicitBounds;
        private int backgroundColor = Color.WHITE;
        private Bitmap backgroundImage;
        private Image.ScaleType backgroundScaleType = Image.ScaleType.CENTER_CROP;
        private float cornerRadius = 40f;
        private boolean cornerRadiusInPixels;
        private DropShadow dropShadow = DropShadow.DEFAULT;
        private boolean dropShadowInPixels;
        private boolean horizontalCentered;
        private boolean verticalCentered;
        private boolean dimEnabled = true;
        private int dimColor = Color.BLACK;
        private float dimAlpha = 0.5f;
        private float alpha = 1f;
        private float surfaceAlpha = 1f;
        private boolean enabled = true;
        private boolean initiallyShown;
        private boolean interactiveDuringTransition;
        private boolean dismissOnBackPressed = true;
        private OutsideTouchPolicy outsideTouchPolicy = OutsideTouchPolicy.IGNORE;
        private DialogTransition enterTransition = DialogTransition.fadeScale(220L, 0.92f);
        private DialogTransition exitTransition = DialogTransition.fadeScale(180L, 0.96f);
        private OnShowListener showListener;
        private OnDismissListener dismissListener;
        private ContentBuilder contentBuilder;

        public Builder(Context context, String id, Position position, Size size) {
            Objects.requireNonNull(context, "Context cannot be null.");
            this.id = id; this.position = Objects.requireNonNull(position);
            this.size = Objects.requireNonNull(size); explicitBounds = null;
        }
        public Builder(Context context, String id, RectF bounds) {
            Objects.requireNonNull(context, "Context cannot be null.");
            this.id = id; explicitBounds = new RectF(Objects.requireNonNull(bounds));
            position = null; size = null;
        }
        public Builder setBackgroundColor(int value) { backgroundColor = value; backgroundImage = null; return this; }
        public Builder setBackgroundImage(Bitmap value) { backgroundImage = Objects.requireNonNull(value); return this; }
        public Builder setBackgroundScaleType(Image.ScaleType value) { backgroundScaleType = Objects.requireNonNull(value); return this; }
        public Builder setCornerRadius(float value) { cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = false; return this; }
        public Builder setCornerRadiusPx(float value) { cornerRadius = requireNonNegative(value, "Corner radius"); cornerRadiusInPixels = true; return this; }
        public Builder setDropShadow(DropShadow value) { dropShadow = Objects.requireNonNull(value); dropShadowInPixels = false; return this; }
        public Builder setDropShadowPx(DropShadow value) { dropShadow = Objects.requireNonNull(value); dropShadowInPixels = true; return this; }
        public Builder removeDropShadow() { dropShadow = null; return this; }
        public Builder horizontalCenter(boolean value) { horizontalCentered = value; return this; }
        public Builder verticalCenter(boolean value) { verticalCentered = value; return this; }
        public Builder setDimEnabled(boolean value) { dimEnabled = value; return this; }
        public Builder setDimColor(int value) { dimColor = value; return this; }
        public Builder setDimAlpha(float value) { dimAlpha = requireAlpha(value); return this; }
        public Builder setAlpha(float value) { alpha = requireAlpha(value); return this; }
        public Builder setSurfaceAlpha(float value) { surfaceAlpha = requireAlpha(value); return this; }
        public Builder setEnabled(boolean value) { enabled = value; return this; }
        public Builder setInitiallyShown(boolean value) { initiallyShown = value; return this; }
        public Builder setInteractiveDuringTransition(boolean value) { interactiveDuringTransition = value; return this; }
        public Builder setDismissOnBackPressed(boolean value) { dismissOnBackPressed = value; return this; }
        public Builder setOutsideTouchPolicy(OutsideTouchPolicy value) { outsideTouchPolicy = Objects.requireNonNull(value); return this; }
        public Builder setEnterTransition(DialogTransition value) { enterTransition = Objects.requireNonNull(value); return this; }
        public Builder setExitTransition(DialogTransition value) { exitTransition = Objects.requireNonNull(value); return this; }
        public Builder setOnShowListener(OnShowListener value) { showListener = value; return this; }
        public Builder setOnDismissListener(OnDismissListener value) { dismissListener = value; return this; }
        public Builder setContent(ContentBuilder value) { contentBuilder = value; return this; }
        @Override public Dialog build(View hostView) { return new Dialog(this, hostView); }
    }
}

package com.ogfa.nativeviews.animation;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.ogfa.nativeviews.button.internal.Region;
import com.ogfa.nativeviews.text.TextMakerEngine;
import com.ogfa.nativeviews.text.TextWriter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * Draws bitmap-font text and provides an optional animated press/click region.
 *
 * <p>This is a Canvas helper, not an Android {@code TextView}. Call {@link #onDraw(Canvas)}
 * from the host view and forward touch events to {@link #onTouchEvent(MotionEvent)}.</p>
 */
public class TextViewAnimator implements AutoCloseable {

    private static final float DEFAULT_PRESSED_SCALE = 0.96f;
    private static final long PRESS_ANIMATION_DURATION_MS = 80L;

    public Bitmap mBitmap;

    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG
    );
    private final RectF bounds;
    private final RectF drawBounds = new RectF();
    private final Region touchRegion;
    private final String id;
    private final int width;
    private final int height;

    private OnClickListener clickListener;
    private Runnable invalidateCallback;
    private ValueAnimator scaleAnimator;
    private float drawScale = 1f;
    private float pressedScale = DEFAULT_PRESSED_SCALE;
    private boolean pressed;
    private boolean clickable;

    public TextViewAnimator(
            Context context,
            OnClickListener clickListener,
            String id,
            int width,
            int height,
            Bitmap bitmap,
            int left,
            int top
    ) {
        this(
                context,
                clickListener,
                id,
                width,
                height,
                bitmap,
                left,
                top,
                clickListener != null
        );
    }

    public TextViewAnimator(
            Context context,
            OnClickListener clickListener,
            String id,
            int width,
            int height,
            Bitmap bitmap,
            int left,
            int top,
            boolean isClickable
    ) {
        Objects.requireNonNull(context, "Context cannot be null.");
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Text animator ID cannot be empty.");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Text animator width and height must be greater than zero."
            );
        }
        requireBitmap(bitmap);

        this.id = id;
        this.width = width;
        this.height = height;
        this.mBitmap = bitmap;
        this.clickListener = clickListener;
        this.clickable = isClickable && clickListener != null;
        bounds = new RectF(left, top, left + width, top + height);
        touchRegion = new Region(
                bounds.left,
                bounds.right,
                bounds.top,
                bounds.bottom,
                id
        );
    }

    public void onDraw(Canvas canvas) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        requireBitmap(mBitmap);

        float insetX = width * (1f - drawScale) / 2f;
        float insetY = height * (1f - drawScale) / 2f;
        drawBounds.set(
                bounds.left + insetX,
                bounds.top + insetY,
                bounds.right - insetX,
                bounds.bottom - insetY
        );
        canvas.drawBitmap(mBitmap, null, drawBounds, paint);
    }

    public static void Draw(
            Canvas canvas,
            ArrayList<TextViewAnimator> textViewAnimators
    ) {
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        Objects.requireNonNull(
                textViewAnimators,
                "Text animator list cannot be null."
        );
        for (TextViewAnimator animator : textViewAnimators) {
            Objects.requireNonNull(animator, "Text animator cannot be null.")
                    .onDraw(canvas);
        }
    }

    public static boolean HandleTouch(
            MotionEvent event,
            ArrayList<TextViewAnimator> textViewAnimators
    ) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        Objects.requireNonNull(
                textViewAnimators,
                "Text animator list cannot be null."
        );

        // Last drawn is visually topmost, so it receives touch first.
        for (int index = textViewAnimators.size() - 1; index >= 0; index--) {
            TextViewAnimator animator = textViewAnimators.get(index);
            if (animator != null && animator.onTouchEvent(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        if (!clickable) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!touchRegion.regionClickedDown(x, y)) {
                    return false;
                }
                pressed = true;
                animateScaleTo(pressedScale);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!pressed) {
                    return false;
                }
                if (touchRegion.regionClickedMove(x, y)) {
                    return true;
                }
                cancelPress();
                return false;

            case MotionEvent.ACTION_UP:
                if (!pressed) {
                    return false;
                }
                boolean clicked = touchRegion.isRegionClicked(x, y);
                cancelPress();
                if (clicked && clickListener != null) {
                    clickListener.onClick(id);
                }
                return clicked;

            case MotionEvent.ACTION_CANCEL:
                if (!pressed) {
                    return false;
                }
                cancelPress();
                return true;

            default:
                return false;
        }
    }

    private void cancelPress() {
        pressed = false;
        touchRegion.regionClickedMove(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        animateScaleTo(1f);
    }

    private void animateScaleTo(float targetScale) {
        if (scaleAnimator != null) {
            scaleAnimator.cancel();
        }
        scaleAnimator = ValueAnimator.ofFloat(drawScale, targetScale);
        scaleAnimator.setDuration(PRESS_ANIMATION_DURATION_MS);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.addUpdateListener(animation -> {
            drawScale = (float) animation.getAnimatedValue();
            requestInvalidate();
        });
        scaleAnimator.start();
    }

    private void requestInvalidate() {
        if (invalidateCallback != null) {
            invalidateCallback.run();
        }
    }

    /**
     * Supplies the host view's invalidation callback, for example
     * {@code animator.setInvalidateCallback(view::postInvalidateOnAnimation)}.
     */
    public TextViewAnimator setInvalidateCallback(Runnable callback) {
        invalidateCallback = callback;
        return this;
    }

    public TextViewAnimator setPressedScale(float scale) {
        if (scale <= 0f || scale > 1f) {
            throw new IllegalArgumentException(
                    "Pressed scale must be greater than 0 and at most 1."
            );
        }
        pressedScale = scale;
        return this;
    }

    public TextViewAnimator setClickable(boolean value) {
        clickable = value && clickListener != null;
        if (!clickable && pressed) {
            cancelPress();
        }
        return this;
    }

    public boolean isClickable() {
        return clickable;
    }

    public String getId() {
        return id;
    }

    public RectF getBounds() {
        return new RectF(bounds);
    }

    public void setBitmap(Bitmap bitmap) {
        requireBitmap(bitmap);
        mBitmap = bitmap;
        requestInvalidate();
    }

    public static Bitmap makeTransparent(Bitmap bitmap, int alpha) {
        requireBitmap(bitmap);
        Bitmap transparent = Bitmap.createBitmap(
                bitmap.getWidth(),
                bitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        Paint alphaPaint = new Paint(Paint.ANTI_ALIAS_FLAG
                | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        alphaPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
        new Canvas(transparent).drawBitmap(bitmap, 0f, 0f, alphaPaint);
        return transparent;
    }

    public static void addTextView(
            ArrayList<TextViewAnimator> textViewAnimators,
            Context context,
            OnClickListener clickListener,
            String id,
            float left,
            float top,
            boolean isClickable,
            float alpha,
            TextWriter.LineType lineType,
            float lineHeight,
            String text,
            Map<String, Bitmap> textMap
    ) {
        addTextView(
                textViewAnimators,
                context,
                clickListener,
                id,
                left,
                top,
                isClickable,
                alpha,
                lineType,
                lineHeight,
                text,
                textMap,
                null
        );
    }

    public static void addTextView(
            ArrayList<TextViewAnimator> textViewAnimators,
            Context context,
            OnClickListener clickListener,
            String id,
            float left,
            float top,
            boolean isClickable,
            float alpha,
            TextWriter.LineType lineType,
            float lineHeight,
            String text,
            Map<String, Bitmap> textMap,
            int lineSpacePx
    ) {
        addTextView(
                textViewAnimators,
                context,
                clickListener,
                id,
                left,
                top,
                isClickable,
                alpha,
                lineType,
                lineHeight,
                text,
                textMap,
                (Integer) lineSpacePx
        );
    }

    public static void addTextView(
            ArrayList<TextViewAnimator> textViewAnimators,
            Context context,
            OnClickListener clickListener,
            String id,
            PointF viewMargin,
            float ref,
            float viewWidth,
            boolean isClickable,
            float alpha,
            TextWriter.LineType lineType,
            float lineHeight,
            String text,
            Map<String, Bitmap> textMap
    ) {
        Objects.requireNonNull(viewMargin, "View margin cannot be null.");
        if (ref <= 0f || viewWidth <= 0f) {
            throw new IllegalArgumentException(
                    "Reference width and view width must be greater than zero."
            );
        }
        float ratio = viewWidth / ref;
        addTextView(
                textViewAnimators,
                context,
                clickListener,
                id,
                viewMargin.x * ratio,
                viewMargin.y * ratio,
                isClickable,
                alpha,
                lineType,
                lineHeight,
                text,
                textMap
        );
    }

    private static void addTextView(
            ArrayList<TextViewAnimator> textViewAnimators,
            Context context,
            OnClickListener clickListener,
            String id,
            float anchorX,
            float top,
            boolean isClickable,
            float alpha,
            TextWriter.LineType lineType,
            float lineHeight,
            String text,
            Map<String, Bitmap> textMap,
            Integer spacing
    ) {
        Objects.requireNonNull(
                textViewAnimators,
                "Text animator list cannot be null."
        );
        Objects.requireNonNull(lineType, "Line type cannot be null.");

        Bitmap textBitmap = spacing == null
                ? TextMakerEngine.generateTextBitmap(
                        textMap,
                        text,
                        Math.round(lineHeight)
                )
                : TextMakerEngine.generateTextBitmapWithSpacing(
                        textMap,
                        text,
                        Math.round(lineHeight),
                        spacing
                );
        Bitmap alphaBitmap = makeTransparent(
                textBitmap,
                TextWriterAlpha.toByte(alpha)
        );
        textBitmap.recycle();

        float left = anchorX;
        if (lineType == TextWriter.LineType.END) {
            left -= alphaBitmap.getWidth();
        } else if (lineType == TextWriter.LineType.MIDDLE) {
            left -= alphaBitmap.getWidth() / 2f;
        }

        textViewAnimators.add(
                new TextViewAnimator(
                        context,
                        clickListener,
                        id,
                        alphaBitmap.getWidth(),
                        alphaBitmap.getHeight(),
                        alphaBitmap,
                        Math.round(left),
                        Math.round(top),
                        isClickable
                )
        );
    }

    public static void removeText(
            String id,
            ArrayList<TextViewAnimator> textViewAnimators
    ) {
        Objects.requireNonNull(textViewAnimators, "Text animator list cannot be null.");
        for (int index = 0; index < textViewAnimators.size(); index++) {
            TextViewAnimator animator = textViewAnimators.get(index);
            if (animator != null && animator.id.equals(id)) {
                animator.release();
                textViewAnimators.remove(index);
                return;
            }
        }
    }

    public static void changeIcon(
            String id,
            ArrayList<TextViewAnimator> textViewAnimators,
            Bitmap bitmap
    ) {
        requireBitmap(bitmap);
        Objects.requireNonNull(textViewAnimators, "Text animator list cannot be null.");
        for (TextViewAnimator animator : textViewAnimators) {
            if (animator != null && animator.id.equals(id)) {
                animator.setBitmap(bitmap);
                return;
            }
        }
    }

    public void release() {
        pressed = false;
        if (scaleAnimator != null) {
            scaleAnimator.cancel();
            scaleAnimator.removeAllUpdateListeners();
            scaleAnimator = null;
        }
        invalidateCallback = null;
    }

    @Override
    public void close() {
        release();
    }

    private static void requireBitmap(Bitmap bitmap) {
        Objects.requireNonNull(bitmap, "Bitmap cannot be null.");
        if (bitmap.isRecycled()) {
            throw new IllegalArgumentException("Bitmap has been recycled.");
        }
    }

    public interface OnClickListener {
        void onClick(String id);
    }

    private static final class TextWriterAlpha {
        private static int toByte(float alpha) {
            return Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        }
    }
}

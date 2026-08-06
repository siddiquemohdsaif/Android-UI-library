package com.ogfa.nativeviews.text;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns the drawing order, lookup, invalidation, and lifecycle of Canvas text.
 */
public final class TextGroup implements AutoCloseable {

    private final View hostView;
    private final ArrayList<Text> drawingOrder = new ArrayList<>();
    private final Map<String, Text> textsById = new LinkedHashMap<>();

    private boolean released;
    private Text touchTarget;
    private boolean touchCaptured;
    private boolean clickCancelled;

    public TextGroup(View hostView) {
        this.hostView = Objects.requireNonNull(
                hostView,
                "Host view cannot be null."
        );
    }

    public Text add(Text.Builder builder) {
        ensureActive();
        Objects.requireNonNull(builder, "Text.Builder cannot be null.");
        Text text = builder.build(hostView);
        if (textsById.containsKey(text.getId())) {
            text.release();
            throw new IllegalArgumentException(
                    "TextGroup already contains ID: " + text.getId()
            );
        }
        text.attach(this);
        drawingOrder.add(text);
        textsById.put(text.getId(), text);
        invalidateHost();
        return text;
    }

    public Text find(String id) {
        ensureActive();
        return textsById.get(id);
    }

    public boolean contains(String id) {
        ensureActive();
        return textsById.containsKey(id);
    }

    public boolean remove(String id) {
        ensureActive();
        Text text = textsById.remove(id);
        if (text == null) {
            return false;
        }
        if (text == touchTarget) {
            resetTouch();
        }
        drawingOrder.remove(text);
        text.release();
        invalidateHost();
        return true;
    }

    public int size() {
        ensureActive();
        return drawingOrder.size();
    }

    public boolean isEmpty() {
        ensureActive();
        return drawingOrder.isEmpty();
    }

    public void draw(Canvas canvas) {
        ensureActive();
        Objects.requireNonNull(canvas, "Canvas cannot be null.");
        for (Text text : drawingOrder) {
            text.draw(canvas);
        }
    }

    /**
     * Dispatches a click gesture to the topmost clickable Text region.
     */
    public boolean onTouchEvent(MotionEvent event) {
        ensureActive();
        Objects.requireNonNull(event, "MotionEvent cannot be null.");
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetTouch();
                touchTarget = findTopmostTouchTarget(x, y);
                touchCaptured = touchTarget != null;
                return touchCaptured;

            case MotionEvent.ACTION_MOVE:
                if (!touchCaptured) {
                    return false;
                }
                if (!clickCancelled
                        && (touchTarget == null
                        || !touchTarget.acceptsTouch(x, y))) {
                    clickCancelled = true;
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!touchCaptured) {
                    return false;
                }
                Text target = touchTarget;
                boolean shouldClick = !clickCancelled
                        && target != null
                        && target.acceptsTouch(x, y);
                resetTouch();
                if (shouldClick) {
                    target.performClick();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (!touchCaptured) {
                    return false;
                }
                resetTouch();
                return true;

            default:
                return touchCaptured;
        }
    }

    public void clear() {
        ensureActive();
        releaseChildren();
        invalidateHost();
    }

    public void release() {
        if (released) {
            return;
        }
        releaseChildren();
        released = true;
        hostView.invalidate();
    }

    @Override
    public void close() {
        release();
    }

    void invalidateHost() {
        if (!released) {
            hostView.postInvalidateOnAnimation();
        }
    }

    private void releaseChildren() {
        resetTouch();
        for (Text text : drawingOrder) {
            text.release();
        }
        drawingOrder.clear();
        textsById.clear();
    }

    private Text findTopmostTouchTarget(float x, float y) {
        for (int index = drawingOrder.size() - 1; index >= 0; index--) {
            Text candidate = drawingOrder.get(index);
            if (candidate.acceptsTouch(x, y)) {
                return candidate;
            }
        }
        return null;
    }

    private void resetTouch() {
        touchTarget = null;
        touchCaptured = false;
        clickCancelled = false;
    }

    private void ensureActive() {
        if (released) {
            throw new IllegalStateException("TextGroup has been released.");
        }
    }
}

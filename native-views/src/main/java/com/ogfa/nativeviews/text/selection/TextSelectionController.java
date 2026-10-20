package com.ogfa.nativeviews.text.selection;

import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.ogfa.nativeviews.textfield.TextBoundaryHelper;

/** Shared word, endpoint, and tap rules used by Text and TextField. */
public final class TextSelectionController {
    private TextSelectionController() {}

    public static boolean isSecondTap(MotionEvent event, long lastTapUpTime,
                                      float lastTapX, float lastTapY, int doubleTapSlop) {
        if (lastTapUpTime <= 0L || event.getEventTime() - lastTapUpTime
                > ViewConfiguration.getDoubleTapTimeout()) return false;
        float dx = event.getX() - lastTapX;
        float dy = event.getY() - lastTapY;
        return dx * dx + dy * dy <= doubleTapSlop * doubleTapSlop;
    }

    public static boolean isWithinTouchSlop(float x, float y,
                                            float downX, float downY, int touchSlop) {
        float dx = x - downX;
        float dy = y - downY;
        return dx * dx + dy * dy <= touchSlop * touchSlop;
    }

    public static int[] wordAt(TextSelectionGeometry geometry, float x, float y) {
        return TextBoundaryHelper.wordAt(geometry.selectionText(),
                geometry.selectionOffsetAt(x, y));
    }

    public static void extendDoubleTap(TextSelectionGeometry geometry,
                                       int anchorStart, int anchorEnd,
                                       float x, float y) {
        int[] word = wordAt(geometry, x, y);
        if (word[0] < anchorStart) {
            geometry.applySelection(word[0], anchorEnd);
        } else if (word[1] > anchorEnd) {
            geometry.applySelection(anchorStart, word[1]);
        } else {
            geometry.applySelection(anchorStart, anchorEnd);
        }
    }

    public static void extendLongPress(TextSelectionGeometry geometry,
                                       int anchorStart, int anchorEnd,
                                       float x, float y) {
        int offset = geometry.selectionOffsetAt(x, y);
        if (offset < anchorStart) {
            geometry.applySelection(anchorEnd, offset);
        } else if (offset > anchorEnd) {
            geometry.applySelection(anchorStart, offset);
        } else {
            geometry.applySelection(anchorStart, anchorEnd);
        }
    }

    public static void dragStartHandle(TextSelectionGeometry geometry,
                                       int movingOffset, int fixedEnd) {
        int maximumStart = TextBoundaryHelper.previousGrapheme(
                geometry.selectionText(), fixedEnd);
        geometry.applySelection(Math.max(0, Math.min(movingOffset, maximumStart)),
                fixedEnd);
    }

    public static void dragEndHandle(TextSelectionGeometry geometry,
                                     int fixedStart, int movingOffset) {
        int minimumEnd = TextBoundaryHelper.nextGrapheme(
                geometry.selectionText(), fixedStart);
        geometry.applySelection(fixedStart, Math.min(geometry.selectionText().length(),
                Math.max(movingOffset, minimumEnd)));
    }
}

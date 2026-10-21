package app.builderx.ogfa.androiduicomponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ogfa.nativeviews.text.Text;
import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TextSelectionInstrumentedTest {
    @Test
    public void repeatedDragInsideSelectionContinuesFromStoppedScrollPosition() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            View host = new View(context);
            host.layout(0, 0, 500, 300);
            ZLayerGroup group = new ZLayerGroup(host);
            String value = "one two three four five six seven eight nine ten "
                    + "eleven twelve thirteen fourteen fifteen sixteen seventeen";
            Text text = group.addLayer("text").add(new Text.Builder(context,
                    "paragraph", value, new RectF(0f, 0f, 200f, 120f))
                    .setTextSizePx(30f)
                    .setSelectionEnabled(true));
            Bitmap bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888);
            group.draw(new Canvas(bitmap));
            text.setSelection(0, value.length());

            long now = SystemClock.uptimeMillis();
            dispatch(group, now, now, MotionEvent.ACTION_DOWN, 30f, 90f);
            dispatch(group, now, now + 20, MotionEvent.ACTION_MOVE, 30f, 50f);
            dispatch(group, now, now + 40, MotionEvent.ACTION_UP, 30f, 50f);
            assertEquals(40f, verticalScroll(text), 0.5f);

            dispatch(group, now + 100, now + 100, MotionEvent.ACTION_DOWN, 30f, 90f);
            dispatch(group, now + 100, now + 120, MotionEvent.ACTION_MOVE, 30f, 60f);
            dispatch(group, now + 100, now + 140, MotionEvent.ACTION_UP, 30f, 60f);
            assertEquals(70f, verticalScroll(text), 0.5f);
            assertEquals(0, text.getSelectionStart());
            assertEquals(value.length(), text.getSelectionEnd());
            group.release();
            bitmap.recycle();
        });
    }

    @Test
    public void doubleTapSelectsWordAndOutsideTapClearsIt() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            View host = new View(context);
            host.layout(0, 0, 500, 300);
            ZLayerGroup group = new ZLayerGroup(host);
            Text text = group.addLayer("text").add(new Text.Builder(context,
                    "paragraph", "hello world", new RectF(0f, 0f, 400f, 120f))
                    .setTextSizePx(40f)
                    .setSelectionEnabled(true));
            Bitmap bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888);
            group.draw(new Canvas(bitmap));

            long now = SystemClock.uptimeMillis();
            dispatch(group, now, now, MotionEvent.ACTION_DOWN, 30f, 30f);
            dispatch(group, now, now + 20, MotionEvent.ACTION_UP, 30f, 30f);
            dispatch(group, now + 100, now + 100, MotionEvent.ACTION_DOWN, 30f, 30f);
            dispatch(group, now + 100, now + 120, MotionEvent.ACTION_UP, 30f, 30f);

            assertTrue(text.hasSelection());
            assertEquals("hello", text.getText().subSequence(
                    text.getSelectionStart(), text.getSelectionEnd()).toString());

            dispatch(group, now + 300, now + 300, MotionEvent.ACTION_DOWN, 450f, 200f);
            assertFalse(text.hasSelection());
            group.release();
            bitmap.recycle();
        });
    }

    @Test
    public void doubleTapOnCursorHandleSelectsWordAtCursor() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            View host = new View(context);
            host.layout(0, 0, 500, 300);
            ZLayerGroup group = new ZLayerGroup(host);
            TextField field = group.addLayer("field").add(new TextField.Builder(context,
                    "input", new RectF(20f, 30f, 420f, 130f))
                    .setText("hello world")
                    .setTextSizePx(40f));
            Bitmap bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888);
            group.draw(new Canvas(bitmap));
            group.requestFocus(field);
            field.setSelection(8);

            PointF handle = cursorHandleEndpoint(field);
            float x = handle.x;
            float y = handle.y + 10f;
            assertTrue(field.containsSelectionHandle(x, y));

            long now = SystemClock.uptimeMillis();
            dispatch(group, now, now, MotionEvent.ACTION_DOWN, x, y);
            dispatch(group, now, now + 20, MotionEvent.ACTION_UP, x, y);
            dispatch(group, now + 100, now + 100, MotionEvent.ACTION_DOWN, x, y);
            dispatch(group, now + 100, now + 120, MotionEvent.ACTION_UP, x, y);

            assertEquals("world", field.getText().substring(
                    field.getSelectionStart(), field.getSelectionEnd()));
            group.release();
            bitmap.recycle();
        });
    }

    @Test
    public void repeatedScrollKeepsSelectionAndResumesAtStoppedPosition() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            View host = new View(context);
            host.layout(0, 0, 500, 300);
            ZLayerGroup group = new ZLayerGroup(host);
            String value = "one two three four five six seven eight nine ten "
                    + "eleven twelve thirteen fourteen fifteen sixteen seventeen "
                    + "eighteen nineteen twenty twenty-one twenty-two";
            TextField field = group.addLayer("field").add(new TextField.Builder(context,
                    "input", new RectF(20f, 30f, 420f, 130f))
                    .setText(value)
                    .setTextSizePx(40f)
                    .setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_FLAG_MULTI_LINE));
            Bitmap bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888);
            group.draw(new Canvas(bitmap));
            group.requestFocus(field);
            int selectedStart = value.indexOf("fourteen");
            int selectedEnd = selectedStart + "fourteen".length();
            field.setSelection(selectedStart, selectedEnd);
            float initialScroll = verticalScroll(field);
            assertTrue(initialScroll > 0f);

            long now = SystemClock.uptimeMillis();
            dispatch(group, now, now, MotionEvent.ACTION_DOWN, 330f, 110f);
            dispatch(group, now, now + 10, MotionEvent.ACTION_MOVE, 390f, 100f);
            dispatch(group, now, now + 20, MotionEvent.ACTION_MOVE, 390f, 50f);
            dispatch(group, now, now + 40, MotionEvent.ACTION_UP, 390f, 50f);
            float stoppedScroll = verticalScroll(field);
            assertTrue(stoppedScroll > initialScroll);
            assertEquals(selectedStart, field.getSelectionStart());
            assertEquals(selectedEnd, field.getSelectionEnd());

            dispatch(group, now + 100, now + 100, MotionEvent.ACTION_DOWN, 390f, 110f);
            assertEquals(stoppedScroll, verticalScroll(field), 0.5f);
            dispatch(group, now + 100, now + 120, MotionEvent.ACTION_MOVE, 390f, 50f);
            dispatch(group, now + 100, now + 140, MotionEvent.ACTION_UP, 390f, 50f);
            float resumedScroll = verticalScroll(field);
            assertTrue(resumedScroll > stoppedScroll);
            assertEquals(selectedStart, field.getSelectionStart());
            assertEquals(selectedEnd, field.getSelectionEnd());

            dispatch(group, now + 200, now + 200, MotionEvent.ACTION_DOWN, 390f, 50f);
            dispatch(group, now + 200, now + 220, MotionEvent.ACTION_MOVE, 390f, 110f);
            dispatch(group, now + 200, now + 240, MotionEvent.ACTION_UP, 390f, 110f);
            assertTrue(verticalScroll(field) < resumedScroll);
            assertEquals(selectedStart, field.getSelectionStart());
            assertEquals(selectedEnd, field.getSelectionEnd());

            for (int i = 0; i < 12; i++) {
                long gesture = now + 300 + i * 100L;
                dispatch(group, gesture, gesture, MotionEvent.ACTION_DOWN, 390f, 50f);
                dispatch(group, gesture, gesture + 20, MotionEvent.ACTION_MOVE, 390f, 110f);
                dispatch(group, gesture, gesture + 40, MotionEvent.ACTION_UP, 390f, 110f);
            }
            assertEquals(0f, verticalScroll(field), 0.5f);
            assertEquals(selectedStart, field.getSelectionStart());
            assertEquals(selectedEnd, field.getSelectionEnd());
            group.release();
            bitmap.recycle();
        });
    }

    private static PointF cursorHandleEndpoint(TextField field) {
        try {
            java.lang.reflect.Method method = TextField.class.getDeclaredMethod(
                    "cursorHandleEndpoint");
            method.setAccessible(true);
            return (PointF) method.invoke(field);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void dispatch(ZLayerGroup group, long downTime, long eventTime,
                                 int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            group.onTouchEvent(event);
        } finally {
            event.recycle();
        }
    }

    private static float verticalScroll(Text text) {
        try {
            java.lang.reflect.Field field = Text.class.getDeclaredField("verticalScrollPx");
            field.setAccessible(true);
            return field.getFloat(text);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static float verticalScroll(TextField field) {
        try {
            java.lang.reflect.Field scroll = TextField.class.getDeclaredField("verticalScrollPx");
            scroll.setAccessible(true);
            return scroll.getFloat(field);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}

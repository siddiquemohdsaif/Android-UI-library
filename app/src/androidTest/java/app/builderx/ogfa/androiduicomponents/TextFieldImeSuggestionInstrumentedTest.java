package app.builderx.ogfa.androiduicomponents;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.RectF;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ogfa.nativeviews.textfield.TextField;
import com.ogfa.nativeviews.zlayer.ZLayerGroup;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TextFieldImeSuggestionInstrumentedTest {
    @Test
    public void directEditableSuggestionReplacementNotifiesImmediatelyOnce() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            View host = new View(context);
            host.layout(0, 0, 500, 200);
            ZLayerGroup group = new ZLayerGroup(host);
            List<String> changes = new ArrayList<>();
            TextField field = group.addLayer("fields").add(new TextField.Builder(
                    context, "input", new RectF(0f, 0f, 400f, 100f))
                    .setText("breakfart")
                    .setOnTextChangedListener((id, text) -> changes.add(text)));

            field.getEditable().replace(0, field.getEditable().length(), "breakfast");
            assertEquals("breakfast", field.getText());
            assertEquals(1, changes.size());
            assertEquals("breakfast", changes.get(0));

            field.setText("breakfast h");
            assertEquals(2, changes.size());
            assertEquals("breakfast h", changes.get(1));
            group.release();
        });
    }
}

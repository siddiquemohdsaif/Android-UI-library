package app.builderx.ogfa.androiduicomponents;

import android.app.Application;

import com.ogfa.nativeviews.component.FigmaConfig;

/**
 * Demonstrates configuring one Figma reference width for the complete app.
 */
public final class NativeViewsTestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FigmaConfig.setDefault(new FigmaConfig(1080f));
    }
}

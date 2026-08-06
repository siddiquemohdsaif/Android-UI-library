package com.ogfa.nativeviews.internal.util;

import android.os.Handler;
import android.os.Looper;

public class MainThreadRunner {

    public static void run(Runnable runnable){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

}

package com.ogfa.nativeviews.internal.util.LogManager;

import com.ogfa.nativeviews.BuildConfig;

public class Log {

    private static boolean isLogEnable = BuildConfig.DEBUG;


    public static int d(String tag, String msg) {
        if (isLogEnable){
            return android.util.Log.d(tag, msg);
        }
        return 0;
    }

    public static int e(String tag, String msg) {
        if (isLogEnable){
            return android.util.Log.e(tag, msg);
        }
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (isLogEnable){
            return android.util.Log.e(tag, msg, tr);
        }
        return 0;
    }

    public static int w(String tag, String msg) {
        if (isLogEnable){
            return android.util.Log.w(tag, msg);
        }
        return 0;
    }


    public static int i(String tag, String msg) {
        if (isLogEnable){
            return android.util.Log.i(tag, msg);
        }
        return 0;
    }

}

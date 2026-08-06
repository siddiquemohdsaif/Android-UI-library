package com.ogfa.nativeviews.internal.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundRunner {

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void run(Runnable runnable){
        executor.submit(runnable);
    }

}

package com.ogfa.nativeviews.animation.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;


import com.ogfa.nativeviews.internal.util.BackgroundRunner;
import com.ogfa.nativeviews.internal.util.MainThreadRunner;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifDrawable;

public class GIFComposition {

    private Map<Integer,Bitmap> frames;
    private int noOfFrames;
    private int frameRate;
    private long duration; // in milli-second
    private GifDrawable gifDrawable;

    public GIFComposition(int frameRate, GifDrawable gifDrawable) {
        this.frameRate = frameRate;
        this.gifDrawable = gifDrawable;
        this.noOfFrames = gifDrawable.getNumberOfFrames();
        this.duration = gifDrawable.getDuration();
    }

    public GIFComposition(int frameRate, Map<Integer,Bitmap> frames) {
        this.frameRate = frameRate;
        this.frames = frames;
        this.noOfFrames = frames.size();
        //this.duration = (noOfFrames-1) * (long) (100 * ((float)frameRate/60.0)) ;
        this.duration = ((noOfFrames*1000L)/frameRate);
    }


    public static void fromAsset(Context context, final String fileName, final LoadCallback callback) {
        BackgroundRunner.run(() -> {
            try {
                GIFComposition gifComposition = fromAssetSync(context, fileName);
                MainThreadRunner.run(() -> callback.onLoaded(gifComposition));
            } catch (Exception e) {
                e.printStackTrace();
                MainThreadRunner.run(() ->  callback.onError(e.toString()));
            }
        });
    }

    /**
     * Loads a GIF directly from the parent application's assets.
     */
    public static GIFComposition fromAssetSync(Context context, String fileName)
            throws IOException {
        try (InputStream inputStream = context.getAssets().open(fileName)) {
            GifDrawable gif = new GifDrawable(inputStream);
            int frameDurationMillis = gif.getFrameDuration(0);
            int frameRate = frameDurationMillis > 0
                    ? Math.round(1000f / frameDurationMillis)
                    : 0;
            GIFComposition composition = new GIFComposition(frameRate, gif);
            composition.initializeFrames();
            return composition;
        }
    }


    public Map<Integer,Bitmap> getFrames() {
        return frames;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public long getDuration() {
        return duration;
    }

    private void initializeFrames() {
        frames = new HashMap<>();
        gifDrawable.start();
        for (int i = 0; i < noOfFrames; i++) {
            frames.put(i, gifDrawable.seekToFrameAndGet(i));
        }
        gifDrawable.stop();
    }

    public void draw(Canvas canvas, RectF rectF, long timeElapsed, Paint paint) {

        if (frames == null){
            initializeFrames();
        }

        if (noOfFrames == 0) {
            // If there are no frames, return 0 progress
            return;
        }

        // Calculate progress based on timeElapsed and duration
        float progress = (float) timeElapsed / duration;

        // Calculate the index of the frame to draw
        int frameIndex = (int) (progress * (noOfFrames - 1));

        // Ensure frameIndex is within bounds
        frameIndex = Math.max(0, Math.min(frameIndex, noOfFrames - 1));

        // Get the bitmap of the frame to draw
        Bitmap frameBitmap = frames.get(frameIndex);

        // Draw the frame on the canvas
        canvas.drawBitmap(frameBitmap, null, rectF, paint);

    }


    public interface LoadCallback{
        void onLoaded(GIFComposition gifComposition);
        void onError(String error);
    }


}

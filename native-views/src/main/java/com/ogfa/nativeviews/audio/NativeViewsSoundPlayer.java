package com.ogfa.nativeviews.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.io.IOException;

/**
 * Process-wide player for the default AnimatedButton sound.
 */
public final class NativeViewsSoundPlayer {

    private static final String TAG = "NativeViewsSoundPlayer";
    private static final String BUTTON_SOUND_ASSET =
            "nativeviews/audio/sfx/g_button.mp3";
    private static final Object LOCK = new Object();

    private static volatile Runnable buttonSoundPlayer;

    private static SoundPool soundPool;
    private static int buttonSoundId;
    private static boolean loadStarted;
    private static boolean buttonSoundLoaded;
    private static boolean playWhenLoaded;

    private NativeViewsSoundPlayer() {
    }

    /**
     * Configures an application-owned fallback. Pass {@code null} to use the bundled
     * {@code nativeviews/audio/sfx/g_button.mp3} sound again.
     */
    public static void setButtonSoundOverride(Runnable soundPlayer) {
        buttonSoundPlayer = soundPlayer;
    }

    /**
     * Starts loading the bundled button sound once per process.
     */
    public static void preload(Context context) {
        if (context == null || buttonSoundPlayer != null) {
            return;
        }

        synchronized (LOCK) {
            if (loadStarted) {
                return;
            }
            loadStarted = true;

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(attributes)
                    .build();

            SoundPool loadingPool = soundPool;
            loadingPool.setOnLoadCompleteListener((pool, sampleId, status) ->
                    onLoadComplete(loadingPool, sampleId, status));

            try (AssetFileDescriptor descriptor =
                         context.getApplicationContext()
                                 .getAssets()
                                 .openFd(BUTTON_SOUND_ASSET)) {
                buttonSoundId = loadingPool.load(descriptor, 1);
                if (buttonSoundId == 0) {
                    Log.e(TAG, "SoundPool rejected " + BUTTON_SOUND_ASSET);
                    releaseFailedLoad(loadingPool);
                }
            } catch (IOException exception) {
                Log.e(TAG, "Unable to preload " + BUTTON_SOUND_ASSET, exception);
                releaseFailedLoad(loadingPool);
            }
        }
    }

    /**
     * Plays the application callback when configured; otherwise plays the bundled
     * button sound. A click arriving while the first preload is in progress is played
     * immediately after loading completes.
     */
    public static void playButtonSound() {
        Runnable soundPlayer = buttonSoundPlayer;
        if (soundPlayer != null) {
            soundPlayer.run();
            return;
        }

        playBundledButtonSound();
    }

    /**
     * Ensures preloading has started before playing the default sound.
     */
    public static void playButtonSound(Context context) {
        Runnable soundPlayer = buttonSoundPlayer;
        if (soundPlayer != null) {
            soundPlayer.run();
            return;
        }

        preload(context);
        playBundledButtonSound();
    }

    private static void playBundledButtonSound() {
        SoundPool pool;
        int soundId;
        synchronized (LOCK) {
            if (!buttonSoundLoaded || soundPool == null || buttonSoundId == 0) {
                playWhenLoaded = loadStarted;
                return;
            }
            pool = soundPool;
            soundId = buttonSoundId;
        }

        pool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    public static boolean isButtonSoundLoaded() {
        synchronized (LOCK) {
            return buttonSoundLoaded;
        }
    }

    /**
     * Releases the process-wide SoundPool. A later preload can initialize it again.
     */
    public static void release() {
        SoundPool pool;
        synchronized (LOCK) {
            pool = soundPool;
            soundPool = null;
            buttonSoundId = 0;
            loadStarted = false;
            buttonSoundLoaded = false;
            playWhenLoaded = false;
        }
        if (pool != null) {
            pool.release();
        }
    }

    private static void onLoadComplete(SoundPool loadingPool, int sampleId, int status) {
        boolean playPendingSound = false;

        synchronized (LOCK) {
            if (soundPool != loadingPool || sampleId != buttonSoundId) {
                return;
            }

            if (status == 0) {
                buttonSoundLoaded = true;
                playPendingSound = playWhenLoaded;
                playWhenLoaded = false;
            } else {
                Log.e(TAG, "Unable to load " + BUTTON_SOUND_ASSET + "; status=" + status);
                releaseFailedLoad(loadingPool);
            }
        }

        if (playPendingSound) {
            loadingPool.play(sampleId, 1f, 1f, 1, 0, 1f);
        }
    }

    private static void releaseFailedLoad(SoundPool failedPool) {
        if (soundPool == failedPool) {
            soundPool = null;
            buttonSoundId = 0;
            loadStarted = false;
            buttonSoundLoaded = false;
            playWhenLoaded = false;
        }
        failedPool.release();
    }
}

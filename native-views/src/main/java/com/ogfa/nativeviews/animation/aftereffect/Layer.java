package com.ogfa.nativeviews.animation.aftereffect;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class Layer {

    public final Bitmap bitmap;
    public ArrayList<SoundEffect> soundEffects = new ArrayList<>();
    public final ArrayList<KeyFrameTimeLineDefinition> keyFrameTimeLineDefinitions = new ArrayList<>();

    public Layer(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void addSoundEffect(SoundEffect soundEffect){
        soundEffects.add(soundEffect);
    }

    public void playSound(long animDuration, long animationTimePassed){
        for (int i=0; i< soundEffects.size(); i++){
            soundEffects.get(i).playSound(animDuration, animationTimePassed);
        }
    }

    public void addKeyFrameTimeLineDefinition(KeyFrameAnimation keyFrameAnimation, long startTimeInMillis, long endTimeInMillis){
        keyFrameTimeLineDefinitions.add(new KeyFrameTimeLineDefinition(keyFrameAnimation, startTimeInMillis, endTimeInMillis));
    }

    public static class KeyFrameTimeLineDefinition {
        public final KeyFrameAnimation keyFrameAnimation;
        public final long startTimeInMillis;
        public final long endTimeInMillis;

        public KeyFrameTimeLineDefinition(KeyFrameAnimation keyFrameAnimation, long startTimeInMillis, long endTimeInMillis) {
            this.keyFrameAnimation = keyFrameAnimation;
            this.startTimeInMillis = startTimeInMillis;
            this.endTimeInMillis = endTimeInMillis;
        }
    }


}

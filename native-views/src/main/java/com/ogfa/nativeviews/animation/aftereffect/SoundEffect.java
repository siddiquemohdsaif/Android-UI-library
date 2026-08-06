package com.ogfa.nativeviews.animation.aftereffect;

public class SoundEffect {
    private Runnable runnable;
    private long delay;
    private long lastPlayed = 0;

    private SoundEffect(Runnable runnable, long delay, long lastPlayed) {
        this.runnable = runnable;
        this.delay = delay;
        this.lastPlayed = lastPlayed;
    }

    public static SoundEffect get(Runnable runnable, long delay){

        return new SoundEffect(runnable, delay, 0);
    }

    public void playSound(long animDuration, long animationTimePassed){
        if (animationTimePassed > delay && animationTimePassed < delay+100){

            if (System.currentTimeMillis()-lastPlayed > 110){
//                Log.d("playSound ", "lastPlayed:" + lastPlayed + " animDuration :" + animDuration + " animationTimePassed:" + animationTimePassed + " currentTime:" + System.currentTimeMillis());
                lastPlayed = System.currentTimeMillis();
                runnable.run();
            }

        }
    }

    /**
     * usage:
     * //        layer.addSoundEffect(SoundEffect.get(() -> {
     * //            SoundManager.soundManager.playSound(SoundManager.Sounds.g_pop_up,1,0);
     * //        }, 1400);
     */

}

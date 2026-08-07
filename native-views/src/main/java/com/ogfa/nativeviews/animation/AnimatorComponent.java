package com.ogfa.nativeviews.animation;

import com.ogfa.nativeviews.component.Component;

public interface AnimatorComponent extends Component {
    int INFINITE = -1;
    PlaybackState getPlaybackState();
    void play();
    void pause();
    void resume();
    void restart();
    void stop();
    void seekTo(float progress);
    float getProgress();
    AnimatorComponent setSpeed(float speed);
    float getSpeed();
    AnimatorComponent setRepeatCount(int count);
    int getRepeatCount();
    AnimatorComponent setRepeatMode(RepeatMode mode);
    RepeatMode getRepeatMode();
    boolean needsNextFrame();
}

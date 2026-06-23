package com.armor.audio;

public class SilentAudioManager extends AudioManager {
    @Override
    public void playSound(String name) {
        // Do nothing to avoid playing sounds on the server side in the web version
    }

    @Override
    public void playBGM(String name) {
        // Do nothing
    }
}

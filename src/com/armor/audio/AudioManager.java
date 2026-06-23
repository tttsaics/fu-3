package com.armor.audio;

import javax.sound.sampled.*;//加音效
import java.io.File;

public class AudioManager {
    // 用來記錄目前正在撥放的背景音樂，以便隨時停止
    public Clip bgmClip;

    // 沒有初始化要做
    public AudioManager() {
    }

    // 控制音效
    public void playSound(String name) {
        try {
            File soundFile = new File("sfx/" + name + ".wav");

            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                clip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 背景音樂
    public void playBGM(String name) {
        try {
            // 先停止上一首
            stopBGM();

            File soundFile = new File("sfx/" + name + ".wav");
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                bgmClip = AudioSystem.getClip();
                bgmClip.open(audioIn);

                // 設定連續循環播放
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);

                bgmClip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 停止背景音樂
    public void stopBGM() {
        if (bgmClip != null) {
            if (bgmClip.isRunning()) {
                bgmClip.stop(); // 停止播放
            }
            bgmClip.close(); // 釋放資源
        }
    }
}

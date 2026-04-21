package lib.manager;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import lombok.extern.slf4j.Slf4j;

/**
 * 简易音效管理器，负责加载并播放 WAV 音效。
 */
@Slf4j
public final class SoundManager {
    private final Map<String, byte[]> soundDataCache = new HashMap<>();
    private float volume = 1.0f; // 0.0 to 1.0

    public void playSound(String soundName) {
        if (volume <= 0.0f) {
            return;
        }
        String path = "/audio/" + soundName + ".wav";
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                log.warn("Sound file not found: {}", path);
                return;
            }
            
            // 使用 javax.sound.sampled 播放
            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            
            // 设置音量
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                // 确保 dB 在有效范围内
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                gainControl.setValue(Math.max(min, Math.min(max, dB)));
            }
            
            clip.start();
            // 自动关闭资源
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                    try {
                        ais.close();
                        bis.close();
                        is.close();
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception e) {
            log.error("Error playing sound: {}", soundName, e);
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public float getVolume() {
        return volume;
    }
}

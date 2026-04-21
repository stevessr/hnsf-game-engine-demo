package lib.manager;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private enum SoundCategory {
        DAMAGE,
        SHOOT,
        MENU,
        EFFECT
    }

    private final Map<String, List<String>> soundVariants = new HashMap<>();
    private final Map<String, String> soundAliases = new HashMap<>();
    private final Map<String, SoundCategory> soundCategories = new HashMap<>();
    private final Random random = new Random();
    private boolean soundEnabled = true;
    private float masterVolume = 1.0f;
    private float damageVolume = 1.0f;
    private float shootVolume = 1.0f;
    private float menuVolume = 1.0f;
    private float effectVolume = 1.0f;

    public SoundManager() {
        registerSoundGroup("hurt", SoundCategory.DAMAGE, "hurt", "damage");
        registerSoundGroup("shoot", SoundCategory.SHOOT, "shoot", "shoot_alt");
        registerSoundGroup("menu_click", SoundCategory.MENU, "menu_click", "menu_click_alt");
        registerSoundGroup("menu_back", SoundCategory.MENU, "menu_back", "menu_back_alt");
        registerSoundGroup("victory", SoundCategory.MENU, "victory", "victory_alt");
        registerSoundGroup("jump", SoundCategory.EFFECT, "jump");
        registerSoundGroup("crash", SoundCategory.EFFECT, "crash", "crash_alt");
    }

    public void playSound(String soundName) {
        String requested = normalize(soundName);
        if (requested.isBlank() || !soundEnabled || masterVolume <= 0.0f) {
            return;
        }

        List<String> candidateNames = getCandidateNames(requested);
        SoundCategory category = resolveCategory(requested);
        float effectiveVolume = Math.max(0.0f, Math.min(1.0f, masterVolume * getCategoryVolume(category)));
        if (effectiveVolume <= 0.0f) {
            return;
        }

        List<String> shuffledCandidates = new ArrayList<>(candidateNames);
        Collections.shuffle(shuffledCandidates, random);
        for (String candidate : shuffledCandidates) {
            if (tryPlayCandidate(candidate, effectiveVolume)) {
                return;
            }
        }

        log.warn("Sound file not found: /audio/{}.wav", requested);
    }

    public boolean isEnabled() {
        return soundEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public void setVolume(float volume) {
        this.masterVolume = clamp(volume);
    }

    public float getVolume() {
        return masterVolume;
    }

    public void setDamageVolume(float volume) {
        this.damageVolume = clamp(volume);
    }

    public float getDamageVolume() {
        return damageVolume;
    }

    public void setShootVolume(float volume) {
        this.shootVolume = clamp(volume);
    }

    public float getShootVolume() {
        return shootVolume;
    }

    public void setMenuVolume(float volume) {
        this.menuVolume = clamp(volume);
    }

    public float getMenuVolume() {
        return menuVolume;
    }

    public void setEffectVolume(float volume) {
        this.effectVolume = clamp(volume);
    }

    public float getEffectVolume() {
        return effectVolume;
    }

    private void registerSoundGroup(String canonicalName, SoundCategory category, String... variants) {
        String canonical = normalize(canonicalName);
        List<String> normalizedVariants = new ArrayList<>();
        if (variants != null) {
            for (String variant : variants) {
                String normalized = normalize(variant);
                if (normalized.isBlank() || normalizedVariants.contains(normalized)) {
                    continue;
                }
                normalizedVariants.add(normalized);
                soundAliases.put(normalized, canonical);
                soundCategories.put(normalized, category);
            }
        }
        if (normalizedVariants.isEmpty()) {
            normalizedVariants.add(canonical);
        } else if (!normalizedVariants.contains(canonical)) {
            normalizedVariants.add(0, canonical);
        }
        soundAliases.put(canonical, canonical);
        soundVariants.put(canonical, List.copyOf(normalizedVariants));
        soundCategories.put(canonical, category);
    }

    private List<String> getCandidateNames(String requestedName) {
        String canonical = soundAliases.getOrDefault(requestedName, requestedName);
        List<String> variants = soundVariants.get(canonical);
        if (variants == null || variants.isEmpty()) {
            return List.of(requestedName);
        }
        return variants;
    }

    private SoundCategory resolveCategory(String requestedName) {
        String canonical = soundAliases.getOrDefault(requestedName, requestedName);
        return soundCategories.getOrDefault(canonical, SoundCategory.EFFECT);
    }

    private float getCategoryVolume(SoundCategory category) {
        return switch (category) {
            case DAMAGE -> damageVolume;
            case SHOOT -> shootVolume;
            case MENU -> menuVolume;
            case EFFECT -> effectVolume;
        };
    }

    private boolean tryPlayCandidate(String soundName, float volume) {
        String path = "/audio/" + soundName + ".wav";
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                return false;
            }

            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(Math.max(0.0001f, volume)) / Math.log(10.0) * 20.0);
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                gainControl.setValue(Math.max(min, Math.min(max, dB)));
            }

            clip.start();
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                    try {
                        ais.close();
                        bis.close();
                        is.close();
                    } catch (Exception ignored) {
                    }
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Error playing sound: {}", soundName, e);
            return false;
        }
    }

    private float clamp(float volume) {
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    private String normalize(String soundName) {
        if (soundName == null) {
            return "";
        }
        return soundName.trim();
    }
}

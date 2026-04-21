package lib.manager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundManagerTest {
    @Test
    void audioControlsShouldClampAndToggle() {
        SoundManager soundManager = new SoundManager();

        assertTrue(soundManager.isEnabled());
        soundManager.setEnabled(false);
        assertFalse(soundManager.isEnabled());

        soundManager.setVolume(1.5f);
        assertEquals(1.0f, soundManager.getVolume(), 1e-6f);

        soundManager.setDamageVolume(-0.25f);
        assertEquals(0.0f, soundManager.getDamageVolume(), 1e-6f);

        soundManager.setShootVolume(0.4f);
        assertEquals(0.4f, soundManager.getShootVolume(), 1e-6f);

        soundManager.setMenuVolume(0.6f);
        assertEquals(0.6f, soundManager.getMenuVolume(), 1e-6f);

        soundManager.setEffectVolume(0.8f);
        assertEquals(0.8f, soundManager.getEffectVolume(), 1e-6f);
    }

    @Test
    void missingSoundShouldNotCrashWhenMutedOrUnavailable() {
        SoundManager soundManager = new SoundManager();
        assertDoesNotThrow(() -> soundManager.playSound("definitely_missing_sound"));

        soundManager.setEnabled(false);
        assertDoesNotThrow(() -> soundManager.playSound("menu_click"));
    }

    @Test
    void windSoundResourceShouldBePackaged() {
        assertNotNull(getClass().getResource("/audio/wind.wav"));
    }

    @Test
    void healSoundResourceShouldBePackaged() {
        assertNotNull(getClass().getResource("/audio/heal.wav"));
    }
}

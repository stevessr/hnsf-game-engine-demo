package lib.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class SettingsRepositoryTest {
    @Test
    void shouldPersistExtendedAudioSettings() throws Exception {
        Path tempDir = Files.createTempDirectory("settings-repository-test");
        Path settingsFile = tempDir.resolve("settings.json");
        SettingsRepository repository = new SettingsRepository(settingsFile);

        JSONObject keyBindings = new JSONObject();
        keyBindings.put("DUMMY", new org.json.JSONArray().put(1).put(2));

        repository.saveSettings(
            144,
            20,
            1280,
            720,
            800,
            95,
            true,
            false,
            true,
            0.3f,
            1.4f,
            0.75f,
            false,
            0.5f,
            0.6f,
            0.7f,
            0.8f,
            keyBindings
        );

        JSONObject loaded = repository.loadSettings();
        assertEquals(144, loaded.getInt("targetFPS"));
        assertEquals(20, loaded.getInt("uiFontSize"));
        assertEquals(1280, loaded.getInt("width"));
        assertEquals(720, loaded.getInt("height"));
        assertEquals(800, loaded.getInt("throttlePower"));
        assertEquals(95, loaded.getInt("deceleration"));
        assertTrue(loaded.getBoolean("gravityEnabled"));
        assertFalse(loaded.getBoolean("lightingEnabled"));
        assertTrue(loaded.getBoolean("debugEnabled"));
        assertEquals(0.3f, (float) loaded.getDouble("ambientLight"), 1e-6f);
        assertEquals(1.4f, (float) loaded.getDouble("lightingIntensity"), 1e-6f);
        assertEquals(0.75f, (float) loaded.getDouble("volume"), 1e-6f);
        assertFalse(loaded.getBoolean("soundEnabled"));
        assertEquals(0.5f, (float) loaded.getDouble("damageVolume"), 1e-6f);
        assertEquals(0.6f, (float) loaded.getDouble("shootVolume"), 1e-6f);
        assertEquals(0.7f, (float) loaded.getDouble("menuVolume"), 1e-6f);
        assertEquals(0.8f, (float) loaded.getDouble("effectVolume"), 1e-6f);
        assertEquals(2, loaded.getJSONObject("keyBindings").getJSONArray("DUMMY").length());
    }
}

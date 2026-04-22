package lib.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Color;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lib.object.dto.MapBackgroundMode;
import lib.object.dto.MapBackgroundPreset;
import lib.object.dto.MapData;

public class MapRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    public void testSaveAndLoadBackgroundPreset() {
        MapRepository repository = new MapRepository(tempDir.resolve("maps.db"));

        MapData mapData = new MapData();
        mapData.setName("preset-map");
        mapData.setWidth(320);
        mapData.setHeight(200);
        mapData.setBackgroundColor(new Color(54, 92, 56));
        mapData.setBackgroundPreset(MapBackgroundPreset.FOREST);
        mapData.setBackgroundMode(MapBackgroundMode.GRADIENT);

        long mapId = repository.saveMap(mapData);
        assertEquals(mapId, mapData.getId());

        MapData loaded = repository.loadMapById(mapId);
        assertNotNull(loaded);
        assertEquals("preset-map", loaded.getName());
        assertEquals(MapBackgroundPreset.FOREST, loaded.getBackgroundPreset());
        assertEquals(MapBackgroundMode.GRADIENT, loaded.getBackgroundMode());
        assertEquals(new Color(54, 92, 56).getRGB(), loaded.getBackgroundColor().getRGB());
    }
}

package lib.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import lib.game.GameWorld;
import lib.object.GameObjectType;
import lib.object.PlayerObject;
import lib.object.dto.MapBackgroundMode;
import lib.object.dto.MapBackgroundPreset;
import lib.object.dto.MapData;
import lib.object.dto.ObjectData;

public class MapDataMapperTest {

    @Test
    public void testFullCycleTransformation() {
        // 1. Create a GameWorld with some objects
        GameWorld world = new GameWorld(1200, 800, Color.BLACK);
        world.setGravityEnabled(true);
        world.setGravityStrength(1000);
        
        ObjectData wallData = new ObjectData();
        wallData.setType(GameObjectType.WALL);
        wallData.setName("test-wall");
        wallData.setX(10);
        wallData.setY(20);
        wallData.setWidth(100);
        wallData.setHeight(50);
        wallData.setColor(Color.RED);
        wallData.setSolid(true);
        
        MapData originalData = new MapData();
        originalData.setName("test-map");
        originalData.setWidth(1200);
        originalData.setHeight(800);
        originalData.setBackgroundColor(Color.BLACK);
        originalData.setGravityEnabled(true);
        originalData.setGravityStrength(1000);
        originalData.addObject(wallData);

        // 2. Transform to World and back
        GameWorld createdWorld = MapDataMapper.toWorld(originalData);
        assertEquals(1200, createdWorld.getWidth());
        assertEquals(Color.BLACK, createdWorld.getBackgroundColor());
        assertEquals(1, createdWorld.getObjects().size());
        
        MapData mappedBack = MapDataMapper.fromWorld(createdWorld, "test-map");
        assertEquals("test-map", mappedBack.getName());
        assertEquals(originalData.getGravityStrength(), mappedBack.getGravityStrength());
        assertEquals(1, mappedBack.getObjects().size());
        assertEquals("test-wall", mappedBack.getObjects().get(0).getName());

        // 3. Export to JSON and Import
        JSONObject json = MapDataMapper.exportToJson(mappedBack);
        assertNotNull(json);
        assertEquals("test-map", json.getString("name"));
        
        MapData importedData = MapDataMapper.importFromJson(json);
        assertEquals(mappedBack.getName(), importedData.getName());
        assertEquals(mappedBack.getWidth(), importedData.getWidth());
        assertEquals(mappedBack.getBackgroundColor().getRGB(), importedData.getBackgroundColor().getRGB());
        assertEquals(MapBackgroundMode.GRADIENT, importedData.getBackgroundMode());
        assertEquals(1, importedData.getObjects().size());
        assertEquals("test-wall", importedData.getObjects().get(0).getName());
    }

    @Test
    public void testEmptyWorld() {
        GameWorld world = new GameWorld(800, 600, Color.WHITE);
        MapData data = MapDataMapper.fromWorld(world, "empty");
        assertEquals(0, data.getObjects().size());
        
        GameWorld restored = MapDataMapper.toWorld(data);
        assertEquals(0, restored.getObjects().size());
        assertEquals(800, restored.getWidth());
    }

    @Test
    public void testApplyToWorldShouldResetExplorationFog() {
        GameWorld world = new GameWorld(200, 200, Color.WHITE);
        world.getLightingManager().setEnabled(true);
        world.getLightingManager().setAmbientLight(0.0f);

        PlayerObject initialPlayer = new PlayerObject("hero", 60, 60);
        initialPlayer.setLightRadius(40);
        world.addObject(initialPlayer);

        BufferedImage firstImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D firstGraphics = firstImage.createGraphics();
        try {
            world.render(firstGraphics);
        } finally {
            firstGraphics.dispose();
        }

        Color exploredPixel = new Color(firstImage.getRGB(60, 60), true);
        assertNotEquals(Color.BLACK.getRGB(), exploredPixel.getRGB(), "第一次渲染后旧探索区域应被点亮");

        MapData nextMap = new MapData();
        nextMap.setWidth(200);
        nextMap.setHeight(200);
        nextMap.setBackgroundColor(Color.RED);
        ObjectData nextPlayer = new ObjectData();
        nextPlayer.setType(GameObjectType.PLAYER);
        nextPlayer.setName("hero");
        nextPlayer.setX(160);
        nextPlayer.setY(160);
        nextPlayer.setWidth(48);
        nextPlayer.setHeight(48);
        nextPlayer.setColor(Color.BLUE);
        nextMap.addObject(nextPlayer);

        MapDataMapper.applyToWorld(world, nextMap);
        world.findPlayer().orElseThrow().setLightRadius(40);

        BufferedImage secondImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D secondGraphics = secondImage.createGraphics();
        try {
            world.render(secondGraphics);
        } finally {
            secondGraphics.dispose();
        }

        Color resetPixel = new Color(secondImage.getRGB(60, 60), true);
        Color playerPixel = new Color(secondImage.getRGB(160, 160), true);
        assertEquals(Color.BLACK.getRGB(), resetPixel.getRGB(), "切换地图后未解锁区域应恢复为黑色");
        assertNotEquals(Color.BLACK.getRGB(), playerPixel.getRGB(), "新玩家附近应保持可见");
    }

    @Test
    public void testBackgroundImageRoundTripThroughMapper() throws Exception {
        GameWorld world = new GameWorld(80, 60, Color.BLACK);
        world.setBackgroundMode(MapBackgroundMode.IMAGE);

        BufferedImage background = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        background.setRGB(0, 0, new Color(240, 80, 80).getRGB());
        background.setRGB(1, 0, new Color(80, 240, 120).getRGB());
        background.setRGB(0, 1, new Color(70, 120, 240).getRGB());
        background.setRGB(1, 1, new Color(240, 220, 90).getRGB());
        world.setBackgroundImage(background, "custom-bg.png");

        MapData mapData = MapDataMapper.fromWorld(world, "custom");
        assertEquals(MapBackgroundMode.IMAGE, mapData.getBackgroundMode());
        assertNotNull(mapData.getBackgroundImageData());
        assertEquals("custom-bg.png", mapData.getBackgroundImageName());

        GameWorld restored = MapDataMapper.toWorld(mapData);
        assertEquals(MapBackgroundMode.IMAGE, restored.getBackgroundMode());
        assertEquals("custom-bg.png", restored.getBackgroundImageName());
        assertNotNull(restored.getBackgroundImage());
        assertEquals(background.getRGB(0, 0), restored.getBackgroundImage().getRGB(0, 0));
        assertEquals(background.getRGB(1, 1), restored.getBackgroundImage().getRGB(1, 1));
    }

    @Test
    public void testBackgroundPresetRoundTripThroughMapper() {
        GameWorld world = new GameWorld(80, 60, new Color(54, 92, 56));
        world.setBackgroundPreset(MapBackgroundPreset.FOREST);

        MapData mapData = MapDataMapper.fromWorld(world, "forest");
        assertEquals(MapBackgroundPreset.FOREST, mapData.getBackgroundPreset());

        JSONObject json = MapDataMapper.exportToJson(mapData);
        assertEquals(MapBackgroundPreset.FOREST.name(), json.getString("backgroundPreset"));

        MapData imported = MapDataMapper.importFromJson(json);
        assertEquals(MapBackgroundPreset.FOREST, imported.getBackgroundPreset());

        GameWorld restored = MapDataMapper.toWorld(imported);
        assertEquals(MapBackgroundPreset.FOREST, restored.getBackgroundPreset());
    }

    @Test
    public void testGradientBackgroundShouldRenderThemeColors() {
        GameWorld world = new GameWorld(40, 80, new Color(90, 150, 90));
        world.setBackgroundMode(MapBackgroundMode.GRADIENT);

        BufferedImage image = new BufferedImage(40, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            world.render(graphics);
        } finally {
            graphics.dispose();
        }

        Color top = new Color(image.getRGB(20, 4), true);
        Color bottom = new Color(image.getRGB(20, 74), true);
        assertNotEquals(top.getRGB(), bottom.getRGB(), "渐变背景顶部和底部颜色应不同");
    }

    @Test
    public void testApplyToWorldShouldPreservePlayerMovementSettings() {
        GameWorld world = new GameWorld(200, 200, Color.WHITE);
        PlayerObject oldPlayer = new PlayerObject("hero", 20, 20);
        oldPlayer.setThrottlePower(1000);
        oldPlayer.setDeceleration(0.88);
        world.addObject(oldPlayer);

        MapData nextMap = new MapData();
        nextMap.setWidth(200);
        nextMap.setHeight(200);
        nextMap.setBackgroundColor(Color.GRAY);

        ObjectData nextPlayer = new ObjectData();
        nextPlayer.setType(GameObjectType.PLAYER);
        nextPlayer.setName("hero");
        nextPlayer.setX(40);
        nextPlayer.setY(40);
        nextPlayer.setWidth(48);
        nextPlayer.setHeight(48);
        nextPlayer.setColor(Color.BLUE);
        nextMap.addObject(nextPlayer);

        MapDataMapper.applyToWorld(world, nextMap);

        PlayerObject restoredPlayer = world.findPlayer().orElseThrow();
        assertEquals(1000, restoredPlayer.getThrottlePower(), "切换地图后应保留玩家油门力度");
        assertEquals(88, restoredPlayer.getDecelerationPercent(), "切换地图后应保留玩家减速度");
    }
}

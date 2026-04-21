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
}

package lib.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.game.GameWorld;
import org.junit.jupiter.api.Test;
import lib.object.dto.MapData;
import lib.object.GameObjectType;
import lib.object.PlayerObject;
import lib.persistence.MapDataMapper;

public class ProceduralLevelGeneratorTest {

    @Test
    public void testForestGeneration() {
        MapData map = ProceduralLevelGenerator.generateForest("test-forest", 12345L);
        assertNotNull(map);
        assertEquals("test-forest", map.getName());
        assertTrue(map.getObjects().size() > 10, "Forest should have many objects");
        
        // Ensure player and exit exist
        boolean hasPlayer = map.getObjects().stream().anyMatch(o -> o.getType() == GameObjectType.PLAYER);
        boolean hasExit = map.getObjects().stream().anyMatch(o -> o.getType() == GameObjectType.GOAL);
        
        assertTrue(hasPlayer, "Forest should have a player");
        assertTrue(hasExit, "Forest should have an exit");
    }

    @Test
    public void testForestSpawnShouldNotStartInsideGround() {
        MapData map = ProceduralLevelGenerator.generateForest("test-forest", 12345L);
        GameWorld world = MapDataMapper.toWorld(map);
        PlayerObject player = world.findPlayer().orElseThrow();

        assertFalse(world.collidesWithSolid(player, player.getX(), player.getY()), "程序化森林出生点不应嵌在地面里");
    }

    @Test
    public void testCaveGeneration() {
        MapData map = ProceduralLevelGenerator.generateCave("test-cave", 67890L);
        assertNotNull(map);
        assertTrue(map.getObjects().size() > 50, "Cave should have many wall objects");
    }
}

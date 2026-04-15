package lib.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.object.BoundaryObject;
import lib.object.PlayerObject;
import lib.object.WallObject;

class GameWorldPhysicsTest {
    @Test
    void playerShouldStopAtWallEdgeInsteadOfPassingThrough() {
        GameWorld world = new GameWorld(200, 120);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setFriction(1.0);
        WallObject wall = new WallObject("wall", 60, 20, 20, 48);

        world.addObject(player);
        world.addObject(wall);
        player.setVelocity(100, 0);

        world.update(1.0);

        assertEquals(12, player.getX());
        assertFalse(world.collidesWithSolid(player, player.getX(), player.getY()));
    }

    @Test
    void worldShouldReportBlockedSolidPositions() {
        GameWorld world = new GameWorld(120, 90);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        BoundaryObject boundary = BoundaryObject.right(120, 90, 10);

        world.addObject(player);
        world.addObject(boundary);

        assertFalse(world.collidesWithSolid(player, 50, 20));
        assertTrue(world.collidesWithSolid(player, 70, 20));
    }
}
package lib.game;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.object.BoundaryObject;
import lib.object.VoxelObject;
import lib.object.MonsterObject;
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

    @Test
    void damagedPlayerShouldNotClipThroughWallDuringKnockback() {
        GameWorld world = new GameWorld(160, 120);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        MonsterObject monster = new MonsterObject("slime", 0, 20, 0);
        WallObject wall = new WallObject("wall", 60, 20, 20, 48);

        monster.setAggressive(false);
        monster.setSpeed(0);

        world.addObject(player);
        world.addObject(monster);
        world.addObject(wall);

        world.update(1.0 / 60.0);

        assertEquals(12, player.getX(), "击退应被墙体阻挡在墙边");
        assertFalse(world.collidesWithSolid(player, player.getX(), player.getY()));
    }

    @Test
    void gravityShouldAcceleratePlayerDownwardWhenEnabled() {
        GameWorld world = new GameWorld(200, 1000);
        world.setGravityEnabled(true);
        world.setGravityStrength(600);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setFriction(1.0);

        world.addObject(player);

        world.update(1.0);

        assertTrue(player.getY() > 20, "启用重力后角色应向下移动");
    }

    @Test
    void complementaryColorSolidBlockShouldDamagePlayer() {
        GameWorld world = new GameWorld(200, 120);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setFriction(1.0);
        player.setColor(new Color(66, 135, 245));
        VoxelObject voxel = new VoxelObject("hazard", 60, 20, 20, new Color(255, 168, 72));

        world.addObject(player);
        world.addObject(voxel);
        player.setVelocity(100, 0);

        world.update(1.0);

        assertEquals(12, player.getX(), "补色体素应阻挡角色前进");
        assertTrue(player.getHealth() < 120, "补色接触应造成掉血");
    }
}

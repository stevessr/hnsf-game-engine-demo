package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import lib.game.GameWorld;

class GameObjectModelTest {
    @Test
    void playerShouldLevelUpMultipleTimesAfterGainingExperience() {
        PlayerObject player = new PlayerObject("hero", 10, 20);

        player.gainExperience(350);

        assertEquals(GameObjectType.PLAYER, player.getType());
        assertEquals(3, player.getLevel());
        assertEquals(50, player.getExperience());
        assertEquals(10, player.getX());
        assertEquals(20, player.getY());
    }

    @Test
    void monsterShouldDeactivateAfterTakingFatalDamage() {
        MonsterObject monster = new MonsterObject("slime", 3, 4, 25);

        monster.takeDamage(1000);

        assertEquals(GameObjectType.MONSTER, monster.getType());
        assertEquals(0, monster.getHealth());
        assertFalse(monster.isActive());
        assertFalse(monster.canAttack());
        assertEquals(25, monster.getRewardExperience());
    }

    @Test
    void sceneShouldNormalizeSharedProperties() {
        SceneObject scene = new SceneObject("", 1, 2, -30, -40, true, true);

        scene.setColor(null);
        scene.moveBy(5, 6);

        assertEquals(GameObjectType.SCENE, scene.getType());
        assertEquals("object", scene.getName());
        assertEquals(0, scene.getWidth());
        assertEquals(0, scene.getHeight());
        assertEquals(6, scene.getX());
        assertEquals(8, scene.getY());
        assertEquals(Color.WHITE, scene.getColor());
        assertTrue(scene.isSolid());
        assertTrue(scene.isBackground());
    }

    @Test
    void playerShouldUpdatePositionAndRenderColor() {
        GameWorld world = new GameWorld(200, 160);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setVelocity(30, 15);

        player.update(world, 2.0);

        assertEquals(70, player.getX());
        assertEquals(50, player.getY());

        BufferedImage image = new BufferedImage(200, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            player.render(graphics);
        } finally {
            graphics.dispose();
        }

        int centerX = player.getX() + (player.getWidth() / 2);
        int centerY = player.getY() + (player.getHeight() / 2);
        assertEquals(player.getColor().getRGB(), image.getRGB(centerX, centerY));
    }

    @Test
    void monsterShouldPatrolWithinWorldBounds() {
        GameWorld world = new GameWorld(120, 90);
        MonsterObject monster = new MonsterObject("slime", 60, 10, 25);

        int originalX = monster.getX();
        world.addObject(monster);
        world.update(1.0);

        assertTrue(monster.getX() > originalX);

        for (int index = 0; index < 50; index++) {
            world.update(1.0);
        }

        assertTrue(monster.getX() >= 0);
        assertTrue(monster.getX() <= world.getWidth() - monster.getWidth());
    }

    @Test
    void worldShouldRenderSceneObjectOverBackground() {
        GameWorld world = new GameWorld(80, 80, Color.BLACK);
        SceneObject scene = new SceneObject("wall", 10, 10, 20, 20, true, false);
        scene.setColor(new Color(10, 200, 100));
        world.addObject(scene);

        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            world.render(graphics);
        } finally {
            graphics.dispose();
        }

        assertEquals(Color.BLACK.getRGB(), image.getRGB(0, 0));
        assertEquals(scene.getColor().getRGB(), image.getRGB(15, 15));
    }
}
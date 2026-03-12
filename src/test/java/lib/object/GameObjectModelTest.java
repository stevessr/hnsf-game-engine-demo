package lib.object;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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
}
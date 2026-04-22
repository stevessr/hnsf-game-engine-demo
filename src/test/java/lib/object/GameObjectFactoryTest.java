package lib.object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import lib.object.dto.ObjectData;

public class GameObjectFactoryTest {

    @Test
    public void testPlayerSerialization() {
        PlayerObject player = new PlayerObject("Hero", 100, 200);
        player.setHealth(50);
        player.setLevel(5);
        
        ObjectData data = GameObjectFactory.toObjectData(player);
        assertEquals(GameObjectType.PLAYER, data.getType());
        assertTrue(data.getExtraJson().contains("\"health\":50"));
        assertTrue(data.getExtraJson().contains("\"level\":5"));
        
        PlayerObject restored = (PlayerObject) GameObjectFactory.fromObjectData(data);
        assertEquals("Hero", restored.getName());
        assertEquals(100, restored.getX());
        assertEquals(200, restored.getY());
        assertEquals(50, restored.getHealth());
        assertEquals(5, restored.getLevel());
    }

    @Test
    public void testMonsterSerialization() {
        MonsterObject monster = new MonsterObject("Griz", 300, 400, 100);
        monster.setMonsterKind(MonsterKind.BAT);
        monster.setAggressive(true);
        monster.setAttack(15);
        monster.setHealDropAmount(20);
        monster.setRangedAttacker(true);
        monster.setShootRange(420);
        monster.setProjectileSpeed(500);
        monster.setShootCooldown(0.8);
        monster.setBomber(true);
        monster.setBombRadius(96);
        monster.setGravityPercent(50);
        monster.setRevivable(true);
        monster.setReviveDelaySeconds(7.5);
        
        ObjectData data = GameObjectFactory.toObjectData(monster);
        assertEquals(GameObjectType.MONSTER, data.getType());
        
        MonsterObject restored = (MonsterObject) GameObjectFactory.fromObjectData(data);
        assertEquals(MonsterKind.BAT, restored.getMonsterKind());
        assertEquals(100, restored.getRewardExperience());
        assertTrue(restored.isAggressive());
        assertEquals(15, restored.getAttack());
        assertEquals(20, restored.getHealDropAmount());
        assertTrue(restored.isRangedAttacker());
        assertEquals(420, restored.getShootRange());
        assertEquals(500, restored.getProjectileSpeed());
        assertEquals(0.8, restored.getShootCooldown());
        assertTrue(restored.isAirborne());
        assertTrue(restored.isBomber());
        assertEquals(96, restored.getBombRadius());
        assertEquals(50, restored.getGravityPercent());
        assertTrue(restored.isRevivable());
        assertEquals(7.5, restored.getReviveDelaySeconds());
    }

    @Test
    public void testGhostMonsterSerialization() {
        MonsterObject monster = new MonsterObject("Haunt", 120, 220, 80);
        monster.setMonsterKind(MonsterKind.GHOST);
        monster.setAggressive(true);
        monster.setGravityPercent(10);

        ObjectData data = GameObjectFactory.toObjectData(monster);
        MonsterObject restored = (MonsterObject) GameObjectFactory.fromObjectData(data);

        assertEquals(MonsterKind.GHOST, restored.getMonsterKind());
        assertTrue(restored.isAirborne());
        assertEquals(10, restored.getGravityPercent());
        assertEquals(monster.getColor(), restored.getColor());
    }

    @Test
    public void testItemSerialization() {
        ItemObject item = new ItemObject("Coin", 50, 50, 32, 32, "gold", 50, "You found gold!");
        
        ObjectData data = GameObjectFactory.toObjectData(item);
        assertEquals(GameObjectType.ITEM, data.getType());
        
        ItemObject restored = (ItemObject) GameObjectFactory.fromObjectData(data);
        assertEquals("gold", restored.getKind());
        assertEquals(50, restored.getValue());
        assertEquals("You found gold!", restored.getMessage());
    }

    @Test
    public void testLightSourceItemSerialization() {
        ItemObject item = new ItemObject("Orb", 50, 50, 32, 32, "lightorb", 50, "Vision!");

        ObjectData data = GameObjectFactory.toObjectData(item);
        ItemObject restored = (ItemObject) GameObjectFactory.fromObjectData(data);

        assertTrue(restored.isRenewable());
        assertEquals(15.0, restored.getRespawnDelaySeconds());
    }

    @Test
    public void testMenuSerialization() {
        MenuObject menu = new MenuObject("Options", 0, 0, 200, 150, "Settings", List.of("Easy", "Hard"));
        menu.setSelectedIndex(1);
        menu.setSubtitle("Cause: demo");
        
        ObjectData data = GameObjectFactory.toObjectData(menu);
        assertEquals(GameObjectType.MENU, data.getType());
        
        MenuObject restored = (MenuObject) GameObjectFactory.fromObjectData(data);
        assertEquals("Settings", restored.getTitle());
        assertEquals("Cause: demo", restored.getSubtitle());
        assertEquals(2, restored.getOptions().size());
        assertEquals(1, restored.getSelectedIndex());
    }

    @Test
    public void testSceneSerialization() {
        SceneObject scene = new SceneObject("crate", 20, 30, 48, 48, true, false);
        scene.setDestructible(true);
        scene.setDurability(35);
        scene.setCollapseWhenUnsupported(true);
        scene.setCollapseDamage(42);
        scene.setBreakAfterSteps(3);

        ObjectData data = GameObjectFactory.toObjectData(scene);
        SceneObject restored = (SceneObject) GameObjectFactory.fromObjectData(data);

        assertTrue(restored.isDestructible());
        assertEquals(35, restored.getDurability());
        assertTrue(restored.isCollapseWhenUnsupported());
        assertEquals(42, restored.getCollapseDamage());
        assertEquals(3, restored.getBreakAfterSteps());
    }
}

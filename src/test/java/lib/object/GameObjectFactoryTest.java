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
        player.setProjectileType(ProjectileType.FLARE);
        
        ObjectData data = GameObjectFactory.toObjectData(player);
        assertEquals(GameObjectType.PLAYER, data.getType());
        assertTrue(data.getExtraJson().contains("\"health\":50"));
        assertTrue(data.getExtraJson().contains("\"level\":5"));
        assertTrue(data.getExtraJson().contains("\"projectileType\":\"FLARE\""));
        
        PlayerObject restored = (PlayerObject) GameObjectFactory.fromObjectData(data);
        assertEquals("Hero", restored.getName());
        assertEquals(100, restored.getX());
        assertEquals(200, restored.getY());
        assertEquals(50, restored.getHealth());
        assertEquals(5, restored.getLevel());
        assertEquals(ProjectileType.FLARE, restored.getProjectileType());
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
    public void testTriggerSerialization() {
        TriggerObject trigger = new TriggerObject("GateTrigger", 40, 60, 96, 64);
        trigger.setTargetName("gate-door");
        trigger.setAction(TriggerAction.ACTIVATE);
        trigger.setTriggerOnce(true);

        ObjectData data = GameObjectFactory.toObjectData(trigger);
        assertEquals(GameObjectType.TRIGGER, data.getType());
        assertTrue(data.getExtraJson().contains("\"targetName\":\"gate-door\""));
        assertTrue(data.getExtraJson().contains("\"action\":\"ACTIVATE\""));

        TriggerObject restored = (TriggerObject) GameObjectFactory.fromObjectData(data);
        assertEquals("gate-door", restored.getTargetName());
        assertEquals(TriggerAction.ACTIVATE, restored.getAction());
        assertTrue(restored.isTriggerOnce());
    }

    @Test
    public void testWorldTriggerActionSerialization() {
        TriggerObject trigger = new TriggerObject("RespawnTrigger", 40, 60, 96, 64);
        trigger.setAction(TriggerAction.RESPAWN_PLAYER);

        ObjectData data = GameObjectFactory.toObjectData(trigger);
        assertTrue(data.getExtraJson().contains("\"action\":\"RESPAWN_PLAYER\""));

        TriggerObject restored = (TriggerObject) GameObjectFactory.fromObjectData(data);
        assertEquals(TriggerAction.RESPAWN_PLAYER, restored.getAction());
    }

    @Test
    public void testSpawnerSerialization() {
        SpawnerObject spawner = new SpawnerObject("SlimeSpawner", 80, 90, 64, 64);
        spawner.setMonsterKind(MonsterKind.SLIME);
        spawner.setSpawnIntervalSeconds(2.5);
        spawner.setMaxAlive(4);
        spawner.setSpawnWaveSize(3);
        spawner.setSpawnRadius(48);
        spawner.setSpawnOffsetX(12);
        spawner.setSpawnOffsetY(-6);
        String spawnGroupId = spawner.getSpawnGroupId();

        ObjectData data = GameObjectFactory.toObjectData(spawner);
        assertEquals(GameObjectType.SPAWNER, data.getType());
        assertTrue(data.getExtraJson().contains("\"monsterKind\":\"SLIME\""));
        assertTrue(data.getExtraJson().contains("\"spawnWaveSize\":3"));
        assertTrue(data.getExtraJson().contains("\"spawnRadius\":48"));
        assertTrue(data.getExtraJson().contains("\"spawnGroupId\""));

        SpawnerObject restored = (SpawnerObject) GameObjectFactory.fromObjectData(data);
        assertEquals(MonsterKind.SLIME, restored.getMonsterKind());
        assertEquals(2.5, restored.getSpawnIntervalSeconds());
        assertEquals(4, restored.getMaxAlive());
        assertEquals(3, restored.getSpawnWaveSize());
        assertEquals(48, restored.getSpawnRadius());
        assertEquals(12, restored.getSpawnOffsetX());
        assertEquals(-6, restored.getSpawnOffsetY());
        assertEquals(spawnGroupId, restored.getSpawnGroupId());
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
    public void testProjectileSerializationShouldPreserveTypeAndVelocity() {
        ProjectileObject projectile = new ProjectileObject("flare", 12, 24, 320, -48, 7, null, ProjectileType.FLARE);

        ObjectData data = GameObjectFactory.toObjectData(projectile);
        assertEquals(GameObjectType.PROJECTILE, data.getType());
        assertTrue(data.getExtraJson().contains("\"projectileType\":\"FLARE\""));

        ProjectileObject restored = (ProjectileObject) GameObjectFactory.fromObjectData(data);
        assertEquals(ProjectileType.FLARE, restored.getProjectileType());
        assertEquals(320.0, restored.getVelocityX());
        assertEquals(-48.0, restored.getVelocityY());
        assertEquals(7, restored.getDamage());
    }

    @Test
    public void testLaserProjectileSerializationShouldPreserveType() {
        ProjectileObject projectile = new ProjectileObject("laser", 12, 24, 320, -48, 7, null, ProjectileType.LASER);

        ObjectData data = GameObjectFactory.toObjectData(projectile);
        assertTrue(data.getExtraJson().contains("\"projectileType\":\"LASER\""));

        ProjectileObject restored = (ProjectileObject) GameObjectFactory.fromObjectData(data);
        assertEquals(ProjectileType.LASER, restored.getProjectileType());
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

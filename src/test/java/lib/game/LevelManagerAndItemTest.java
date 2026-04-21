package lib.game;

import java.awt.Color;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectFactory;
import lib.object.GameObjectType;
import lib.object.ItemObject;
import lib.object.MenuObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.object.VoxelObject;
import lib.object.dto.MapData;
import lib.object.dto.ObjectData;
import lib.persistence.MapDataMapper;

class LevelManagerAndItemTest {
    @Test
    void builtinLevelsShouldExposeMoreThanOnePlayableMap() {
        LevelManager levelManager = new LevelManager();

        List<String> levelNames = levelManager.getLevelNames();

        assertTrue(levelNames.contains("demo-map"));
        assertTrue(levelNames.contains("level-1"));
        assertTrue(levelNames.contains("level-2"));
        assertTrue(levelNames.contains("level-3"));
        assertTrue(levelNames.contains("level-4"));
        assertTrue(levelNames.contains("air-raid-demo"));
    }

    @Test
    void builtinLevelShouldContainItemsAndEnemies() {
        LevelManager levelManager = new LevelManager();
        GameWorld world = MapDataMapper.toWorld(levelManager.createLevelData("demo-map"));

        assertFalse(world.getObjectsByType(GameObjectType.ITEM).isEmpty(), "关卡应包含可拾取物品");
        assertFalse(world.getObjectsByType(GameObjectType.MONSTER).isEmpty(), "关卡应包含敌人");
    }

    @Test
    void builtinLevelsShouldExposeVoxelsAndGravity() {
        LevelManager levelManager = new LevelManager();
        GameWorld demoWorld = MapDataMapper.toWorld(levelManager.createLevelData("demo-map"));
        GameWorld gravityWorld = MapDataMapper.toWorld(levelManager.createLevelData("level-3"));

        assertFalse(demoWorld.getObjectsByType(GameObjectType.VOXEL).isEmpty(), "演示关卡应包含体素方块");
        assertTrue(gravityWorld.isGravityEnabled(), "部分内置关卡应开启重力");
    }

    @Test
    void builtinLevelsShouldExposeDestructibleBuildingsAndHealingDrops() {
        LevelManager levelManager = new LevelManager();
        GameWorld tutorialWorld = MapDataMapper.toWorld(levelManager.createLevelData("tutorial"));
        GameWorld demoWorld = MapDataMapper.toWorld(levelManager.createLevelData("demo-map"));

        assertTrue(
            tutorialWorld.getObjects().stream()
                .filter(SceneObject.class::isInstance)
                .map(SceneObject.class::cast)
                .anyMatch(SceneObject::isDestructible),
            "教程关卡应至少包含一个可破坏建筑"
        );
        assertTrue(
            demoWorld.getObjects().stream()
                .filter(SceneObject.class::isInstance)
                .map(SceneObject.class::cast)
                .filter(SceneObject::isDestructible)
                .count() >= 2,
            "演示关卡应至少包含两个可破坏建筑"
        );
        assertTrue(
            tutorialWorld.getObjectsByType(GameObjectType.MONSTER).stream()
                .filter(MonsterObject.class::isInstance)
                .map(MonsterObject.class::cast)
                .anyMatch(monster -> monster.getHealDropAmount() > 0),
            "教程关卡应有预设回血掉落的怪物"
        );
        assertTrue(
            demoWorld.getObjectsByType(GameObjectType.MONSTER).stream()
                .filter(MonsterObject.class::isInstance)
                .map(MonsterObject.class::cast)
                .filter(monster -> monster.getHealDropAmount() > 0)
                .count() >= 2,
            "演示关卡应至少有两个预设回血掉落的怪物"
        );
    }

    @Test
    void airRaidDemoShouldContainFlyingEnemyPlaneAndCover() {
        LevelManager levelManager = new LevelManager();
        GameWorld airRaidWorld = MapDataMapper.toWorld(levelManager.createLevelData("air-raid-demo"));

        MonsterObject plane = airRaidWorld.getObjectsByType(GameObjectType.MONSTER).stream()
            .filter(MonsterObject.class::isInstance)
            .map(MonsterObject.class::cast)
            .filter(MonsterObject::isAirborne)
            .findFirst()
            .orElseThrow();

        assertTrue(plane.isRangedAttacker(), "空袭敌机应具有远程攻击能力");
        assertTrue(plane.isBomber(), "空袭敌机应投放炸弹而不是普通子弹");
        assertTrue(plane.getShootRange() >= 1000, "空袭敌机应能在较远距离发动攻击");
        assertTrue(
            airRaidWorld.getObjects().stream()
                .filter(SceneObject.class::isInstance)
                .map(SceneObject.class::cast)
                .anyMatch(SceneObject::isDestructible),
            "空袭 demo 关卡应包含可被破坏的掩体"
        );
    }

    @Test
    void generatedProceduralLevelShouldUseProvidedSeedAndRemainDeterministic() {
        LevelManager levelManager = new LevelManager();

        MapData first = levelManager.createGeneratedProceduralLevel(
            "procedural-forest",
            "procedural-forest-12345",
            12345L
        );
        MapData second = levelManager.createGeneratedProceduralLevel(
            "procedural-forest",
            "procedural-forest-12345",
            12345L
        );

        assertNotNull(first);
        assertNotNull(second);
        assertEquals("procedural-forest-12345", first.getName());
        assertEquals(first.getName(), second.getName());
        assertEquals(first.getWinCondition(), second.getWinCondition());
        assertEquals(first.getTargetItems(), second.getTargetItems());
        assertEquals(
            MapDataMapper.exportToJson(first).toString(),
            MapDataMapper.exportToJson(second).toString(),
            "同一模板和种子应生成完全相同的地图"
        );
    }

    @Test
    void tutorialLevelShouldNotStartWithDialogsAlreadyActive() {
        LevelManager levelManager = new LevelManager();
        GameWorld tutorialWorld = MapDataMapper.toWorld(levelManager.createLevelData("tutorial"));

        assertTrue(
            tutorialWorld.getObjectsByType(GameObjectType.DIALOG).stream().noneMatch(GameObject::isActive),
            "教程关卡中的提示对话应保持未激活，等待玩家靠近后再触发"
        );
    }

    @Test
    void itemPickupShouldHealPlayerAndDeactivateItem() {
        GameWorld world = new GameWorld(160, 120);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setHealth(40);
        ItemObject item = new ItemObject("heart", 10, 20, 28, 28, "health", 25, "Heal");

        world.addObject(player);
        world.addObject(item);

        world.update(1.0 / 60.0);

        assertFalse(item.isActive(), "拾取后物品应失效");
        assertTrue(player.getHealth() > 40, "治疗物品应恢复生命值");
        assertTrue(player.isHealEffectActive(), "治疗物品应触发补血特效");
    }

    @Test
    void itemShouldRoundTripThroughObjectFactory() {
        ItemObject source = new ItemObject("coin", 12, 18, 28, 28, "coin", 15, "Coin");

        ObjectData data = GameObjectFactory.toObjectData(source);
        GameObject restored = GameObjectFactory.fromObjectData(data);

        assertInstanceOf(ItemObject.class, restored);
        ItemObject item = (ItemObject) restored;
        assertEquals(source.getKind(), item.getKind());
        assertEquals(source.getValue(), item.getValue());
    }

    @Test
    void textObjectsAndVoxelShouldRoundTripThroughObjectFactory() {
        MenuObject menu = new MenuObject("menu", 12, 18, 220, 150, "Menu", List.of("Start", "Exit"));
        menu.setFontSize(26);
        menu.setActive(false);
        DialogObject dialog = new DialogObject("dialog", 12, 18, 320, 80, "Guide", "Hello world");
        dialog.setFontSize(24);
        dialog.setActive(false);
        VoxelObject voxel = new VoxelObject("voxel", 20, 20, 24, new Color(255, 168, 72));

        ObjectData menuData = GameObjectFactory.toObjectData(menu);
        ObjectData dialogData = GameObjectFactory.toObjectData(dialog);
        ObjectData voxelData = GameObjectFactory.toObjectData(voxel);

        GameObject restoredMenu = GameObjectFactory.fromObjectData(menuData);
        GameObject restoredDialog = GameObjectFactory.fromObjectData(dialogData);
        GameObject restoredVoxel = GameObjectFactory.fromObjectData(voxelData);

        assertInstanceOf(MenuObject.class, restoredMenu);
        assertInstanceOf(DialogObject.class, restoredDialog);
        assertInstanceOf(VoxelObject.class, restoredVoxel);
        assertEquals(26, ((MenuObject) restoredMenu).getFontSize());
        assertEquals(24, ((DialogObject) restoredDialog).getFontSize());
        assertFalse(restoredMenu.isActive());
        assertFalse(restoredDialog.isActive());
    }

    @Test
    void mapGravitySettingsShouldRoundTripThroughMapper() {
        GameWorld world = new GameWorld(320, 240);
        world.setGravityEnabled(true);
        world.setGravityStrength(777);

        MapData mapData = MapDataMapper.fromWorld(world, "gravity-map");
        assertNotNull(mapData);
        assertTrue(mapData.isGravityEnabled());
        assertEquals(777, mapData.getGravityStrength());

        GameWorld restored = MapDataMapper.toWorld(mapData);
        assertTrue(restored.isGravityEnabled());
        assertEquals(777, restored.getGravityStrength());
    }
}

package lib.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lib.object.BoundaryObject;
import lib.object.DialogObject;
import lib.object.GoalObject;
import lib.object.ItemObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.object.VoxelObject;
import lib.object.WallObject;
import lib.object.dto.MapData;
import lib.persistence.MapDataMapper;
import lib.persistence.ProceduralLevelGenerator;
import lib.state.DefaultGameStateMachine;

/**
 * 关卡管理器，负责管理内置关卡模板和关卡切换。
 */
public final class LevelManager {
    private static final String TUTORIAL_LEVEL = "tutorial";
    private static final String DEMO_MAP = "demo-map";
    private static final String LEVEL_1 = "level-1";
    private static final String LEVEL_2 = "level-2";
    private static final String LEVEL_3 = "level-3";
    private static final String LEVEL_4 = "level-4";
    private static final String PROC_FOREST = "procedural-forest";
    private static final String PROC_CAVE = "procedural-cave";
    private static final String AIR_RAID_DEMO = "air-raid-demo";

    private final GameWorld world;
    private final List<String> levelNames;
    private int currentLevelIndex;

    public LevelManager() {
        this(null);
    }

    public LevelManager(GameWorld world) {
        this.world = world;
        this.levelNames = new ArrayList<>();
        this.currentLevelIndex = 0;
        registerBuiltinLevels();
    }

    public void addLevel(String name) {
        String normalized = normalizeLevelName(name);
        if (normalized == null || levelNames.contains(normalized)) {
            return;
        }
        levelNames.add(normalized);
    }

    public List<String> getLevelNames() {
        return Collections.unmodifiableList(levelNames);
    }

    public boolean hasNextLevel() {
        return currentLevelIndex + 1 < levelNames.size();
    }

    public String getCurrentLevelName() {
        if (currentLevelIndex >= 0 && currentLevelIndex < levelNames.size()) {
            return levelNames.get(currentLevelIndex);
        }
        return null;
    }

    public void setCurrentLevel(String levelName) {
        String normalized = normalizeLevelName(levelName);
        if (normalized == null) {
            return;
        }
        int index = levelNames.indexOf(normalized);
        if (index >= 0) {
            currentLevelIndex = index;
        }
    }

    public void loadNextLevel() {
        if (levelNames.isEmpty() || world == null) {
            return;
        }
        if (currentLevelIndex + 1 < levelNames.size()) {
            currentLevelIndex++;
            loadLevel(levelNames.get(currentLevelIndex));
        }
    }

    public void restartLevel() {
        if (levelNames.isEmpty() || world == null) {
            return;
        }
        loadLevel(levelNames.get(Math.max(0, currentLevelIndex)));
    }

    public void loadLevel(String levelName) {
        if (world == null) {
            return;
        }
        MapData mapData = createLevelData(levelName);
        if (mapData == null) {
            return;
        }
        int index = levelNames.indexOf(normalizeLevelName(levelName));
        if (index >= 0) {
            currentLevelIndex = index;
        }
        MapDataMapper.applyToWorld(world, mapData);
        
        if (world.getStateMachine() instanceof DefaultGameStateMachine dsm) {
            dsm.recenterUI(world);
        }
    }

    public MapData createLevelData(String levelName) {
        String normalized = normalizeLevelName(levelName);
        if (normalized == null) {
            normalized = TUTORIAL_LEVEL;
        }

        if (normalized.equals(PROC_FOREST)) {
            return createGeneratedProceduralLevel(PROC_FOREST, PROC_FOREST, System.currentTimeMillis());
        }
        if (normalized.equals(PROC_CAVE)) {
            return createGeneratedProceduralLevel(PROC_CAVE, PROC_CAVE, System.currentTimeMillis());
        }
        if (normalized.equals(AIR_RAID_DEMO)) {
            return MapDataMapper.fromWorld(createAirRaidDemoWorld(), AIR_RAID_DEMO);
        }

        GameWorld levelWorld = switch (normalized) {
            case TUTORIAL_LEVEL -> createTutorialLevelWorld();
            case DEMO_MAP -> createDemoMapWorld();
            case LEVEL_1 -> {
                GameWorld world = createForestLevelWorld();
                world.setWinCondition(WinConditionType.KILL_ALL_MONSTERS);
                yield world;
            }
            case LEVEL_2 -> createRuinsLevelWorld();
            case LEVEL_3 -> createCavernLevelWorld();
            case LEVEL_4 -> createBossArenaWorld();
            default -> createTutorialLevelWorld();
        };
        return MapDataMapper.fromWorld(levelWorld, normalized);
    }

    public MapData createGeneratedProceduralLevel(String templateName, String generatedName, long seed) {
        String normalizedTemplate = normalizeLevelName(templateName);
        if (normalizedTemplate == null) {
            return null;
        }
        String normalizedName = normalizeLevelName(generatedName);
        if (normalizedName == null) {
            normalizedName = normalizedTemplate + "-" + seed;
        }

        MapData mapData = switch (normalizedTemplate) {
            case PROC_FOREST -> ProceduralLevelGenerator.generateForest(normalizedName, seed);
            case PROC_CAVE -> ProceduralLevelGenerator.generateCave(normalizedName, seed);
            default -> null;
        };
        if (mapData == null) {
            return null;
        }
        if (PROC_FOREST.equals(normalizedTemplate)) {
            mapData.setWinCondition(WinConditionType.COLLECT_TARGET_COUNT);
            mapData.setTargetItems(5);
        } else if (PROC_CAVE.equals(normalizedTemplate)) {
            mapData.setWinCondition(WinConditionType.KILL_TARGET_COUNT);
            mapData.setTargetKills(10);
        }
        return mapData;
    }

    private void registerBuiltinLevels() {
        addLevel(TUTORIAL_LEVEL);
        addLevel(DEMO_MAP);
        addLevel(LEVEL_1);
        addLevel(LEVEL_2);
        addLevel(LEVEL_3);
        addLevel(LEVEL_4);
        addLevel(PROC_FOREST);
        addLevel(PROC_CAVE);
        addLevel(AIR_RAID_DEMO);
    }

    private GameWorld createTutorialLevelWorld() {
        GameWorld levelWorld = new GameWorld(960 * 3, 540, new Color(40, 44, 52));
        addFrame(levelWorld, 960 * 3, 540);
        addGround(levelWorld, 0, 420, 960 * 3, 120, new Color(80, 84, 92));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 100, 320);

        // 教程 1: 移动
        DialogObject tut1 = new DialogObject("tut-1", 100, 100, 400, 60, "System", "Use WASD or Arrow Keys to MOVE.");
        tut1.setActive(false);
        levelWorld.addObject(tut1);
        
        // 教程 2: 跳跃
        levelWorld.addObject(new WallObject("step-1", 600, 320, 100, 100));
        DialogObject tut2 = new DialogObject("tut-2", 700, 100, 400, 60, "System", "Press SPACE to JUMP over obstacles.");
        tut2.setActive(false);
        levelWorld.addObject(tut2);

        // 教程 3: 战斗
        MonsterObject tutorialDummy = new MonsterObject("dummy", 1200, 340, 50);
        tutorialDummy.setHealDropAmount(25);
        levelWorld.addObject(tutorialDummy);
        WallObject fragileCrate = new WallObject("fragile-crate", 1430, 330, 56, 90);
        fragileCrate.setDestructible(true);
        fragileCrate.setDurability(18);
        levelWorld.addObject(fragileCrate);
        DialogObject tut3 = new DialogObject("tut-3", 1200, 100, 400, 60, "System", "Press B to switch bullet type (normal / flare / bomb). Press K or Left Click to SHOOT. Some monsters drop healing, and fragile blocks can be destroyed.");
        tut3.setActive(false);
        levelWorld.addObject(tut3);

        // 教程 4: 收集
        levelWorld.addObject(new ItemObject("orb", 1800, 340, 28, 28, "lightorb", 150, "Collected Light Orb!"));
        DialogObject tut4 = new DialogObject("tut-4", 1800, 100, 400, 60, "System", "Collect LIGHT ORBS to increase your vision.");
        tut4.setActive(false);
        levelWorld.addObject(tut4);

        // 终点
        DialogObject tutExit = new DialogObject("tut-exit", 2500, 100, 400, 60, "System", "Reach the GOLDEN PORTAL to complete the tutorial.");
        tutExit.setActive(false);
        levelWorld.addObject(tutExit);
        levelWorld.addObject(new GoalObject("goal", 2700, 350, 64, 72));

        return levelWorld;
    }

    private GameWorld createDemoMapWorld() {
        GameWorld levelWorld = new GameWorld(960 * 3, 540, new Color(36, 42, 56));
        addFrame(levelWorld, 960 * 3, 540);
        addGround(levelWorld, 0, 420, 960 * 3, 120, new Color(102, 153, 102));
        addPlayer(levelWorld, 120, 320);
        MonsterObject slimeDemo = new MonsterObject("slime-demo", 360, 340, 30);
        slimeDemo.setHealDropAmount(12);
        levelWorld.addObject(slimeDemo);
        levelWorld.addObject(new MonsterObject("bat-demo", 700, 260, 50));
        MonsterObject batFar = new MonsterObject("bat-far", 1800, 240, 60);
        batFar.setHealDropAmount(20);
        levelWorld.addObject(batFar);
        WallObject centerWall = new WallObject("demo-center-wall", 280, 290, 110, 80);
        centerWall.setDestructible(true);
        centerWall.setDurability(30);
        levelWorld.addObject(centerWall);
        SceneObject demoCrate = new SceneObject("demo-breakable-crate", 980, 360, 52, 60, true, false);
        demoCrate.setColor(new Color(154, 116, 84));
        demoCrate.setDestructible(true);
        demoCrate.setDurability(16);
        levelWorld.addObject(demoCrate);
        levelWorld.addObject(new VoxelObject("demo-voxel-a", 560, 310, 24, 24, new Color(255, 168, 72)));
        levelWorld.addObject(new VoxelObject("demo-voxel-b", 584, 310, 24, 24, new Color(255, 168, 72)));
        levelWorld.addObject(new VoxelObject("demo-voxel-c", 608, 310, 24, 24, new Color(255, 168, 72)));
        levelWorld.addObject(new ItemObject("demo-coin", 200, 360, 28, 28, "coin", 15, "Demo coin"));
        levelWorld.addObject(new ItemObject("demo-heart", 540, 360, 28, 28, "health", 20, "Small heal"));
        levelWorld.addObject(new ItemObject("demo-light-1", 160, 340, 28, 28, "lightorb", 150, "Vision Enhanced!"));
        levelWorld.addObject(new ItemObject("demo-light-2", 1200, 340, 28, 28, "lightorb", 200, "Vision Enhanced!"));
        levelWorld.addObject(new GoalObject("demo-exit", 960 * 3 - 100, 350, 60, 70));
        levelWorld.addObject(new DialogObject("demo-guide", 150, 450, 660, 60, "Guide", "Explore the large world! Use WASD to move."));
        return levelWorld;
    }

    private GameWorld createAirRaidDemoWorld() {
        int width = 960 * 3;
        int height = 640;
        GameWorld levelWorld = new GameWorld(width, height, new Color(74, 106, 168));
        addFrame(levelWorld, width, height);
        levelWorld.setGravityEnabled(true);
        addGround(levelWorld, 0, 500, width, 140, new Color(88, 110, 88));
        addPlayer(levelWorld, 100, 440);

        addAirCloud(levelWorld, 160, 110, 160, 50, new Color(240, 245, 255, 180));
        addAirCloud(levelWorld, 640, 90, 210, 60, new Color(235, 243, 252, 160));
        addAirCloud(levelWorld, 1320, 120, 180, 52, new Color(244, 248, 255, 170));
        addAirCloud(levelWorld, 2060, 100, 220, 58, new Color(238, 244, 255, 150));

        SceneObject bunkerOne = new SceneObject("air-bunker-1", 520, 430, 150, 70, true, false);
        bunkerOne.setColor(new Color(118, 110, 98));
        bunkerOne.setDestructible(true);
        bunkerOne.setDurability(30);
        levelWorld.addObject(bunkerOne);

        SceneObject bunkerTwo = new SceneObject("air-bunker-2", 1460, 430, 170, 76, true, false);
        bunkerTwo.setColor(new Color(132, 120, 105));
        bunkerTwo.setDestructible(true);
        bunkerTwo.setDurability(36);
        levelWorld.addObject(bunkerTwo);

        SceneObject radarTower = new SceneObject("air-radar-tower", 2240, 360, 46, 140, true, false);
        radarTower.setColor(new Color(92, 104, 114));
        radarTower.setCollapseWhenUnsupported(true);
        radarTower.setCollapseDamage(26);
        levelWorld.addObject(radarTower);

        MonsterObject plane = new MonsterObject("enemy-plane", 260, 140, 120);
        plane.setSize(116, 42);
        plane.setColor(new Color(208, 216, 228));
        plane.setMaterial("plane");
        plane.setAirborne(true);
        plane.setBomber(true);
        plane.setAggressive(true);
        plane.setSpeed(150);
        plane.setAttack(24);
        plane.setHealDropAmount(35);
        plane.setRangedAttacker(true);
        plane.setShootRange(1400);
        plane.setProjectileSpeed(180);
        plane.setShootCooldown(1.05);
        plane.setBombRadius(88);
        levelWorld.addObject(plane);

        levelWorld.addObject(new ItemObject("air-supply", 760, 430, 28, 28, "health", 18, "Supply Drop"));
        levelWorld.addObject(new ItemObject("air-supply-2", 1800, 430, 28, 28, "health", 22, "Supply Drop"));
        levelWorld.addObject(new GoalObject("air-raid-exit", width - 120, 430, 64, 72));
        DialogObject guide = new DialogObject(
            "air-raid-guide",
            120,
            70,
            760,
            60,
            "Command",
            "Hostile aircraft are bombing the airfield. Use the bunkers for cover and reach the exit."
        );
        guide.setActive(false);
        levelWorld.addObject(guide);
        return levelWorld;
    }

    private void addAirCloud(GameWorld levelWorld, int x, int y, int width, int height, Color color) {
        SceneObject cloud = new SceneObject("cloud", x, y, width, height, false, true);
        cloud.setColor(color);
        cloud.setSolid(false);
        cloud.setMaterial("cloud");
        levelWorld.addObject(cloud);
    }

    private GameWorld createForestLevelWorld() {
        GameWorld levelWorld = new GameWorld(1280 * 3, 720, new Color(46, 68, 45));
        addFrame(levelWorld, 1280 * 3, 720);
        addGround(levelWorld, 0, 580, 1280 * 3, 140, new Color(92, 142, 92));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 100, 500);
        levelWorld.addObject(new MonsterObject("forest-slime", 420, 520, 40));
        levelWorld.addObject(new WallObject("forest-bridge-1", 260, 460, 120, 28));
        levelWorld.addObject(new GoalObject("forest-exit", 1280 * 3 - 120, 510, 64, 72));
        return levelWorld;
    }

    private GameWorld createRuinsLevelWorld() {
        GameWorld levelWorld = new GameWorld(1200 * 3, 680, new Color(50, 48, 64));
        addFrame(levelWorld, 1200 * 3, 680);
        addGround(levelWorld, 0, 540, 1200 * 3, 140, new Color(100, 96, 116));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 90, 470);
        levelWorld.addObject(new GoalObject("ruins-exit", 1200 * 3 - 120, 470, 64, 72));
        return levelWorld;
    }

    private GameWorld createCavernLevelWorld() {
        GameWorld levelWorld = new GameWorld(1024 * 3, 640, new Color(32, 34, 40));
        addFrame(levelWorld, 1024 * 3, 640);
        addGround(levelWorld, 0, 500, 1024 * 3, 140, new Color(70, 76, 88));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 80, 430);
        levelWorld.addObject(new GoalObject("cave-exit", 1024 * 3 - 120, 430, 64, 72));
        return levelWorld;
    }

    private GameWorld createBossArenaWorld() {
        GameWorld levelWorld = new GameWorld(1366 * 2, 768, new Color(42, 30, 32));
        addFrame(levelWorld, 1366 * 2, 768);
        addGround(levelWorld, 0, 620, 1366 * 2, 148, new Color(110, 72, 72));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 100, 560);
        levelWorld.addObject(new GoalObject("boss-exit", 1366 * 2 - 150, 550, 80, 80));
        return levelWorld;
    }

    private void addFrame(GameWorld levelWorld, int width, int height) {
        levelWorld.addObject(BoundaryObject.top(width, 12));
        levelWorld.addObject(BoundaryObject.bottom(width, height, 12));
        levelWorld.addObject(BoundaryObject.left(height, 12));
        levelWorld.addObject(BoundaryObject.right(width, height, 12));
    }

    private void addGround(GameWorld levelWorld, int x, int y, int width, int height, Color color) {
        SceneObject ground = new SceneObject("ground", x, y, width, height, true, true);
        ground.setColor(color);
        levelWorld.addObject(ground);
    }

    private void addPlayer(GameWorld levelWorld, int x, int y) {
        levelWorld.addObject(new PlayerObject("player", x, y));
    }

    private String normalizeLevelName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }
}

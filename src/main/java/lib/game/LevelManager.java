package lib.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lib.object.BoundaryObject;
import lib.object.DialogObject;
import lib.object.ItemObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.object.WallObject;
import lib.object.VoxelObject;
import lib.object.dto.MapData;
import lib.persistence.MapDataMapper;

/**
 * 关卡管理器，负责管理内置关卡模板和关卡切换。
 */
public final class LevelManager {
    private static final String DEMO_MAP = "demo-map";
    private static final String LEVEL_1 = "level-1";
    private static final String LEVEL_2 = "level-2";
    private static final String LEVEL_3 = "level-3";
    private static final String LEVEL_4 = "level-4";

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
        if (world.getStateMachine() instanceof lib.state.DefaultGameStateMachine dsm) {
            dsm.recenterUI(world);
        }
    }

    /**
     * 创建关卡数据，供持久化或预览使用。
     *
     * @param levelName 关卡名称
     * @return 关卡数据
     */
    public MapData createLevelData(String levelName) {
        String normalized = normalizeLevelName(levelName);
        if (normalized == null) {
            normalized = DEMO_MAP;
        }

        GameWorld levelWorld = switch (normalized) {
            case DEMO_MAP -> createDemoMapWorld();
            case LEVEL_1 -> createForestLevelWorld();
            case LEVEL_2 -> createRuinsLevelWorld();
            case LEVEL_3 -> createCavernLevelWorld();
            case LEVEL_4 -> createBossArenaWorld();
            default -> createDemoMapWorld();
        };
        return MapDataMapper.fromWorld(levelWorld, normalized);
    }

    private void registerBuiltinLevels() {
        addLevel(DEMO_MAP);
        addLevel(LEVEL_1);
        addLevel(LEVEL_2);
        addLevel(LEVEL_3);
        addLevel(LEVEL_4);
    }

    private GameWorld createDemoMapWorld() {
        GameWorld levelWorld = new GameWorld(960, 540, new Color(36, 42, 56));
        addFrame(levelWorld, 960, 540);
        addGround(levelWorld, 0, 420, 960, 120, new Color(102, 153, 102));
        addPlayer(levelWorld, 120, 320);
        levelWorld.addObject(new MonsterObject("slime-demo", 360, 340, 30));
        levelWorld.addObject(new MonsterObject("bat-demo", 700, 260, 50));
        levelWorld.addObject(new WallObject("demo-center-wall", 280, 290, 110, 80));
        levelWorld.addObject(new VoxelObject("demo-voxel-a", 560, 310, 24, 24, new Color(255, 168, 72)));
        levelWorld.addObject(new VoxelObject("demo-voxel-b", 584, 310, 24, 24, new Color(255, 168, 72)));
        levelWorld.addObject(new VoxelObject("demo-voxel-c", 608, 310, 24, 24, new Color(255, 168, 72)));
        levelWorld.addObject(new ItemObject("demo-coin", 200, 360, 28, 28, "coin", 15, "Demo coin"));
        levelWorld.addObject(new ItemObject("demo-heart", 540, 360, 28, 28, "health", 20, "Small heal"));
        levelWorld.addObject(new DialogObject(
            "demo-guide",
            150,
            450,
            660,
            60,
            "Guide",
            "Use WASD/IJKL or arrows to move. Pick a level from Levels, or open the editor."
        ));
        return levelWorld;
    }

    private GameWorld createForestLevelWorld() {
        GameWorld levelWorld = new GameWorld(1280, 720, new Color(46, 68, 45));
        addFrame(levelWorld, 1280, 720);
        addGround(levelWorld, 0, 580, 1280, 140, new Color(92, 142, 92));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 100, 500);
        levelWorld.addObject(new MonsterObject("forest-slime", 420, 520, 40));
        levelWorld.addObject(new MonsterObject("forest-bat", 760, 420, 55));
        levelWorld.addObject(new WallObject("forest-bridge-1", 260, 460, 120, 28));
        levelWorld.addObject(new WallObject("forest-bridge-2", 580, 390, 160, 28));
        levelWorld.addObject(new WallObject("forest-tree", 920, 430, 80, 150));
        levelWorld.addObject(new ItemObject("forest-coin-1", 330, 520, 28, 28, "coin", 20, "Forest coin"));
        levelWorld.addObject(new ItemObject("forest-coin-2", 620, 350, 28, 28, "coin", 20, "Forest coin"));
        levelWorld.addObject(new ItemObject("forest-heart", 1040, 510, 28, 28, "health", 25, "Healing herb"));
        levelWorld.addObject(new DialogObject(
            "forest-note",
            160,
            60,
            480,
            52,
            "Scout",
            "The forest is full of shortcuts and hidden supplies."
        ));
        return levelWorld;
    }

    private GameWorld createRuinsLevelWorld() {
        GameWorld levelWorld = new GameWorld(1200, 680, new Color(50, 48, 64));
        addFrame(levelWorld, 1200, 680);
        addGround(levelWorld, 0, 540, 1200, 140, new Color(100, 96, 116));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 90, 470);
        levelWorld.addObject(new MonsterObject("ruins-slime", 400, 500, 45));
        levelWorld.addObject(new MonsterObject("ruins-bat", 780, 360, 60));
        levelWorld.addObject(new MonsterObject("ruins-guard", 940, 500, 75));
        levelWorld.addObject(new WallObject("ruins-wall-1", 240, 440, 100, 120));
        levelWorld.addObject(new WallObject("ruins-wall-2", 460, 390, 160, 32));
        levelWorld.addObject(new WallObject("ruins-wall-3", 680, 320, 120, 32));
        levelWorld.addObject(new VoxelObject("ruins-voxel-a", 720, 500, 24, 24, new Color(72, 210, 255)));
        levelWorld.addObject(new VoxelObject("ruins-voxel-b", 744, 500, 24, 24, new Color(72, 210, 255)));
        levelWorld.addObject(new ItemObject("ruins-gem", 540, 470, 28, 28, "gem", 30, "Ancient gem"));
        levelWorld.addObject(new ItemObject("ruins-key", 840, 290, 28, 28, "shield", 20, "Runic shield"));
        levelWorld.addObject(new ItemObject("ruins-heart", 1060, 500, 28, 28, "health", 30, "Restoration"));
        return levelWorld;
    }

    private GameWorld createCavernLevelWorld() {
        GameWorld levelWorld = new GameWorld(1024, 640, new Color(32, 34, 40));
        addFrame(levelWorld, 1024, 640);
        addGround(levelWorld, 0, 500, 1024, 140, new Color(70, 76, 88));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 80, 430);
        levelWorld.addObject(new MonsterObject("cave-bat", 340, 380, 35));
        levelWorld.addObject(new MonsterObject("cave-slime", 620, 450, 40));
        levelWorld.addObject(new MonsterObject("cave-bat-2", 860, 280, 35));
        levelWorld.addObject(new WallObject("cave-wall-1", 200, 420, 80, 150));
        levelWorld.addObject(new WallObject("cave-wall-2", 420, 350, 80, 220));
        levelWorld.addObject(new WallObject("cave-wall-3", 700, 300, 80, 270));
        levelWorld.addObject(new ItemObject("cave-speed", 500, 250, 28, 28, "speed", 40, "Speed crystal"));
        levelWorld.addObject(new ItemObject("cave-xp", 760, 240, 28, 28, "xp", 35, "Deep cavern XP"));
        levelWorld.addObject(new ItemObject("cave-heart", 920, 460, 28, 28, "health", 20, "Warm spring"));
        return levelWorld;
    }

    private GameWorld createBossArenaWorld() {
        GameWorld levelWorld = new GameWorld(1366, 768, new Color(42, 30, 32));
        addFrame(levelWorld, 1366, 768);
        addGround(levelWorld, 0, 620, 1366, 148, new Color(110, 72, 72));
        levelWorld.setGravityEnabled(true);
        addPlayer(levelWorld, 100, 560);
        MonsterObject boss = new MonsterObject("boss-slime", 930, 500, 200);
        boss.setHealth(220);
        boss.setAttack(24);
        boss.setSpeed(5);
        levelWorld.addObject(boss);
        levelWorld.addObject(new MonsterObject("minion-a", 500, 540, 60));
        levelWorld.addObject(new MonsterObject("minion-b", 700, 500, 60));
        levelWorld.addObject(new WallObject("arena-pillar-1", 300, 460, 50, 160));
        levelWorld.addObject(new WallObject("arena-pillar-2", 620, 420, 50, 200));
        levelWorld.addObject(new WallObject("arena-pillar-3", 1040, 450, 50, 170));
        levelWorld.addObject(new VoxelObject("arena-voxel-a", 1130, 480, 24, 24, new Color(72, 210, 255)));
        levelWorld.addObject(new VoxelObject("arena-voxel-b", 1154, 480, 24, 24, new Color(72, 210, 255)));
        levelWorld.addObject(new ItemObject("arena-shield", 420, 360, 28, 28, "shield", 40, "Battle shield"));
        levelWorld.addObject(new ItemObject("arena-heart", 1120, 560, 28, 28, "health", 40, "Emergency heal"));
        levelWorld.addObject(new ItemObject("arena-gem", 1240, 260, 28, 28, "gem", 80, "Boss trophy"));
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

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
import lib.state.DefaultGameStateMachine;

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

    public boolean hasNextLevel() {
        return currentLevelIndex + 1 < levelNames.size();
    }

    public String getCurrentLevelName() {
        if (currentLevelIndex >= 0 && currentLevelIndex < levelNames.size()) {
            return levelNames.get(currentLevelIndex);
        }
        return null;
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
        
        // 某些情况下需要同步面板大小，这里尝试通过状态机上下文或直接操作
        if (world.getStateMachine() instanceof DefaultGameStateMachine dsm) {
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
        GameWorld levelWorld = new GameWorld(960 * 3, 540, new Color(36, 42, 56));
        addFrame(levelWorld, 960 * 3, 540);
        addGround(levelWorld, 0, 420, 960 * 3, 120, new Color(102, 153, 102));
        addPlayer(levelWorld, 120, 320);
        levelWorld.addObject(new MonsterObject("slime-demo", 360, 340, 30));
        levelWorld.addObject(new MonsterObject("bat-demo", 700, 260, 50));
        levelWorld.addObject(new MonsterObject("bat-far", 1800, 240, 60));
        levelWorld.addObject(new WallObject("demo-center-wall", 280, 290, 110, 80));
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

package lib.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lib.object.BoundaryObject;
import lib.object.DialogObject;
import lib.object.MenuObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.object.WallObject;

/**
 * 关卡管理器，负责管理关卡数据和关卡切换。
 */
public final class LevelManager {
    private final GameWorld world;
    private final List<String> levelNames;
    private int currentLevelIndex;

    public LevelManager(GameWorld world) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.levelNames = new ArrayList<>();
        this.currentLevelIndex = 0;
    }

    public void addLevel(String name) {
        levelNames.add(name);
    }

    public void loadNextLevel() {
        if (currentLevelIndex + 1 < levelNames.size()) {
            currentLevelIndex++;
            loadLevel(levelNames.get(currentLevelIndex));
        }
    }

    public void restartLevel() {
        loadLevel(levelNames.get(currentLevelIndex));
    }

    public void loadLevel(String levelName) {
        world.getEntityManager().clear();
        
        // 简单关卡生成逻辑（实际可改为从数据库加载）
        if ("level-1".equals(levelName)) {
            setupLevel1();
        } else if ("level-2".equals(levelName)) {
            setupLevel2();
        }
    }

    private void setupLevel1() {
        SceneObject ground = new SceneObject("ground", 0, 420, 960, 120, true, true);
        ground.setColor(102, 153, 102);
        
        world.addObject(ground);
        world.addObject(BoundaryObject.top(960, 12));
        world.addObject(BoundaryObject.bottom(960, 540, 12));
        world.addObject(BoundaryObject.left(540, 12));
        world.addObject(BoundaryObject.right(960, 540, 12));
        
        world.addObject(new PlayerObject("player", 100, 300));
        world.addObject(new MonsterObject("slime", 400, 350, 20));
        
        DialogObject intro = new DialogObject("intro", 100, 100, 400, 60, "System", "Welcome to Level 1!");
        world.addObject(intro);
    }

    private void setupLevel2() {
        SceneObject ground = new SceneObject("ground", 0, 420, 960, 120, true, true);
        ground.setColor(100, 100, 200);
        
        world.addObject(ground);
        world.addObject(BoundaryObject.top(960, 12));
        world.addObject(BoundaryObject.bottom(960, 540, 12));
        world.addObject(BoundaryObject.left(540, 12));
        world.addObject(BoundaryObject.right(960, 540, 12));
        
        world.addObject(new PlayerObject("player", 100, 300));
        world.addObject(new WallObject("wall", 400, 200, 50, 200));
        world.addObject(new MonsterObject("boss-slime", 600, 300, 100));
        
        DialogObject intro = new DialogObject("intro", 100, 100, 400, 60, "System", "Level 2 is harder!");
        world.addObject(intro);
    }
}

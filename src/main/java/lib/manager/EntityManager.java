package lib.manager;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.SceneObject;

/**
 * 实体管理器，负责游戏世界中所有对象的生命周期管理、类型过滤缓存及分层渲染。
 * 
 * <p>性能优化：
 * <ul>
 *   <li>类型缓存：通过 EnumMap 存储按类型分组的对象列表，减少每帧过滤开销。</li>
 *   <li>脏标记：仅在对象增删或状态改变时重新构建缓存。</li>
 * </ul>
 */
public final class EntityManager {
    /** 存储世界中所有的游戏对象 */
    private final List<GameObject> objects;
    /** 按类型索引的缓存映射 */
    private final Map<GameObjectType, List<GameObject>> typeCache;
    /** 缓存是否失效的标记 */
    private boolean cacheDirty = true;

    public EntityManager() {
        this.objects = new ArrayList<>();
        this.typeCache = new EnumMap<>(GameObjectType.class);
        for (GameObjectType type : GameObjectType.values()) {
            typeCache.put(type, new ArrayList<>());
        }
    }

    /**
     * 向管理器添加一个游戏对象。
     * 
     * @param gameObject 待添加的对象
     */
    public void add(GameObject gameObject) {
        if (gameObject == null) {
            return;
        }
        objects.add(gameObject);
        cacheDirty = true;
    }

    /**
     * 从管理器中移除一个游戏对象。
     * 
     * @param gameObject 待移除的对象
     * @return 如果成功移除返回 true
     */
    public boolean remove(GameObject gameObject) {
        boolean removed = objects.remove(gameObject);
        if (removed) {
            cacheDirty = true;
        }
        return removed;
    }

    /**
     * 清空所有管理的对象并重置缓存。
     */
    public void clear() {
        objects.clear();
        for (List<GameObject> list : typeCache.values()) {
            list.clear();
        }
        cacheDirty = true;
    }

    /**
     * 更新类型缓存列表。
     */
    private void updateCache() {
        if (!cacheDirty) {
            return;
        }
        for (List<GameObject> list : typeCache.values()) {
            list.clear();
        }
        for (GameObject object : objects) {
            typeCache.get(object.getType()).add(object);
        }
        cacheDirty = false;
    }

    /**
     * 获取所有对象的只读视图。
     */
    public List<GameObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    /**
     * 过滤出当前活跃的所有对象。
     */
    public List<GameObject> getActiveObjects() {
        List<GameObject> activeObjects = new ArrayList<>();
        for (GameObject object : objects) {
            if (object.isActive()) {
                activeObjects.add(object);
            }
        }
        return activeObjects;
    }

    /**
     * 根据类型获取对象列表，利用缓存提高性能。
     * 
     * @param type 目标类型
     * @return 只读的对象列表
     */
    public List<GameObject> getObjectsByType(GameObjectType type) {
        updateCache();
        return Collections.unmodifiableList(typeCache.get(type));
    }

    /**
     * 查找第一个符合指定类型的对象。
     * 
     * @param type 目标类型
     * @return 对象的 Optional 封装
     */
    public Optional<GameObject> findFirstByType(GameObjectType type) {
        updateCache();
        List<GameObject> list = typeCache.get(type);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    /**
     * 获取所有具有实体碰撞属性的场景对象。
     */
    public List<SceneObject> getSolidScenes() {
        List<SceneObject> solidScenes = new ArrayList<>();
        for (GameObject object : objects) {
            if (object instanceof SceneObject sceneObject && sceneObject.isActive() && sceneObject.isSolid()) {
                solidScenes.add(sceneObject);
            }
        }
        return solidScenes;
    }

    /**
     * 更新所有活跃对象的逻辑。
     * 
     * @param world        游戏世界上下文
     * @param deltaSeconds 时间增量
     */
    public void updateAll(GameWorld world, double deltaSeconds) {
        boolean anyStateChanged = false;
        // 使用副本遍历以允许在 update 中进行增删操作
        for (GameObject object : List.copyOf(objects)) {
            boolean wasActive = object.isActive();
            if (wasActive) {
                object.update(world, deltaSeconds);
                if (!object.isActive()) {
                    anyStateChanged = true;
                }
            }
        }
        if (anyStateChanged) {
            cacheDirty = true;
        }
    }

    /**
     * 渲染世界层级（背景、墙壁、实体、光效等）。
     */
    public void renderWorld(Graphics2D graphics) {
        updateCache();
        
        // 层级 1: 静态环境层
        renderList(graphics, typeCache.get(GameObjectType.SCENE));
        renderList(graphics, typeCache.get(GameObjectType.WALL));
        renderList(graphics, typeCache.get(GameObjectType.BOUNDARY));
        renderList(graphics, typeCache.get(GameObjectType.VOXEL));
        renderList(graphics, typeCache.get(GameObjectType.GOAL));
        renderList(graphics, typeCache.get(GameObjectType.TRIGGER));
        renderList(graphics, typeCache.get(GameObjectType.SPAWNER));

        // 层级 2: 动态实体与物品层
        renderList(graphics, typeCache.get(GameObjectType.PLAYER));
        renderList(graphics, typeCache.get(GameObjectType.MONSTER));
        renderList(graphics, typeCache.get(GameObjectType.ITEM));
        renderList(graphics, typeCache.get(GameObjectType.PROJECTILE));
    }

    /**
     * 渲染 UI 层级（菜单、对话框等），通常在光照层之后渲染以保持明亮。
     */
    public void renderUI(Graphics2D graphics) {
        updateCache();
        renderList(graphics, typeCache.get(GameObjectType.MENU));
        renderList(graphics, typeCache.get(GameObjectType.DIALOG));
    }

    private void renderList(Graphics2D graphics, List<GameObject> list) {
        for (GameObject object : list) {
            if (object.isActive()) {
                object.render(graphics);
            }
        }
    }
}

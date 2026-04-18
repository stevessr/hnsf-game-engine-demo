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

public final class EntityManager {
    private final List<GameObject> objects;
    private final Map<GameObjectType, List<GameObject>> typeCache;
    private boolean cacheDirty = true;

    public EntityManager() {
        this.objects = new ArrayList<>();
        this.typeCache = new EnumMap<>(GameObjectType.class);
        for (GameObjectType type : GameObjectType.values()) {
            typeCache.put(type, new ArrayList<>());
        }
    }

    public void add(GameObject gameObject) {
        if (gameObject == null) {
            return;
        }
        objects.add(gameObject);
        cacheDirty = true;
    }

    public boolean remove(GameObject gameObject) {
        boolean removed = objects.remove(gameObject);
        if (removed) {
            cacheDirty = true;
        }
        return removed;
    }

    public void clear() {
        objects.clear();
        for (List<GameObject> list : typeCache.values()) {
            list.clear();
        }
        cacheDirty = true;
    }

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

    public List<GameObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public List<GameObject> getActiveObjects() {
        List<GameObject> activeObjects = new ArrayList<>();
        for (GameObject object : objects) {
            if (object.isActive()) {
                activeObjects.add(object);
            }
        }
        return activeObjects;
    }

    public List<GameObject> getObjectsByType(GameObjectType type) {
        updateCache();
        return Collections.unmodifiableList(typeCache.get(type));
    }

    public Optional<GameObject> findFirstByType(GameObjectType type) {
        updateCache();
        List<GameObject> list = typeCache.get(type);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    public List<SceneObject> getSolidScenes() {
        List<SceneObject> solidScenes = new ArrayList<>();
        for (GameObject object : objects) {
            if (object instanceof SceneObject sceneObject && sceneObject.isActive() && sceneObject.isSolid()) {
                solidScenes.add(sceneObject);
            }
        }
        return solidScenes;
    }

    public void updateAll(GameWorld world, double deltaSeconds) {
        boolean anyStateChanged = false;
        // 使用 Copy 以支持在更新过程中添加/删除对象
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

    public void renderWorld(Graphics2D graphics) {
        updateCache();
        
        // 层级 1: 静态场景与体素与终点
        renderList(graphics, typeCache.get(GameObjectType.SCENE));
        renderList(graphics, typeCache.get(GameObjectType.WALL));
        renderList(graphics, typeCache.get(GameObjectType.BOUNDARY));
        renderList(graphics, typeCache.get(GameObjectType.VOXEL));
        renderList(graphics, typeCache.get(GameObjectType.GOAL));

        // 层级 2: 动态实体与物品
        renderList(graphics, typeCache.get(GameObjectType.PLAYER));
        renderList(graphics, typeCache.get(GameObjectType.MONSTER));
        renderList(graphics, typeCache.get(GameObjectType.ITEM));
        renderList(graphics, typeCache.get(GameObjectType.PROJECTILE));
    }

    public void renderUI(Graphics2D graphics) {
        updateCache();
        // 层级 3: UI 元素 (菜单, 对话框)
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

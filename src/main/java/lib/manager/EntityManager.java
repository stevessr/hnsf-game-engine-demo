package lib.manager;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.SceneObject;

public final class EntityManager {
    private final List<GameObject> objects;

    public EntityManager() {
        this.objects = new ArrayList<>();
    }

    public void add(GameObject gameObject) {
        if (gameObject == null) {
            return;
        }
        objects.add(gameObject);
    }

    public boolean remove(GameObject gameObject) {
        return objects.remove(gameObject);
    }

    public void clear() {
        objects.clear();
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
        return Collections.unmodifiableList(activeObjects);
    }

    public List<GameObject> getObjectsByType(GameObjectType type) {
        List<GameObject> objectsByType = new ArrayList<>();
        for (GameObject object : objects) {
            if (object.getType() == type) {
                objectsByType.add(object);
            }
        }
        return Collections.unmodifiableList(objectsByType);
    }

    public Optional<GameObject> findFirstByType(GameObjectType type) {
        for (GameObject object : objects) {
            if (object.getType() == type) {
                return Optional.of(object);
            }
        }
        return Optional.empty();
    }

    public List<SceneObject> getSolidScenes() {
        List<SceneObject> solidScenes = new ArrayList<>();
        for (GameObject object : objects) {
            if (object instanceof SceneObject sceneObject && sceneObject.isActive() && sceneObject.isSolid()) {
                solidScenes.add(sceneObject);
            }
        }
        return Collections.unmodifiableList(solidScenes);
    }

    public void updateAll(GameWorld world, double deltaSeconds) {
        for (GameObject object : List.copyOf(objects)) {
            if (object.isActive()) {
                object.update(world, deltaSeconds);
            }
        }
    }

    public void renderAll(Graphics2D graphics) {
        renderWorld(graphics);
        renderUI(graphics);
    }

    public void renderWorld(Graphics2D graphics) {
        List<GameObject> all = List.copyOf(objects);
        
        // 层级 1: 静态场景与体素
        for (GameObject object : all) {
            if (!object.isActive()) {
                continue;
            }
            GameObjectType type = object.getType();
            if (type == GameObjectType.SCENE || type == GameObjectType.WALL || type == GameObjectType.BOUNDARY || type == GameObjectType.VOXEL || type == GameObjectType.GOAL) {
                object.render(graphics);
            }
        }

        // 层级 2: 动态实体与物品
        for (GameObject object : all) {
            if (!object.isActive()) {
                continue;
            }
            GameObjectType type = object.getType();
            if (type == GameObjectType.PLAYER || type == GameObjectType.MONSTER || type == GameObjectType.ITEM || type == GameObjectType.PROJECTILE) {
                object.render(graphics);
            }
        }
    }

    public void renderUI(Graphics2D graphics) {
        List<GameObject> all = List.copyOf(objects);
        
        // 层级 3: UI 元素 (菜单, 对话框)
        for (GameObject object : all) {
            if (!object.isActive()) {
                continue;
            }
            GameObjectType type = object.getType();
            if (type == GameObjectType.MENU || type == GameObjectType.DIALOG) {
                object.render(graphics);
            }
        }
    }
}
package lib.persistence;

import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectFactory;
import lib.object.dto.MapData;
import lib.object.dto.ObjectData;

public final class MapDataMapper {
    private MapDataMapper() {
    }

    public static MapData fromWorld(GameWorld world, String name) {
        if (world == null) {
            return null;
        }
        MapData mapData = new MapData();
        mapData.setName(name);
        mapData.setWidth(world.getWidth());
        mapData.setHeight(world.getHeight());
        mapData.setBackgroundColor(world.getBackgroundColor());
        mapData.setGravityEnabled(world.isGravityEnabled());
        mapData.setGravityStrength(world.getGravityStrength());
        for (GameObject object : world.getObjects()) {
            ObjectData data = GameObjectFactory.toObjectData(object);
            if (data != null) {
                mapData.addObject(data);
            }
        }
        return mapData;
    }

    public static GameWorld toWorld(MapData mapData) {
        if (mapData == null) {
            return null;
        }
        GameWorld world = new GameWorld(mapData.getWidth(), mapData.getHeight(), mapData.getBackgroundColor());
        world.setGravityEnabled(mapData.isGravityEnabled());
        world.setGravityStrength(mapData.getGravityStrength());
        for (ObjectData objectData : mapData.getObjects()) {
            GameObject object = GameObjectFactory.fromObjectData(objectData);
            if (object != null) {
                world.addObject(object);
            }
        }
        return world;
    }

    public static void applyToWorld(GameWorld world, MapData mapData) {
        if (world == null || mapData == null) {
            return;
        }
        world.setSize(mapData.getWidth(), mapData.getHeight());
        world.setBackgroundColor(mapData.getBackgroundColor());
        world.setGravityEnabled(mapData.isGravityEnabled());
        world.setGravityStrength(mapData.getGravityStrength());
        world.getEntityManager().clear();
        for (ObjectData objectData : mapData.getObjects()) {
            GameObject object = GameObjectFactory.fromObjectData(objectData);
            if (object != null) {
                world.addObject(object);
            }
        }
    }
}

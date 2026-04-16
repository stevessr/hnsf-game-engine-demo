package lib.persistence;

import java.awt.Color;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public static JSONObject exportToJson(MapData mapData) {
        if (mapData == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        json.put("name", mapData.getName());
        json.put("width", mapData.getWidth());
        json.put("height", mapData.getHeight());
        json.put("backgroundColor", mapData.getBackgroundColor().getRGB());
        json.put("gravityEnabled", mapData.isGravityEnabled());
        json.put("gravityStrength", mapData.getGravityStrength());
        
        JSONArray objects = new JSONArray();
        for (ObjectData obj : mapData.getObjects()) {
            JSONObject objJson = new JSONObject();
            objJson.put("type", obj.getType().name());
            objJson.put("name", obj.getName());
            objJson.put("x", obj.getX());
            objJson.put("y", obj.getY());
            objJson.put("width", obj.getWidth());
            objJson.put("height", obj.getHeight());
            objJson.put("color", obj.getColor().getRGB());
            objJson.put("solid", obj.isSolid());
            objJson.put("background", obj.isBackground());
            objJson.put("texturePath", obj.getTexturePath());
            objJson.put("material", obj.getMaterial());
            objJson.put("extraJson", obj.getExtraJson());
            objects.put(objJson);
        }
        json.put("objects", objects);
        return json;
    }

    public static MapData importFromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        MapData mapData = new MapData();
        mapData.setName(json.optString("name", "Imported Map"));
        mapData.setWidth(json.optInt("width", 960));
        mapData.setHeight(json.optInt("height", 540));
        mapData.setBackgroundColor(new Color(json.optInt("backgroundColor", -13421773), true));
        mapData.setGravityEnabled(json.optBoolean("gravityEnabled", false));
        mapData.setGravityStrength(json.optInt("gravityStrength", 900));
        
        JSONArray objects = json.optJSONArray("objects");
        if (objects != null) {
            for (int i = 0; i < objects.length(); i++) {
                JSONObject objJson = objects.getJSONObject(i);
                ObjectData obj = new ObjectData();
                obj.setType(lib.object.GameObjectType.valueOf(objJson.getString("type")));
                obj.setName(objJson.getString("name"));
                obj.setX(objJson.getInt("x"));
                obj.setY(objJson.getInt("y"));
                obj.setWidth(objJson.getInt("width"));
                obj.setHeight(objJson.getInt("height"));
                obj.setColor(new Color(objJson.getInt("color"), true));
                obj.setSolid(objJson.getBoolean("solid"));
                obj.setBackground(objJson.getBoolean("background"));
                obj.setTexturePath(objJson.optString("texturePath", null));
                obj.setMaterial(objJson.optString("material", null));
                obj.setExtraJson(objJson.optString("extraJson", "{}"));
                mapData.addObject(obj);
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

package lib.persistence;

import java.awt.Color;
import org.json.JSONArray;
import org.json.JSONObject;

import lib.game.GameWorld;
import lib.game.WinConditionType;
import lib.object.GameObject;
import lib.object.GameObjectFactory;
import lib.object.dto.MapData;
import lib.object.dto.ObjectData;

/**
 * 地图数据转换工具类。
 * 负责在 GameWorld、MapData (DTO) 和 JSONObject (导出格式) 之间进行无损转换。
 */
public final class MapDataMapper {
    private MapDataMapper() {
    }

    /**
     * 将实时游戏世界状态快照转换为地图数据对象。
     * 
     * @param world 游戏世界实例
     * @param name  地图名称
     * @return MapData 实例
     */
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
        mapData.setWinCondition(world.getWinCondition());
        mapData.setTargetKills(world.getTargetKills());
        mapData.setTargetItems(world.getTargetItems());
        for (GameObject object : world.getObjects()) {
            ObjectData data = GameObjectFactory.toObjectData(object);
            if (data != null) {
                mapData.addObject(data);
            }
        }
        return mapData;
    }

    /**
     * 将地图数据导出为 JSON 格式，适用于文件保存和分享。
     * 
     * @param mapData 地图数据对象
     * @return 包含地图完整信息的 JSONObject
     */
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
        json.put("winCondition", mapData.getWinCondition().name());
        json.put("targetKills", mapData.getTargetKills());
        json.put("targetItems", mapData.getTargetItems());
        
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

    /**
     * 从 JSON 对象导入地图数据。
     * 
     * @param json 源 JSON
     * @return 转换后的 MapData 实例
     */
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
        
        if (json.has("winCondition")) {
            try {
                mapData.setWinCondition(WinConditionType.valueOf(json.getString("winCondition")));
            } catch (Exception ignored) {
                // Ignore invalid enum values
            }
        }
        mapData.setTargetKills(json.optInt("targetKills", 0));
        mapData.setTargetItems(json.optInt("targetItems", 0));
        
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

    /**
     * 将地图数据创建为一个新的 GameWorld 实例。
     * 
     * @param mapData 源地图数据
     * @return 新的 GameWorld 实例
     */
    public static GameWorld toWorld(MapData mapData) {
        if (mapData == null) {
            return null;
        }
        GameWorld world = new GameWorld(mapData.getWidth(), mapData.getHeight(), mapData.getBackgroundColor());
        world.setGravityEnabled(mapData.isGravityEnabled());
        world.setGravityStrength(mapData.getGravityStrength());
        world.setWinCondition(mapData.getWinCondition());
        world.setTargetKills(mapData.getTargetKills());
        world.setTargetItems(mapData.getTargetItems());
        for (ObjectData objectData : mapData.getObjects()) {
            GameObject object = GameObjectFactory.fromObjectData(objectData);
            if (object != null) {
                world.addObject(object);
            }
        }
        return world;
    }

    /**
     * 将地图数据应用到已有的 GameWorld 实例，会清空世界中的已有对象。
     * 
     * @param world   目标世界实例
     * @param mapData 要加载的数据
     */
    public static void applyToWorld(GameWorld world, MapData mapData) {
        if (world == null || mapData == null) {
            return;
        }
        world.setSize(mapData.getWidth(), mapData.getHeight());
        world.setBackgroundColor(mapData.getBackgroundColor());
        world.setGravityEnabled(mapData.isGravityEnabled());
        world.setGravityStrength(mapData.getGravityStrength());
        world.setWinCondition(mapData.getWinCondition());
        world.setTargetKills(mapData.getTargetKills());
        world.setTargetItems(mapData.getTargetItems());
        world.getEntityManager().clear();
        for (ObjectData objectData : mapData.getObjects()) {
            GameObject object = GameObjectFactory.fromObjectData(objectData);
            if (object != null) {
                world.addObject(object);
            }
        }
    }
}

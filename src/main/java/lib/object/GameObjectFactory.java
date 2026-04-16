package lib.object;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import lib.object.dto.ObjectData;

public final class GameObjectFactory {
    private GameObjectFactory() {
    }

    public static ObjectData toObjectData(GameObject object) {
        if (object == null) {
            return null;
        }
        ObjectData data = new ObjectData();
        data.setType(object.getType());
        data.setName(object.getName());
        data.setX(object.getX());
        data.setY(object.getY());
        data.setWidth(object.getWidth());
        data.setHeight(object.getHeight());
        data.setColor(object.getColor());

        if (object instanceof SceneObject sceneObject) {
            data.setSolid(sceneObject.isSolid());
            data.setBackground(sceneObject.isBackground());
        }

        JSONObject extra = new JSONObject();
        if (object instanceof PlayerObject player) {
            extra.put("level", player.getLevel());
            extra.put("experience", player.getExperience());
            extra.put("health", player.getHealth());
            extra.put("attack", player.getAttack());
            extra.put("speed", player.getSpeed());
            extra.put("complementaryColorDamageEnabled", player.isComplementaryColorDamageEnabled());
            extra.put("complementaryColorDamage", player.getComplementaryColorDamage());
        } else if (object instanceof MonsterObject monster) {
            extra.put("rewardExperience", monster.getRewardExperience());
            extra.put("aggressive", monster.isAggressive());
            extra.put("health", monster.getHealth());
            extra.put("attack", monster.getAttack());
            extra.put("speed", monster.getSpeed());
        } else if (object instanceof ItemObject item) {
            extra.put("kind", item.getKind());
            extra.put("value", item.getValue());
            extra.put("message", item.getMessage());
        } else if (object instanceof MenuObject menu) {
            extra.put("title", menu.getTitle());
            extra.put("selectedIndex", menu.getSelectedIndex());
            extra.put("fontSize", menu.getFontSize());
            JSONArray options = new JSONArray();
            for (String option : menu.getOptions()) {
                options.put(option);
            }
            extra.put("options", options);
        } else if (object instanceof DialogObject dialog) {
            extra.put("speakerName", dialog.getSpeakerName());
            extra.put("message", dialog.getMessage());
            extra.put("fontSize", dialog.getFontSize());
        }

        data.setExtraJson(extra.isEmpty() ? "{}" : extra.toString());
        return data;
    }

    public static GameObject fromObjectData(ObjectData data) {
        if (data == null) {
            return null;
        }
        GameObjectType type = data.getType();
        GameObject object;
        switch (type) {
            case PLAYER -> object = createPlayer(data);
            case MONSTER -> object = createMonster(data);
            case ITEM -> object = createItem(data);
            case VOXEL -> object = createVoxel(data);
            case WALL -> object = new WallObject(data.getName(), data.getX(), data.getY(), data.getWidth(), data.getHeight());
            case BOUNDARY -> object = new BoundaryObject(data.getName(), data.getX(), data.getY(), data.getWidth(), data.getHeight());
            case MENU -> object = createMenu(data);
            case DIALOG -> object = createDialog(data);
            case SCENE -> object = new SceneObject(
                data.getName(),
                data.getX(),
                data.getY(),
                data.getWidth(),
                data.getHeight(),
                data.isSolid(),
                data.isBackground()
            );
            default -> object = new SceneObject(data.getName(), data.getX(), data.getY(), data.getWidth(), data.getHeight(), false, false);
        }

        object.setPosition(data.getX(), data.getY());
        object.setSize(data.getWidth(), data.getHeight());
        object.setColor(resolveColor(data.getColor()));
        return object;
    }

    private static GameObject createPlayer(ObjectData data) {
        PlayerObject player = new PlayerObject(data.getName(), data.getX(), data.getY());
        JSONObject extra = parseExtra(data.getExtraJson());
        if (extra.has("level")) {
            player.setLevel(extra.optInt("level", player.getLevel()));
        }
        if (extra.has("experience")) {
            player.setExperience(extra.optInt("experience", player.getExperience()));
        }
        if (extra.has("health")) {
            player.setHealth(extra.optInt("health", player.getHealth()));
        }
        if (extra.has("attack")) {
            player.setAttack(extra.optInt("attack", player.getAttack()));
        }
        if (extra.has("speed")) {
            player.setSpeed(extra.optInt("speed", player.getSpeed()));
        }
        if (extra.has("complementaryColorDamageEnabled")) {
            player.setComplementaryColorDamageEnabled(extra.optBoolean(
                "complementaryColorDamageEnabled",
                player.isComplementaryColorDamageEnabled()
            ));
        }
        if (extra.has("complementaryColorDamage")) {
            player.setComplementaryColorDamage(extra.optInt(
                "complementaryColorDamage",
                player.getComplementaryColorDamage()
            ));
        }
        return player;
    }

    private static GameObject createMonster(ObjectData data) {
        JSONObject extra = parseExtra(data.getExtraJson());
        int rewardExperience = extra.optInt("rewardExperience", 0);
        MonsterObject monster = new MonsterObject(data.getName(), data.getX(), data.getY(), rewardExperience);
        if (extra.has("aggressive")) {
            monster.setAggressive(extra.optBoolean("aggressive", monster.isAggressive()));
        }
        if (extra.has("health")) {
            monster.setHealth(extra.optInt("health", monster.getHealth()));
        }
        if (extra.has("attack")) {
            monster.setAttack(extra.optInt("attack", monster.getAttack()));
        }
        if (extra.has("speed")) {
            monster.setSpeed(extra.optInt("speed", monster.getSpeed()));
        }
        return monster;
    }

    private static GameObject createItem(ObjectData data) {
        JSONObject extra = parseExtra(data.getExtraJson());
        String kind = extra.optString("kind", "coin");
        int value = extra.optInt("value", 10);
        String message = extra.optString("message", null);
        return new ItemObject(
            data.getName(),
            data.getX(),
            data.getY(),
            data.getWidth(),
            data.getHeight(),
            kind,
            value,
            message
        );
    }

    private static GameObject createVoxel(ObjectData data) {
        return new VoxelObject(
            data.getName(),
            data.getX(),
            data.getY(),
            data.getWidth(),
            data.getHeight(),
            resolveColor(data.getColor())
        );
    }

    private static GameObject createMenu(ObjectData data) {
        JSONObject extra = parseExtra(data.getExtraJson());
        String title = extra.optString("title", "Menu");
        List<String> options = new ArrayList<>();
        JSONArray optionsArray = extra.optJSONArray("options");
        if (optionsArray != null) {
            for (int index = 0; index < optionsArray.length(); index++) {
                String option = optionsArray.optString(index, null);
                if (option != null && !option.isBlank()) {
                    options.add(option);
                }
            }
        }
        if (options.isEmpty()) {
            options.add("Empty");
        }
        MenuObject menu = new MenuObject(
            data.getName(),
            data.getX(),
            data.getY(),
            data.getWidth(),
            data.getHeight(),
            title,
            options
        );
        if (extra.has("selectedIndex")) {
            menu.setSelectedIndex(extra.optInt("selectedIndex", menu.getSelectedIndex()));
        }
        if (extra.has("fontSize")) {
            menu.setFontSize(extra.optInt("fontSize", menu.getFontSize()));
        }
        return menu;
    }

    private static GameObject createDialog(ObjectData data) {
        JSONObject extra = parseExtra(data.getExtraJson());
        String speakerName = extra.optString("speakerName", "Narrator");
        String message = extra.optString("message", "...");
        DialogObject dialog = new DialogObject(
            data.getName(),
            data.getX(),
            data.getY(),
            data.getWidth(),
            data.getHeight(),
            speakerName,
            message
        );
        if (extra.has("fontSize")) {
            dialog.setFontSize(extra.optInt("fontSize", dialog.getFontSize()));
        }
        return dialog;
    }

    private static Color resolveColor(Color color) {
        return color == null ? Color.WHITE : color;
    }

    private static JSONObject parseExtra(String extraJson) {
        if (extraJson == null || extraJson.isBlank()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(extraJson);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }
}

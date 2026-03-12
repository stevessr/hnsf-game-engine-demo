package lib.object.dto;

import java.awt.Color;
import lib.object.GameObjectType;

public final class ObjectData {
    private long id;
    private long mapId;
    private GameObjectType type;
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    private Color color;
    private boolean solid;
    private boolean background;
    private String extraJson;

    public ObjectData() {
        this(0L, 0L, GameObjectType.SCENE, "object", 0, 0, 64, 64, Color.WHITE, false, false, "{}");
    }

    public ObjectData(
        long id,
        long mapId,
        GameObjectType type,
        String name,
        int x,
        int y,
        int width,
        int height,
        Color color,
        boolean solid,
        boolean background,
        String extraJson
    ) {
        this.id = id;
        this.mapId = mapId;
        this.type = type == null ? GameObjectType.SCENE : type;
        this.name = normalizeName(name);
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.color = color == null ? Color.WHITE : color;
        this.solid = solid;
        this.background = background;
        this.extraJson = extraJson == null ? "{}" : extraJson;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = Math.max(0L, id);
    }

    public long getMapId() {
        return mapId;
    }

    public void setMapId(long mapId) {
        this.mapId = Math.max(0L, mapId);
    }

    public GameObjectType getType() {
        return type;
    }

    public void setType(GameObjectType type) {
        this.type = type == null ? GameObjectType.SCENE : type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = Math.max(0, width);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = Math.max(0, height);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color == null ? Color.WHITE : color;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson == null ? "{}" : extraJson;
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return "object";
        }
        return value;
    }
}

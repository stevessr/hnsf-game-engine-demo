package lib.object.dto;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lib.game.WinConditionType;
public final class MapData {
    private long id;
    private String name;
    private int width;
    private int height;
    private Color backgroundColor;
    private boolean gravityEnabled;
    private int gravityStrength;
    private WinConditionType winCondition = WinConditionType.REACH_GOAL;
    private int targetKills = 0;
    private int targetItems = 0;
    private final List<ObjectData> objects;

    public MapData() {
        this(0L, "untitled", 960, 540, new Color(32, 36, 48), false, 900, new ArrayList<>());
    }

    public WinConditionType getWinCondition() {
        return winCondition;
    }

    public void setWinCondition(WinConditionType winCondition) {
        this.winCondition = winCondition == null ? WinConditionType.REACH_GOAL : winCondition;
    }

    public int getTargetKills() {
        return targetKills;
    }

    public void setTargetKills(int targetKills) {
        this.targetKills = Math.max(0, targetKills);
    }

    public int getTargetItems() {
        return targetItems;
    }

    public void setTargetItems(int targetItems) {
        this.targetItems = Math.max(0, targetItems);
    }

    public MapData(
        long id,
        String name,
        int width,
        int height,
        Color backgroundColor,
        boolean gravityEnabled,
        int gravityStrength,
        List<ObjectData> objects
    ) {
        this.id = id;
        this.name = normalizeName(name);
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.backgroundColor = backgroundColor == null ? new Color(32, 36, 48) : backgroundColor;
        this.gravityEnabled = gravityEnabled;
        this.gravityStrength = Math.max(0, gravityStrength);
        this.objects = new ArrayList<>();
        if (objects != null) {
            this.objects.addAll(objects);
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = Math.max(0L, id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
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

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        if (backgroundColor == null) {
            return;
        }
        this.backgroundColor = backgroundColor;
    }

    public boolean isGravityEnabled() {
        return gravityEnabled;
    }

    public void setGravityEnabled(boolean gravityEnabled) {
        this.gravityEnabled = gravityEnabled;
    }

    public int getGravityStrength() {
        return gravityStrength;
    }

    public void setGravityStrength(int gravityStrength) {
        this.gravityStrength = Math.max(0, gravityStrength);
    }

    public List<ObjectData> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public void clearObjects() {
        objects.clear();
    }

    public void addObject(ObjectData objectData) {
        if (objectData == null) {
            return;
        }
        objects.add(objectData);
    }

    public void setObjects(List<ObjectData> items) {
        objects.clear();
        if (items != null) {
            objects.addAll(items);
        }
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return "untitled";
        }
        return value;
    }
}

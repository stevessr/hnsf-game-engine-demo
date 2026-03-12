package lib.object.dto;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public final class MapData {
    private long id;
    private String name;
    private int width;
    private int height;
    private Color backgroundColor;
    private final List<ObjectData> objects;

    public MapData() {
        this(0L, "untitled", 960, 540, new Color(32, 36, 48), new ArrayList<>());
    }

    public MapData(long id, String name, int width, int height, Color backgroundColor, List<ObjectData> objects) {
        this.id = id;
        this.name = normalizeName(name);
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.backgroundColor = backgroundColor == null ? new Color(32, 36, 48) : backgroundColor;
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

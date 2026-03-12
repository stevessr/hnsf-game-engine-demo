package lib.object;

import java.awt.Color;
import java.util.Objects;

public abstract class BaseObject implements GameObject {
    private final GameObjectType type;
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    private Color color;
    private boolean active;

    protected BaseObject(GameObjectType type, String name) {
        this(type, name, 0, 0, 0, 0, Color.WHITE, true);
    }

    protected BaseObject(
        GameObjectType type,
        String name,
        int x,
        int y,
        int width,
        int height,
        Color color,
        boolean active
    ) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.name = normalizeName(name);
        this.x = x;
        this.y = y;
        this.width = normalizeNonNegative(width);
        this.height = normalizeNonNegative(height);
        this.color = normalizeColor(color);
        this.active = active;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final void setName(String name) {
        this.name = normalizeName(name);
    }

    @Override
    public final GameObjectType getType() {
        return type;
    }

    @Override
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }

    @Override
    public final void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public final void moveBy(int deltaX, int deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }

    @Override
    public final void setSize(int width, int height) {
        this.width = normalizeNonNegative(width);
        this.height = normalizeNonNegative(height);
    }

    @Override
    public final Color getColor() {
        return color;
    }

    @Override
    public final void setColor(Color color) {
        this.color = normalizeColor(color);
    }

    public final void setColor(int red, int green, int blue) {
        this.color = new Color(clampColor(red), clampColor(green), clampColor(blue));
    }

    @Override
    public final boolean isActive() {
        return active;
    }

    @Override
    public final void setActive(boolean active) {
        this.active = active;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "object";
        }
        return name;
    }

    private static int normalizeNonNegative(int value) {
        return Math.max(0, value);
    }

    private static Color normalizeColor(Color color) {
        return color == null ? Color.WHITE : color;
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
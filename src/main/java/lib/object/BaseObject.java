package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Objects;

import lib.game.GameWorld;

public abstract class BaseObject implements GameObject {
    private final GameObjectType type;
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean active;
    private Color color;
    private String texturePath;
    private String material;

    protected BaseObject(GameObjectType type, String name, Color color, boolean active) {
        this(type, name, 0, 0, 32, 32, color, active);
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
        this.name = normalizeText(name, "object");
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.color = color == null ? Color.WHITE : color;
        this.active = active;
    }

    @Override
    public final GameObjectType getType() {
        return type;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = normalizeText(name, "object");
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

    public final void moveBy(int dx, int dy) {
        setPosition(this.x + dx, this.y + dy);
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
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    @Override
    public final boolean isActive() {
        return active;
    }

    @Override
    public final void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public final Color getColor() {
        return color;
    }

    @Override
    public final void setColor(Color color) {
        this.color = color == null ? Color.WHITE : color;
    }

    public final String getTexturePath() {
        return texturePath;
    }

    public final void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public final String getMaterial() {
        return material;
    }

    public final void setMaterial(String material) {
        this.material = material;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
    }

    @Override
    public abstract void render(Graphics2D graphics);

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

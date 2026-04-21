package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Locale;

import lib.game.GameWorld;

/**
 * 可拾取物品对象。
 * 支持经验、回复、加速、视野增强等简单效果。
 */
public final class ItemObject extends BaseObject {
    private String kind;
    private int value;
    private String message;

    public ItemObject(String name, int x, int y) {
        this(name, x, y, 28, 28, "coin", 10, "Collected coin");
    }

    public ItemObject(String name, int x, int y, int width, int height, String kind, int value) {
        this(name, x, y, width, height, kind, value, defaultMessage(kind));
    }

    public ItemObject(String name, int x, int y, int width, int height, String kind, int value, String message) {
        super(GameObjectType.ITEM, name, x, y, width, height, resolveColor(kind), true);
        this.kind = normalizeKind(kind);
        this.value = Math.max(0, value);
        this.message = normalizeMessage(message, this.kind, this.value);
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = normalizeKind(kind);
        if (message == null || message.isBlank()) {
            this.message = normalizeMessage(null, this.kind, value);
        }
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = Math.max(0, value);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = normalizeMessage(message, kind, value);
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (world == null || !isActive()) {
            return;
        }
        PlayerObject player = world.findPlayer().orElse(null);
        if (player == null || !player.isActive()) {
            return;
        }
        if (!world.getCollisions(this).contains(player)) {
            return;
        }
        applyEffect(world, player);
        world.recordItemCollection();
        setActive(false);
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 10, 10);
        graphics.setColor(Color.WHITE);
        graphics.drawRoundRect(getX(), getY(), getWidth(), getHeight(), 10, 10);
        graphics.setColor(Color.BLACK);
        String symbol = kind.isBlank() ? "?" : kind.substring(0, 1).toUpperCase(Locale.ROOT);
        graphics.drawString(symbol, getX() + Math.max(4, getWidth() / 3), getY() + Math.max(12, getHeight() / 2));
    }

    private void applyEffect(GameWorld world, PlayerObject player) {
        String normalizedKind = kind.toLowerCase(Locale.ROOT);
        switch (normalizedKind) {
            case "health", "heal", "heart" -> player.heal(world, value);
            case "speed", "boost", "dash" -> player.setThrottlePower(player.getThrottlePower() + value);
            case "lightorb", "vision", "light" -> player.addLightOrbEffect(value, 15.0);
            case "coin", "gem", "xp", "experience" -> player.gainExperience(value);
            case "shield" -> player.heal(world, Math.max(1, value / 2));
            default -> player.gainExperience(Math.max(1, value));
        }
    }

    private static String normalizeKind(String value) {
        if (value == null || value.isBlank()) {
            return "coin";
        }
        return value.trim();
    }

    private static String normalizeMessage(String value, String kind, int amount) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return switch (kind == null ? "coin" : kind.toLowerCase(Locale.ROOT)) {
            case "health", "heal", "heart" -> "Recovered " + amount + " HP";
            case "speed", "boost", "dash" -> "Speed + " + amount;
            case "lightorb", "vision", "light" -> "Vision Enhanced!";
            case "shield" -> "Shield + " + amount;
            case "gem", "xp", "experience" -> "Gained " + amount + " XP";
            default -> "Collected " + amount;
        };
    }

    private static String defaultMessage(String kind) {
        return normalizeMessage(null, kind, 10);
    }

    private static Color resolveColor(String kind) {
        if (kind == null) {
            return new Color(255, 210, 92);
        }
        return switch (kind.toLowerCase(Locale.ROOT)) {
            case "health", "heal", "heart" -> new Color(92, 205, 120);
            case "speed", "boost", "dash" -> new Color(112, 160, 255);
            case "lightorb", "vision", "light" -> new Color(255, 255, 200);
            case "shield" -> new Color(170, 130, 255);
            case "gem", "xp", "experience" -> new Color(255, 165, 60);
            default -> new Color(255, 210, 92);
        };
    }
}

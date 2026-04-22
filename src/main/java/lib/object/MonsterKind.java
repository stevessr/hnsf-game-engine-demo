package lib.object;

import java.awt.Color;
import java.util.Locale;

public enum MonsterKind {
    DEFAULT("默认", new Color(220, 80, 80), false, 100, 1.0),
    SLIME("史莱姆", new Color(92, 205, 120), false, 100, 0.82),
    SPIDER("蜘蛛", new Color(52, 34, 24), false, 120, 1.28),
    BAT("蝙蝠", new Color(78, 60, 108), true, 50, 1.22),
    GHOST("幽灵", new Color(215, 235, 255, 180), true, 10, 0.90),
    GARGOYLE("石像鬼", new Color(120, 126, 138), true, 70, 1.02),
    DRAGON("飞龙", new Color(186, 82, 52), true, 35, 1.12),
    PLANE("飞行器", new Color(208, 216, 228), true, 0, 1.05);

    private final String displayName;
    private final Color defaultColor;
    private final boolean flying;
    private final int defaultGravityPercent;
    private final double movementSpeedMultiplier;

    MonsterKind(String displayName, Color defaultColor, boolean flying, int defaultGravityPercent,
        double movementSpeedMultiplier) {
        this.displayName = displayName;
        this.defaultColor = defaultColor;
        this.flying = flying;
        this.defaultGravityPercent = defaultGravityPercent;
        this.movementSpeedMultiplier = movementSpeedMultiplier;
    }

    public boolean isFlying() {
        return flying;
    }

    public int getDefaultGravityPercent() {
        return defaultGravityPercent;
    }

    public Color getDefaultColor() {
        return defaultColor;
    }

    public double getMovementSpeedMultiplier() {
        return movementSpeedMultiplier;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static MonsterKind fromSerialized(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String normalized = value.trim();
        for (MonsterKind kind : values()) {
            if (kind.name().equalsIgnoreCase(normalized) || kind.displayName.equalsIgnoreCase(normalized)) {
                return kind;
            }
        }
        return infer(normalized, null);
    }

    public static MonsterKind infer(String name, String material) {
        String normalizedName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String normalizedMaterial = material == null ? "" : material.toLowerCase(Locale.ROOT);
        String rawHaystack = (name == null ? "" : name) + " " + (material == null ? "" : material);
        String haystack = normalizedName + " " + normalizedMaterial + " " + rawHaystack;

        if (haystack.contains("spider")
            || haystack.contains("arachnid")
            || haystack.contains("web")
            || haystack.contains("蜘蛛")) {
            return SPIDER;
        }
        if (haystack.contains("gargoyle")
            || haystack.contains("stoneguard")
            || haystack.contains("石像鬼")) {
            return GARGOYLE;
        }
        if (haystack.contains("ghost")
            || haystack.contains("phantom")
            || haystack.contains("spirit")
            || haystack.contains("wraith")
            || haystack.contains("specter")
            || haystack.contains("spectre")
            || haystack.contains("幽灵")
            || haystack.contains("鬼")) {
            return GHOST;
        }
        if (haystack.contains("dragon")
            || haystack.contains("wyrm")
            || haystack.contains("drake")
            || haystack.contains("飞龙")
            || haystack.contains("龙")) {
            return DRAGON;
        }
        if (haystack.contains("bat") || haystack.contains("蝙蝠") || haystack.contains("batman")) {
            return BAT;
        }
        if (haystack.contains("plane")
            || haystack.contains("aircraft")
            || haystack.contains("drone")
            || haystack.contains("飞行器")
            || haystack.contains("飞机")
            || haystack.contains("无人机")) {
            return PLANE;
        }
        if (haystack.contains("slime")
            || haystack.contains("jelly")
            || haystack.contains("goo")
            || haystack.contains("史莱姆")
            || haystack.contains("果冻")
            || haystack.contains("黏液")) {
            return SLIME;
        }
        return DEFAULT;
    }
}

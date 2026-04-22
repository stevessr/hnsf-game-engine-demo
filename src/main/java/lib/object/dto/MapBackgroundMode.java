package lib.object.dto;

import java.util.Locale;

public enum MapBackgroundMode {
    GRADIENT("渐变"),
    SOLID("纯色"),
    IMAGE("图片");

    private final String displayName;

    MapBackgroundMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static MapBackgroundMode fromSerialized(String value) {
        if (value == null || value.isBlank()) {
            return GRADIENT;
        }
        String normalized = value.trim();
        for (MapBackgroundMode mode : values()) {
            if (mode.name().equalsIgnoreCase(normalized) || mode.displayName.equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("image") || lowered.contains("图片")) {
            return IMAGE;
        }
        if (lowered.contains("solid") || lowered.contains("纯色")) {
            return SOLID;
        }
        return GRADIENT;
    }
}

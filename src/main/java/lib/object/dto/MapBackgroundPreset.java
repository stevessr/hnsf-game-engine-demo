package lib.object.dto;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Locale;

public enum MapBackgroundPreset {
    DEFAULT("默认", new Color(32, 36, 48), new Color(26, 30, 40), new Color(42, 48, 62), new Color(70, 84, 100)),
    FOREST("森林", new Color(54, 92, 56), new Color(26, 52, 28), new Color(60, 102, 62), new Color(122, 176, 108)),
    CAVE("洞穴", new Color(40, 42, 50), new Color(18, 18, 24), new Color(52, 56, 64), new Color(96, 100, 112)),
    ICE("冰原", new Color(126, 176, 208), new Color(92, 132, 172), new Color(158, 210, 232), new Color(236, 250, 255)),
    VOLCANO("火山", new Color(178, 74, 52), new Color(96, 24, 24), new Color(204, 102, 64), new Color(255, 188, 112)),
    DESERT("沙漠", new Color(214, 176, 112), new Color(172, 124, 72), new Color(232, 204, 144), new Color(255, 242, 202)),
    SKY("天空", new Color(104, 154, 216), new Color(74, 124, 198), new Color(168, 212, 248), new Color(250, 252, 255)),
    NIGHT("夜晚", new Color(24, 30, 60), new Color(12, 14, 28), new Color(38, 46, 80), new Color(120, 156, 214)),
    RUINS("废墟", new Color(88, 82, 102), new Color(52, 48, 62), new Color(114, 108, 132), new Color(182, 176, 194));

    private final String displayName;
    private final Color suggestedBaseColor;
    private final Color topColor;
    private final Color middleColor;
    private final Color bottomColor;

    MapBackgroundPreset(String displayName, Color suggestedBaseColor, Color topColor, Color middleColor, Color bottomColor) {
        this.displayName = displayName;
        this.suggestedBaseColor = suggestedBaseColor;
        this.topColor = topColor;
        this.middleColor = middleColor;
        this.bottomColor = bottomColor;
    }

    public Color getSuggestedBaseColor() {
        return suggestedBaseColor;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static MapBackgroundPreset fromSerialized(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String normalized = value.trim();
        for (MapBackgroundPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(normalized) || preset.displayName.equalsIgnoreCase(normalized)) {
                return preset;
            }
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.contains("forest") || lowered.contains("森林")) {
            return FOREST;
        }
        if (lowered.contains("cave") || lowered.contains("洞穴")) {
            return CAVE;
        }
        if (lowered.contains("ice") || lowered.contains("冰原") || lowered.contains("雪")) {
            return ICE;
        }
        if (lowered.contains("volcano") || lowered.contains("火山") || lowered.contains("lava")) {
            return VOLCANO;
        }
        if (lowered.contains("desert") || lowered.contains("沙漠")) {
            return DESERT;
        }
        if (lowered.contains("sky") || lowered.contains("天空") || lowered.contains("cloud")) {
            return SKY;
        }
        if (lowered.contains("night") || lowered.contains("夜晚") || lowered.contains("moon")) {
            return NIGHT;
        }
        if (lowered.contains("ruin") || lowered.contains("废墟")) {
            return RUINS;
        }
        return DEFAULT;
    }

    public void paint(Graphics2D graphics, int width, int height, Color baseColor) {
        if (graphics == null) {
            return;
        }
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        Color anchor = baseColor == null ? suggestedBaseColor : baseColor;

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color[] palette = buildPalette(anchor);
        LinearGradientPaint paint = new LinearGradientPaint(
            new Point2D.Float(0.0f, 0.0f),
            new Point2D.Float(0.0f, safeHeight),
            new float[] {0.0f, 0.55f, 1.0f},
            palette
        );
        graphics.setPaint(paint);
        graphics.fillRect(0, 0, safeWidth, safeHeight);

        switch (this) {
            case FOREST -> paintForest(graphics, safeWidth, safeHeight, anchor);
            case CAVE -> paintCave(graphics, safeWidth, safeHeight, anchor);
            case ICE -> paintIce(graphics, safeWidth, safeHeight, anchor);
            case VOLCANO -> paintVolcano(graphics, safeWidth, safeHeight, anchor);
            case DESERT -> paintDesert(graphics, safeWidth, safeHeight, anchor);
            case SKY -> paintSky(graphics, safeWidth, safeHeight, anchor);
            case NIGHT -> paintNight(graphics, safeWidth, safeHeight, anchor);
            case RUINS -> paintRuins(graphics, safeWidth, safeHeight, anchor);
            default -> {
                // 默认主题只使用渐变色
            }
        }
    }

    private Color[] buildPalette(Color anchor) {
        return switch (this) {
            case FOREST -> new Color[] {
                mix(anchor, topColor, 0.72),
                mix(anchor, middleColor, 0.62),
                mix(anchor, bottomColor, 0.55)
            };
            case CAVE -> new Color[] {
                mix(anchor, topColor, 0.84),
                mix(anchor, middleColor, 0.64),
                mix(anchor, bottomColor, 0.48)
            };
            case ICE -> new Color[] {
                mix(anchor, topColor, 0.52),
                mix(anchor, middleColor, 0.38),
                mix(anchor, bottomColor, 0.24)
            };
            case VOLCANO -> new Color[] {
                mix(anchor, topColor, 0.78),
                mix(anchor, middleColor, 0.58),
                mix(anchor, bottomColor, 0.44)
            };
            case DESERT -> new Color[] {
                mix(anchor, topColor, 0.54),
                mix(anchor, middleColor, 0.42),
                mix(anchor, bottomColor, 0.28)
            };
            case SKY -> new Color[] {
                mix(anchor, topColor, 0.58),
                mix(anchor, middleColor, 0.42),
                mix(anchor, bottomColor, 0.22)
            };
            case NIGHT -> new Color[] {
                mix(anchor, topColor, 0.84),
                mix(anchor, middleColor, 0.70),
                mix(anchor, bottomColor, 0.52)
            };
            case RUINS -> new Color[] {
                mix(anchor, topColor, 0.70),
                mix(anchor, middleColor, 0.56),
                mix(anchor, bottomColor, 0.42)
            };
            default -> new Color[] {
                mix(anchor, topColor, 0.55),
                mix(anchor, middleColor, 0.35),
                mix(anchor, bottomColor, 0.25)
            };
        };
    }

    private void paintForest(Graphics2D graphics, int width, int height, Color anchor) {
        Color canopy = withAlpha(mix(anchor, new Color(34, 72, 34), 0.55), 100);
        Color trunk = withAlpha(mix(anchor, new Color(54, 36, 24), 0.55), 130);
        Color mist = withAlpha(new Color(190, 224, 180), 28);
        graphics.setColor(mist);
        graphics.fillRect(0, height / 2, width, height / 5);
        for (int index = 0; index < 5; index++) {
            int x = (index * width) / 4 - width / 12;
            int treeWidth = Math.max(50, width / 5);
            int treeHeight = Math.max(90, height / 3);
            graphics.setColor(trunk);
            graphics.fillRect(x + treeWidth / 2 - 6, height - treeHeight / 2, 12, treeHeight / 2);
            graphics.setColor(canopy);
            graphics.fillOval(x, height - treeHeight, treeWidth, treeHeight);
            graphics.fillOval(x + treeWidth / 6, height - treeHeight - treeHeight / 5, treeWidth * 2 / 3, treeHeight * 3 / 4);
        }
    }

    private void paintCave(Graphics2D graphics, int width, int height, Color anchor) {
        Color stalactite = withAlpha(mix(anchor, new Color(74, 76, 84), 0.82), 190);
        Color rock = withAlpha(mix(anchor, new Color(24, 24, 30), 0.74), 140);
        Color dust = withAlpha(new Color(140, 132, 122), 24);
        graphics.setColor(dust);
        graphics.fillRect(0, height / 3, width, height / 3);
        graphics.setColor(stalactite);
        for (int index = 0; index < 7; index++) {
            int spikeWidth = Math.max(26, width / 16);
            int spikeHeight = Math.max(36, height / 4);
            int x = index * width / 7;
            Polygon spike = new Polygon();
            spike.addPoint(x, 0);
            spike.addPoint(x + spikeWidth / 2, spikeHeight + (index % 2) * 10);
            spike.addPoint(x + spikeWidth, 0);
            graphics.fillPolygon(spike);
        }
        graphics.setColor(rock);
        for (int index = 0; index < 4; index++) {
            int rockWidth = Math.max(80, width / 6);
            int rockHeight = Math.max(26, height / 10);
            int x = index * width / 4 - width / 20;
            graphics.fillOval(x, height - rockHeight - 6, rockWidth, rockHeight);
        }
    }

    private void paintIce(Graphics2D graphics, int width, int height, Color anchor) {
        Color shard = withAlpha(mix(anchor, new Color(220, 248, 255), 0.70), 130);
        Color sparkle = withAlpha(new Color(255, 255, 255), 160);
        for (int index = 0; index < 8; index++) {
            int x = (index * width) / 8;
            int shardWidth = Math.max(28, width / 12);
            int shardHeight = Math.max(48, height / 4);
            Polygon polygon = new Polygon();
            polygon.addPoint(x, height / 6);
            polygon.addPoint(x + shardWidth / 2, height / 6 + shardHeight);
            polygon.addPoint(x + shardWidth, height / 8);
            graphics.setColor(shard);
            graphics.fillPolygon(polygon);
        }
        graphics.setColor(sparkle);
        for (int index = 0; index < 24; index++) {
            int x = (index * 73) % width;
            int y = (index * 47) % Math.max(1, height);
            graphics.fillOval(x, y, 2, 2);
        }
    }

    private void paintVolcano(Graphics2D graphics, int width, int height, Color anchor) {
        Color glow = withAlpha(mix(anchor, new Color(255, 120, 72), 0.70), 92);
        Color lava = withAlpha(new Color(255, 94, 38), 190);
        Color smoke = withAlpha(new Color(40, 34, 34), 72);
        graphics.setColor(glow);
        graphics.fillOval(width / 6, height - height / 3, width * 2 / 3, height / 2);
        graphics.fillOval(width / 3, height - height / 4, width / 3, height / 5);
        graphics.setColor(lava);
        for (int index = 0; index < 5; index++) {
            int x = (index * width) / 5 + width / 12;
            graphics.fillOval(x, height - height / 5, Math.max(12, width / 24), Math.max(12, height / 18));
        }
        graphics.setColor(smoke);
        for (int index = 0; index < 4; index++) {
            int x = (index * width) / 4;
            graphics.fillOval(x + width / 12, height / 8, Math.max(30, width / 8), Math.max(18, height / 14));
        }
    }

    private void paintDesert(Graphics2D graphics, int width, int height, Color anchor) {
        Color sun = withAlpha(new Color(255, 242, 180), 160);
        Color dune = withAlpha(mix(anchor, new Color(244, 214, 142), 0.55), 150);
        Color sand = withAlpha(mix(anchor, new Color(246, 224, 176), 0.45), 90);
        graphics.setColor(sun);
        graphics.fillOval(width - width / 5, height / 10, Math.max(60, width / 8), Math.max(60, height / 5));
        graphics.setColor(sand);
        graphics.fillOval(-width / 8, height - height / 3, width * 3 / 5, height / 2);
        graphics.fillOval(width / 3, height - height / 4, width * 3 / 5, height / 3);
        graphics.setColor(dune);
        graphics.fillOval(width / 6, height - height / 5, width * 4 / 5, height / 5);
    }

    private void paintSky(Graphics2D graphics, int width, int height, Color anchor) {
        Color cloud = withAlpha(mix(anchor, new Color(255, 255, 255), 0.60), 160);
        Color mist = withAlpha(new Color(255, 255, 255), 36);
        graphics.setColor(mist);
        graphics.fillRect(0, height / 2, width, height / 3);
        paintCloud(graphics, width / 12, height / 8, width / 5, height / 12, cloud);
        paintCloud(graphics, width / 2, height / 10, width / 4, height / 10, cloud);
        paintCloud(graphics, width * 2 / 3, height / 4, width / 5, height / 12, cloud);
        graphics.setColor(withAlpha(new Color(255, 245, 196), 120));
        graphics.fillOval(width - width / 6, height / 12, Math.max(32, width / 12), Math.max(32, height / 8));
    }

    private void paintNight(Graphics2D graphics, int width, int height, Color anchor) {
        Color stars = withAlpha(new Color(255, 248, 214), 210);
        Color moon = withAlpha(new Color(240, 240, 255), 180);
        graphics.setColor(stars);
        for (int index = 0; index < 24; index++) {
            int x = (index * 67 + 31) % width;
            int y = (index * 41 + 19) % Math.max(1, height / 2 + 1);
            graphics.fillOval(x, y, 2 + index % 2, 2 + index % 2);
        }
        graphics.setColor(moon);
        graphics.fillOval(width - width / 4, height / 10, Math.max(36, width / 8), Math.max(36, height / 5));
        graphics.setColor(withAlpha(mix(anchor, new Color(70, 80, 128), 0.55), 120));
        graphics.fillOval(width / 10, height / 2, width / 3, height / 3);
    }

    private void paintRuins(Graphics2D graphics, int width, int height, Color anchor) {
        Color column = withAlpha(mix(anchor, new Color(98, 94, 112), 0.60), 190);
        Color crack = withAlpha(new Color(42, 38, 44), 120);
        Color dust = withAlpha(new Color(220, 210, 220), 26);
        graphics.setColor(dust);
        graphics.fillRect(0, height / 2, width, height / 3);
        graphics.setColor(column);
        for (int index = 0; index < 4; index++) {
            int x = (index * width) / 4 + width / 20;
            int columnWidth = Math.max(24, width / 18);
            int columnHeight = Math.max(height / 3, height / 2);
            graphics.fillRect(x, height - columnHeight, columnWidth, columnHeight);
            graphics.fillRect(x - 10, height - columnHeight - 18, columnWidth + 20, 16);
        }
        graphics.setColor(crack);
        graphics.drawLine(width / 8, height - height / 4, width / 3, height - height / 3);
        graphics.drawLine(width / 2, height - height / 5, width * 3 / 4, height - height / 3);
    }

    private void paintCloud(Graphics2D graphics, int x, int y, int width, int height, Color cloud) {
        graphics.setColor(cloud);
        graphics.fillOval(x, y, width, height);
        graphics.fillOval(x + width / 4, y - height / 3, width / 2, height);
        graphics.fillOval(x + width / 2, y, width / 2, height);
    }

    private Color mix(Color base, Color overlay, double ratio) {
        Color safeBase = base == null ? suggestedBaseColor : base;
        Color safeOverlay = overlay == null ? suggestedBaseColor : overlay;
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        int red = (int) Math.round(safeBase.getRed() * (1.0 - clamped) + safeOverlay.getRed() * clamped);
        int green = (int) Math.round(safeBase.getGreen() * (1.0 - clamped) + safeOverlay.getGreen() * clamped);
        int blue = (int) Math.round(safeBase.getBlue() * (1.0 - clamped) + safeOverlay.getBlue() * clamped);
        int alpha = (int) Math.round(safeBase.getAlpha() * (1.0 - clamped) + safeOverlay.getAlpha() * clamped);
        return new Color(
            clamp(red),
            clamp(green),
            clamp(blue),
            clamp(alpha)
        );
    }

    private Color withAlpha(Color color, int alpha) {
        Color safe = color == null ? suggestedBaseColor : color;
        return new Color(safe.getRed(), safe.getGreen(), safe.getBlue(), clamp(alpha));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

package lib.render;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import lib.object.ItemObject;
import lib.object.MonsterKind;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.object.VoxelObject;
import lib.object.WallObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SpriteAssets {
    private static final String PLAYER_HERO = "/sprites/characters/player-hero.png";
    private static final String MONSTER_SLIME = "/sprites/monsters/slime.png";
    private static final String MONSTER_BAT = "/sprites/monsters/bat.png";
    private static final String MONSTER_SPIDER = "/sprites/monsters/spider.png";
    private static final String MONSTER_GHOST = "/sprites/monsters/ghost.png";
    private static final String MONSTER_GARGOYLE = "/sprites/monsters/gargoyle.png";
    private static final String MONSTER_DRAGON = "/sprites/monsters/dragon.png";
    private static final String MONSTER_PLANE = "/sprites/monsters/plane.png";
    private static final String ITEM_COIN = "/sprites/items/coin.png";
    private static final String ITEM_HEART = "/sprites/items/heart.png";
    private static final String ITEM_LIGHTORB = "/sprites/items/lightorb.png";
    private static final String ITEM_SPEED = "/sprites/items/speed.png";
    private static final String ITEM_SHIELD = "/sprites/items/shield.png";
    private static final String PROP_WOOD_CRATE = "/sprites/props/wood-crate.png";
    private static final String TILE_STONE = "/sprites/tiles/stone.png";
    private static final String TILE_BRICK = "/sprites/tiles/brick.png";
    private static final String TILE_WOOD = "/sprites/tiles/wood-planks.png";

    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    private SpriteAssets() {
    }

    public static boolean drawPlayer(Graphics2D graphics, PlayerObject player) {
        if (graphics == null || player == null) {
            return false;
        }
        return drawSprite(
            graphics,
            PLAYER_HERO,
            player.getX(),
            player.getY(),
            player.getWidth(),
            player.getHeight(),
            player.getLastDirectionX() < -0.1
        );
    }

    public static boolean drawMonster(Graphics2D graphics, MonsterObject monster, boolean faceLeft) {
        if (graphics == null || monster == null) {
            return false;
        }
        String resourcePath = switch (monster.getMonsterKind()) {
            case SLIME -> MONSTER_SLIME;
            case BAT -> MONSTER_BAT;
            case SPIDER -> MONSTER_SPIDER;
            case GHOST -> MONSTER_GHOST;
            case GARGOYLE -> MONSTER_GARGOYLE;
            case DRAGON -> MONSTER_DRAGON;
            case PLANE -> MONSTER_PLANE;
            default -> null;
        };
        if (resourcePath == null) {
            return false;
        }
        return drawSprite(
            graphics,
            resourcePath,
            monster.getX(),
            monster.getY(),
            monster.getWidth(),
            monster.getHeight(),
            faceLeft
        );
    }

    public static boolean drawItem(Graphics2D graphics, ItemObject item) {
        if (graphics == null || item == null) {
            return false;
        }
        String kind = normalize(item.getKind());
        String resourcePath = switch (kind) {
            case "health", "heal", "heart" -> ITEM_HEART;
            case "lightorb", "vision", "light" -> ITEM_LIGHTORB;
            case "speed", "boost", "dash" -> ITEM_SPEED;
            case "shield" -> ITEM_SHIELD;
            case "coin", "gem", "xp", "experience" -> ITEM_COIN;
            default -> null;
        };
        if (resourcePath == null) {
            return false;
        }
        return drawSprite(
            graphics,
            resourcePath,
            item.getX(),
            item.getY(),
            item.getWidth(),
            item.getHeight(),
            false
        );
    }

    public static boolean drawWall(Graphics2D graphics, WallObject wall) {
        if (graphics == null || wall == null) {
            return false;
        }
        String resourcePath = resolveBlockResource(wall.getName(), wall.getMaterial(), true);
        if (PROP_WOOD_CRATE.equals(resourcePath)) {
            return drawSprite(
                graphics,
                resourcePath,
                wall.getX(),
                wall.getY(),
                wall.getWidth(),
                wall.getHeight(),
                false
            );
        }
        if (resourcePath == null) {
            return false;
        }
        return drawTexture(
            graphics,
            resourcePath,
            wall.getX(),
            wall.getY(),
            wall.getWidth(),
            wall.getHeight()
        );
    }

    public static boolean drawScene(Graphics2D graphics, SceneObject scene) {
        if (graphics == null || scene == null) {
            return false;
        }
        String resourcePath = resolveBlockResource(scene.getName(), scene.getMaterial(), false);
        if (PROP_WOOD_CRATE.equals(resourcePath)) {
            return drawSprite(
                graphics,
                resourcePath,
                scene.getX(),
                scene.getY(),
                scene.getWidth(),
                scene.getHeight(),
                false
            );
        }
        if (resourcePath == null) {
            return false;
        }
        return drawTexture(
            graphics,
            resourcePath,
            scene.getX(),
            scene.getY(),
            scene.getWidth(),
            scene.getHeight()
        );
    }

    public static boolean drawVoxel(Graphics2D graphics, VoxelObject voxel) {
        if (graphics == null || voxel == null) {
            return false;
        }
        String material = normalize(voxel.getMaterial());
        String name = normalize(voxel.getName());
        String resourcePath;
        if ("stone".equals(material) || name.contains("stone")) {
            resourcePath = TILE_STONE;
        } else if ("wood".equals(material) || name.contains("wood") || name.contains("plank")) {
            resourcePath = TILE_WOOD;
        } else if ("brick".equals(material) || name.contains("brick")) {
            resourcePath = TILE_BRICK;
        } else {
            return false;
        }
        return drawTexture(
            graphics,
            resourcePath,
            voxel.getX(),
            voxel.getY(),
            voxel.getWidth(),
            voxel.getHeight()
        );
    }

    private static String resolveBlockResource(String name, String material, boolean preferBrick) {
        String normalizedName = normalize(name);
        String normalizedMaterial = normalize(material);
        String haystack = normalizedName + " " + normalizedMaterial;

        if (containsAny(haystack, "crate", "box", "supply")) {
            return PROP_WOOD_CRATE;
        }
        if (containsAny(haystack, "bridge", "plank", "wood")) {
            return TILE_WOOD;
        }
        if (containsAny(haystack, "stone", "rock", "bunker", "tower", "radar")) {
            return TILE_STONE;
        }
        if (containsAny(haystack, "brick", "wall", "step")) {
            return TILE_BRICK;
        }
        return preferBrick ? TILE_BRICK : null;
    }

    private static boolean drawSprite(
        Graphics2D graphics,
        String resourcePath,
        int x,
        int y,
        int width,
        int height,
        boolean flipHorizontally
    ) {
        BufferedImage image = loadImage(resourcePath);
        if (image == null) {
            return false;
        }
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g2d.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );
            if (flipHorizontally) {
                g2d.drawImage(image, x + width, y, -width, height, null);
            } else {
                g2d.drawImage(image, x, y, width, height, null);
            }
            return true;
        } finally {
            g2d.dispose();
        }
    }

    private static boolean drawTexture(
        Graphics2D graphics,
        String resourcePath,
        int x,
        int y,
        int width,
        int height
    ) {
        BufferedImage image = loadImage(resourcePath);
        if (image == null) {
            return false;
        }
        Graphics2D g2d = (Graphics2D) graphics.create();
        Paint originalPaint = g2d.getPaint();
        try {
            double tileSize = Math.max(32.0, Math.min(72.0, Math.min(width, height)));
            TexturePaint texturePaint = new TexturePaint(
                image,
                new Rectangle2D.Double(x, y, tileSize, tileSize)
            );
            g2d.setPaint(texturePaint);
            g2d.fillRect(x, y, width, height);
            return true;
        } finally {
            g2d.setPaint(originalPaint);
            g2d.dispose();
        }
    }

    private static BufferedImage loadImage(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        return CACHE.computeIfAbsent(resourcePath, SpriteAssets::readImage);
    }

    private static BufferedImage readImage(String resourcePath) {
        try (InputStream inputStream = SpriteAssets.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.warn("Missing sprite resource: {}", resourcePath);
                return null;
            }
            return ImageIO.read(inputStream);
        } catch (IOException exception) {
            log.warn("Failed to load sprite resource: {}", resourcePath, exception);
            return null;
        }
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

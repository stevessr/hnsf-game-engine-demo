package lib.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import lib.game.GameWorld;
import lib.object.dto.MapBackgroundMode;
import lib.object.dto.MapData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BackgroundAssets {
    private static final String LEVEL_TUTORIAL = "/backgrounds/levels/tutorial.jpg";
    private static final String LEVEL_DEMO = "/backgrounds/levels/demo-map.jpg";
    private static final String LEVEL_FOREST = "/backgrounds/levels/forest.jpg";
    private static final String LEVEL_PROCEDURAL_FOREST = "/backgrounds/levels/procedural-forest.jpg";
    private static final String LEVEL_RUINS = "/backgrounds/levels/ruins.jpg";
    private static final String LEVEL_CAVERN = "/backgrounds/levels/cavern.jpg";
    private static final String LEVEL_PROCEDURAL_CAVE = "/backgrounds/levels/procedural-cave.jpg";
    private static final String LEVEL_BOSS = "/backgrounds/levels/boss-arena.jpg";
    private static final String LEVEL_AIR_RAID = "/backgrounds/levels/air-raid.jpg";
    private static final String UI_MAIN_MENU = "/backgrounds/ui/main-menu.jpg";
    private static final String UI_PAUSE_MENU = "/backgrounds/ui/pause-menu.jpg";

    private static final Map<String, String> LEVEL_RESOURCE_MAP = Map.of(
        "tutorial", LEVEL_TUTORIAL,
        "demo-map", LEVEL_DEMO,
        "level-1", LEVEL_FOREST,
        "level-2", LEVEL_RUINS,
        "level-3", LEVEL_CAVERN,
        "level-4", LEVEL_BOSS,
        "air-raid-demo", LEVEL_AIR_RAID,
        "showcase-demo", LEVEL_DEMO
    );

    private static final Map<String, BufferedImage> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> BASE64_CACHE = new ConcurrentHashMap<>();

    private BackgroundAssets() {
    }

    public static BufferedImage loadMainMenuBackdrop() {
        return loadImage(UI_MAIN_MENU);
    }

    public static BufferedImage loadPauseMenuBackdrop() {
        return loadImage(UI_PAUSE_MENU);
    }

    public static void applyLevelBackground(GameWorld world, String levelName) {
        if (world == null) {
            return;
        }
        String resourcePath = resolveLevelResource(levelName);
        if (resourcePath == null) {
            return;
        }
        BufferedImage image = loadImage(resourcePath);
        if (image == null) {
            return;
        }
        world.setBackgroundMode(MapBackgroundMode.IMAGE);
        world.setBackgroundImage(image, resourceFileName(resourcePath));
    }

    public static void applyLevelBackground(MapData mapData, String levelName) {
        if (mapData == null) {
            return;
        }
        String resourcePath = resolveLevelResource(levelName);
        if (resourcePath == null) {
            return;
        }
        String encoded = loadBase64(resourcePath);
        if (encoded == null) {
            return;
        }
        mapData.setBackgroundMode(MapBackgroundMode.IMAGE);
        mapData.setBackgroundImageName(resourceFileName(resourcePath));
        mapData.setBackgroundImageData(encoded);
    }

    public static String resolveLevelResource(String levelName) {
        if (levelName == null || levelName.isBlank()) {
            return null;
        }
        String normalized = levelName.trim();
        if (normalized.startsWith("procedural-forest")) {
            return LEVEL_PROCEDURAL_FOREST;
        }
        if (normalized.startsWith("procedural-cave")) {
            return LEVEL_PROCEDURAL_CAVE;
        }
        return LEVEL_RESOURCE_MAP.get(normalized);
    }

    public static BufferedImage loadImage(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        String normalized = normalizeResourcePath(resourcePath);
        return IMAGE_CACHE.computeIfAbsent(normalized, BackgroundAssets::readImage);
    }

    private static String loadBase64(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        String normalized = normalizeResourcePath(resourcePath);
        return BASE64_CACHE.computeIfAbsent(normalized, BackgroundAssets::readBase64);
    }

    private static BufferedImage readImage(String resourcePath) {
        try (InputStream inputStream = BackgroundAssets.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.warn("Missing background resource: {}", resourcePath);
                return null;
            }
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                log.warn("Unsupported background image format: {}", resourcePath);
            }
            return image;
        } catch (IOException exception) {
            log.warn("Failed to read background image: {}", resourcePath, exception);
            return null;
        }
    }

    private static String readBase64(String resourcePath) {
        try (InputStream inputStream = BackgroundAssets.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                log.warn("Missing background resource for Base64 encoding: {}", resourcePath);
                return null;
            }
            return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        } catch (IOException exception) {
            log.warn("Failed to encode background image: {}", resourcePath, exception);
            return null;
        }
    }

    private static String normalizeResourcePath(String resourcePath) {
        return resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    }

    private static String resourceFileName(String resourcePath) {
        int slash = resourcePath.lastIndexOf('/');
        return slash >= 0 ? resourcePath.substring(slash + 1) : resourcePath;
    }
}

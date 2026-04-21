package lib.persistence;

import java.awt.Color;
import java.util.Random;

import lib.object.GameObjectType;
import lib.object.dto.MapData;
import lib.object.dto.ObjectData;

/**
 * 程序化关卡生成器。
 * 使用噪音生成随机的地形、敌人和道具分布。
 */
public final class ProceduralLevelGenerator {
    private static final int PLAYER_WIDTH = 32;
    private static final int PLAYER_HEIGHT = 48;
    private static final int FOREST_SAFE_ZONE_WIDTH = 320;
    private static final int FOREST_SAFE_SURFACE_OFFSET = 80;
    private static final int CAVE_VOID_HEIGHT = 120;

    private ProceduralLevelGenerator() {
    }

    /**
     * 生成一个具有程序化地形的森林关卡。
     * 
     * @param name 关卡名称
     * @param seed 随机种子
     * @return 包含生成的物体数据的 MapData
     */
    public static MapData generateForest(String name, long seed) {
        Random rand = new Random(seed);
        NoiseGenerator noise = new NoiseGenerator(seed);
        
        int width = 4000;
        int height = 800;
        MapData map = new MapData();
        map.setName(name);
        map.setWidth(width);
        map.setHeight(height);
        map.setBackgroundColor(new Color(40, 50, 45));
        map.setGravityEnabled(true);
        map.setGravityStrength(900);

        int groundY = height - 120;
        int tileSize = 40;
        int safeSurfaceY = groundY - FOREST_SAFE_SURFACE_OFFSET;

        // 生成起伏的地形
        for (int x = 0; x < width; x += tileSize) {
            double n = noise.noise(x * 0.002, 0) * 0.5 + 0.5;
            int currentY = x < FOREST_SAFE_ZONE_WIDTH ? safeSurfaceY : groundY - (int) (n * 200);
            
            // 填充地面
            ObjectData dirt = createObject(GameObjectType.SCENE, "dirt", x, currentY, tileSize, height - currentY, new Color(80, 60, 45));
            dirt.setSolid(true);
            dirt.setMaterial("dirt");
            map.addObject(dirt);

            // 表面草皮
            ObjectData grass = createObject(GameObjectType.VOXEL, "grass", x, currentY, tileSize, 20, new Color(90, 160, 80));
            grass.setMaterial("grass");
            map.addObject(grass);

            // 随机树木 (背景)
            if (x % 200 == 0 && rand.nextDouble() > 0.4) {
                int treeH = 100 + rand.nextInt(150);
                ObjectData tree = createObject(GameObjectType.SCENE, "tree", x + 5, currentY - treeH, 30, treeH, new Color(60, 50, 40));
                tree.setBackground(true);
                tree.setSolid(false);
                tree.setMaterial("tree");
                map.addObject(tree);
            }

            // 随机放置怪物
            if (x > 500 && x < width - 400 && x % 400 == 0 && rand.nextDouble() > 0.5) {
                map.addObject(createMonster("forest-slime", x, currentY - 40));
            }

            // 随机放置道具
            if (x % 320 == 0 && rand.nextDouble() > 0.7) {
                map.addObject(createItem("orb", x, currentY - 80));
            }
        }

        // 放置玩家和终点
        map.addObject(createObject(GameObjectType.PLAYER, "player", 120, safeSurfaceY - PLAYER_HEIGHT, PLAYER_WIDTH, PLAYER_HEIGHT, Color.BLUE));
        map.addObject(createObject(GameObjectType.GOAL, "exit", width - 200, groundY - 240, 64, 80, Color.YELLOW));

        return map;
    }

    /**
     * 生成一个具有程序化洞穴结构的关卡。
     */
    public static MapData generateCave(String name, long seed) {
        Random rand = new Random(seed);
        NoiseGenerator noise = new NoiseGenerator(seed);
        
        int width = 3200;
        int height = 1200;
        MapData map = new MapData();
        map.setName(name);
        map.setWidth(width);
        map.setHeight(height);
        map.setBackgroundColor(new Color(20, 20, 25));
        map.setGravityEnabled(true);
        map.setGravityStrength(800);

        int tileSize = 60;
        int caveVoidY = height - CAVE_VOID_HEIGHT;
        map.addObject(createVoidZone(0, caveVoidY, width, CAVE_VOID_HEIGHT));
        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
                if (y + tileSize > caveVoidY) {
                    continue;
                }
                double n = noise.noise(x * 0.005, y * 0.005);
                // 使用噪音阀值生成洞穴结构
                if (n > 0.3) {
                    ObjectData wall = createObject(GameObjectType.WALL, "cave-rock", x, y, tileSize, tileSize, new Color(60, 65, 75));
                    wall.setMaterial("stone");
                    map.addObject(wall);
                } else if (n < -0.4 && rand.nextDouble() > 0.95) {
                    // 在空旷处随机放置怪物
                    map.addObject(createMonster("bat", x + 10, y + 10));
                }
            }
        }

        // 确保入口和出口可用 (简单粗暴的清空周围区域)
        int[] spawn = findSupportedSpawn(
            map,
            width,
            height,
            PLAYER_WIDTH,
            PLAYER_HEIGHT,
            120,
            height / 2
        );
        int playerX = spawn == null ? 120 : spawn[0];
        int playerY = spawn == null ? height / 2 : spawn[1];
        map.addObject(createObject(GameObjectType.PLAYER, "player", playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, Color.BLUE));
        map.addObject(createObject(GameObjectType.GOAL, "exit", width - 150, height / 2, 64, 80, Color.YELLOW));

        return map;
    }

    private static ObjectData createObject(GameObjectType type, String name, int x, int y, int w, int h, Color color) {
        ObjectData data = new ObjectData();
        data.setType(type);
        data.setName(name);
        data.setX(x);
        data.setY(y);
        data.setWidth(w);
        data.setHeight(h);
        data.setColor(color);
        data.setSolid(type == GameObjectType.WALL || type == GameObjectType.VOXEL);
        return data;
    }

    private static ObjectData createMonster(String name, int x, int y) {
        ObjectData data = createObject(GameObjectType.MONSTER, name, x, y, 40, 40, Color.RED);
        data.setExtraJson("{\"health\":60, \"attack\":10, \"rewardExperience\":30}");
        return data;
    }

    private static ObjectData createItem(String name, int x, int y) {
        ObjectData data = createObject(GameObjectType.ITEM, name, x, y, 28, 28, Color.CYAN);
        data.setExtraJson("{\"kind\":\"lightorb\", \"value\":150, \"message\":\"Vision enhanced by procedurally generated orb!\"}");
        return data;
    }

    private static ObjectData createVoidZone(int x, int y, int width, int height) {
        ObjectData data = createObject(GameObjectType.SCENE, "cave-void", x, y, width, height, new Color(8, 8, 16, 220));
        data.setSolid(false);
        data.setBackground(false);
        data.setMaterial("void");
        return data;
    }

    private static int[] findSupportedSpawn(
        MapData map,
        int worldWidth,
        int worldHeight,
        int objectWidth,
        int objectHeight,
        int preferredX,
        int preferredY
    ) {
        if (map == null || map.getObjects().isEmpty()) {
            return null;
        }

        int minX = 0;
        int maxX = Math.max(0, worldWidth - objectWidth);
        int minY = 0;
        int maxY = Math.max(0, worldHeight - objectHeight);
        int searchLimit = Math.max(worldWidth, worldHeight);

        for (int radius = 0; radius <= searchLimit; radius += 40) {
            int startX = clamp(preferredX - radius, minX, maxX);
            int endX = clamp(preferredX + radius, minX, maxX);
            int startY = clamp(preferredY - radius, minY, maxY);
            int endY = clamp(preferredY + radius, minY, maxY);

            for (int candidateY = startY; candidateY <= endY; candidateY += 4) {
                for (int candidateX = startX; candidateX <= endX; candidateX += 4) {
                    if (isSpawnClear(map, candidateX, candidateY, objectWidth, objectHeight)
                        && hasSupportBelow(map, candidateX, candidateY, objectWidth, objectHeight)) {
                        return new int[] {candidateX, candidateY};
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSpawnClear(MapData map, int x, int y, int width, int height) {
        for (ObjectData object : map.getObjects()) {
            if (object == null || !object.isSolid()) {
                continue;
            }
            if (rectanglesOverlap(x, y, width, height, object.getX(), object.getY(), object.getWidth(), object.getHeight())) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasSupportBelow(MapData map, int x, int y, int width, int height) {
        int bottom = y + height;
        for (ObjectData object : map.getObjects()) {
            if (object == null || !object.isSolid()) {
                continue;
            }
            int objectTop = object.getY();
            boolean nearSupport = objectTop >= bottom - 2 && objectTop <= bottom + 4;
            boolean horizontalOverlap = x < object.getX() + object.getWidth() && x + width > object.getX();
            if (nearSupport && horizontalOverlap) {
                return true;
            }
        }
        return bottom >= map.getHeight();
    }

    private static boolean rectanglesOverlap(
        int x1,
        int y1,
        int w1,
        int h1,
        int x2,
        int y2,
        int w2,
        int h2
    ) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

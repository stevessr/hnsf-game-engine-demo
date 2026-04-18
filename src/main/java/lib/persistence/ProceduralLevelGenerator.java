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

        // 生成起伏的地形
        for (int x = 0; x < width; x += tileSize) {
            double n = noise.noise(x * 0.002, 0) * 0.5 + 0.5;
            int currentY = groundY - (int) (n * 200);
            
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
        map.addObject(createObject(GameObjectType.PLAYER, "player", 150, groundY - 100, 32, 48, Color.BLUE));
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
        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
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
        map.addObject(createObject(GameObjectType.PLAYER, "player", 120, height / 2, 32, 48, Color.BLUE));
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
}

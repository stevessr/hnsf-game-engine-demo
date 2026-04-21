package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import lib.game.GameWorld;

class GameObjectModelTest {
    @Test
    void playerShouldLevelUpMultipleTimesAfterGainingExperience() {
        PlayerObject player = new PlayerObject("hero", 10, 20);

        player.gainExperience(350);

        assertEquals(GameObjectType.PLAYER, player.getType());
        assertEquals(3, player.getLevel());
        assertEquals(50, player.getExperience());
        assertEquals(10, player.getX());
        assertEquals(20, player.getY());
    }

    @Test
    void monsterShouldDeactivateAfterTakingFatalDamage() {
        MonsterObject monster = new MonsterObject("slime", 3, 4, 25);

        monster.takeDamage(null, 1000);

        assertEquals(0, monster.getHealth());
        assertTrue(monster.isDying());
        assertTrue(monster.isActive());

        // 模拟动画完成
        monster.update(null, 1.0);

        assertTrue(monster.isDying());
        assertFalse(monster.isActive());
        assertFalse(monster.canAttack());
        assertEquals(25, monster.getRewardExperience());
    }

    @Test
    void sceneShouldNormalizeSharedProperties() {
        SceneObject scene = new SceneObject("", 1, 2, -30, -40, true, true);

        scene.setColor(null);
        scene.moveBy(5, 6);

        assertEquals(GameObjectType.SCENE, scene.getType());
        assertEquals("object", scene.getName());
        assertEquals(0, scene.getWidth());
        assertEquals(0, scene.getHeight());
        assertEquals(6, scene.getX());
        assertEquals(8, scene.getY());
        assertEquals(Color.WHITE, scene.getColor());
        assertTrue(scene.isSolid());
        assertTrue(scene.isBackground());
    }

    @Test
    void playerShouldUpdatePositionAndRenderColor() {
        GameWorld world = new GameWorld(200, 160);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setFriction(1.0);
        player.setVelocity(30, 15);

        player.update(world, 2.0);

        assertEquals(70, player.getX());
        assertEquals(50, player.getY());

        BufferedImage image = new BufferedImage(200, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            player.render(graphics);
        } finally {
            graphics.dispose();
        }

        int centerX = player.getX() + (player.getWidth() / 2);
        int centerY = player.getY() + (player.getHeight() / 2) + 8; // 更加靠下，确保命中衣服颜色
        assertEquals(player.getColor().getRGB(), image.getRGB(centerX, centerY));
    }

    @Test
    void playerShouldKeepLegsInsideItsBoundingBox() {
        PlayerObject player = new PlayerObject("hero", 20, 20);

        BufferedImage image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            player.render(graphics);
        } finally {
            graphics.dispose();
        }

        int legAlpha = (image.getRGB(player.getX() + 14, player.getY() + player.getHeight() - 2) >>> 24) & 0xFF;
        int belowFeetAlpha = (image.getRGB(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() + 2) >>> 24) & 0xFF;
        assertTrue(legAlpha > 0, "玩家腿部应被正常渲染");
        assertEquals(0, belowFeetAlpha, "玩家渲染不应超出自身碰撞框底部");
    }

    @Test
    void playerShouldRenderHealEffectAfterHealing() {
        PlayerObject player = new PlayerObject("hero", 30, 30);
        player.setHealth(50);
        player.heal(20);

        assertTrue(player.isHealEffectActive(), "回血后应触发补血特效");

        BufferedImage image = new BufferedImage(140, 140, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            player.render(graphics);
        } finally {
            graphics.dispose();
        }

        Color effectPixel = new Color(image.getRGB(player.getX() + player.getWidth() / 2, player.getY() - 4), true);
        assertTrue(effectPixel.getAlpha() > 0, "补血特效应在角色上方形成可见光效");
        assertTrue(effectPixel.getGreen() >= effectPixel.getRed(), "补血特效应偏向绿色");
    }

    @Test
    void voidSceneShouldKillActorsAndDeactivateOtherEntities() {
        GameWorld world = new GameWorld(220, 160);
        SceneObject voidZone = new SceneObject("cave-void", 0, 96, 220, 64, false, false);
        voidZone.setColor(new Color(8, 8, 16, 220));
        voidZone.setMaterial("void");
        PlayerObject player = new PlayerObject("hero", 24, 104);
        MonsterObject monster = new MonsterObject("slime", 80, 104, 20);
        ItemObject item = new ItemObject("orb", 140, 104, 28, 28, "lightorb", 5, "Orb");

        world.addObject(voidZone);
        world.addObject(player);
        world.addObject(monster);
        world.addObject(item);

        world.update(1.0);
        world.update(1.0);

        assertFalse(player.isActive(), "虚空应杀死玩家");
        assertFalse(monster.isActive(), "虚空应杀死怪物");
        assertFalse(item.isActive(), "虚空应清除其他实体");
        assertTrue(world.getFailureReason() != null && world.getFailureReason().contains("虚空"), "玩家坠入虚空应记录失败原因");
    }

    @Test
    void monsterShouldPatrolWithinWorldBounds() {
        GameWorld world = new GameWorld(120, 90);
        MonsterObject monster = new MonsterObject("slime", 60, 10, 25);

        int originalX = monster.getX();
        world.addObject(monster);
        world.update(1.0);

        assertTrue(monster.getX() > originalX);

        for (int index = 0; index < 50; index++) {
            world.update(1.0);
        }

        assertTrue(monster.getX() >= 0);
        assertTrue(monster.getX() <= world.getWidth() - monster.getWidth());
    }

    @Test
    void worldRespawnShouldRestoreDeadPlayerWithoutResettingProgress() {
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 64, 48);
        player.setLevel(4);
        player.gainExperience(120);
        player.setThrottlePower(1000);
        world.addObject(player);
        world.setRespawnPoint(24, 36);
        world.recordKill();
        world.recordItemCollection();

        player.takeDamage(world, player.getHealth());
        player.update(world, 1.0);

        assertTrue(player.isDying(), "玩家死亡后应进入死亡动画状态");
        assertFalse(player.isActive(), "死亡动画结束后玩家应失活");

        boolean respawned = world.respawnPlayer();

        assertTrue(respawned, "世界应能重生当前玩家");
        assertTrue(player.isActive(), "重生后玩家应重新激活");
        assertFalse(player.isDying(), "重生后玩家不应继续保持死亡状态");
        assertEquals(24, player.getX(), "重生后玩家应回到出生点 X");
        assertEquals(36, player.getY(), "重生后玩家应回到出生点 Y");
        assertEquals(4, player.getLevel(), "重生不应重置玩家等级");
        assertEquals(120, player.getExperience(), "重生不应重置玩家经验");
        assertEquals(1, world.getKills(), "重生不应重置击杀进度");
        assertEquals(1, world.getItemsCollected(), "重生不应重置收集进度");
    }

    @Test
    void monsterShouldJumpOverObstacleAhead() {
        GameWorld world = new GameWorld(220, 160);
        world.setGravityEnabled(true);
        world.setGravityStrength(900);

        SceneObject ground = new SceneObject("ground", 0, 120, 220, 40, true, false);
        WallObject obstacle = new WallObject("wall", 90, 76, 20, 44);
        MonsterObject monster = new MonsterObject("slime", 40, 76, 25);
        monster.setSpeed(0);

        world.addObject(ground);
        world.addObject(obstacle);
        world.addObject(monster);

        int originalY = monster.getY();
        world.update(1.0 / 60.0);

        assertTrue(monster.getY() < originalY, "怪物应会尝试跳跃越过前方障碍");
    }

    @Test
    void monsterShouldDodgeIncomingProjectile() {
        GameWorld world = new GameWorld(320, 180);
        SceneObject ground = new SceneObject("ground", 0, 114, 320, 66, true, false);
        PlayerObject player = new PlayerObject("hero", 20, 70);
        MonsterObject monster = new MonsterObject("slime", 160, 70, 25);
        monster.setSpeed(0);
        ProjectileObject projectile = new ProjectileObject("bullet", 40, 88, 360, 0, 10, player);

        world.addObject(ground);
        world.addObject(player);
        world.addObject(monster);
        world.addObject(projectile);

        int originalX = monster.getX();
        world.update(1.0 / 60.0);

        assertTrue(monster.getX() > originalX, "怪物应向远离来袭投射物的方向闪躲");
    }

    @Test
    void monsterShouldChaseVisiblePlayerWhenHealthy() {
        GameWorld world = new GameWorld(320, 180);
        SceneObject ground = new SceneObject("ground", 0, 114, 320, 66, true, false);
        PlayerObject player = new PlayerObject("hero", 220, 70);
        MonsterObject monster = new MonsterObject("slime", 80, 70, 25);
        monster.setSpeed(30);

        world.addObject(ground);
        world.addObject(player);
        world.addObject(monster);

        int originalX = monster.getX();
        world.update(1.0 / 60.0);

        assertTrue(monster.getX() > originalX, "怪物应主动追向可见玩家");
    }

    @Test
    void lowHealthRangedMonsterShouldRetreatFromPlayer() {
        GameWorld world = new GameWorld(320, 180);
        SceneObject ground = new SceneObject("ground", 0, 114, 320, 66, true, false);
        PlayerObject player = new PlayerObject("hero", 100, 70);
        MonsterObject monster = new MonsterObject("archer", 140, 70, 25);
        monster.setSpeed(30);
        monster.setRangedAttacker(true);
        monster.setShootRange(420);
        monster.setHealth(10);

        world.addObject(ground);
        world.addObject(player);
        world.addObject(monster);

        int originalX = monster.getX();
        world.update(1.0 / 60.0);

        assertTrue(monster.getX() > originalX, "残血远程怪应优先拉开距离");
    }

    @Test
    void airborneMonsterShouldKeepAltitudeWhenWorldHasGravity() {
        GameWorld world = new GameWorld(400, 240);
        world.setGravityEnabled(true);
        MonsterObject plane = new MonsterObject("enemy-plane", 80, 70, 80);
        plane.setAirborne(true);
        plane.setMaterial("plane");
        plane.setSize(100, 40);
        plane.setSpeed(0);

        world.addObject(plane);
        world.update(1.0 / 60.0);
        world.update(1.0 / 60.0);

        assertEquals(70, plane.getY(), "空中敌机不应受重力坠落");
    }

    @Test
    void airborneMonsterShouldRenderAircraftShape() {
        MonsterObject plane = new MonsterObject("enemy-plane", 30, 30, 80);
        plane.setAirborne(true);
        plane.setMaterial("plane");
        plane.setSize(100, 40);
        plane.setColor(new Color(208, 216, 228));

        BufferedImage image = new BufferedImage(180, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            plane.render(graphics);
        } finally {
            graphics.dispose();
        }

        Color wingPixel = new Color(image.getRGB(76, 39), true);
        Color nosePixel = new Color(image.getRGB(108, 50), true);

        assertTrue(wingPixel.getAlpha() > 0, "飞机机翼区域应被绘制");
        assertTrue(nosePixel.getAlpha() > 0, "飞机机头区域应被绘制");
    }

    @Test
    void bomberMonsterShouldDropBombsInsteadOfBullets() {
        GameWorld world = new GameWorld(640, 360);
        world.setGravityEnabled(true);
        PlayerObject player = new PlayerObject("hero", 280, 160);
        MonsterObject plane = new MonsterObject("enemy-plane", 100, 60, 80);
        plane.setAirborne(true);
        plane.setBomber(true);
        plane.setMaterial("plane");
        plane.setSize(100, 40);
        plane.setSpeed(0);
        plane.setRangedAttacker(true);
        plane.setShootRange(1000);
        plane.setShootCooldown(0.1);
        plane.setProjectileSpeed(160);

        world.addObject(player);
        world.addObject(plane);
        world.update(0.2);

        assertTrue(
            world.getObjectsByType(GameObjectType.PROJECTILE).stream()
                .filter(ProjectileObject.class::isInstance)
                .map(ProjectileObject.class::cast)
                .anyMatch(projectile -> projectile.isActive() && projectile.getName().contains("-bomb-")),
            "敌机应在攻击范围内投放炸弹"
        );
    }

    @Test
    void worldShouldRenderSceneObjectOverBackground() {
        GameWorld world = new GameWorld(80, 80, Color.BLACK);
        SceneObject scene = new SceneObject("wall", 10, 10, 20, 20, true, false);
        scene.setColor(new Color(10, 200, 100));
        world.addObject(scene);

        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            world.render(graphics);
        } finally {
            graphics.dispose();
        }

        assertEquals(Color.BLACK.getRGB(), image.getRGB(0, 0));
        assertEquals(scene.getColor().getRGB(), image.getRGB(15, 15));
    }

    @Test
    void treeSceneObjectShouldRenderFoliageInsteadOfBareTrunk() {
        SceneObject tree = new SceneObject("tree", 30, 12, 30, 120, false, true);
        tree.setColor(new Color(92, 60, 34));
        tree.setMaterial("tree");

        BufferedImage image = new BufferedImage(140, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            tree.render(graphics);
        } finally {
            graphics.dispose();
        }

        Color canopyPixel = new Color(image.getRGB(45, 28), true);
        Color trunkPixel = new Color(image.getRGB(43, 112), true);

        assertTrue(canopyPixel.getAlpha() > 0, "树冠区域应被绘制");
        assertTrue(canopyPixel.getGreen() > canopyPixel.getRed(), "树冠区域应显示绿色树叶，而不是光秃秃的树干");
        assertTrue(trunkPixel.getRed() > trunkPixel.getGreen(), "树干区域仍应保留褐色树干");
    }

    @Test
    void wallAndBoundaryShouldExposeDedicatedTypes() {
        WallObject wall = new WallObject("wall", 8, 9, 24, 26);
        BoundaryObject boundary = BoundaryObject.right(120, 90, 10);

        assertEquals(GameObjectType.WALL, wall.getType());
        assertTrue(wall.isSolid());
        assertEquals(GameObjectType.BOUNDARY, boundary.getType());
        assertTrue(boundary.isSolid());
        assertEquals(110, boundary.getX());
        assertEquals(10, boundary.getWidth());
    }

    @Test
    void monsterShouldSpawnHealingDropWhenConfigured() {
        GameWorld world = new GameWorld(200, 160);
        MonsterObject monster = new MonsterObject("slime", 40, 50, 25);
        monster.setHealDropAmount(18);
        world.addObject(monster);

        monster.takeDamage(world, 1000);
        world.update(1.0 / 60.0);

        assertEquals(1, world.getObjectsByType(GameObjectType.ITEM).size());
        GameObject dropObject = world.getObjectsByType(GameObjectType.ITEM).get(0);
        ItemObject drop = assertInstanceOf(ItemObject.class, dropObject);
        assertEquals("health", drop.getKind());
        assertEquals(18, drop.getValue());
    }

    @Test
    void projectileShouldBreakDestructibleSceneObject() {
        GameWorld world = new GameWorld(160, 120);
        SceneObject crate = new SceneObject("crate", 35, 0, 24, 24, true, false);
        crate.setDestructible(true);
        crate.setDurability(10);
        ProjectileObject projectile = new ProjectileObject("bullet", 0, 0, 200, 0, 10, null);
        world.addObject(crate);
        world.addObject(projectile);

        world.update(0.2);

        assertFalse(crate.isActive(), "可破坏建筑在耐久归零后应被销毁");
        assertFalse(projectile.isActive(), "子弹命中建筑后应消失");
    }

    @Test
    void bombProjectileShouldExplodeAndDamageNearbyTargets() {
        GameWorld world = new GameWorld(260, 180);
        world.setGravityEnabled(true);
        PlayerObject player = new PlayerObject("hero", 120, 110);
        player.setHealth(40);
        SceneObject bunker = new SceneObject("bunker", 140, 120, 32, 24, true, false);
        bunker.setDestructible(true);
        bunker.setDurability(18);
        ProjectileObject bomb = ProjectileObject.createBomb("bomb", 120, 20, 0, 0, 12, null, 80, 24, 1.0);

        world.addObject(player);
        world.addObject(bunker);
        world.addObject(bomb);

        boolean shakeTriggered = false;
        for (int i = 0; i < 240 && bomb.isActive(); i++) {
            world.update(1.0 / 60.0);
            shakeTriggered |= world.isScreenShaking();
        }

        assertTrue(shakeTriggered, "爆炸应触发屏幕震动");
        assertFalse(bomb.isActive(), "炸弹在爆炸动画结束后应失效");
        assertTrue(player.getHealth() < 40, "爆炸应对附近玩家造成伤害");
        assertFalse(bunker.isActive(), "爆炸应能摧毁附近可破坏建筑");
    }

    @Test
    void unsupportedSceneObjectShouldCollapseAndCanSetDeathReason() {
        GameWorld world = new GameWorld(180, 220);
        world.setGravityEnabled(true);
        PlayerObject player = new PlayerObject("hero", 40, 140);
        player.setHealth(20);
        SceneObject slab = new SceneObject("slab", 40, 0, 48, 24, true, false);
        slab.setCollapseWhenUnsupported(true);
        slab.setCollapseDamage(30);
        world.addObject(slab);
        world.addObject(player);

        for (int i = 0; i < 120 && world.getFailureReason() == null; i++) {
            world.update(1.0 / 60.0);
        }

        assertTrue(slab.getY() > 0, "失去支撑的建筑应开始倒塌");
        assertTrue(world.getFailureReason() != null && world.getFailureReason().contains("slab"), "倒塌建筑砸中玩家时应记录死因");
    }

    @Test
    void sceneObjectShouldBreakAfterBeingSteppedOnEnoughTimes() {
        GameWorld world = new GameWorld(180, 140);
        PlayerObject player = new PlayerObject("hero", 30, 12);
        SceneObject platform = new SceneObject("fragile-platform", 20, 60, 80, 12, true, false);
        platform.setBreakAfterSteps(2);
        world.addObject(player);
        world.addObject(platform);

        world.update(1.0 / 60.0);
        assertEquals(1, platform.getStepCount());
        assertTrue(platform.isActive());

        player.setPosition(30, 0);
        world.update(1.0 / 60.0);
        player.setPosition(30, 12);
        world.update(1.0 / 60.0);

        assertFalse(platform.isActive(), "脆弱平台被踩到阈值后应损坏");
    }

    @Test
    void rangedMonsterShouldShootProjectileTowardPlayer() {
        GameWorld world = new GameWorld(420, 180);
        PlayerObject player = new PlayerObject("hero", 240, 80);
        MonsterObject monster = new MonsterObject("archer", 60, 80, 20);
        monster.setSpeed(0);
        monster.setRangedAttacker(true);
        monster.setShootRange(400);
        monster.setShootCooldown(0.1);
        world.addObject(player);
        world.addObject(monster);

        world.update(0.2);

        assertTrue(world.getObjectsByType(GameObjectType.PROJECTILE).size() >= 1, "远程怪物应生成投射物");
    }

    @Test
    void monsterProjectileShouldRecordFailureReasonWhenItKillsPlayer() {
        GameWorld world = new GameWorld(420, 180);
        PlayerObject player = new PlayerObject("hero", 240, 80);
        player.setHealth(10);
        MonsterObject monster = new MonsterObject("archer", 60, 80, 20);
        monster.setSpeed(0);
        monster.setAttack(10);
        monster.setRangedAttacker(true);
        monster.setShootRange(400);
        monster.setProjectileSpeed(500);
        monster.setShootCooldown(0.1);
        world.addObject(player);
        world.addObject(monster);

        for (int i = 0; i < 120 && world.getFailureReason() == null; i++) {
            world.update(1.0 / 60.0);
        }

        assertTrue(world.getFailureReason() != null && world.getFailureReason().contains("archer"), "远程击杀应记录怪物射击死因");
    }

    @Test
    void menuAndDialogShouldKeepSimpleUiState() {
        MenuObject menu = new MenuObject("menu", 5, 5, 140, 90, "Main", List.of("Start", "Exit"));
        DialogObject dialog = new DialogObject("dialog", 10, 50, 160, 40, "Guide", "Welcome");

        menu.nextOption();
        dialog.setMessage("Ready");

        assertEquals(GameObjectType.MENU, menu.getType());
        assertEquals(1, menu.getSelectedIndex());
        assertEquals("Exit", menu.getSelectedOption());
        assertEquals(GameObjectType.DIALOG, dialog.getType());
        assertEquals("Guide", dialog.getSpeakerName());
        assertEquals("Ready", dialog.getMessage());
    }

    @Test
    void uiObjectsShouldRenderTheirPanels() {
        MenuObject menu = new MenuObject("menu", 10, 10, 120, 80, "Main", List.of("Start"));
        DialogObject dialog = new DialogObject("dialog", 20, 60, 140, 50, "Guide", "Hello");

        BufferedImage image = new BufferedImage(200, 140, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            menu.render(graphics);
            dialog.render(graphics);
        } finally {
            graphics.dispose();
        }

        assertNotEquals(0, (image.getRGB(20, 20) >> 24) & 0xFF, "Menu should render non-transparent pixel");
        assertNotEquals(0, (image.getRGB(60, 95) >> 24) & 0xFF, "Dialog should render non-transparent pixel");
    }
}

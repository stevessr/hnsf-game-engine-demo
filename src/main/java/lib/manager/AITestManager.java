package lib.manager;

import java.util.List;
import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.PlayerObject;

/**
 * 自动测试 AI 管理器。
 * 能够控制角色自动寻路至终点并处理途中障碍。
 */
public final class AITestManager {
    private boolean enabled = false;
    private long lastJumpTime = 0;
    private long lastShootTime = 0;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(GameWorld world, double deltaSeconds) {
        if (!enabled || world == null) {
            return;
        }

        PlayerObject player = world.findPlayer().orElse(null);
        if (player == null || !player.isActive()) {
            return;
        }

        GameObject target = findTarget(world, player);
        if (target == null) {
            return;
        }

        // 1. 基础移动逻辑：向目标移动
        double dx = target.getX() - player.getX();
        double dy = target.getY() - player.getY();
        double ax = 0;
        double ay = 0;

        if (Math.abs(dx) > 10) {
            ax = dx > 0 ? 1.0 : -1.0;
        }
        
        // 只有在非重力模式下才使用垂直移动
        if (!world.isGravityEnabled() && Math.abs(dy) > 10) {
            ay = dy > 0 ? 1.0 : -1.0;
        }

        // 2. 障碍检测与跳跃
        if (world.isGravityEnabled()) {
            if (isObstacleAhead(world, player, ax)) {
                long now = System.currentTimeMillis();
                if (now - lastJumpTime > 500) {
                    player.jump(world);
                    lastJumpTime = now;
                }
            }
        }

        // 3. 自动战斗逻辑
        if (isMonsterNearby(world, player)) {
            long now = System.currentTimeMillis();
            if (now - lastShootTime > 300) {
                player.shoot(world);
                lastShootTime = now;
            }
        }

        // 应用 AI 输入
        player.accelerate(ax, ay, deltaSeconds);
    }

    private GameObject findTarget(GameWorld world, PlayerObject player) {
        return switch (world.getWinCondition()) {
            case COLLECT_TARGET_COUNT, CLEAR_ALL_ITEMS -> findNearest(world, player, GameObjectType.ITEM);
            case KILL_ALL_MONSTERS, KILL_TARGET_COUNT -> findNearest(world, player, GameObjectType.MONSTER);
            case REACH_GOAL -> findNearest(world, player, GameObjectType.GOAL);
        };
    }

    private GameObject findNearest(GameWorld world, PlayerObject player, GameObjectType type) {
        List<GameObject> candidates = world.getObjectsByType(type);
        GameObject nearest = null;
        double minDistSq = Double.MAX_VALUE;
        
        for (GameObject obj : candidates) {
            if (!obj.isActive()) {
                continue;
            }
            double d2 = Math.pow(obj.getX() - player.getX(), 2) + Math.pow(obj.getY() - player.getY(), 2);
            if (d2 < minDistSq) {
                minDistSq = d2;
                nearest = obj;
            }
        }
        
        // 如果找不到目标类型，回退到寻找 Goal
        if (nearest == null && type != GameObjectType.GOAL) {
            return findNearest(world, player, GameObjectType.GOAL);
        }
        
        return nearest;
    }

    private boolean isObstacleAhead(GameWorld world, PlayerObject player, double directionX) {
        if (directionX == 0) {
            return false;
        }
        
        // 检测前方是否有固体方块
        int checkX = player.getX() + (directionX > 0 ? player.getWidth() + 10 : -20);
        int checkY = player.getY() + player.getHeight() - 10;
        
        return world.collidesWithSolid(player, checkX, checkY) 
            || world.collidesWithSolid(player, checkX, checkY - player.getHeight() / 2);
    }

    private boolean isMonsterNearby(GameWorld world, PlayerObject player) {
        List<GameObject> monsters = world.getObjectsByType(GameObjectType.MONSTER);
        for (GameObject monster : monsters) {
            if (!monster.isActive()) {
                continue;
            }
            double distSq = Math.pow(monster.getX() - player.getX(), 2) + Math.pow(monster.getY() - player.getY(), 2);
            if (distSq < 400 * 400) { // 400 像素范围内
                return true;
            }
        }
        return false;
    }
}

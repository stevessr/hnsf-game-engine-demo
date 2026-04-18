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

        GameObject goal = findGoal(world);
        if (goal == null) {
            return;
        }

        // 1. 基础移动逻辑：向终点移动
        double dx = goal.getX() - player.getX();
        double dy = goal.getY() - player.getY();
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

    private GameObject findGoal(GameWorld world) {
        return world.getObjectsByType(GameObjectType.GOAL).stream()
            .filter(GameObject::isActive)
            .findFirst()
            .orElse(null);
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

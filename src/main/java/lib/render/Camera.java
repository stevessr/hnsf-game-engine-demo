package lib.render;

import lib.game.GameWorld;
import lib.object.GameObject;

/**
 * 摄像机类，负责跟踪目标并在渲染时提供偏移量。
 */
public final class Camera {
    private double x;
    private double y;
    private final int viewportWidth;
    private final int viewportHeight;

    public Camera(int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public int getX() {
        return (int) Math.round(x);
    }

    public int getY() {
        return (int) Math.round(y);
    }

    public void update(GameWorld world, GameObject target) {
        if (target == null) {
            return;
        }

        // 将目标置于视口中心
        double targetX = target.getX() + target.getWidth() / 2.0 - viewportWidth / 2.0;
        double targetY = target.getY() + target.getHeight() / 2.0 - viewportHeight / 2.0;

        // 限制摄像机范围，不超出地图边界
        x = Math.max(0, Math.min(targetX, world.getWidth() - viewportWidth));
        y = Math.max(0, Math.min(targetY, world.getHeight() - viewportHeight));
        
        // 如果地图比视口小，则居中
        if (world.getWidth() < viewportWidth) {
            x = (world.getWidth() - viewportWidth) / 2.0;
        }
        if (world.getHeight() < viewportHeight) {
            y = (world.getHeight() - viewportHeight) / 2.0;
        }
    }
}

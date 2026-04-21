package lib.render;

import lib.game.GameWorld;
import lib.object.GameObject;

/**
 * 摄像机类，负责跟踪目标（通常是玩家）并在渲染时提供世界坐标到屏幕坐标的偏移量。
 * 
 * <p>核心功能：
 * <ul>
 *   <li>平滑跟踪：将目标保持在视口中心。</li>
 *   <li>边界限制：防止摄像机移动到地图外部区域。</li>
 *   <li>自适应居中：当地图尺寸小于视口时，自动将地图居中显示。</li>
 * </ul>
 */
public final class Camera {
    /** 摄像机在世界坐标系中的 X 坐标 (视口左上角) */
    private double x;
    /** 摄像机在世界坐标系中的 Y 坐标 (视口左上角) */
    private double y;
    /** 视口的宽度 */
    private int viewportWidth;
    /** 视口的高度 */
    private int viewportHeight;

    /**
     * 创建一个新的摄像机实例。
     * 
     * @param viewportWidth  视口宽度 (逻辑像素)
     * @param viewportHeight 视口高度 (逻辑像素)
     */
    public Camera(int viewportWidth, int viewportHeight) {
        setViewportSize(viewportWidth, viewportHeight);
    }

    /**
     * 获取当前摄像机的 X 偏移量。
     * 
     * @return 整数形式的 X 坐标
     */
    public int getX() {
        return (int) Math.round(x);
    }

    /**
     * 获取当前摄像机的 Y 偏移量。
     * 
     * @return 整数形式的 Y 坐标
     */
    public int getY() {
        return (int) Math.round(y);
    }

    /**
     * 根据目标对象的位置更新摄像机坐标。
     * 
     * @param world  当前游戏世界，用于获取边界
     * @param target 要跟踪的目标对象
     */
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

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportSize(int viewportWidth, int viewportHeight) {
        this.viewportWidth = Math.max(1, viewportWidth);
        this.viewportHeight = Math.max(1, viewportHeight);
    }
}

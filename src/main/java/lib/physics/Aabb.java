package lib.physics;

import lib.object.GameObject;

/**
 * 轴对齐边界框 (Axis-Aligned Bounding Box)，用于碰撞检测。
 */
public final class Aabb {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    /**
     * 创建一个新的边界框。
     * 
     * @param x      左上角 X 坐标
     * @param y      左上角 Y 坐标
     * @param width  宽度
     * @param height 高度
     */
    public Aabb(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    /**
     * 从游戏对象创建边界框。
     * 
     * @param gameObject 源对象
     * @return 边界框实例
     */
    public static Aabb from(GameObject gameObject) {
        return new Aabb(gameObject.getX(), gameObject.getY(), gameObject.getWidth(), gameObject.getHeight());
    }

    /**
     * 在指定坐标处为对象创建边界框。
     * 
     * @param gameObject 源对象
     * @param x          假设的 X 坐标
     * @param y          假设的 Y 坐标
     * @return 边界框实例
     */
    public static Aabb at(GameObject gameObject, int x, int y) {
        return new Aabb(x, y, gameObject.getWidth(), gameObject.getHeight());
    }

    /**
     * 检测此边界框是否与另一个相交。
     * 
     * @param other 另一个边界框
     * @return 如果相交则返回 true
     */
    public boolean intersects(Aabb other) {
        if (width == 0 || height == 0 || other.width == 0 || other.height == 0) {
            return false;
        }
        return x < other.x + other.width
            && x + width > other.x
            && y < other.y + other.height
            && y + height > other.y;
    }
}

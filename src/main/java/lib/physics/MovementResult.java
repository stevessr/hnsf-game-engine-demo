package lib.physics;

import lib.object.GameObject;

/**
 * 物理移动尝试的结果。
 * 包含最终确定的位置以及是否被障碍物阻挡的信息。
 */
public final class MovementResult {
    private final int resolvedX;
    private final int resolvedY;
    private final boolean blockedX;
    private final boolean blockedY;
    private final GameObject blockedByX;
    private final GameObject blockedByY;

    /**
     * 创建基础移动结果。
     * 
     * @param resolvedX 确定的 X 坐标
     * @param resolvedY 确定的 Y 坐标
     * @param blockedX  X 轴是否被阻挡
     * @param blockedY  Y 轴是否被阻挡
     */
    public MovementResult(int resolvedX, int resolvedY, boolean blockedX, boolean blockedY) {
        this(resolvedX, resolvedY, blockedX, blockedY, null, null);
    }

    /**
     * 创建详细的移动结果，包含阻挡者。
     * 
     * @param resolvedX  确定的 X 坐标
     * @param resolvedY  确定的 Y 坐标
     * @param blockedX   X 轴是否被阻挡
     * @param blockedY   Y 轴是否被阻挡
     * @param blockedByX X 轴的阻挡者对象
     * @param blockedByY Y 轴的阻挡者对象
     */
    public MovementResult(
        int resolvedX,
        int resolvedY,
        boolean blockedX,
        boolean blockedY,
        GameObject blockedByX,
        GameObject blockedByY
    ) {
        this.resolvedX = resolvedX;
        this.resolvedY = resolvedY;
        this.blockedX = blockedX;
        this.blockedY = blockedY;
        this.blockedByX = blockedByX;
        this.blockedByY = blockedByY;
    }

    public int getResolvedX() {
        return resolvedX;
    }

    public int getResolvedY() {
        return resolvedY;
    }

    public boolean isBlockedX() {
        return blockedX;
    }

    public boolean isBlockedY() {
        return blockedY;
    }

    public GameObject getBlockedByX() {
        return blockedByX;
    }

    public GameObject getBlockedByY() {
        return blockedByY;
    }

    /**
     * @return 只要任一轴被阻挡就返回 true
     */
    public boolean isBlocked() {
        return blockedX || blockedY;
    }
}

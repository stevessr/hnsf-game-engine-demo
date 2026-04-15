package lib.state;

/**
 * 游戏设置接口，允许状态机动态调整游戏的分辨率和 FPS。
 */
public interface GameSettings {
    /**
     * 设置目标 FPS。
     *
     * @param fps 目标 FPS 数值
     */
    void setTargetFPS(int fps);

    /**
     * 获取当前目标 FPS。
     *
     * @return 当前目标 FPS
     */
    int getTargetFPS();

    /**
     * 设置游戏分辨率。
     *
     * @param width 宽度（像素）
     * @param height 高度（像素）
     */
    void setResolution(int width, int height);
}

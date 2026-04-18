package lib.state;

/**
 * 游戏设置接口，允许状态机动态调整游戏的分辨率和 FPS 以及油门。
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
     * 设置油门力度。
     *
     * @param power 力度数值
     */
    void setThrottlePower(int power);

    /**
     * 获取当前油门力度。
     *
     * @return 当前油门力度
     */
    int getThrottlePower();

    /**
     * 设置减速度（百分比）。
     *
     * @param percent 减速度百分比（0-100）
     */
    void setDeceleration(int percent);

    /**
     * 获取当前减速度（百分比）。
     *
     * @return 当前减速度百分比（0-100）
     */
    int getDeceleration();

    /**
     * 设置世界重力是否启用。
     *
     * @param enabled 是否启用重力
     */
    void setGravityEnabled(boolean enabled);

    /**
     * 判断当前世界是否启用了重力。
     *
     * @return 是否启用重力
     */
    boolean isGravityEnabled();

    /**
     * 设置重力强度。
     *
     * @param strength 重力强度
     */
    void setGravityStrength(int strength);

    /**
     * 获取重力强度。
     *
     * @return 重力强度
     */
    int getGravityStrength();

    /**
     * 设置 UI 字体大小。
     *
     * @param fontSize 字体大小
     */
    void setUIFontSize(int fontSize);

    /**
     * 获取 UI 字体大小。
     *
     * @return 字体大小
     */
    int getUIFontSize();

    /**
     * 设置游戏分辨率。
     *
     * @param width 宽度（像素）
     * @param height 高度（像素）
     */
    void setResolution(int width, int height);

    /**
     * 设置逻辑坐标系的分辨率（世界大小）。
     *
     * @param width 宽度（像素）
     * @param height 高度（像素）
     */
    void setLogicalResolution(int width, int height);

    /**
     * 强制重绘界面。
     */
    void forceRepaint();

    /**
     * 光照系统是否开启。
     */
    boolean isLightingEnabled();

    /**
     * 循环切换油门功率。
     */
    void cycleThrottle();

    /**
     * 设置光照系统状态。
     */
    void setLightingEnabled(boolean enabled);

    /**
     * 获取环境光亮度 (0.0 - 1.0)。
     */
    float getAmbientLight();

    /**
     * 设置环境光亮度。
     */
    void setAmbientLight(float intensity);

    /**
     * 获取光照强度倍率 (0.0 - 2.0)。
     */
    float getLightingIntensity();

    /**
     * 设置光照强度倍率。
     */
    void setLightingIntensity(float intensity);
    /**
     * 手动触发一次设置的持久化保存。
     */
    void savePersistentSettings();

    /**
     * 获取调试模式状态。
     */
    boolean isDebugEnabled();

    /**
     * 设置调试模式状态。
     */
    void setDebugEnabled(boolean enabled);

    /**
     * 切换调试模式。
     */
    default void toggleDebug() {
        setDebugEnabled(!isDebugEnabled());
    }
}

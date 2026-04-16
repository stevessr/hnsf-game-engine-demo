package lib.state;

/**
 * 应用级运行时动作接口。
 * 状态机通过此接口请求超出 GameState 切换范围的操作，例如退出程序。
 */
@FunctionalInterface
public interface GameRuntimeActions {
    GameRuntimeActions NO_OP = () -> {
    };

    /**
     * 请求退出当前游戏运行时。
     */
    void requestExit();

    /**
     * 请求加载指定关卡。
     *
     * @param levelName 关卡名称
     */
    default void requestLoadLevel(String levelName) {
    }

    /**
     * 请求打开关卡编辑器。
     */
    default void requestOpenEditor() {
    }

    /**
     * 获取空实现，便于在不支持运行时动作时安全降级。
     *
     * @return 空操作实现
     */
    static GameRuntimeActions noOp() {
        return NO_OP;
    }
}

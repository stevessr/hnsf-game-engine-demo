package lib.state;

/**
 * 游戏状态机接口，定义状态管理的核心操作。
 */
public interface GameStateMachine {

    /**
     * 获取当前游戏状态。
     *
     * @return 当前状态
     */
    GameState getCurrentState();

    /**
     * 尝试转换到新状态。
     *
     * @param newState 目标状态
     * @throws IllegalStateException 如果转换不被允许
     */
    void transitionTo(GameState newState);

    /**
     * 检查是否可以转换到指定状态。
     *
     * @param newState 目标状态
     * @return 如果允许转换返回 true
     */
    boolean canTransitionTo(GameState newState);

    /**
     * 处理当前状态的输入。
     * 根据当前状态调用相应的输入处理逻辑。
     *
     * @param context 游戏状态上下文
     */
    void processInput(GameStateContext context);
}
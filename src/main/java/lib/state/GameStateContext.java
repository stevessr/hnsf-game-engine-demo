package lib.state;

import java.util.Objects;

import lib.game.GameWorld;
import lib.input.GameInputController;

/**
 * 游戏状态上下文，持有状态处理所需的所有依赖。
 * 传递给状态机的 processInput 方法以处理特定状态的输入。
 */
public final class GameStateContext {

    private final GameWorld world;
    private final GameInputController inputController;
    private final GameSettings settings;
    private final GameRuntimeActions runtimeActions;
    private final double deltaSeconds;

    /**
     * 创建游戏状态上下文。
     *
     * @param world 游戏世界，不能为 null
     * @param inputController 输入控制器，不能为 null
     */
    public GameStateContext(GameWorld world, GameInputController inputController) {
        this(world, inputController, null, GameRuntimeActions.noOp(), 1.0 / 60.0);
    }

    /**
     * 创建游戏状态上下文，包含运行时动作支持。
     *
     * @param world 游戏世界，不能为 null
     * @param inputController 输入控制器，不能为 null
     * @param runtimeActions 运行时动作接口，可以为 null
     */
    public GameStateContext(GameWorld world, GameInputController inputController, GameRuntimeActions runtimeActions) {
        this(world, inputController, null, runtimeActions, 1.0 / 60.0);
    }

    /**
     * 创建游戏状态上下文，包含设置支持。
     *
     * @param world 游戏世界，不能为 null
     * @param inputController 输入控制器，不能为 null
     * @param settings 游戏设置接口，可以为 null
     */
    public GameStateContext(GameWorld world, GameInputController inputController, GameSettings settings) {
        this(world, inputController, settings, GameRuntimeActions.noOp(), 1.0 / 60.0);
    }

    /**
     * 创建游戏状态上下文，包含设置与运行时动作支持。
     *
     * @param world 游戏世界，不能为 null
     * @param inputController 输入控制器，不能为 null
     * @param settings 游戏设置接口，可以为 null
     * @param runtimeActions 运行时动作接口，可以为 null
     */
    public GameStateContext(GameWorld world, GameInputController inputController, GameSettings settings,
                            GameRuntimeActions runtimeActions) {
        this(world, inputController, settings, runtimeActions, 1.0 / 60.0);
    }

    /**
     * 创建游戏状态上下文，包含设置、运行时动作和帧时间支持。
     *
     * @param world 游戏世界，不能为 null
     * @param inputController 输入控制器，不能为 null
     * @param settings 游戏设置接口，可以为 null
     * @param runtimeActions 运行时动作接口，可以为 null
     * @param deltaSeconds 当前帧时间（秒）
     */
    public GameStateContext(GameWorld world, GameInputController inputController, GameSettings settings,
                            GameRuntimeActions runtimeActions, double deltaSeconds) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.inputController = Objects.requireNonNull(inputController, "inputController must not be null");
        this.settings = settings;
        this.runtimeActions = runtimeActions == null ? GameRuntimeActions.noOp() : runtimeActions;
        this.deltaSeconds = deltaSeconds > 0.0 ? deltaSeconds : 1.0 / 60.0;
    }

    /**
     * 获取游戏世界。
     *
     * @return 游戏世界
     */
    public GameWorld getWorld() {
        return world;
    }

    /**
     * 获取输入控制器。
     *
     * @return 输入控制器
     */
    public GameInputController getInputController() {
        return inputController;
    }

    /**
     * 获取游戏设置接口。
     *
     * @return 游戏设置接口，如果不支持则返回 null
     */
    public GameSettings getSettings() {
        return settings;
    }

    /**
     * 获取运行时动作接口。
     *
     * @return 运行时动作接口，永不为 null
     */
    public GameRuntimeActions getRuntimeActions() {
        return runtimeActions;
    }

    /**
     * 获取当前帧时间（秒）。
     *
     * @return 当前帧时间
     */
    public double getDeltaSeconds() {
        return deltaSeconds;
    }
}

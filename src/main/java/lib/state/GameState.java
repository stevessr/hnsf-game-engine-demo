package lib.state;

/**
 * 游戏状态枚举，定义游戏可能处于的所有状态。
 * 每个状态定义了在该状态下允许的操作。
 */
public enum GameState {
    /**
     * 菜单状态 - 用户正在菜单中导航。
     * 此状态下只处理菜单导航输入，游戏世界不更新。
     */
    MENU,

    /**
     * 游戏进行状态 - 玩家正在游戏中活动。
     * 此状态下处理玩家移动和游戏逻辑，游戏世界正常更新。
     */
    PLAYING,

    /**
     * 对话状态 - 玩家正在与NPC对话或查看剧情。
     * 此状态下只处理对话推进输入，玩家不能移动。
     */
    DIALOG,

    /**
     * 暂停状态 - 游戏暂停中。
     * 此状态下只响应恢复游戏的输入，游戏世界不更新。
     */
    PAUSED,

    /**
     * 游戏结束状态 - 玩家死亡或任务失败。
     */
    GAMEOVER,

    /**
     * 结算状态 - 关卡完成或游戏胜利。
     */
    SETTLEMENT;

    /**
     * 判断当前状态是否允许玩家移动。
     *
     * @return 如果允许玩家移动返回 true
     */
    public boolean allowsPlayerMovement() {
        return this == PLAYING;
    }

    /**
     * 判断当前状态是否允许游戏世界更新。
     *
     * @return 如果允许世界更新返回 true
     */
    public boolean allowsWorldUpdate() {
        return this == PLAYING || this == GAMEOVER || this == SETTLEMENT;
    }

    /**
     * 判断当前状态是否允许菜单导航。
     *
     * @return 如果允许菜单导航返回 true
     */
    public boolean allowsMenuNavigation() {
        return this == MENU || this == PAUSED || this == GAMEOVER || this == SETTLEMENT;
    }

    /**
     * 判断当前状态是否允许对话交互。
     *
     * @return 如果允许对话交互返回 true
     */
    public boolean allowsDialogInteraction() {
        return this == DIALOG;
    }
}

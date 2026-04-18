package lib.game;

/**
 * 游戏通关条件类型。
 */
public enum WinConditionType {
    /** 到达终点 (默认) */
    REACH_GOAL,
    /** 消灭所有怪物 */
    KILL_ALL_MONSTERS,
    /** 消灭指定数量的怪物 */
    KILL_TARGET_COUNT,
    /** 收集指定数量的物品 */
    COLLECT_TARGET_COUNT,
    /** 清除地图上所有可收集物品 */
    CLEAR_ALL_ITEMS
}

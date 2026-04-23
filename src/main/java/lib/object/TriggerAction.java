package lib.object;

/**
 * 触发器动作类型。
 */
public enum TriggerAction {
    ACTIVATE("启用目标", true),
    DEACTIVATE("关闭目标", true),
    TOGGLE("切换目标", true),
    SET_RESPAWN("设置复活点", false),
    SHOW_GOALS("显示关卡目标", false),
    HIDE_GOALS("隐藏关卡目标", false),
    TOGGLE_GOALS("切换关卡目标", false),
    RESPAWN_PLAYER("重生玩家", false);

    private final String displayName;
    private final boolean requiresTargetName;

    TriggerAction(String displayName, boolean requiresTargetName) {
        this.displayName = displayName;
        this.requiresTargetName = requiresTargetName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public boolean requiresTargetName() {
        return requiresTargetName;
    }

    public static TriggerAction fromSerialized(String value) {
        if (value == null || value.isBlank()) {
            return TOGGLE;
        }
        String normalized = value.trim();
        for (TriggerAction action : values()) {
            if (action.name().equalsIgnoreCase(normalized) || action.displayName.equalsIgnoreCase(normalized)) {
                return action;
            }
        }
        return TOGGLE;
    }
}

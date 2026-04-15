package lib.state;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lib.game.GameWorld;
import lib.input.InputAction;
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.object.PlayerObject;

/**
 * 默认游戏状态机实现。
 * 管理游戏状态转换和状态特定的输入处理。
 */
public final class DefaultGameStateMachine implements GameStateMachine {

    private GameState currentState;
    private GameState previousState;
    private final Map<GameState, Set<GameState>> allowedTransitions;

    /**
     * 创建默认游戏状态机。
     * 初始状态为 MENU。
     */
    public DefaultGameStateMachine() {
        this(GameState.MENU);
    }

    /**
     * 创建指定初始状态的游戏状态机。
     *
     * @param initialState 初始状态
     */
    public DefaultGameStateMachine(GameState initialState) {
        this.currentState = Objects.requireNonNull(initialState, "initialState must not be null");
        this.previousState = initialState;
        this.allowedTransitions = new EnumMap<>(GameState.class);
        initializeTransitions();
    }

    private void initializeTransitions() {
        // MENU -> DIALOG/PLAYING: 选择 "Start" 或 "开始游戏"
        allowedTransitions.put(GameState.MENU, EnumSet.of(GameState.DIALOG, GameState.PLAYING));

        // PLAYING -> PAUSED, DIALOG, MENU
        allowedTransitions.put(GameState.PLAYING,
            EnumSet.of(GameState.PAUSED, GameState.DIALOG, GameState.MENU));

        // DIALOG -> PLAYING: 对话结束
        allowedTransitions.put(GameState.DIALOG, EnumSet.of(GameState.PLAYING));

        // PAUSED -> PLAYING: 按 ESC/P 恢复
        allowedTransitions.put(GameState.PAUSED, EnumSet.of(GameState.PLAYING));
    }

    @Override
    public GameState getCurrentState() {
        return currentState;
    }

    @Override
    public boolean canTransitionTo(GameState newState) {
        if (newState == currentState) {
            return true;
        }
        Set<GameState> allowed = allowedTransitions.get(currentState);
        return allowed != null && allowed.contains(newState);
    }

    @Override
    public void transitionTo(GameState newState) {
        Objects.requireNonNull(newState, "newState must not be null");
        if (newState == currentState) {
            return;
        }
        if (!canTransitionTo(newState)) {
            throw new IllegalStateException(
                "Cannot transition from " + currentState + " to " + newState);
        }
        // 记录非暂停状态的转换，用于从暂停恢复
        if (currentState != GameState.PAUSED) {
            previousState = currentState;
        }
        currentState = newState;
    }

    /**
     * 切换暂停状态。
     * 在 PLAYING 和 PAUSED 之间切换。
     */
    public void togglePause(GameWorld world) {
        if (currentState == GameState.PAUSED) {
            removePauseMenu(world);
            transitionTo(previousState == GameState.PAUSED ? GameState.PLAYING : previousState);
        } else if (currentState == GameState.PLAYING) {
            transitionTo(GameState.PAUSED);
            createPauseMenu(world);
        }
    }

    private void createPauseMenu(GameWorld world) {
        if (world == null) return;
        int menuWidth = 200;
        int menuHeight = 120;
        int menuX = (world.getWidth() - menuWidth) / 2;
        int menuY = (world.getHeight() - menuHeight) / 2;
        MenuObject pauseMenu = new MenuObject(
            "pause-menu",
            menuX,
            menuY,
            menuWidth,
            menuHeight,
            "Paused",
            List.of("Resume", "Exit to Menu")
        );
        world.addObject(pauseMenu);
    }

    private void removePauseMenu(GameWorld world) {
        if (world == null) return;
        world.getObjectsByType(GameObjectType.MENU).stream()
            .filter(obj -> "pause-menu".equals(obj.getName()))
            .findFirst()
            .ifPresent(world::removeObject);
    }

    @Override
    public void processInput(GameStateContext context) {
        Objects.requireNonNull(context, "context must not be null");
        switch (currentState) {
            case MENU -> processMenuInput(context);
            case PLAYING -> processPlayingInput(context);
            case DIALOG -> processDialogInput(context);
            case PAUSED -> processPausedInput(context);
        }
    }

    private void processMenuInput(GameStateContext context) {
        var inputController = context.getInputController();
        var keyboard = inputController.getKeyboardManager();
        var actionMapper = inputController.getActionMapper();

        MenuObject menu = findActiveMenu(context.getWorld());
        if (menu == null) {
            return;
        }

        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_PREVIOUS, keyboard)) {
            menu.previousOption();
            syncActiveDialog(context.getWorld(), "切换到", menu.getSelectedOption());
        }
        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_NEXT, keyboard)) {
            menu.nextOption();
            syncActiveDialog(context.getWorld(), "切换到", menu.getSelectedOption());
        }
        if (actionMapper.isMouseJustActivated(InputAction.MENU_CONFIRM, inputController.getMouseManager())) {
            int hoveredIndex = findHoveredOptionIndex(menu, inputController.getMouseManager().getMouseX(),
                inputController.getMouseManager().getMouseY());
            if (hoveredIndex >= 0) {
                menu.setSelectedIndex(hoveredIndex);
            }
            syncActiveDialog(context.getWorld(), "已确认", menu.getSelectedOption());
            handleMenuSelection(menu, context);
        } else if (actionMapper.isKeyboardJustActivated(InputAction.MENU_CONFIRM, keyboard)) {
            syncActiveDialog(context.getWorld(), "已确认", menu.getSelectedOption());
            handleMenuSelection(menu, context);
        }
    }

    private void handleMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if ("pause-menu".equals(menu.getName())) {
            handlePauseMenuSelection(menu, context);
            return;
        }

        if (isStartOption(selected)) {
            menu.setActive(false);
            DialogObject dialog = findActiveDialog(context.getWorld());
            transitionTo(dialog == null ? GameState.PLAYING : GameState.DIALOG);
        } else if (isExitOption(selected)) {
            transitionTo(GameState.MENU);
        }
        clearPlayerMovement(context);
    }

    private void handlePauseMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if (isResumeOption(selected)) {
            removePauseMenu(context.getWorld());
            transitionTo(previousState);
        } else if (isExitToMenuOption(selected)) {
            removePauseMenu(context.getWorld());
            // 激活主菜单
            context.getWorld().getObjectsByType(GameObjectType.MENU).stream()
                .filter(obj -> "main-menu".equals(obj.getName()))
                .findFirst()
                .ifPresent(obj -> obj.setActive(true));
            transitionTo(GameState.MENU);
        }
        clearPlayerMovement(context);
    }

    private void processPlayingInput(GameStateContext context) {
        var inputController = context.getInputController();
        var keyboard = inputController.getKeyboardManager();
        var actionMapper = inputController.getActionMapper();

        // 检查暂停请求
        if (actionMapper.isKeyboardJustActivated(InputAction.PAUSE, keyboard)) {
            togglePause(context.getWorld());
            clearPlayerMovement(context);
            return;
        }

        if (findActiveDialog(context.getWorld()) != null) {
            transitionTo(GameState.DIALOG);
            clearPlayerMovement(context);
            return;
        }

        // 处理玩家移动
        inputController.applyPlayerMovement(
            context.getWorld().findPlayer().orElse(null));
    }

    private void processDialogInput(GameStateContext context) {
        var inputController = context.getInputController();
        var keyboard = inputController.getKeyboardManager();
        var actionMapper = inputController.getActionMapper();
        DialogObject dialog = findActiveDialog(context.getWorld());

        if (dialog == null) {
            transitionTo(GameState.PLAYING);
            return;
        }

        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_CONFIRM, keyboard)
            || actionMapper.isKeyboardJustActivated(InputAction.DIALOG_NEXT, keyboard)
            || actionMapper.isMouseJustActivated(InputAction.MENU_CONFIRM, inputController.getMouseManager())) {
            dialog.setActive(false);
            transitionTo(GameState.PLAYING);
        }
        clearPlayerMovement(context);
    }

    private void processPausedInput(GameStateContext context) {
        var inputController = context.getInputController();
        var keyboard = inputController.getKeyboardManager();
        var actionMapper = inputController.getActionMapper();

        if (actionMapper.isKeyboardJustActivated(InputAction.PAUSE, keyboard)) {
            togglePause(context.getWorld());
            return;
        }
        
        // 处理暂停菜单输入
        processMenuInput(context);
        clearPlayerMovement(context);
    }

    private void clearPlayerMovement(GameStateContext context) {
        PlayerObject player = context.getWorld().findPlayer().orElse(null);
        if (player != null) {
            player.setVelocity(0, 0);
        }
    }

    private void syncActiveDialog(GameWorld world, String prefix, String selectedOption) {
        DialogObject dialog = findActiveDialog(world);
        if (dialog != null) {
            dialog.setMessage(prefix + "菜单项：" + selectedOption);
        }
    }

    private MenuObject findActiveMenu(GameWorld world) {
        for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
            if (object instanceof MenuObject menu && menu.isActive()) {
                return menu;
            }
        }
        return null;
    }

    private DialogObject findActiveDialog(GameWorld world) {
        for (GameObject object : world.getObjectsByType(GameObjectType.DIALOG)) {
            if (object instanceof DialogObject dialog && dialog.isActive()) {
                return dialog;
            }
        }
        return null;
    }

    private int findHoveredOptionIndex(MenuObject menu, int mouseX, int mouseY) {
        if (!isInsideMenu(menu, mouseX, mouseY)) {
            return -1;
        }
        int optionStartY = menu.getY() + 34;
        int optionHeight = 18;
        int relativeY = mouseY - optionStartY;
        int hoveredIndex = relativeY / optionHeight;
        if (hoveredIndex < 0 || hoveredIndex >= menu.getOptions().size()) {
            return -1;
        }
        return hoveredIndex;
    }

    private boolean isInsideMenu(MenuObject menu, int mouseX, int mouseY) {
        return mouseX >= menu.getX()
            && mouseX <= menu.getX() + menu.getWidth()
            && mouseY >= menu.getY()
            && mouseY <= menu.getY() + menu.getHeight();
    }

    private boolean isStartOption(String selected) {
        List<String> startOptions = List.of("Start", "start", "开始游戏", "开始");
        return startOptions.contains(selected);
    }

    private boolean isExitOption(String selected) {
        List<String> exitOptions = List.of("Exit", "exit", "退出", "退出游戏");
        return exitOptions.contains(selected);
    }

    private boolean isResumeOption(String selected) {
        List<String> options = List.of("Resume", "resume", "恢复", "继续游戏");
        return options.contains(selected);
    }

    private boolean isExitToMenuOption(String selected) {
        List<String> options = List.of("Exit to Menu", "返回主菜单", "退出到菜单");
        return options.contains(selected);
    }
}

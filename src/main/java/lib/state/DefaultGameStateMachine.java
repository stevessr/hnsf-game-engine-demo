package lib.state;

import java.util.ArrayList;
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
    private static final String MAIN_MENU_NAME = "main-menu";
    private static final String LEVEL_SELECT_MENU_NAME = "level-select-menu";
    private static final String PAUSE_MENU_NAME = "pause-menu";
    private static final String OPTIONS_MENU_NAME = "options-menu";

    private GameState currentState;
    private GameState previousState;
    private final Map<GameState, Set<GameState>> allowedTransitions;
    private DialogObject hiddenDialog;

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
        allowedTransitions.put(GameState.MENU, EnumSet.of(GameState.DIALOG, GameState.PLAYING));
        allowedTransitions.put(GameState.PLAYING, EnumSet.of(GameState.PAUSED, GameState.DIALOG, GameState.MENU));
        allowedTransitions.put(GameState.DIALOG, EnumSet.of(GameState.PLAYING));
        allowedTransitions.put(GameState.PAUSED, EnumSet.of(GameState.PLAYING, GameState.MENU));
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
            throw new IllegalStateException("Cannot transition from " + currentState + " to " + newState);
        }
        if (currentState != GameState.PAUSED) {
            previousState = currentState;
        }
        currentState = newState;
    }

    /**
     * 切换暂停状态。
     * 在 PLAYING 和 PAUSED 之间切换。
     */
    public void togglePause(GameWorld world, GameSettings settings) {
        if (currentState == GameState.PAUSED) {
            removeMenu(world, PAUSE_MENU_NAME);
            removeMenu(world, OPTIONS_MENU_NAME);
            restoreHiddenDialog(world);
            transitionTo(previousState == GameState.PAUSED ? GameState.PLAYING : previousState);
            return;
        }
        if (currentState == GameState.PLAYING) {
            hideActiveDialog(world);
            transitionTo(GameState.PAUSED);
            createPauseMenu(world, settings);
        }
    }

    private void hideActiveDialog(GameWorld world) {
        if (world == null) {
            return;
        }
        DialogObject dialog = findActiveDialog(world);
        if (dialog != null && dialog.isActive()) {
            hiddenDialog = dialog;
            dialog.setActive(false);
        }
    }

    private void restoreHiddenDialog(GameWorld world) {
        if (world == null || hiddenDialog == null) {
            return;
        }
        hiddenDialog.setActive(true);
        hiddenDialog = null;
    }

    private void createPauseMenu(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeMenu(world, PAUSE_MENU_NAME);
        int menuWidth = 220;
        int fontSize = settings != null ? settings.getUIFontSize() : 18;
        int menuHeight = Math.max(160, 48 + (3 * Math.max(20, fontSize + 8)) + 12);
        int menuX = (world.getWidth() - menuWidth) / 2;
        int menuY = (world.getHeight() - menuHeight) / 2;
        MenuObject pauseMenu = new MenuObject(
            PAUSE_MENU_NAME,
            menuX,
            menuY,
            menuWidth,
            menuHeight,
            "Paused",
            List.of("Resume", "Options", "Exit to Menu")
        );
        pauseMenu.setFontSize(fontSize);
        pauseMenu.setSize(menuWidth, Math.max(menuHeight, pauseMenu.getPreferredHeight()));
        pauseMenu.setPosition(
            Math.max(0, (world.getWidth() - pauseMenu.getWidth()) / 2),
            Math.max(0, (world.getHeight() - pauseMenu.getHeight()) / 2)
        );
        world.addObject(pauseMenu);
    }

    private void removeMenu(GameWorld world, String name) {
        if (world == null) {
            return;
        }
        world.getObjectsByType(GameObjectType.MENU).stream()
            .filter(obj -> name.equals(obj.getName()))
            .forEach(world::removeObject);
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
        GameWorld world = context.getWorld();

        MenuObject menu = findActiveMenu(world);
        if (menu == null) {
            return;
        }

        if (actionMapper.isKeyboardJustActivated(InputAction.PAUSE, keyboard)) {
            if (isOptionsMenu(menu)) {
                world.removeObject(menu);
                restoreMenuAfterOptions(world);
                clearPlayerMovement(context);
                return;
            }
            if (isLevelSelectMenu(menu)) {
                activateMainMenu(world);
                clearPlayerMovement(context);
                return;
            }
        }

        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_PREVIOUS, keyboard)) {
            menu.previousOption();
            syncActiveDialog(world, "切换到", menu.getSelectedOption());
        }
        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_NEXT, keyboard)) {
            menu.nextOption();
            syncActiveDialog(world, "切换到", menu.getSelectedOption());
        }
        if (actionMapper.isMouseJustActivated(InputAction.MENU_CONFIRM, inputController.getMouseManager())) {
            int hoveredIndex = findHoveredOptionIndex(menu, inputController.getMouseManager().getMouseX(),
                inputController.getMouseManager().getMouseY());
            if (hoveredIndex >= 0) {
                menu.setSelectedIndex(hoveredIndex);
            }
            syncActiveDialog(world, "已确认", menu.getSelectedOption());
            handleMenuSelection(menu, context);
        } else if (actionMapper.isKeyboardJustActivated(InputAction.MENU_CONFIRM, keyboard)) {
            syncActiveDialog(world, "已确认", menu.getSelectedOption());
            handleMenuSelection(menu, context);
        }
    }

    private void handleMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        GameWorld world = context.getWorld();
        if (isPauseMenu(menu)) {
            handlePauseMenuSelection(menu, context);
            return;
        }
        if (isOptionsMenu(menu)) {
            handleOptionsMenuSelection(menu, context);
            return;
        }
        if (isLevelSelectMenu(menu)) {
            handleLevelSelectSelection(menu, context);
            return;
        }

        if (isStartOption(selected)) {
            handleMainMenuSelectionForGameplay(menu, context);
        } else if (isLevelsOption(selected)) {
            activateLevelSelectMenu(world);
            clearPlayerMovement(context);
        } else if (isEditorOption(selected)) {
            context.getRuntimeActions().requestOpenEditor();
            clearPlayerMovement(context);
        } else if (isOptionsOption(selected)) {
            showOptionsMenu(world, context.getSettings());
            clearPlayerMovement(context);
        } else if (isExitOption(selected)) {
            context.getRuntimeActions().requestExit();
            clearPlayerMovement(context);
        } else if (isResumeOption(selected)) {
            transitionTo(previousState);
            clearPlayerMovement(context);
        }
    }

    private void handleLevelSelectSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        GameWorld world = context.getWorld();
        if (isBackOption(selected)) {
            activateMainMenu(world);
            transitionTo(GameState.MENU);
            clearPlayerMovement(context);
            return;
        }

        menu.setActive(false);
        context.getRuntimeActions().requestLoadLevel(selected);
        DialogObject dialog = findActiveDialog(world);
        transitionTo(dialog == null ? GameState.PLAYING : GameState.DIALOG);
        clearPlayerMovement(context);
    }

    private void handlePauseMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if (isResumeOption(selected)) {
            removeMenu(context.getWorld(), PAUSE_MENU_NAME);
            transitionTo(previousState);
        } else if (isOptionsOption(selected)) {
            showOptionsMenu(context.getWorld(), context.getSettings());
        } else if (isExitToMenuOption(selected)) {
            removeMenu(context.getWorld(), PAUSE_MENU_NAME);
            removeMenu(context.getWorld(), OPTIONS_MENU_NAME);
            activateMainMenu(context.getWorld());
            transitionTo(GameState.MENU);
        }
        clearPlayerMovement(context);
    }

    private void showOptionsMenu(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeMenu(world, OPTIONS_MENU_NAME);
        world.getObjectsByType(GameObjectType.MENU).stream()
            .filter(GameObject::isActive)
            .forEach(obj -> obj.setActive(false));

        int menuWidth = 320;
        int fontSize = settings != null ? settings.getUIFontSize() : 18;
        int menuHeight = Math.max(220, 56 + (6 * Math.max(20, fontSize + 8)) + 12);
        int menuX = (world.getWidth() - menuWidth) / 2;
        int menuY = (world.getHeight() - menuHeight) / 2;

        int throttle = settings != null ? settings.getThrottlePower() : 600;
        int deceleration = settings != null ? settings.getDeceleration() : 92;
        boolean gravityEnabled = settings != null && settings.isGravityEnabled();

        MenuObject optionsMenu = new MenuObject(
            OPTIONS_MENU_NAME,
            menuX,
            menuY,
            menuWidth,
            menuHeight,
            "Options",
            List.of(
                "Resolution: " + world.getWidth() + "x" + world.getHeight(),
                "FPS: " + (settings != null ? settings.getTargetFPS() : 60),
                "Throttle: " + throttle,
                "Deceleration: " + deceleration + "%",
                "Gravity: " + (gravityEnabled ? "On" : "Off"),
                "UI Font: " + fontSize,
                "Back"
            )
        );
        optionsMenu.setFontSize(fontSize);
        optionsMenu.setSize(menuWidth, Math.max(menuHeight, optionsMenu.getPreferredHeight()));
        optionsMenu.setPosition(
            Math.max(0, (world.getWidth() - optionsMenu.getWidth()) / 2),
            Math.max(0, (world.getHeight() - optionsMenu.getHeight()) / 2)
        );
        world.addObject(optionsMenu);
    }

    private void handleOptionsMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        GameSettings settings = context.getSettings();
        if (isResolutionOption(selected)) {
            cycleResolution(context.getWorld(), settings, menu);
        } else if (isFPSOption(selected)) {
            cycleFPS(settings, menu);
        } else if (isThrottleOption(selected)) {
            cycleThrottle(settings, menu);
        } else if (isDecelerationOption(selected)) {
            cycleDeceleration(settings, menu);
        } else if (isGravityOption(selected)) {
            cycleGravity(context.getWorld(), settings, menu);
        } else if (isUIFontSizeOption(selected)) {
            cycleUIFontSize(context.getWorld(), settings, menu);
        } else if (isBackOption(selected)) {
            context.getWorld().removeObject(menu);
            restoreMenuAfterOptions(context.getWorld());
        }
    }

    private void handleMainMenuSelectionForGameplay(MenuObject menu, GameStateContext context) {
        menu.setActive(false);
        DialogObject dialog = findActiveDialog(context.getWorld());
        transitionTo(dialog == null ? GameState.PLAYING : GameState.DIALOG);
        clearPlayerMovement(context);
    }

    private void cycleThrottle(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        int[] powers = {200, 400, 600, 800, 1000};
        int current = settings.getThrottlePower();
        int nextIdx = 0;
        for (int i = 0; i < powers.length; i++) {
            if (powers[i] >= current) {
                nextIdx = (i + 1) % powers.length;
                break;
            }
        }
        int newPower = powers[nextIdx];
        settings.setThrottlePower(newPower);

        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (isThrottleOption(options.get(i))) {
                options.set(i, "Throttle: " + newPower);
            }
        }
        menu.setOptions(options);
    }

    private void cycleDeceleration(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        int[] decelerations = {80, 85, 90, 92, 95, 98};
        int current = settings.getDeceleration();
        int nextIdx = 0;
        for (int i = 0; i < decelerations.length; i++) {
            if (decelerations[i] >= current) {
                nextIdx = (i + 1) % decelerations.length;
                break;
            }
        }
        int newDeceleration = decelerations[nextIdx];
        settings.setDeceleration(newDeceleration);

        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (isDecelerationOption(options.get(i))) {
                options.set(i, "Deceleration: " + newDeceleration + "%");
            }
        }
        menu.setOptions(options);
    }

    private void cycleGravity(GameWorld world, GameSettings settings, MenuObject menu) {
        if (settings == null || world == null) {
            return;
        }
        boolean enabled = !settings.isGravityEnabled();
        settings.setGravityEnabled(enabled);

        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (isGravityOption(options.get(i))) {
                options.set(i, "Gravity: " + (enabled ? "On" : "Off"));
            }
        }
        menu.setOptions(options);
    }

    private void cycleUIFontSize(GameWorld world, GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        int[] fontSizes = {14, 16, 18, 20, 22, 24, 28};
        int currentFontSize = settings.getUIFontSize();
        int nextIdx = 0;
        for (int i = 0; i < fontSizes.length; i++) {
            if (fontSizes[i] >= currentFontSize) {
                nextIdx = (i + 1) % fontSizes.length;
                break;
            }
        }
        int newFontSize = fontSizes[nextIdx];
        settings.setUIFontSize(newFontSize);

        menu.setFontSize(newFontSize);
        menu.setSize(menu.getWidth(), Math.max(menu.getHeight(), menu.getPreferredHeight()));
        if (world != null) {
            menu.setPosition(
                Math.max(0, (world.getWidth() - menu.getWidth()) / 2),
                Math.max(0, (world.getHeight() - menu.getHeight()) / 2)
            );
        }

        if (world != null) {
            for (GameObject object : world.getObjectsByType(GameObjectType.DIALOG)) {
                if (object instanceof DialogObject dialog) {
                    dialog.setFontSize(newFontSize);
                }
            }
            for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
                if (object instanceof MenuObject otherMenu) {
                    otherMenu.setFontSize(newFontSize);
                    otherMenu.setSize(otherMenu.getWidth(), Math.max(otherMenu.getHeight(), otherMenu.getPreferredHeight()));
                }
            }
        }

        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (isUIFontSizeOption(options.get(i))) {
                options.set(i, "UI Font: " + newFontSize);
            }
        }
        menu.setOptions(options);
    }

    private void cycleResolution(GameWorld world, GameSettings settings, MenuObject menu) {
        if (settings == null || world == null) {
            return;
        }
        int[][] resolutions = {{960, 540}, {1280, 720}, {1920, 1080}, {640, 480}};
        int currentWidth = world.getWidth();
        int nextIdx = 0;
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i][0] == currentWidth) {
                nextIdx = (i + 1) % resolutions.length;
                break;
            }
        }
        int newW = resolutions[nextIdx][0];
        int newH = resolutions[nextIdx][1];
        settings.setResolution(newW, newH);
        menu.setPosition(Math.max(0, (newW - menu.getWidth()) / 2), Math.max(0, (newH - menu.getHeight()) / 2));

        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (isResolutionOption(options.get(i))) {
                options.set(i, "Resolution: " + newW + "x" + newH);
            }
        }
        menu.setOptions(options);
    }

    private void cycleFPS(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        int[] fpsOptions = {30, 60, 120, 144};
        int currentFPS = settings.getTargetFPS();
        int nextIdx = 0;
        for (int i = 0; i < fpsOptions.length; i++) {
            if (fpsOptions[i] == currentFPS) {
                nextIdx = (i + 1) % fpsOptions.length;
                break;
            }
        }
        int newFPS = fpsOptions[nextIdx];
        settings.setTargetFPS(newFPS);

        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (isFPSOption(options.get(i))) {
                options.set(i, "FPS: " + newFPS);
            }
        }
        menu.setOptions(options);
    }

    private void processPlayingInput(GameStateContext context) {
        var inputController = context.getInputController();
        var keyboard = inputController.getKeyboardManager();
        var actionMapper = inputController.getActionMapper();

        if (actionMapper.isKeyboardJustActivated(InputAction.PAUSE, keyboard)) {
            togglePause(context.getWorld(), context.getSettings());
            clearPlayerMovement(context);
            return;
        }

        if (findActiveDialog(context.getWorld()) != null) {
            transitionTo(GameState.DIALOG);
            clearPlayerMovement(context);
            return;
        }

        PlayerObject player = context.getWorld().findPlayer().orElse(null);
        if (player != null && player.isActive()) {
            double ax = 0;
            double ay = 0;
            if (actionMapper.isActive(InputAction.MOVE_LEFT, keyboard, inputController.getMouseManager())) {
                ax -= 1.0;
            }
            if (actionMapper.isActive(InputAction.MOVE_RIGHT, keyboard, inputController.getMouseManager())) {
                ax += 1.0;
            }
            if (actionMapper.isActive(InputAction.MOVE_UP, keyboard, inputController.getMouseManager())) {
                ay -= 1.0;
            }
            if (actionMapper.isActive(InputAction.MOVE_DOWN, keyboard, inputController.getMouseManager())) {
                ay += 1.0;
            }
            if (actionMapper.isActive(InputAction.THROTTLE_LEFT, keyboard, inputController.getMouseManager())) {
                ax -= 1.0;
            }
            if (actionMapper.isActive(InputAction.THROTTLE_RIGHT, keyboard, inputController.getMouseManager())) {
                ax += 1.0;
            }
            if (actionMapper.isActive(InputAction.THROTTLE_UP, keyboard, inputController.getMouseManager())) {
                ay -= 1.0;
            }
            if (actionMapper.isActive(InputAction.THROTTLE_DOWN, keyboard, inputController.getMouseManager())) {
                ay += 1.0;
            }

            if (ax != 0 && ay != 0) {
                double mag = Math.sqrt(ax * ax + ay * ay);
                ax /= mag;
                ay /= mag;
            }

            player.accelerate(ax, ay, 1.0 / 60.0);
        }
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
            togglePause(context.getWorld(), context.getSettings());
            return;
        }

        processMenuInput(context);
        clearPlayerMovement(context);
    }

    private void clearPlayerMovement(GameStateContext context) {
        if (context == null || context.getWorld() == null) {
            return;
        }
        context.getWorld().findPlayer().ifPresent(player -> player.setVelocity(0.0, 0.0));
    }

    private void syncActiveDialog(GameWorld world, String prefix, String selectedOption) {
        DialogObject dialog = findActiveDialog(world);
        if (dialog != null) {
            dialog.setMessage(prefix + "菜单项：" + selectedOption);
        }
    }

    private MenuObject findActiveMenu(GameWorld world) {
        if (world == null) {
            return null;
        }
        for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
            if (object instanceof MenuObject menu && menu.isActive()) {
                return menu;
            }
        }
        return null;
    }

    private MenuObject findMenuByName(GameWorld world, String name) {
        if (world == null) {
            return null;
        }
        for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
            if (object instanceof MenuObject menu && name.equals(menu.getName())) {
                return menu;
            }
        }
        return null;
    }

    private DialogObject findActiveDialog(GameWorld world) {
        if (world == null) {
            return null;
        }
        for (GameObject object : world.getObjectsByType(GameObjectType.DIALOG)) {
            if (object instanceof DialogObject dialog && dialog.isActive()) {
                return dialog;
            }
        }
        return null;
    }

    private void activateMainMenu(GameWorld world) {
        if (world == null) {
            return;
        }
        MenuObject mainMenu = findMenuByName(world, MAIN_MENU_NAME);
        if (mainMenu != null) {
            mainMenu.setActive(true);
        }
        MenuObject levelMenu = findMenuByName(world, LEVEL_SELECT_MENU_NAME);
        if (levelMenu != null) {
            levelMenu.setActive(false);
        }
    }

    private void activateLevelSelectMenu(GameWorld world) {
        if (world == null) {
            return;
        }
        MenuObject mainMenu = findMenuByName(world, MAIN_MENU_NAME);
        if (mainMenu != null) {
            mainMenu.setActive(false);
        }
        MenuObject levelMenu = findMenuByName(world, LEVEL_SELECT_MENU_NAME);
        if (levelMenu != null) {
            levelMenu.setActive(true);
            if (levelMenu.getSelectedIndex() < 0) {
                levelMenu.setSelectedIndex(0);
            }
        }
    }

    private void restoreMenuAfterOptions(GameWorld world) {
        if (world == null) {
            return;
        }
        if (currentState == GameState.PAUSED) {
            MenuObject pauseMenu = findMenuByName(world, PAUSE_MENU_NAME);
            if (pauseMenu != null) {
                pauseMenu.setActive(true);
                return;
            }
        }
        MenuObject mainMenu = findMenuByName(world, MAIN_MENU_NAME);
        if (mainMenu != null) {
            mainMenu.setActive(true);
            return;
        }
        MenuObject levelMenu = findMenuByName(world, LEVEL_SELECT_MENU_NAME);
        if (levelMenu != null) {
            levelMenu.setActive(true);
        }
    }

    private int findHoveredOptionIndex(MenuObject menu, int mouseX, int mouseY) {
        if (!isInsideMenu(menu, mouseX, mouseY)) {
            return -1;
        }
        int optionStartY = menu.getOptionStartY();
        int optionHeight = menu.getOptionLineHeight();
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
        List<String> options = List.of("Start", "start", "开始游戏", "开始");
        return options.contains(selected);
    }

    private boolean isLevelsOption(String selected) {
        List<String> options = List.of("Levels", "Level Select", "level select", "关卡选择", "选择关卡");
        return options.contains(selected);
    }

    private boolean isEditorOption(String selected) {
        List<String> options = List.of("Editor", "Level Editor", "关卡编辑器", "编辑器");
        return options.contains(selected);
    }

    private boolean isExitOption(String selected) {
        List<String> options = List.of("Exit", "exit", "退出", "退出游戏");
        return options.contains(selected);
    }

    private boolean isResumeOption(String selected) {
        List<String> options = List.of("Resume", "resume", "恢复", "继续游戏");
        return options.contains(selected);
    }

    private boolean isExitToMenuOption(String selected) {
        List<String> options = List.of("Exit to Menu", "返回主菜单", "退出到菜单");
        return options.contains(selected);
    }

    private boolean isOptionsOption(String selected) {
        List<String> options = List.of("Options", "options", "选项", "设置");
        return options.contains(selected);
    }

    private boolean isResolutionOption(String selected) {
        return selected != null && selected.startsWith("Resolution:");
    }

    private boolean isFPSOption(String selected) {
        return selected != null && (selected.startsWith("FPS:") || selected.startsWith("帧率:"));
    }

    private boolean isThrottleOption(String selected) {
        return selected != null && (selected.startsWith("Throttle:") || selected.startsWith("油门:"));
    }

    private boolean isDecelerationOption(String selected) {
        return selected != null && (selected.startsWith("Deceleration:") || selected.startsWith("减速度:"));
    }

    private boolean isGravityOption(String selected) {
        return selected != null && (selected.startsWith("Gravity:") || selected.startsWith("重力:"));
    }

    private boolean isUIFontSizeOption(String selected) {
        return selected != null && (selected.startsWith("UI Font:") || selected.startsWith("字体:"));
    }

    private boolean isBackOption(String selected) {
        List<String> options = List.of("Back", "back", "返回");
        return options.contains(selected);
    }

    private boolean isPauseMenu(MenuObject menu) {
        return menu != null && PAUSE_MENU_NAME.equals(menu.getName());
    }

    private boolean isOptionsMenu(MenuObject menu) {
        return menu != null && OPTIONS_MENU_NAME.equals(menu.getName());
    }

    private boolean isLevelSelectMenu(MenuObject menu) {
        return menu != null && LEVEL_SELECT_MENU_NAME.equals(menu.getName());
    }
}

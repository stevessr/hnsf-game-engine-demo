package lib.state;

import java.awt.Color;
import java.awt.event.KeyEvent;
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
import lib.render.SwingGamePanel;

/**
 * 默认游戏状态机实现。
 */
public final class DefaultGameStateMachine implements GameStateMachine {
    private static final String MAIN_MENU_NAME = "main-menu";
    private static final String LEVEL_SELECT_MENU_NAME = "level-select-menu";
    private static final String PAUSE_MENU_NAME = "pause-menu";
    private static final String OPTIONS_MENU_NAME = "options-menu";
    private static final String KEYBINDINGS_MENU_NAME = "keybindings-menu";
    private static final String GAMEOVER_MENU_NAME = "gameover-menu";
    private static final String VICTORY_DIALOG_NAME = "victory-dialog";

    private GameState currentState;
    private GameState previousState;
    private final Map<GameState, Set<GameState>> allowedTransitions;
    private DialogObject hiddenDialog;
    private InputAction keyToRebind = null;

    public DefaultGameStateMachine() {
        this(GameState.MENU);
    }

    public DefaultGameStateMachine(GameState initialState) {
        this.currentState = Objects.requireNonNull(initialState, "initialState must not be null");
        this.previousState = initialState;
        this.allowedTransitions = new EnumMap<>(GameState.class);
        initializeTransitions();
    }

    private void initializeTransitions() {
        allowedTransitions.put(GameState.MENU, EnumSet.of(GameState.DIALOG, GameState.PLAYING));
        allowedTransitions.put(GameState.PLAYING, EnumSet.of(GameState.PAUSED, GameState.DIALOG, GameState.MENU, GameState.GAMEOVER, GameState.SETTLEMENT));
        allowedTransitions.put(GameState.DIALOG, EnumSet.of(GameState.PLAYING, GameState.SETTLEMENT));
        allowedTransitions.put(GameState.PAUSED, EnumSet.of(GameState.PLAYING, GameState.MENU));
        allowedTransitions.put(GameState.GAMEOVER, EnumSet.of(GameState.PLAYING, GameState.MENU));
        allowedTransitions.put(GameState.SETTLEMENT, EnumSet.of(GameState.PLAYING, GameState.MENU));
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

    public void togglePause(GameWorld world, GameSettings settings) {
        if (currentState == GameState.PAUSED) {
            removeMenu(world, PAUSE_MENU_NAME);
            removeMenu(world, OPTIONS_MENU_NAME);
            removeMenu(world, KEYBINDINGS_MENU_NAME);
            restoreHiddenDialog(world);
            transitionTo(previousState == GameState.PAUSED ? GameState.PLAYING : previousState);
            return;
        }
        if (currentState == GameState.PLAYING) {
            hideActiveDialog(world);
            transitionTo(GameState.PAUSED);
            createPauseMenu(world, settings);
            clearPlayerMovement(world);
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
        int menuWidth = 320;
        int fontSize = settings != null ? settings.getUIFontSize() : 18;
        int menuHeight = Math.max(160, 48 + (3 * Math.max(20, fontSize + 8)) + 12);
        
        MenuObject pauseMenu = new MenuObject(PAUSE_MENU_NAME, 0, 0, menuWidth, menuHeight, "Paused", List.of("Resume", "Options", "Exit to Menu"));
        pauseMenu.setFontSize(fontSize);
        pauseMenu.setSize(menuWidth, Math.max(menuHeight, pauseMenu.getPreferredHeight()));
        world.addObject(pauseMenu);
        recenterUI(world);
    }

    private void createGameOverMenu(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeMenu(world, GAMEOVER_MENU_NAME);
        int menuWidth = 320;
        int fontSize = settings != null ? settings.getUIFontSize() : 24;
        int menuHeight = 200;
        
        MenuObject gameOverMenu = new MenuObject(GAMEOVER_MENU_NAME, 0, 0, menuWidth, menuHeight, "GAME OVER", List.of("Restart Level", "Back to Menu"));
        gameOverMenu.setColor(new Color(80, 0, 0, 200));
        gameOverMenu.setFontSize(fontSize);
        gameOverMenu.setSize(menuWidth, Math.max(menuHeight, gameOverMenu.getPreferredHeight()));
        world.addObject(gameOverMenu);
        recenterUI(world);
    }

    private void createVictoryDialog(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeDialog(world, VICTORY_DIALOG_NAME);
        DialogObject victoryDialog = new DialogObject(VICTORY_DIALOG_NAME, 0, 0, (int) (world.getWidth() * 0.8), 100, "SYSTEM", "VICTORY! All enemies defeated. Press Confirm to continue.");
        victoryDialog.setColor(new Color(0, 80, 0, 220));
        victoryDialog.setFontSize(settings != null ? settings.getUIFontSize() : 20);
        world.addObject(victoryDialog);
        recenterUI(world);
    }

    public void recenterUI(GameWorld world) {
        if (world == null) {
            return;
        }
        int worldWidth = world.getWidth();
        int worldHeight = world.getHeight();

        for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
            if (object instanceof MenuObject menu && menu.isActive()) {
                menu.setPosition(Math.max(0, (worldWidth - menu.getWidth()) / 2), Math.max(0, (worldHeight - menu.getHeight()) / 2));
            }
        }

        for (GameObject object : world.getObjectsByType(GameObjectType.DIALOG)) {
            if (object instanceof DialogObject dialog && dialog.isActive()) {
                int dialogWidth = (int) (worldWidth * 0.8);
                int dialogHeight = Math.max(60, dialog.getFontSize() * 3 + 20);
                dialog.setSize(dialogWidth, dialogHeight);
                dialog.setPosition((worldWidth - dialogWidth) / 2, worldHeight - dialogHeight - 40);
            }
        }
    }

    private void removeMenu(GameWorld world, String name) {
        if (world == null) {
            return;
        }
        world.getObjectsByType(GameObjectType.MENU).stream().filter(obj -> name.equals(obj.getName())).forEach(world::removeObject);
    }

    private void removeDialog(GameWorld world, String name) {
        if (world == null) {
            return;
        }
        world.getObjectsByType(GameObjectType.DIALOG).stream().filter(obj -> name.equals(obj.getName())).forEach(world::removeObject);
    }

    @Override
    public void processInput(GameStateContext context) {
        Objects.requireNonNull(context, "context must not be null");
        
        if (keyToRebind != null) {
            handleRebindInput(context);
            return;
        }

        switch (currentState) {
            case MENU -> processMenuInput(context);
            case PLAYING -> processPlayingInput(context);
            case DIALOG -> processDialogInput(context);
            case PAUSED -> processPausedInput(context);
            case GAMEOVER -> processGameOverInput(context);
            case SETTLEMENT -> processSettlementInput(context);
        }
    }

    private void handleRebindInput(GameStateContext context) {
        var keyboard = context.getInputController().getKeyboardManager();
        int lastKey = -1;
        // Check all common keys
        for (int i = 0; i < 512; i++) {
            if (keyboard.isPressed(i)) {
                lastKey = i;
                break;
            }
        }
        
        if (lastKey != -1 && lastKey != KeyEvent.VK_P && lastKey != KeyEvent.VK_ESCAPE) {
            var mapper = context.getInputController().getActionMapper();
            mapper.clearBindings(keyToRebind);
            mapper.bindKey(keyToRebind, lastKey);
            
            if (context.getSettings() instanceof SwingGamePanel panel) {
                panel.syncInputMap();
            }
            
            syncActiveDialog(context.getWorld(), "Rebound!", keyToRebind.name() + " to " + KeyEvent.getKeyText(lastKey));
            keyToRebind = null;
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
            if (isKeyBindingsMenu(menu)) {
                world.removeObject(menu);
                showOptionsMenu(world, context.getSettings());
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
            int hoveredIndex = findHoveredOptionIndex(menu, inputController.getMouseManager().getMouseX(), inputController.getMouseManager().getMouseY());
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
        if (isKeyBindingsMenu(menu)) {
            handleKeyBindingsSelection(menu, context);
            return;
        }
        if (isLevelSelectMenu(menu)) {
            handleLevelSelectSelection(menu, context);
            return;
        }
        if (isGameOverMenu(menu)) {
            handleGameOverMenuSelection(menu, context);
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

    private void handleKeyBindingsSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if (selected.equals("Back")) {
            context.getWorld().removeObject(menu);
            showOptionsMenu(context.getWorld(), context.getSettings());
            return;
        }
        if (selected.startsWith("Rebind ")) {
            String actionName = selected.substring(7);
            try {
                keyToRebind = InputAction.valueOf(actionName);
                syncActiveDialog(context.getWorld(), "Press any key for", actionName);
            } catch (Exception e) {
                // Ignore
            }
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

    private void handleGameOverMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if (selected.contains("Restart")) {
            removeMenu(context.getWorld(), GAMEOVER_MENU_NAME);
            context.getRuntimeActions().requestLoadLevel(null);
            transitionTo(GameState.PLAYING);
        } else {
            removeMenu(context.getWorld(), GAMEOVER_MENU_NAME);
            activateMainMenu(context.getWorld());
            transitionTo(GameState.MENU);
        }
    }

    private void showOptionsMenu(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeMenu(world, OPTIONS_MENU_NAME);
        world.getObjectsByType(GameObjectType.MENU).stream().filter(GameObject::isActive).forEach(obj -> obj.setActive(false));

        int menuWidth = 320;
        int fontSize = settings != null ? settings.getUIFontSize() : 18;
        int menuHeight = Math.max(220, 56 + (7 * Math.max(20, fontSize + 8)) + 12);

        int throttle = settings != null ? settings.getThrottlePower() : 600;
        int deceleration = settings != null ? settings.getDeceleration() : 92;
        boolean gravityEnabled = settings != null && settings.isGravityEnabled();
        boolean lightingEnabled = settings != null && settings.isLightingEnabled();

        MenuObject optionsMenu = new MenuObject(OPTIONS_MENU_NAME, 0, 0, menuWidth, menuHeight, "Options", 
            List.of("Resolution: " + world.getWidth() + "x" + world.getHeight(), "FPS: " + (settings != null ? settings.getTargetFPS() : 60), 
                    "Throttle: " + throttle, "Deceleration: " + deceleration + "%", "Gravity: " + (gravityEnabled ? "On" : "Off"), 
                    "Lighting: " + (lightingEnabled ? "On" : "Off"), "UI Font: " + fontSize, "Key Bindings", "Back"));
        optionsMenu.setFontSize(fontSize);
        optionsMenu.setSize(menuWidth, Math.max(menuHeight, optionsMenu.getPreferredHeight()));
        world.addObject(optionsMenu);
        recenterUI(world);
    }

    private void showKeyBindingsMenu(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeMenu(world, KEYBINDINGS_MENU_NAME);
        List<String> options = new ArrayList<>();
        for (InputAction action : InputAction.values()) {
            options.add("Rebind " + action.name());
        }
        options.add("Back");
        MenuObject menu = new MenuObject(KEYBINDINGS_MENU_NAME, 0, 0, 400, 400, "Rebind Keys", options);
        menu.setFontSize(settings != null ? settings.getUIFontSize() : 18);
        menu.setSize(400, Math.max(400, menu.getPreferredHeight()));
        world.addObject(menu);
        recenterUI(world);
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
        } else if (isLightingOption(selected)) {
            cycleLighting(settings, menu);
        } else if (selected.equals("Key Bindings")) {
            showKeyBindingsMenu(context.getWorld(), settings);
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

    private void cycleLighting(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        boolean enabled = !settings.isLightingEnabled();
        settings.setLightingEnabled(enabled);
        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (isLightingOption(options.get(i))) {
                options.set(i, "Lighting: " + (enabled ? "On" : "Off"));
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
            recenterUI(world);
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
        settings.setLogicalResolution(newW, newH);
        settings.setResolution(newW, newH);
        recenterUI(world);
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
        var actionMapper = inputController.getActionMapper();
        GameWorld world = context.getWorld();

        if (actionMapper.isKeyboardJustActivated(InputAction.PAUSE, inputController.getKeyboardManager())) {
            togglePause(world, context.getSettings());
            clearPlayerMovement(context);
            return;
        }

        if (findActiveDialog(world) != null) {
            transitionTo(GameState.DIALOG);
            clearPlayerMovement(context);
            return;
        }

        PlayerObject player = world.findPlayer().orElse(null);
        if (player == null || (!player.isActive() && !player.isDying())) {
            transitionTo(GameState.GAMEOVER);
            createGameOverMenu(world, context.getSettings());
            return;
        }
        if (player.isDying()) {
            return;
        }
        if (checkLevelComplete(world)) {
            transitionTo(GameState.SETTLEMENT);
            createVictoryDialog(world, context.getSettings());
            return;
        }

        double ax = 0;
        double ay = 0;
        var kb = inputController.getKeyboardManager();
        var ms = inputController.getMouseManager();
        if (actionMapper.isActive(InputAction.MOVE_LEFT, kb, ms)) {
            ax -= 1.0;
        }
        if (actionMapper.isActive(InputAction.MOVE_RIGHT, kb, ms)) {
            ax += 1.0;
        }
        if (actionMapper.isActive(InputAction.MOVE_UP, kb, ms)) {
            ay -= 1.0;
        }
        if (actionMapper.isActive(InputAction.MOVE_DOWN, kb, ms)) {
            ay += 1.0;
        }
        if (ax != 0 && ay != 0) {
            double mag = Math.sqrt(ax * ax + ay * ay);
            ax /= mag;
            ay /= mag;
        }
        player.accelerate(ax, ay, 1.0 / 60.0);
    }

    private boolean checkLevelComplete(GameWorld world) {
        return world.getObjectsByType(GameObjectType.MONSTER).stream().noneMatch(GameObject::isActive);
    }

    private void processDialogInput(GameStateContext context) {
        var inputController = context.getInputController();
        var actionMapper = inputController.getActionMapper();
        DialogObject dialog = findActiveDialog(context.getWorld());
        if (dialog == null) {
            transitionTo(GameState.PLAYING);
            return;
        }

        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_CONFIRM, inputController.getKeyboardManager())
            || actionMapper.isKeyboardJustActivated(InputAction.DIALOG_NEXT, inputController.getKeyboardManager())
            || actionMapper.isMouseJustActivated(InputAction.MENU_CONFIRM, inputController.getMouseManager())) {
            dialog.setActive(false);
            if (currentState == GameState.SETTLEMENT) {
                activateMainMenu(context.getWorld());
                transitionTo(GameState.MENU);
            } else {
                transitionTo(GameState.PLAYING);
            }
        }
        clearPlayerMovement(context);
    }

    private void processPausedInput(GameStateContext context) {
        if (context.getInputController().getActionMapper().isKeyboardJustActivated(InputAction.PAUSE, context.getInputController().getKeyboardManager())) {
            togglePause(context.getWorld(), context.getSettings());
            return;
        }
        processMenuInput(context);
        clearPlayerMovement(context);
    }

    private void processGameOverInput(GameStateContext context) {
        processMenuInput(context);
    }

    private void processSettlementInput(GameStateContext context) {
        processDialogInput(context);
    }

    private void clearPlayerMovement(GameWorld world) {
        if (world == null) {
            return;
        }
        world.findPlayer().ifPresent(p -> p.setVelocity(0.0, 0.0));
    }

    private void clearPlayerMovement(GameStateContext context) {
        if (context != null) {
            clearPlayerMovement(context.getWorld());
        }
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
        } else {
            MenuObject levelMenu = findMenuByName(world, LEVEL_SELECT_MENU_NAME);
            if (levelMenu != null) {
                levelMenu.setActive(true);
            }
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
        return mouseX >= menu.getX() && mouseX <= menu.getX() + menu.getWidth() && mouseY >= menu.getY() && mouseY <= menu.getY() + menu.getHeight();
    }

    private boolean isStartOption(String s) {
        return List.of("Start", "start", "开始游戏", "开始").contains(s);
    }

    private boolean isLevelsOption(String s) {
        return List.of("Levels", "Level Select", "level select", "关卡选择", "选择关卡").contains(s);
    }

    private boolean isEditorOption(String s) {
        return List.of("Editor", "Level Editor", "关卡编辑器", "编辑器").contains(s);
    }

    private boolean isExitOption(String s) {
        return List.of("Exit", "exit", "退出", "退出游戏").contains(s);
    }

    private boolean isResumeOption(String s) {
        return List.of("Resume", "resume", "恢复", "继续游戏").contains(s);
    }

    private boolean isExitToMenuOption(String s) {
        return List.of("Exit to Menu", "返回主菜单", "退出到菜单").contains(s);
    }

    private boolean isOptionsOption(String s) {
        return List.of("Options", "options", "选项", "设置").contains(s);
    }

    private boolean isResolutionOption(String s) {
        return s != null && s.startsWith("Resolution:");
    }

    private boolean isFPSOption(String s) {
        return s != null && (s.startsWith("FPS:") || s.startsWith("帧率:"));
    }

    private boolean isThrottleOption(String s) {
        return s != null && (s.startsWith("Throttle:") || s.startsWith("油门:"));
    }

    private boolean isDecelerationOption(String s) {
        return s != null && (s.startsWith("Deceleration:") || s.startsWith("减速度:"));
    }

    private boolean isGravityOption(String s) {
        return s != null && (s.startsWith("Gravity:") || s.startsWith("重力:"));
    }

    private boolean isLightingOption(String s) {
        return s != null && (s.startsWith("Lighting:") || s.startsWith("光照:"));
    }

    private boolean isUIFontSizeOption(String s) {
        return s != null && (s.startsWith("UI Font:") || s.startsWith("字体:"));
    }

    private boolean isBackOption(String s) {
        return List.of("Back", "back", "返回").contains(s);
    }

    private boolean isPauseMenu(MenuObject m) {
        return m != null && PAUSE_MENU_NAME.equals(m.getName());
    }

    private boolean isOptionsMenu(MenuObject m) {
        return m != null && OPTIONS_MENU_NAME.equals(m.getName());
    }

    private boolean isKeyBindingsMenu(MenuObject m) {
        return m != null && KEYBINDINGS_MENU_NAME.equals(m.getName());
    }

    private boolean isLevelSelectMenu(MenuObject m) {
        return m != null && LEVEL_SELECT_MENU_NAME.equals(m.getName());
    }

    private boolean isGameOverMenu(MenuObject m) {
        return m != null && GAMEOVER_MENU_NAME.equals(m.getName());
    }
}

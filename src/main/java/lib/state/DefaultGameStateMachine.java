package lib.state;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lib.game.GameWorld;
import lib.game.WinConditionType;
import lib.input.InputAction;
import lib.input.KeyBindingsWindow;
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
    private static final String GAMEOVER_MENU_NAME = "gameover-menu";
    private static final String VICTORY_MENU_NAME = "victory-menu";
    private static final String PROCEDURAL_FOREST_TEMPLATE = "procedural-forest";
    private static final String PROCEDURAL_CAVE_TEMPLATE = "procedural-cave";

    private GameState currentState;
    private GameState previousState;
    private final Map<GameState, Set<GameState>> allowedTransitions;
    private final Set<String> dismissedDialogNames;
    private DialogObject hiddenDialog;

    public DefaultGameStateMachine() {
        this(GameState.MENU);
    }

    public DefaultGameStateMachine(GameState initialState) {
        this.currentState = Objects.requireNonNull(initialState, "initialState must not be null");
        this.previousState = initialState;
        this.allowedTransitions = new EnumMap<>(GameState.class);
        this.dismissedDialogNames = new HashSet<>();
        initializeTransitions();
    }

    public void init(GameWorld world) {
        dismissedDialogNames.clear();
        hiddenDialog = null;
        recenterUI(world);
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
            playMenuBackSound(world);
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
        recenterUI(world);
    }

    private void createPauseMenu(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeMenu(world, PAUSE_MENU_NAME);
        int menuWidth = 320;
        int fontSize = settings != null ? settings.getUIFontSize() : 18;
        int menuHeight = Math.max(180, 48 + (4 * Math.max(20, fontSize + 8)) + 12);
        
        MenuObject pauseMenu = new MenuObject(PAUSE_MENU_NAME, 0, 0, menuWidth, menuHeight, "Paused", List.of("Resume", "Restart Level", "Options", "Exit to Menu"));
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
        int menuHeight = 220;
        
        MenuObject gameOverMenu = new MenuObject(GAMEOVER_MENU_NAME, 0, 0, menuWidth, menuHeight, "GAME OVER", List.of("Respawn", "Back to Menu"));
        gameOverMenu.setSubtitle(resolveFailureReason(world));
        gameOverMenu.setColor(new Color(80, 0, 0, 200));
        gameOverMenu.setFontSize(fontSize);
        gameOverMenu.setSize(menuWidth, Math.max(menuHeight, gameOverMenu.getPreferredHeight()));
        world.addObject(gameOverMenu);
        recenterUI(world);
    }

    private void createVictoryMenu(GameWorld world, GameSettings settings, boolean hasNext) {
        if (world == null) {
            return;
        }
        removeMenu(world, VICTORY_MENU_NAME);
        int menuWidth = 320;
        int fontSize = settings != null ? settings.getUIFontSize() : 20;
        
        List<String> options = new ArrayList<>();
        if (hasNext) {
            options.add("Next Level");
        }
        options.add("Back to Menu");
        
        MenuObject victoryMenu = new MenuObject(VICTORY_MENU_NAME, 0, 0, menuWidth, 200, "VICTORY!", options);
        victoryMenu.setColor(new Color(0, 80, 0, 200));
        victoryMenu.setFontSize(fontSize);
        victoryMenu.setSize(menuWidth, Math.max(200, victoryMenu.getPreferredHeight()));
        world.addObject(victoryMenu);
        recenterUI(world);
    }

    public void recenterUI(GameWorld world) {
        if (world == null) {
            return;
        }
        int viewW = resolveViewportWidth(world);
        int viewH = resolveViewportHeight(world);

        for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
            if (object instanceof MenuObject menu && menu.isActive()) {
                menu.setPosition(Math.max(0, (viewW - menu.getWidth()) / 2), Math.max(0, (viewH - menu.getHeight()) / 2));
            }
        }

        for (GameObject object : world.getObjectsByType(GameObjectType.DIALOG)) {
            if (object instanceof DialogObject dialog && dialog.isActive()) {
                int dialogWidth = (int) (viewW * 0.8);
                int dialogHeight = Math.max(60, dialog.getFontSize() * 3 + 20);
                dialog.setSize(dialogWidth, dialogHeight);
                dialog.setPosition((viewW - dialogWidth) / 2, viewH - dialogHeight - 40);
            }
        }
    }

    private int resolveViewportWidth(GameWorld world) {
        if (world != null && world.getCamera() != null) {
            return world.getCamera().getViewportWidth();
        }
        return 960;
    }

    private int resolveViewportHeight(GameWorld world) {
        if (world != null && world.getCamera() != null) {
            return world.getCamera().getViewportHeight();
        }
        return 540;
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
        
        switch (currentState) {
            case MENU:
                processMenuInput(context);
                break;
            case PLAYING:
                processPlayingInput(context);
                break;
            case DIALOG:
                processDialogInput(context);
                break;
            case PAUSED:
                processPausedInput(context);
                break;
            case GAMEOVER:
                processGameOverInput(context);
                break;
            case SETTLEMENT:
                processSettlementInput(context);
                break;
            default:
                break;
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

        int hoveredIndex = findHoveredOptionIndex(
            menu,
            inputController.getMouseManager().getMouseX(),
            inputController.getMouseManager().getMouseY()
        );
        menu.setHoveredIndex(hoveredIndex);

        if (actionMapper.isKeyboardJustActivated(InputAction.PAUSE, keyboard)) {
            if (isOptionsMenu(menu)) {
                playMenuBackSound(world);
                world.removeObject(menu);
                restoreMenuAfterOptions(world);
                clearPlayerMovement(context);
                return;
            }
            if (isLevelSelectMenu(menu)) {
                playMenuBackSound(world);
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
            int clickedHoverIndex = findHoveredOptionIndex(
                menu,
                inputController.getMouseManager().getMouseX(),
                inputController.getMouseManager().getMouseY()
            );
            if (clickedHoverIndex >= 0) {
                menu.setSelectedIndex(clickedHoverIndex);
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
        if (isGameOverMenu(menu)) {
            handleGameOverMenuSelection(menu, context);
            return;
        }
        if (isVictoryMenu(menu)) {
            handleVictoryMenuSelection(menu, context);
            return;
        }

        if (isStartOption(selected)) {
            playMenuClickSound(world);
            handleMainMenuSelectionForGameplay(menu, context);
        } else if (isLevelsOption(selected)) {
            playMenuClickSound(world);
            activateLevelSelectMenu(world);
            clearPlayerMovement(context);
        } else if (isGenerateForestOption(selected)) {
            playMenuClickSound(world);
            if (context.getRuntimeActions().requestGenerateProceduralLevel(PROCEDURAL_FOREST_TEMPLATE)) {
                menu.setActive(false);
                transitionTo(GameState.PLAYING);
                clearPlayerMovement(context);
            }
        } else if (isGenerateCaveOption(selected)) {
            playMenuClickSound(world);
            if (context.getRuntimeActions().requestGenerateProceduralLevel(PROCEDURAL_CAVE_TEMPLATE)) {
                menu.setActive(false);
                transitionTo(GameState.PLAYING);
                clearPlayerMovement(context);
            }
        } else if (isEditorOption(selected)) {
            playMenuClickSound(world);
            context.getRuntimeActions().requestOpenEditor();
            clearPlayerMovement(context);
        } else if (isOptionsOption(selected)) {
            playMenuClickSound(world);
            showOptionsMenu(world, context.getSettings());
            clearPlayerMovement(context);
        } else if (isExitOption(selected)) {
            playMenuClickSound(world);
            context.getRuntimeActions().requestExit();
            clearPlayerMovement(context);
        } else if (isResumeOption(selected)) {
            playMenuBackSound(world);
            transitionTo(previousState);
            clearPlayerMovement(context);
        }
    }

    private void handleLevelSelectSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        GameWorld world = context.getWorld();
        if (isBackOption(selected)) {
            playMenuBackSound(world);
            activateMainMenu(world);
            transitionTo(GameState.MENU);
            clearPlayerMovement(context);
            return;
        }
        playMenuClickSound(world);
        menu.setActive(false);
        context.getRuntimeActions().requestLoadLevel(selected);
        DialogObject dialog = findActiveDialog(world);
        transitionTo(dialog == null ? GameState.PLAYING : GameState.DIALOG);
        clearPlayerMovement(context);
    }

    private void handlePauseMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if (isResumeOption(selected)) {
            playMenuBackSound(context.getWorld());
            removeMenu(context.getWorld(), PAUSE_MENU_NAME);
            transitionTo(previousState);
        } else if (isRestartLevelOption(selected)) {
            playMenuClickSound(context.getWorld());
            restartCurrentLevel(context);
        } else if (isOptionsOption(selected)) {
            playMenuClickSound(context.getWorld());
            showOptionsMenu(context.getWorld(), context.getSettings());
        } else if (isExitToMenuOption(selected)) {
            playMenuBackSound(context.getWorld());
            removeMenu(context.getWorld(), PAUSE_MENU_NAME);
            removeMenu(context.getWorld(), OPTIONS_MENU_NAME);
            activateMainMenu(context.getWorld());
            transitionTo(GameState.MENU);
        }
        clearPlayerMovement(context);
    }

    private void handleGameOverMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if (isRespawnOption(selected) || isRestartLevelOption(selected)) {
            playMenuClickSound(context.getWorld());
            removeMenu(context.getWorld(), GAMEOVER_MENU_NAME);
            respawnAfterDeath(context);
        } else {
            playMenuBackSound(context.getWorld());
            removeMenu(context.getWorld(), GAMEOVER_MENU_NAME);
            activateMainMenu(context.getWorld());
            transitionTo(GameState.MENU);
        }
    }

    private void handleVictoryMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        if (selected.equals("Next Level")) {
            playMenuClickSound(context.getWorld());
            removeMenu(context.getWorld(), VICTORY_MENU_NAME);
            context.getRuntimeActions().requestLoadNextLevel();
            transitionTo(GameState.PLAYING);
        } else {
            playMenuBackSound(context.getWorld());
            removeMenu(context.getWorld(), VICTORY_MENU_NAME);
            activateMainMenu(context.getWorld());
            transitionTo(GameState.MENU);
        }
    }

    private void restartCurrentLevel(GameStateContext context) {
        if (context == null) {
            return;
        }
        GameWorld world = context.getWorld();
        removeMenu(world, PAUSE_MENU_NAME);
        removeMenu(world, OPTIONS_MENU_NAME);
        context.getRuntimeActions().requestLoadLevel(null);
        transitionTo(GameState.PLAYING);
        clearPlayerMovement(context);
    }

    private void respawnAfterDeath(GameStateContext context) {
        if (context == null) {
            return;
        }
        GameWorld world = context.getWorld();
        if (world == null || !world.respawnPlayer()) {
            restartCurrentLevel(context);
            return;
        }
        removeMenu(world, GAMEOVER_MENU_NAME);
        removeMenu(world, OPTIONS_MENU_NAME);
        transitionTo(GameState.PLAYING);
        clearPlayerMovement(context);
    }

    private void showOptionsMenu(GameWorld world, GameSettings settings) {
        if (world == null) {
            return;
        }
        removeMenu(world, OPTIONS_MENU_NAME);
        world.getObjectsByType(GameObjectType.MENU).stream().filter(GameObject::isActive).forEach(obj -> obj.setActive(false));

        int viewWidth = resolveViewportWidth(world);
        int viewHeight = resolveViewportHeight(world);
        int fontSize = settings != null ? settings.getUIFontSize() : 18;
        int lineHeight = Math.max(20, fontSize + 8);
        int throttle = settings != null ? settings.getThrottlePower() : 600;
        int deceleration = settings != null ? settings.getDeceleration() : 92;
        boolean gravityEnabled = settings != null && settings.isGravityEnabled();
        boolean lightingEnabled = settings != null && settings.isLightingEnabled();
        float ambient = settings != null ? settings.getAmbientLight() : 0.0f;
        float intensity = settings != null ? settings.getLightingIntensity() : 1.0f;
        boolean soundEnabled = settings == null || settings.isSoundEnabled();
        float masterVolume = settings != null ? settings.getVolume() : 1.0f;
        float damageVolume = settings != null ? settings.getDamageVolume() : 1.0f;
        float shootVolume = settings != null ? settings.getShootVolume() : 1.0f;
        float menuVolume = settings != null ? settings.getMenuVolume() : 1.0f;
        float effectVolume = settings != null ? settings.getEffectVolume() : 1.0f;

        List<String> options = new ArrayList<>();
        options.add("Sound: " + (soundEnabled ? "On" : "Off"));
        options.add("Master Audio: " + percentText(masterVolume));
        options.add("Damage Audio: " + percentText(damageVolume));
        options.add("Shoot Audio: " + percentText(shootVolume));
        options.add("Menu Audio: " + percentText(menuVolume));
        options.add("Effect Audio: " + percentText(effectVolume));
        options.add("Resolution: " + (settings instanceof SwingGamePanel p ? p.getWidth() : world.getWidth()) + "x" + (settings instanceof SwingGamePanel p ? p.getHeight() : world.getHeight()));
        options.add("FPS: " + (settings != null ? settings.getTargetFPS() : 60));
        options.add("Throttle: " + throttle);
        options.add("Deceleration: " + deceleration + "%");
        options.add("Gravity: " + (gravityEnabled ? "On" : "Off"));
        options.add("Lighting: " + (lightingEnabled ? "On" : "Off"));
        options.add("Ambient: " + (int) (ambient * 100) + "%");
        options.add("Intensity: " + (int) (intensity * 100) + "%");
        options.add("UI Font: " + fontSize);
        options.add("Key Bindings");
        options.add("Back");

        int optionColumns = 1;
        int maxMenuHeight = Math.max(280, viewHeight - 48);
        while (optionColumns < 3) {
            int rows = Math.max(1, (options.size() + optionColumns - 1) / optionColumns);
            int estimatedHeight = Math.max(280, 56 + (rows * lineHeight) + 12);
            if (estimatedHeight <= maxMenuHeight) {
                break;
            }
            optionColumns++;
        }

        int menuWidth = optionColumns > 1
            ? Math.max(520, Math.min(viewWidth - 48, 760))
            : Math.max(340, Math.min(viewWidth - 64, 420));
        MenuObject optionsMenu = new MenuObject(OPTIONS_MENU_NAME, 0, 0, menuWidth, maxMenuHeight, "Options", options);
        optionsMenu.setFontSize(fontSize);
        optionsMenu.setOptionColumns(optionColumns);
        int menuHeight = Math.min(maxMenuHeight, Math.max(280, optionsMenu.getPreferredHeight()));
        optionsMenu.setSize(menuWidth, menuHeight);
        world.addObject(optionsMenu);
        recenterUI(world);
    }

    private void handleOptionsMenuSelection(MenuObject menu, GameStateContext context) {
        String selected = menu.getSelectedOption();
        GameSettings settings = context.getSettings();
        if (isSoundEnabledOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleSoundEnabled(settings, menu);
        } else if (isMasterAudioOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleVolume(settings, menu);
        } else if (isDamageAudioOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleDamageVolume(settings, menu);
        } else if (isShootAudioOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleShootVolume(settings, menu);
        } else if (isMenuAudioOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleMenuVolume(settings, menu);
        } else if (isEffectAudioOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleEffectVolume(settings, menu);
        } else if (isResolutionOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleResolution(context.getWorld(), settings, menu);
        } else if (isFPSOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleFPS(settings, menu);
        } else if (isThrottleOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleThrottle(settings, menu);
        } else if (isDecelerationOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleDeceleration(settings, menu);
        } else if (isGravityOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleGravity(context.getWorld(), settings, menu);
        } else if (isLightingOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleLighting(settings, menu);
        } else if (selected.startsWith("Ambient: ")) {
            playMenuClickSound(context.getWorld());
            cycleAmbientLight(settings, menu);
        } else if (selected.startsWith("Intensity: ")) {
            playMenuClickSound(context.getWorld());
            cycleLightingIntensity(settings, menu);
        } else if (selected.equals("Key Bindings")) {
            playMenuClickSound(context.getWorld());
            if (settings instanceof SwingGamePanel panel) {
                KeyBindingsWindow.open(panel);
            }
        } else if (isUIFontSizeOption(selected)) {
            playMenuClickSound(context.getWorld());
            cycleUIFontSize(context.getWorld(), settings, menu);
        } else if (isBackOption(selected)) {
            playMenuBackSound(context.getWorld());
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

    private void cycleAmbientLight(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        float current = settings.getAmbientLight();
        float next = (current + 0.1f);
        if (next > 1.05f) {
            next = 0.0f;
        }
        settings.setAmbientLight(next);
        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).startsWith("Ambient: ")) {
                options.set(i, "Ambient: " + (int) (next * 100) + "%");
            }
        }
        menu.setOptions(options);
    }

    private void cycleLightingIntensity(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        float current = settings.getLightingIntensity();
        float next = (current + 0.2f);
        if (next > 2.05f) {
            next = 0.2f;
        }
        settings.setLightingIntensity(next);
        List<String> options = new ArrayList<>(menu.getOptions());
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).startsWith("Intensity: ")) {
                options.set(i, "Intensity: " + (int) (next * 100) + "%");
            }
        }
        menu.setOptions(options);
    }

    private void cycleVolume(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        float current = settings.getVolume();
        float next = (current + 0.25f);
        if (next > 1.05f) {
            next = 0.0f;
        }
        settings.setVolume(next);
        updateVolumeOption(menu, "Master Audio: ", next);
        updateVolumeOption(menu, "Audio: ", next);
    }

    private void cycleDamageVolume(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        float next = cycleVolumeValue(settings.getDamageVolume());
        settings.setDamageVolume(next);
        updateVolumeOption(menu, "Damage Audio: ", next);
    }

    private void cycleShootVolume(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        float next = cycleVolumeValue(settings.getShootVolume());
        settings.setShootVolume(next);
        updateVolumeOption(menu, "Shoot Audio: ", next);
    }

    private void cycleMenuVolume(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        float next = cycleVolumeValue(settings.getMenuVolume());
        settings.setMenuVolume(next);
        updateVolumeOption(menu, "Menu Audio: ", next);
    }

    private void cycleEffectVolume(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        float next = cycleVolumeValue(settings.getEffectVolume());
        settings.setEffectVolume(next);
        updateVolumeOption(menu, "Effect Audio: ", next);
    }

    private void cycleSoundEnabled(GameSettings settings, MenuObject menu) {
        if (settings == null) {
            return;
        }
        boolean next = !settings.isSoundEnabled();
        settings.setSoundEnabled(next);
        updateToggleOption(menu, "Sound: ", next ? "On" : "Off");
    }

    private float cycleVolumeValue(float current) {
        float next = current + 0.25f;
        if (next > 1.05f) {
            next = 0.0f;
        }
        return next;
    }

    private void updateVolumeOption(MenuObject menu, String prefix, float value) {
        if (menu == null) {
            return;
        }
        List<String> options = new ArrayList<>(menu.getOptions());
        String replacement = prefix + percentText(value);
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).startsWith(prefix)) {
                options.set(i, replacement);
            }
        }
        menu.setOptions(options);
    }

    private void updateToggleOption(MenuObject menu, String prefix, String suffix) {
        if (menu == null) {
            return;
        }
        List<String> options = new ArrayList<>(menu.getOptions());
        String replacement = prefix + suffix;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).startsWith(prefix)) {
                options.set(i, replacement);
            }
        }
        menu.setOptions(options);
    }

    private String percentText(float value) {
        return (int) (Math.max(0.0f, Math.min(1.0f, value)) * 100) + "%";
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
        int panelW = (settings instanceof SwingGamePanel panel) ? panel.getWidth() : world.getWidth();
        int nextIdx = 0;
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i][0] == panelW) {
                nextIdx = (i + 1) % resolutions.length;
                break;
            }
        }
        int newW = resolutions[nextIdx][0];
        int newH = resolutions[nextIdx][1];
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

        if (actionMapper.isKeyboardJustActivated(InputAction.TOGGLE_GOALS, inputController.getKeyboardManager())) {
            world.toggleShowGoals();
        }

        PlayerObject player = world.findPlayer().orElse(null);

        // Proximity-based dialog activation
        if (player != null) {
            for (GameObject obj : world.getObjectsByType(GameObjectType.DIALOG)) {
                if (obj instanceof DialogObject dialog
                    && !dialog.isActive()
                    && !dismissedDialogNames.contains(dialog.getName())) {
                    // Activate if player is close (within 300 pixels of horizontal center)
                    int dist = Math.abs((player.getX() + player.getWidth() / 2) - (dialog.getX() + dialog.getWidth() / 2));
                    if (dist < 300) {
                        dialog.setActive(true);
                        recenterUI(world);
                        break; // Only one dialog at a time
                    }
                }
            }
        }

        if (findActiveDialog(world) != null) {
            transitionTo(GameState.DIALOG);
            clearPlayerMovement(context);
            return;
        }

        if (player == null || (!player.isActive() && !player.isDying())) {
            transitionTo(GameState.GAMEOVER);
            createGameOverMenu(world, context.getSettings());
            return;
        }
        if (player.isDying()) {
            return;
        }
        
        // Check for Goal contact (only if winCondition is REACH_GOAL)
        if (world.getWinCondition() == WinConditionType.REACH_GOAL) {
            for (GameObject other : world.getCollisions(player)) {
                if (other.getType() == GameObjectType.GOAL && other.isActive()) {
                    transitionTo(GameState.SETTLEMENT);
                    world.getSoundManager().playSound("victory");
                    createVictoryMenu(world, context.getSettings(), context.getRuntimeActions().hasNextLevel());
                    return;
                }
            }
        }

        if (world.isComplete()) {
            transitionTo(GameState.SETTLEMENT);
            world.getSoundManager().playSound("victory");
            createVictoryMenu(world, context.getSettings(), context.getRuntimeActions().hasNextLevel());
            return;
        }

        inputController.applyPlayerMovement(world, context.getSettings(), player, context.getDeltaSeconds());
        inputController.applyVoxelSystem(world);
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
            GameWorld world = context.getWorld();
            if (world != null) {
                world.getSoundManager().playSound("menu_click");
            }
            dismissedDialogNames.add(dialog.getName());
            dialog.setActive(false);
            recenterUI(context.getWorld());
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
        processMenuInput(context);
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

    private void playMenuClickSound(GameWorld world) {
        if (world != null) {
            world.getSoundManager().playSound("menu_click");
        }
    }

    private void playMenuBackSound(GameWorld world) {
        if (world != null) {
            world.getSoundManager().playSound("menu_back");
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
        return menu.findOptionIndexAt(mouseX, mouseY);
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

    private boolean isGenerateForestOption(String s) {
        return List.of("Generate Forest", "生成森林", "生成森林关卡").contains(s);
    }

    private boolean isGenerateCaveOption(String s) {
        return List.of("Generate Cave", "生成洞穴", "生成洞穴关卡").contains(s);
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

    private boolean isRestartLevelOption(String s) {
        return List.of("Restart Level", "Restart", "restart", "重启关卡", "重新开始").contains(s);
    }

    private boolean isRespawnOption(String s) {
        return List.of("Respawn", "respawn", "重生", "复活", "重新出生").contains(s);
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

    private boolean isSoundEnabledOption(String s) {
        return s != null && s.startsWith("Sound:");
    }

    private boolean isMasterAudioOption(String s) {
        return s != null && (s.startsWith("Master Audio:") || s.startsWith("Audio:"));
    }

    private boolean isDamageAudioOption(String s) {
        return s != null && s.startsWith("Damage Audio:");
    }

    private boolean isShootAudioOption(String s) {
        return s != null && s.startsWith("Shoot Audio:");
    }

    private boolean isMenuAudioOption(String s) {
        return s != null && s.startsWith("Menu Audio:");
    }

    private boolean isEffectAudioOption(String s) {
        return s != null && s.startsWith("Effect Audio:");
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

    private boolean isLevelSelectMenu(MenuObject m) {
        return m != null && LEVEL_SELECT_MENU_NAME.equals(m.getName());
    }

    private boolean isGameOverMenu(MenuObject m) {
        return m != null && GAMEOVER_MENU_NAME.equals(m.getName());
    }

    private boolean isVictoryMenu(MenuObject m) {
        return m != null && VICTORY_MENU_NAME.equals(m.getName());
    }

    private String resolveFailureReason(GameWorld world) {
        if (world == null) {
            return "原因：生命值耗尽";
        }
        String failureReason = world.getFailureReason();
        if (failureReason == null || failureReason.isBlank()) {
            return "原因：生命值耗尽";
        }
        return "原因：" + failureReason;
    }
}

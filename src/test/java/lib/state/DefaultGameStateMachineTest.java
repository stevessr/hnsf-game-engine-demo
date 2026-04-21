package lib.state;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.game.GameWorld;
import lib.input.GameInputController;
import lib.object.GameObjectType;
import lib.object.DialogObject;
import lib.object.MenuObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.render.Camera;

class DefaultGameStateMachineTest {
    @Test
    void menuStartShouldGateWorldAndEnterDialogBeforePlaying() {
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setDeceleration(1.0);
        MenuObject menu = new MenuObject("menu", 10, 10, 120, 80, "Main", List.of("Start", "Options"));
        DialogObject dialog = new DialogObject("dialog", 10, 110, 180, 40, "Guide", "Welcome");
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine();
        GameInputController inputController = GameInputController.createDefault();
        GameStateContext context = new GameStateContext(world, inputController);

        world.addObject(player);
        world.addObject(menu);
        world.addObject(dialog);
        world.setStateMachine(stateMachine);

        inputController.getKeyboardManager().pressKey(KeyEvent.VK_D);
        stateMachine.processInput(context);
        world.update(1.0);
        inputController.finishFrame();

        assertEquals(GameState.MENU, stateMachine.getCurrentState());
        assertEquals(10, player.getX());
        assertEquals(0, player.getVelocityX());

        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals(GameState.DIALOG, stateMachine.getCurrentState());
        assertFalse(menu.isActive());
        assertTrue(dialog.isActive());

        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        assertFalse(dialog.isActive());
    }

    @Test
    void menuExitShouldRequestRuntimeExitInsteadOfStayingInMenu() {
        GameWorld world = new GameWorld(240, 180);
        MenuObject menu = new MenuObject("main-menu", 10, 10, 120, 80, "Main", List.of("Start", "Options", "Exit"));
        menu.setSelectedIndex(2);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine();
        GameInputController inputController = GameInputController.createDefault();
        AtomicBoolean exitRequested = new AtomicBoolean(false);
        GameStateContext context = new GameStateContext(world, inputController, () -> exitRequested.set(true));

        world.addObject(menu);
        world.setStateMachine(stateMachine);

        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertTrue(exitRequested.get(), "主菜单 Exit 应触发运行时退出请求");
        assertEquals(GameState.MENU, stateMachine.getCurrentState(), "退出请求不应伪造状态切换");
        assertTrue(menu.isActive(), "退出请求前不应先破坏主菜单对象状态");
    }

    @Test
    void menuGenerateForestShouldRequestProceduralGenerationAndEnterPlaying() {
        GameWorld world = new GameWorld(240, 180);
        MenuObject menu = new MenuObject(
            "main-menu",
            10,
            10,
            220,
            120,
            "Main",
            List.of("Start", "Levels", "Generate Forest", "Generate Cave", "Editor", "Options", "Exit")
        );
        menu.setSelectedIndex(2);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.MENU);
        GameInputController inputController = GameInputController.createDefault();
        AtomicReference<String> requestedTemplate = new AtomicReference<>();

        GameRuntimeActions actions = new GameRuntimeActions() {
            @Override
            public void requestExit() {
            }

            @Override
            public boolean requestGenerateProceduralLevel(String templateName) {
                requestedTemplate.set(templateName);
                return true;
            }
        };
        GameStateContext context = new GameStateContext(world, inputController, actions);

        world.addObject(menu);
        world.setStateMachine(stateMachine);

        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals("procedural-forest", requestedTemplate.get());
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState(), "生成关卡后应切回 PLAYING");
        assertFalse(menu.isActive(), "生成后主菜单应隐藏");
    }

    @Test
    void menuGenerateCaveShouldRequestProceduralGenerationAndEnterPlaying() {
        GameWorld world = new GameWorld(240, 180);
        MenuObject menu = new MenuObject(
            "main-menu",
            10,
            10,
            220,
            120,
            "Main",
            List.of("Start", "Levels", "Generate Forest", "Generate Cave", "Editor", "Options", "Exit")
        );
        menu.setSelectedIndex(3);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.MENU);
        GameInputController inputController = GameInputController.createDefault();
        AtomicReference<String> requestedTemplate = new AtomicReference<>();

        GameRuntimeActions actions = new GameRuntimeActions() {
            @Override
            public void requestExit() {
            }

            @Override
            public boolean requestGenerateProceduralLevel(String templateName) {
                requestedTemplate.set(templateName);
                return true;
            }
        };
        GameStateContext context = new GameStateContext(world, inputController, actions);

        world.addObject(menu);
        world.setStateMachine(stateMachine);

        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals("procedural-cave", requestedTemplate.get());
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState(), "生成关卡后应切回 PLAYING");
        assertFalse(menu.isActive(), "生成后主菜单应隐藏");
    }

    @Test
    void playingPauseShouldStopWorldUpdateUntilResumed() {
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        // Add a monster so we don't trigger automatic victory (SETTLEMENT)
        MonsterObject monster = new MonsterObject("mob", 100, 100, 0);
        
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.PLAYING);
        GameInputController inputController = GameInputController.createDefault();
        GameStateContext context = new GameStateContext(world, inputController);

        world.addObject(player);
        world.addObject(monster);
        world.setStateMachine(stateMachine);

        player.setDeceleration(1.0);
        player.setThrottlePower(1000);

        inputController.getKeyboardManager().pressKey(KeyEvent.VK_D);

        // Apply throttle for multiple frames to build up velocity
        for (int i = 0; i < 5; i++) {
            stateMachine.processInput(context);
            world.update(1.0 / 60.0);
            inputController.finishFrame();
        }

        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        assertTrue(player.getX() > 10, "Player should have moved");

        // Stop applying input
        inputController.getKeyboardManager().releaseKey(KeyEvent.VK_D);
        int xBeforePause = player.getX();

        tapKey(inputController, KeyEvent.VK_P);
        stateMachine.processInput(context); // Toggles pause and clears velocity
        
        assertEquals(GameState.PAUSED, stateMachine.getCurrentState());
        int xAtPause = player.getX();
        
        world.update(1.0); 
        inputController.finishFrame();
        assertEquals(xAtPause, player.getX(), "Player position should not change while paused");

        // Prepare movement input BEFORE unpausing
        inputController.getKeyboardManager().pressKey(KeyEvent.VK_D);
        
        // Unpause
        inputController.getKeyboardManager().releaseKey(KeyEvent.VK_P);
        inputController.getKeyboardManager().pressKey(KeyEvent.VK_P);
        stateMachine.processInput(context);
        inputController.finishFrame();
        
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState(), "Should return to PLAYING state after explicit unpause toggle");
        
        // Move
        for (int i = 0; i < 5; i++) {
            inputController.getKeyboardManager().pressKey(KeyEvent.VK_D);
            stateMachine.processInput(context);
            world.update(1.0); 
            inputController.finishFrame();
        }

        assertTrue(player.getX() > xAtPause, "Player should move to the right after unpause. currentX=" + player.getX() + ", xAtPause=" + xAtPause);
    }

    @Test
    void pauseMenuRestartShouldRequestReloadAndReturnToPlaying() {
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.PLAYING);
        GameInputController inputController = GameInputController.createDefault();
        AtomicReference<String> requestedLevel = new AtomicReference<>("unset");

        GameRuntimeActions actions = new GameRuntimeActions() {
            @Override
            public void requestExit() {
            }

            @Override
            public void requestLoadLevel(String levelName) {
                requestedLevel.set(levelName);
            }
        };
        GameStateContext context = new GameStateContext(world, inputController, actions);

        world.addObject(player);
        world.setStateMachine(stateMachine);

        stateMachine.togglePause(world, null);
        MenuObject pauseMenu = world.getObjectsByType(GameObjectType.MENU).stream()
            .filter(MenuObject.class::isInstance)
            .map(MenuObject.class::cast)
            .filter(menu -> "pause-menu".equals(menu.getName()))
            .findFirst()
            .orElseThrow();

        assertTrue(pauseMenu.getOptions().contains("Restart Level"), "暂停菜单应包含重启关卡选项");

        pauseMenu.setSelectedIndex(1);
        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals(null, requestedLevel.get(), "重启关卡应请求重新加载当前关卡");
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState(), "重启后应切回 PLAYING");
        assertFalse(world.getObjectsByType(GameObjectType.MENU).stream()
            .anyMatch(obj -> "pause-menu".equals(obj.getName())), "重启后暂停菜单应被移除");
    }

    @Test
    void invalidTransitionShouldFailFast() {
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine();

        assertFalse(stateMachine.canTransitionTo(GameState.PAUSED));
        assertThrows(IllegalStateException.class, () -> stateMachine.transitionTo(GameState.PAUSED));
    }

    @Test
    void levelSelectMenuShouldLoadChosenLevelAndReturnToPlaying() {
        GameWorld world = new GameWorld(320, 200);
        MenuObject mainMenu = new MenuObject(
            "main-menu",
            10,
            10,
            160,
            120,
            "Main",
            List.of("Start", "Levels", "Editor", "Exit")
        );
        MenuObject levelMenu = new MenuObject(
            "level-select-menu",
            10,
            10,
            180,
            120,
            "Select Level",
            List.of("level-1", "level-2", "Back")
        );
        levelMenu.setActive(false);

        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine();
        GameInputController inputController = GameInputController.createDefault();
        AtomicReference<String> requestedLevel = new AtomicReference<>();

        GameRuntimeActions actions = new GameRuntimeActions() {
            @Override
            public void requestExit() {
            }

            @Override
            public void requestLoadLevel(String levelName) {
                requestedLevel.set(levelName);
            }
        };
        GameStateContext selectionContext = new GameStateContext(world, inputController, actions);

        world.addObject(mainMenu);
        world.addObject(levelMenu);
        world.setStateMachine(stateMachine);

        mainMenu.setSelectedIndex(1);
        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(selectionContext);
        inputController.finishFrame();

        assertFalse(mainMenu.isActive(), "主菜单应在进入关卡选择后隐藏");
        assertTrue(levelMenu.isActive(), "关卡选择菜单应在进入后显示");
        assertEquals(GameState.MENU, stateMachine.getCurrentState());

        levelMenu.setSelectedIndex(0);
        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(selectionContext);
        inputController.finishFrame();

        assertEquals("level-1", requestedLevel.get());
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        assertFalse(levelMenu.isActive(), "选中关卡后关卡选择菜单应隐藏");
    }

    @Test
    void settlementNextLevelShouldRequestRuntimeNextLevel() {
        GameWorld world = new GameWorld(320, 200);
        MenuObject victoryMenu = new MenuObject(
            "victory-menu",
            10,
            10,
            200,
            120,
            "Victory",
            List.of("Next Level", "Back to Menu")
        );
        victoryMenu.setSelectedIndex(0);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.SETTLEMENT);
        GameInputController inputController = GameInputController.createDefault();
        AtomicBoolean nextRequested = new AtomicBoolean(false);
        GameRuntimeActions actions = new GameRuntimeActions() {
            @Override
            public void requestExit() {
            }

            @Override
            public void requestLoadNextLevel() {
                nextRequested.set(true);
            }

            @Override
            public boolean hasNextLevel() {
                return true;
            }
        };
        GameStateContext context = new GameStateContext(world, inputController, actions);

        world.addObject(victoryMenu);
        world.setStateMachine(stateMachine);

        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertTrue(nextRequested.get(), "结算菜单选择 Next Level 时应请求下一关");
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState(), "进入下一关后应切回 PLAYING");
    }

    @Test
    void dismissedDialogShouldNotImmediatelyReopenWhilePlayerStaysNearby() {
        GameWorld world = new GameWorld(960, 540);
        PlayerObject player = new PlayerObject("hero", 100, 320);
        DialogObject dialog = new DialogObject("tutorial", 100, 100, 400, 60, "Guide", "Welcome");
        dialog.setActive(false);

        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.PLAYING);
        GameInputController inputController = GameInputController.createDefault();
        GameStateContext context = new GameStateContext(world, inputController);

        world.addObject(player);
        world.addObject(dialog);
        world.setStateMachine(stateMachine);
        stateMachine.init(world);

        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals(GameState.DIALOG, stateMachine.getCurrentState());
        assertTrue(dialog.isActive(), "玩家靠近后应触发提示对话");

        tapKey(inputController, KeyEvent.VK_ENTER);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        assertFalse(dialog.isActive(), "确认后对话应关闭");

        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals(GameState.PLAYING, stateMachine.getCurrentState(), "关闭后的对话不应在同一区域立刻再次弹出");
        assertFalse(dialog.isActive(), "关闭后的对话应保持关闭");
    }

    @Test
    void recenterUiShouldIgnoreCameraOffsetForSettlementMenu() {
        GameWorld world = new GameWorld(960 * 3, 540);
        PlayerObject player = new PlayerObject("hero", 2700, 320);
        MenuObject victoryMenu = new MenuObject("victory-menu", 0, 0, 320, 200, "Victory", List.of("Next Level"));
        Camera camera = new Camera(960, 540);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.SETTLEMENT);

        world.addObject(player);
        world.addObject(victoryMenu);
        world.setCamera(camera);
        camera.update(world, player);

        stateMachine.recenterUI(world);

        assertEquals((960 - victoryMenu.getWidth()) / 2, victoryMenu.getX(), "结算菜单应固定居中在视口内，而不是跟随世界坐标漂移");
        assertEquals((540 - victoryMenu.getHeight()) / 2, victoryMenu.getY());
    }

    @Test
    void recenterUiShouldCenterMainMenuWithoutCameraUsingViewportSize() {
        GameWorld world = new GameWorld(4000, 800);
        MenuObject mainMenu = new MenuObject("main-menu", 24, 24, 260, 180, "Demo Menu", List.of("Start", "Exit"));
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.MENU);

        world.addObject(mainMenu);
        world.setStateMachine(stateMachine);

        stateMachine.recenterUI(world);

        assertEquals((960 - mainMenu.getWidth()) / 2, mainMenu.getX(), "主菜单应按视口居中，而不是按地图尺寸居中");
        assertEquals((540 - mainMenu.getHeight()) / 2, mainMenu.getY());
    }

    @Test
    void gameOverMenuShouldDisplayFailureReason() {
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        MonsterObject monster = new MonsterObject("slime", 10, 20, 0);
        monster.setAttack(player.getHealth());
        monster.setSpeed(0);

        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.PLAYING);
        GameInputController inputController = GameInputController.createDefault();
        GameStateContext context = new GameStateContext(world, inputController);

        world.addObject(player);
        world.addObject(monster);
        world.setStateMachine(stateMachine);

        for (int i = 0; i < 120 && stateMachine.getCurrentState() != GameState.GAMEOVER; i++) {
            stateMachine.processInput(context);
            world.update(1.0 / 60.0);
            inputController.finishFrame();
        }

        assertEquals(GameState.GAMEOVER, stateMachine.getCurrentState(), "玩家死亡后应进入 GAMEOVER");
        MenuObject gameOverMenu = world.getObjectsByType(lib.object.GameObjectType.MENU).stream()
            .filter(MenuObject.class::isInstance)
            .map(MenuObject.class::cast)
            .filter(menu -> "gameover-menu".equals(menu.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(gameOverMenu, "应创建游戏结束菜单");
        assertTrue(gameOverMenu.getSubtitle().contains("slime"), "游戏结束菜单应显示死亡原因");
    }

    private static void tapKey(GameInputController inputController, int keyCode) {
        inputController.getKeyboardManager().releaseKey(keyCode);
        inputController.getKeyboardManager().pressKey(keyCode);
    }
}

package lib.state;

import java.awt.event.KeyEvent;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.game.GameWorld;
import lib.input.GameInputController;
import lib.object.DialogObject;
import lib.object.MenuObject;
import lib.object.PlayerObject;

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
    void playingPauseShouldStopWorldUpdateUntilResumed() {
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine(GameState.PLAYING);
        GameInputController inputController = GameInputController.createDefault();
        GameStateContext context = new GameStateContext(world, inputController);

        world.addObject(player);
        world.setStateMachine(stateMachine);

        inputController.getKeyboardManager().pressKey(KeyEvent.VK_D);
        
        // Apply throttle for multiple frames to build up velocity
        for (int i = 0; i < 5; i++) {
            stateMachine.processInput(context);
            world.update(1.0 / 60.0);
            inputController.finishFrame();
        }

        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        assertTrue(player.getX() > 10, "Player should have moved");
        int xAfterMove = player.getX();

        tapKey(inputController, KeyEvent.VK_P);
        stateMachine.processInput(context);
        world.update(1.0);
        inputController.finishFrame();

        assertEquals(GameState.PAUSED, stateMachine.getCurrentState());
        assertTrue(player.getX() >= xAfterMove, "Player position should not decrease");

        tapKey(inputController, KeyEvent.VK_P);
        stateMachine.processInput(context);
        inputController.finishFrame();

        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
    }

    @Test
    void invalidTransitionShouldFailFast() {
        DefaultGameStateMachine stateMachine = new DefaultGameStateMachine();

        assertFalse(stateMachine.canTransitionTo(GameState.PAUSED));
        assertThrows(IllegalStateException.class, () -> stateMachine.transitionTo(GameState.PAUSED));
    }

    private static void tapKey(GameInputController inputController, int keyCode) {
        inputController.getKeyboardManager().releaseKey(keyCode);
        inputController.getKeyboardManager().pressKey(keyCode);
    }
}

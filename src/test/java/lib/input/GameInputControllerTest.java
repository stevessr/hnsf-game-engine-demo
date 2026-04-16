package lib.input;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.game.GameWorld;
import lib.object.DialogObject;
import lib.object.MenuObject;
import lib.object.PlayerObject;

class GameInputControllerTest {
    @Test
    void keyboardActionsShouldDrivePlayerMovement() {
        GameInputController inputController = GameInputController.createDefault();
        GameWorld world = new GameWorld(200, 160);
        PlayerObject player = new PlayerObject("hero", 10, 20);
        player.setDeceleration(1.0);
        world.addObject(player);

        inputController.getKeyboardManager().pressKey(KeyEvent.VK_D);
        inputController.getKeyboardManager().pressKey(KeyEvent.VK_W);

        inputController.applyInputs(world);
        world.update(1.0);
        inputController.finishFrame();

        assertTrue(player.getVelocityX() > 0, "Player should have positive X velocity");
        assertTrue(player.getVelocityY() < 0, "Player should have negative Y velocity");
        assertTrue(player.getX() > 10, "Player should have moved right");
        assertTrue(player.getY() < 20, "Player should have moved up");
    }

    @Test
    void menuActionsShouldSupportKeyboardAndMouseSelection() {
        GameInputController inputController = GameInputController.createDefault();
        GameWorld world = new GameWorld(240, 180);
        MenuObject menu = new MenuObject("menu", 10, 10, 120, 120, "Main", List.of("Start", "Options", "Exit"));
        DialogObject dialog = new DialogObject("dialog", 10, 110, 180, 40, "Guide", "...");
        world.addObject(menu);
        world.addObject(dialog);

        inputController.getKeyboardManager().pressKey(KeyEvent.VK_E);
        inputController.applyInputs(world);
        inputController.finishFrame();

        assertEquals(1, menu.getSelectedIndex());
        assertTrue(dialog.getMessage().contains("Options"));

        inputController.getMouseManager().moveTo(30, 104);
        inputController.getMouseManager().pressButton(MouseEvent.BUTTON1, 30, 104);
        inputController.applyInputs(world);
        inputController.finishFrame();

        assertEquals(2, menu.getSelectedIndex());
        assertTrue(dialog.getMessage().contains("已确认"));
        assertTrue(dialog.getMessage().contains("Exit"));
    }
}

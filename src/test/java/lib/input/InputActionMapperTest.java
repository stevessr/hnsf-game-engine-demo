package lib.input;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputActionMapperTest {
    @Test
    void defaultMappingShouldRecognizeKeyboardAndMouseActions() {
        KeyboardManager keyboardManager = new KeyboardManager();
        MouseManager mouseManager = new MouseManager();
        InputActionMapper mapper = InputActionMapper.createDefaultGameMapping();

        keyboardManager.pressKey(KeyEvent.VK_A);
        keyboardManager.pressKey(KeyEvent.VK_SHIFT);
        mouseManager.pressButton(MouseEvent.BUTTON1, 32, 48);
        mouseManager.pressButton(MouseEvent.BUTTON2, 32, 48);

        assertTrue(mapper.isKeyboardActive(InputAction.MOVE_LEFT, keyboardManager));
        assertTrue(mapper.isKeyboardActive(InputAction.SPRINT, keyboardManager));
        assertTrue(mapper.isActive(InputAction.MOVE_LEFT, keyboardManager, mouseManager));
        assertTrue(mapper.isMouseJustActivated(InputAction.MENU_CONFIRM, mouseManager));
        assertTrue(mapper.isMouseJustActivated(InputAction.SHOOT, mouseManager));
        assertTrue(mapper.isMouseJustActivated(InputAction.VOXEL_BUILD, mouseManager));
        assertTrue(mapper.isJustActivated(InputAction.MENU_CONFIRM, keyboardManager, mouseManager));

        keyboardManager.clearTransientStates();
        mouseManager.clearTransientStates();

        assertFalse(mapper.isKeyboardJustActivated(InputAction.MOVE_LEFT, keyboardManager));
        assertFalse(mapper.isMouseJustActivated(InputAction.MENU_CONFIRM, mouseManager));
    }

    @Test
    void clearingKeyBindingsShouldKeepMouseBindingsIntact() {
        InputActionMapper mapper = InputActionMapper.createDefaultGameMapping();
        MouseManager mouseManager = new MouseManager();

        mapper.clearKeyBindings(InputAction.SHOOT);
        mouseManager.pressButton(MouseEvent.BUTTON1, 10, 10);

        assertTrue(mapper.isMouseJustActivated(InputAction.SHOOT, mouseManager));
    }
}

package lib.input;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InputActionMapper {
    private final Map<InputAction, Set<Integer>> keyBindings;
    private final Map<InputAction, Set<Integer>> mouseBindings;

    public InputActionMapper() {
        this.keyBindings = new EnumMap<>(InputAction.class);
        this.mouseBindings = new EnumMap<>(InputAction.class);
        for (InputAction action : InputAction.values()) {
            keyBindings.put(action, new HashSet<>());
            mouseBindings.put(action, new HashSet<>());
        }
    }

    public static InputActionMapper createDefaultGameMapping() {
        InputActionMapper mapper = new InputActionMapper();
        mapper.bindKey(InputAction.MOVE_UP, KeyEvent.VK_W);
        mapper.bindKey(InputAction.MOVE_UP, KeyEvent.VK_UP);
        mapper.bindKey(InputAction.MOVE_UP, KeyEvent.VK_I);
        mapper.bindKey(InputAction.MOVE_DOWN, KeyEvent.VK_S);
        mapper.bindKey(InputAction.MOVE_DOWN, KeyEvent.VK_DOWN);
        mapper.bindKey(InputAction.MOVE_DOWN, KeyEvent.VK_K);
        mapper.bindKey(InputAction.MOVE_LEFT, KeyEvent.VK_A);
        mapper.bindKey(InputAction.MOVE_LEFT, KeyEvent.VK_LEFT);
        mapper.bindKey(InputAction.MOVE_LEFT, KeyEvent.VK_J);
        mapper.bindKey(InputAction.MOVE_RIGHT, KeyEvent.VK_D);
        mapper.bindKey(InputAction.MOVE_RIGHT, KeyEvent.VK_RIGHT);
        mapper.bindKey(InputAction.MOVE_RIGHT, KeyEvent.VK_L);
        mapper.bindKey(InputAction.MENU_PREVIOUS, KeyEvent.VK_Q);
        mapper.bindKey(InputAction.MENU_NEXT, KeyEvent.VK_E);
        mapper.bindKey(InputAction.MENU_CONFIRM, KeyEvent.VK_ENTER);
        mapper.bindKey(InputAction.MENU_CONFIRM, KeyEvent.VK_SPACE);
        mapper.bindMouseButton(InputAction.MENU_CONFIRM, MouseEvent.BUTTON1);
        return mapper;
    }

    public void bindKey(InputAction action, int keyCode) {
        keyBindings.get(action).add(keyCode);
    }

    public void bindMouseButton(InputAction action, int mouseButton) {
        mouseBindings.get(action).add(mouseButton);
    }

    public boolean isKeyboardActive(InputAction action, KeyboardManager keyboardManager) {
        for (int keyCode : keyBindings.get(action)) {
            if (keyboardManager.isPressed(keyCode)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMouseActive(InputAction action, MouseManager mouseManager) {
        for (int button : mouseBindings.get(action)) {
            if (mouseManager.isPressed(button)) {
                return true;
            }
        }
        return false;
    }

    public boolean isActive(InputAction action, KeyboardManager keyboardManager, MouseManager mouseManager) {
        return isKeyboardActive(action, keyboardManager) || isMouseActive(action, mouseManager);
    }

    public boolean isKeyboardJustActivated(InputAction action, KeyboardManager keyboardManager) {
        for (int keyCode : keyBindings.get(action)) {
            if (keyboardManager.wasPressedThisFrame(keyCode)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMouseJustActivated(InputAction action, MouseManager mouseManager) {
        for (int button : mouseBindings.get(action)) {
            if (mouseManager.wasClickedThisFrame(button)) {
                return true;
            }
        }
        return false;
    }

    public boolean isJustActivated(InputAction action, KeyboardManager keyboardManager, MouseManager mouseManager) {
        return isKeyboardJustActivated(action, keyboardManager) || isMouseJustActivated(action, mouseManager);
    }
}
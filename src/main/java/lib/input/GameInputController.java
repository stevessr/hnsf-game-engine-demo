package lib.input;

import java.util.List;
import java.util.Objects;

import lib.game.GameWorld;
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.object.PlayerObject;

public final class GameInputController {
    private final KeyboardManager keyboardManager;
    private final MouseManager mouseManager;
    private final InputActionMapper actionMapper;

    public GameInputController(KeyboardManager keyboardManager, MouseManager mouseManager, InputActionMapper actionMapper) {
        this.keyboardManager = Objects.requireNonNull(keyboardManager, "keyboardManager must not be null");
        this.mouseManager = Objects.requireNonNull(mouseManager, "mouseManager must not be null");
        this.actionMapper = Objects.requireNonNull(actionMapper, "actionMapper must not be null");
    }

    public static GameInputController createDefault() {
        return new GameInputController(new KeyboardManager(), new MouseManager(), InputActionMapper.createDefaultGameMapping());
    }

    public KeyboardManager getKeyboardManager() {
        return keyboardManager;
    }

    public MouseManager getMouseManager() {
        return mouseManager;
    }

    public InputActionMapper getActionMapper() {
        return actionMapper;
    }

    public void applyInputs(GameWorld world) {
        if (world == null) {
            return;
        }
        applyPlayerMovement(world.findPlayer().orElse(null));

        MenuObject menu = findFirstActiveMenu(world);
        if (menu == null) {
            return;
        }

        DialogObject dialog = findFirstActiveDialog(world);
        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_PREVIOUS, keyboardManager)) {
            menu.previousOption();
            syncDialog(dialog, "切换到", menu.getSelectedOption());
        }
        if (actionMapper.isKeyboardJustActivated(InputAction.MENU_NEXT, keyboardManager)) {
            menu.nextOption();
            syncDialog(dialog, "切换到", menu.getSelectedOption());
        }

        if (actionMapper.isMouseJustActivated(InputAction.MENU_CONFIRM, mouseManager)) {
            int hoveredIndex = findHoveredOptionIndex(menu);
            if (hoveredIndex >= 0) {
                menu.setSelectedIndex(hoveredIndex);
            }
            syncDialog(dialog, "已确认", menu.getSelectedOption());
        } else if (actionMapper.isKeyboardJustActivated(InputAction.MENU_CONFIRM, keyboardManager)) {
            syncDialog(dialog, "已确认", menu.getSelectedOption());
        }
    }

    public void finishFrame() {
        keyboardManager.clearTransientStates();
        mouseManager.clearTransientStates();
    }

    private void applyPlayerMovement(PlayerObject player) {
        if (player == null || !player.isActive()) {
            return;
        }
        int velocityX = 0;
        int velocityY = 0;
        int speed = player.getSpeed();

        if (actionMapper.isActive(InputAction.MOVE_LEFT, keyboardManager, mouseManager)) {
            velocityX -= speed;
        }
        if (actionMapper.isActive(InputAction.MOVE_RIGHT, keyboardManager, mouseManager)) {
            velocityX += speed;
        }
        if (actionMapper.isActive(InputAction.MOVE_UP, keyboardManager, mouseManager)) {
            velocityY -= speed;
        }
        if (actionMapper.isActive(InputAction.MOVE_DOWN, keyboardManager, mouseManager)) {
            velocityY += speed;
        }

        player.setVelocity(velocityX, velocityY);
    }

    private MenuObject findFirstActiveMenu(GameWorld world) {
        List<GameObject> menus = world.getObjectsByType(GameObjectType.MENU);
        for (GameObject object : menus) {
            if (object instanceof MenuObject menu && menu.isActive()) {
                return menu;
            }
        }
        return null;
    }

    private DialogObject findFirstActiveDialog(GameWorld world) {
        List<GameObject> dialogs = world.getObjectsByType(GameObjectType.DIALOG);
        for (GameObject object : dialogs) {
            if (object instanceof DialogObject dialog && dialog.isActive()) {
                return dialog;
            }
        }
        return null;
    }

    private int findHoveredOptionIndex(MenuObject menu) {
        if (!isInsideMenu(menu, mouseManager.getMouseX(), mouseManager.getMouseY())) {
            return -1;
        }
        int optionStartY = menu.getY() + 34;
        int optionHeight = 18;
        int relativeY = mouseManager.getMouseY() - optionStartY;
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

    private void syncDialog(DialogObject dialog, String prefix, String selectedOption) {
        if (dialog == null) {
            return;
        }
        dialog.setMessage(prefix + "菜单项：" + selectedOption);
    }
}
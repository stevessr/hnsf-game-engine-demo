package lib.input;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

import lib.game.GameWorld;
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.object.PlayerObject;
import lib.object.VoxelObject;
import lib.object.ProjectileType;
import lib.state.GameSettings;
import lib.state.GameStateContext;

public final class GameInputController {
    private static final long SPRINT_WIND_SOUND_COOLDOWN_NANOS = 180_000_000L;
    private final KeyboardManager keyboardManager;
    private final MouseManager mouseManager;
    private final InputActionMapper actionMapper;
    private long lastSprintWindSoundTimeNanos;

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

    public void processInputs(GameStateContext context) {
        if (context == null) {
            return;
        }
        if (context.getWorld().getStateMachine() != null) {
            context.getWorld().getStateMachine().processInput(context);
            return;
        }
        applyPlayerMovement(
            context.getWorld(),
            context.getSettings(),
            context.getWorld().findPlayer().orElse(null),
            context.getDeltaSeconds()
        );
        applyMenuNavigation(context.getWorld());
        applyVoxelSystem(context.getWorld());
    }

    public void applyInputs(GameWorld world) {
        processInputs(new GameStateContext(world, this));
    }

    public void applyPlayerMovement(GameWorld world, GameSettings settings, PlayerObject player) {
        double deltaSeconds = settings != null && settings.getTargetFPS() > 0
            ? 1.0 / Math.max(1, settings.getTargetFPS())
            : 1.0 / 60.0;
        applyPlayerMovement(world, settings, player, deltaSeconds);
    }

    public void applyPlayerMovement(GameWorld world, GameSettings settings, PlayerObject player, double deltaSeconds) {
        if (player == null || !player.isActive()) {
            return;
        }
        if (hasActiveUiOverlay(world)) {
            player.cancelBombCharge();
            return;
        }

        if (actionMapper.isKeyboardJustActivated(InputAction.CYCLE_COLOR, keyboardManager)) {
            player.cycleColor();
        }

        if (actionMapper.isKeyboardJustActivated(InputAction.CYCLE_PROJECTILE_TYPE, keyboardManager)) {
            player.cycleProjectileType();
        }

        if (world.isGravityEnabled() && actionMapper.isKeyboardJustActivated(InputAction.JUMP, keyboardManager)) {
            player.jump(world);
        }

        double frameDeltaSeconds = deltaSeconds > 0.0 ? deltaSeconds : 1.0 / 60.0;
        boolean shootActive = actionMapper.isActive(InputAction.SHOOT, keyboardManager, mouseManager);
        boolean shootJustActivated = actionMapper.isJustActivated(InputAction.SHOOT, keyboardManager, mouseManager);
        if (player.getProjectileType() == ProjectileType.BOMB) {
            if (shootActive) {
                boolean usesMouse = actionMapper.isMouseActive(InputAction.SHOOT, mouseManager)
                    || actionMapper.isMouseJustActivated(InputAction.SHOOT, mouseManager);
                if (!player.isBombChargeActive()) {
                    player.beginBombCharge(
                        resolveShootDirX(player, usesMouse),
                        resolveShootDirY(player, usesMouse),
                        usesMouse
                    );
                }
                player.updateBombCharge(
                    resolveShootDirX(player, player.isBombChargeMouseDriven()),
                    resolveShootDirY(player, player.isBombChargeMouseDriven()),
                    frameDeltaSeconds
                );
            } else if (player.isBombChargeActive()) {
                if (player.isBombChargeMouseDriven()) {
                    player.updateBombCharge(
                        resolveShootDirX(player, true),
                        resolveShootDirY(player, true),
                        0.0
                    );
                }
                player.releaseBombCharge(world);
            }
        } else if (actionMapper.isMouseJustActivated(InputAction.SHOOT, mouseManager)) {
            player.shoot(world, mouseManager.getMouseX(), mouseManager.getMouseY());
        } else if (shootJustActivated) {
            player.shoot(world);
        }

        if (settings != null && actionMapper.isKeyboardJustActivated(InputAction.CYCLE_THROTTLE, keyboardManager)) {
            settings.cycleThrottle();
        }

        double ax = 0;
        double ay = 0;

        if (actionMapper.isActive(InputAction.MOVE_LEFT, keyboardManager, mouseManager)) {
            ax -= 1.0;
        }
        if (actionMapper.isActive(InputAction.MOVE_RIGHT, keyboardManager, mouseManager)) {
            ax += 1.0;
        }
        if (actionMapper.isActive(InputAction.MOVE_UP, keyboardManager, mouseManager)) {
            ay -= 1.0;
        }
        if (actionMapper.isActive(InputAction.MOVE_DOWN, keyboardManager, mouseManager)) {
            ay += 1.0;
        }
        
        if (actionMapper.isActive(InputAction.THROTTLE_LEFT, keyboardManager, mouseManager)) {
            ax -= 1.0;
        }
        if (actionMapper.isActive(InputAction.THROTTLE_RIGHT, keyboardManager, mouseManager)) {
            ax += 1.0;
        }
        if (actionMapper.isActive(InputAction.THROTTLE_UP, keyboardManager, mouseManager)) {
            ay -= 1.0;
        }
        if (actionMapper.isActive(InputAction.THROTTLE_DOWN, keyboardManager, mouseManager)) {
            ay += 1.0;
        }

        if (ax != 0 && ay != 0) {
            double mag = Math.sqrt(ax * ax + ay * ay);
            ax /= mag;
            ay /= mag;
        }
        boolean movingInput = ax != 0.0 || ay != 0.0;
        boolean sprintHeld = actionMapper.isKeyboardActive(InputAction.SPRINT, keyboardManager);

        if (sprintHeld && movingInput) {
            boolean burstBoost = actionMapper.isKeyboardJustActivated(InputAction.SPRINT, keyboardManager);
            if (player.sprintAccelerate(ax, ay, frameDeltaSeconds, burstBoost)) {
                playSprintWindSound(world, burstBoost);
            } else {
                player.accelerate(ax, ay, frameDeltaSeconds);
            }
        } else if (movingInput) {
            player.accelerate(ax, ay, frameDeltaSeconds);
        } else {
            player.recoverStamina(frameDeltaSeconds);
        }
    }

    private void playSprintWindSound(GameWorld world, boolean burstBoost) {
        if (world == null
            || !world.getSoundManager().isEnabled()
            || world.getSoundManager().getVolume() <= 0.0f
            || world.getSoundManager().getEffectVolume() <= 0.0f) {
            return;
        }

        long now = System.nanoTime();
        if (!burstBoost && now - lastSprintWindSoundTimeNanos < SPRINT_WIND_SOUND_COOLDOWN_NANOS) {
            return;
        }

        world.getSoundManager().playSound("wind");
        lastSprintWindSoundTimeNanos = now;
    }

    private double resolveShootDirX(PlayerObject player, boolean allowMouse) {
        if (player == null) {
            return 1.0;
        }
        if (allowMouse) {
            double dx = mouseManager.getMouseX() - (player.getX() + player.getWidth() / 2.0);
            double dy = mouseManager.getMouseY() - (player.getY() + player.getHeight() / 2.0);
            double length = Math.hypot(dx, dy);
            if (length > 0.0001) {
                return dx / length;
            }
        }
        double length = Math.hypot(player.getLastDirectionX(), player.getLastDirectionY());
        if (length > 0.0001) {
            return player.getLastDirectionX() / length;
        }
        return 1.0;
    }

    private double resolveShootDirY(PlayerObject player, boolean allowMouse) {
        if (player == null) {
            return 0.0;
        }
        if (allowMouse) {
            double dx = mouseManager.getMouseX() - (player.getX() + player.getWidth() / 2.0);
            double dy = mouseManager.getMouseY() - (player.getY() + player.getHeight() / 2.0);
            double length = Math.hypot(dx, dy);
            if (length > 0.0001) {
                return dy / length;
            }
        }
        double length = Math.hypot(player.getLastDirectionX(), player.getLastDirectionY());
        if (length > 0.0001) {
            return player.getLastDirectionY() / length;
        }
        return 0.0;
    }

    public void applyVoxelSystem(GameWorld world) {
        if (hasActiveUiOverlay(world)) {
            return;
        }
        if (actionMapper.isMouseJustActivated(InputAction.VOXEL_BUILD, mouseManager)) {
            int mx = mouseManager.getMouseX();
            int my = mouseManager.getMouseY();
            int snapX = (mx / 20) * 20;
            int snapY = (my / 20) * 20;
            boolean occupied = world.getObjects().stream()
                .anyMatch(obj -> obj.getX() == snapX && obj.getY() == snapY && obj.getType() == GameObjectType.VOXEL);
            if (!occupied) {
                world.addObject(new VoxelObject("voxel-" + System.currentTimeMillis(), snapX, snapY, 20, 20, new Color(164, 164, 180)));
            }
        }
        if (actionMapper.isMouseJustActivated(InputAction.VOXEL_DESTROY, mouseManager)) {
            int mx = mouseManager.getMouseX();
            int my = mouseManager.getMouseY();
            world.getObjects().stream()
                .filter(obj -> obj.getType() == GameObjectType.VOXEL && obj.isActive())
                .filter(obj -> mx >= obj.getX() && mx < obj.getX() + obj.getWidth() && my >= obj.getY() && my < obj.getY() + obj.getHeight())
                .findFirst()
                .ifPresent(world::removeObject);
        }
    }

    public void applyMenuNavigation(GameWorld world) {
        MenuObject menu = findFirstActiveMenu(world);
        if (menu == null) {
            return;
        }
        DialogObject dialog = findFirstActiveDialog(world);
        menu.setHoveredIndex(findHoveredOptionIndex(menu));
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
            world.getSoundManager().playSound("menu_click");
            syncDialog(dialog, "已确认", menu.getSelectedOption());
        } else if (actionMapper.isKeyboardJustActivated(InputAction.MENU_CONFIRM, keyboardManager)) {
            world.getSoundManager().playSound("menu_click");
            syncDialog(dialog, "已确认", menu.getSelectedOption());
        }
    }

    public void finishFrame() {
        keyboardManager.clearTransientStates();
        mouseManager.clearTransientStates();
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
        int mx = mouseManager.getMouseX();
        int my = mouseManager.getMouseY();

        if (!isInsideMenu(menu, mx, my)) {
            return -1;
        }
        return menu.findOptionIndexAt(mx, my);
    }

    private boolean isInsideMenu(MenuObject menu, int mouseX, int mouseY) {
        return mouseX >= menu.getX() && mouseX <= menu.getX() + menu.getWidth() && mouseY >= menu.getY() && mouseY <= menu.getY() + menu.getHeight();
    }

    private void syncDialog(DialogObject dialog, String prefix, String selectedOption) {
        if (dialog == null) {
            return;
        }
        dialog.setMessage(prefix + "菜单项：" + selectedOption);
    }

    private boolean hasActiveUiOverlay(GameWorld world) {
        if (world == null) {
            return false;
        }
        boolean hasActiveMenu = world.getObjectsByType(GameObjectType.MENU).stream()
            .anyMatch(object -> object instanceof MenuObject menu && menu.isActive());
        if (hasActiveMenu) {
            return true;
        }
        return world.getObjectsByType(GameObjectType.DIALOG).stream()
            .anyMatch(object -> object instanceof DialogObject dialog && dialog.isActive());
    }
}

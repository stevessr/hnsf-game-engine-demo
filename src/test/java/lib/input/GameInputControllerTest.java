package lib.input;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.game.GameWorld;
import lib.object.DialogObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.object.PlayerObject;
import lib.object.ProjectileObject;
import lib.object.ProjectileType;

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
    void shiftShouldIncreaseMovementAccelerationAndConsumeStamina() {
        GameInputController normalInput = GameInputController.createDefault();
        GameWorld normalWorld = new GameWorld(200, 160);
        PlayerObject normalPlayer = new PlayerObject("normal", 10, 20);
        normalPlayer.setDeceleration(1.0);
        normalWorld.addObject(normalPlayer);

        normalInput.getKeyboardManager().pressKey(KeyEvent.VK_D);
        normalInput.applyInputs(normalWorld);
        normalWorld.update(1.0);
        normalInput.finishFrame();

        GameInputController sprintInput = GameInputController.createDefault();
        GameWorld sprintWorld = new GameWorld(200, 160);
        PlayerObject sprintPlayer = new PlayerObject("sprinter", 10, 20);
        sprintPlayer.setDeceleration(1.0);
        sprintWorld.addObject(sprintPlayer);

        sprintInput.getKeyboardManager().pressKey(KeyEvent.VK_D);
        sprintInput.getKeyboardManager().pressKey(KeyEvent.VK_SHIFT);
        sprintInput.applyInputs(sprintWorld);
        sprintWorld.update(1.0);
        sprintInput.finishFrame();

        assertTrue(sprintPlayer.getVelocityX() > normalPlayer.getVelocityX(), "按住 Shift 时应有更高的水平速度");
        assertTrue(sprintPlayer.getX() > normalPlayer.getX(), "按住 Shift 时角色应移动得更远");
        assertTrue(sprintPlayer.getStamina() < sprintPlayer.getMaxStamina(), "冲刺应消耗体力");
    }

    @Test
    void staminaShouldRecoverWhenPlayerStopsMoving() {
        GameInputController inputController = GameInputController.createDefault();
        GameWorld world = new GameWorld(200, 160);
        PlayerObject player = new PlayerObject("runner", 10, 20);
        player.setDeceleration(1.0);
        player.setStamina(40.0);
        world.addObject(player);

        inputController.getKeyboardManager().pressKey(KeyEvent.VK_D);
        inputController.getKeyboardManager().pressKey(KeyEvent.VK_SHIFT);
        inputController.applyInputs(world);
        world.update(1.0 / 60.0);
        inputController.finishFrame();

        double staminaAfterSprint = player.getStamina();
        assertTrue(staminaAfterSprint < 40.0, "冲刺应减少体力");

        inputController.getKeyboardManager().releaseKey(KeyEvent.VK_D);
        inputController.getKeyboardManager().releaseKey(KeyEvent.VK_SHIFT);
        inputController.applyInputs(world);
        world.update(1.0 / 60.0);
        inputController.finishFrame();

        assertTrue(player.getStamina() > staminaAfterSprint, "停止移动后体力应恢复");
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

    @Test
    void leftMouseClickShouldShootDuringGameplay() {
        GameInputController inputController = GameInputController.createDefault();
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 100, 100);
        world.addObject(player);

        inputController.getMouseManager().moveTo(200, 200);
        inputController.getMouseManager().pressButton(MouseEvent.BUTTON1, 200, 200);
        inputController.applyInputs(world);
        inputController.finishFrame();

        assertEquals(1, world.getObjectsByType(GameObjectType.PROJECTILE).size(), "左键点击应发射子弹");
        ProjectileObject projectile = (ProjectileObject) world.getObjectsByType(GameObjectType.PROJECTILE).get(0);
        assertTrue(projectile.getVelocityX() > 0, "子弹应朝鼠标所在方向水平发射");
        assertTrue(projectile.getVelocityY() > 0, "子弹应朝鼠标所在方向垂直发射");
        assertTrue(world.getObjectsByType(GameObjectType.VOXEL).isEmpty(), "左键点击不应再创建方块");
    }

    @Test
    void bKeyShouldCyclePlayerProjectileType() {
        GameInputController inputController = GameInputController.createDefault();
        GameWorld world = new GameWorld(240, 180);
        PlayerObject player = new PlayerObject("hero", 100, 100);
        world.addObject(player);

        inputController.getKeyboardManager().pressKey(KeyEvent.VK_B);
        inputController.applyInputs(world);
        inputController.finishFrame();

        assertEquals(ProjectileType.FLARE, player.getProjectileType(), "B 键应切换到下一种子弹");
    }

    @Test
    void middleMouseClickShouldBuildVoxelDuringGameplay() {
        GameInputController inputController = GameInputController.createDefault();
        GameWorld world = new GameWorld(240, 180);

        inputController.getMouseManager().pressButton(MouseEvent.BUTTON2, 40, 40);
        inputController.applyInputs(world);
        inputController.finishFrame();

        assertEquals(1, world.getObjectsByType(GameObjectType.VOXEL).size(), "中键点击应创建方块");
    }
}

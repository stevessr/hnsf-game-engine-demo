package lib.input;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.game.GameWorld;
import lib.object.PlayerObject;
import lib.render.SwingGamePanel;

class RobotInputIntegrationTest {
    private JFrame frame;
    private SwingGamePanel panel;
    private GameWorld world;
    private PlayerObject player;

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        Assumptions.assumeTrue(Boolean.getBoolean("uiTests"), "未启用 UI Robot 测试");
        world = new GameWorld(240, 180);
        player = new PlayerObject("robot", 40, 60);
        world.addObject(player);
        EventQueue.invokeAndWait(() -> {
            panel = new SwingGamePanel(world);
            frame = new JFrame("Robot Test");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.start();
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (panel != null) {
            EventQueue.invokeAndWait(panel::stop);
        }
        if (frame != null) {
            EventQueue.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void robotShouldMovePlayerWithWASD() throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(30);

        Point screenPoint = getPanelCenterOnScreen();
        robot.mouseMove(screenPoint.x, screenPoint.y);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        ensureFocusOrSkip(1000);

        int startX = player.getX();
        robot.keyPress(KeyEvent.VK_D);
        Thread.sleep(1200);
        int endX = player.getX();
        robot.keyRelease(KeyEvent.VK_D);

        assertTrue(endX > startX, "玩家应该向右移动");
    }

    private void ensureFocusOrSkip(int timeoutMillis) throws Exception {
        int elapsed = 0;
        while (elapsed < timeoutMillis) {
            if (panel.isFocusOwner()) {
                return;
            }
            EventQueue.invokeAndWait(() -> {
                frame.toFront();
                panel.requestFocusInWindow();
            });
            Thread.sleep(100);
            elapsed += 100;
        }
        Assumptions.assumeTrue(panel.isFocusOwner(), "无法获得焦点，跳过 Robot 测试");
    }

    private Point getPanelCenterOnScreen() throws Exception {
        final Point[] point = new Point[1];
        EventQueue.invokeAndWait(() -> {
            java.awt.Point location = panel.getLocationOnScreen();
            int centerX = location.x + panel.getWidth() / 2;
            int centerY = location.y + panel.getHeight() / 2;
            point[0] = new Point(centerX, centerY);
        });
        return point[0];
    }

    private int waitForMoveX(int startX, int timeoutMillis) throws InterruptedException {
        int elapsed = 0;
        int currentX = player.getX();
        while (elapsed < timeoutMillis) {
            if (currentX > startX) {
                return currentX;
            }
            Thread.sleep(100);
            elapsed += 100;
            currentX = player.getX();
        }
        return currentX;
    }
}

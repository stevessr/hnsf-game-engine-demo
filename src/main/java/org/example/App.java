package org.example;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import lib.game.GameWorld;
import lib.object.BoundaryObject;
import lib.object.DialogObject;
import lib.object.MenuObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.object.WallObject;
import lib.render.SwingGamePanel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::startGame);
    }

    public static int add(int a, int b) {
        return a + b;
    }

    private static void startGame() {
        GameWorld world = new GameWorld(960, 540);

        SceneObject ground = new SceneObject("ground", 0, 420, 960, 120, true, true);
        ground.setColor(102, 153, 102);

        BoundaryObject topBoundary = BoundaryObject.top(960, 12);
        BoundaryObject bottomBoundary = BoundaryObject.bottom(960, 540, 12);
        BoundaryObject leftBoundary = BoundaryObject.left(540, 12);
        BoundaryObject rightBoundary = BoundaryObject.right(960, 540, 12);

        WallObject centerWall = new WallObject("center-wall", 280, 300, 100, 70);
        WallObject rightWall = new WallObject("right-wall", 610, 250, 80, 120);

        PlayerObject player = new PlayerObject("player", 120, 320);
        player.setVelocity(24, 0);

        MonsterObject slime = new MonsterObject("slime", 420, 330, 40);
        MonsterObject bat = new MonsterObject("bat", 720, 260, 60);
        bat.setSpeed(7);

        MenuObject menu = new MenuObject(
            "main-menu",
            24,
            24,
            180,
            110,
            "Demo Menu",
            List.of("Start", "Options", "Exit")
        );
        menu.setSelectedIndex(1);

        DialogObject dialog = new DialogObject(
            "intro-dialog",
            170,
            450,
            620,
            64,
            "Guide",
            "欢迎来到基础场景：这里已经有墙壁、边界、菜单和对话框。"
        );

        world.addObject(ground);
        world.addObject(topBoundary);
        world.addObject(bottomBoundary);
        world.addObject(leftBoundary);
        world.addObject(rightBoundary);
        world.addObject(centerWall);
        world.addObject(rightWall);
        world.addObject(player);
        world.addObject(slime);
        world.addObject(bat);
        world.addObject(menu);
        world.addObject(dialog);

        SwingGamePanel panel = new SwingGamePanel(world);

        JFrame frame = new JFrame("Primary Software Game Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        panel.start();

        log.info("Game window started with {} objects", world.getObjects().size());
    }
}

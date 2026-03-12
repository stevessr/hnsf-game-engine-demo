package org.example;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import lib.game.GameWorld;
import lib.object.MonsterObject;
import lib.object.PlayerObject;
import lib.object.SceneObject;
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

        PlayerObject player = new PlayerObject("player", 120, 320);
        player.setVelocity(24, 0);

        MonsterObject slime = new MonsterObject("slime", 420, 330, 40);
        MonsterObject bat = new MonsterObject("bat", 720, 260, 60);
        bat.setSpeed(7);

        world.addObject(ground);
        world.addObject(player);
        world.addObject(slime);
        world.addObject(bat);

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

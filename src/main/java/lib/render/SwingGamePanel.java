package lib.render;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.Timer;

import lib.game.GameWorld;

public final class SwingGamePanel extends JPanel {
    private final GameWorld world;
    private final Timer timer;
    private long lastUpdateNanos;

    public SwingGamePanel(GameWorld world) {
        this.world = world;
        this.timer = new Timer(16, event -> onFrame());
        this.lastUpdateNanos = 0L;
        setPreferredSize(new Dimension(world.getWidth(), world.getHeight()));
        setDoubleBuffered(true);
        setFocusable(true);
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void onFrame() {
        long now = System.nanoTime();
        double deltaSeconds = lastUpdateNanos == 0L
            ? 1.0 / 60.0
            : (now - lastUpdateNanos) / 1_000_000_000.0;
        lastUpdateNanos = now;
        world.setSize(getWidth(), getHeight());
        world.update(deltaSeconds);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2d = (Graphics2D) graphics.create();
        try {
            world.render(graphics2d);
        } finally {
            graphics2d.dispose();
        }
    }
}
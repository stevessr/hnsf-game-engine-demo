package lib.editor;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import lib.game.GameWorld;

public final class EditorGamePanel extends JPanel {
    private final GameWorld world;
    private final EditorOverlay overlay;

    public EditorGamePanel(GameWorld world, EditorOverlay overlay) {
        this.world = world;
        this.overlay = overlay == null ? new EditorOverlay() : overlay;
        if (world != null) {
            setPreferredSize(new Dimension(world.getWidth(), world.getHeight()));
        }
        setDoubleBuffered(true);
        setFocusable(true);
    }

    public GameWorld getWorld() {
        return world;
    }

    public EditorOverlay getOverlay() {
        return overlay;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (world == null) {
            return;
        }
        Graphics2D graphics2d = (Graphics2D) graphics.create();
        try {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int worldWidth = world.getWidth();
            int worldHeight = world.getHeight();

            double scaleX = (double) panelWidth / worldWidth;
            double scaleY = (double) panelHeight / worldHeight;
            double scale = Math.min(scaleX, scaleY);

            int offsetX = (int) ((panelWidth - worldWidth * scale) / 2);
            int offsetY = (int) ((panelHeight - worldHeight * scale) / 2);

            graphics2d.translate(offsetX, offsetY);
            graphics2d.scale(scale, scale);
            graphics2d.setClip(0, 0, worldWidth, worldHeight);

            world.render(graphics2d);
            overlay.render(graphics2d, worldWidth, worldHeight);
        } finally {
            graphics2d.dispose();
        }
    }
}

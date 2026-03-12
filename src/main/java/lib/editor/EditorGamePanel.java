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
            world.setSize(getWidth(), getHeight());
            world.render(graphics2d);
            overlay.render(graphics2d, getWidth(), getHeight());
        } finally {
            graphics2d.dispose();
        }
    }
}

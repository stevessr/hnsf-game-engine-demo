package lib.render;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import lib.game.GameWorld;
import lib.input.GameInputController;

public final class SwingGamePanel extends JPanel {
    private final GameWorld world;
    private final GameInputController inputController;
    private final Timer timer;
    private long lastUpdateNanos;

    public SwingGamePanel(GameWorld world) {
        this(world, GameInputController.createDefault());
    }

    public SwingGamePanel(GameWorld world, GameInputController inputController) {
        this.world = world;
        this.inputController = inputController;
        this.timer = new Timer(16, event -> onFrame());
        this.lastUpdateNanos = 0L;
        setPreferredSize(new Dimension(world.getWidth(), world.getHeight()));
        setDoubleBuffered(true);
        setFocusable(true);
        registerInputListeners();
    }

    public GameInputController getInputController() {
        return inputController;
    }

    public void start() {
        timer.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
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
        inputController.applyInputs(world);
        world.update(deltaSeconds);
        inputController.finishFrame();
        repaint();
    }

    private void registerInputListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                inputController.getKeyboardManager().pressKey(event.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent event) {
                inputController.getKeyboardManager().releaseKey(event.getKeyCode());
            }
        });

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                inputController.getMouseManager().pressButton(event.getButton(), event.getX(), event.getY());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                inputController.getMouseManager().releaseButton(event.getButton(), event.getX(), event.getY());
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                inputController.getMouseManager().moveTo(event.getX(), event.getY());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                inputController.getMouseManager().moveTo(event.getX(), event.getY());
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
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
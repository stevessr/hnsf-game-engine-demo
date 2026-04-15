package lib.render;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.KeyStroke;
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
        // Key listener (requires the panel to be focus owner)
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

        // Key bindings so key events are processed when the window is focused
        int[] keys = new int[] {
            KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_I,
            KeyEvent.VK_S, KeyEvent.VK_DOWN, KeyEvent.VK_K,
            KeyEvent.VK_A, KeyEvent.VK_LEFT, KeyEvent.VK_J,
            KeyEvent.VK_D, KeyEvent.VK_RIGHT, KeyEvent.VK_L,
            KeyEvent.VK_Q, KeyEvent.VK_E, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE
        };
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        for (int key : keys) {
            final int k = key;
            String pressAction = "press_" + k;
            String releaseAction = "release_" + k;
            inputMap.put(KeyStroke.getKeyStroke(k, 0, false), pressAction);
            inputMap.put(KeyStroke.getKeyStroke(k, 0, true), releaseAction);
            actionMap.put(pressAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    inputController.getKeyboardManager().pressKey(k);
                }
            });
            actionMap.put(releaseAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    inputController.getKeyboardManager().releaseKey(k);
                }
            });
        }

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
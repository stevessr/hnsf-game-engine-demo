package lib.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import lib.game.GameWorld;
import lib.input.GameInputController;
import lib.state.DefaultGameStateMachine;
import lib.state.GameState;

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
        setFocusTraversalKeysEnabled(false); // 允许捕获 TAB 键等
        registerInputListeners();
    }

    public GameInputController getInputController() {
        return inputController;
    }

    public void start() {
        timer.start();
        // 尝试多次请求焦点，确保在窗口显示后能获得焦点
        SwingUtilities.invokeLater(() -> {
            requestFocusInWindow();
            if (!isFocusOwner()) {
                requestFocus();
            }
        });
    }

    public void stop() {
        timer.stop();
        inputController.getKeyboardManager().reset();
    }

    public boolean isPaused() {
        return world.getCurrentState() == GameState.PAUSED;
    }

    public void setPaused(boolean paused) {
        if (world.getStateMachine() instanceof DefaultGameStateMachine dsm) {
            if (isPaused() != paused) {
                dsm.togglePause(world);
            }
        } else if (world.getStateMachine() != null) {
            GameState targetState = paused ? GameState.PAUSED : GameState.PLAYING;
            if (world.getCurrentState() != targetState && world.getStateMachine().canTransitionTo(targetState)) {
                world.getStateMachine().transitionTo(targetState);
            }
        }
        repaint();
    }

    public void togglePaused() {
        setPaused(!isPaused());
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
        // 当失去焦点时重置所有按键状态，防止按键“卡死”
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                inputController.getKeyboardManager().reset();
            }
        });

        // Key listener
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

        // Key bindings as a fallback and to handle focus more reliably
        int[] keys = new int[] {
            KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_I,
            KeyEvent.VK_S, KeyEvent.VK_DOWN, KeyEvent.VK_K,
            KeyEvent.VK_A, KeyEvent.VK_LEFT, KeyEvent.VK_J,
            KeyEvent.VK_D, KeyEvent.VK_RIGHT, KeyEvent.VK_L,
            KeyEvent.VK_Q, KeyEvent.VK_E, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE,
            KeyEvent.VK_ESCAPE, KeyEvent.VK_P
        };
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        for (int key : keys) {
            final int k = key;
            String pressAction = "press_" + k;
            String releaseAction = "release_" + k;
            
            // 使用 KeyStroke.getKeyStroke(k, 0) 这种更通用的形式
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
            if (world.getCurrentState() == GameState.PAUSED) {
                renderPausedOverlay(graphics2d);
            }
        } finally {
            graphics2d.dispose();
        }
    }

    private void renderPausedOverlay(Graphics2D graphics2d) {
        graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        graphics2d.setColor(Color.BLACK);
        graphics2d.fillRect(0, 0, getWidth(), getHeight());
        graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        graphics2d.setFont(new Font("SansSerif", Font.BOLD, 36));
        String text = "PAUSED";
        int textWidth = graphics2d.getFontMetrics().stringWidth(text);
        int textX = (getWidth() - textWidth) / 2;
        int textY = getHeight() / 2;
        graphics2d.setColor(Color.WHITE);
        graphics2d.drawString(text, textX, textY);
    }
}

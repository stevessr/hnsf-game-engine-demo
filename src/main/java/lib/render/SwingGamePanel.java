package lib.render;

import java.awt.Dimension;
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
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.state.DefaultGameStateMachine;
import lib.state.GameRuntimeActions;
import lib.state.GameSettings;
import lib.state.GameState;
import lib.state.GameStateContext;

public final class SwingGamePanel extends JPanel implements GameSettings {
    private final GameWorld world;
    private final GameInputController inputController;
    private final Timer timer;
    private GameRuntimeActions runtimeActions = GameRuntimeActions.noOp();
    private long lastUpdateNanos;
    private int targetFPS = 60;
    private int uiFontSize = 18;

    public SwingGamePanel(GameWorld world) {
        this(world, GameInputController.createDefault());
    }

    public SwingGamePanel(GameWorld world, GameInputController inputController) {
        this.world = world;
        this.inputController = inputController;
        this.timer = new Timer(1000 / targetFPS, event -> onFrame());
        this.lastUpdateNanos = 0L;
        setPreferredSize(new Dimension(world.getWidth(), world.getHeight()));
        setDoubleBuffered(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // 允许捕获 TAB 键等
        registerInputListeners();
    }

    public void setTargetFPS(int fps) {
        if (fps <= 0) {
            fps = 60;
        }
        this.targetFPS = fps;
        timer.setDelay(1000 / targetFPS);
    }

    public int getTargetFPS() {
        return targetFPS;
    }

    @Override
    public void setThrottlePower(int power) {
        world.findPlayer().ifPresent(player -> player.setThrottlePower(power));
    }

    @Override
    public int getThrottlePower() {
        return world.findPlayer().map(p -> p.getThrottlePower()).orElse(600);
    }

    @Override
    public void setDeceleration(int percent) {
        world.findPlayer().ifPresent(player -> player.setDeceleration(percent / 100.0));
    }

    @Override
    public int getDeceleration() {
        return world.findPlayer().map(p -> p.getDecelerationPercent()).orElse(92);
    }

    @Override
    public void setGravityEnabled(boolean enabled) {
        if (world != null) {
            world.setGravityEnabled(enabled);
        }
    }

    @Override
    public boolean isGravityEnabled() {
        return world != null && world.isGravityEnabled();
    }

    @Override
    public void setGravityStrength(int strength) {
        if (world != null) {
            world.setGravityStrength(strength);
        }
    }

    @Override
    public int getGravityStrength() {
        return world != null ? world.getGravityStrength() : 900;
    }

    @Override
    public void setUIFontSize(int fontSize) {
        this.uiFontSize = Math.max(10, Math.min(64, fontSize));
        applyUIFontSizeToWorld();
        repaint();
    }

    @Override
    public int getUIFontSize() {
        return uiFontSize;
    }

    @Override
    public void setLogicalResolution(int width, int height) {
        if (world != null) {
            world.setSize(width, height);
            applyUIFontSizeToWorld();
            repaint();
        }
    }

    @Override
    public void forceRepaint() {
        repaint();
    }

    public void setResolution(int width, int height) {
        // 仅修改显示分辨率（面板首选大小），而不修改逻辑世界大小
        // 这样 paintComponent 中的缩放逻辑就会自动将逻辑世界拟合到新窗口中
        setPreferredSize(new Dimension(width, height));
        
        // 寻找父窗口并重调大小
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof javax.swing.JFrame frame) {
            frame.pack();
            frame.setLocationRelativeTo(null);
        }
        revalidate();
        repaint();
    }

    public GameInputController getInputController() {
        return inputController;
    }

    public void setRuntimeActions(GameRuntimeActions runtimeActions) {
        this.runtimeActions = runtimeActions == null ? GameRuntimeActions.noOp() : runtimeActions;
    }

    public void applyUIFontSizeToWorld() {
        if (world == null) {
            return;
        }
        for (GameObject object : world.getObjectsByType(GameObjectType.MENU)) {
            if (object instanceof MenuObject menu) {
                menu.setFontSize(uiFontSize);
                menu.setSize(menu.getWidth(), Math.max(menu.getHeight(), menu.getPreferredHeight()));
            }
        }
        for (GameObject object : world.getObjectsByType(GameObjectType.DIALOG)) {
            if (object instanceof DialogObject dialog) {
                dialog.setFontSize(uiFontSize);
            }
        }
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
                dsm.togglePause(world, this);
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
        inputController.processInputs(new GameStateContext(world, inputController, this, runtimeActions));
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

            // 限制裁剪区域以防溢出
            graphics2d.setClip(0, 0, worldWidth, worldHeight);

            world.render(graphics2d);
        } finally {
            graphics2d.dispose();
        }
    }
}

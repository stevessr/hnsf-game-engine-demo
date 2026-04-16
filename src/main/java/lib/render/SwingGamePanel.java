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

import org.json.JSONArray;
import org.json.JSONObject;

import lib.game.GameWorld;
import lib.input.GameInputController;
import lib.input.InputAction;
import lib.object.DialogObject;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.MenuObject;
import lib.persistence.SettingsRepository;
import lib.state.DefaultGameStateMachine;
import lib.state.GameRuntimeActions;
import lib.state.GameSettings;
import lib.state.GameState;
import lib.state.GameStateContext;

public final class SwingGamePanel extends JPanel implements GameSettings {
    private final GameWorld world;
    private final GameInputController inputController;
    private final Timer timer;
    private final SettingsRepository settingsRepository;
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
        this.settingsRepository = new SettingsRepository();
        this.timer = new Timer(1000 / targetFPS, event -> onFrame());
        this.lastUpdateNanos = 0L;
        
        loadPersistentSettings();
        
        setPreferredSize(new Dimension(world.getWidth(), world.getHeight()));
        setDoubleBuffered(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        registerInputListeners();
    }

    private void loadPersistentSettings() {
        JSONObject json = settingsRepository.loadSettings();
        if (json.isEmpty()) {
            return;
        }
        
        this.targetFPS = json.optInt("targetFPS", 60);
        this.uiFontSize = json.optInt("uiFontSize", 18);
        int w = json.optInt("width", world.getWidth());
        int h = json.optInt("height", world.getHeight());
        
        setTargetFPS(targetFPS);
        setUIFontSize(uiFontSize);
        setResolution(w, h);
        
        if (json.has("gravityEnabled")) {
            setGravityEnabled(json.getBoolean("gravityEnabled"));
        }
        
        if (json.has("lightingEnabled")) {
            setLightingEnabled(json.getBoolean("lightingEnabled"));
        }

        if (json.has("keyBindings")) {
            deserializeKeyBindings(json.getJSONObject("keyBindings"));
        }
        
        // Apply player settings if available
        world.findPlayer().ifPresent(player -> {
            player.setThrottlePower(json.optInt("throttlePower", 600));
            player.setDeceleration(json.optInt("deceleration", 92) / 100.0);
        });
    }

    private void savePersistentSettings() {
        settingsRepository.saveSettings(
            targetFPS,
            uiFontSize,
            getWidth(),
            getHeight(),
            getThrottlePower(),
            getDeceleration(),
            isGravityEnabled(),
            isLightingEnabled(),
            serializeKeyBindings()
        );
    }

    private JSONObject serializeKeyBindings() {
        JSONObject json = new JSONObject();
        var mapper = inputController.getActionMapper();
        for (InputAction action : InputAction.values()) {
            JSONArray keys = new JSONArray();
            var bindings = mapper.getKeyBindings().get(action);
            if (bindings != null) {
                for (int code : bindings) {
                    keys.put(code);
                }
            }
            json.put(action.name(), keys);
        }
        return json;
    }

    private void deserializeKeyBindings(JSONObject json) {
        var mapper = inputController.getActionMapper();
        for (InputAction action : InputAction.values()) {
            if (json.has(action.name())) {
                mapper.clearBindings(action);
                JSONArray keys = json.getJSONArray(action.name());
                for (int i = 0; i < keys.length(); i++) {
                    mapper.bindKey(action, keys.getInt(i));
                }
            }
        }
    }

    @Override
    public void setTargetFPS(int fps) {
        if (fps <= 0) {
            fps = 60;
        }
        this.targetFPS = fps;
        timer.setDelay(1000 / targetFPS);
        savePersistentSettings();
    }

    @Override
    public int getTargetFPS() {
        return targetFPS;
    }

    @Override
    public void setThrottlePower(int power) {
        world.findPlayer().ifPresent(player -> player.setThrottlePower(power));
        savePersistentSettings();
    }

    @Override
    public int getThrottlePower() {
        return world.findPlayer().map(p -> p.getThrottlePower()).orElse(600);
    }

    @Override
    public void cycleThrottle() {
        int current = getThrottlePower();
        int[] powers = {200, 400, 600, 800, 1000};
        int nextIdx = 0;
        for (int i = 0; i < powers.length; i++) {
            if (powers[i] >= current) {
                nextIdx = (i + 1) % powers.length;
                break;
            }
        }
        setThrottlePower(powers[nextIdx]);
    }

    @Override
    public void setDeceleration(int percent) {
        world.findPlayer().ifPresent(player -> player.setDeceleration(percent / 100.0));
        savePersistentSettings();
    }

    @Override
    public int getDeceleration() {
        return world.findPlayer().map(p -> p.getDecelerationPercent()).orElse(92);
    }

    @Override
    public void setGravityEnabled(boolean enabled) {
        if (world != null) {
            world.setGravityEnabled(enabled);
            savePersistentSettings();
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
        savePersistentSettings();
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

    @Override
    public boolean isLightingEnabled() {
        return world != null && world.getLightingManager().isEnabled();
    }

    @Override
    public void setLightingEnabled(boolean enabled) {
        if (world != null) {
            world.getLightingManager().setEnabled(enabled);
            savePersistentSettings();
            repaint();
        }
    }

    public void setResolution(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof javax.swing.JFrame frame) {
            frame.pack();
            frame.setLocationRelativeTo(null);
        }
        savePersistentSettings();
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
        if (world.getStateMachine() instanceof DefaultGameStateMachine dsm) {
            dsm.recenterUI(world);
        }
    }

    public void start() {
        timer.start();
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
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                inputController.getKeyboardManager().reset();
            }
        });

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

        // Use dynamic keys from mapper for InputMap
        syncInputMap();
        
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

    public void syncInputMap() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.clear();
        actionMap.clear();
        
        var mapper = inputController.getActionMapper();
        for (InputAction action : InputAction.values()) {
            var bindings = mapper.getKeyBindings().get(action);
            if (bindings != null) {
                for (int k : bindings) {
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
            }
        }
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
            graphics2d.setClip(0, 0, worldWidth, worldHeight);

            world.render(graphics2d);
        } finally {
            graphics2d.dispose();
        }
    }
}

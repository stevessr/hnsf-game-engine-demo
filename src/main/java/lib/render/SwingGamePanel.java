package lib.render;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import lib.game.GameWorld;
import lib.input.GameInputController;
import lib.input.InputAction;
import lib.manager.DebugManager;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SwingGamePanel extends JPanel implements GameSettings {
    private final GameWorld world;
    private final GameInputController inputController;
    private final Timer timer;
    private final SettingsRepository settingsRepository;
    private final Camera camera;
    private final ControlHintsOverlay hintsOverlay;
    private final DebugManager debugManager;
    private final MinimapOverlay minimapOverlay;
    private final GoalOverlay goalOverlay;
    private final lib.manager.AITestManager aiTestManager;
    private final BufferedImage mainMenuBackdrop;
    private final BufferedImage pauseMenuBackdrop;
    private GameRuntimeActions runtimeActions = GameRuntimeActions.noOp();
    private long lastUpdateNanos;
    private int targetFPS = 60;
    private int uiFontSize = 18;
    private boolean debugEnabled = false;

    public SwingGamePanel(GameWorld world) {
        this(world, GameInputController.createDefault());
    }

    public SwingGamePanel(GameWorld world, GameInputController inputController) {
        this.world = world;
        this.inputController = inputController;
        this.settingsRepository = new SettingsRepository();
        this.hintsOverlay = new ControlHintsOverlay();
        this.debugManager = new DebugManager();
        this.minimapOverlay = new MinimapOverlay();
        this.goalOverlay = new GoalOverlay();
        this.aiTestManager = new lib.manager.AITestManager();
        this.mainMenuBackdrop = BackgroundAssets.loadMainMenuBackdrop();
        this.pauseMenuBackdrop = BackgroundAssets.loadPauseMenuBackdrop();
        
        this.camera = new Camera(960, 540);
        world.setCamera(camera);
        
        this.timer = new Timer(1000 / targetFPS, event -> onFrame());
        this.lastUpdateNanos = 0L;
        
        loadPersistentSettings();
        
        setPreferredSize(new Dimension(960, 540));
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
        
        this.debugEnabled = json.optBoolean("debugEnabled", false);
        this.targetFPS = json.optInt("targetFPS", 60);
        this.uiFontSize = json.optInt("uiFontSize", 18);
        int w = json.optInt("width", 960);
        int h = json.optInt("height", 540);
        
        setTargetFPS(targetFPS);
        setUIFontSize(uiFontSize);
        setResolution(w, h);
        
        if (json.has("gravityEnabled")) {
            setGravityEnabled(json.getBoolean("gravityEnabled"));
        }
        
        if (json.has("lightingEnabled")) {
            setLightingEnabled(json.getBoolean("lightingEnabled"));
        }

        if (json.has("ambientLight")) {
            setAmbientLight((float)json.getDouble("ambientLight"));
        }
        if (json.has("lightingIntensity")) {
            setLightingIntensity((float)json.getDouble("lightingIntensity"));
        }
        
        if (json.has("volume")) {
            setVolume((float)json.getDouble("volume"));
        }
        if (json.has("soundEnabled")) {
            setSoundEnabled(json.getBoolean("soundEnabled"));
        }
        if (json.has("damageVolume")) {
            setDamageVolume((float) json.getDouble("damageVolume"));
        }
        if (json.has("shootVolume")) {
            setShootVolume((float) json.getDouble("shootVolume"));
        }
        if (json.has("menuVolume")) {
            setMenuVolume((float) json.getDouble("menuVolume"));
        }
        if (json.has("effectVolume")) {
            setEffectVolume((float) json.getDouble("effectVolume"));
        }

        if (json.has("keyBindings")) {
            deserializeKeyBindings(json.getJSONObject("keyBindings"));
        }
        
        world.findPlayer().ifPresent(player -> {
            player.setThrottlePower(json.optInt("throttlePower", 600));
            player.setDeceleration(json.optInt("deceleration", 92) / 100.0);
        });
    }

    @Override
    public void savePersistentSettings() {
        settingsRepository.saveSettings(
            targetFPS,
            uiFontSize,
            resolveSavedPanelWidth(),
            resolveSavedPanelHeight(),
            getThrottlePower(),
            getDeceleration(),
            isGravityEnabled(),
            isLightingEnabled(),
            isDebugEnabled(),
            getAmbientLight(),
            getLightingIntensity(),
            getVolume(),
            isSoundEnabled(),
            getDamageVolume(),
            getShootVolume(),
            getMenuVolume(),
            getEffectVolume(),
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
                mapper.clearKeyBindings(action);
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

    @Override
    public float getAmbientLight() {
        return world != null ? world.getLightingManager().getAmbientLight() : 0.0f;
    }

    @Override
    public void setAmbientLight(float intensity) {
        if (world != null) {
            world.getLightingManager().setAmbientLight(intensity);
            savePersistentSettings();
            repaint();
        }
    }

    @Override
    public float getLightingIntensity() {
        return world != null ? world.getLightingManager().getIntensityMultiplier() : 1.0f;
    }

    @Override
    public void setLightingIntensity(float intensity) {
        if (world != null) {
            world.getLightingManager().setIntensityMultiplier(intensity);
            savePersistentSettings();
            repaint();
        }
    }

    @Override
    public float getVolume() {
        return world != null ? world.getSoundManager().getVolume() : 1.0f;
    }

    @Override
    public void setVolume(float volume) {
        if (world != null) {
            world.getSoundManager().setVolume(volume);
            savePersistentSettings();
        }
    }

    @Override
    public boolean isSoundEnabled() {
        return world != null ? world.getSoundManager().isEnabled() : true;
    }

    @Override
    public void setSoundEnabled(boolean enabled) {
        if (world != null) {
            world.getSoundManager().setEnabled(enabled);
            savePersistentSettings();
        }
    }

    @Override
    public float getDamageVolume() {
        return world != null ? world.getSoundManager().getDamageVolume() : 1.0f;
    }

    @Override
    public void setDamageVolume(float volume) {
        if (world != null) {
            world.getSoundManager().setDamageVolume(volume);
            savePersistentSettings();
        }
    }

    @Override
    public float getShootVolume() {
        return world != null ? world.getSoundManager().getShootVolume() : 1.0f;
    }

    @Override
    public void setShootVolume(float volume) {
        if (world != null) {
            world.getSoundManager().setShootVolume(volume);
            savePersistentSettings();
        }
    }

    @Override
    public float getMenuVolume() {
        return world != null ? world.getSoundManager().getMenuVolume() : 1.0f;
    }

    @Override
    public void setMenuVolume(float volume) {
        if (world != null) {
            world.getSoundManager().setMenuVolume(volume);
            savePersistentSettings();
        }
    }

    @Override
    public float getEffectVolume() {
        return world != null ? world.getSoundManager().getEffectVolume() : 1.0f;
    }

    @Override
    public void setEffectVolume(float volume) {
        if (world != null) {
            world.getSoundManager().setEffectVolume(volume);
            savePersistentSettings();
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        savePersistentSettings();
        repaint();
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
        
        // Cap deltaSeconds to avoid physics explosions when resuming from a long pause
        if (deltaSeconds > 0.1) {
            deltaSeconds = 0.1;
        }
        
        inputController.processInputs(new GameStateContext(world, inputController, this, runtimeActions, deltaSeconds));
        if (aiTestManager.isEnabled()) {
            aiTestManager.update(world, deltaSeconds);
        }
        world.update(deltaSeconds);
        
        world.findPlayer().ifPresent(player -> camera.update(world, player));
        
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
                if (event.getKeyCode() == KeyEvent.VK_F3) {
                    toggleDebug();
                } else if (event.getKeyCode() == KeyEvent.VK_H) {
                    aiTestManager.setEnabled(!aiTestManager.isEnabled());
                    log.info("AI Auto-play: {}", aiTestManager.isEnabled() ? "ENABLED" : "DISABLED");
                } else if (event.getKeyCode() == KeyEvent.VK_BACK_QUOTE && debugEnabled) {
                    showDebugConsole();
                }
                inputController.getKeyboardManager().pressKey(event.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent event) {
                inputController.getKeyboardManager().releaseKey(event.getKeyCode());
            }
        });

        syncInputMap();
        
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                double scale = getScale();
                int viewW = getViewportWidth();
                int viewH = getViewportHeight();
                int offsetX = (int) ((getWidth() - viewW * scale) / 2);
                int offsetY = (int) ((getHeight() - viewH * scale) / 2);

                int logicalX = resolveInputX(event, scale, offsetX);
                int logicalY = resolveInputY(event, scale, offsetY);

                inputController.getMouseManager().pressButton(event.getButton(), logicalX, logicalY);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                double scale = getScale();
                int viewW = getViewportWidth();
                int viewH = getViewportHeight();
                int offsetX = (int) ((getWidth() - viewW * scale) / 2);
                int offsetY = (int) ((getHeight() - viewH * scale) / 2);
                int logicalX = resolveInputX(event, scale, offsetX);
                int logicalY = resolveInputY(event, scale, offsetY);
                inputController.getMouseManager().releaseButton(event.getButton(), logicalX, logicalY);
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                double scale = getScale();
                int viewW = getViewportWidth();
                int viewH = getViewportHeight();
                int offsetX = (int) ((getWidth() - viewW * scale) / 2);
                int offsetY = (int) ((getHeight() - viewH * scale) / 2);
                int logicalX = resolveInputX(event, scale, offsetX);
                int logicalY = resolveInputY(event, scale, offsetY);
                inputController.getMouseManager().moveTo(logicalX, logicalY);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                mouseMoved(event);
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private int resolveInputX(MouseEvent event, double scale, int offsetX) {
        int screenX = (int) ((event.getX() - offsetX) / scale);
        if (usesScreenSpaceInput()) {
            return screenX;
        }
        return screenX + camera.getX();
    }

    private int resolveInputY(MouseEvent event, double scale, int offsetY) {
        int screenY = (int) ((event.getY() - offsetY) / scale);
        if (usesScreenSpaceInput()) {
            return screenY;
        }
        return screenY + camera.getY();
    }

    private boolean usesScreenSpaceInput() {
        GameState currentState = world == null ? GameState.PLAYING : world.getCurrentState();
        return currentState.allowsMenuNavigation() || currentState.allowsDialogInteraction();
    }

    private void showDebugConsole() {
        String cmd = JOptionPane.showInputDialog(this, "Debug Console (god, auto, speed [val], tp [x] [y], heal):", "Debug Console", JOptionPane.PLAIN_MESSAGE);
        if (cmd != null && !cmd.isBlank()) {
            if ("auto".equalsIgnoreCase(cmd.trim())) {
                aiTestManager.setEnabled(!aiTestManager.isEnabled());
                debugManager.log("AI Auto-play: " + (aiTestManager.isEnabled() ? "ON" : "OFF"));
            } else {
                debugManager.executeCommand(cmd, world);
            }
            repaint();
        }
    }

    private double getScale() {
        double scaleX = (double) getWidth() / getViewportWidth();
        double scaleY = (double) getHeight() / getViewportHeight();
        return Math.min(scaleX, scaleY);
    }

    private int getViewportWidth() {
        return camera == null ? 960 : camera.getViewportWidth();
    }

    private int getViewportHeight() {
        return camera == null ? 540 : camera.getViewportHeight();
    }

    private int resolveSavedPanelWidth() {
        int currentWidth = getWidth();
        if (currentWidth > 0) {
            return currentWidth;
        }
        Dimension preferredSize = getPreferredSize();
        if (preferredSize != null && preferredSize.width > 0) {
            return preferredSize.width;
        }
        return 960;
    }

    private int resolveSavedPanelHeight() {
        int currentHeight = getHeight();
        if (currentHeight > 0) {
            return currentHeight;
        }
        Dimension preferredSize = getPreferredSize();
        if (preferredSize != null && preferredSize.height > 0) {
            return preferredSize.height;
        }
        return 540;
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
            int viewW = getViewportWidth();
            int viewH = getViewportHeight();
            double scale = getScale();
            int offsetX = (int) ((panelWidth - viewW * scale) / 2);
            int offsetY = (int) ((panelHeight - viewH * scale) / 2);

            graphics2d.translate(offsetX, offsetY);
            graphics2d.scale(scale, scale);
            graphics2d.setClip(0, 0, viewW, viewH);

            world.renderBackgroundLayer(graphics2d);
            world.renderWorldLayer(graphics2d);
            renderStateBackdrop(graphics2d, viewW, viewH);
            world.renderUiLayer(graphics2d);
            if (shouldRenderGameplayHud(world)) {
                hintsOverlay.render(graphics2d, viewW, viewH, world, aiTestManager.isEnabled());
            }
            
            if (debugEnabled) {
                debugManager.render(graphics2d, world, viewW, viewH);
            }
            if (shouldRenderGameplayHud(world)) {
                minimapOverlay.render(graphics2d, world, viewW, viewH);
                goalOverlay.render(graphics2d, world, viewW, viewH);
            }
        } finally {
            graphics2d.dispose();
        }
    }

    private boolean shouldRenderGameplayHud(GameWorld currentWorld) {
        if (currentWorld == null) {
            return false;
        }
        return switch (currentWorld.getCurrentState()) {
            case MENU, PAUSED -> false;
            default -> true;
        };
    }

    private void renderStateBackdrop(Graphics2D graphics, int viewWidth, int viewHeight) {
        BufferedImage backdrop = switch (world.getCurrentState()) {
            case MENU -> mainMenuBackdrop;
            case PAUSED -> pauseMenuBackdrop;
            default -> null;
        };
        if (backdrop == null) {
            return;
        }

        float alpha = world.getCurrentState() == GameState.PAUSED ? 0.82f : 0.96f;
        var previousComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
        graphics.drawImage(backdrop, 0, 0, viewWidth, viewHeight, null);
        graphics.setComposite(previousComposite);
    }
}

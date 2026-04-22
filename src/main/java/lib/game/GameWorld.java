package lib.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import lib.manager.EntityManager;
import lib.manager.SoundManager;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.PlayerObject;
import lib.object.SceneObject;
import lib.object.dto.MapBackgroundMode;
import lib.physics.MovementResult;
import lib.physics.PhysicsEngine;
import lib.render.Camera;
import lib.render.LightingManager;
import lib.state.DefaultGameStateMachine;
import lib.state.GameState;
import lib.state.GameStateMachine;

public final class GameWorld {
    private final EntityManager entityManager;
    private final PhysicsEngine physicsEngine;
    private final LightingManager lightingManager;
    private final SoundManager soundManager;
    private Camera camera;
    private GameStateMachine stateMachine;
    private int width;
    private int height;
    private Color backgroundColor;
    private MapBackgroundMode backgroundMode = MapBackgroundMode.GRADIENT;
    private BufferedImage backgroundImage;
    private String backgroundImageName;
    private boolean gravityEnabled;
    private int gravityStrength;
    private WinConditionType winCondition = WinConditionType.REACH_GOAL;
    private int targetKills = 0;
    private int targetItems = 0;
    private int kills = 0;
    private int itemsCollected = 0;
    private int respawnX = 0;
    private int respawnY = 0;
    private boolean respawnPointSet = false;
    private boolean showGoals = true;
    private String failureReason;
    private double screenShakeMagnitude = 0.0;
    private double screenShakeDuration = 0.0;
    private double screenShakeRemaining = 0.0;
    private double screenShakeElapsed = 0.0;

    public GameWorld(int width, int height) {
        this(width, height, new Color(32, 36, 48));
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public WinConditionType getWinCondition() {
        return winCondition;
    }

    public void setWinCondition(WinConditionType winCondition) {
        this.winCondition = winCondition == null ? WinConditionType.REACH_GOAL : winCondition;
    }

    public int getTargetKills() {
        return targetKills;
    }

    public void setTargetKills(int targetKills) {
        this.targetKills = Math.max(0, targetKills);
    }

    public int getTargetItems() {
        return targetItems;
    }

    public void setTargetItems(int targetItems) {
        this.targetItems = Math.max(0, targetItems);
    }

    public int getKills() {
        return kills;
    }

    public void recordKill() {
        this.kills++;
    }

    public int getItemsCollected() {
        return itemsCollected;
    }

    public void recordItemCollection() {
        this.itemsCollected++;
    }

    public boolean hasRespawnPoint() {
        return respawnPointSet;
    }

    public void setRespawnPoint(int x, int y) {
        this.respawnX = Math.max(0, x);
        this.respawnY = Math.max(0, y);
        this.respawnPointSet = true;
    }

    public boolean respawnPlayer() {
        PlayerObject player = entityManager.getObjects().stream()
            .filter(object -> object.getType() == GameObjectType.PLAYER)
            .filter(PlayerObject.class::isInstance)
            .map(PlayerObject.class::cast)
            .findFirst()
            .orElse(null);
        if (player == null) {
            return false;
        }

        int spawnX = respawnPointSet ? respawnX : player.getX();
        int spawnY = respawnPointSet ? respawnY : player.getY();
        int maxX = Math.max(0, width - player.getWidth());
        int maxY = Math.max(0, height - player.getHeight());
        int clampedX = Math.max(0, Math.min(maxX, spawnX));
        int clampedY = Math.max(0, Math.min(maxY, spawnY));
        player.respawnAt(clampedX, clampedY);
        setFailureReason(null);
        return true;
    }

    public boolean isShowGoals() {
        return showGoals;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            this.failureReason = null;
            return;
        }
        this.failureReason = failureReason.trim();
    }

    public void triggerScreenShake(double magnitude, double durationSeconds) {
        if (magnitude <= 0.0 || durationSeconds <= 0.0) {
            return;
        }
        screenShakeMagnitude = Math.max(screenShakeMagnitude, magnitude);
        screenShakeDuration = Math.max(screenShakeDuration, durationSeconds);
        screenShakeRemaining = Math.max(screenShakeRemaining, durationSeconds);
        screenShakeElapsed = 0.0;
    }

    public boolean isScreenShaking() {
        return screenShakeRemaining > 0.0;
    }

    public int getScreenShakeOffsetX() {
        if (!isScreenShaking() || screenShakeDuration <= 0.0) {
            return 0;
        }
        double progress = screenShakeRemaining / screenShakeDuration;
        double amplitude = screenShakeMagnitude * progress * progress;
        double wave = Math.sin(screenShakeElapsed * 56.0 + 0.7);
        return (int) Math.round(wave * amplitude);
    }

    public int getScreenShakeOffsetY() {
        if (!isScreenShaking() || screenShakeDuration <= 0.0) {
            return 0;
        }
        double progress = screenShakeRemaining / screenShakeDuration;
        double amplitude = screenShakeMagnitude * progress * progress * 0.75;
        double wave = Math.cos(screenShakeElapsed * 63.0 + 1.9);
        return (int) Math.round(wave * amplitude);
    }

    public void setShowGoals(boolean showGoals) {
        this.showGoals = showGoals;
    }

    public void toggleShowGoals() {
        this.showGoals = !this.showGoals;
    }

    public boolean isComplete() {
        return switch (winCondition) {
            case REACH_GOAL -> false; // Handled by GoalObject collision usually
            case KILL_ALL_MONSTERS -> getObjectsByType(GameObjectType.MONSTER).stream().noneMatch(GameObject::isActive);
            case KILL_TARGET_COUNT -> kills >= targetKills;
            case COLLECT_TARGET_COUNT -> itemsCollected >= targetItems;
            case CLEAR_ALL_ITEMS -> getObjectsByType(GameObjectType.ITEM).stream().noneMatch(GameObject::isActive);
        };
    }

    public GameWorld(int width, int height, Color backgroundColor) {
        this.entityManager = new EntityManager();
        this.physicsEngine = new PhysicsEngine();
        this.lightingManager = new LightingManager();
        this.soundManager = new SoundManager();
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.backgroundColor = backgroundColor == null ? new Color(32, 36, 48) : backgroundColor;
        this.gravityEnabled = false;
        this.gravityStrength = 900;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public LightingManager getLightingManager() {
        return lightingManager;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setSize(int width, int height) {
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        if (stateMachine instanceof DefaultGameStateMachine dsm) {
            dsm.recenterUI(this);
        }
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        if (backgroundColor == null) {
            return;
        }
        this.backgroundColor = backgroundColor;
    }

    public MapBackgroundMode getBackgroundMode() {
        return backgroundMode;
    }

    public void setBackgroundMode(MapBackgroundMode backgroundMode) {
        this.backgroundMode = backgroundMode == null ? MapBackgroundMode.GRADIENT : backgroundMode;
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public String getBackgroundImageName() {
        return backgroundImageName;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        setBackgroundImage(backgroundImage, null);
    }

    public void setBackgroundImage(BufferedImage backgroundImage, String backgroundImageName) {
        this.backgroundImage = backgroundImage;
        if (backgroundImageName != null && !backgroundImageName.isBlank()) {
            this.backgroundImageName = backgroundImageName.trim();
        } else if (backgroundImage == null) {
            this.backgroundImageName = null;
        } else {
            this.backgroundImageName = null;
        }
    }

    public void clearBackgroundImage() {
        this.backgroundImage = null;
        this.backgroundImageName = null;
    }

    public boolean isGravityEnabled() {
        return gravityEnabled;
    }

    public void setGravityEnabled(boolean gravityEnabled) {
        this.gravityEnabled = gravityEnabled;
    }

    public int getGravityStrength() {
        return gravityStrength;
    }

    public void setGravityStrength(int gravityStrength) {
        this.gravityStrength = Math.max(0, gravityStrength);
    }

    /**
     * 设置游戏状态机。
     *
     * @param stateMachine 状态机实例
     */
    public void setStateMachine(GameStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * 获取游戏状态机。
     *
     * @return 状态机实例，如果未设置则返回 null
     */
    public GameStateMachine getStateMachine() {
        return stateMachine;
    }

    /**
     * 获取当前游戏状态。
     * 如果未设置状态机，默认返回 PLAYING 状态。
     *
     * @return 当前游戏状态
     */
    public GameState getCurrentState() {
        return stateMachine != null ? stateMachine.getCurrentState() : GameState.PLAYING;
    }

    public void addObject(GameObject gameObject) {
        entityManager.add(gameObject);
        if (!respawnPointSet && gameObject instanceof PlayerObject player) {
            setRespawnPoint(player.getX(), player.getY());
        }
    }

    public boolean removeObject(GameObject gameObject) {
        return entityManager.remove(gameObject);
    }

    public List<GameObject> getObjects() {
        return entityManager.getObjects();
    }

    public List<GameObject> getActiveObjects() {
        return entityManager.getActiveObjects();
    }

    public List<GameObject> getObjectsByType(GameObjectType type) {
        return entityManager.getObjectsByType(type);
    }

    public List<SceneObject> getSolidObjects() {
        return entityManager.getSolidScenes();
    }

    public Optional<PlayerObject> findPlayer() {
        return entityManager.getObjects().stream()
            .filter(GameObject::isActive)
            .filter(object -> object.getType() == GameObjectType.PLAYER)
            .filter(PlayerObject.class::isInstance)
            .map(PlayerObject.class::cast)
            .findFirst();
    }

    public MovementResult moveObject(GameObject gameObject, int targetX, int targetY) {
        MovementResult result = physicsEngine.resolveMovement(
            gameObject,
            targetX,
            targetY,
            width,
            height,
            getSolidObjects()
        );
        gameObject.setPosition(result.getResolvedX(), result.getResolvedY());
        return result;
    }

    public boolean collidesWithSolid(GameObject gameObject, int targetX, int targetY) {
        return physicsEngine.collidesAt(gameObject, targetX, targetY, getSolidObjects());
    }

    public List<GameObject> getCollisions(GameObject gameObject) {
        return physicsEngine.findCollisions(gameObject, getActiveObjects());
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }
        if (!getCurrentState().allowsWorldUpdate()) {
            return;
        }
        updateScreenShake(deltaSeconds);
        entityManager.updateAll(this, deltaSeconds);
        updateInactiveObjects(deltaSeconds);
    }

    public void render(Graphics2D graphics) {
        Graphics2D backgroundGraphics = (Graphics2D) graphics.create();
        try {
            renderBackground(backgroundGraphics);
        } finally {
            backgroundGraphics.dispose();
        }
        
        Graphics2D worldGraphics = (Graphics2D) graphics.create();
        if (camera != null) {
            worldGraphics.translate(-camera.getX() + getScreenShakeOffsetX(), -camera.getY() + getScreenShakeOffsetY());
        }
        
        entityManager.renderWorld(worldGraphics);
        lightingManager.render(worldGraphics, this);
        worldGraphics.dispose();
        
        entityManager.renderUI(graphics);
    }

    private void renderBackground(Graphics2D graphics) {
        MapBackgroundMode mode = backgroundMode == null ? MapBackgroundMode.GRADIENT : backgroundMode;
        Color baseColor = backgroundColor == null ? new Color(32, 36, 48) : backgroundColor;
        switch (mode) {
            case SOLID -> {
                graphics.setColor(baseColor);
                graphics.fillRect(0, 0, width, height);
            }
            case IMAGE -> {
                if (backgroundImage != null) {
                    graphics.drawImage(backgroundImage, 0, 0, width, height, null);
                } else {
                    paintBiomeGradient(graphics, baseColor);
                }
            }
            case GRADIENT -> paintBiomeGradient(graphics, baseColor);
            default -> paintBiomeGradient(graphics, baseColor);
        }
    }

    private void paintBiomeGradient(Graphics2D graphics, Color baseColor) {
        int width = Math.max(1, this.width);
        int height = Math.max(1, this.height);
        Color top = mix(baseColor, Color.BLACK, 0.34);
        Color middle = mix(baseColor, new Color(255, 255, 255), 0.05);
        Color bottom = mix(baseColor, new Color(255, 255, 255), 0.24);
        LinearGradientPaint paint = new LinearGradientPaint(
            0.0f,
            0.0f,
            0.0f,
            (float) height,
            new float[] {0.0f, 0.58f, 1.0f},
            new Color[] {top, middle, bottom}
        );
        graphics.setPaint(paint);
        graphics.fillRect(0, 0, width, height);
    }

    private Color mix(Color base, Color overlay, double ratio) {
        Color safeBase = base == null ? new Color(32, 36, 48) : base;
        Color safeOverlay = overlay == null ? Color.BLACK : overlay;
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        int red = (int) Math.round(safeBase.getRed() * (1.0 - clamped) + safeOverlay.getRed() * clamped);
        int green = (int) Math.round(safeBase.getGreen() * (1.0 - clamped) + safeOverlay.getGreen() * clamped);
        int blue = (int) Math.round(safeBase.getBlue() * (1.0 - clamped) + safeOverlay.getBlue() * clamped);
        int alpha = (int) Math.round(safeBase.getAlpha() * (1.0 - clamped) + safeOverlay.getAlpha() * clamped);
        return new Color(
            Math.max(0, Math.min(255, red)),
            Math.max(0, Math.min(255, green)),
            Math.max(0, Math.min(255, blue)),
            Math.max(0, Math.min(255, alpha))
        );
    }

    private void updateScreenShake(double deltaSeconds) {
        if (screenShakeRemaining <= 0.0) {
            screenShakeMagnitude = 0.0;
            screenShakeDuration = 0.0;
            screenShakeElapsed = 0.0;
            return;
        }
        screenShakeElapsed += deltaSeconds;
        screenShakeRemaining = Math.max(0.0, screenShakeRemaining - deltaSeconds);
        if (screenShakeRemaining <= 0.0) {
            screenShakeMagnitude = 0.0;
            screenShakeDuration = 0.0;
            screenShakeElapsed = 0.0;
        }
    }

    private void updateInactiveObjects(double deltaSeconds) {
        for (GameObject object : entityManager.getObjects()) {
            if (!object.isActive()) {
                object.updateInactive(this, deltaSeconds);
            }
        }
    }
}

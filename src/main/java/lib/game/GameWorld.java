package lib.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Optional;

import lib.manager.EntityManager;
import lib.manager.SoundManager;
import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.PlayerObject;
import lib.object.SceneObject;
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
    private boolean gravityEnabled;
    private int gravityStrength;
    private WinConditionType winCondition = WinConditionType.REACH_GOAL;
    private int targetKills = 0;
    private int targetItems = 0;
    private int kills = 0;
    private int itemsCollected = 0;
    private boolean showGoals = true;
    private String failureReason;

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
        entityManager.updateAll(this, deltaSeconds);
    }

    public void render(Graphics2D graphics) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, width, height);
        
        Graphics2D worldGraphics = (Graphics2D) graphics.create();
        if (camera != null) {
            worldGraphics.translate(-camera.getX(), -camera.getY());
        }
        
        entityManager.renderWorld(worldGraphics);
        lightingManager.render(worldGraphics, this);
        worldGraphics.dispose();
        
        entityManager.renderUI(graphics);
    }
}

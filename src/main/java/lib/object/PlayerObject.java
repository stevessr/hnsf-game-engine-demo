package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

import lib.game.GameWorld;
import lib.physics.MovementResult;

public final class PlayerObject extends ActorObject {
    private static final double DEFAULT_DECELERATION = 0.92;
    private int level;
    private int experience;
    private double velocityX;
    private double velocityY;
    private double throttlePower = 600.0;
    private double deceleration = DEFAULT_DECELERATION;
    private long lastDamageTimeNanos;
    private static final long INVULNERABILITY_DURATION_NANOS = 1_000_000_000L; // 1秒

    public PlayerObject(String name) {
        this(name, 0, 0);
    }

    public PlayerObject(String name, int x, int y) {
        super(GameObjectType.PLAYER, name, x, y, 48, 48, new Color(66, 135, 245), 120, 18, 200);
        this.level = 1;
        this.experience = 0;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.lastDamageTimeNanos = 0L;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }

    public void gainExperience(int amount) {
        if (amount <= 0) {
            return;
        }
        this.experience += amount;
        while (this.experience >= experienceNeededForNextLevel()) {
            this.experience -= experienceNeededForNextLevel();
            this.level += 1;
        }
    }

    public int experienceNeededForNextLevel() {
        return level * 100;
    }

    public void setThrottlePower(int power) {
        this.throttlePower = Math.max(0.0, power);
    }

    public int getThrottlePower() {
        return (int) throttlePower;
    }

    public void setDeceleration(double deceleration) {
        this.deceleration = Math.max(0.0, Math.min(1.0, deceleration));
    }

    public double getDeceleration() {
        return deceleration;
    }

    public int getDecelerationPercent() {
        return (int) Math.round(deceleration * 100);
    }

    public void setFriction(double friction) {
        setDeceleration(friction);
    }

    public double getFriction() {
        return deceleration;
    }

    public void accelerate(double ax, double ay, double deltaSeconds) {
        velocityX += ax * throttlePower * deltaSeconds;
        velocityY += ay * throttlePower * deltaSeconds;
    }

    public void setVelocity(double vx, double vy) {
        this.velocityX = vx;
        this.velocityY = vy;
    }

    public int getVelocityX() {
        return (int) Math.round(velocityX);
    }

    public int getVelocityY() {
        return (int) Math.round(velocityY);
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (world != null && isActive()) {
            checkMonsterCollisions(world);
        }

        // 应用阻尼（减速）
        velocityX *= Math.pow(deceleration, deltaSeconds * 60.0);
        velocityY *= Math.pow(deceleration, deltaSeconds * 60.0);

        // 如果速度极小则直接归零，防止漂移
        if (Math.abs(velocityX) < 1.0) velocityX = 0.0;
        if (Math.abs(velocityY) < 1.0) velocityY = 0.0;

        if (velocityX == 0 && velocityY == 0) {
            return;
        }

        int deltaX = (int) Math.round(velocityX * deltaSeconds);
        int deltaY = (int) Math.round(velocityY * deltaSeconds);

        int nextX = getX() + deltaX;
        int nextY = getY() + deltaY;

        if (world == null) {
            setPosition(nextX, nextY);
            return;
        }

        MovementResult movementResult = world.moveObject(this, nextX, nextY);
        
        // 使用碰撞检测后的实际位置，防止卡墙
        setPosition(movementResult.getResolvedX(), movementResult.getResolvedY());
        
        if (movementResult.isBlockedX()) {
            velocityX = 0.0;
        }
        if (movementResult.isBlockedY()) {
            velocityY = 0.0;
        }
    }

    private void checkMonsterCollisions(GameWorld world) {
        long now = System.nanoTime();
        if (now - lastDamageTimeNanos < INVULNERABILITY_DURATION_NANOS) {
            return;
        }

        for (GameObject other : world.getCollisions(this)) {
            if (other instanceof MonsterObject monster && monster.isActive()) {
                takeDamage(monster.getAttack());
                lastDamageTimeNanos = now;
                
                // 简单的击退效果
                int pushDirectionX = Integer.compare(getX(), monster.getX());
                int pushDirectionY = Integer.compare(getY(), monster.getY());
                moveBy(pushDirectionX * 10, pushDirectionY * 10);
                
                break;
            }
        }
    }

    @Override
    public void render(Graphics2D graphics) {
        long now = System.nanoTime();
        // 受伤闪烁效果
        if (now - lastDamageTimeNanos < INVULNERABILITY_DURATION_NANOS && (now / 100_000_000L) % 2 == 0) {
            return;
        }
        graphics.setColor(getColor());
        graphics.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 16, 16);
        graphics.setColor(Color.WHITE);
        graphics.drawString(getName() + " HP: " + getHealth(), getX(), Math.max(12, getY() - 4));
    }
}
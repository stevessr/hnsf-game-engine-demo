package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Locale;

import lib.game.GameWorld;
import lib.physics.MovementResult;

public final class MonsterObject extends ActorObject {
    private static final int DEFAULT_HEAL_DROP_SIZE = 28;
    private static final Color PROJECTILE_COLOR = new Color(255, 120, 80);

    private int rewardExperience;
    private boolean aggressive;
    private int directionX;
    private int fontSize = 12;
    private boolean killRecorded = false;
    private int healDropAmount = 0;
    private boolean rangedAttacker = false;
    private int shootRange = 360;
    private int projectileSpeed = 320;
    private double shootCooldown = 1.2;
    private double shootCooldownRemaining = 0.0;
    private boolean airborne = false;

    public MonsterObject(String name) {
        this(name, 0, 0, 60);
    }

    public MonsterObject(String name, int x, int y, int rewardExperience) {
        super(GameObjectType.MONSTER, name, x, y, 44, 44, new Color(220, 80, 80), 80, 12, 4);
        this.rewardExperience = Math.max(0, rewardExperience);
        this.aggressive = true;
        this.directionX = 1;
    }

    public int getRewardExperience() {
        return rewardExperience;
    }

    public void setRewardExperience(int rewardExperience) {
        this.rewardExperience = Math.max(0, rewardExperience);
    }

    public boolean isAggressive() {
        return aggressive;
    }

    public void setAggressive(boolean aggressive) {
        this.aggressive = aggressive;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(8, fontSize);
    }

    public boolean canAttack() {
        return isActive() && aggressive && getHealth() > 0 && !isDying();
    }

    public int getHealDropAmount() {
        return healDropAmount;
    }

    public void setHealDropAmount(int healDropAmount) {
        this.healDropAmount = Math.max(0, healDropAmount);
    }

    public boolean isRangedAttacker() {
        return rangedAttacker;
    }

    public void setRangedAttacker(boolean rangedAttacker) {
        this.rangedAttacker = rangedAttacker;
    }

    public int getShootRange() {
        return shootRange;
    }

    public void setShootRange(int shootRange) {
        this.shootRange = Math.max(40, shootRange);
    }

    public int getProjectileSpeed() {
        return projectileSpeed;
    }

    public void setProjectileSpeed(int projectileSpeed) {
        this.projectileSpeed = Math.max(80, projectileSpeed);
    }

    public double getShootCooldown() {
        return shootCooldown;
    }

    public void setShootCooldown(double shootCooldown) {
        this.shootCooldown = Math.max(0.1, shootCooldown);
    }

    public boolean isAirborne() {
        return airborne;
    }

    public void setAirborne(boolean airborne) {
        this.airborne = airborne;
        if (airborne) {
            setVelocityY(0.0);
        }
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (isDying()) {
            if (!killRecorded && world != null) {
                world.recordKill();
                world.findPlayer().ifPresent(p -> p.gainExperience(rewardExperience));
                spawnHealingDrop(world);
                killRecorded = true;
            }
            updateDeathAnimation(deltaSeconds);
            return;
        }

        shootCooldownRemaining = Math.max(0.0, shootCooldownRemaining - deltaSeconds);

        if (airborne) {
            setVelocityY(0.0);
        } else if (world != null && world.isGravityEnabled()) {
            setVelocityY(getVelocityYDouble() + world.getGravityStrength() * deltaSeconds);
        }

        int deltaX = (int) Math.round(getSpeed() * directionX * deltaSeconds);
        if (deltaX == 0 && getSpeed() > 0) {
            deltaX = directionX;
        }
        
        int deltaY = (int) Math.round(getVelocityYDouble() * deltaSeconds);

        int nextX = getX() + deltaX;
        int nextY = getY() + deltaY;

        if (world == null) {
            setPosition(nextX, nextY);
            return;
        }

        MovementResult movementResult = world.moveObject(this, nextX, nextY);
        setPosition(movementResult.getResolvedX(), movementResult.getResolvedY());

        if (movementResult.isBlockedX()) {
            directionX *= -1;
        }
        if (movementResult.isBlockedY()) {
            setVelocityY(0.0);
        }

        if (world != null) {
            tryShootAtPlayer(world);
        }
    }

    @Override
    public void render(Graphics2D graphics) {
        if (isDying()) {
            renderDeathAnimation(graphics, this::renderBase);
            return;
        }
        renderBase(graphics);
        renderInfo(graphics, fontSize);
    }

    private void renderBase(Graphics2D graphics) {
        if (isPlaneLike()) {
            renderAircraft(graphics);
            return;
        }
        graphics.setColor(getColor());
        graphics.fillOval(getX(), getY(), getWidth(), getHeight());
    }

    private void renderAircraft(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            boolean faceLeft = getFacingDirection() < 0;
            if (faceLeft) {
                g2d.translate(getX() + getWidth(), getY());
                g2d.scale(-1.0, 1.0);
                drawAircraftFacingRight(g2d, 0, 0, getWidth(), getHeight());
            } else {
                g2d.translate(getX(), getY());
                drawAircraftFacingRight(g2d, 0, 0, getWidth(), getHeight());
            }
        } finally {
            g2d.dispose();
        }
    }

    private void drawAircraftFacingRight(Graphics2D graphics, int x, int y, int width, int height) {
        int bodyX = x + width / 10;
        int bodyY = y + height / 4;
        int bodyW = Math.max(30, width * 3 / 5);
        int bodyH = Math.max(16, height / 2);
        int midY = y + height / 2;

        Color base = getColor();
        Color body = base == null ? new Color(200, 210, 220) : base;
        Color shadow = body.darker();
        Color highlight = body.brighter();

        graphics.setColor(new Color(0, 0, 0, 60));
        graphics.fillOval(x + width / 12 + 4, y + height / 2 + 5, width * 3 / 4, Math.max(8, height / 3));

        graphics.setColor(body);
        graphics.fillRoundRect(bodyX, bodyY, bodyW, bodyH, bodyH, bodyH);

        Polygon nose = new Polygon();
        nose.addPoint(bodyX + bodyW, midY);
        nose.addPoint(bodyX + bodyW - Math.max(10, width / 8), bodyY);
        nose.addPoint(bodyX + bodyW - Math.max(10, width / 8), bodyY + bodyH);
        graphics.fillPolygon(nose);

        Polygon wing = new Polygon();
        wing.addPoint(x + width / 3, midY);
        wing.addPoint(x + width / 2, y + height / 8);
        wing.addPoint(x + width * 3 / 5, midY);
        wing.addPoint(x + width / 2, y + height * 7 / 8);
        graphics.setColor(shadow);
        graphics.fillPolygon(wing);

        Polygon tail = new Polygon();
        tail.addPoint(bodyX + Math.max(8, bodyW / 10), bodyY);
        tail.addPoint(bodyX + Math.max(8, bodyW / 10) - Math.max(10, width / 12), y + height / 6);
        tail.addPoint(bodyX + Math.max(8, bodyW / 10) + Math.max(4, width / 18), bodyY);
        graphics.fillPolygon(tail);

        graphics.setColor(highlight);
        graphics.fillOval(bodyX + bodyW / 3, bodyY + 2, Math.max(6, bodyW / 5), Math.max(6, bodyH / 2));

        graphics.setColor(new Color(30, 30, 40, 120));
        graphics.drawLine(bodyX + 2, midY, bodyX + bodyW + Math.max(8, width / 8) - 1, midY);
    }

    private boolean isPlaneLike() {
        if (airborne) {
            return true;
        }
        String material = getMaterial();
        if (material != null) {
            String normalizedMaterial = material.toLowerCase(Locale.ROOT);
            if (normalizedMaterial.contains("plane") || normalizedMaterial.contains("aircraft")) {
                return true;
            }
        }
        String name = getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains("plane");
    }

    private int getFacingDirection() {
        return directionX >= 0 ? 1 : -1;
    }

    private void spawnHealingDrop(GameWorld world) {
        if (world == null || healDropAmount <= 0) {
            return;
        }
        int dropX = getX() + Math.max(0, (getWidth() - DEFAULT_HEAL_DROP_SIZE) / 2);
        int dropY = getY() + Math.max(0, (getHeight() - DEFAULT_HEAL_DROP_SIZE) / 2);
        ItemObject drop = new ItemObject(
            getName() + "-heal-drop-" + System.nanoTime(),
            dropX,
            dropY,
            DEFAULT_HEAL_DROP_SIZE,
            DEFAULT_HEAL_DROP_SIZE,
            "health",
            healDropAmount
        );
        world.addObject(drop);
    }

    private void tryShootAtPlayer(GameWorld world) {
        if (world == null || !rangedAttacker || !aggressive || shootCooldownRemaining > 0.0 || isDying()) {
            return;
        }
        PlayerObject player = world.findPlayer().orElse(null);
        if (player == null || !player.isActive() || player.isDying()) {
            return;
        }
        double dx = (player.getX() + player.getWidth() / 2.0) - (getX() + getWidth() / 2.0);
        double dy = (player.getY() + player.getHeight() / 2.0) - (getY() + getHeight() / 2.0);
        double distance = Math.hypot(dx, dy);
        if (distance <= 0.0 || distance > shootRange) {
            return;
        }
        double vx = (dx / distance) * projectileSpeed;
        double vy = (dy / distance) * projectileSpeed;
        ProjectileObject projectile = new ProjectileObject(
            getName() + "-shot-" + System.nanoTime(),
            getX() + getWidth() / 2,
            getY() + getHeight() / 2,
            vx,
            vy,
            getAttack(),
            this
        );
        projectile.setColor(PROJECTILE_COLOR);
        world.addObject(projectile);
        world.getSoundManager().playSound("shoot");
        shootCooldownRemaining = shootCooldown;
    }
}

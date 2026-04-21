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
    private static final double JUMP_VELOCITY = 650.0;
    private static final double JUMP_COOLDOWN_SECONDS = 0.75;
    private static final double DODGE_DURATION_SECONDS = 0.28;
    private static final double DODGE_COOLDOWN_SECONDS = 1.0;
    private static final double DODGE_SPEED_MULTIPLIER = 2.2;
    private static final double DODGE_MIN_SPEED = 160.0;
    private static final double DODGE_TRIGGER_DISTANCE = 220.0;
    private static final double SMART_DETECTION_RANGE = 560.0;
    private static final double RANGED_KEEP_DISTANCE_MIN = 180.0;
    private static final double RANGED_KEEP_DISTANCE_MAX = 360.0;
    private static final double LOW_HEALTH_RETREAT_RATIO = 0.30;
    private static final double JUMP_TARGET_VERTICAL_THRESHOLD = 16.0;
    private static final double STRAFE_INTERVAL_SECONDS = 0.75;
    private static final double STRAFE_SPEED_MULTIPLIER = 0.55;
    private static final double CHASE_SPEED_MULTIPLIER = 1.15;
    private static final double RETREAT_SPEED_MULTIPLIER = 1.35;

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
    private boolean bomber = false;
    private int bombRadius = 72;
    private double jumpCooldownRemaining = 0.0;
    private double dodgeTimeRemaining = 0.0;
    private double dodgeCooldownRemaining = 0.0;
    private int dodgeDirectionX = 0;
    private double strafeTimer = STRAFE_INTERVAL_SECONDS;
    private int strafeDirection = 1;

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

    public boolean isBomber() {
        return bomber;
    }

    public void setBomber(boolean bomber) {
        this.bomber = bomber;
    }

    public int getBombRadius() {
        return bombRadius;
    }

    public void setBombRadius(int bombRadius) {
        this.bombRadius = Math.max(20, bombRadius);
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

        double frameSeconds = Math.max(0.0, deltaSeconds);
        shootCooldownRemaining = Math.max(0.0, shootCooldownRemaining - frameSeconds);
        jumpCooldownRemaining = Math.max(0.0, jumpCooldownRemaining - frameSeconds);
        dodgeCooldownRemaining = Math.max(0.0, dodgeCooldownRemaining - frameSeconds);
        if (dodgeTimeRemaining > 0.0) {
            dodgeTimeRemaining = Math.max(0.0, dodgeTimeRemaining - frameSeconds);
            if (dodgeTimeRemaining <= 0.0) {
                dodgeDirectionX = 0;
            }
        }

        if (world == null) {
            int deltaX = (int) Math.round(getSpeed() * directionX * frameSeconds);
            if (deltaX == 0 && getSpeed() > 0) {
                deltaX = directionX;
            }
            int deltaY = (int) Math.round(getVelocityYDouble() * frameSeconds);
            setPosition(getX() + deltaX, getY() + deltaY);
            return;
        }

        if (airborne) {
            setVelocityY(0.0);
        } else if (world.isGravityEnabled()) {
            setVelocityY(getVelocityYDouble() + world.getGravityStrength() * frameSeconds);
        }

        PlayerObject player = world.findPlayer().orElse(null);
        boolean dodging = maybeStartDodge(world);
        MovementPlan plan = dodging ? MovementPlan.dodge(dodgeDirectionX) : resolveMovementPlan(world, player, frameSeconds);
        if (plan.faceDirectionX != 0) {
            directionX = plan.faceDirectionX;
        }

        if (plan.shouldJump) {
            setVelocityY(-JUMP_VELOCITY);
            jumpCooldownRemaining = JUMP_COOLDOWN_SECONDS;
            world.getSoundManager().playSound("jump");
        }

        int moveDirection = dodgeDirectionX != 0 ? dodgeDirectionX : plan.moveDirectionX;
        double moveSpeed = dodgeDirectionX != 0
            ? Math.max(getSpeed(), DODGE_MIN_SPEED) * DODGE_SPEED_MULTIPLIER
            : getSpeed() * plan.speedMultiplier;
        int deltaX = (int) Math.round(moveSpeed * moveDirection * frameSeconds);
        if (deltaX == 0 && moveSpeed > 0) {
            deltaX = moveDirection;
        }

        int deltaY = (int) Math.round(getVelocityYDouble() * frameSeconds);

        int nextX = getX() + deltaX;
        int nextY = getY() + deltaY;

        MovementResult movementResult = world.moveObject(this, nextX, nextY);
        setPosition(movementResult.getResolvedX(), movementResult.getResolvedY());

        if (movementResult.isBlockedX()) {
            dodgeDirectionX = 0;
            dodgeTimeRemaining = 0.0;
            directionX *= -1;
        }
        if (movementResult.isBlockedY()) {
            setVelocityY(0.0);
        }

        tryAttackAtPlayer(world);
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

    private boolean maybeStartDodge(GameWorld world) {
        if (world == null || dodgeTimeRemaining > 0.0 || dodgeCooldownRemaining > 0.0) {
            return false;
        }

        ProjectileObject threat = findThreateningProjectile(world);
        if (threat == null) {
            return false;
        }

        int dodgeDirection = chooseDodgeDirection(world, threat);
        if (dodgeDirection == 0) {
            return false;
        }

        dodgeDirectionX = dodgeDirection;
        dodgeTimeRemaining = DODGE_DURATION_SECONDS;
        dodgeCooldownRemaining = DODGE_COOLDOWN_SECONDS;
        directionX = dodgeDirection;
        return true;
    }

    private ProjectileObject findThreateningProjectile(GameWorld world) {
        if (world == null) {
            return null;
        }

        double monsterCenterX = getX() + getWidth() / 2.0;
        double monsterCenterY = getY() + getHeight() / 2.0;
        ProjectileObject bestThreat = null;
        double bestTimeToImpact = Double.MAX_VALUE;

        for (GameObject object : world.getObjectsByType(GameObjectType.PROJECTILE)) {
            if (!(object instanceof ProjectileObject projectile) || !projectile.isActive()) {
                continue;
            }
            if (projectile.getShooter() != null && !(projectile.getShooter() instanceof PlayerObject)) {
                continue;
            }

            double projectileCenterX = projectile.getX() + projectile.getWidth() / 2.0;
            double projectileCenterY = projectile.getY() + projectile.getHeight() / 2.0;
            double dx = monsterCenterX - projectileCenterX;
            double dy = monsterCenterY - projectileCenterY;
            double distance = Math.hypot(dx, dy);
            if (distance > DODGE_TRIGGER_DISTANCE) {
                continue;
            }

            double velocityX = projectile.getVelocityX();
            double velocityY = projectile.getVelocityY();
            double speed = Math.hypot(velocityX, velocityY);
            if (speed <= 0.0) {
                continue;
            }

            double closingSpeed = dx * velocityX + dy * velocityY;
            if (closingSpeed <= 0.0) {
                continue;
            }

            double timeToImpact = distance / speed;
            if (timeToImpact >= bestTimeToImpact || timeToImpact > 1.0) {
                continue;
            }

            bestThreat = projectile;
            bestTimeToImpact = timeToImpact;
        }

        return bestThreat;
    }

    private MovementPlan resolveMovementPlan(GameWorld world, PlayerObject player, double frameSeconds) {
        if (world == null) {
            return MovementPlan.patrol(directionX, true);
        }
        if (!aggressive || player == null || !player.isActive() || player.isDying()) {
            return MovementPlan.patrol(directionX, shouldJumpForTerrain(world, directionX, false));
        }

        double monsterCenterX = getX() + getWidth() / 2.0;
        double monsterCenterY = getY() + getHeight() / 2.0;
        double playerCenterX = player.getX() + player.getWidth() / 2.0;
        double playerCenterY = player.getY() + player.getHeight() / 2.0;
        double dx = playerCenterX - monsterCenterX;
        double dy = playerCenterY - monsterCenterY;
        double distance = Math.hypot(dx, dy);
        if (distance > SMART_DETECTION_RANGE) {
            return MovementPlan.patrol(directionX, shouldJumpForTerrain(world, directionX, false));
        }

        int faceDirection = dx >= 0.0 ? 1 : -1;
        boolean lowHealth = getHealth() <= Math.max(1, (int) Math.round(getMaxHealth() * LOW_HEALTH_RETREAT_RATIO));
        boolean playerAbove = dy < -JUMP_TARGET_VERTICAL_THRESHOLD && Math.abs(dx) < 300.0;

        if (rangedAttacker) {
            if (lowHealth && distance < shootRange) {
                int retreatDirection = -faceDirection;
                return new MovementPlan(
                    retreatDirection,
                    RETREAT_SPEED_MULTIPLIER,
                    shouldJumpForTerrain(world, retreatDirection, playerAbove)
                );
            }
            if (distance < RANGED_KEEP_DISTANCE_MIN) {
                int retreatDirection = -faceDirection;
                return new MovementPlan(
                    retreatDirection,
                    RETREAT_SPEED_MULTIPLIER,
                    shouldJumpForTerrain(world, retreatDirection, playerAbove)
                );
            }
            if (distance > RANGED_KEEP_DISTANCE_MAX) {
                return new MovementPlan(
                    faceDirection,
                    CHASE_SPEED_MULTIPLIER,
                    shouldJumpForTerrain(world, faceDirection, playerAbove)
                );
            }

            strafeTimer -= frameSeconds;
            if (strafeTimer <= 0.0) {
                strafeDirection *= -1;
                strafeTimer = STRAFE_INTERVAL_SECONDS;
            }
            int strafeMoveDirection = faceDirection * strafeDirection;
            return new MovementPlan(strafeMoveDirection, STRAFE_SPEED_MULTIPLIER, faceDirection, false);
        }

        if (lowHealth && distance < 260.0) {
            int retreatDirection = -faceDirection;
            return new MovementPlan(
                retreatDirection,
                RETREAT_SPEED_MULTIPLIER,
                shouldJumpForTerrain(world, retreatDirection, playerAbove)
            );
        }

        return new MovementPlan(
            faceDirection,
            CHASE_SPEED_MULTIPLIER,
            shouldJumpForTerrain(world, faceDirection, playerAbove)
        );
    }

    private boolean shouldJumpForTerrain(GameWorld world, int moveDirection, boolean targetAbove) {
        if (world == null || moveDirection == 0 || airborne || !world.isGravityEnabled()
            || jumpCooldownRemaining > 0.0 || getVelocityYDouble() < 0.0 || !isStandingOnGround(world)) {
            return false;
        }
        if (hasObstacleAhead(world, moveDirection) || hasGapAhead(world, moveDirection)) {
            return true;
        }
        return targetAbove;
    }

    private int chooseDodgeDirection(GameWorld world, ProjectileObject threat) {
        if (world == null || threat == null) {
            return 0;
        }

        int monsterCenterX = getX() + getWidth() / 2;
        int threatCenterX = threat.getX() + threat.getWidth() / 2;
        int preferredDirection = threatCenterX <= monsterCenterX ? 1 : -1;

        if (canMoveSafely(world, preferredDirection)) {
            return preferredDirection;
        }

        int alternateDirection = -preferredDirection;
        if (canMoveSafely(world, alternateDirection)) {
            return alternateDirection;
        }

        return 0;
    }

    private boolean isStandingOnGround(GameWorld world) {
        return world != null && world.collidesWithSolid(this, getX(), getY() + 1);
    }

    private boolean hasObstacleAhead(GameWorld world, int direction) {
        if (world == null || direction == 0) {
            return false;
        }

        int probeX = getX() + (direction > 0 ? getWidth() + 4 : -4);
        int footProbeY = getY() + getHeight() - 10;
        int chestProbeY = getY() + Math.max(8, getHeight() / 2);
        return isSolidAt(world, probeX, footProbeY, 6, 8) || isSolidAt(world, probeX, chestProbeY, 6, 8);
    }

    private boolean hasGapAhead(GameWorld world, int direction) {
        if (world == null || direction == 0) {
            return false;
        }

        int probeX = getX() + (direction > 0 ? getWidth() + 4 : -4);
        int supportProbeY = getY() + getHeight() - 4;
        return !isSolidAt(world, probeX, supportProbeY, 6, 8);
    }

    private boolean canMoveSafely(GameWorld world, int direction) {
        if (world == null || direction == 0) {
            return false;
        }

        return !hasObstacleAhead(world, direction) && !hasGapAhead(world, direction);
    }

    private boolean isSolidAt(GameWorld world, int x, int y, int width, int height) {
        if (world == null) {
            return false;
        }

        for (SceneObject solid : world.getSolidObjects()) {
            if (solid != null && rectanglesOverlap(x, y, width, height, solid.getX(), solid.getY(), solid.getWidth(), solid.getHeight())) {
                return true;
            }
        }
        return false;
    }

    private boolean rectanglesOverlap(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
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

    private void tryAttackAtPlayer(GameWorld world) {
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
        if (bomber) {
            dropBomb(world, dx, dy, distance);
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

    private void dropBomb(GameWorld world, double dx, double dy, double distance) {
        int spawnX = getX() + getWidth() / 2 - 4;
        int spawnY = getY() + getHeight() - 2;
        double forwardDrift = Math.max(60.0, projectileSpeed * 0.45);
        double horizontalVelocity = (dx >= 0 ? 1.0 : -1.0) * forwardDrift;
        double verticalVelocity = Math.max(20.0, Math.min(120.0, Math.abs(dy) * 0.08 + distance * 0.02));
        ProjectileObject bomb = ProjectileObject.createBomb(
            getName() + "-bomb-" + System.nanoTime(),
            spawnX,
            spawnY,
            horizontalVelocity,
            verticalVelocity,
            Math.max(8, getAttack()),
            this,
            bombRadius,
            Math.max(8, getAttack() + 6),
            Math.max(0.8, shootCooldown * 1.5)
        );
        bomb.setColor(new Color(40, 42, 48));
        world.addObject(bomb);
        world.getSoundManager().playSound("bomb_drop");
        shootCooldownRemaining = shootCooldown;
    }

    private static final class MovementPlan {
        private final int moveDirectionX;
        private final double speedMultiplier;
        private final int faceDirectionX;
        private final boolean shouldJump;

        private MovementPlan(int moveDirectionX, double speedMultiplier, int faceDirectionX, boolean shouldJump) {
            this.moveDirectionX = moveDirectionX;
            this.speedMultiplier = speedMultiplier;
            this.faceDirectionX = faceDirectionX;
            this.shouldJump = shouldJump;
        }

        private static MovementPlan idle(int faceDirectionX, boolean shouldJump) {
            return new MovementPlan(0, 0.0, faceDirectionX, shouldJump);
        }

        private static MovementPlan patrol(int directionX, boolean shouldJump) {
            return new MovementPlan(directionX, 1.0, directionX, shouldJump);
        }

        private static MovementPlan dodge(int dodgeDirectionX) {
            return new MovementPlan(dodgeDirectionX, 1.0, dodgeDirectionX, false);
        }

        private MovementPlan(int moveDirectionX, double speedMultiplier, boolean shouldJump) {
            this(moveDirectionX, speedMultiplier, moveDirectionX == 0 ? 0 : moveDirectionX, shouldJump);
        }
    }
}

package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Locale;

import lib.game.GameWorld;
import lib.physics.MovementResult;

public final class MonsterObject extends ActorObject {
    private static final int DEFAULT_HEAL_DROP_SIZE = 28;
    private static final Color DEFAULT_MONSTER_COLOR = new Color(220, 80, 80);
    private static final double DEFAULT_REVIVE_DELAY_SECONDS = 6.0;
    private static final Color PROJECTILE_COLOR = new Color(255, 120, 80);
    private static final double BAT_FLIGHT_SPEED_MULTIPLIER = 1.35;
    private static final double BAT_RETREAT_SPEED_MULTIPLIER = 1.55;
    private static final double BAT_VERTICAL_CHASE_MULTIPLIER = 2.2;
    private static final double BAT_WING_FLAP_SPEED = 8.0;
    private static final double BAT_WING_FLAP_AMPLITUDE = 5.0;
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
    private MonsterKind monsterKind;
    private int healDropAmount = 0;
    private boolean rangedAttacker = false;
    private int shootRange = 360;
    private int projectileSpeed = 320;
    private double shootCooldown = 1.2;
    private double shootCooldownRemaining = 0.0;
    private boolean airborne = false;
    private boolean bomber = false;
    private int bombRadius = 72;
    private int gravityPercent = 100;
    private boolean revivable = false;
    private double reviveDelaySeconds = DEFAULT_REVIVE_DELAY_SECONDS;
    private double reviveTimer = 0.0;
    private double jumpCooldownRemaining = 0.0;
    private double dodgeTimeRemaining = 0.0;
    private double dodgeCooldownRemaining = 0.0;
    private int dodgeDirectionX = 0;
    private double strafeTimer = STRAFE_INTERVAL_SECONDS;
    private int strafeDirection = 1;
    private double animationPhase = 0.0;

    public MonsterObject(String name) {
        this(name, 0, 0, 60);
    }

    public MonsterObject(String name, int x, int y, int rewardExperience) {
        super(GameObjectType.MONSTER, name, x, y, 44, 44, DEFAULT_MONSTER_COLOR, 80, 12, 4);
        this.rewardExperience = Math.max(0, rewardExperience);
        this.aggressive = true;
        this.directionX = 1;
        this.monsterKind = MonsterKind.infer(name, null);
        applyMonsterKindDefaults();
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

    public MonsterKind getMonsterKind() {
        return monsterKind;
    }

    public void setMonsterKind(MonsterKind monsterKind) {
        this.monsterKind = monsterKind == null ? MonsterKind.DEFAULT : monsterKind;
        applyMonsterKindDefaults();
    }

    public int getGravityPercent() {
        return gravityPercent;
    }

    public void setGravityPercent(int gravityPercent) {
        this.gravityPercent = Math.max(0, Math.min(200, gravityPercent));
    }

    private void applyMonsterKindDefaults() {
        MonsterKind resolvedKind = monsterKind == null ? MonsterKind.DEFAULT : monsterKind;
        monsterKind = resolvedKind;
        gravityPercent = resolvedKind.getDefaultGravityPercent();
        airborne = resolvedKind.isFlying();
        if (getColor() == null || DEFAULT_MONSTER_COLOR.equals(getColor())) {
            setColor(resolvedKind.getDefaultColor());
        }
    }

    private MonsterKind resolveMonsterKind() {
        return monsterKind == null ? MonsterKind.DEFAULT : monsterKind;
    }

    private double getJumpVelocity() {
        MonsterKind kind = resolveMonsterKind();
        if (kind == MonsterKind.SPIDER) {
            return 560.0;
        }
        if (kind == MonsterKind.SLIME) {
            return 600.0;
        }
        return JUMP_VELOCITY;
    }

    public boolean isRevivable() {
        return revivable;
    }

    public void setRevivable(boolean revivable) {
        this.revivable = revivable;
        if (!revivable) {
            reviveTimer = 0.0;
        }
    }

    public double getReviveDelaySeconds() {
        return reviveDelaySeconds;
    }

    public void setReviveDelaySeconds(double reviveDelaySeconds) {
        this.reviveDelaySeconds = Math.max(0.0, reviveDelaySeconds);
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
        animationPhase += frameSeconds;
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

        PlayerObject player = world.findPlayer().orElse(null);
        MonsterKind resolvedKind = resolveMonsterKind();
        if (resolvedKind == MonsterKind.BAT) {
            updateFlyingBat(world, player, frameSeconds);
            return;
        }
        if (resolvedKind == MonsterKind.GHOST
            || resolvedKind == MonsterKind.GARGOYLE
            || resolvedKind == MonsterKind.DRAGON) {
            updateSpecialAerialMonster(world, player, frameSeconds, resolvedKind);
            return;
        }

        if (airborne) {
            setVelocityY(0.0);
        } else if (world.isGravityEnabled()) {
            double gravityMultiplier = Math.max(0.0, gravityPercent / 100.0);
            setVelocityY(getVelocityYDouble() + world.getGravityStrength() * frameSeconds * gravityMultiplier);
        }

        boolean dodging = maybeStartDodge(world);
        MovementPlan plan = dodging ? MovementPlan.dodge(dodgeDirectionX) : resolveMovementPlan(world, player, frameSeconds);
        if (plan.faceDirectionX != 0) {
            directionX = plan.faceDirectionX;
        }

        if (plan.shouldJump) {
            setVelocityY(-getJumpVelocity());
            jumpCooldownRemaining = JUMP_COOLDOWN_SECONDS;
            world.getSoundManager().playSound("jump");
        }

        int moveDirection = dodgeDirectionX != 0 ? dodgeDirectionX : plan.moveDirectionX;
        double moveSpeed = dodgeDirectionX != 0
            ? Math.max(getSpeed(), DODGE_MIN_SPEED) * DODGE_SPEED_MULTIPLIER * resolvedKind.getMovementSpeedMultiplier()
            : getSpeed() * plan.speedMultiplier * resolvedKind.getMovementSpeedMultiplier();
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
    public void updateInactive(GameWorld world, double deltaSeconds) {
        if (world == null || isActive() || !isDying() || !revivable || deltaSeconds <= 0.0) {
            return;
        }
        if (reviveTimer <= 0.0) {
            reviveTimer = reviveDelaySeconds;
        }
        if (reviveTimer <= 0.0) {
            reviveFromDormantState();
            return;
        }
        reviveTimer = Math.max(0.0, reviveTimer - deltaSeconds);
        if (reviveTimer <= 0.0) {
            reviveFromDormantState();
        }
    }

    private void updateFlyingBat(GameWorld world, PlayerObject player, double frameSeconds) {
        if (world == null) {
            return;
        }

        boolean dodging = maybeStartDodge(world);
        int moveDirection = dodgeDirectionX != 0 ? dodgeDirectionX : directionX;
        double speedMultiplier = BAT_FLIGHT_SPEED_MULTIPLIER;
        double targetY = getY() + Math.sin(animationPhase * 1.8) * 12.0;
        boolean hasTarget = player != null && player.isActive() && !player.isDying() && aggressive;

        if (hasTarget) {
            double monsterCenterX = getX() + getWidth() / 2.0;
            double monsterCenterY = getY() + getHeight() / 2.0;
            double playerCenterX = player.getX() + player.getWidth() / 2.0;
            double playerCenterY = player.getY() + player.getHeight() / 2.0;
            double dx = playerCenterX - monsterCenterX;
            double dy = playerCenterY - monsterCenterY;
            double distance = Math.hypot(dx, dy);

            int faceDirection = dx >= 0.0 ? 1 : -1;
            boolean lowHealth = getHealth() <= Math.max(1, (int) Math.round(getMaxHealth() * LOW_HEALTH_RETREAT_RATIO));
            if (lowHealth || distance < 150.0) {
                moveDirection = -faceDirection;
                speedMultiplier = BAT_RETREAT_SPEED_MULTIPLIER;
            } else if (distance > 240.0) {
                moveDirection = faceDirection;
                speedMultiplier = BAT_FLIGHT_SPEED_MULTIPLIER;
            } else {
                moveDirection = faceDirection * (animationPhase % 0.75 < 0.375 ? 1 : -1);
                speedMultiplier = BAT_FLIGHT_SPEED_MULTIPLIER * 0.85;
            }

            double gravityFactor = Math.max(0.25, gravityPercent / 100.0);
            double hoverOffset = -40.0 - (gravityFactor * 18.0);
            targetY = playerCenterY + hoverOffset + Math.sin(animationPhase * BAT_WING_FLAP_SPEED) * (BAT_WING_FLAP_AMPLITUDE * gravityFactor);
            if (distance < 120.0) {
                targetY -= 16.0;
            }

            if (Math.abs(dy) > 12.0) {
                targetY += Math.signum(dy) * Math.min(28.0, Math.abs(dy) * 0.25);
            }

            directionX = faceDirection;
        }

        if (dodging) {
            moveDirection = dodgeDirectionX;
            speedMultiplier = BAT_RETREAT_SPEED_MULTIPLIER;
        }

        double gravityFactor = Math.max(0.25, gravityPercent / 100.0);
        double currentCenterY = getY() + getHeight() / 2.0;
        double verticalError = targetY - currentCenterY;
        double verticalVelocity = Math.max(-280.0, Math.min(280.0, verticalError * BAT_VERTICAL_CHASE_MULTIPLIER * gravityFactor));
        setVelocityY(verticalVelocity);

        double moveSpeed = Math.max(getSpeed(), 60) * speedMultiplier;
        int deltaX = (int) Math.round(moveSpeed * moveDirection * frameSeconds);
        if (deltaX == 0 && moveSpeed > 0.0) {
            deltaX = moveDirection == 0 ? directionX : moveDirection;
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

    private void updateSpecialAerialMonster(GameWorld world, PlayerObject player, double frameSeconds, MonsterKind kind) {
        if (world == null) {
            return;
        }

        double hoverFrequency = 1.7;
        double hoverAmplitude = 9.0;
        double baseHoverOffset = -42.0;
        double verticalChaseMultiplier = 1.75;
        double retreatSpeedMultiplier = 1.3;
        double maxVerticalVelocity = 240.0;
        double farDistance = 260.0;
        double closeDistance = 130.0;
        double closeLift = 14.0;
        double verticalCorrectionFactor = 0.25;
        double verticalCorrectionCap = 28.0;
        double strafePeriod = 0.8;
        switch (kind) {
            case GHOST -> {
                hoverFrequency = 1.3;
                hoverAmplitude = 7.0;
                baseHoverOffset = -54.0;
                verticalChaseMultiplier = 1.05;
                retreatSpeedMultiplier = 1.08;
                maxVerticalVelocity = 160.0;
                farDistance = 220.0;
                closeDistance = 100.0;
                closeLift = 6.0;
                verticalCorrectionFactor = 0.16;
                verticalCorrectionCap = 18.0;
                strafePeriod = 1.15;
            }
            case GARGOYLE -> {
                hoverFrequency = 1.9;
                hoverAmplitude = 6.0;
                baseHoverOffset = -34.0;
                verticalChaseMultiplier = 1.55;
                retreatSpeedMultiplier = 1.24;
                maxVerticalVelocity = 210.0;
                farDistance = 250.0;
                closeDistance = 118.0;
                closeLift = 10.0;
                verticalCorrectionFactor = 0.22;
                verticalCorrectionCap = 24.0;
                strafePeriod = 0.9;
            }
            case DRAGON -> {
                hoverFrequency = 2.0;
                hoverAmplitude = 12.0;
                baseHoverOffset = -30.0;
                verticalChaseMultiplier = 2.1;
                retreatSpeedMultiplier = 1.42;
                maxVerticalVelocity = 290.0;
                farDistance = 300.0;
                closeDistance = 150.0;
                closeLift = 18.0;
                verticalCorrectionFactor = 0.3;
                verticalCorrectionCap = 32.0;
                strafePeriod = 0.7;
            }
            default -> {
                // no-op
            }
        }

        boolean dodging = maybeStartDodge(world);
        int moveDirection = dodgeDirectionX != 0 ? dodgeDirectionX : directionX;
        double speedMultiplier = kind.getMovementSpeedMultiplier();
        double targetY = getY() + Math.sin(animationPhase * hoverFrequency) * hoverAmplitude;
        boolean hasTarget = player != null && player.isActive() && !player.isDying() && aggressive;

        if (hasTarget) {
            double monsterCenterX = getX() + getWidth() / 2.0;
            double monsterCenterY = getY() + getHeight() / 2.0;
            double playerCenterX = player.getX() + player.getWidth() / 2.0;
            double playerCenterY = player.getY() + player.getHeight() / 2.0;
            double dx = playerCenterX - monsterCenterX;
            double dy = playerCenterY - monsterCenterY;
            double distance = Math.hypot(dx, dy);
            int faceDirection = dx >= 0.0 ? 1 : -1;
            boolean lowHealth = getHealth() <= Math.max(1, (int) Math.round(getMaxHealth() * LOW_HEALTH_RETREAT_RATIO));

            if (lowHealth || distance < closeDistance) {
                moveDirection = -faceDirection;
                speedMultiplier = retreatSpeedMultiplier;
            } else if (distance > farDistance) {
                moveDirection = faceDirection;
                speedMultiplier = kind.getMovementSpeedMultiplier();
            } else {
                moveDirection = faceDirection * (animationPhase % strafePeriod < strafePeriod / 2.0 ? 1 : -1);
                speedMultiplier = kind.getMovementSpeedMultiplier() * 0.88;
            }

            double gravityFactor = Math.max(0.15, gravityPercent / 100.0);
            targetY = playerCenterY + baseHoverOffset
                + Math.sin(animationPhase * hoverFrequency) * (hoverAmplitude * gravityFactor);
            if (distance < closeDistance) {
                targetY -= closeLift;
            }
            if (Math.abs(dy) > 12.0) {
                targetY += Math.signum(dy) * Math.min(
                    verticalCorrectionCap,
                    Math.abs(dy) * verticalCorrectionFactor
                );
            }
            directionX = faceDirection;
        }

        if (dodging) {
            moveDirection = dodgeDirectionX;
            speedMultiplier = retreatSpeedMultiplier;
        }

        double gravityFactor = Math.max(0.15, gravityPercent / 100.0);
        double currentCenterY = getY() + getHeight() / 2.0;
        double verticalError = targetY - currentCenterY;
        double verticalVelocity = Math.max(
            -maxVerticalVelocity,
            Math.min(maxVerticalVelocity, verticalError * verticalChaseMultiplier * gravityFactor)
        );
        setVelocityY(verticalVelocity);

        double moveSpeed = Math.max(getSpeed(), 60) * speedMultiplier;
        int deltaX = (int) Math.round(moveSpeed * moveDirection * frameSeconds);
        if (deltaX == 0 && moveSpeed > 0.0) {
            deltaX = moveDirection == 0 ? directionX : moveDirection;
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
        MonsterKind kind = resolveMonsterKind();
        switch (kind) {
            case BAT -> {
                renderBat(graphics);
                return;
            }
            case SPIDER -> {
                renderSpider(graphics);
                return;
            }
            case GHOST -> {
                renderGhost(graphics);
                return;
            }
            case GARGOYLE -> {
                renderGargoyle(graphics);
                return;
            }
            case DRAGON -> {
                renderDragon(graphics);
                return;
            }
            case SLIME -> {
                renderSlime(graphics);
                return;
            }
            case PLANE -> {
                renderAircraft(graphics);
                return;
            }
            default -> {
                if (isPlaneStyle()) {
                    renderAircraft(graphics);
                    return;
                }
            }
        }
        graphics.setColor(getColor());
        graphics.fillOval(getX(), getY(), getWidth(), getHeight());
    }

    private void renderSpider(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            boolean faceLeft = getFacingDirection() < 0;
            if (faceLeft) {
                g2d.translate(getX() + getWidth(), getY());
                g2d.scale(-1.0, 1.0);
            } else {
                g2d.translate(getX(), getY());
            }

            int width = getWidth();
            int height = getHeight();
            Color base = getColor() == null ? MonsterKind.SPIDER.getDefaultColor() : getColor();
            Color legColor = base.darker().darker();
            Color eyeColor = new Color(210, 70, 70);

            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval(width / 8, height - 6, width * 3 / 4, 6);

            int thoraxW = Math.max(14, width / 3);
            int thoraxH = Math.max(12, height / 3);
            int abdomenW = Math.max(18, width / 2);
            int abdomenH = Math.max(16, height / 2);
            int thoraxX = width / 3;
            int thoraxY = Math.max(4, height / 4);
            int abdomenX = Math.max(2, width / 4);
            int abdomenY = Math.max(6, height / 3);

            g2d.setColor(legColor);
            for (int index = 0; index < 4; index++) {
                int segmentY = thoraxY + 4 + index * 4;
                g2d.drawLine(thoraxX + thoraxW / 2, segmentY, 1, segmentY - 3 + index);
                g2d.drawLine(thoraxX + thoraxW / 2, segmentY, width - 2, segmentY - 3 + index);
            }
            g2d.drawLine(thoraxX + thoraxW / 2, thoraxY + 3, 3, thoraxY - 2);
            g2d.drawLine(thoraxX + thoraxW / 2, thoraxY + 3, width - 4, thoraxY - 2);

            g2d.setColor(base);
            g2d.fillOval(abdomenX, abdomenY, abdomenW, abdomenH);
            g2d.fillOval(thoraxX, thoraxY, thoraxW, thoraxH);

            g2d.setColor(base.brighter());
            g2d.fillOval(abdomenX + abdomenW / 4, abdomenY + abdomenH / 5, abdomenW / 4, abdomenH / 4);

            g2d.setColor(eyeColor);
            g2d.fillOval(thoraxX + thoraxW / 2 - 5, thoraxY + 4, 3, 3);
            g2d.fillOval(thoraxX + thoraxW / 2, thoraxY + 4, 3, 3);
            g2d.fillOval(thoraxX + thoraxW / 2 + 5, thoraxY + 4, 3, 3);
        } finally {
            g2d.dispose();
        }
    }

    private void renderGhost(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            boolean faceLeft = getFacingDirection() < 0;
            if (faceLeft) {
                g2d.translate(getX() + getWidth(), getY());
                g2d.scale(-1.0, 1.0);
            } else {
                g2d.translate(getX(), getY());
            }

            int width = getWidth();
            int height = getHeight();
            Color base = getColor() == null ? MonsterKind.GHOST.getDefaultColor() : getColor();
            Color aura = new Color(base.getRed(), base.getGreen(), base.getBlue(), 70);
            Color body = new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                Math.min(220, Math.max(140, base.getAlpha()))
            );
            Color eyeColor = new Color(255, 255, 255, 220);
            Color pupilColor = new Color(60, 70, 90, 220);

            g2d.setColor(aura);
            g2d.fillOval(width / 10, 2, width * 4 / 5, height - 4);

            g2d.setColor(body);
            g2d.fillRoundRect(width / 5, 2, width * 3 / 5, height * 2 / 3, width / 2, width / 2);
            g2d.fillOval(width / 6, height / 2, width / 4, height / 3);
            g2d.fillOval(width / 2 - width / 8, height / 2 + 2, width / 4, height / 3);
            g2d.fillOval(width * 5 / 6 - width / 4, height / 2, width / 4, height / 3);

            g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 110));
            g2d.fillOval(width / 3, height / 5, width / 3, height / 2);

            g2d.setColor(eyeColor);
            g2d.fillOval(width / 3 - 3, height / 3, 5, 5);
            g2d.fillOval(width * 2 / 3 - 2, height / 3, 5, 5);
            g2d.setColor(pupilColor);
            g2d.fillOval(width / 3 - 1, height / 3 + 2, 2, 2);
            g2d.fillOval(width * 2 / 3, height / 3 + 2, 2, 2);
        } finally {
            g2d.dispose();
        }
    }

    private void renderGargoyle(Graphics2D graphics) {
        renderWingedCreature(graphics, false);
    }

    private void renderDragon(Graphics2D graphics) {
        renderWingedCreature(graphics, true);
    }

    private void renderWingedCreature(Graphics2D graphics, boolean dragonStyle) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            boolean faceLeft = getFacingDirection() < 0;
            if (faceLeft) {
                g2d.translate(getX() + getWidth(), getY());
                g2d.scale(-1.0, 1.0);
            } else {
                g2d.translate(getX(), getY());
            }

            int width = getWidth();
            int height = getHeight();
            Color base = getColor() == null
                ? (dragonStyle ? MonsterKind.DRAGON.getDefaultColor() : MonsterKind.GARGOYLE.getDefaultColor())
                : getColor();
            Color wingColor = base.darker().darker();
            Color highlight = base.brighter();
            Color eyeColor = dragonStyle ? new Color(255, 235, 120) : new Color(255, 96, 96);

            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval(width / 10, height - 7, width * 4 / 5, 8);

            int bodyW = dragonStyle ? Math.max(20, width / 2) : Math.max(16, width / 3);
            int bodyH = dragonStyle ? Math.max(20, height / 2) : Math.max(16, height / 2);
            int bodyX = (width - bodyW) / 2;
            int bodyY = dragonStyle ? Math.max(3, height / 3) : Math.max(4, height / 3);
            int flap = (int) Math.round(Math.sin(animationPhase * (dragonStyle ? 4.2 : 3.4))
                * (dragonStyle ? 7.0 : 4.0));
            int wingTop = Math.max(0, bodyY - 8 + flap);
            int wingBottom = Math.min(height - 1, bodyY + bodyH + 8 - flap / 2);

            Polygon leftWing = new Polygon();
            leftWing.addPoint(bodyX + bodyW / 2, bodyY + bodyH / 2);
            leftWing.addPoint(0, wingTop);
            leftWing.addPoint(Math.max(2, width / 5), Math.max(0, bodyY + bodyH / 3 - flap));
            leftWing.addPoint(0, wingBottom);
            leftWing.addPoint(bodyX + bodyW / 4, bodyY + bodyH / 2 + 2);
            g2d.setColor(wingColor);
            g2d.fillPolygon(leftWing);

            Polygon rightWing = new Polygon();
            rightWing.addPoint(bodyX + bodyW / 2, bodyY + bodyH / 2);
            rightWing.addPoint(width, wingTop);
            rightWing.addPoint(width - Math.max(2, width / 5), Math.max(0, bodyY + bodyH / 3 - flap));
            rightWing.addPoint(width, wingBottom);
            rightWing.addPoint(bodyX + bodyW * 3 / 4, bodyY + bodyH / 2 + 2);
            g2d.fillPolygon(rightWing);

            g2d.setColor(base);
            g2d.fillOval(bodyX, bodyY, bodyW, bodyH);

            if (dragonStyle) {
                Polygon snout = new Polygon();
                snout.addPoint(bodyX + bodyW, bodyY + bodyH / 2);
                snout.addPoint(bodyX + bodyW - Math.max(10, width / 8), bodyY + 2);
                snout.addPoint(bodyX + bodyW - Math.max(10, width / 8), bodyY + bodyH - 2);
                g2d.fillPolygon(snout);

                Polygon tail = new Polygon();
                tail.addPoint(bodyX + 4, bodyY + bodyH - 2);
                tail.addPoint(bodyX - Math.max(12, width / 8), bodyY + bodyH + Math.max(6, height / 6));
                tail.addPoint(bodyX + 1, bodyY + bodyH + 2);
                tail.addPoint(bodyX + 6, bodyY + bodyH - 2);
                g2d.fillPolygon(tail);
            } else {
                Polygon hornLeft = new Polygon();
                hornLeft.addPoint(bodyX + 3, bodyY + 4);
                hornLeft.addPoint(bodyX - 3, Math.max(0, bodyY - 6));
                hornLeft.addPoint(bodyX + 8, bodyY + 5);
                g2d.fillPolygon(hornLeft);

                Polygon hornRight = new Polygon();
                hornRight.addPoint(bodyX + bodyW - 3, bodyY + 4);
                hornRight.addPoint(bodyX + bodyW + 3, Math.max(0, bodyY - 6));
                hornRight.addPoint(bodyX + bodyW - 8, bodyY + 5);
                g2d.fillPolygon(hornRight);
            }

            g2d.setColor(highlight);
            g2d.fillOval(bodyX + bodyW / 4, bodyY + 2, Math.max(6, bodyW / 4), Math.max(6, bodyH / 3));

            g2d.setColor(eyeColor);
            g2d.fillOval(bodyX + bodyW / 4, bodyY + bodyH / 2 - 1, 4, 4);
            g2d.fillOval(bodyX + bodyW / 2, bodyY + bodyH / 2 - 1, 4, 4);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(bodyX + bodyW / 4 + 1, bodyY + bodyH / 2 + 1, 1, 1);
            g2d.fillOval(bodyX + bodyW / 2 + 1, bodyY + bodyH / 2 + 1, 1, 1);
        } finally {
            g2d.dispose();
        }
    }

    private void renderBat(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            boolean faceLeft = getFacingDirection() < 0;
            if (faceLeft) {
                g2d.translate(getX() + getWidth(), getY());
                g2d.scale(-1.0, 1.0);
            } else {
                g2d.translate(getX(), getY());
            }

            int width = getWidth();
            int height = getHeight();
            int bodyW = Math.max(16, width / 3);
            int bodyH = Math.max(16, height / 2);
            int bodyX = (width - bodyW) / 2;
            int bodyY = Math.max(4, (height - bodyH) / 2);
            int flap = (int) Math.round(Math.sin(animationPhase * BAT_WING_FLAP_SPEED) * BAT_WING_FLAP_AMPLITUDE);
            int wingTop = Math.max(0, bodyY - 8 + flap);
            int wingBottom = Math.min(height - 1, bodyY + bodyH + 8 - flap / 2);

            g2d.setColor(new Color(0, 0, 0, 70));
            g2d.fillOval(width / 8, height - 7, width * 3 / 4, 7);

            Color body = getColor();
            Color wingColor = body == null ? MonsterKind.BAT.getDefaultColor().darker() : body.darker();
            Color highlight = body == null ? MonsterKind.BAT.getDefaultColor().brighter() : body.brighter();

            Polygon leftWing = new Polygon();
            leftWing.addPoint(bodyX + bodyW / 2, bodyY + bodyH / 2);
            leftWing.addPoint(0, wingTop);
            leftWing.addPoint(Math.max(2, width / 5), Math.max(0, bodyY + bodyH / 3 - flap));
            leftWing.addPoint(0, wingBottom);
            leftWing.addPoint(bodyX + bodyW / 4, bodyY + bodyH / 2 + 2);
            g2d.setColor(wingColor);
            g2d.fillPolygon(leftWing);

            Polygon rightWing = new Polygon();
            rightWing.addPoint(bodyX + bodyW / 2, bodyY + bodyH / 2);
            rightWing.addPoint(width, wingTop);
            rightWing.addPoint(width - Math.max(2, width / 5), Math.max(0, bodyY + bodyH / 3 - flap));
            rightWing.addPoint(width, wingBottom);
            rightWing.addPoint(bodyX + bodyW * 3 / 4, bodyY + bodyH / 2 + 2);
            g2d.fillPolygon(rightWing);

            g2d.setColor(body == null ? MonsterKind.BAT.getDefaultColor() : body);
            g2d.fillOval(bodyX, bodyY, bodyW, bodyH);

            Polygon leftEar = new Polygon();
            leftEar.addPoint(bodyX + 4, bodyY + 4);
            leftEar.addPoint(bodyX + 2, Math.max(0, bodyY - 6));
            leftEar.addPoint(bodyX + 9, bodyY + 4);
            g2d.fillPolygon(leftEar);

            Polygon rightEar = new Polygon();
            rightEar.addPoint(bodyX + bodyW - 4, bodyY + 4);
            rightEar.addPoint(bodyX + bodyW - 2, Math.max(0, bodyY - 6));
            rightEar.addPoint(bodyX + bodyW - 9, bodyY + 4);
            g2d.fillPolygon(rightEar);

            g2d.setColor(highlight);
            g2d.fillOval(bodyX + bodyW / 2 - 3, bodyY + bodyH / 3 - 2, 6, 6);

            g2d.setColor(Color.WHITE);
            g2d.fillOval(bodyX + 4, bodyY + bodyH / 2 - 1, 3, 3);
            g2d.fillOval(bodyX + bodyW - 7, bodyY + bodyH / 2 - 1, 3, 3);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(bodyX + 5, bodyY + bodyH / 2, 1, 1);
            g2d.fillOval(bodyX + bodyW - 6, bodyY + bodyH / 2, 1, 1);
        } finally {
            g2d.dispose();
        }
    }

    private void renderSlime(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            boolean faceLeft = getFacingDirection() < 0;
            if (faceLeft) {
                g2d.translate(getX() + getWidth(), getY());
                g2d.scale(-1.0, 1.0);
            } else {
                g2d.translate(getX(), getY());
            }

            int width = getWidth();
            int height = getHeight();
            int wobble = (int) Math.round(Math.sin(animationPhase * 5.0) * 2.0);
            int bodyH = Math.max(14, height - 4 - wobble);
            int bodyY = height - bodyH;
            Color body = getColor();
            Color highlight = body == null ? MonsterKind.SLIME.getDefaultColor().brighter() : body.brighter();
            Color shadow = body == null ? MonsterKind.SLIME.getDefaultColor().darker() : body.darker();

            g2d.setColor(new Color(0, 0, 0, 55));
            g2d.fillOval(3, height - 5, width - 6, 6);

            g2d.setColor(body == null ? MonsterKind.SLIME.getDefaultColor() : body);
            g2d.fillRoundRect(2, bodyY, width - 4, bodyH, Math.max(12, width / 2), Math.max(12, width / 2));
            g2d.setColor(highlight);
            g2d.fillOval(width / 4, bodyY + 2, width / 5, Math.max(6, bodyH / 3));
            g2d.setColor(shadow);
            g2d.drawArc(4, bodyY + bodyH / 3, width - 8, Math.max(8, bodyH / 2), 20, 140);

            g2d.setColor(Color.WHITE);
            g2d.fillOval(width / 3 - 4, bodyY + bodyH / 3, 5, 5);
            g2d.fillOval(width * 2 / 3 - 1, bodyY + bodyH / 3, 5, 5);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(width / 3 - 2, bodyY + bodyH / 3 + 2, 2, 2);
            g2d.fillOval(width * 2 / 3 + 1, bodyY + bodyH / 3 + 2, 2, 2);
        } finally {
            g2d.dispose();
        }
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

    private boolean isPlaneStyle() {
        MonsterKind kind = resolveMonsterKind();
        if (kind == MonsterKind.PLANE) {
            return true;
        }
        if (kind != MonsterKind.DEFAULT) {
            return false;
        }
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
        MonsterKind kind = resolveMonsterKind();
        if (kind == MonsterKind.SPIDER) {
            return hasObstacleAhead(world, moveDirection) || hasGapAhead(world, moveDirection) || targetAbove;
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

    private void reviveFromDormantState() {
        revive();
        setHealth(getMaxHealth());
        setVelocityY(0.0);
        shootCooldownRemaining = 0.0;
        jumpCooldownRemaining = 0.0;
        dodgeTimeRemaining = 0.0;
        dodgeCooldownRemaining = 0.0;
        dodgeDirectionX = 0;
        strafeTimer = STRAFE_INTERVAL_SECONDS;
        strafeDirection = 1;
        killRecorded = false;
        reviveTimer = 0.0;
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

package lib.object;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import lib.game.GameWorld;
import lib.physics.MovementResult;

public final class PlayerObject extends ActorObject {
    private static final double DEFAULT_DECELERATION = 0.92;
    private int level;
    private int experience;
    private double velocityX;
    private double throttlePower = 600.0;
    private double deceleration = DEFAULT_DECELERATION;
    private long lastDamageTimeNanos;
    private boolean complementaryColorDamageEnabled;
    private int complementaryColorDamage;
    private int fontSize = 18;
    private double lastShootTime;
    private double shootCooldown = 0.3;
    private double lastDirX = 1.0;
    private double lastDirY = 0.0;
    private ProjectileType projectileType = ProjectileType.STANDARD;
    private double walkingTimer = 0;
    private int lightRadius = 200;
    private double lightOrbTimer = 0;
    private static final double HEAL_EFFECT_DURATION = 0.75;
    private double healEffectTimer = 0.0;
    private double maxStamina = 100.0;
    private double stamina = 100.0;
    private double staminaRecoveryPerSecond = 24.0;
    private double sprintDrainPerSecond = 36.0;
    private double sprintAccelerationMultiplier = 4.0;
    private double sprintBurstMultiplier = 1.35;
    private static final long INVULNERABILITY_DURATION_NANOS = 1_000_000_000L; // 1秒
    private static final String REASON_MONSTER_PREFIX = "被怪物击败：";
    private static final String REASON_COLOR_CONFLICT = "接触到互补色危险方块";

    public PlayerObject(String name) {
        this(name, 0, 0);
    }

    public PlayerObject(String name, int x, int y) {
        super(GameObjectType.PLAYER, name, x, y, 48, 48, new Color(66, 135, 245), 120, 18, 200);
        this.level = 1;
        this.experience = 0;
        this.velocityX = 0.0;
        setVelocityY(0.0);
        this.lastDamageTimeNanos = 0L;
        this.complementaryColorDamageEnabled = true;
        this.complementaryColorDamage = 14;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(8, fontSize);
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

    public double getStamina() {
        return stamina;
    }

    public double getMaxStamina() {
        return maxStamina;
    }

    public double getStaminaRatio() {
        if (maxStamina <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, stamina / maxStamina));
    }

    public int getStaminaPercent() {
        return (int) Math.round(getStaminaRatio() * 100.0);
    }

    public void setMaxStamina(double maxStamina) {
        this.maxStamina = Math.max(1.0, maxStamina);
        this.stamina = Math.min(this.stamina, this.maxStamina);
    }

    public void setStamina(double stamina) {
        this.stamina = Math.max(0.0, Math.min(maxStamina, stamina));
    }

    public double getStaminaRecoveryPerSecond() {
        return staminaRecoveryPerSecond;
    }

    public void setStaminaRecoveryPerSecond(double staminaRecoveryPerSecond) {
        this.staminaRecoveryPerSecond = Math.max(0.0, staminaRecoveryPerSecond);
    }

    public double getSprintDrainPerSecond() {
        return sprintDrainPerSecond;
    }

    public void setSprintDrainPerSecond(double sprintDrainPerSecond) {
        this.sprintDrainPerSecond = Math.max(0.0, sprintDrainPerSecond);
    }

    public double getSprintAccelerationMultiplier() {
        return sprintAccelerationMultiplier;
    }

    public void setSprintAccelerationMultiplier(double sprintAccelerationMultiplier) {
        this.sprintAccelerationMultiplier = Math.max(1.0, sprintAccelerationMultiplier);
    }

    public double getSprintBurstMultiplier() {
        return sprintBurstMultiplier;
    }

    public void setSprintBurstMultiplier(double sprintBurstMultiplier) {
        this.sprintBurstMultiplier = Math.max(1.0, sprintBurstMultiplier);
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
        applyAcceleration(ax, ay, deltaSeconds, 1.0);
    }

    public boolean sprintAccelerate(double ax, double ay, double deltaSeconds) {
        return sprintAccelerate(ax, ay, deltaSeconds, false);
    }

    public boolean sprintAccelerate(double ax, double ay, double deltaSeconds, boolean burstBoost) {
        if (deltaSeconds <= 0.0 || (ax == 0.0 && ay == 0.0) || stamina <= 0.0) {
            return false;
        }
        double staminaCost = sprintDrainPerSecond * deltaSeconds * (burstBoost ? sprintBurstMultiplier : 1.0);
        if (staminaCost <= 0.0 || stamina < staminaCost) {
            return false;
        }
        applyAcceleration(ax, ay, deltaSeconds, sprintAccelerationMultiplier * (burstBoost ? sprintBurstMultiplier : 1.0));
        setStamina(stamina - staminaCost);
        return true;
    }

    public void recoverStamina(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || stamina >= maxStamina) {
            return;
        }
        setStamina(stamina + (staminaRecoveryPerSecond * deltaSeconds));
    }

    private void applyAcceleration(double ax, double ay, double deltaSeconds, double multiplier) {
        if (ax != 0 || ay != 0) {
            lastDirX = ax;
            lastDirY = ay;
        }
        velocityX += ax * throttlePower * multiplier * deltaSeconds;
        setVelocityY(getVelocityYDouble() + ay * throttlePower * multiplier * deltaSeconds);
    }

    public void setVelocity(double vx, double vy) {
        this.velocityX = vx;
        setVelocityY(vy);
    }

    public boolean isComplementaryColorDamageEnabled() {
        return complementaryColorDamageEnabled;
    }

    public void setComplementaryColorDamageEnabled(boolean enabled) {
        this.complementaryColorDamageEnabled = enabled;
    }

    public int getComplementaryColorDamage() {
        return complementaryColorDamage;
    }

    public void setComplementaryColorDamage(int damage) {
        this.complementaryColorDamage = Math.max(0, damage);
    }

    public int getVelocityX() {
        return (int) Math.round(velocityX);
    }

    public int getVelocityY() {
        return (int) Math.round(getVelocityYDouble());
    }

    public int getLightRadius() {
        return lightRadius;
    }

    public void setLightRadius(int lightRadius) {
        this.lightRadius = Math.max(50, lightRadius);
    }

    public void addLightOrbEffect(int bonus, double duration) {
        this.lightRadius += bonus;
        this.lightOrbTimer = Math.max(this.lightOrbTimer, duration);
    }

    public ProjectileType getProjectileType() {
        return projectileType;
    }

    public void setProjectileType(ProjectileType projectileType) {
        this.projectileType = projectileType == null ? ProjectileType.STANDARD : projectileType;
    }

    public void cycleProjectileType() {
        this.projectileType = (projectileType == null ? ProjectileType.STANDARD : projectileType).next();
    }

    public void triggerHealEffect() {
        this.healEffectTimer = HEAL_EFFECT_DURATION;
    }

    public boolean isHealEffectActive() {
        return healEffectTimer > 0.0;
    }

    public void respawnAt(int x, int y) {
        revive();
        setPosition(x, y);
        setVelocity(0.0, 0.0);
        setHealth(getMaxHealth());
        setStamina(getMaxStamina());
        walkingTimer = 0.0;
        healEffectTimer = 0.0;
        lastDamageTimeNanos = System.nanoTime();
        lastShootTime = 0.0;
    }

    public void jump(GameWorld world) {
        if (world == null || !world.isGravityEnabled()) {
            return;
        }
        
        boolean onGround = world.collidesWithSolid(this, getX(), getY() + 1);
        
        if (onGround && getVelocityYDouble() >= 0) {
            setVelocityY(-650.0);
            world.getSoundManager().playSound("jump");
        }
    }

    public void shoot(GameWorld world) {
        if (world == null) {
            return;
        }
        shootTowards(world, lastDirX, lastDirY);
    }

    public void shoot(GameWorld world, double targetX, double targetY) {
        if (world == null) {
            return;
        }
        double centerX = getX() + getWidth() / 2.0;
        double centerY = getY() + getHeight() / 2.0;
        double dx = targetX - centerX;
        double dy = targetY - centerY;
        double length = Math.hypot(dx, dy);
        if (length <= 0.0001) {
            dx = lastDirX;
            dy = lastDirY;
            length = Math.hypot(dx, dy);
        }
        if (length <= 0.0001) {
            dx = 1.0;
            dy = 0.0;
            length = 1.0;
        }
        lastDirX = dx / length;
        lastDirY = dy / length;
        shootTowards(world, lastDirX, lastDirY);
    }

    private void shootTowards(GameWorld world, double dirX, double dirY) {
        if (world == null) {
            return;
        }
        double now = System.currentTimeMillis() / 1000.0;
        if (now - lastShootTime < shootCooldown) {
            return;
        }
        
        lastShootTime = now;
        double px = getX() + getWidth() / 2.0;
        double py = getY() + getHeight() / 2.0;
        ProjectileType type = projectileType == null ? ProjectileType.STANDARD : projectileType;
        int damage = type.computeDamage(getAttack());
        double speed = 600.0 * type.getSpeedMultiplier();
        ProjectileObject p = new ProjectileObject("bullet", (int) px, (int) py, dirX * speed, dirY * speed, damage, this, type);
        world.addObject(p);
        world.getSoundManager().playSound("shoot");
    }

    public void cycleColor() {
        float[] hsb = Color.RGBtoHSB(getColor().getRed(), getColor().getGreen(), getColor().getBlue(), null);
        hsb[0] = (hsb[0] + 0.1f) % 1.0f;
        setColor(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (isDying()) {
            updateDeathAnimation(deltaSeconds);
            return;
        }

        if (world != null && isActive()) {
            checkMonsterCollisions(world);
            checkColorConflicts(world);
            if (world.isGravityEnabled()) {
                setVelocityY(getVelocityYDouble() + world.getGravityStrength() * deltaSeconds);
            }
            
            if (lightOrbTimer > 0) {
                lightOrbTimer -= deltaSeconds;
                if (lightOrbTimer <= 0) {
                    lightRadius = 200;
                }
            }
            if (healEffectTimer > 0) {
                healEffectTimer -= deltaSeconds;
                if (healEffectTimer < 0) {
                    healEffectTimer = 0.0;
                }
            }
        }

        if (Math.abs(velocityX) > 10 || Math.abs(getVelocityYDouble()) > 10) {
            walkingTimer += deltaSeconds * 10;
        } else {
            walkingTimer = 0;
        }

        velocityX *= Math.pow(deceleration, deltaSeconds * 60.0);
        double vY = getVelocityYDouble();
        if (world != null && !world.isGravityEnabled()) {
            vY *= Math.pow(deceleration, deltaSeconds * 60.0);
        } else if (world == null) {
            vY *= Math.pow(deceleration, deltaSeconds * 60.0);
        }
        setVelocityY(vY);

        if (Math.abs(velocityX) < 1.0) {
            velocityX = 0.0;
        }
        if (Math.abs(getVelocityYDouble()) < 1.0) {
            setVelocityY(0.0);
        }

        if (velocityX != 0 || getVelocityYDouble() != 0) {
            int deltaX = (int) Math.round(velocityX * deltaSeconds);
            int deltaY = (int) Math.round(getVelocityYDouble() * deltaSeconds);

            int nextX = getX() + deltaX;
            int nextY = getY() + deltaY;

            if (world == null) {
                setPosition(nextX, nextY);
            } else {
                MovementResult movementResult = world.moveObject(this, nextX, nextY);
                setPosition(movementResult.getResolvedX(), movementResult.getResolvedY());

                if (movementResult.isBlockedX()) {
                    velocityX = 0.0;
                }
                if (movementResult.isBlockedY()) {
                    setVelocityY(0.0);
                }

                if (isActive()) {
                    checkColorConflictWithBlocks(world, movementResult);
                }
            }
        }
    }

    private void checkMonsterCollisions(GameWorld world) {
        long now = System.nanoTime();
        if (now - lastDamageTimeNanos < INVULNERABILITY_DURATION_NANOS) {
            return;
        }

        for (GameObject other : world.getCollisions(this)) {
            if (other instanceof MonsterObject monster && monster.isActive()) {
                if (getHealth() <= monster.getAttack()) {
                    world.setFailureReason(REASON_MONSTER_PREFIX + monster.getName());
                }
                takeDamage(world, monster.getAttack());
                lastDamageTimeNanos = now;
                
                int pushDirectionX = Integer.compare(getX(), monster.getX());
                int pushDirectionY = Integer.compare(getY(), monster.getY());
                if (pushDirectionX == 0) {
                    pushDirectionX = 1;
                }
                if (pushDirectionY == 0) {
                    pushDirectionY = -1;
                }
                world.moveObject(this, getX() + (pushDirectionX * 10), getY() + (pushDirectionY * 10));
                
                break;
            }
        }
    }

    private void checkColorConflicts(GameWorld world) {
        if (!complementaryColorDamageEnabled || world == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastDamageTimeNanos < INVULNERABILITY_DURATION_NANOS) {
            return;
        }
        for (GameObject other : world.getCollisions(this)) {
            if (other == null || !other.isActive() || other == this) {
                continue;
            }
            if (isColorDamageSource(other) && isColorConflict(other)) {
                if (getHealth() <= complementaryColorDamage) {
                    world.setFailureReason(REASON_COLOR_CONFLICT);
                }
                takeDamage(world, complementaryColorDamage);
                lastDamageTimeNanos = now;
                return;
            }
        }
    }

    private void checkColorConflictWithBlocks(GameWorld world, MovementResult movementResult) {
        if (!complementaryColorDamageEnabled || world == null || movementResult == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastDamageTimeNanos < INVULNERABILITY_DURATION_NANOS) {
            return;
        }
        GameObject blockedX = movementResult.getBlockedByX();
        GameObject blockedY = movementResult.getBlockedByY();
        if ((isColorDamageSource(blockedX) && isColorConflict(blockedX))
            || (isColorDamageSource(blockedY) && isColorConflict(blockedY))) {
            if (getHealth() <= complementaryColorDamage) {
                world.setFailureReason(REASON_COLOR_CONFLICT);
            }
            takeDamage(world, complementaryColorDamage);
            lastDamageTimeNanos = now;
        }
    }

    private boolean isColorConflict(GameObject other) {
        if (other == null) {
            return false;
        }
        return ColorUtils.isComplementary(getColor(), other.getColor());
    }

    private boolean isColorDamageSource(GameObject other) {
        if (!(other instanceof SceneObject scene)) {
            return false;
        }
        if (!scene.isActive() || !scene.isSolid()) {
            return false;
        }
        if (scene.getType() == GameObjectType.BOUNDARY) {
            return false;
        }
        if (scene.getType() == GameObjectType.WALL || scene.getType() == GameObjectType.VOXEL) {
            return true;
        }
        return scene.isDestructible() || scene.isCollapseWhenUnsupported() || scene.getBreakAfterSteps() > 0;
    }

    @Override
    public void render(Graphics2D graphics) {
        if (isDying()) {
            renderDeathAnimation(graphics, this::renderBase);
            renderDeathEffects(graphics);
            return;
        }
        renderBase(graphics);
        renderHealEffects(graphics);
        renderInfo(graphics, fontSize);
    }

    private void renderBase(Graphics2D graphics) {
        long now = System.nanoTime();
        if (now - lastDamageTimeNanos < INVULNERABILITY_DURATION_NANOS && (now / 100_000_000L) % 2 == 0) {
            return;
        }

        int x = getX();
        int y = getY();
        int w = getWidth();

        Color skinColor = new Color(255, 224, 189);
        Color shirtColor = getColor();
        Color pantColor = new Color(50, 50, 50);

        boolean isBack = lastDirY < 0;
        boolean isSide = Math.abs(lastDirX) > Math.abs(lastDirY);
        boolean isLeft = lastDirX < 0;

        int legSwing = (int) (Math.sin(walkingTimer) * 8);
        int armOffset = (int) (Math.cos(walkingTimer) * 6);
        int legBaseY = y + 32;
        int legHeight = 16;

        graphics.setColor(pantColor);
        if (isSide) {
            graphics.fillRect(x + 12, legBaseY + legSwing, 12, legHeight - legSwing);
            graphics.fillRect(x + 24, legBaseY - legSwing, 12, legHeight + legSwing);
        } else {
            graphics.fillRect(x + 6, legBaseY + legSwing, 15, legHeight - legSwing);
            graphics.fillRect(x + 27, legBaseY - legSwing, 15, legHeight + legSwing);
        }

        graphics.setColor(shirtColor);
        graphics.fillRoundRect(x, y + 16, w, 22, 8, 8);

        graphics.setColor(skinColor);
        if (isSide) {
            int armX = isLeft ? x : x + w - 8;
            graphics.fillRect(armX, y + 20, 8, 16 + armOffset);
        } else {
            graphics.fillRect(x - 6, y + 20, 8, 16 + armOffset);
            graphics.fillRect(x + w - 2, y + 20, 8, 16 - armOffset);
        }

        graphics.setColor(skinColor);
        graphics.fillOval(x + 8, y, 32, 32);

        graphics.setColor(Color.BLACK);
        if (!isBack) {
            if (isSide) {
                int eyeX = isLeft ? x + 12 : x + 32;
                graphics.fillRect(eyeX, y + 12, 6, 6);
            } else {
                graphics.fillRect(x + 16, y + 12, 6, 6);
                graphics.fillRect(x + 26, y + 12, 6, 6);
            }
        } else {
            graphics.setColor(new Color(60, 30, 20));
            graphics.fillArc(x + 8, y, 32, 32, 0, 180);
        }
    }

    private void renderHealEffects(Graphics2D graphics) {
        if (healEffectTimer <= 0.0) {
            return;
        }

        double remainingRatio = Math.max(0.0, Math.min(1.0, healEffectTimer / HEAL_EFFECT_DURATION));
        double progress = 1.0 - remainingRatio;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int expansion = 4 + (int) Math.round(progress * 12.0);
        float alpha = (float) (0.55 * remainingRatio + 0.15);

        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(new Color(112, 255, 156));
            g2d.fillOval(x - expansion, y - expansion, w + expansion * 2, h + expansion * 2);

            g2d.setStroke(new BasicStroke(3f));
            g2d.setColor(new Color(220, 255, 228));
            int ringExpansion = expansion + 4;
            g2d.drawOval(x - ringExpansion, y - ringExpansion, w + ringExpansion * 2, h + ringExpansion * 2);

            int cx = x + (w / 2);
            int sparkleSize = 6 + (int) Math.round(progress * 4.0);
            g2d.setColor(new Color(245, 255, 248));
            g2d.fillRect(cx - 1, y - 18 - sparkleSize, 2, 10 + sparkleSize);
            g2d.fillRect(cx - 5 - sparkleSize / 2, y - 14 - sparkleSize / 2, 10 + sparkleSize, 2);
            g2d.fillRect(x + 6, y - 8, 2, 2);
            g2d.fillRect(x + w - 8, y - 10, 2, 2);
        } finally {
            g2d.dispose();
        }
    }

    private void renderDeathEffects(Graphics2D graphics) {
        double progress = getDeathAnimationProgress();
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            int cx = getX() + getWidth() / 2;
            int cy = getY() + getHeight() / 2;

            g2d.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                (float) Math.max(0.0, 0.8 - progress * 0.6)
            ));
            g2d.setStroke(new BasicStroke(3f));
            g2d.setColor(new Color(255, 80, 80));
            int radius = 16 + (int) Math.round(progress * 34);
            g2d.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g2d.setColor(new Color(255, 200, 120, 160));
            int innerRadius = 10 + (int) Math.round(progress * 20);
            g2d.drawOval(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2);

            g2d.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                (float) Math.max(0.0, 1.0 - progress)
            ));
            g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, Math.max(14f, fontSize)));
            g2d.setColor(new Color(255, 235, 235));
            g2d.drawString("DEFEATED", getX() - 8, getY() - 12);
        } finally {
            g2d.dispose();
        }
    }
}

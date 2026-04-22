package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import lib.game.GameWorld;

/**
 * 投影物对象，如子弹、箭等。
 */
public final class ProjectileObject extends BaseObject {
    private final GameObject shooter;
    private double velocityX;
    private double velocityY;
    private final int damage;
    private final boolean gravityAffected;
    private final boolean explosive;
    private final int explosionRadius;
    private final int explosionDamage;
    private final double fuseSeconds;
    private final double explosionDuration;
    private double lifetime = 3.0; // 存活 3 秒
    private double age = 0.0;
    private boolean exploding = false;
    private double explosionTimer = 0.0;
    private boolean explosionApplied = false;

    public ProjectileObject(String name, int x, int y, double vx, double vy, int damage, GameObject shooter) {
        this(name, x, y, vx, vy, damage, shooter, false, false, 0, damage, 0.0, 0.45, 3.0);
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public GameObject getShooter() {
        return shooter;
    }

    public static ProjectileObject createBomb(
        String name,
        int x,
        int y,
        double vx,
        double vy,
        int damage,
        GameObject shooter,
        int explosionRadius,
        int explosionDamage,
        double fuseSeconds
    ) {
        return new ProjectileObject(
            name,
            x,
            y,
            vx,
            vy,
            damage,
            shooter,
            true,
            true,
            Math.max(1, explosionRadius),
            Math.max(1, explosionDamage),
            Math.max(0.1, fuseSeconds),
            0.75,
            Math.max(3.0, fuseSeconds + 1.0)
        );
    }

    private ProjectileObject(
        String name,
        int x,
        int y,
        double vx,
        double vy,
        int damage,
        GameObject shooter,
        boolean gravityAffected,
        boolean explosive,
        int explosionRadius,
        int explosionDamage,
        double fuseSeconds,
        double explosionDuration,
        double lifetime
    ) {
        super(GameObjectType.PROJECTILE, name, x, y, 8, 8, Color.YELLOW, true);
        this.velocityX = vx;
        this.velocityY = vy;
        this.damage = damage;
        this.shooter = shooter;
        this.gravityAffected = gravityAffected;
        this.explosive = explosive;
        this.explosionRadius = explosionRadius;
        this.explosionDamage = explosionDamage;
        this.fuseSeconds = fuseSeconds;
        this.explosionDuration = explosionDuration;
        this.lifetime = lifetime;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (!isActive() || deltaSeconds <= 0) {
            return;
        }

        age += deltaSeconds;
        if (exploding) {
            explosionTimer += deltaSeconds;
            if (explosionTimer >= explosionDuration) {
                setActive(false);
            }
            return;
        }

        lifetime -= deltaSeconds;
        if (lifetime <= 0) {
            if (explosive) {
                triggerExplosion(world);
            } else {
                setActive(false);
            }
            return;
        }

        if (gravityAffected) {
            double gravity = world != null && world.isGravityEnabled() ? world.getGravityStrength() : 900.0;
            velocityY += gravity * deltaSeconds;
        }

        int nextX = (int) Math.round(getX() + velocityX * deltaSeconds);
        int nextY = (int) Math.round(getY() + velocityY * deltaSeconds);

        setPosition(nextX, nextY);

        if (world == null) {
            return;
        }

        for (GameObject other : world.getCollisions(this)) {
            if (other == shooter || other == this || !other.isActive()) {
                continue;
            }

            if (explosive) {
                if (other instanceof ActorObject actor) {
                    if (isFriendlyFire(actor)) {
                        continue;
                    }
                    triggerExplosion(world);
                    return;
                }
                if (other instanceof SceneObject scene) {
                    if (scene.isDestructible() || scene.isSolid()) {
                        triggerExplosion(world);
                        return;
                    }
                } else if (isCollisionBarrier(other)) {
                    triggerExplosion(world);
                    return;
                }
                continue;
            }

            if (other instanceof ActorObject actor) {
                if (isFriendlyFire(actor)) {
                    continue;
                }
                if (other instanceof PlayerObject player && player.getHealth() <= damage) {
                    world.setFailureReason(resolvePlayerHitReason());
                }
                actor.takeDamage(world, damage);
                setActive(false);
                return;
            } else if (other instanceof SceneObject scene) {
                if (scene.isDestructible()) {
                    scene.applyStructuralDamage(world, damage);
                    setActive(false);
                    return;
                }
                if (scene.isSolid()) {
                    setActive(false);
                    return;
                }
            } else if (isCollisionBarrier(other)) {
                setActive(false);
                return;
            }
        }

        if (explosive && age >= fuseSeconds) {
            triggerExplosion(world);
        }
    }

    @Override
    public void render(Graphics2D graphics) {
        if (exploding) {
            renderExplosion(graphics);
            return;
        }
        if (explosive) {
            renderBomb(graphics);
            return;
        }
        graphics.setColor(getColor());
        graphics.fillOval(getX(), getY(), getWidth(), getHeight());
    }

    private boolean isFriendlyFire(ActorObject actor) {
        if (shooter == null || actor == null) {
            return false;
        }
        return (shooter instanceof MonsterObject && actor instanceof MonsterObject)
            || (shooter instanceof PlayerObject && actor instanceof PlayerObject);
    }

    private boolean isCollisionBarrier(GameObject other) {
        if (other == null) {
            return false;
        }
        return other.getType() == GameObjectType.VOXEL
            || other.getType() == GameObjectType.WALL
            || other.getType() == GameObjectType.BOUNDARY;
    }

    private void triggerExplosion(GameWorld world) {
        if (!explosive || exploding) {
            return;
        }
        exploding = true;
        explosionTimer = 0.0;
        velocityX = 0.0;
        velocityY = 0.0;
        if (world != null) {
            if (!explosionApplied) {
                applyExplosionDamage(world);
                explosionApplied = true;
            }
            world.triggerScreenShake(
                Math.min(16.0, Math.max(6.0, explosionRadius / 9.0)),
                Math.max(0.35, 0.35 + explosionRadius / 350.0)
            );
            world.getSoundManager().playSound("explosion");
        }
    }

    private void applyExplosionDamage(GameWorld world) {
        if (world == null) {
            return;
        }
        int centerX = getX() + getWidth() / 2;
        int centerY = getY() + getHeight() / 2;
        for (GameObject other : List.copyOf(world.getActiveObjects())) {
            if (other == null || other == this || other == shooter || !other.isActive()) {
                continue;
            }
            int otherCenterX = other.getX() + other.getWidth() / 2;
            int otherCenterY = other.getY() + other.getHeight() / 2;
            double distance = Math.hypot(otherCenterX - centerX, otherCenterY - centerY);
            if (distance > explosionRadius) {
                continue;
            }
            if (other instanceof ActorObject actor) {
                if (isFriendlyFire(actor)) {
                    continue;
                }
                if (actor instanceof PlayerObject player && player.getHealth() <= explosionDamage) {
                    world.setFailureReason(resolveExplosionPlayerHitReason());
                }
                actor.takeDamage(world, explosionDamage);
            } else if (other instanceof SceneObject scene && scene.isDestructible()) {
                scene.applyStructuralDamage(world, explosionDamage);
            }
        }
    }

    private void renderBomb(Graphics2D graphics) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        graphics.setColor(new Color(36, 38, 45));
        graphics.fillOval(x, y, width, height);
        graphics.setColor(new Color(220, 60, 40));
        graphics.fillRect(x + width / 2 - 1, y - 4, 2, 6);
        graphics.setColor(new Color(255, 220, 120));
        int fusePulse = (int) (Math.abs(Math.sin(age * 12.0)) * 3);
        graphics.fillOval(x + width / 2 - 1 - fusePulse / 2, y - 7 - fusePulse / 2, 4 + fusePulse, 4 + fusePulse);
    }

    private void renderExplosion(Graphics2D graphics) {
        int cx = getX() + getWidth() / 2;
        int cy = getY() + getHeight() / 2;
        double progress = Math.min(1.0, explosionTimer / Math.max(0.01, explosionDuration));
        int outerRadius = (int) Math.round(explosionRadius * (0.55 + progress * 0.75));
        int innerRadius = (int) Math.round(outerRadius * 0.55);
        int alpha = (int) Math.max(0, 220 * (1.0 - progress));

        graphics.setColor(new Color(255, 190, 70, alpha));
        graphics.fillOval(cx - outerRadius, cy - outerRadius, outerRadius * 2, outerRadius * 2);
        graphics.setColor(new Color(255, 110, 30, Math.max(0, alpha - 20)));
        graphics.drawOval(cx - outerRadius, cy - outerRadius, outerRadius * 2, outerRadius * 2);
        graphics.setColor(new Color(255, 245, 180, Math.max(0, alpha - 40)));
        graphics.fillOval(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2);

        renderExplosionSmoke(graphics, cx, cy, outerRadius, progress);
    }

    private void renderExplosionSmoke(Graphics2D graphics, int cx, int cy, int outerRadius, double progress) {
        int smokeAlpha = (int) Math.round(150.0 * Math.sin(Math.min(1.0, progress) * Math.PI));
        if (smokeAlpha <= 0) {
            return;
        }

        int rise = (int) Math.round(outerRadius * (0.18 + progress * 0.42));
        int spread = (int) Math.max(8, outerRadius * (0.35 + progress * 0.3));
        int coreRadius = (int) Math.max(10, outerRadius * (0.38 + progress * 0.34));

        drawSmokePuff(graphics, cx, cy - rise / 2, coreRadius, smokeColor(48, 46, 42, smokeAlpha));
        drawSmokePuff(graphics, cx - spread / 2, cy - rise, (int) Math.max(8, coreRadius * 0.82), smokeColor(68, 64, 60, smokeAlpha - 25));
        drawSmokePuff(graphics, cx + spread / 2, cy - rise + 4, (int) Math.max(8, coreRadius * 0.78), smokeColor(58, 54, 50, smokeAlpha - 20));
        drawSmokePuff(graphics, cx - spread / 3, cy + outerRadius / 8 - rise / 4, (int) Math.max(8, coreRadius * 0.65), smokeColor(90, 78, 60, smokeAlpha - 40));
        drawSmokePuff(graphics, cx + spread / 4, cy + outerRadius / 10 - rise / 5, (int) Math.max(8, coreRadius * 0.7), smokeColor(76, 70, 66, smokeAlpha - 35));
    }

    private void drawSmokePuff(Graphics2D graphics, int centerX, int centerY, int radius, Color color) {
        if (radius <= 0 || color == null) {
            return;
        }
        int alpha = Math.max(0, color.getAlpha());
        graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private Color smokeColor(int red, int green, int blue, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        return new Color(red, green, blue, clampedAlpha);
    }

    private String resolvePlayerHitReason() {
        if (shooter instanceof MonsterObject monster) {
            return "被远程怪物射杀：" + monster.getName();
        }
        return "被投射物击中";
    }

    private String resolveExplosionPlayerHitReason() {
        if (shooter instanceof MonsterObject monster) {
            return "被空袭轰炸：" + monster.getName();
        }
        return "被爆炸击中";
    }
}

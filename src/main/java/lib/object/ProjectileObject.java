package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import lib.game.GameWorld;

/**
 * 投影物对象，如子弹、箭等。
 */
public final class ProjectileObject extends BaseObject {
    private final GameObject shooter;
    private final double vx;
    private final double vy;
    private final int damage;
    private double lifetime = 3.0; // 存活 3 秒

    public ProjectileObject(String name, int x, int y, double vx, double vy, int damage, GameObject shooter) {
        super(GameObjectType.PROJECTILE, name, x, y, 8, 8, Color.YELLOW, true);
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.shooter = shooter;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        lifetime -= deltaSeconds;
        if (lifetime <= 0) {
            setActive(false);
            return;
        }

        int nextX = (int)(getX() + vx * deltaSeconds);
        int nextY = (int)(getY() + vy * deltaSeconds);
        
        // 简单的碰撞检测
        setPosition(nextX, nextY);
        
        for (GameObject other : world.getCollisions(this)) {
            if (other == shooter || other == this || !other.isActive()) {
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
            } else if (other.getType() == GameObjectType.VOXEL) {
                setActive(false);
                return;
            }
        }
    }

    @Override
    public void render(Graphics2D graphics) {
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

    private String resolvePlayerHitReason() {
        if (shooter instanceof MonsterObject monster) {
            return "被远程怪物射杀：" + monster.getName();
        }
        return "被投射物击中";
    }
}

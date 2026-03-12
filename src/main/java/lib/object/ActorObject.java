package lib.object;

import java.awt.Color;

public abstract class ActorObject extends BaseObject {
    private int health;
    private int attack;
    private int speed;

    protected ActorObject(GameObjectType type, String name, Color color) {
        this(type, name, 0, 0, 48, 48, color, 100, 10, 5);
    }

    protected ActorObject(
        GameObjectType type,
        String name,
        int x,
        int y,
        int width,
        int height,
        Color color,
        int health,
        int attack,
        int speed
    ) {
        super(type, name, x, y, width, height, color, health > 0);
        this.health = normalizeNonNegative(health);
        this.attack = normalizeNonNegative(attack);
        this.speed = normalizeNonNegative(speed);
    }

    public final int getHealth() {
        return health;
    }

    public final void setHealth(int health) {
        this.health = normalizeNonNegative(health);
        setActive(this.health > 0);
    }

    public final int getAttack() {
        return attack;
    }

    public final void setAttack(int attack) {
        this.attack = normalizeNonNegative(attack);
    }

    public final int getSpeed() {
        return speed;
    }

    public final void setSpeed(int speed) {
        this.speed = normalizeNonNegative(speed);
    }

    public final void takeDamage(int damage) {
        if (damage <= 0) {
            return;
        }
        setHealth(health - damage);
    }

    public final void heal(int amount) {
        if (amount <= 0) {
            return;
        }
        setHealth(health + amount);
    }

    private static int normalizeNonNegative(int value) {
        return Math.max(0, value);
    }
}
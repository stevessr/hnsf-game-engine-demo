package lib.object;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public abstract class ActorObject extends BaseObject {
    private int health;
    private int attack;
    private int speed;
    private double velocityY;
    private boolean dying;
    private double deathAnimationTime;
    private static final double DEATH_DURATION = 0.5;

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
        this.velocityY = 0.0;
        this.dying = false;
        this.deathAnimationTime = 0.0;
    }

    public final int getHealth() {
        return health;
    }

    public final void setHealth(int health) {
        if (this.health > 0 && health <= 0 && !dying) {
            startDeathAnimation();
        }
        this.health = normalizeNonNegative(health);
        if (!dying) {
            setActive(this.health > 0);
        }
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

    public final double getVelocityYDouble() {
        return velocityY;
    }

    public final void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public final boolean isDying() {
        return dying;
    }

    protected final void startDeathAnimation() {
        this.dying = true;
        this.deathAnimationTime = 0.0;
    }

    protected final void updateDeathAnimation(double deltaSeconds) {
        if (!dying) {
            return;
        }
        deathAnimationTime += deltaSeconds;
        if (deathAnimationTime >= DEATH_DURATION) {
            setActive(false);
            dying = false;
        }
    }

    protected final void renderDeathAnimation(Graphics2D graphics, Runnable baseRender) {
        double progress = Math.min(1.0, deathAnimationTime / DEATH_DURATION);
        float alpha = (float) (1.0 - progress);
        
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            
            // 旋转并缩小效果
            double centerX = getX() + getWidth() / 2.0;
            double centerY = getY() + getHeight() / 2.0;
            AffineTransform transform = new AffineTransform();
            transform.translate(centerX, centerY);
            transform.rotate(progress * Math.PI * 2);
            transform.scale(1.0 - progress, 1.0 - progress);
            transform.translate(-centerX, -centerY);
            g2d.transform(transform);
            
            baseRender.run();
        } finally {
            g2d.dispose();
        }
    }

    public final void takeDamage(int damage) {
        if (damage <= 0 || dying) {
            return;
        }
        setHealth(health - damage);
    }

    public final void heal(int amount) {
        if (amount <= 0 || dying) {
            return;
        }
        setHealth(health + amount);
    }

    protected final void renderInfo(Graphics2D graphics, int fontSize) {
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(graphics.getFont().deriveFont((float) fontSize));
        
        String text = getName() + " HP: " + health;
        int textX = getX();
        int textY = Math.max(fontSize, getY() - 4);
        
        // 绘制阴影以提高可读性
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.drawString(text, textX + 1, textY + 1);
        
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, textX, textY);
    }

    private static int normalizeNonNegative(int value) {
        return Math.max(0, value);
    }
}

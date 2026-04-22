package lib.object;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.function.Consumer;

import lib.game.GameWorld;

/**
 * 具有生命值、攻击力和死亡动画的实体基类。
 */
public abstract class ActorObject extends BaseObject {
    private int health;
    private int maxHealth;
    private int attack;
    private int speed;
    private boolean dying = false;
    private double deathTimer = 0;
    private static final double DEATH_ANIMATION_DURATION = 0.8;

    public ActorObject(GameObjectType type, String name, int x, int y, int width, int height, Color color, int health, int attack, int speed) {
        super(type, name, x, y, width, height, color, true);
        this.maxHealth = Math.max(1, health);
        this.health = this.maxHealth;
        this.attack = Math.max(0, attack);
        this.speed = Math.max(0, speed);
    }

    public final int getHealth() {
        return health;
    }

    public final void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
        if (this.health == 0 && !dying) {
            startDeathAnimation();
        }
    }

    public final int getMaxHealth() {
        return maxHealth;
    }

    public final void setMaxHealth(int maxHealth) {
        this.maxHealth = Math.max(1, maxHealth);
        this.health = Math.min(health, this.maxHealth);
    }

    public final int getAttack() {
        return attack;
    }

    public final void setAttack(int attack) {
        this.attack = Math.max(0, attack);
    }

    public final int getSpeed() {
        return speed;
    }

    public final void setSpeed(int speed) {
        this.speed = Math.max(0, speed);
    }

    public final boolean isDying() {
        return dying;
    }

    protected final void startDeathAnimation() {
        this.dying = true;
        this.deathTimer = 0;
    }

    protected final void updateDeathAnimation(double deltaSeconds) {
        deathTimer += deltaSeconds;
        if (deathTimer >= DEATH_ANIMATION_DURATION) {
            setActive(false);
            // 保持 dying 为 true 以表示它是死掉的，而不是简单的非活跃
        }
    }

    protected final double getDeathAnimationProgress() {
        if (!dying) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, deathTimer / DEATH_ANIMATION_DURATION));
    }

    protected final void revive() {
        this.dying = false;
        this.deathTimer = 0.0;
        setActive(true);
    }

    protected final void renderDeathAnimation(Graphics2D graphics, Consumer<Graphics2D> baseRender) {
        double progress = getDeathAnimationProgress();
        Graphics2D g2d = (Graphics2D) graphics.create();
        
        int cx = getX() + getWidth() / 2;
        int cy = getY() + getHeight() / 2;
        
        AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
        at.rotate(progress * Math.PI * 4); // 旋转
        at.scale(1.0 - progress, 1.0 - progress); // 缩小
        at.translate(-cx, -cy);
        
        g2d.transform(at);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1.0 - progress)));
        baseRender.accept(g2d);
        g2d.dispose();
    }

    public final void takeDamage(GameWorld world, int amount) {
        if (amount <= 0 || dying) {
            return;
        }
        setHealth(health - amount);
        if (world != null) {
            if (this instanceof PlayerObject) {
                world.getSoundManager().playSound("damage");
            } else {
                world.getSoundManager().playSound("hurt");
            }
        }
    }

    public final void heal(int amount) {
        heal(null, amount);
    }

    public final void heal(GameWorld world, int amount) {
        if (amount <= 0 || dying) {
            return;
        }
        int previousHealth = health;
        setHealth(health + amount);
        int healedAmount = health - previousHealth;
        if (healedAmount <= 0) {
            return;
        }
        if (world != null) {
            world.getSoundManager().playSound("heal");
        }
        if (this instanceof PlayerObject player) {
            player.triggerHealEffect();
        }
    }

    protected final void renderInfo(Graphics2D graphics, int fontSize) {
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int barW = 60;
        int barH = 6;
        boolean showStaminaBar = this instanceof PlayerObject;
        int barX = getX() + (getWidth() - barW) / 2;
        int barY = getY() - (showStaminaBar ? 22 : 12);
        
        // 血条背景
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRoundRect(barX - 1, barY - 1, barW + 2, barH + 2, 4, 4);
        
        // 血条前景
        float healthPercent = (float) health / maxHealth;
        Color healthColor = Color.getHSBColor(healthPercent * 0.33f, 0.9f, 0.9f); // 绿到红
        graphics.setColor(healthColor);
        graphics.fillRoundRect(barX, barY, (int) (barW * healthPercent), barH, 4, 4);

        if (this instanceof PlayerObject player) {
            int staminaBarY = barY + barH + 4;
            float staminaPercent = (float) player.getStaminaRatio();
            Color staminaColor = Color.getHSBColor(0.05f + 0.55f * staminaPercent, 0.9f, 0.95f);
            graphics.setColor(new Color(0, 0, 0, 180));
            graphics.fillRoundRect(barX - 1, staminaBarY - 1, barW + 2, barH + 2, 4, 4);
            graphics.setColor(staminaColor);
            graphics.fillRoundRect(barX, staminaBarY, (int) (barW * staminaPercent), barH, 4, 4);
        }
        
        // 名字文本
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, (float) fontSize - 4));
        FontMetrics metrics = graphics.getFontMetrics();
        int nameW = metrics.stringWidth(getName());
        int nameX = getX() + (getWidth() - nameW) / 2;
        int nameY = barY - 4;
        
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.drawString(getName(), nameX + 1, nameY + 1);
        graphics.setColor(Color.WHITE);
        graphics.drawString(getName(), nameX, nameY);
    }
}

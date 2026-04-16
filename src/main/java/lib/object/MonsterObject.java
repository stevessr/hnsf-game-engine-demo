package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

import lib.game.GameWorld;
import lib.physics.MovementResult;

public final class MonsterObject extends ActorObject {
    private int rewardExperience;
    private boolean aggressive;
    private int directionX;
    private int fontSize = 12;

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

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (isDying()) {
            updateDeathAnimation(deltaSeconds);
            return;
        }

        if (!canAttack()) {
            return;
        }

        if (world != null && world.isGravityEnabled()) {
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
    }

    @Override
    public void render(Graphics2D graphics) {
        if (isDying()) {
            renderDeathAnimation(graphics, () -> renderBase(graphics));
            return;
        }
        renderBase(graphics);
    }

    private void renderBase(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillOval(getX(), getY(), getWidth(), getHeight());
        
        // 渲染文本
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(graphics.getFont().deriveFont((float) fontSize));
        String text = getName();
        int textX = getX();
        int textY = Math.max(fontSize, getY() - 4);
        
        // 绘制阴影
        graphics.setColor(new Color(255, 255, 255, 120));
        graphics.drawString(text, textX + 1, textY + 1);
        
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, textX, textY);
    }
}

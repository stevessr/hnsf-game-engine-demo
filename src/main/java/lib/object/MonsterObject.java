package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

import lib.game.GameWorld;

public final class MonsterObject extends ActorObject {
    private int rewardExperience;
    private boolean aggressive;
    private int directionX;

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

    public boolean canAttack() {
        return isActive() && aggressive && getHealth() > 0;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (!canAttack()) {
            return;
        }
        int deltaX = (int) Math.round(getSpeed() * directionX * deltaSeconds);
        if (deltaX == 0) {
            deltaX = directionX;
        }
        int nextX = getX() + deltaX;
        int maxX = Math.max(0, world.getWidth() - getWidth());
        if (nextX < 0 || nextX > maxX) {
            directionX *= -1;
            nextX = getX() + (int) Math.round(getSpeed() * directionX * deltaSeconds);
        }
        moveWithinWorld(world, nextX, getY());
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillOval(getX(), getY(), getWidth(), getHeight());
        graphics.setColor(Color.BLACK);
        graphics.drawString(getName(), getX(), Math.max(12, getY() - 4));
    }
}
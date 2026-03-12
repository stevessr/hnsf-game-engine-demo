package lib.object;

import java.awt.Color;

public final class MonsterObject extends ActorObject {
    private int rewardExperience;
    private boolean aggressive;

    public MonsterObject(String name) {
        this(name, 0, 0, 60);
    }

    public MonsterObject(String name, int x, int y, int rewardExperience) {
        super(GameObjectType.MONSTER, name, x, y, 44, 44, new Color(220, 80, 80), 80, 12, 4);
        setPosition(x, y);
        this.rewardExperience = Math.max(0, rewardExperience);
        this.aggressive = true;
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
}
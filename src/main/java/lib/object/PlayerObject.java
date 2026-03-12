package lib.object;

import java.awt.Color;

public final class PlayerObject extends ActorObject {
    private int level;
    private int experience;

    public PlayerObject(String name) {
        this(name, 0, 0);
    }

    public PlayerObject(String name, int x, int y) {
        super(GameObjectType.PLAYER, name, x, y, 48, 48, new Color(66, 135, 245), 120, 18, 8);
        this.level = 1;
        this.experience = 0;
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
}
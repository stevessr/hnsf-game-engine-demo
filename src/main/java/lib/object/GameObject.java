package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

import lib.game.GameWorld;

public interface GameObject {
    String getName();

    void setName(String name);

    GameObjectType getType();

    int getX();

    int getY();

    void setPosition(int x, int y);

    int getWidth();

    int getHeight();

    void setSize(int width, int height);

    Color getColor();

    void setColor(Color color);

    boolean isActive();

    void setActive(boolean active);

    void update(GameWorld world, double deltaSeconds);

    /**
     * 更新对象在非活跃状态下的后台逻辑。
     *
     * <p>默认实现为空，只有需要在失活后继续计时或复活的对象才需要覆盖。
     *
     * @param world 游戏世界
     * @param deltaSeconds 时间增量（秒）
     */
    default void updateInactive(GameWorld world, double deltaSeconds) {
    }

    void render(Graphics2D graphics);
}

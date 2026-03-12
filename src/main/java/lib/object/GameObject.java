package lib.object;

import java.awt.Color;

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
}
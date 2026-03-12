package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public final class WallObject extends SceneObject {
    public WallObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.WALL, name, x, y, width, height, true, false);
        setColor(new Color(139, 94, 60));
    }

    @Override
    public void render(Graphics2D graphics) {
        super.render(graphics);
        graphics.setColor(new Color(99, 63, 39));
        graphics.drawLine(getX(), getY() + (getHeight() / 2), getX() + getWidth(), getY() + (getHeight() / 2));
        graphics.drawLine(getX() + (getWidth() / 3), getY(), getX() + (getWidth() / 3), getY() + getHeight());
        graphics.drawLine(getX() + ((getWidth() * 2) / 3), getY(), getX() + ((getWidth() * 2) / 3), getY() + getHeight());
    }
}
package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public final class WallObject extends SceneObject {
    public WallObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.WALL, name, x, y, width, height, new Color(140, 110, 80), true, false);
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillRect(getX(), getY(), getWidth(), getHeight());
        
        // 砖块纹理效果
        graphics.setColor(new Color(0, 0, 0, 60));
        graphics.drawRect(getX(), getY(), getWidth(), getHeight());
        graphics.drawLine(getX(), getY() + (getHeight() / 2), getX() + getWidth(), getY() + (getHeight() / 2));
        graphics.drawLine(getX() + (getWidth() / 3), getY(), getX() + (getWidth() / 3), getY() + (getHeight() / 2));
        graphics.drawLine(getX() + ((getWidth() * 2) / 3), getY() + (getHeight() / 2), getX() + ((getWidth() * 2) / 3), getY() + getHeight());
    }
}

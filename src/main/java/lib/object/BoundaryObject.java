package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public final class BoundaryObject extends SceneObject {
    public BoundaryObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.BOUNDARY, name, x, y, width, height, new Color(40, 40, 40, 150), true, false);
    }

    public static BoundaryObject top(int worldWidth, int thickness) {
        return new BoundaryObject("boundary-top", 0, 0, worldWidth, thickness);
    }

    public static BoundaryObject bottom(int worldWidth, int worldHeight, int thickness) {
        return new BoundaryObject("boundary-bottom", 0, worldHeight - thickness, worldWidth, thickness);
    }

    public static BoundaryObject left(int worldHeight, int thickness) {
        return new BoundaryObject("boundary-left", 0, 0, thickness, worldHeight);
    }

    public static BoundaryObject right(int worldWidth, int worldHeight, int thickness) {
        return new BoundaryObject("boundary-right", worldWidth - thickness, 0, thickness, worldHeight);
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillRect(getX(), getY(), getWidth(), getHeight());
        
        // 警告边框
        graphics.setColor(new Color(255, 100, 0, 180));
        graphics.drawRect(getX(), getY(), getWidth(), getHeight());
        
        // 斜线纹理
        for (int i = 0; i < getWidth() + getHeight(); i += 20) {
            graphics.drawLine(getX() + i, getY(), getX() + i - 20, getY() + getHeight());
        }
    }
}

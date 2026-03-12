package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public final class BoundaryObject extends SceneObject {
    public BoundaryObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.BOUNDARY, name, x, y, width, height, true, false);
        setColor(new Color(82, 88, 112));
    }

    public static BoundaryObject top(int worldWidth, int thickness) {
        return new BoundaryObject("top-boundary", 0, 0, worldWidth, thickness);
    }

    public static BoundaryObject bottom(int worldWidth, int worldHeight, int thickness) {
        return new BoundaryObject(
            "bottom-boundary",
            0,
            Math.max(0, worldHeight - thickness),
            worldWidth,
            thickness
        );
    }

    public static BoundaryObject left(int worldHeight, int thickness) {
        return new BoundaryObject("left-boundary", 0, 0, thickness, worldHeight);
    }

    public static BoundaryObject right(int worldWidth, int worldHeight, int thickness) {
        return new BoundaryObject(
            "right-boundary",
            Math.max(0, worldWidth - thickness),
            0,
            thickness,
            worldHeight
        );
    }

    @Override
    public void render(Graphics2D graphics) {
        super.render(graphics);
        graphics.setColor(new Color(140, 148, 177));
        graphics.drawRect(getX() + 1, getY() + 1, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 2));
    }
}
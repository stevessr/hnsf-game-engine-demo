package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public final class VoxelObject extends SceneObject {
    public VoxelObject(String name, int x, int y, int size) {
        this(name, x, y, size, size, new Color(164, 164, 180));
    }

    public VoxelObject(String name, int x, int y, int size, Color color) {
        this(name, x, y, size, size, color);
    }

    public VoxelObject(String name, int x, int y, int width, int height, Color color) {
        super(GameObjectType.VOXEL, name, x, y, width, height, true, false);
        setColor(color == null ? new Color(164, 164, 180) : color);
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillRect(getX(), getY(), getWidth(), getHeight());
        graphics.setColor(new Color(255, 255, 255, 80));
        graphics.drawLine(getX(), getY(), getX() + getWidth(), getY());
        graphics.drawLine(getX(), getY(), getX(), getY() + getHeight());
        graphics.setColor(new Color(0, 0, 0, 110));
        graphics.drawLine(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight() - 1);
        graphics.drawLine(getX() + getWidth() - 1, getY(), getX() + getWidth() - 1, getY() + getHeight());
    }
}

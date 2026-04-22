package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

import lib.render.SpriteAssets;

public final class WallObject extends SceneObject {
    public WallObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.WALL, name, x, y, width, height, new Color(140, 110, 80), true, false);
    }

    @Override
    public void render(Graphics2D graphics) {
        if (!SpriteAssets.drawWall(graphics, this)) {
            graphics.setColor(getColor());
            graphics.fillRect(getX(), getY(), getWidth(), getHeight());

            graphics.setColor(new Color(0, 0, 0, 60));
            graphics.drawLine(getX(), getY() + (getHeight() / 2), getX() + getWidth(), getY() + (getHeight() / 2));
            graphics.drawLine(
                getX() + (getWidth() / 3),
                getY(),
                getX() + (getWidth() / 3),
                getY() + (getHeight() / 2)
            );
            graphics.drawLine(
                getX() + ((getWidth() * 2) / 3),
                getY() + (getHeight() / 2),
                getX() + ((getWidth() * 2) / 3),
                getY() + getHeight()
            );
        }

        graphics.setColor(new Color(0, 0, 0, 70));
        graphics.drawRect(getX(), getY(), getWidth(), getHeight());
        if (isDestructible()) {
            graphics.setColor(new Color(255, 220, 140, 170));
            graphics.drawLine(getX() + 4, getY() + 4, getX() + getWidth() - 4, getY() + getHeight() - 4);
            graphics.drawLine(getX() + getWidth() - 4, getY() + 4, getX() + 4, getY() + getHeight() - 4);
        }
    }
}

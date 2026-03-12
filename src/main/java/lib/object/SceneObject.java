package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public final class SceneObject extends BaseObject {
    private boolean solid;
    private boolean background;

    public SceneObject(String name) {
        this(name, 0, 0, 128, 128, true, false);
    }

    public SceneObject(String name, int x, int y, int width, int height, boolean solid, boolean background) {
        super(GameObjectType.SCENE, name, x, y, width, height, new Color(120, 180, 120), true);
        this.solid = solid;
        this.background = background;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillRect(getX(), getY(), getWidth(), getHeight());
        if (solid) {
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawRect(getX(), getY(), getWidth(), getHeight());
        }
    }
}
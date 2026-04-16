package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public class SceneObject extends BaseObject {
    private boolean solid;
    private boolean background;

    public SceneObject(String name) {
        this(GameObjectType.SCENE, name, 0, 0, 128, 128, new Color(120, 180, 120), true, false);
    }

    public SceneObject(String name, int x, int y, int width, int height, boolean solid, boolean background) {
        this(GameObjectType.SCENE, name, x, y, width, height, new Color(120, 180, 120), solid, background);
    }

    protected SceneObject(
        GameObjectType type,
        String name,
        int x,
        int y,
        int width,
        int height,
        Color color,
        boolean solid,
        boolean background
    ) {
        super(type, name, x, y, width, height, color, true);
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

package lib.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lib.object.GameObject;
import lib.object.GameObjectType;
import lib.object.PlayerObject;

public final class GameWorld {
    private final List<GameObject> objects;
    private int width;
    private int height;
    private Color backgroundColor;

    public GameWorld(int width, int height) {
        this(width, height, new Color(32, 36, 48));
    }

    public GameWorld(int width, int height, Color backgroundColor) {
        this.objects = new ArrayList<>();
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.backgroundColor = backgroundColor == null ? new Color(32, 36, 48) : backgroundColor;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setSize(int width, int height) {
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        if (backgroundColor == null) {
            return;
        }
        this.backgroundColor = backgroundColor;
    }

    public void addObject(GameObject gameObject) {
        if (gameObject == null) {
            return;
        }
        objects.add(gameObject);
    }

    public List<GameObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public Optional<PlayerObject> findPlayer() {
        return objects.stream()
            .filter(GameObject::isActive)
            .filter(object -> object.getType() == GameObjectType.PLAYER)
            .filter(PlayerObject.class::isInstance)
            .map(PlayerObject.class::cast)
            .findFirst();
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }
        for (GameObject object : objects) {
            if (object.isActive()) {
                object.update(this, deltaSeconds);
            }
        }
    }

    public void render(Graphics2D graphics) {
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, width, height);
        for (GameObject object : objects) {
            if (object.isActive()) {
                object.render(graphics);
            }
        }
    }
}
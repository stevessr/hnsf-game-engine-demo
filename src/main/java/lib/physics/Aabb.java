package lib.physics;

import lib.object.GameObject;

public final class Aabb {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public Aabb(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public static Aabb from(GameObject gameObject) {
        return new Aabb(gameObject.getX(), gameObject.getY(), gameObject.getWidth(), gameObject.getHeight());
    }

    public static Aabb at(GameObject gameObject, int x, int y) {
        return new Aabb(x, y, gameObject.getWidth(), gameObject.getHeight());
    }

    public boolean intersects(Aabb other) {
        if (width == 0 || height == 0 || other.width == 0 || other.height == 0) {
            return false;
        }
        return x < other.x + other.width
            && x + width > other.x
            && y < other.y + other.height
            && y + height > other.y;
    }
}
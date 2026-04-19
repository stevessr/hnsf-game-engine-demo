package lib.editor;

import lib.object.GameObject;

/**
 * 编辑器边界修正工具。
 */
public final class EditorBounds {
    private static final int DEFAULT_MIN_SIZE = 4;

    private EditorBounds() {
    }

    public record Rect(int x, int y, int width, int height) {
    }

    public static Rect normalizeRect(int x, int y, int width, int height, int worldWidth, int worldHeight) {
        return normalizeRect(x, y, width, height, worldWidth, worldHeight, DEFAULT_MIN_SIZE);
    }

    public static Rect normalizeRect(int x, int y, int width, int height, int worldWidth, int worldHeight, int minSize) {
        int min = Math.max(1, minSize);
        int safeWorldWidth = Math.max(min, worldWidth);
        int safeWorldHeight = Math.max(min, worldHeight);

        int normalizedWidth = Math.max(min, width);
        int normalizedHeight = Math.max(min, height);

        int normalizedX = clamp(x, 0, Math.max(0, safeWorldWidth - normalizedWidth));
        int normalizedY = clamp(y, 0, Math.max(0, safeWorldHeight - normalizedHeight));

        normalizedWidth = clamp(normalizedWidth, min, Math.max(min, safeWorldWidth - normalizedX));
        normalizedHeight = clamp(normalizedHeight, min, Math.max(min, safeWorldHeight - normalizedY));

        normalizedX = clamp(normalizedX, 0, Math.max(0, safeWorldWidth - normalizedWidth));
        normalizedY = clamp(normalizedY, 0, Math.max(0, safeWorldHeight - normalizedHeight));
        return new Rect(normalizedX, normalizedY, normalizedWidth, normalizedHeight);
    }

    public static Rect normalizePosition(GameObject object, int targetX, int targetY, int worldWidth, int worldHeight) {
        if (object == null) {
            return normalizeRect(targetX, targetY, DEFAULT_MIN_SIZE, DEFAULT_MIN_SIZE, worldWidth, worldHeight);
        }
        return normalizeRect(targetX, targetY, object.getWidth(), object.getHeight(), worldWidth, worldHeight, 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

package lib.physics;

import java.util.ArrayList;
import java.util.List;

import lib.object.GameObject;

public final class PhysicsEngine {
    public MovementResult resolveMovement(
        GameObject movingObject,
        int targetX,
        int targetY,
        int worldWidth,
        int worldHeight,
        List<? extends GameObject> obstacles
    ) {
        int clampedTargetX = clamp(targetX, 0, Math.max(0, worldWidth - movingObject.getWidth()));
        int clampedTargetY = clamp(targetY, 0, Math.max(0, worldHeight - movingObject.getHeight()));

        int resolvedX = movingObject.getX();
        int resolvedY = movingObject.getY();
        boolean blockedX = clampedTargetX != targetX;
        boolean blockedY = clampedTargetY != targetY;

        while (resolvedX != clampedTargetX) {
            int stepX = Integer.compare(clampedTargetX, resolvedX);
            int candidateX = resolvedX + stepX;
            if (collidesAt(movingObject, candidateX, resolvedY, obstacles)) {
                blockedX = true;
                break;
            }
            resolvedX = candidateX;
        }

        while (resolvedY != clampedTargetY) {
            int stepY = Integer.compare(clampedTargetY, resolvedY);
            int candidateY = resolvedY + stepY;
            if (collidesAt(movingObject, resolvedX, candidateY, obstacles)) {
                blockedY = true;
                break;
            }
            resolvedY = candidateY;
        }

        return new MovementResult(resolvedX, resolvedY, blockedX, blockedY);
    }

    public boolean collidesAt(GameObject movingObject, int targetX, int targetY, List<? extends GameObject> obstacles) {
        Aabb movingBox = Aabb.at(movingObject, targetX, targetY);
        for (GameObject obstacle : obstacles) {
            if (obstacle == movingObject || !obstacle.isActive()) {
                continue;
            }
            if (movingBox.intersects(Aabb.from(obstacle))) {
                return true;
            }
        }
        return false;
    }

    public List<GameObject> findCollisions(GameObject gameObject, List<? extends GameObject> candidates) {
        List<GameObject> collisions = new ArrayList<>();
        Aabb sourceBox = Aabb.from(gameObject);
        for (GameObject candidate : candidates) {
            if (candidate == gameObject || !candidate.isActive()) {
                continue;
            }
            if (sourceBox.intersects(Aabb.from(candidate))) {
                collisions.add(candidate);
            }
        }
        return collisions;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
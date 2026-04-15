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
        int originalX = movingObject.getX();
        int originalY = movingObject.getY();
        int clampedTargetX = clamp(targetX, 0, Math.max(0, worldWidth - movingObject.getWidth()));
        int clampedTargetY = clamp(targetY, 0, Math.max(0, worldHeight - movingObject.getHeight()));

        if (originalX == clampedTargetX && originalY == clampedTargetY) {
            return new MovementResult(originalX, originalY, clampedTargetX != targetX, clampedTargetY != targetY);
        }

        // 优化：计算移动范围内的潜在障碍物
        int minX = Math.min(originalX, clampedTargetX);
        int minY = Math.min(originalY, clampedTargetY);
        int maxX = Math.max(originalX, clampedTargetX) + movingObject.getWidth();
        int maxY = Math.max(originalY, clampedTargetY) + movingObject.getHeight();
        Aabb rangeBox = new Aabb(minX, minY, maxX - minX, maxY - minY);
        
        List<GameObject> nearbyObstacles = new ArrayList<>();
        for (GameObject obs : obstacles) {
            if (obs != movingObject && obs.isActive() && rangeBox.intersects(Aabb.from(obs))) {
                nearbyObstacles.add(obs);
            }
        }

        int resolvedX = originalX;
        int resolvedY = originalY;
        boolean blockedX = clampedTargetX != targetX;
        boolean blockedY = clampedTargetY != targetY;

        // X 轴移动
        if (clampedTargetX != originalX) {
            int stepX = Integer.compare(clampedTargetX, originalX);
            int currentX = originalX;
            while (currentX != clampedTargetX) {
                int nextX = currentX + stepX;
                if (collidesAt(movingObject, nextX, resolvedY, nearbyObstacles)) {
                    blockedX = true;
                    break;
                }
                currentX = nextX;
            }
            resolvedX = currentX;
        }

        // Y 轴移动
        if (clampedTargetY != originalY) {
            int stepY = Integer.compare(clampedTargetY, originalY);
            int currentY = originalY;
            while (currentY != clampedTargetY) {
                int nextY = currentY + stepY;
                if (collidesAt(movingObject, resolvedX, nextY, nearbyObstacles)) {
                    blockedY = true;
                    break;
                }
                currentY = nextY;
            }
            resolvedY = currentY;
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
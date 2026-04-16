package lib.physics;

import lib.object.GameObject;

public final class MovementResult {
    private final int resolvedX;
    private final int resolvedY;
    private final boolean blockedX;
    private final boolean blockedY;
    private final GameObject blockedByX;
    private final GameObject blockedByY;

    public MovementResult(int resolvedX, int resolvedY, boolean blockedX, boolean blockedY) {
        this(resolvedX, resolvedY, blockedX, blockedY, null, null);
    }

    public MovementResult(
        int resolvedX,
        int resolvedY,
        boolean blockedX,
        boolean blockedY,
        GameObject blockedByX,
        GameObject blockedByY
    ) {
        this.resolvedX = resolvedX;
        this.resolvedY = resolvedY;
        this.blockedX = blockedX;
        this.blockedY = blockedY;
        this.blockedByX = blockedByX;
        this.blockedByY = blockedByY;
    }

    public int getResolvedX() {
        return resolvedX;
    }

    public int getResolvedY() {
        return resolvedY;
    }

    public boolean isBlockedX() {
        return blockedX;
    }

    public boolean isBlockedY() {
        return blockedY;
    }

    public GameObject getBlockedByX() {
        return blockedByX;
    }

    public GameObject getBlockedByY() {
        return blockedByY;
    }

    public boolean isBlocked() {
        return blockedX || blockedY;
    }
}

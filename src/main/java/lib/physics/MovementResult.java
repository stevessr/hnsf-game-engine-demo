package lib.physics;

public final class MovementResult {
    private final int resolvedX;
    private final int resolvedY;
    private final boolean blockedX;
    private final boolean blockedY;

    public MovementResult(int resolvedX, int resolvedY, boolean blockedX, boolean blockedY) {
        this.resolvedX = resolvedX;
        this.resolvedY = resolvedY;
        this.blockedX = blockedX;
        this.blockedY = blockedY;
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

    public boolean isBlocked() {
        return blockedX || blockedY;
    }
}
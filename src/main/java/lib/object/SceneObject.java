package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Locale;

import lib.game.GameWorld;
import lib.render.SpriteAssets;

public class SceneObject extends BaseObject {
    private boolean solid;
    private boolean background;
    private boolean destructible;
    private int durability;
    private boolean collapseWhenUnsupported;
    private int collapseDamage;
    private boolean collapsing;
    private double collapseVelocityY;
    private int breakAfterSteps;
    private int stepCount;
    private boolean playerStandingLastFrame;

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
        this.destructible = false;
        this.durability = 100;
        this.collapseWhenUnsupported = false;
        this.collapseDamage = 30;
        this.collapsing = false;
        this.collapseVelocityY = 0.0;
        this.breakAfterSteps = 0;
        this.stepCount = 0;
        this.playerStandingLastFrame = false;
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

    public boolean isDestructible() {
        return destructible;
    }

    public void setDestructible(boolean destructible) {
        this.destructible = destructible;
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = Math.max(1, durability);
    }

    public boolean isCollapseWhenUnsupported() {
        return collapseWhenUnsupported;
    }

    public void setCollapseWhenUnsupported(boolean collapseWhenUnsupported) {
        this.collapseWhenUnsupported = collapseWhenUnsupported;
        if (!collapseWhenUnsupported) {
            this.collapsing = false;
            this.collapseVelocityY = 0.0;
        }
    }

    public int getCollapseDamage() {
        return collapseDamage;
    }

    public void setCollapseDamage(int collapseDamage) {
        this.collapseDamage = Math.max(0, collapseDamage);
    }

    public boolean isCollapsing() {
        return collapsing;
    }

    public int getBreakAfterSteps() {
        return breakAfterSteps;
    }

    public void setBreakAfterSteps(int breakAfterSteps) {
        this.breakAfterSteps = Math.max(0, breakAfterSteps);
        if (this.breakAfterSteps == 0) {
            this.stepCount = 0;
        } else {
            this.stepCount = Math.min(this.stepCount, this.breakAfterSteps);
        }
    }

    public int getStepCount() {
        return stepCount;
    }

    public boolean applyStructuralDamage(GameWorld world, int amount) {
        if (!destructible || amount <= 0 || !isActive()) {
            return false;
        }
        durability = Math.max(0, durability - amount);
        if (durability == 0) {
            playBreakSound(world);
            setActive(false);
            return true;
        }
        return false;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (world == null || !isActive() || deltaSeconds <= 0) {
            return;
        }

        if (isVoidLike()) {
            handleVoid(world);
            return;
        }

        PlayerObject player = world.findPlayer().orElse(null);
        handleStepBreak(world, player);
        if (!isActive()) {
            return;
        }
        handleCollapse(world, deltaSeconds, player);
    }

    @Override
    public void render(Graphics2D graphics) {
        if (isVoidLike()) {
            renderVoid(graphics);
            return;
        }
        if (isCloudLike()) {
            renderCloud(graphics);
            return;
        }
        if (isTreeLike()) {
            renderTree(graphics);
            return;
        }
        boolean rendered = SpriteAssets.drawScene(graphics, this);
        if (!rendered) {
            graphics.setColor(getColor());
            graphics.fillRect(getX(), getY(), getWidth(), getHeight());
        }
        if (solid) {
            graphics.setColor(Color.DARK_GRAY);
            graphics.drawRect(getX(), getY(), getWidth(), getHeight());
        }
        if (destructible) {
            graphics.setColor(new Color(255, 210, 120, 180));
            graphics.drawLine(getX() + 4, getY() + 4, getX() + getWidth() - 4, getY() + getHeight() - 4);
            graphics.drawLine(getX() + getWidth() - 4, getY() + 4, getX() + 4, getY() + getHeight() - 4);
        }
        if (collapseWhenUnsupported) {
            graphics.setColor(new Color(255, 120, 80, 180));
            int midX = getX() + getWidth() / 2;
            graphics.drawLine(midX, getY() + 4, midX, getY() + getHeight() - 8);
            graphics.drawLine(midX, getY() + getHeight() - 8, midX - 5, getY() + getHeight() - 14);
            graphics.drawLine(midX, getY() + getHeight() - 8, midX + 5, getY() + getHeight() - 14);
        }
        if (breakAfterSteps > 0) {
            graphics.setColor(new Color(255, 245, 180, 180));
            graphics.drawString(stepCount + "/" + breakAfterSteps, getX() + 4, getY() + Math.min(14, Math.max(12, getHeight() - 4)));
        }
    }

    private boolean isVoidLike() {
        String material = getMaterial();
        if (material != null && "void".equalsIgnoreCase(material)) {
            return true;
        }
        String name = getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains("void");
    }

    private void renderVoid(Graphics2D graphics) {
        Color base = getColor() == null ? new Color(6, 6, 12, 230) : getColor();
        graphics.setColor(base);
        graphics.fillRect(getX(), getY(), getWidth(), getHeight());
        graphics.setColor(new Color(18, 18, 32, Math.min(255, base.getAlpha() + 18)));
        graphics.drawLine(getX(), getY() + getHeight() / 3, getX() + getWidth(), getY() + getHeight() / 3);
        graphics.drawLine(getX(), getY() + getHeight() * 2 / 3, getX() + getWidth(), getY() + getHeight() * 2 / 3);
    }

    private void handleVoid(GameWorld world) {
        String failureReason = "坠入虚空：" + getName();
        for (GameObject other : List.copyOf(world.getActiveObjects())) {
            if (other == this || other == null || !other.isActive()) {
                continue;
            }
            if (!overlaps(other)) {
                continue;
            }
            if (other instanceof PlayerObject player) {
                if (world.getFailureReason() == null) {
                    world.setFailureReason(failureReason);
                }
                player.takeDamage(world, Math.max(1, player.getHealth()));
                continue;
            }
            if (other instanceof ActorObject actor) {
                actor.takeDamage(world, Math.max(1, actor.getHealth()));
                continue;
            }
            other.setActive(false);
        }
    }

    private boolean isCloudLike() {
        String material = getMaterial();
        if (material != null && "cloud".equalsIgnoreCase(material)) {
            return true;
        }
        String name = getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains("cloud");
    }

    private void renderCloud(Graphics2D graphics) {
        int x = getX();
        int y = getY();
        int width = Math.max(20, getWidth());
        int height = Math.max(12, getHeight());
        Color base = getColor() == null ? new Color(255, 255, 255, 180) : getColor();
        Color shadow = new Color(Math.max(0, base.getRed() - 30), Math.max(0, base.getGreen() - 30), Math.max(0, base.getBlue() - 30), base.getAlpha());

        graphics.setColor(shadow);
        graphics.fillOval(x + width / 10, y + height / 5, width * 2 / 5, height * 3 / 5);
        graphics.fillOval(x + width * 3 / 10, y, width / 2, height);
        graphics.fillOval(x + width / 2, y + height / 6, width * 2 / 5, height * 2 / 3);

        graphics.setColor(base);
        graphics.fillOval(x, y + height / 5, width * 2 / 5, height * 3 / 5);
        graphics.fillOval(x + width / 5, y, width / 2, height);
        graphics.fillOval(x + width / 2, y + height / 5, width * 2 / 5, height * 3 / 5);
    }

    private boolean isTreeLike() {
        String material = getMaterial();
        if (material != null && "tree".equalsIgnoreCase(material)) {
            return true;
        }
        String name = getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains("tree");
    }

    private void renderTree(Graphics2D graphics) {
        int x = getX();
        int y = getY();
        int width = Math.max(12, getWidth());
        int height = Math.max(24, getHeight());

        int canopyWidth = Math.max(36, width * 2);
        int canopyHeight = Math.max(28, Math.max(height / 2, 36));
        int canopyX = x - (canopyWidth - width) / 2;
        int canopyY = y - Math.max(4, canopyHeight / 8);

        graphics.setColor(new Color(36, 84, 44));
        graphics.fillOval(canopyX, canopyY + canopyHeight / 4, canopyWidth, canopyHeight);
        graphics.setColor(new Color(54, 128, 60));
        graphics.fillOval(canopyX + canopyWidth / 8, canopyY, canopyWidth * 3 / 4, canopyHeight);
        graphics.setColor(new Color(76, 164, 76));
        graphics.fillOval(canopyX + canopyWidth / 3, canopyY - canopyHeight / 10, canopyWidth / 3, canopyHeight * 3 / 4);
        graphics.setColor(new Color(102, 190, 92, 220));
        graphics.fillOval(canopyX + canopyWidth / 2 - canopyWidth / 8, canopyY + canopyHeight / 10, canopyWidth / 4, canopyHeight / 3);

        int trunkWidth = Math.max(8, Math.min(width / 2, 16));
        int trunkHeight = Math.max(24, Math.round(height * 0.4f));
        int trunkX = x + (width - trunkWidth) / 2;
        int trunkY = y + height - trunkHeight;

        Color trunkBase = getColor() == null ? new Color(109, 69, 35) : getColor();
        Color trunkShadow = trunkBase.darker();
        graphics.setColor(trunkBase);
        graphics.fillRect(trunkX, trunkY, trunkWidth, trunkHeight);
        graphics.setColor(trunkShadow);
        graphics.fillRect(trunkX + trunkWidth / 3, trunkY, Math.max(1, trunkWidth / 3), trunkHeight);
        graphics.setColor(new Color(0, 0, 0, 60));
        graphics.drawLine(trunkX, trunkY, trunkX, trunkY + trunkHeight - 1);
        graphics.drawLine(trunkX + trunkWidth - 1, trunkY, trunkX + trunkWidth - 1, trunkY + trunkHeight - 1);
    }

    private void handleStepBreak(GameWorld world, PlayerObject player) {
        if (breakAfterSteps <= 0 || player == null || !player.isActive() || player.isDying() || collapsing) {
            playerStandingLastFrame = false;
            return;
        }
        boolean standingOnTop = isPlayerStandingOnTop(player);
        if (standingOnTop && !playerStandingLastFrame) {
            stepCount++;
            if (stepCount >= breakAfterSteps) {
                playBreakSound(world);
                setActive(false);
                playerStandingLastFrame = false;
                return;
            }
        }
        playerStandingLastFrame = standingOnTop;
    }

    private void handleCollapse(GameWorld world, double deltaSeconds, PlayerObject player) {
        if (!collapseWhenUnsupported) {
            return;
        }
        if (!collapsing && !hasSupport(world)) {
            collapsing = true;
            playBreakSound(world);
        }
        if (!collapsing) {
            return;
        }

        collapseVelocityY += world.getGravityStrength() * deltaSeconds;
        int nextY = getY() + (int) Math.round(collapseVelocityY * deltaSeconds);
        var movement = world.moveObject(this, getX(), nextY);
        setPosition(movement.getResolvedX(), movement.getResolvedY());

        if (player != null && player.isActive() && !player.isDying() && overlaps(player)) {
            if (player.getHealth() <= collapseDamage) {
                world.setFailureReason("被倒塌建筑砸中：" + getName());
            }
            player.takeDamage(world, collapseDamage);
        }

        if (movement.isBlockedY()) {
            collapsing = false;
            collapseVelocityY = 0.0;
        }
    }

    private void playBreakSound(GameWorld world) {
        if (world != null) {
            world.getSoundManager().playSound("crash");
        }
    }

    private boolean hasSupport(GameWorld world) {
        int supportTop = getY() + getHeight();
        for (SceneObject other : world.getSolidObjects()) {
            if (other == this || !other.isActive()) {
                continue;
            }
            int otherTop = other.getY();
            boolean directlyBelow = otherTop >= supportTop && otherTop <= supportTop + 2;
            boolean horizontalOverlap = getX() < other.getX() + other.getWidth()
                && getX() + getWidth() > other.getX();
            if (directlyBelow && horizontalOverlap) {
                return true;
            }
        }
        return getY() + getHeight() >= world.getHeight();
    }

    private boolean isPlayerStandingOnTop(PlayerObject player) {
        int playerBottom = player.getY() + player.getHeight();
        boolean horizontalOverlap = player.getX() < getX() + getWidth()
            && player.getX() + player.getWidth() > getX();
        boolean nearTop = playerBottom >= getY() - 4 && playerBottom <= getY() + 6;
        return horizontalOverlap && nearTop && player.getVelocityY() >= 0;
    }

    private boolean overlaps(GameObject other) {
        return other != null
            && getX() < other.getX() + other.getWidth()
            && getX() + getWidth() > other.getX()
            && getY() < other.getY() + other.getHeight()
            && getY() + getHeight() > other.getY();
    }
}

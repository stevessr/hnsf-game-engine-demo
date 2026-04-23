package lib.object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

import lib.game.GameWorld;

/**
 * 触发器对象：玩家进入后，根据配置影响目标对象或世界状态。
 */
public final class TriggerObject extends BaseObject {
    private String targetName;
    private TriggerAction action;
    private boolean triggerOnce;
    private boolean playerInsideLastFrame;

    public TriggerObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.TRIGGER, name, x, y, width, height, new Color(160, 90, 240, 120), true);
        this.targetName = "";
        this.action = TriggerAction.TOGGLE;
        this.triggerOnce = false;
        this.playerInsideLastFrame = false;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName == null ? "" : targetName.trim();
    }

    public TriggerAction getAction() {
        return action;
    }

    public void setAction(TriggerAction action) {
        this.action = action == null ? TriggerAction.TOGGLE : action;
    }

    public boolean isTriggerOnce() {
        return triggerOnce;
    }

    public void setTriggerOnce(boolean triggerOnce) {
        this.triggerOnce = triggerOnce;
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        if (world == null || deltaSeconds <= 0.0) {
            return;
        }
        if (!isActive()) {
            playerInsideLastFrame = false;
            return;
        }
        PlayerObject player = world.findPlayer().orElse(null);
        boolean overlapping = player != null && player.isActive() && !player.isDying() && overlaps(player);
        if (overlapping && !playerInsideLastFrame) {
            fire(world);
            if (triggerOnce) {
                setActive(false);
            }
        }
        playerInsideLastFrame = overlapping;
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color base = getColor() == null ? new Color(160, 90, 240, 120) : getColor();
            Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), isActive() ? 120 : 70);
            Color stroke = new Color(255, 255, 255, isActive() ? 170 : 80);
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            g2d.setColor(fill);
            g2d.fillRoundRect(x, y, w, h, 16, 16);

            Stroke oldStroke = g2d.getStroke();
            g2d.setColor(stroke);
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 4f}, 0f));
            g2d.drawRoundRect(x, y, w, h, 16, 16);
            g2d.setStroke(oldStroke);

            FontMetrics metrics = g2d.getFontMetrics();
            String label = "TRIGGER";
            int labelWidth = metrics.stringWidth(label);
            int labelX = x + Math.max(6, (w - labelWidth) / 2);
            int labelY = y + Math.max(metrics.getAscent() + 4, h / 2 - 2);
            g2d.setColor(new Color(255, 245, 255, 230));
            g2d.drawString(label, labelX, labelY);

            String actionText = action == null ? TriggerAction.TOGGLE.name() : action.toString();
            int actionWidth = metrics.stringWidth(actionText);
            g2d.setColor(new Color(230, 220, 255, 220));
            g2d.drawString(actionText, x + Math.max(6, (w - actionWidth) / 2), y + h - 8);

            if (targetName != null && !targetName.isBlank()) {
                String targetLabel = targetName;
                int targetWidth = metrics.stringWidth(targetLabel);
                if (targetWidth > w - 10) {
                    targetLabel = clipText(metrics, targetLabel, w - 16);
                    targetWidth = metrics.stringWidth(targetLabel);
                }
                g2d.setColor(new Color(255, 255, 255, 190));
                g2d.drawString(targetLabel, x + Math.max(6, (w - targetWidth) / 2), y + 14);
            }
        } finally {
            g2d.dispose();
        }
    }

    private void fire(GameWorld world) {
        if (world == null) {
            return;
        }
        if (action == TriggerAction.SET_RESPAWN) {
            world.setRespawnPoint(getX() + getWidth() / 2, getY() + getHeight() / 2);
            return;
        }
        if (action == TriggerAction.SHOW_GOALS) {
            world.setShowGoals(true);
            return;
        }
        if (action == TriggerAction.HIDE_GOALS) {
            world.setShowGoals(false);
            return;
        }
        if (action == TriggerAction.TOGGLE_GOALS) {
            world.toggleShowGoals();
            return;
        }
        if (action == TriggerAction.RESPAWN_PLAYER) {
            world.respawnPlayer();
            return;
        }
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        for (GameObject object : world.getObjects()) {
            if (object == this || object == null) {
                continue;
            }
            if (!targetName.equals(object.getName())) {
                continue;
            }
            switch (action) {
                case ACTIVATE -> object.setActive(true);
                case DEACTIVATE -> object.setActive(false);
                case TOGGLE -> object.setActive(!object.isActive());
                case SET_RESPAWN, SHOW_GOALS, HIDE_GOALS, TOGGLE_GOALS, RESPAWN_PLAYER -> {
                    // handled above
                }
            }
            break;
        }
    }

    private boolean overlaps(GameObject other) {
        return other != null
            && getX() < other.getX() + other.getWidth()
            && getX() + getWidth() > other.getX()
            && getY() < other.getY() + other.getHeight()
            && getY() + getHeight() > other.getY();
    }

    private static String clipText(FontMetrics metrics, String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        if (metrics.stringWidth(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        int available = Math.max(0, maxWidth - metrics.stringWidth(ellipsis));
        if (available <= 0) {
            return ellipsis;
        }
        int end = value.length();
        while (end > 0 && metrics.stringWidth(value.substring(0, end)) > available) {
            end--;
        }
        if (end <= 0) {
            return ellipsis;
        }
        return value.substring(0, end) + ellipsis;
    }
}

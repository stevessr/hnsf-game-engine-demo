package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;

public final class DialogObject extends BaseObject {
    private String speakerName;
    private String message;

    public DialogObject(String name, int x, int y, int width, int height, String speakerName, String message) {
        super(GameObjectType.DIALOG, name, x, y, width, height, new Color(20, 24, 32, 220), true);
        this.speakerName = normalizeText(speakerName, "Narrator");
        this.message = normalizeText(message, "...");
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = normalizeText(speakerName, "Narrator");
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = normalizeText(message, "...");
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 20, 20);
        graphics.setColor(Color.WHITE);
        graphics.drawRoundRect(getX(), getY(), getWidth(), getHeight(), 20, 20);
        graphics.setColor(new Color(255, 218, 120));
        graphics.drawString(speakerName + ":", getX() + 14, getY() + 22);
        graphics.setColor(Color.WHITE);
        graphics.drawString(message, getX() + 14, getY() + 44);
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
package lib.object;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class DialogObject extends BaseObject {
    private String speakerName;
    private String message;
    private int fontSize;

    public DialogObject(String name, int x, int y, int width, int height, String speakerName, String message) {
        super(GameObjectType.DIALOG, name, x, y, width, height, new Color(20, 24, 32, 220), true);
        this.speakerName = normalizeText(speakerName, "Narrator");
        this.message = normalizeText(message, "...");
        this.fontSize = 18;
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

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(10, Math.min(64, fontSize));
    }

    @Override
    public void render(Graphics2D graphics) {
        Font originalFont = graphics.getFont();
        Font font = originalFont.deriveFont((float) fontSize);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);
        graphics.setColor(getColor());
        graphics.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 20, 20);
        graphics.setColor(Color.WHITE);
        graphics.drawRoundRect(getX(), getY(), getWidth(), getHeight(), 20, 20);
        graphics.setColor(new Color(255, 218, 120));
        int baseline = getY() + metrics.getAscent() + 10;
        graphics.drawString(speakerName + ":", getX() + 14, baseline);
        graphics.setColor(Color.WHITE);
        int textY = baseline + metrics.getHeight();
        for (String line : wrapText(message, Math.max(60, getWidth() - 28), metrics)) {
            if (textY > getY() + getHeight() - 10) {
                break;
            }
            graphics.drawString(line, getX() + 14, textY);
            textY += metrics.getHeight();
        }
        graphics.setFont(originalFont);
    }

    private List<String> wrapText(String text, int maxWidth, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("...");
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            current.append(ch);
            if (metrics.stringWidth(current.toString()) > maxWidth) {
                current.deleteCharAt(current.length() - 1);
                if (current.length() > 0) {
                    lines.add(current.toString());
                }
                current.setLength(0);
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add(text);
        }
        return lines;
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}

package lib.object;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class MenuObject extends BaseObject {
    private String title;
    private List<String> options;
    private int selectedIndex;
    private int fontSize;

    public MenuObject(String name, int x, int y, int width, int height, String title, List<String> options) {
        super(GameObjectType.MENU, name, x, y, width, height, new Color(28, 32, 45, 230), true);
        this.title = normalizeText(title, "Menu");
        this.options = normalizeOptions(options);
        this.selectedIndex = 0;
        this.fontSize = 18;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = normalizeText(title, "Menu");
    }

    public List<String> getOptions() {
        return List.copyOf(options);
    }

    public void setOptions(List<String> options) {
        this.options = normalizeOptions(options);
        this.selectedIndex = clampIndex(selectedIndex);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = clampIndex(selectedIndex);
    }

    public String getSelectedOption() {
        return options.get(selectedIndex);
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(10, Math.min(64, fontSize));
    }

    public int getTitleAreaHeight() {
        if (fontSize == 18) {
            return 42;
        }
        return Math.max(30, fontSize + 14);
    }

    public int getOptionLineHeight() {
        if (fontSize == 18) {
            return 18;
        }
        return Math.max(20, fontSize + 8);
    }

    public int getOptionStartY() {
        return getY() + getTitleAreaHeight();
    }

    public int getPreferredHeight() {
        return getTitleAreaHeight() + (options.size() * getOptionLineHeight()) + 12;
    }

    public void nextOption() {
        selectedIndex = (selectedIndex + 1) % options.size();
    }

    public void previousOption() {
        selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
    }

    @Override
    public void render(Graphics2D graphics) {
        Font originalFont = graphics.getFont();
        Font font = originalFont.deriveFont((float) fontSize);
        graphics.setFont(font);
        
        // 渲染阴影
        graphics.setColor(new Color(0, 0, 0, 80));
        graphics.fillRoundRect(getX() + 4, getY() + 4, getWidth(), getHeight(), 18, 18);

        // 背景渐变
        java.awt.GradientPaint gp = new java.awt.GradientPaint(
            getX(), getY(), new Color(28, 32, 45, 245),
            getX(), getY() + getHeight(), new Color(48, 55, 75, 245)
        );
        graphics.setPaint(gp);
        graphics.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 18, 18);
        
        graphics.setColor(new Color(255, 255, 255, 60));
        graphics.drawRoundRect(getX(), getY(), getWidth(), getHeight(), 18, 18);
        
        FontMetrics metrics = graphics.getFontMetrics(font);
        
        // 居中标题
        graphics.setColor(Color.WHITE);
        int titleWidth = metrics.stringWidth(title);
        int titleX = getX() + (getWidth() - titleWidth) / 2;
        int titleBaseline = getY() + (fontSize == 18 ? 20 : Math.max(20, metrics.getAscent() + 10));
        graphics.drawString(title, titleX, titleBaseline);
        
        // 标题下方装饰线
        graphics.setColor(new Color(255, 255, 255, 40));
        graphics.drawLine(getX() + 20, titleBaseline + 8, getX() + getWidth() - 20, titleBaseline + 8);

        for (int index = 0; index < options.size(); index++) {
            int lineY = getOptionStartY() + (index * getOptionLineHeight()) + (fontSize == 18 ? 0 : metrics.getAscent());
            boolean isSelected = (index == selectedIndex);
            
            if (isSelected) {
                // 选中项的高亮背景
                graphics.setColor(new Color(255, 215, 100, 40));
                graphics.fillRoundRect(getX() + 8, lineY - metrics.getAscent() + 2, getWidth() - 16, getOptionLineHeight(), 8, 8);
                graphics.setColor(new Color(255, 220, 120));
            } else {
                graphics.setColor(new Color(200, 200, 200));
            }
            
            String prefix = isSelected ? "▶ " : "  ";
            graphics.drawString(prefix + options.get(index), getX() + 20, lineY);
        }
        graphics.setFont(originalFont);
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static List<String> normalizeOptions(List<String> rawOptions) {
        List<String> normalized = new ArrayList<>();
        if (rawOptions != null) {
            for (String option : rawOptions) {
                if (option != null && !option.isBlank()) {
                    normalized.add(option);
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add("Empty");
        }
        return normalized;
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(options.size() - 1, index));
    }
}

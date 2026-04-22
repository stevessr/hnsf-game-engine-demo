package lib.object;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MenuObject extends BaseObject {
    private String title;
    private String subtitle;
    private List<String> options;
    private int selectedIndex;
    private int hoveredIndex;
    private int fontSize;
    private int optionColumns;
    private int maxVisibleRows;
    private int scrollOffset;

    public MenuObject(String name, int x, int y, int width, int height, String title, List<String> options) {
        super(GameObjectType.MENU, name, x, y, width, height, new Color(28, 32, 45, 230), true);
        this.title = normalizeText(title, "Menu");
        this.subtitle = null;
        this.options = normalizeOptions(options);
        this.selectedIndex = 0;
        this.hoveredIndex = -1;
        this.fontSize = 18;
        this.optionColumns = 1;
        this.maxVisibleRows = Integer.MAX_VALUE;
        this.scrollOffset = 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = normalizeText(title, "Menu");
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = normalizeSubtitle(subtitle);
    }

    public List<String> getOptions() {
        return List.copyOf(options);
    }

    public void setOptions(List<String> options) {
        this.options = normalizeOptions(options);
        this.selectedIndex = clampIndex(selectedIndex);
        this.hoveredIndex = hoveredIndex < 0 ? -1 : clampIndex(hoveredIndex);
        clampScrollOffset();
        ensureSelectedVisible();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = clampIndex(selectedIndex);
        ensureSelectedVisible();
    }

    public int getHoveredIndex() {
        return hoveredIndex;
    }

    public void setHoveredIndex(int hoveredIndex) {
        this.hoveredIndex = hoveredIndex < 0 ? -1 : clampIndex(hoveredIndex);
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

    public int getOptionColumns() {
        return optionColumns;
    }

    public void setOptionColumns(int optionColumns) {
        this.optionColumns = Math.max(1, Math.min(4, optionColumns));
        clampScrollOffset();
        ensureSelectedVisible();
    }

    public int getMaxVisibleRows() {
        return maxVisibleRows;
    }

    public void setMaxVisibleRows(int maxVisibleRows) {
        this.maxVisibleRows = maxVisibleRows <= 0 ? Integer.MAX_VALUE : maxVisibleRows;
        clampScrollOffset();
        ensureSelectedVisible();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
        clampScrollOffset();
    }

    public int getTitleAreaHeight() {
        int titleHeight = Math.max(44, fontSize + 22);
        if (subtitle != null && !subtitle.isBlank()) {
            return titleHeight + Math.max(22, fontSize + 6);
        }
        return titleHeight;
    }

    public int getOptionLineHeight() {
        return Math.max(30, fontSize + 16);
    }

    public int getOptionStartY() {
        return getY() + getTitleAreaHeight();
    }

    public int getPreferredHeight() {
        return getTitleAreaHeight() + (getVisibleRowCount() * getOptionLineHeight()) + 18;
    }

    public int getOptionRows() {
        return Math.max(1, (options.size() + optionColumns - 1) / optionColumns);
    }

    public int getVisibleRowCount() {
        return Math.max(1, Math.min(getOptionRows(), maxVisibleRows));
    }

    public Rectangle getOptionBounds(int index) {
        if (options.isEmpty() || index < 0 || index >= options.size()) {
            return new Rectangle();
        }
        int rows = getOptionRows();
        int column = index / rows;
        int row = index % rows;
        int visibleRow = row - scrollOffset;
        if (visibleRow < 0 || visibleRow >= getVisibleRowCount()) {
            return new Rectangle();
        }
        int gap = getOptionColumnGap();
        int scrollBarAllowance = hasVerticalScrollBar() ? 18 : 0;
        int buttonWidth = Math.max(120, (getWidth() - 24 - gap * (optionColumns - 1) - scrollBarAllowance) / optionColumns);
        int buttonHeight = getOptionLineHeight() - 6;
        int buttonX = getX() + 12 + column * (buttonWidth + gap);
        int buttonY = getOptionStartY() + visibleRow * getOptionLineHeight() + 2;
        return new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
    }

    public int findOptionIndexAt(int mouseX, int mouseY) {
        for (int index = 0; index < options.size(); index++) {
            Rectangle bounds = getOptionBounds(index);
            if (!bounds.isEmpty() && bounds.contains(mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    public void nextOption() {
        setSelectedIndex((selectedIndex + 1) % options.size());
    }

    public void previousOption() {
        setSelectedIndex((selectedIndex - 1 + options.size()) % options.size());
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        Font originalFont = g2d.getFont();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            Color panelBase = getColor() == null ? new Color(28, 32, 45, 230) : getColor();
            Color panelTop = brighten(panelBase, 18, 240);
            Color panelBottom = darken(panelBase, 26, 244);
            Color accent = resolveAccentColor();
            Color accentSecondary = mix(accent, new Color(120, 222, 255), 0.35, 255);

            g2d.setColor(new Color(0, 0, 0, 62));
            g2d.fillRoundRect(x + 8, y + 10, width, height, 24, 24);
            g2d.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 38));
            g2d.fillRoundRect(x - 4, y - 4, width + 8, height + 8, 28, 28);

            g2d.setPaint(new GradientPaint(x, y, panelTop, x, y + height, panelBottom));
            g2d.fillRoundRect(x, y, width, height, 22, 22);

            g2d.setPaint(new GradientPaint(
                x,
                y,
                new Color(255, 255, 255, 42),
                x,
                y + height / 2,
                new Color(255, 255, 255, 0)
            ));
            g2d.fillRoundRect(x + 2, y + 2, width - 4, Math.max(20, height / 2), 20, 20);

            g2d.setColor(new Color(255, 255, 255, 74));
            g2d.setStroke(new BasicStroke(1.4f));
            g2d.drawRoundRect(x, y, width, height, 22, 22);
            g2d.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 86));
            g2d.drawRoundRect(x + 1, y + 1, width - 2, height - 2, 22, 22);

            Font titleFont = originalFont.deriveFont(Font.BOLD, Math.max(20f, fontSize + 6f));
            g2d.setFont(titleFont);
            FontMetrics titleMetrics = g2d.getFontMetrics(titleFont);
            int titleWidth = titleMetrics.stringWidth(title);
            int titleX = x + (width - titleWidth) / 2;
            int titleBaseline = y + Math.max(30, titleMetrics.getAscent() + 14);

            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.drawString(title, titleX + 2, titleBaseline + 2);
            g2d.setPaint(new GradientPaint(
                titleX,
                y,
                mix(accent, Color.WHITE, 0.48, 255),
                titleX,
                titleBaseline,
                accentSecondary
            ));
            g2d.drawString(title, titleX, titleBaseline);

            int dividerY = titleBaseline + 12;
            if (subtitle != null && !subtitle.isBlank()) {
                Font subtitleFont = originalFont.deriveFont((float) Math.max(11, fontSize - 2));
                g2d.setFont(subtitleFont);
                FontMetrics subtitleMetrics = g2d.getFontMetrics(subtitleFont);
                int subtitleWidth = subtitleMetrics.stringWidth(subtitle);
                int subtitleX = x + (width - subtitleWidth) / 2;
                int subtitleBaseline = titleBaseline + subtitleMetrics.getHeight();
                g2d.setColor(new Color(235, 243, 255, 215));
                g2d.drawString(subtitle, subtitleX, subtitleBaseline);
                dividerY = subtitleBaseline + 10;
            }

            g2d.setPaint(new GradientPaint(
                x + 22,
                dividerY,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28),
                x + width - 22,
                dividerY,
                new Color(accentSecondary.getRed(), accentSecondary.getGreen(), accentSecondary.getBlue(), 88)
            ));
            g2d.drawLine(x + 22, dividerY, x + width - 22, dividerY);

            float optionFontSize = optionColumns > 1 ? Math.max(12f, fontSize - 1f) : (float) fontSize;
            Font optionFont = originalFont.deriveFont(optionFontSize);
            g2d.setFont(optionFont);
            FontMetrics optionMetrics = g2d.getFontMetrics(optionFont);

            for (int index = 0; index < options.size(); index++) {
                boolean isSelected = index == selectedIndex;
                boolean isHovered = index == hoveredIndex;
                Rectangle bounds = getOptionBounds(index);
                if (bounds.isEmpty()) {
                    continue;
                }
                int buttonX = bounds.x;
                int buttonY = bounds.y;
                int buttonWidth = bounds.width;
                int buttonHeight = bounds.height;

                if (isHovered || isSelected) {
                    Color glowColor = isHovered
                        ? mix(accentSecondary, Color.WHITE, 0.15, 96)
                        : mix(accent, Color.WHITE, 0.10, 76);
                    g2d.setColor(glowColor);
                    g2d.fillRoundRect(
                        buttonX - 4,
                        buttonY - 4,
                        buttonWidth + 8,
                        buttonHeight + 8,
                        18,
                        18
                    );
                }

                Color buttonTop = isHovered
                    ? mix(accentSecondary, panelTop, 0.48, 232)
                    : isSelected
                        ? mix(accent, panelTop, 0.42, 224)
                        : mix(panelTop, Color.WHITE, 0.12, 192);
                Color buttonBottom = isHovered
                    ? mix(accent, panelBottom, 0.35, 238)
                    : isSelected
                        ? mix(accentSecondary, panelBottom, 0.25, 228)
                        : darken(panelBottom, 6, 182);
                g2d.setPaint(new GradientPaint(
                    buttonX,
                    buttonY,
                    buttonTop,
                    buttonX,
                    buttonY + buttonHeight,
                    buttonBottom
                ));
                g2d.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 16, 16);

                g2d.setColor(new Color(255, 255, 255, isHovered || isSelected ? 112 : 48));
                g2d.drawRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 16, 16);

                int textX = buttonX + 16;
                if (isSelected) {
                    int arrowCenterY = buttonY + buttonHeight / 2;
                    g2d.setColor(new Color(255, 245, 220, 235));
                    g2d.fillPolygon(
                        new int[]{buttonX + 12, buttonX + 12, buttonX + 26},
                        new int[]{arrowCenterY - 10, arrowCenterY + 10, arrowCenterY},
                        3
                    );
                    textX += 22;
                }
                int textY = buttonY + (buttonHeight - optionMetrics.getHeight()) / 2 + optionMetrics.getAscent();
                g2d.setColor(isHovered || isSelected ? new Color(255, 250, 240) : new Color(210, 220, 232));
                Shape previousClip = g2d.getClip();
                g2d.setClip(buttonX + 10, buttonY + 4, Math.max(20, buttonWidth - 20), Math.max(12, buttonHeight - 8));
                g2d.drawString(options.get(index), textX, textY);
                g2d.setClip(previousClip);
            }
            renderScrollBar(g2d);
        } finally {
            g2d.setFont(originalFont);
            g2d.dispose();
        }
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return lib.utils.Unicode2Gbk.convert(value);
    }

    private static String normalizeSubtitle(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return lib.utils.Unicode2Gbk.convert(value);
    }

    private static List<String> normalizeOptions(List<String> rawOptions) {
        List<String> normalized = new ArrayList<>();
        if (rawOptions != null) {
            for (String option : rawOptions) {
                if (option != null && !option.isBlank()) {
                    normalized.add(lib.utils.Unicode2Gbk.convert(option));
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

    private int getOptionColumnGap() {
        return optionColumns > 1 ? 14 : 0;
    }

    private boolean hasVerticalScrollBar() {
        return getOptionRows() > getVisibleRowCount();
    }

    private void clampScrollOffset() {
        int maxScroll = Math.max(0, getOptionRows() - getVisibleRowCount());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    private void ensureSelectedVisible() {
        int rows = getOptionRows();
        int visibleRows = getVisibleRowCount();
        int selectedRow = clampIndex(selectedIndex) % rows;
        if (selectedRow < scrollOffset) {
            scrollOffset = selectedRow;
        } else if (selectedRow >= scrollOffset + visibleRows) {
            scrollOffset = selectedRow - visibleRows + 1;
        }
        clampScrollOffset();
    }

    private void renderScrollBar(Graphics2D graphics) {
        if (!hasVerticalScrollBar()) {
            return;
        }
        int totalRows = getOptionRows();
        int visibleRows = getVisibleRowCount();
        int trackX = getX() + getWidth() - 14;
        int trackY = getOptionStartY() + 8;
        int trackHeight = Math.max(40, visibleRows * getOptionLineHeight() - 12);
        int thumbHeight = Math.max(24, (int) Math.round(trackHeight * (visibleRows / (double) totalRows)));
        int maxScroll = Math.max(1, totalRows - visibleRows);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (int) Math.round((scrollOffset / (double) maxScroll) * thumbTravel);

        graphics.setColor(new Color(255, 255, 255, 28));
        graphics.fillRoundRect(trackX, trackY, 6, trackHeight, 6, 6);
        graphics.setColor(new Color(140, 220, 255, 138));
        graphics.fillRoundRect(trackX, thumbY, 6, thumbHeight, 6, 6);
    }

    private Color resolveAccentColor() {
        String normalizedName = getName() == null ? "" : getName().toLowerCase(Locale.ROOT);
        if (normalizedName.contains("pause") || normalizedName.contains("options")) {
            return new Color(118, 200, 255);
        }
        if (normalizedName.contains("victory")) {
            return new Color(126, 235, 168);
        }
        if (normalizedName.contains("gameover")) {
            return new Color(255, 126, 126);
        }
        if (normalizedName.contains("level")) {
            return new Color(120, 228, 255);
        }
        return new Color(255, 214, 120);
    }

    private static Color brighten(Color color, int amount, int alpha) {
        return new Color(
            clampColor(color.getRed() + amount),
            clampColor(color.getGreen() + amount),
            clampColor(color.getBlue() + amount),
            clampColor(alpha)
        );
    }

    private static Color darken(Color color, int amount, int alpha) {
        return new Color(
            clampColor(color.getRed() - amount),
            clampColor(color.getGreen() - amount),
            clampColor(color.getBlue() - amount),
            clampColor(alpha)
        );
    }

    private static Color mix(Color first, Color second, double ratio, int alpha) {
        double safeRatio = Math.max(0.0, Math.min(1.0, ratio));
        int red = (int) Math.round(first.getRed() * safeRatio + second.getRed() * (1.0 - safeRatio));
        int green = (int) Math.round(first.getGreen() * safeRatio + second.getGreen() * (1.0 - safeRatio));
        int blue = (int) Math.round(first.getBlue() * safeRatio + second.getBlue() * (1.0 - safeRatio));
        return new Color(clampColor(red), clampColor(green), clampColor(blue), clampColor(alpha));
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}

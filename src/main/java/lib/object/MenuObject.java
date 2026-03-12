package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public final class MenuObject extends BaseObject {
    private String title;
    private List<String> options;
    private int selectedIndex;

    public MenuObject(String name, int x, int y, int width, int height, String title, List<String> options) {
        super(GameObjectType.MENU, name, x, y, width, height, new Color(28, 32, 45, 230), true);
        this.title = normalizeText(title, "Menu");
        this.options = normalizeOptions(options);
        this.selectedIndex = 0;
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

    public void nextOption() {
        selectedIndex = (selectedIndex + 1) % options.size();
    }

    public void previousOption() {
        selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
    }

    @Override
    public void render(Graphics2D graphics) {
        graphics.setColor(getColor());
        graphics.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 18, 18);
        graphics.setColor(Color.WHITE);
        graphics.drawRoundRect(getX(), getY(), getWidth(), getHeight(), 18, 18);
        graphics.drawString(title, getX() + 12, getY() + 20);

        for (int index = 0; index < options.size(); index++) {
            int lineY = getY() + 42 + (index * 18);
            String prefix = index == selectedIndex ? "> " : "  ";
            graphics.setColor(index == selectedIndex ? new Color(255, 220, 120) : Color.LIGHT_GRAY);
            graphics.drawString(prefix + options.get(index), getX() + 12, lineY);
        }
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
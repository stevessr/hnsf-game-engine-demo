package lib.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public final class EditorOverlay {
    private boolean showGrid = true;
    private int gridSize = 20;
    private String modeInfo = "SELECT";

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = Math.max(4, gridSize);
    }

    public void setModeInfo(String modeInfo) {
        this.modeInfo = modeInfo;
    }

    public void render(Graphics2D graphics, int width, int height) {
        if (showGrid) {
            graphics.setColor(new Color(255, 255, 255, 20));
            graphics.setStroke(new BasicStroke(1f));
            for (int x = 0; x <= width; x += gridSize) {
                graphics.drawLine(x, 0, x, height);
            }
            for (int y = 0; y <= height; y += gridSize) {
                graphics.drawLine(0, y, width, y);
            }
        }

        renderLegend(graphics, width, height);
    }

    private void renderLegend(Graphics2D g, int width, int height) {
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 12));
        int x = 10;
        int y = height - 140;

        // Background for legend
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, y, 220, 130, 10, 10);
        g.setColor(new Color(255, 255, 255, 100));
        g.drawRoundRect(x, y, 220, 130, 10, 10);

        g.setColor(Color.YELLOW);
        g.drawString("EDITOR HINTS", x + 10, y + 20);
        
        g.setColor(Color.WHITE);
        int line = y + 40;
        g.drawString("MODE: " + modeInfo, x + 10, line);
        line += 15;
        g.drawString("Ctrl+Z/Y: Undo/Redo", x + 10, line);
        line += 15;
        g.drawString("Ctrl+D  : Duplicate", x + 10, line);
        line += 15;
        g.drawString("Del     : Delete", x + 10, line);
        line += 15;
        g.drawString("Arrows  : Nudge (Shift x10)", x + 10, line);
        line += 15;
        g.drawString("G/S/B/E : Toggle/Mode", x + 10, line);
    }
}

package lib.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public final class EditorOverlay {
    private boolean showGrid = true;
    private int gridSize = 20;

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

    public void render(Graphics2D graphics, int width, int height) {
        if (!showGrid) {
            return;
        }
        graphics.setColor(new Color(255, 255, 255, 20));
        graphics.setStroke(new BasicStroke(1f));
        for (int x = 0; x <= width; x += gridSize) {
            graphics.drawLine(x, 0, x, height);
        }
        for (int y = 0; y <= height; y += gridSize) {
            graphics.drawLine(0, y, width, y);
        }
    }
}

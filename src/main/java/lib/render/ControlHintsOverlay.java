package lib.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import lib.game.GameWorld;
import lib.state.GameState;

/**
 * 屏幕按键提示遮罩层。
 * 
 * <p>在游戏画面左上角渲染一个半透明的控制面板，实时向玩家展示当前的常用快捷键和对应的功能描述。
 * 该层在所有游戏对象和光照层之上渲染，确保在黑暗关卡中依然清晰可见。
 */
public final class ControlHintsOverlay {
    /** 提示层是否可见 */
    private boolean visible = true;

    /**
     * 设置遮罩层的可见性。
     * 
     * @param visible true 表示显示，false 表示隐藏
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * 在指定的图形上下文中渲染按键提示。
     * 
     * @param graphics       AWT 图形上下文
     * @param viewportWidth  视口宽度
     * @param viewportHeight 视口高度
     */
    public void render(Graphics2D graphics, int viewportWidth, int viewportHeight) {
        render(graphics, viewportWidth, viewportHeight, false);
    }

    public void render(Graphics2D graphics, int viewportWidth, int viewportHeight, boolean aiActive) {
        render(graphics, viewportWidth, viewportHeight, null, aiActive);
    }

    public void render(Graphics2D graphics, int viewportWidth, int viewportHeight, GameWorld world, boolean aiActive) {
        if (!visible) {
            return;
        }

        // 开启文本抗锯齿
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        List<Hint> hints = buildHints(world == null ? GameState.PLAYING : world.getCurrentState());

        int bgW = 180;
        int rowHeight = 20;
        int bgH = (hints.size() + (aiActive ? 2 : 1)) * rowHeight + 15;
        
        // 移至右下角，避开右上角小地图
        int x = viewportWidth - bgW - 20;
        int y = viewportHeight - bgH - 20;
        
        // 绘制背景板 (极简毛玻璃感)
        graphics.setColor(new Color(15, 15, 20, 160));
        graphics.fillRoundRect(x, y, bgW, bgH, 12, 12);
        graphics.setColor(new Color(255, 255, 255, 30));
        graphics.drawRoundRect(x, y, bgW, bgH, 12, 12);

        int textX = x + 15;
        int textY = y + 22;

        graphics.setColor(new Color(120, 190, 255));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 11));
        String stateName = world == null ? "PLAYING" : world.getCurrentState().name();
        graphics.drawString("STATE: " + stateName, textX, textY);
        textY += rowHeight;
        
        if (aiActive) {
            graphics.setColor(new Color(100, 255, 100));
            graphics.setFont(new Font("SansSerif", Font.BOLD, 11));
            graphics.drawString("● AI ACTIVE", textX, textY);
            textY += rowHeight;
        }

        Font keyFont = new Font("Monospaced", Font.BOLD, 11);
        Font descFont = new Font("SansSerif", Font.PLAIN, 11);

        for (Hint hint : hints) {
            graphics.setFont(keyFont);
            graphics.setColor(new Color(255, 215, 100)); 
            graphics.drawString(hint.key, textX, textY);
            
            graphics.setFont(descFont);
            graphics.setColor(new Color(220, 220, 220));
            graphics.drawString(hint.desc, textX + 65, textY);
            
            textY += rowHeight;
        }
    }

    private List<Hint> buildHints(GameState state) {
        List<Hint> hints = new ArrayList<>();
        switch (state) {
            case MENU, GAMEOVER, SETTLEMENT -> {
                hints.add(new Hint("W/S · Q/E", "Navigate"));
                hints.add(new Hint("Enter/Click", "Confirm"));
                hints.add(new Hint("P/Esc", "Back"));
            }
            case DIALOG -> {
                hints.add(new Hint("Enter/Space", "Next Dialog"));
                hints.add(new Hint("WASD", "Move After Close"));
                hints.add(new Hint("P/Esc", "Pause"));
            }
            case PAUSED -> {
                hints.add(new Hint("W/S · Q/E", "Navigate"));
                hints.add(new Hint("Enter/Click", "Confirm"));
                hints.add(new Hint("P/Esc", "Resume"));
            }
            case PLAYING -> {
                hints.add(new Hint("WASD", "Move"));
                hints.add(new Hint("Shift", "Sprint"));
                hints.add(new Hint("Space", "Jump"));
                hints.add(new Hint("K / Click", "Shoot"));
                hints.add(new Hint("B", "Switch Bullet Type"));
                hints.add(new Hint("MidClick", "Build Voxel"));
                hints.add(new Hint("RClick", "Destroy Voxel"));
                hints.add(new Hint("G", "Goals"));
                hints.add(new Hint("P/Esc", "Pause"));
            }
            default -> {
                hints.add(new Hint("WASD", "Move"));
                hints.add(new Hint("Enter", "Confirm"));
            }
        }
        hints.add(new Hint("H", "AI Auto"));
        return hints;
    }

    /** 简单的提示数据结构 */
    private static record Hint(String key, String desc) {}
}

package lib.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

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
        if (!visible) {
            return;
        }

        // 开启文本抗锯齿
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        List<Hint> hints = new ArrayList<>();
        hints.add(new Hint("WASD / Arrows", "Move (移动)"));
        hints.add(new Hint("Space", "Jump (跳跃)"));
        hints.add(new Hint("K", "Shoot (攻击)"));
        hints.add(new Hint("T", "Cycle Throttle (切换动力)"));
        hints.add(new Hint("C", "Cycle Color (切换颜色)"));
        hints.add(new Hint("P / Esc", "Pause (暂停)"));
        hints.add(new Hint("Mouse L/R", "Voxel (体素建造/销毁)"));

        int x = 20;
        int y = 30;
        int rowHeight = 22;
        
        // 绘制背景板 (半透明深色)
        int bgW = 240;
        int bgH = hints.size() * rowHeight + 15;
        graphics.setColor(new Color(0, 0, 0, 100));
        graphics.fillRoundRect(10, 10, bgW, bgH, 10, 10);
        graphics.setColor(new Color(255, 255, 255, 40));
        graphics.drawRoundRect(10, 10, bgW, bgH, 10, 10);

        Font keyFont = new Font("Monospaced", Font.BOLD, 12);
        Font descFont = new Font("SansSerif", Font.PLAIN, 12);

        for (Hint hint : hints) {
            // 绘制按键标识 (金色)
            graphics.setFont(keyFont);
            graphics.setColor(new Color(255, 215, 0)); 
            graphics.drawString("[" + hint.key + "]", x, y);
            
            // 绘制功能描述 (白色)
            graphics.setFont(descFont);
            graphics.setColor(Color.WHITE);
            graphics.drawString(hint.desc, x + 110, y);
            
            y += rowHeight;
        }
    }

    /** 简单的提示数据结构 */
    private static record Hint(String key, String desc) {}
}

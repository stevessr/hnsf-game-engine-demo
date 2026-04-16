package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import lib.game.GameWorld;

/**
 * 关卡终点/出口对象。
 */
public final class GoalObject extends BaseObject {
    private double animationTimer = 0;

    public GoalObject(String name, int x, int y, int width, int height) {
        super(GameObjectType.GOAL, name, x, y, width, height, new Color(255, 215, 0), true);
    }

    @Override
    public void update(GameWorld world, double deltaSeconds) {
        animationTimer += deltaSeconds;
    }

    @Override
    public void render(Graphics2D graphics) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int cx = x + w / 2;
        int cy = y + h / 2;

        // 1. 绘制外部发光光环
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {new Color(255, 215, 0, 100), new Color(255, 215, 0, 0)};
        int glowRadius = (int)(w * 0.8 + Math.sin(animationTimer * 4) * 10);
        RadialGradientPaint glow = new RadialGradientPaint(
            new Point2D.Float(cx, cy),
            glowRadius,
            dist,
            colors
        );
        graphics.setPaint(glow);
        graphics.fillOval(cx - glowRadius, cy - glowRadius, glowRadius * 2, glowRadius * 2);

        // 2. 绘制金色传送门外圈
        graphics.setColor(getColor());
        graphics.setStroke(new BasicStroke(4));
        graphics.drawOval(x, y, w, h);
        
        // 3. 内部旋转核心
        int padding = 12 + (int)(Math.sin(animationTimer * 5) * 6);
        graphics.fillOval(x + padding, y + padding, w - padding * 2, h - padding * 2);
        
        // 4. 发光粒子效果
        graphics.setColor(Color.WHITE);
        for (int i = 0; i < 6; i++) {
            double angle = animationTimer * 2.5 + (i * Math.PI / 3);
            int orbitRadius = w / 2 + 8;
            int px = (int)(cx + Math.cos(angle) * orbitRadius);
            int py = (int)(cy + Math.sin(angle) * orbitRadius);
            int size = 4 + (int)(Math.sin(animationTimer * 10 + i) * 2);
            graphics.fillOval(px - size / 2, py - size / 2, size, size);
        }
    }
}

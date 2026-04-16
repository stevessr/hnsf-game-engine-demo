package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
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

        // 绘制金色传送门效果
        graphics.setColor(getColor());
        graphics.setStroke(new BasicStroke(3));
        graphics.drawOval(x, y, w, h);
        
        // 内部旋转核心
        int padding = 10 + (int)(Math.sin(animationTimer * 5) * 5);
        graphics.fillOval(x + padding, y + padding, w - padding * 2, h - padding * 2);
        
        // 发光粒子效果 (简单绘制)
        graphics.setColor(new Color(255, 255, 255, 150));
        for (int i = 0; i < 4; i++) {
            double angle = animationTimer * 3 + (i * Math.PI / 2);
            int px = (int)(x + w/2 + Math.cos(angle) * (w/2 + 5));
            int py = (int)(y + h/2 + Math.sin(angle) * (h/2 + 5));
            graphics.fillOval(px - 2, py - 2, 4, 4);
        }
    }
}

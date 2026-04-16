package lib.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import lib.game.GameWorld;

/**
 * 可选的光照系统管理器。
 */
public final class LightingManager {
    private boolean enabled = false;
    private float ambientLight = 0.0f;
    private final List<LightSource> lights = new ArrayList<>();
    private BufferedImage overlayBuffer;

    public static record LightSource(int x, int y, int radius, float intensity) {}

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void addLight(int x, int y, int radius, float intensity) {
        lights.add(new LightSource(x, y, radius, intensity));
    }

    public void clearLights() {
        lights.clear();
    }

    public void render(Graphics2D graphics, GameWorld world) {
        if (!enabled) {
            return;
        }

        int width = world.getWidth();
        int height = world.getHeight();
        
        if (width <= 0 || height <= 0) {
            return;
        }

        // 复用缓冲区以提高性能
        if (overlayBuffer == null || overlayBuffer.getWidth() != width || overlayBuffer.getHeight() != height) {
            overlayBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        
        Graphics2D g2d = overlayBuffer.createGraphics();
        
        // 1. 绘制环境光（暗色蒙版）
        // 强制使用 SRC 模式清除上一帧
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(new Color(0, 0, 0, (int)(255 * (1.0f - ambientLight))));
        g2d.fillRect(0, 0, width, height);
        
        // 2. 挖掘“光洞”
        g2d.setComposite(AlphaComposite.DstOut);
        
        // 自动为玩家添加光源
        world.findPlayer().ifPresent(player -> {
            drawLight(g2d, player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, 200, 1.0f);
        });
        
        for (LightSource light : lights) {
            drawLight(g2d, light.x, light.y, light.radius, light.intensity);
        }
        
        g2d.dispose();
        
        // 绘制到主屏幕，drawImage 会自动处理当前的 Graphics2D 变换（如缩放）
        graphics.drawImage(overlayBuffer, 0, 0, null);
    }

    private void drawLight(Graphics2D g, int x, int y, int radius, float intensity) {
        if (radius <= 0) {
            return;
        }
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {new Color(0, 0, 0, (int)(255 * intensity)), new Color(0, 0, 0, 0)};
        RadialGradientPaint p = new RadialGradientPaint(
            new Point2D.Float(x, y),
            radius,
            dist,
            colors
        );
        g.setPaint(p);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }
}

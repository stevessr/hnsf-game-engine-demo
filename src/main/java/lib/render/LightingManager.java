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
    private float intensityMultiplier = 1.0f;
    private final List<LightSource> lights = new ArrayList<>();
    private BufferedImage overlayBuffer;

    public static record LightSource(int x, int y, int radius, float intensity) {}

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getAmbientLight() {
        return ambientLight;
    }

    public void setAmbientLight(float ambientLight) {
        this.ambientLight = Math.max(0.0f, Math.min(1.0f, ambientLight));
    }

    public float getIntensityMultiplier() {
        return intensityMultiplier;
    }

    public void setIntensityMultiplier(float intensityMultiplier) {
        this.intensityMultiplier = Math.max(0.0f, Math.min(2.0f, intensityMultiplier));
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
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(new Color(0, 0, 0, (int)(255 * (1.0f - ambientLight))));
        g2d.fillRect(0, 0, width, height);
        
        // 2. 挖掘“光洞”
        g2d.setComposite(AlphaComposite.DstOut);
        
        // 自动为玩家添加光源
        world.findPlayer().ifPresent(player -> {
            drawLight(g2d, player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, 200, 1.0f * intensityMultiplier);
        });
        
        // 为出口添加光源
        for (lib.object.GameObject obj : world.getObjectsByType(lib.object.GameObjectType.GOAL)) {
            if (obj.isActive()) {
                drawLight(g2d, obj.getX() + obj.getWidth() / 2, obj.getY() + obj.getHeight() / 2, 250, 1.2f * intensityMultiplier);
            }
        }
        
        // 为投影物添加光源
        for (lib.object.GameObject obj : world.getObjectsByType(lib.object.GameObjectType.PROJECTILE)) {
            if (obj.isActive()) {
                drawLight(g2d, obj.getX() + obj.getWidth() / 2, obj.getY() + obj.getHeight() / 2, 80, 0.8f * intensityMultiplier);
            }
        }
        
        for (LightSource light : lights) {
            drawLight(g2d, light.x, light.y, light.radius, light.intensity * intensityMultiplier);
        }
        
        g2d.dispose();
        
        graphics.drawImage(overlayBuffer, 0, 0, null);
    }

    private void drawLight(Graphics2D g, int x, int y, int radius, float intensity) {
        if (radius <= 0) {
            return;
        }
        float[] dist = {0.0f, 1.0f};
        // intensity 控制光洞的“深度”，即透明度
        int alpha = (int)(255 * Math.max(0.0f, Math.min(1.0f, intensity)));
        Color[] colors = {new Color(0, 0, 0, alpha), new Color(0, 0, 0, 0)};
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

package lib.render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectType;

/**
 * 增强型光照系统管理器，支持探索模式(常亮)和简单的阴影遮挡。
 */
public final class LightingManager {
    private boolean enabled = false;
    private boolean explorationMode = true; // 默认开启探索模式
    private float ambientLight = 0.0f;
    private float intensityMultiplier = 1.0f;
    private final List<LightSource> lights = new ArrayList<>();
    private BufferedImage overlayBuffer;
    private BufferedImage visibilityBuffer; // 存储已探索区域

    public static record LightSource(int x, int y, int radius, float intensity) {}

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isExplorationMode() {
        return explorationMode;
    }

    public void setExplorationMode(boolean explorationMode) {
        boolean wasEnabled = this.explorationMode;
        this.explorationMode = explorationMode;
        if (!wasEnabled && explorationMode) {
            resetExploration();
        }
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

    public void resetExploration() {
        visibilityBuffer = null;
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

        // 初始化缓冲区
        if (overlayBuffer == null || overlayBuffer.getWidth() != width || overlayBuffer.getHeight() != height) {
            overlayBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        if (explorationMode && (visibilityBuffer == null || visibilityBuffer.getWidth() != width || visibilityBuffer.getHeight() != height)) {
            visibilityBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            // 初始全黑且不透明
            Graphics2D gVis = visibilityBuffer.createGraphics();
            gVis.setColor(new Color(0, 0, 0, 255));
            gVis.fillRect(0, 0, width, height);
            gVis.dispose();
        }

        Graphics2D g2d = overlayBuffer.createGraphics();
        
        // 1. 绘制环境光（当前帧黑暗层）
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(new Color(0, 0, 0, (int)(255 * (1.0f - ambientLight))));
        g2d.fillRect(0, 0, width, height);
        
        // 2. 准备挖掘“光洞”和阴影
        g2d.setComposite(AlphaComposite.DstOut);
        
        // 收集所有阴影投射者 (墙壁)
        List<GameObject> casters = world.getObjectsByType(GameObjectType.WALL);

        // 玩家光
        world.findPlayer().ifPresent(player -> {
            int radius = player.getLightRadius();
            drawLightWithShadows(g2d, player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, (int)(radius * intensityMultiplier), 1.0f, casters);
            
            // 更新探索层
            if (explorationMode) {
                Graphics2D gVis = visibilityBuffer.createGraphics();
                gVis.setComposite(AlphaComposite.Clear);
                gVis.fillOval(
                    player.getX() + player.getWidth() / 2 - radius,
                    player.getY() + player.getHeight() / 2 - radius,
                    radius * 2,
                    radius * 2
                );
                gVis.dispose();
            }
        });
        
        // 出口光
        for (GameObject obj : world.getObjectsByType(GameObjectType.GOAL)) {
            if (obj.isActive()) {
                drawLightWithShadows(g2d, obj.getX() + obj.getWidth() / 2, obj.getY() + obj.getHeight() / 2, (int)(250 * intensityMultiplier), 1.2f, casters);
            }
        }
        
        // 动态光
        for (LightSource light : lights) {
            drawLightWithShadows(g2d, light.x, light.y, (int)(light.radius * intensityMultiplier), light.intensity, casters);
        }
        
        g2d.dispose();
        
        // 3. 混合探索层
        if (explorationMode) {
            graphics.drawImage(visibilityBuffer, 0, 0, null);
        }

        graphics.drawImage(overlayBuffer, 0, 0, null);
    }

    private void drawLightWithShadows(Graphics2D g, int x, int y, int radius, float intensity, List<GameObject> casters) {
        if (radius <= 0) {
            return;
        }
        
        Graphics2D gLight = (Graphics2D) g.create();
        
        Area lightArea = new Area(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));
        for (GameObject caster : casters) {
            if (!caster.isActive()) {
                continue;
            }
            lightArea.subtract(new Area(new Rectangle2D.Float(caster.getX(), caster.getY(), caster.getWidth(), caster.getHeight())));
        }
        
        gLight.setClip(lightArea);
        drawLight(gLight, x, y, radius, intensity);
        gLight.dispose();
    }

    private void drawLight(Graphics2D g, int x, int y, int radius, float intensity) {
        float[] dist = {0.0f, 1.0f};
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

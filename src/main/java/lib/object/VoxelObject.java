package lib.object;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.TexturePaint;
import lib.render.TextureManager;

public final class VoxelObject extends SceneObject {
    public VoxelObject(String name, int x, int y, int size) {
        this(name, x, y, size, size, new Color(160, 160, 180));
    }

    public VoxelObject(String name, int x, int y, int size, Color color) {
        this(name, x, y, size, size, color);
    }

    public VoxelObject(String name, int x, int y, int width, int height, Color color) {
        super(GameObjectType.VOXEL, name, x, y, width, height, color, true, false);
    }

    @Override
    public void render(Graphics2D graphics) {
        Paint originalPaint = graphics.getPaint();
        
        // 尝试加载贴图
        TexturePaint tp = null;
        if (getTexturePath() != null) {
            tp = TextureManager.createTexturePaint(getTexturePath(), getWidth(), getHeight());
        }
        
        if (tp != null) {
            graphics.setPaint(tp);
        } else {
            graphics.setColor(getColor());
            
            // 基于材质绘制特殊效果
            String mat = getMaterial();
            if ("grass".equalsIgnoreCase(mat)) {
                graphics.setColor(new Color(34, 139, 34));
            } else if ("wood".equalsIgnoreCase(mat)) {
                graphics.setColor(new Color(139, 69, 19));
            } else if ("stone".equalsIgnoreCase(mat)) {
                graphics.setColor(new Color(105, 105, 105));
            }
        }
        
        graphics.fillRect(getX(), getY(), getWidth(), getHeight());
        
        // 边框
        graphics.setColor(new Color(0, 0, 0, 80));
        graphics.drawRect(getX(), getY(), getWidth(), getHeight());
        
        // 简单的 3D 边角效果
        graphics.setColor(new Color(255, 255, 255, 100));
        graphics.drawLine(getX(), getY(), getX() + getWidth(), getY());
        graphics.drawLine(getX(), getY(), getX(), getY() + getHeight());
        
        graphics.setColor(new Color(0, 0, 0, 110));
        graphics.drawLine(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight() - 1);
        graphics.drawLine(getX() + getWidth() - 1, getY(), getX() + getWidth() - 1, getY() + getHeight());
        
        graphics.setPaint(originalPaint);
    }
}

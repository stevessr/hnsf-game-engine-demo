package lib.render;

import java.awt.Color;
import java.awt.Graphics2D;

import lib.game.GameWorld;
import lib.object.GameObject;
import lib.object.GameObjectType;

/**
 * 小地图渲染层，提供世界的缩略俯视图，帮助玩家在大地图中导航。
 * 
 * <p>特性：
 * <ul>
 *   <li>自适应缩放：自动根据地图宽高调整显示比例。</li>
 *   <li>颜色标记：使用不同颜色区分玩家(蓝)、敌对实体(红)、目标(金)和墙壁(灰)。</li>
 *   <li>视口反馈：在缩略图上用白框标出当前摄像机锁定的可见区域。</li>
 * </ul>
 */
public final class MinimapOverlay {
    /** 小地图的最大显示尺寸 (像素) */
    private static final int MINIMAP_SIZE = 150;
    /** 距离屏幕边缘的边距 */
    private static final int MARGIN = 15;

    /**
     * 在屏幕右上角渲染小地图。
     * 
     * @param g      图形上下文
     * @param world  当前世界引用
     * @param viewW  视口宽度
     * @param viewH  视口高度
     */
    public void render(Graphics2D g, GameWorld world, int viewW, int viewH) {
        int worldW = world.getWidth();
        int worldH = world.getHeight();
        if (worldW <= 0 || worldH <= 0) {
            return;
        }

        // 计算比例
        double scale = (double) MINIMAP_SIZE / Math.max(worldW, worldH);
        int mapW = (int) (worldW * scale);
        int mapH = (int) (worldH * scale);

        int startX = viewW - mapW - MARGIN;
        int startY = MARGIN;

        // 绘制背景
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(startX, startY, mapW, mapH);
        g.setColor(new Color(255, 255, 255, 100));
        g.drawRect(startX, startY, mapW, mapH);

        // 绘制所有对象
        for (GameObject obj : world.getObjects()) {
            if (!obj.isActive()) {
                continue;
            }

            int ox = (int) (obj.getX() * scale);
            int oy = (int) (obj.getY() * scale);
            int ow = Math.max(2, (int) (obj.getWidth() * scale));
            int oh = Math.max(2, (int) (obj.getHeight() * scale));

            g.setColor(resolveMarkerColor(obj.getType()));
            g.fillRect(startX + ox, startY + oy, ow, oh);
        }
        
        // 绘制视口区域 (摄像机范围)
        if (world.getCamera() != null) {
            int camX = (int) (world.getCamera().getX() * scale);
            int camY = (int) (world.getCamera().getY() * scale);
            int camW = (int) (viewW * scale);
            int camH = (int) (viewH * scale);
            g.setColor(Color.WHITE);
            g.drawRect(startX + camX, startY + camY, camW, camH);
        }
    }

    private Color resolveMarkerColor(GameObjectType type) {
        return switch (type) {
            case PLAYER -> Color.BLUE;
            case MONSTER -> Color.RED;
            case GOAL -> Color.YELLOW;
            case ITEM -> Color.GREEN;
            case WALL, BOUNDARY -> Color.DARK_GRAY;
            default -> new Color(100, 100, 100, 50);
        };
    }
}

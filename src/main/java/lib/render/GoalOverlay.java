package lib.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import lib.game.GameWorld;

/**
 * 任务目标显示遮罩层。
 * 实时显示当前的通关条件和任务进度。
 */
public final class GoalOverlay {
    private static final int MARGIN = 20;
    private static final int PADDING = 15;

    public void render(Graphics2D graphics, GameWorld world, int viewW, int viewH) {
        if (world == null || !world.isShowGoals()) {
            return;
        }

        graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        String title = "Current Objective (当前目标)";
        String goalText = resolveGoalText(world);
        String progressText = resolveProgressText(world);

        Font titleFont = new Font("SansSerif", Font.BOLD, 14);
        Font textFont = new Font("SansSerif", Font.PLAIN, 13);
        
        FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
        FontMetrics textMetrics = graphics.getFontMetrics(textFont);

        int textW = Math.max(titleMetrics.stringWidth(title), 
                   Math.max(textMetrics.stringWidth(goalText), textMetrics.stringWidth(progressText)));
        int bgW = textW + PADDING * 2;
        int bgH = titleMetrics.getHeight() + textMetrics.getHeight() * 2 + PADDING * 2 + 10;

        // 渲染在屏幕左上角，但在提示信息下方一点，或者如果是右下角提示，这里放左上角很好。
        int x = MARGIN;
        int y = MARGIN;

        // 背景 (现代简约)
        graphics.setColor(new Color(15, 20, 30, 160));
        graphics.fillRoundRect(x, y, bgW, bgH, 12, 12);
        graphics.setColor(new Color(255, 255, 255, 40));
        graphics.drawRoundRect(x, y, bgW, bgH, 12, 12);

        int currentY = y + PADDING + titleMetrics.getAscent();
        
        // 标题
        graphics.setFont(titleFont);
        graphics.setColor(new Color(100, 200, 255));
        graphics.drawString(title, x + PADDING, currentY);
        
        currentY += titleMetrics.getHeight();
        
        // 目标描述
        graphics.setFont(textFont);
        graphics.setColor(Color.WHITE);
        graphics.drawString(goalText, x + PADDING, currentY);
        
        currentY += textMetrics.getHeight();
        
        // 进度
        graphics.setColor(new Color(120, 255, 120));
        graphics.drawString(progressText, x + PADDING, currentY);
        
        // 提示
        graphics.setFont(textFont.deriveFont(10f));
        graphics.setColor(new Color(200, 200, 200, 150));
        graphics.drawString("Press [G] to hide goals", x + PADDING, y + bgH - 8);
    }

    private String resolveGoalText(GameWorld world) {
        return switch (world.getWinCondition()) {
            case REACH_GOAL -> "Reach the exit portal (到达出口)";
            case KILL_ALL_MONSTERS -> "Defeat all monsters (消灭所有怪物)";
            case KILL_TARGET_COUNT -> String.format("Defeat %d monsters (消灭 %d 个怪物)", world.getTargetKills(), world.getTargetKills());
            case COLLECT_TARGET_COUNT -> String.format("Collect %d items (收集 %d 个物品)", world.getTargetItems(), world.getTargetItems());
            case CLEAR_ALL_ITEMS -> "Collect all items (收集所有物品)";
        };
    }

    private String resolveProgressText(GameWorld world) {
        return switch (world.getWinCondition()) {
            case REACH_GOAL -> "Progress: Exploration (进行中)";
            case KILL_ALL_MONSTERS -> "Kills: " + world.getKills();
            case KILL_TARGET_COUNT -> String.format("Kills: %d / %d", world.getKills(), world.getTargetKills());
            case COLLECT_TARGET_COUNT -> String.format("Items: %d / %d", world.getItemsCollected(), world.getTargetItems());
            case CLEAR_ALL_ITEMS -> "Items Collected: " + world.getItemsCollected();
        };
    }
}

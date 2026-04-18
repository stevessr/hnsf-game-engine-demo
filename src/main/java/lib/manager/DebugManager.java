package lib.manager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import lib.game.GameWorld;
import lib.object.PlayerObject;

/**
 * 调试管理器，负责渲染调试信息和处理调试指令。
 */
public final class DebugManager {
    private long lastFrameTime = System.nanoTime();
    private int frameCount = 0;
    private int fps = 0;
    private final List<String> logMessages = new ArrayList<>();

    public void log(String message) {
        logMessages.add(message);
        if (logMessages.size() > 5) {
            logMessages.remove(0);
        }
    }

    public void render(Graphics2D g, GameWorld world, int viewW, int viewH) {
        updateFps();

        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int x = 10;
        int y = viewH - 120;

        // 绘制半透明背景
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x - 5, y - 15, 260, 110);

        g.setColor(Color.GREEN);
        g.drawString("DEBUG INFO", x, y);
        y += 15;
        g.drawString("FPS: " + fps, x, y);
        y += 15;
        
        PlayerObject player = world.findPlayer().orElse(null);
        if (player != null) {
            g.drawString(String.format("Player Pos: (%d, %d)", player.getX(), player.getY()), x, y);
            y += 15;
            g.drawString(String.format("Velocity: (%.1f, %.1f)", (double)player.getVelocityX(), (double)player.getVelocityY()), x, y);
            y += 15;
            g.drawString("Health: " + player.getHealth() + " / " + player.getMaxHealth(), x, y);
            y += 15;
        }

        g.drawString("Objects: " + world.getObjects().size(), x, y);
        y += 15;
        
        // 渲染简易日志
        g.setColor(Color.YELLOW);
        for (String msg : logMessages) {
            g.drawString("> " + msg, x, y);
            y += 15;
        }
    }

    private void updateFps() {
        frameCount++;
        long now = System.nanoTime();
        if (now - lastFrameTime >= 1_000_000_000L) {
            fps = frameCount;
            frameCount = 0;
            lastFrameTime = now;
        }
    }

    /**
     * 处理简单的调试指令 (内置修改器)。
     */
    public void executeCommand(String cmd, GameWorld world) {
        if (cmd == null || world == null) {
            return;
        }
        PlayerObject player = world.findPlayer().orElse(null);
        if (player == null) {
            return;
        }

        String[] parts = cmd.toLowerCase().split(" ");
        switch (parts[0]) {
            case "god" -> {
                player.setMaxHealth(9999);
                player.heal(9999);
                log("God mode enabled");
            }
            case "speed" -> {
                int s = parts.length > 1 ? Integer.parseInt(parts[1]) : 1200;
                player.setThrottlePower(s);
                log("Speed set to " + s);
            }
            case "heal" -> {
                player.heal(100);
                log("Healed 100 HP");
            }
            case "light" -> {
                int r = parts.length > 1 ? Integer.parseInt(parts[1]) : 500;
                player.setLightRadius(r);
                log("Light radius set to " + r);
            }
            case "tp" -> {
                if (parts.length >= 3) {
                    int tx = Integer.parseInt(parts[1]);
                    int ty = Integer.parseInt(parts[2]);
                    world.moveObject(player, tx, ty);
                    log("Teleported to " + tx + ", " + ty);
                }
            }
            default -> log("Unknown command: " + parts[0]);
        }
    }
}

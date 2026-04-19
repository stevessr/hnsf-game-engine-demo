package lib.editor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lib.game.GameWorld;
import lib.game.WinConditionType;
import lib.object.GameObject;
import lib.object.GameObjectType;

/**
 * 地图自动扫描器，用于在编辑器中快速发现关卡配置问题。
 */
public final class LevelAutoScanner {
    private LevelAutoScanner() {
    }

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    public record ScanIssue(Severity severity, String message) {
    }

    public static List<ScanIssue> scan(GameWorld world) {
        List<ScanIssue> issues = new ArrayList<>();
        if (world == null) {
            issues.add(new ScanIssue(Severity.ERROR, "世界为空，无法扫描。"));
            return issues;
        }
        if (world.getWidth() < 100 || world.getHeight() < 100) {
            issues.add(new ScanIssue(Severity.WARNING, "地图尺寸过小，可能导致对象布局与镜头表现异常。"));
        }

        int playerCount = world.getObjectsByType(GameObjectType.PLAYER).size();
        int goalCount = world.getObjectsByType(GameObjectType.GOAL).size();
        int monsterCount = world.getObjectsByType(GameObjectType.MONSTER).size();
        int itemCount = world.getObjectsByType(GameObjectType.ITEM).size();

        if (playerCount == 0) {
            issues.add(new ScanIssue(Severity.ERROR, "缺少 PLAYER：至少需要一个玩家出生点。"));
        } else if (playerCount > 1) {
            issues.add(new ScanIssue(Severity.WARNING, "存在多个 PLAYER：可能导致出生逻辑不确定。"));
        }

        WinConditionType winCondition = world.getWinCondition();
        if (winCondition == WinConditionType.REACH_GOAL && goalCount == 0) {
            issues.add(new ScanIssue(Severity.ERROR, "当前胜利条件为 REACH_GOAL，但地图中没有 GOAL。"));
        }
        if (winCondition == WinConditionType.KILL_TARGET_COUNT) {
            if (world.getTargetKills() <= 0) {
                issues.add(new ScanIssue(Severity.ERROR, "KILL_TARGET_COUNT 需要 targetKills > 0。"));
            }
            if (monsterCount < world.getTargetKills()) {
                issues.add(new ScanIssue(Severity.WARNING, "怪物数量少于 targetKills，可能无法达成目标。"));
            }
        }
        if (winCondition == WinConditionType.COLLECT_TARGET_COUNT) {
            if (world.getTargetItems() <= 0) {
                issues.add(new ScanIssue(Severity.ERROR, "COLLECT_TARGET_COUNT 需要 targetItems > 0。"));
            }
            if (itemCount < world.getTargetItems()) {
                issues.add(new ScanIssue(Severity.WARNING, "物品数量少于 targetItems，可能无法达成目标。"));
            }
        }
        if (winCondition == WinConditionType.CLEAR_ALL_ITEMS && itemCount == 0) {
            issues.add(new ScanIssue(Severity.WARNING, "胜利条件为 CLEAR_ALL_ITEMS，但地图中没有 ITEM。"));
        }

        Map<String, Integer> nameCounts = new HashMap<>();
        for (GameObject object : world.getObjects()) {
            String name = object.getName();
            nameCounts.put(name, nameCounts.getOrDefault(name, 0) + 1);
            if (object.getWidth() <= 0 || object.getHeight() <= 0) {
                issues.add(new ScanIssue(Severity.ERROR, "对象尺寸非法: " + name + " (" + object.getWidth() + "x" + object.getHeight() + ")"));
            }
            if (object.getWidth() > world.getWidth() || object.getHeight() > world.getHeight()) {
                issues.add(new ScanIssue(Severity.ERROR, "对象尺寸超出地图: " + name + " [" + object.getType() + "]"));
            }
            if (isOutOfBounds(object, world.getWidth(), world.getHeight())) {
                issues.add(new ScanIssue(Severity.WARNING, "对象越界: " + name + " [" + object.getType() + "]"));
            }
        }
        for (Map.Entry<String, Integer> entry : nameCounts.entrySet()) {
            if (entry.getValue() > 1) {
                issues.add(new ScanIssue(Severity.WARNING, "对象名称重复: " + entry.getKey() + " (x" + entry.getValue() + ")"));
            }
        }

        addSolidOverlapWarnings(world, issues);
        addTypeSummary(world, issues);
        return issues;
    }

    private static boolean isOutOfBounds(GameObject object, int worldWidth, int worldHeight) {
        int right = object.getX() + object.getWidth();
        int bottom = object.getY() + object.getHeight();
        return object.getX() < 0 || object.getY() < 0 || right > worldWidth || bottom > worldHeight;
    }

    private static void addSolidOverlapWarnings(GameWorld world, List<ScanIssue> issues) {
        List<GameObject> solidCandidates = new ArrayList<>();
        for (GameObject object : world.getObjects()) {
            if (isSolidType(object.getType())) {
                solidCandidates.add(object);
            }
        }

        int overlapCount = 0;
        for (int i = 0; i < solidCandidates.size(); i++) {
            GameObject left = solidCandidates.get(i);
            Rectangle leftRect = new Rectangle(left.getX(), left.getY(), left.getWidth(), left.getHeight());
            for (int j = i + 1; j < solidCandidates.size(); j++) {
                GameObject right = solidCandidates.get(j);
                Rectangle rightRect = new Rectangle(right.getX(), right.getY(), right.getWidth(), right.getHeight());
                if (leftRect.intersects(rightRect)) {
                    overlapCount++;
                    if (overlapCount <= 8) {
                        issues.add(new ScanIssue(Severity.WARNING, "固体对象重叠: " + left.getName() + " 与 " + right.getName()));
                    }
                }
            }
        }
        if (overlapCount > 8) {
            issues.add(new ScanIssue(Severity.WARNING, "固体对象重叠较多: 共 " + overlapCount + " 处。"));
        }
    }

    private static boolean isSolidType(GameObjectType type) {
        return type == GameObjectType.WALL || type == GameObjectType.SCENE || type == GameObjectType.BOUNDARY;
    }

    private static void addTypeSummary(GameWorld world, List<ScanIssue> issues) {
        Map<GameObjectType, Integer> counts = new EnumMap<>(GameObjectType.class);
        for (GameObject object : world.getObjects()) {
            GameObjectType type = object.getType();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        StringBuilder summary = new StringBuilder("对象统计:");
        for (Map.Entry<GameObjectType, Integer> entry : counts.entrySet()) {
            summary.append(' ').append(entry.getKey().name()).append('=').append(entry.getValue());
        }
        issues.add(new ScanIssue(Severity.INFO, summary.toString()));
    }
}

package lib.editor;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lib.game.GameWorld;
import lib.game.WinConditionType;
import lib.object.GoalObject;
import lib.object.MonsterObject;
import lib.object.PlayerObject;

class LevelAutoScannerTest {
    @Test
    void shouldReportMissingPlayerAndGoal() {
        GameWorld world = new GameWorld(400, 300);
        world.setWinCondition(WinConditionType.REACH_GOAL);

        List<LevelAutoScanner.ScanIssue> issues = LevelAutoScanner.scan(world);

        assertTrue(issues.stream().anyMatch(i -> i.severity() == LevelAutoScanner.Severity.ERROR && i.message().contains("PLAYER")));
        assertTrue(issues.stream().anyMatch(i -> i.severity() == LevelAutoScanner.Severity.ERROR && i.message().contains("GOAL")));
    }

    @Test
    void shouldPassCoreReachGoalChecksWithPlayerAndGoal() {
        GameWorld world = new GameWorld(400, 300);
        world.setWinCondition(WinConditionType.REACH_GOAL);
        world.addObject(new PlayerObject("player", 20, 20));
        world.addObject(new GoalObject("goal", 300, 200, 40, 40));

        List<LevelAutoScanner.ScanIssue> issues = LevelAutoScanner.scan(world);

        assertFalse(issues.stream().anyMatch(i -> i.severity() == LevelAutoScanner.Severity.ERROR));
    }

    @Test
    void shouldWarnWhenTargetKillsExceedsMonsterCount() {
        GameWorld world = new GameWorld(400, 300);
        world.setWinCondition(WinConditionType.KILL_TARGET_COUNT);
        world.setTargetKills(3);
        world.addObject(new PlayerObject("player", 20, 20));
        world.addObject(new MonsterObject("m1", 150, 150, 10));

        List<LevelAutoScanner.ScanIssue> issues = LevelAutoScanner.scan(world);

        assertTrue(issues.stream().anyMatch(i -> i.severity() == LevelAutoScanner.Severity.WARNING && i.message().contains("targetKills")));
    }
}

package lib.state;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lib.game.GameWorld;
import lib.object.MenuObject;

public class GameStateMachineTest {

    private DefaultGameStateMachine stateMachine;

    @BeforeEach
    public void setUp() {
        stateMachine = new DefaultGameStateMachine(GameState.MENU);
    }

    @Test
    public void testInitialState() {
        assertEquals(GameState.MENU, stateMachine.getCurrentState());
    }

    @Test
    public void testTransitions() {
        // MENU -> PLAYING is allowed
        stateMachine.transitionTo(GameState.PLAYING);
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        
        // PLAYING -> PAUSED is allowed
        stateMachine.transitionTo(GameState.PAUSED);
        assertEquals(GameState.PAUSED, stateMachine.getCurrentState());
        
        // PAUSED -> PLAYING is allowed
        stateMachine.transitionTo(GameState.PLAYING);
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        
        // Invalid transition: MENU -> PAUSED should throw
        stateMachine.transitionTo(GameState.MENU);
        assertThrows(IllegalStateException.class, () -> stateMachine.transitionTo(GameState.PAUSED));
    }

    @Test
    public void testTogglePause() {
        GameWorld world = new GameWorld(960, 540, Color.BLACK);
        stateMachine.transitionTo(GameState.PLAYING);
        
        // Toggle Pause On
        stateMachine.togglePause(world, null);
        assertEquals(GameState.PAUSED, stateMachine.getCurrentState());
        assertTrue(world.getObjects().stream().anyMatch(o -> "pause-menu".equals(o.getName())));
        
        // Toggle Pause Off
        stateMachine.togglePause(world, null);
        assertEquals(GameState.PLAYING, stateMachine.getCurrentState());
        assertFalse(world.getObjects().stream().anyMatch(o -> "pause-menu".equals(o.getName())));
    }
}

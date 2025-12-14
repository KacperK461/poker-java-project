package poker.model.players;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStateTest {

    @Test
    void testAllPlayerStates() {
        PlayerState[] states = PlayerState.values();
        assertEquals(4, states.length);
    }

    @Test
    void testActiveState() {
        PlayerState state = PlayerState.ACTIVE;
        assertNotNull(state);
        assertEquals("ACTIVE", state.name());
    }

    @Test
    void testFoldedState() {
        PlayerState state = PlayerState.FOLDED;
        assertNotNull(state);
        assertEquals("FOLDED", state.name());
    }

    @Test
    void testAllInState() {
        PlayerState state = PlayerState.ALL_IN;
        assertNotNull(state);
        assertEquals("ALL_IN", state.name());
    }

    @Test
    void testSittingOutState() {
        PlayerState state = PlayerState.SITTING_OUT;
        assertNotNull(state);
        assertEquals("SITTING_OUT", state.name());
    }

    @Test
    void testValueOf() {
        assertEquals(PlayerState.ACTIVE, PlayerState.valueOf("ACTIVE"));
        assertEquals(PlayerState.FOLDED, PlayerState.valueOf("FOLDED"));
        assertEquals(PlayerState.ALL_IN, PlayerState.valueOf("ALL_IN"));
        assertEquals(PlayerState.SITTING_OUT, PlayerState.valueOf("SITTING_OUT"));
    }

    @Test
    void testEnumOrder() {
        PlayerState[] states = PlayerState.values();
        assertEquals(PlayerState.ACTIVE, states[0]);
        assertEquals(PlayerState.FOLDED, states[1]);
        assertEquals(PlayerState.ALL_IN, states[2]);
        assertEquals(PlayerState.SITTING_OUT, states[3]);
    }

    @Test
    void testEnumEquality() {
        assertEquals(PlayerState.ACTIVE, PlayerState.ACTIVE);
        assertNotEquals(PlayerState.ACTIVE, PlayerState.FOLDED);
        assertNotEquals(PlayerState.ALL_IN, PlayerState.SITTING_OUT);
    }

    @Test
    void testToString() {
        assertEquals("ACTIVE", PlayerState.ACTIVE.toString());
        assertEquals("FOLDED", PlayerState.FOLDED.toString());
        assertEquals("ALL_IN", PlayerState.ALL_IN.toString());
        assertEquals("SITTING_OUT", PlayerState.SITTING_OUT.toString());
    }
}

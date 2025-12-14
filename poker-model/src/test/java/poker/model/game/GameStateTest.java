package poker.model.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void testAllGameStates() {
        GameState[] states = GameState.values();
        assertEquals(9, states.length);
    }

    @Test
    void testLobbyState() {
        GameState state = GameState.LOBBY;
        assertNotNull(state);
        assertEquals("LOBBY", state.name());
    }

    @Test
    void testAnteState() {
        GameState state = GameState.ANTE;
        assertNotNull(state);
        assertEquals("ANTE", state.name());
    }

    @Test
    void testDealState() {
        GameState state = GameState.DEAL;
        assertNotNull(state);
        assertEquals("DEAL", state.name());
    }

    @Test
    void testBet1State() {
        GameState state = GameState.BET1;
        assertNotNull(state);
        assertEquals("BET1", state.name());
    }

    @Test
    void testDrawState() {
        GameState state = GameState.DRAW;
        assertNotNull(state);
        assertEquals("DRAW", state.name());
    }

    @Test
    void testBet2State() {
        GameState state = GameState.BET2;
        assertNotNull(state);
        assertEquals("BET2", state.name());
    }

    @Test
    void testShowdownState() {
        GameState state = GameState.SHOWDOWN;
        assertNotNull(state);
        assertEquals("SHOWDOWN", state.name());
    }

    @Test
    void testPayoutState() {
        GameState state = GameState.PAYOUT;
        assertNotNull(state);
        assertEquals("PAYOUT", state.name());
    }

    @Test
    void testEndState() {
        GameState state = GameState.END;
        assertNotNull(state);
        assertEquals("END", state.name());
    }

    @Test
    void testValueOf() {
        assertEquals(GameState.LOBBY, GameState.valueOf("LOBBY"));
        assertEquals(GameState.ANTE, GameState.valueOf("ANTE"));
        assertEquals(GameState.DEAL, GameState.valueOf("DEAL"));
        assertEquals(GameState.BET1, GameState.valueOf("BET1"));
        assertEquals(GameState.DRAW, GameState.valueOf("DRAW"));
        assertEquals(GameState.BET2, GameState.valueOf("BET2"));
        assertEquals(GameState.SHOWDOWN, GameState.valueOf("SHOWDOWN"));
        assertEquals(GameState.PAYOUT, GameState.valueOf("PAYOUT"));
        assertEquals(GameState.END, GameState.valueOf("END"));
    }

    @Test
    void testEnumOrder() {
        GameState[] states = GameState.values();
        assertEquals(GameState.LOBBY, states[0]);
        assertEquals(GameState.ANTE, states[1]);
        assertEquals(GameState.DEAL, states[2]);
        assertEquals(GameState.BET1, states[3]);
        assertEquals(GameState.DRAW, states[4]);
        assertEquals(GameState.BET2, states[5]);
        assertEquals(GameState.SHOWDOWN, states[6]);
        assertEquals(GameState.PAYOUT, states[7]);
        assertEquals(GameState.END, states[8]);
    }
}

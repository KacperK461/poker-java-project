package poker.model.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    void testValidGameConfig() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .maxDraw(3)
            .minPlayers(2)
            .maxPlayers(4)
            .initialChips(1000)
            .build();
        
        assertDoesNotThrow(config::validate);
        assertEquals(10, config.getAnte());
        assertEquals(20, config.getFixedBet());
        assertEquals(3, config.getMaxDraw());
        assertEquals(2, config.getMinPlayers());
        assertEquals(4, config.getMaxPlayers());
        assertEquals(1000, config.getInitialChips());
    }

    @Test
    void testValidGameConfigWithDefaults() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .build();
        
        assertDoesNotThrow(config::validate);
        assertEquals(10, config.getAnte());
        assertEquals(20, config.getFixedBet());
        assertEquals(3, config.getMaxDraw()); // default
        assertEquals(2, config.getMinPlayers()); // default
        assertEquals(4, config.getMaxPlayers()); // default
        assertEquals(1000, config.getInitialChips()); // default
    }

    @Test
    void testInvalidAnteNegative() {
        GameConfig config = GameConfig.builder()
            .ante(-10)
            .fixedBet(20)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Ante cannot be negative"));
    }

    @Test
    void testInvalidFixedBetZero() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(0)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Fixed bet must be positive"));
    }

    @Test
    void testInvalidFixedBetNegative() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(-20)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Fixed bet must be positive"));
    }

    @Test
    void testInvalidMaxDrawNegative() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .maxDraw(-1)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Max draw must be between 0 and 5"));
    }

    @Test
    void testInvalidMaxDrawTooLarge() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .maxDraw(6)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Max draw must be between 0 and 5"));
    }

    @Test
    void testInvalidMinPlayersTooLow() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .minPlayers(1)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Invalid player limits"));
    }

    @Test
    void testInvalidMinPlayersGreaterThanMax() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .minPlayers(5)
            .maxPlayers(4)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Invalid player limits"));
    }

    @Test
    void testInvalidInitialChipsZero() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .initialChips(0)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Initial chips must be positive"));
    }

    @Test
    void testInvalidInitialChipsNegative() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .initialChips(-100)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            config::validate
        );
        assertTrue(exception.getMessage().contains("Initial chips must be positive"));
    }

    @Test
    void testValidMaxDrawZero() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .maxDraw(0)
            .build();
        
        assertDoesNotThrow(config::validate);
        assertEquals(0, config.getMaxDraw());
    }

    @Test
    void testValidMaxDrawFive() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .maxDraw(5)
            .build();
        
        assertDoesNotThrow(config::validate);
        assertEquals(5, config.getMaxDraw());
    }

    @Test
    void testValidAnteZero() {
        GameConfig config = GameConfig.builder()
            .ante(0)
            .fixedBet(20)
            .build();
        
        assertDoesNotThrow(config::validate);
        assertEquals(0, config.getAnte());
    }

    @Test
    void testValidMinEqualsMax() {
        GameConfig config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .minPlayers(3)
            .maxPlayers(3)
            .build();
        
        assertDoesNotThrow(config::validate);
        assertEquals(3, config.getMinPlayers());
        assertEquals(3, config.getMaxPlayers());
    }

    @Test
    void testBuilderWithAllCustomValues() {
        GameConfig config = GameConfig.builder()
            .ante(5)
            .fixedBet(10)
            .maxDraw(2)
            .minPlayers(3)
            .maxPlayers(6)
            .initialChips(500)
            .build();
        
        assertDoesNotThrow(config::validate);
        assertEquals(5, config.getAnte());
        assertEquals(10, config.getFixedBet());
        assertEquals(2, config.getMaxDraw());
        assertEquals(3, config.getMinPlayers());
        assertEquals(6, config.getMaxPlayers());
        assertEquals(500, config.getInitialChips());
    }
}

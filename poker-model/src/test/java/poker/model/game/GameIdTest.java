package poker.model.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameIdTest {

    @Test
    void testGenerateGameId() {
        GameId gameId = GameId.generate();
        
        assertNotNull(gameId);
        assertNotNull(gameId.getId());
        assertEquals(12, gameId.getId().length());
    }

    @Test
    void testGenerateUnique() {
        GameId id1 = GameId.generate();
        GameId id2 = GameId.generate();
        
        assertNotEquals(id1, id2);
        assertNotEquals(id1.getId(), id2.getId());
    }

    @Test
    void testOfValidId() {
        GameId gameId = GameId.of("abc123def456");
        
        assertNotNull(gameId);
        assertEquals("abc123def456", gameId.getId());
    }

    @Test
    void testOfNullId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> GameId.of(null)
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testOfEmptyId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> GameId.of("")
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testOfBlankId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> GameId.of("   ")
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testEquals() {
        GameId id1 = GameId.of("test123");
        GameId id2 = GameId.of("test123");
        GameId id3 = GameId.of("test456");
        
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }

    @Test
    void testHashCode() {
        GameId id1 = GameId.of("test123");
        GameId id2 = GameId.of("test123");
        
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testToString() {
        GameId gameId = GameId.of("test123");
        String str = gameId.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("test123"));
    }

    @Test
    void testGetId() {
        GameId gameId = GameId.of("game789");
        
        assertEquals("game789", gameId.getId());
    }
}

package poker.model.players;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerIdTest {

    @Test
    void testGeneratePlayerId() {
        PlayerId playerId = PlayerId.generate();
        
        assertNotNull(playerId);
        assertNotNull(playerId.getId());
        assertEquals(8, playerId.getId().length());
    }

    @Test
    void testGenerateUnique() {
        PlayerId id1 = PlayerId.generate();
        PlayerId id2 = PlayerId.generate();
        
        assertNotEquals(id1, id2);
        assertNotEquals(id1.getId(), id2.getId());
    }

    @Test
    void testOfValidId() {
        PlayerId playerId = PlayerId.of("player01");
        
        assertNotNull(playerId);
        assertEquals("player01", playerId.getId());
    }

    @Test
    void testOfNullId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PlayerId.of(null)
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testOfEmptyId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PlayerId.of("")
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testOfBlankId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PlayerId.of("   ")
        );
        
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    void testEquals() {
        PlayerId id1 = PlayerId.of("player01");
        PlayerId id2 = PlayerId.of("player01");
        PlayerId id3 = PlayerId.of("player02");
        
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }

    @Test
    void testHashCode() {
        PlayerId id1 = PlayerId.of("player01");
        PlayerId id2 = PlayerId.of("player01");
        
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testToString() {
        PlayerId playerId = PlayerId.of("player01");
        String str = playerId.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("player01"));
    }

    @Test
    void testGetId() {
        PlayerId playerId = PlayerId.of("player99");
        
        assertEquals("player99", playerId.getId());
    }
}

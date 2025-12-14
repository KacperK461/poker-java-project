package poker.model.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTest {

    @Test
    void testProtocolException() {
        ProtocolException exception = new ProtocolException("TEST_CODE", "Test message");
        
        assertEquals("TEST_CODE", exception.getCode());
        assertEquals("Test message", exception.getMessage());
    }

    @Test
    void testProtocolExceptionWithNullMessage() {
        ProtocolException exception = new ProtocolException("CODE", null);
        
        assertEquals("CODE", exception.getCode());
        assertNull(exception.getMessage());
    }

    @Test
    void testSecurityException() {
        SecurityException exception = new SecurityException("SEC_001", "Security violation");
        
        assertEquals("SEC_001", exception.getCode());
        assertEquals("Security violation", exception.getMessage());
    }

    @Test
    void testOutOfTurnException() {
        OutOfTurnException exception = new OutOfTurnException();
        
        assertEquals("OUT_OF_TURN", exception.getCode());
        assertEquals("Not your turn.", exception.getMessage());
    }

    @Test
    void testNotEnoughChipsException() {
        NotEnoughChipsException exception = new NotEnoughChipsException(100, 50);
        
        assertEquals("NOT_ENOUGH_CHIPS", exception.getCode());
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().contains("50"));
    }

    @Test
    void testStateMismatchException() {
        StateMismatchException exception = new StateMismatchException("WAITING", "PLAYING");
        
        assertEquals("STATE_MISMATCH", exception.getCode());
        assertTrue(exception.getMessage().contains("WAITING"));
        assertTrue(exception.getMessage().contains("PLAYING"));
    }

    @Test
    void testInvalidMoveException() {
        InvalidMoveException exception = new InvalidMoveException("INVALID", "Invalid move");
        
        assertEquals("INVALID", exception.getCode());
        assertEquals("Invalid move", exception.getMessage());
    }

    @Test
    void testIllegalDrawException() {
        IllegalDrawException exception = new IllegalDrawException("Invalid draw");
        
        assertEquals("Invalid draw", exception.getMessage());
    }

    @Test
    void testProtocolExceptionFullMessage() {
        ProtocolException exception = new ProtocolException("ERROR_CODE", "Detailed error message");
        String message = exception.getMessage();
        
        assertNotNull(message);
        assertEquals("Detailed error message", message);
        assertEquals("ERROR_CODE", exception.getCode());
    }

    @Test
    void testExceptionInheritance() {
        ProtocolException protocolException = new ProtocolException("CODE", "Message");
        assertTrue(protocolException instanceof RuntimeException);
        
        SecurityException securityException = new SecurityException("CODE", "Message");
        assertTrue(securityException instanceof RuntimeException);
        
        OutOfTurnException outOfTurnException = new OutOfTurnException();
        assertTrue(outOfTurnException instanceof RuntimeException);
        assertTrue(outOfTurnException instanceof InvalidMoveException);
        
        NotEnoughChipsException notEnoughChipsException = new NotEnoughChipsException(100, 50);
        assertTrue(notEnoughChipsException instanceof RuntimeException);
        assertTrue(notEnoughChipsException instanceof InvalidMoveException);
        
        StateMismatchException stateMismatchException = new StateMismatchException("A", "B");
        assertTrue(stateMismatchException instanceof RuntimeException);
        assertTrue(stateMismatchException instanceof InvalidMoveException);
        
        InvalidMoveException invalidMoveException = new InvalidMoveException("CODE", "Message");
        assertTrue(invalidMoveException instanceof RuntimeException);
        
        IllegalDrawException illegalDrawException = new IllegalDrawException("Message");
        assertTrue(illegalDrawException instanceof RuntimeException);
        assertTrue(illegalDrawException instanceof InvalidMoveException);
    }
}

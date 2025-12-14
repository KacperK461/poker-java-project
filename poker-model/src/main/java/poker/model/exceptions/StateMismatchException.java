package poker.model.exceptions;

/**
 * Exception thrown when game state doesn't match expected state.
 */
public class StateMismatchException extends InvalidMoveException {
    public StateMismatchException(String expected, String actual) {
        super("STATE_MISMATCH", 
            String.format("Invalid state: expected %s, actual %s", expected, actual));
    }
}

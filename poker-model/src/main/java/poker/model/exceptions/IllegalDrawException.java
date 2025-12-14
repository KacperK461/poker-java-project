package poker.model.exceptions;

/**
 * Exception thrown when an illegal draw is attempted.
 */
public class IllegalDrawException extends InvalidMoveException {
    public IllegalDrawException(String message) {
        super("ILLEGAL_DRAW", message);
    }
}

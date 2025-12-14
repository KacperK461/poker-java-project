package poker.model.exceptions;

/**
 * Base exception for invalid moves in the poker game.
 */
public class InvalidMoveException extends RuntimeException {
    private final String code;

    public InvalidMoveException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

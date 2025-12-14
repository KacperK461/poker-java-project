package poker.model.exceptions;

/**
 * Exception thrown when a player tries to act out of turn.
 */
public class OutOfTurnException extends InvalidMoveException {
    public OutOfTurnException() {
        super("OUT_OF_TURN", "Not your turn.");
    }
}

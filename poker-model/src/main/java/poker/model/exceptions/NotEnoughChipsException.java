package poker.model.exceptions;

/**
 * Exception thrown when a player doesn't have enough chips for an action.
 */
public class NotEnoughChipsException extends InvalidMoveException {
    public NotEnoughChipsException(int required, int available) {
        super("NOT_ENOUGH_CHIPS", 
            String.format("Not enough chips: required %d, available %d", required, available));
    }
}

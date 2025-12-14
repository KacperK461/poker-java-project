package poker.model.players;

/**
 * Enum representing player states during the game.
 */
public enum PlayerState {
    /** Player is actively playing */
    ACTIVE,
    
    /** Player has folded this round */
    FOLDED,
    
    /** Player is all-in (no more chips to bet) */
    ALL_IN,
    
    /** Player is sitting out/disconnected */
    SITTING_OUT
}

package poker.model.game;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for a poker game.
 */
@Getter
@Builder
public class GameConfig {
    /** Ante amount required from each player */
    private final int ante;
    
    /** Fixed bet amount for limit games */
    private final int fixedBet;
    
    /** Maximum number of cards that can be drawn */
    @Builder.Default
    private final int maxDraw = 3;
    
    /** Minimum number of players */
    @Builder.Default
    private final int minPlayers = 2;
    
    /** Maximum number of players */
    @Builder.Default
    private final int maxPlayers = 4;
    
    /** Initial chips for each player */
    @Builder.Default
    private final int initialChips = 1000;

    public void validate() {
        if (ante < 0) {
            throw new IllegalArgumentException("Ante cannot be negative");
        }
        if (fixedBet <= 0) {
            throw new IllegalArgumentException("Fixed bet must be positive");
        }
        if (maxDraw < 0 || maxDraw > 5) {
            throw new IllegalArgumentException("Max draw must be between 0 and 5");
        }
        if (minPlayers < 2 || minPlayers > maxPlayers) {
            throw new IllegalArgumentException("Invalid player limits");
        }
        if (initialChips <= 0) {
            throw new IllegalArgumentException("Initial chips must be positive");
        }
    }
}

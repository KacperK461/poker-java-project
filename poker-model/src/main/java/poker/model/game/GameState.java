package poker.model.game;

/**
 * Enum representing the phases of a poker game.
 */
public enum GameState {
    /** Waiting for players to join */
    LOBBY,
    
    /** Collecting ante from players */
    ANTE,
    
    /** Dealing initial cards */
    DEAL,
    
    /** First betting round */
    BET1,
    
    /** Drawing/exchanging cards */
    DRAW,
    
    /** Second betting round */
    BET2,
    
    /** Revealing hands and determining winner */
    SHOWDOWN,
    
    /** Distributing winnings */
    PAYOUT,
    
    /** Game ended */
    END
}

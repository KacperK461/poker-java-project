package poker.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import poker.model.exceptions.*;
import poker.model.players.Player;
import poker.model.players.PlayerId;
import poker.model.players.PlayerState;

import static org.junit.jupiter.api.Assertions.*;

class PokerGameTest {

    private PokerGame game;
    private GameConfig config;
    private GameId gameId;

    @BeforeEach
    void setUp() {
        gameId = GameId.generate();
        config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .minPlayers(2)
            .maxPlayers(4)
            .initialChips(1000)
            .build();
        game = new PokerGame(gameId, config);
    }

    @Test
    void testGameCreation() {
        assertEquals(gameId, game.getGameId());
        assertEquals(GameState.LOBBY, game.getState());
        assertEquals(0, game.getPlayerCount());
    }

    @Test
    void testAddPlayer() {
        PlayerId playerId = PlayerId.of("PLAYER1");
        Player player = game.addPlayer(playerId, "Alice");

        assertNotNull(player);
        assertEquals("Alice", player.getName());
        assertEquals(1000, player.getChips());
        assertEquals(1, game.getPlayerCount());
    }

    @Test
    void testAddPlayerWhenGameStarted() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();

        PlayerId p3 = PlayerId.of("P3");
        assertThrows(StateMismatchException.class, 
            () -> game.addPlayer(p3, "Charlie"));
    }

    @Test
    void testAddTooManyPlayers() {
        for (int i = 0; i < 4; i++) {
            game.addPlayer(PlayerId.of("P" + i), "Player" + i);
        }

        assertThrows(InvalidMoveException.class, 
            () -> game.addPlayer(PlayerId.of("P5"), "Player5"));
    }

    @Test
    void testStartGameWithNotEnoughPlayers() {
        PlayerId p1 = PlayerId.of("P1");
        game.addPlayer(p1, "Alice");

        assertThrows(InvalidMoveException.class, game::startGame);
    }

    @Test
    void testStartGame() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();

        assertEquals(GameState.ANTE, game.getState());
    }

    @Test
    void testCollectAnte() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();

        assertEquals(GameState.DEAL, game.getState());
        assertEquals(20, game.getPot()); // 2 players * 10 ante
        assertEquals(990, game.getPlayer(p1).getChips());
    }

    @Test
    void testDealInitialCards() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();

        assertEquals(GameState.BET1, game.getState());
        assertEquals(5, game.getPlayer(p1).getHandSize());
        assertEquals(5, game.getPlayer(p2).getHandSize());
    }

    @Test
    void testFoldOutOfTurn() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();

        // If it's not p2's turn, folding should throw exception
        PlayerId currentTurn = game.getCurrentTurn();
        PlayerId notCurrentTurn = currentTurn.equals(p1) ? p2 : p1;

        assertThrows(OutOfTurnException.class, 
            () -> game.fold(notCurrentTurn));
    }

    @Test
    void testCheckWhenBetExists() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();

        // First player raises
        PlayerId firstPlayer = game.getCurrentTurn();
        game.raise(firstPlayer, 20);

        // Second player cannot check when there's a bet
        PlayerId secondPlayer = game.getCurrentTurn();
        assertThrows(InvalidMoveException.class, 
            () -> game.check(secondPlayer));
    }

    @Test
    void testRemovePlayer() {
        PlayerId p1 = PlayerId.of("P1");
        game.addPlayer(p1, "Alice");
        
        assertEquals(1, game.getPlayerCount());
        
        game.removePlayer(p1);
        assertEquals(0, game.getPlayerCount());
    }

    @Test
    void testRemoveNonExistentPlayer() {
        PlayerId p1 = PlayerId.of("P1");
        assertThrows(InvalidMoveException.class, 
            () -> game.removePlayer(p1));
    }

    @Test
    void testGetPlayer() {
        PlayerId p1 = PlayerId.of("P1");
        Player player = game.addPlayer(p1, "Alice");
        
        assertEquals(player, game.getPlayer(p1));
    }

    @Test
    void testGetAllPlayers() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        
        assertEquals(2, game.getAllPlayers().size());
    }

    @Test
    void testInvalidConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            GameConfig config = GameConfig.builder()
                .ante(-1)
                .fixedBet(20)
                .build();
            config.validate();
        });
    }

    @Test
    void testAddPlayerDuplicate() {
        PlayerId p1 = PlayerId.of("P1");
        game.addPlayer(p1, "Alice");
        
        assertThrows(InvalidMoveException.class, 
            () -> game.addPlayer(p1, "Alice"));
    }

    @Test
    void testRemovePlayerReassignsDealer() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        
        // P1 is dealer (first player)
        assertEquals(p1, game.getDealerId());
        
        // Remove dealer
        game.removePlayer(p1);
        
        // Dealer should be reassigned to p2
        assertEquals(p2, game.getDealerId());
    }

    @Test
    void testStartGameTwice() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        
        assertThrows(StateMismatchException.class, game::startGame);
    }

    @Test
    void testBettingWithInsufficientChips() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        // Create game with low initial chips
        GameConfig lowChipConfig = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .minPlayers(2)
            .maxPlayers(4)
            .initialChips(15)
            .build();
        PokerGame lowChipGame = new PokerGame(gameId, lowChipConfig);
        
        lowChipGame.addPlayer(p1, "Alice");
        lowChipGame.addPlayer(p2, "Bob");
        lowChipGame.startGame();
        lowChipGame.collectAnte();
        lowChipGame.dealInitialCards();
        
        // After ante (10), players have 5 chips left, cannot raise by 20
        PlayerId currentTurn = lowChipGame.getCurrentTurn();
        assertThrows(NotEnoughChipsException.class, 
            () -> lowChipGame.raise(currentTurn, 20));
    }

    @Test
    void testCallGoesAllIn() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        // Create game with very low chips so second player goes all-in on call
        GameConfig lowChipConfig = GameConfig.builder()
            .ante(5)
            .fixedBet(20)
            .minPlayers(2)
            .maxPlayers(4)
            .initialChips(30)
            .build();
        PokerGame lowChipGame = new PokerGame(gameId, lowChipConfig);
        
        lowChipGame.addPlayer(p1, "Alice");
        lowChipGame.addPlayer(p2, "Bob");
        lowChipGame.startGame();
        lowChipGame.collectAnte();
        lowChipGame.dealInitialCards();
        
        // After ante (5), each player has 25 chips
        PlayerId firstPlayer = lowChipGame.getCurrentTurn();
        PlayerId secondPlayer = firstPlayer.equals(p1) ? p2 : p1;
        
        // First player raises by 20, now has bet=20, chips=5
        lowChipGame.raise(firstPlayer, 20);
        
        // Second player tries to call (needs 20) but only has 25 chips
        // This is a normal call, not all-in since player has enough
        lowChipGame.call(secondPlayer);
        
        // Player should have 5 chips left, not all-in
        assertEquals(5, lowChipGame.getPlayer(secondPlayer).getChips());
    }

    @Test
    void testRaiseTooSmall() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();
        
        PlayerId currentTurn = game.getCurrentTurn();
        
        // Try to raise by less than fixed bet
        assertThrows(InvalidMoveException.class, 
            () -> game.raise(currentTurn, 5));
    }

    @Test
    void testFoldWhenOnlyTwoPlayersLeft() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();
        
        PlayerId firstPlayer = game.getCurrentTurn();
        
        // First player folds, should skip to showdown
        game.fold(firstPlayer);
        
        assertEquals(GameState.SHOWDOWN, game.getState());
    }

    @Test
    void testCollectAnteWithInsufficientChips() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        // Create game where player can't afford ante
        GameConfig highAnteConfig = GameConfig.builder()
            .ante(100)
            .fixedBet(20)
            .minPlayers(2)
            .maxPlayers(4)
            .initialChips(50)
            .build();
        PokerGame highAnteGame = new PokerGame(gameId, highAnteConfig);
        
        highAnteGame.addPlayer(p1, "Alice");
        highAnteGame.addPlayer(p2, "Bob");
        highAnteGame.startGame();
        highAnteGame.collectAnte();
        
        // Player without enough chips should be SITTING_OUT
        assertEquals(PlayerState.SITTING_OUT, highAnteGame.getPlayer(p1).getState());
    }

    @Test
    void testDrawInvalidIndices() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();
        
        // Complete first betting round
        PlayerId firstPlayer = game.getCurrentTurn();
        game.check(firstPlayer);
        PlayerId secondPlayer = game.getCurrentTurn();
        game.check(secondPlayer);
        
        // Should now be in DRAW state
        assertEquals(GameState.DRAW, game.getState());
        
        PlayerId currentTurn = game.getCurrentTurn();
        
        // Invalid index (out of range)
        assertThrows(IllegalDrawException.class, 
            () -> game.draw(currentTurn, java.util.Arrays.asList(5)));
        
        // Duplicate indices
        assertThrows(IllegalDrawException.class, 
            () -> game.draw(currentTurn, java.util.Arrays.asList(0, 0)));
    }

    @Test
    void testDrawTooManyCards() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        // Create config with max draw of 3
        GameConfig limitedDrawConfig = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .minPlayers(2)
            .maxPlayers(4)
            .initialChips(1000)
            .maxDraw(3)
            .build();
        PokerGame limitedDrawGame = new PokerGame(gameId, limitedDrawConfig);
        
        limitedDrawGame.addPlayer(p1, "Alice");
        limitedDrawGame.addPlayer(p2, "Bob");
        limitedDrawGame.startGame();
        limitedDrawGame.collectAnte();
        limitedDrawGame.dealInitialCards();
        
        // Complete first betting round
        PlayerId firstPlayer = limitedDrawGame.getCurrentTurn();
        limitedDrawGame.check(firstPlayer);
        PlayerId secondPlayer = limitedDrawGame.getCurrentTurn();
        limitedDrawGame.check(secondPlayer);
        
        assertEquals(GameState.DRAW, limitedDrawGame.getState());
        PlayerId currentTurn = limitedDrawGame.getCurrentTurn();
        
        // Try to draw 4 cards when max is 3
        assertThrows(IllegalDrawException.class, 
            () -> limitedDrawGame.draw(currentTurn, java.util.Arrays.asList(0, 1, 2, 3)));
    }

    @Test
    void testShowdownAndDistributePot() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();
        
        // Complete first betting round
        PlayerId firstPlayer = game.getCurrentTurn();
        game.check(firstPlayer);
        PlayerId secondPlayer = game.getCurrentTurn();
        game.check(secondPlayer);
        
        // Should be in DRAW state
        assertEquals(GameState.DRAW, game.getState());
        
        // Complete draw phase  
        firstPlayer = game.getCurrentTurn();
        game.draw(firstPlayer, java.util.Arrays.asList());
        secondPlayer = game.getCurrentTurn();
        game.draw(secondPlayer, java.util.Arrays.asList());
        
        // Should be in BET2 state
        assertEquals(GameState.BET2, game.getState());
        
        // Complete second betting round
        firstPlayer = game.getCurrentTurn();
        game.check(firstPlayer);
        secondPlayer = game.getCurrentTurn();
        game.check(secondPlayer);
        
        // Should be in SHOWDOWN state
        assertEquals(GameState.SHOWDOWN, game.getState());
        
        // Showdown
        var rankings = game.showdown();
        assertEquals(2, rankings.size());
        
        // Distribute pot
        var payouts = game.distributePot(rankings);
        assertFalse(payouts.isEmpty());
        assertEquals(GameState.LOBBY, game.getState());
        assertEquals(0, game.getPot());
    }

    @Test
    void testDistributePotTransitionsToLobby() {
        PlayerId p1 = PlayerId.of("P1");
        PlayerId p2 = PlayerId.of("P2");
        
        game.addPlayer(p1, "Alice");
        game.addPlayer(p2, "Bob");
        game.startGame();
        game.collectAnte();
        game.dealInitialCards();
        
        // One player folds
        game.fold(game.getCurrentTurn());
        
        // Should be in showdown
        assertEquals(GameState.SHOWDOWN, game.getState());
        
        var rankings = game.showdown();
        var payouts = game.distributePot(rankings);
        
        // Should transition back to LOBBY for next round
        assertEquals(GameState.LOBBY, game.getState());
        assertEquals(0, game.getPot());
        assertFalse(payouts.isEmpty());
    }
}

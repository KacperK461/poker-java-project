package poker.model.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import poker.model.exceptions.*;
import poker.model.players.Player;
import poker.model.players.PlayerId;

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
}

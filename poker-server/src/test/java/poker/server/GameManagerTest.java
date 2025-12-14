package poker.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import poker.model.game.GameConfig;
import poker.model.game.GameId;
import poker.model.game.PokerGame;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTest {
    private GameManager gameManager;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        gameManager = new GameManager();
        config = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .maxDraw(3)
            .minPlayers(2)
            .maxPlayers(4)
            .initialChips(1000)
            .build();
    }

    @Test
    void testCreateGame() {
        GameId gameId = gameManager.createGame(config);
        
        assertNotNull(gameId);
        assertEquals(1, gameManager.getGameCount());
    }

    @Test
    void testCreateMultipleGames() {
        GameId gameId1 = gameManager.createGame(config);
        GameId gameId2 = gameManager.createGame(config);
        GameId gameId3 = gameManager.createGame(config);
        
        assertNotNull(gameId1);
        assertNotNull(gameId2);
        assertNotNull(gameId3);
        assertNotEquals(gameId1, gameId2);
        assertNotEquals(gameId1, gameId3);
        assertNotEquals(gameId2, gameId3);
        assertEquals(3, gameManager.getGameCount());
    }

    @Test
    void testGetGame() {
        GameId gameId = gameManager.createGame(config);
        
        PokerGame game = gameManager.getGame(gameId);
        
        assertNotNull(game);
        assertEquals(gameId, game.getGameId());
    }

    @Test
    void testGetGameThrowsExceptionForNonExistentGame() {
        GameId nonExistentGameId = GameId.generate();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> gameManager.getGame(nonExistentGameId)
        );
        
        assertTrue(exception.getMessage().contains("Game not found"));
        assertTrue(exception.getMessage().contains(nonExistentGameId.getId()));
    }

    @Test
    void testRemoveGame() {
        GameId gameId = gameManager.createGame(config);
        assertEquals(1, gameManager.getGameCount());
        
        gameManager.removeGame(gameId);
        
        assertEquals(0, gameManager.getGameCount());
        assertThrows(IllegalArgumentException.class, () -> gameManager.getGame(gameId));
    }

    @Test
    void testRemoveNonExistentGame() {
        GameId nonExistentGameId = GameId.generate();
        
        // Should not throw exception
        assertDoesNotThrow(() -> gameManager.removeGame(nonExistentGameId));
        assertEquals(0, gameManager.getGameCount());
    }

    @Test
    void testGetGameCount() {
        assertEquals(0, gameManager.getGameCount());
        
        gameManager.createGame(config);
        assertEquals(1, gameManager.getGameCount());
        
        gameManager.createGame(config);
        assertEquals(2, gameManager.getGameCount());
        
        gameManager.createGame(config);
        assertEquals(3, gameManager.getGameCount());
    }

    @Test
    void testGameCountAfterRemoval() {
        GameId gameId1 = gameManager.createGame(config);
        GameId gameId2 = gameManager.createGame(config);
        GameId gameId3 = gameManager.createGame(config);
        
        assertEquals(3, gameManager.getGameCount());
        
        gameManager.removeGame(gameId2);
        assertEquals(2, gameManager.getGameCount());
        
        gameManager.removeGame(gameId1);
        assertEquals(1, gameManager.getGameCount());
        
        gameManager.removeGame(gameId3);
        assertEquals(0, gameManager.getGameCount());
    }

    @Test
    void testMultipleGamesIndependent() {
        GameId gameId1 = gameManager.createGame(config);
        GameId gameId2 = gameManager.createGame(config);
        
        PokerGame game1 = gameManager.getGame(gameId1);
        PokerGame game2 = gameManager.getGame(gameId2);
        
        assertNotSame(game1, game2);
        assertEquals(gameId1, game1.getGameId());
        assertEquals(gameId2, game2.getGameId());
    }
}

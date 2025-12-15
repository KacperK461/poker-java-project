package poker.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import poker.model.game.GameConfig;
import poker.model.game.GameId;
import poker.model.game.PokerGame;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for GameManager to improve coverage.
 */
class GameManagerExtendedTest {

    private GameManager gameManager;

    @BeforeEach
    void setUp() {
        gameManager = new GameManager();
    }

    @Test
    void testMultipleGamesCreation() {
        GameConfig config1 = GameConfig.builder().ante(10).fixedBet(20).build();
        GameConfig config2 = GameConfig.builder().ante(5).fixedBet(10).build();
        GameConfig config3 = GameConfig.builder().ante(15).fixedBet(30).build();

        GameId game1 = gameManager.createGame(config1);
        GameId game2 = gameManager.createGame(config2);
        GameId game3 = gameManager.createGame(config3);

        assertEquals(3, gameManager.getGameCount());
        assertNotEquals(game1, game2);
        assertNotEquals(game2, game3);
        assertNotEquals(game1, game3);
    }

    @Test
    void testGetGameReturnsCorrectGame() {
        GameConfig config = GameConfig.builder().ante(10).fixedBet(20).build();
        GameId gameId = gameManager.createGame(config);

        PokerGame game = gameManager.getGame(gameId);
        assertNotNull(game);
        assertEquals(gameId, game.getGameId());
        assertEquals(10, game.getConfig().getAnte());
        assertEquals(20, game.getConfig().getFixedBet());
    }

    @Test
    void testGetGameThrowsForNonexistentGame() {
        GameId fakeId = GameId.of("nonexistent-id-12345");
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            gameManager.getGame(fakeId);
        });
        
        assertTrue(exception.getMessage().contains("Game not found"));
        assertTrue(exception.getMessage().contains(fakeId.getId()));
    }

    @Test
    void testRemoveGameDecreasesCount() {
        GameConfig config = GameConfig.builder().ante(10).fixedBet(20).build();
        GameId gameId = gameManager.createGame(config);
        
        assertEquals(1, gameManager.getGameCount());
        
        gameManager.removeGame(gameId);
        
        assertEquals(0, gameManager.getGameCount());
    }

    @Test
    void testRemoveGameMakesItInaccessible() {
        GameConfig config = GameConfig.builder().ante(10).fixedBet(20).build();
        GameId gameId = gameManager.createGame(config);
        
        gameManager.removeGame(gameId);
        
        assertThrows(IllegalArgumentException.class, () -> {
            gameManager.getGame(gameId);
        });
    }

    @Test
    void testRemoveNonexistentGameDoesNotThrow() {
        GameId fakeId = GameId.of("nonexistent-id-12345");
        
        assertDoesNotThrow(() -> {
            gameManager.removeGame(fakeId);
        });
        
        assertEquals(0, gameManager.getGameCount());
    }

    @Test
    void testRemoveOneOfMultipleGames() {
        GameConfig config = GameConfig.builder().ante(10).fixedBet(20).build();
        GameId game1 = gameManager.createGame(config);
        GameId game2 = gameManager.createGame(config);
        GameId game3 = gameManager.createGame(config);
        
        assertEquals(3, gameManager.getGameCount());
        
        gameManager.removeGame(game2);
        
        assertEquals(2, gameManager.getGameCount());
        assertDoesNotThrow(() -> gameManager.getGame(game1));
        assertThrows(IllegalArgumentException.class, () -> gameManager.getGame(game2));
        assertDoesNotThrow(() -> gameManager.getGame(game3));
    }

    @Test
    void testGameCountStartsAtZero() {
        assertEquals(0, gameManager.getGameCount());
    }

    @Test
    void testCreateGameWithDifferentConfigs() {
        GameConfig smallStakes = GameConfig.builder().ante(1).fixedBet(2).build();
        GameConfig mediumStakes = GameConfig.builder().ante(10).fixedBet(20).build();
        GameConfig highStakes = GameConfig.builder().ante(100).fixedBet(200).build();

        GameId small = gameManager.createGame(smallStakes);
        GameId medium = gameManager.createGame(mediumStakes);
        GameId high = gameManager.createGame(highStakes);

        assertEquals(1, gameManager.getGame(small).getConfig().getAnte());
        assertEquals(10, gameManager.getGame(medium).getConfig().getAnte());
        assertEquals(100, gameManager.getGame(high).getConfig().getAnte());
    }

    @Test
    void testConcurrentGameOperations() throws InterruptedException {
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                GameConfig config = GameConfig.builder().ante(10).fixedBet(20).build();
                gameManager.createGame(config);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(threadCount, gameManager.getGameCount());
    }

    @Test
    void testGameIsolation() {
        GameConfig config = GameConfig.builder().ante(10).fixedBet(20).build();
        GameId game1 = gameManager.createGame(config);
        GameId game2 = gameManager.createGame(config);

        PokerGame pokerGame1 = gameManager.getGame(game1);
        PokerGame pokerGame2 = gameManager.getGame(game2);

        // Verify they are different instances
        assertNotSame(pokerGame1, pokerGame2);
        assertEquals(0, pokerGame1.getAllPlayers().size());
        assertEquals(0, pokerGame2.getAllPlayers().size());
    }
}

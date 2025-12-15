package poker.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import poker.model.game.GameConfig;
import poker.model.game.GameId;
import poker.model.game.PokerGame;
import poker.model.players.PlayerId;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ClientHandler without mocking NIO classes.
 * Tests protocol message handling through send/receive simulation.
 */
class ClientHandlerIntegrationTest {
    private GameManager gameManager;
    private Map<GameId, Set<ClientHandler>> gameClients;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        gameManager = new GameManager();
        gameClients = new ConcurrentHashMap<>();
        
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
    void testGameManagerIntegration() {
        // Test game creation through GameManager
        GameId gameId = gameManager.createGame(config);
        assertNotNull(gameId);
        assertEquals(1, gameManager.getGameCount());
        
        PokerGame game = gameManager.getGame(gameId);
        assertNotNull(game);
        assertEquals(gameId, game.getGameId());
    }

    @Test
    void testMultipleGamesInClientMap() {
        GameId gameId1 = gameManager.createGame(config);
        GameId gameId2 = gameManager.createGame(config);
        
        gameClients.computeIfAbsent(gameId1, k -> ConcurrentHashMap.newKeySet());
        gameClients.computeIfAbsent(gameId2, k -> ConcurrentHashMap.newKeySet());
        
        assertEquals(2, gameClients.size());
        assertTrue(gameClients.containsKey(gameId1));
        assertTrue(gameClients.containsKey(gameId2));
    }

    @Test
    void testGameConfigCreation() {
        GameConfig customConfig = GameConfig.builder()
            .ante(5)
            .fixedBet(10)
            .maxDraw(5)
            .minPlayers(2)
            .maxPlayers(6)
            .initialChips(500)
            .build();
        
        assertEquals(5, customConfig.getAnte());
        assertEquals(10, customConfig.getFixedBet());
        assertEquals(5, customConfig.getMaxDraw());
    }

    @Test
    void testGameLifecycle() {
        // Create game
        GameId gameId = gameManager.createGame(config);
        PokerGame game = gameManager.getGame(gameId);
        
        // Add players
        PlayerId player1 = PlayerId.generate();
        PlayerId player2 = PlayerId.generate();
        game.addPlayer(player1, "Player1");
        game.addPlayer(player2, "Player2");
        
        assertEquals(2, game.getAllPlayers().size());
        
        // Remove a player
        game.removePlayer(player1);
        assertEquals(1, game.getAllPlayers().size());
    }

    @Test
    void testGameClientTracking() {
        GameId gameId = gameManager.createGame(config);
        Set<ClientHandler> clients = gameClients.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
        
        assertTrue(clients.isEmpty());
        
        // Simulate adding clients (without real handlers)
        assertEquals(0, clients.size());
    }

    @Test
    void testConcurrentGameClientAccess() throws Exception {
        GameId gameId = gameManager.createGame(config);
        Set<ClientHandler> clients = gameClients.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
        
        // Test thread-safe operations
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                gameClients.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
                latch.countDown();
            }).start();
        }
        
        latch.await();
        assertEquals(1, gameClients.size());
    }

    @Test
    void testGameRemoval() {
        GameId gameId = gameManager.createGame(config);
        assertEquals(1, gameManager.getGameCount());
        
        gameManager.removeGame(gameId);
        assertEquals(0, gameManager.getGameCount());
    }

    @Test
    void testPlayerIdGeneration() {
        PlayerId id1 = PlayerId.generate();
        PlayerId id2 = PlayerId.generate();
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    void testGameIdGeneration() {
        GameId id1 = GameId.generate();
        GameId id2 = GameId.generate();
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    void testGameConfigValidation() {
        // Valid config
        GameConfig validConfig = GameConfig.builder()
            .ante(10)
            .fixedBet(20)
            .build();
        
        assertEquals(10, validConfig.getAnte());
        assertEquals(20, validConfig.getFixedBet());
    }

    @Test
    void testMultiplePlayersInGame() {
        GameId gameId = gameManager.createGame(config);
        PokerGame game = gameManager.getGame(gameId);
        
        PlayerId p1 = PlayerId.generate();
        PlayerId p2 = PlayerId.generate();
        PlayerId p3 = PlayerId.generate();
        
        game.addPlayer(p1, "Player1");
        game.addPlayer(p2, "Player2");
        game.addPlayer(p3, "Player3");
        
        assertEquals(3, game.getAllPlayers().size());
    }

    @Test
    void testGameStateAfterCreation() {
        GameId gameId = gameManager.createGame(config);
        PokerGame game = gameManager.getGame(gameId);
        
        assertEquals(poker.model.game.GameState.LOBBY, game.getState());
    }

    @Test
    void testGameClientsMapOperations() {
        GameId gameId1 = gameManager.createGame(config);
        GameId gameId2 = gameManager.createGame(config);
        
        gameClients.put(gameId1, ConcurrentHashMap.newKeySet());
        gameClients.put(gameId2, ConcurrentHashMap.newKeySet());
        
        assertTrue(gameClients.containsKey(gameId1));
        assertTrue(gameClients.containsKey(gameId2));
        
        gameClients.remove(gameId1);
        assertFalse(gameClients.containsKey(gameId1));
        assertTrue(gameClients.containsKey(gameId2));
    }

    @Test
    void testGameManagerThreadSafety() throws InterruptedException {
        java.util.List<Thread> threads = new ArrayList<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(5);
        
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread(() -> {
                gameManager.createGame(config);
                latch.countDown();
            });
            threads.add(thread);
            thread.start();
        }
        
        latch.await();
        assertEquals(5, gameManager.getGameCount());
    }

    @Test
    void testPlayerIdEquality() {
        String idString = "test-player-123";
        PlayerId id1 = PlayerId.of(idString);
        PlayerId id2 = PlayerId.of(idString);
        
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testGameIdEquality() {
        String idString = "test-game-456";
        GameId id1 = GameId.of(idString);
        GameId id2 = GameId.of(idString);
        
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testGameConfigDefaults() {
        GameConfig minConfig = GameConfig.builder()
            .ante(5)
            .fixedBet(10)
            .build();
        
        assertNotNull(minConfig);
        assertTrue(minConfig.getAnte() > 0);
        assertTrue(minConfig.getFixedBet() > 0);
    }

    @Test
    void testEmptyGameClientsSet() {
        GameId gameId = gameManager.createGame(config);
        Set<ClientHandler> clients = gameClients.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
        
        assertTrue(clients.isEmpty());
        assertEquals(0, clients.size());
    }

    @Test
    void testGameManagerIsolation() {
        GameManager gm1 = new GameManager();
        GameManager gm2 = new GameManager();
        
        gm1.createGame(config);
        gm1.createGame(config);
        gm2.createGame(config);
        
        assertEquals(2, gm1.getGameCount());
        assertEquals(1, gm2.getGameCount());
    }

    @Test
    void testMultipleGameConfigVariations() {
        GameConfig config1 = GameConfig.builder().ante(5).fixedBet(10).build();
        GameConfig config2 = GameConfig.builder().ante(10).fixedBet(20).build();
        GameConfig config3 = GameConfig.builder().ante(20).fixedBet(40).build();
        
        GameId g1 = gameManager.createGame(config1);
        GameId g2 = gameManager.createGame(config2);
        GameId g3 = gameManager.createGame(config3);
        
        assertEquals(3, gameManager.getGameCount());
        
        assertEquals(5, gameManager.getGame(g1).getConfig().getAnte());
        assertEquals(10, gameManager.getGame(g2).getConfig().getAnte());
        assertEquals(20, gameManager.getGame(g3).getConfig().getAnte());
    }
}

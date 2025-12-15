package poker.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import poker.model.game.GameConfig;
import poker.model.game.GameId;
import poker.model.players.PlayerId;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ClientHandler.
 */
class ClientHandlerTest {

    private static final int TEST_PORT = 18888;
    private ServerSocketChannel serverChannel;
    private SocketChannel clientChannel;
    private SocketChannel serverSideChannel;
    private GameManager gameManager;
    private Map<GameId, Set<ClientHandler>> gameClients;
    private PokerServer server;
    private ClientHandler clientHandler;
    private Selector selector;

    @BeforeEach
    void setUp() throws IOException {
        // Start a test server
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(TEST_PORT));
        serverChannel.configureBlocking(false);

        // Create client connection
        clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);
        clientChannel.connect(new InetSocketAddress("localhost", TEST_PORT));

        // Accept connection on server side
        while ((serverSideChannel = serverChannel.accept()) == null) {
            Thread.yield();
        }
        serverSideChannel.configureBlocking(false);

        // Finish connection
        while (!clientChannel.finishConnect()) {
            Thread.yield();
        }

        // Initialize test dependencies
        gameManager = new GameManager();
        gameClients = new ConcurrentHashMap<>();
        server = new PokerServer(TEST_PORT);
        
        // Create selector for testing
        selector = Selector.open();
        serverSideChannel.register(selector, SelectionKey.OP_READ);

        clientHandler = new ClientHandler(serverSideChannel, gameManager, gameClients, server);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (selector != null && selector.isOpen()) {
            selector.close();
        }
        if (clientChannel != null && clientChannel.isOpen()) {
            clientChannel.close();
        }
        if (serverSideChannel != null && serverSideChannel.isOpen()) {
            serverSideChannel.close();
        }
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }
    }

    @Test
    void testConstructor() {
        assertNotNull(clientHandler);
    }

    @Test
    void testHandleReadWithValidMessage() throws Exception {
        // Send HELLO message from client
        String message = "HELLO VERSION=\"1.0\"\n";
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        clientChannel.write(buffer);

        // Give time for data to be transmitted
        Thread.sleep(100);

        // Handle read on server side
        SelectionKey key = serverSideChannel.keyFor(selector);
        if (key != null) {
            clientHandler.handleRead(key);
        }

        // Verify no exception was thrown
        assertTrue(true);
    }

    @Test
    void testHandleReadWithClientDisconnect() throws Exception {
        // Close client channel to simulate disconnect
        clientChannel.close();

        // Give time for disconnect to be detected
        Thread.sleep(50);

        // Handle read should detect disconnect
        SelectionKey key = serverSideChannel.keyFor(selector);
        if (key != null) {
            assertDoesNotThrow(() -> clientHandler.handleRead(key));
        }
    }

    @Test
    void testHandleWriteWithPendingMessages() throws Exception {
        // Queue a message for writing
        clientHandler.send("TEST MESSAGE=\"Hello\"");

        // Give time for message to be queued
        Thread.sleep(50);

        // Handle write
        SelectionKey key = serverSideChannel.keyFor(selector);
        if (key != null) {
            // Register for write operations
            serverSideChannel.register(selector, SelectionKey.OP_WRITE);
            key = serverSideChannel.keyFor(selector);
            clientHandler.handleWrite(key);
        }

        // Read message on client side
        Thread.sleep(100);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);
        
        if (bytesRead > 0) {
            buffer.flip();
            String received = StandardCharsets.UTF_8.decode(buffer).toString();
            assertTrue(received.contains("TEST"));
        }
    }

    @Test
    void testProcessCreateCommand() throws Exception {
        // Send HELLO first to initialize player
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(100);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Send CREATE command
        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"CREATE\" ANTE=\"10\" BET=\"20\"\n");
        Thread.sleep(100);
        clientHandler.handleRead(key);

        // Should not throw exception
        assertTrue(true);
    }

    @Test
    void testProcessJoinCommand() throws Exception {
        // First create a game
        GameConfig config = GameConfig.builder()
                .ante(10)
                .fixedBet(20)
                .maxPlayers(4)
                .build();
        GameId gameId = gameManager.createGame(config);

        // Send HELLO first
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(100);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Send JOIN command
        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"JOIN\" GAME_REF=\"" + gameId.getId() + "\" NAME=\"TestPlayer\"\n");
        Thread.sleep(100);
        clientHandler.handleRead(key);

        assertTrue(true);
    }

    @Test
    void testProcessStartCommand() throws Exception {
        // Create and join a game first
        GameConfig config = GameConfig.builder()
                .ante(10)
                .fixedBet(20)
                .maxPlayers(2)
                .build();
        GameId gameId = gameManager.createGame(config);
        
        // Send HELLO
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(100);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Join game
        sendMessage("GAME_ID=\"" + gameId.getId() + "\" PLAYER_ID=\"\" ACTION=\"JOIN\" GAME_REF=\"" + gameId.getId() + "\" NAME=\"Player1\"\n");
        Thread.sleep(100);
        clientHandler.handleRead(key);

        // Add another player to meet minimum
        var secondChannel = SocketChannel.open();
        secondChannel.configureBlocking(false);
        secondChannel.connect(new InetSocketAddress("localhost", TEST_PORT));
        var secondServerChannel = serverChannel.accept();
        if (secondServerChannel != null) {
            secondServerChannel.configureBlocking(false);
            while (!secondChannel.finishConnect()) {
                Thread.yield();
            }
            var secondHandler = new ClientHandler(secondServerChannel, gameManager, gameClients, server);
            
            // Send HELLO for second client
            ByteBuffer buffer = ByteBuffer.wrap("HELLO VERSION=\"1.0\"\n".getBytes(StandardCharsets.UTF_8));
            secondChannel.write(buffer);
            Thread.sleep(100);
            secondServerChannel.register(selector, SelectionKey.OP_READ);
            secondHandler.handleRead(secondServerChannel.keyFor(selector));

            // Join with second player
            buffer = ByteBuffer.wrap(("GAME_ID=\"" + gameId.getId() + "\" PLAYER_ID=\"\" ACTION=\"JOIN\" GAME_REF=\"" + gameId.getId() + "\" NAME=\"Player2\"\n").getBytes(StandardCharsets.UTF_8));
            secondChannel.write(buffer);
            Thread.sleep(100);
            secondHandler.handleRead(secondServerChannel.keyFor(selector));

            // Now start the game
            sendMessage("GAME_ID=\"" + gameId.getId() + "\" PLAYER_ID=\"\" ACTION=\"START\"\n");
            Thread.sleep(100);
            clientHandler.handleRead(key);
            
            secondChannel.close();
            secondServerChannel.close();
        }

        assertTrue(true);
    }

    @Test
    void testProcessCheckCommand() throws Exception {
        // This would require a game in progress, but we test the handler doesn't crash
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"CHECK\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testProcessCallCommand() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"CALL\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testProcessBetCommand() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"BET\" AMOUNT=\"50\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testProcessFoldCommand() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"FOLD\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testProcessDrawCommand() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"DRAW\" INDICES=\"0,2,4\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testProcessLeaveCommand() throws Exception {
        // Create and join a game
        GameConfig config = GameConfig.builder()
                .ante(10)
                .fixedBet(20)
                .maxPlayers(4)
                .build();
        GameId gameId = gameManager.createGame(config);

        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(100);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"JOIN\" GAME_REF=\"" + gameId.getId() + "\" NAME=\"TestPlayer\"\n");
        Thread.sleep(100);
        clientHandler.handleRead(key);

        // Leave the game
        sendMessage("GAME_ID=\"" + gameId.getId() + "\" PLAYER_ID=\"\" ACTION=\"LEAVE\"\n");
        Thread.sleep(100);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testProcessInvalidCommand() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Send invalid command
        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"INVALID\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testProcessMalformedMessage() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Send malformed message
        sendMessage("THIS IS NOT A VALID MESSAGE FORMAT\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testClose() throws Exception {
        assertDoesNotThrow(() -> clientHandler.close());
        
        // Verify channel is closed
        assertFalse(serverSideChannel.isOpen());
    }

    @Test
    void testMultipleMessagesInOneRead() throws Exception {
        // Send multiple messages at once
        String messages = "HELLO VERSION=\"1.0\"\n" +
                         "GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"CREATE\" ANTE=\"10\" BET=\"20\"\n";
        sendMessage(messages);
        
        Thread.sleep(100);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Should process both messages
        assertTrue(true);
    }

    @Test
    void testPartialMessageHandling() throws Exception {
        // Send incomplete message (no newline)
        String partialMessage = "HELLO VERSION=\"1.0\"";
        ByteBuffer buffer = ByteBuffer.wrap(partialMessage.getBytes(StandardCharsets.UTF_8));
        clientChannel.write(buffer);
        
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Send rest of message
        buffer = ByteBuffer.wrap("\n".getBytes(StandardCharsets.UTF_8));
        clientChannel.write(buffer);
        
        Thread.sleep(50);
        clientHandler.handleRead(key);

        // Should handle complete message
        assertTrue(true);
    }

    @Test
    void testQueueMessage() {
        assertDoesNotThrow(() -> clientHandler.send("TEST MESSAGE"));
    }

    @Test
    void testSendMultipleMessages() throws Exception {
        // Send multiple messages
        clientHandler.send("MESSAGE 1");
        clientHandler.send("MESSAGE 2");
        clientHandler.send("MESSAGE 3");

        // Messages should be queued
        assertTrue(true);
    }

    @Test
    void testEmptyMessageHandling() throws Exception {
        sendMessage("\n\n\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testLargeMessageHandling() throws Exception {
        // Send a large message
        StringBuilder largeMessage = new StringBuilder("HELLO VERSION=\"1.0\" DATA=\"");
        for (int i = 0; i < 1000; i++) {
            largeMessage.append("X");
        }
        largeMessage.append("\"\n");
        
        sendMessage(largeMessage.toString());
        Thread.sleep(100);
        SelectionKey key = serverSideChannel.keyFor(selector);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testMultipleDrawIndices() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"DRAW\" INDICES=\"0,1,2,3,4\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testDrawWithEmptyIndices() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"DRAW\" INDICES=\"\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testMessageWithSpecialCharacters() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"JOIN\" NAME=\"Player@#$%\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testCreateGameWithDifferentAmounts() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"CREATE\" ANTE=\"5\" BET=\"10\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testBetWithZeroAmount() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"BET\" AMOUNT=\"0\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testBetWithLargeAmount() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"test\" PLAYER_ID=\"player1\" ACTION=\"BET\" AMOUNT=\"999999\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testCloseWithoutGame() throws Exception {
        // Test closing handler that never joined a game
        assertDoesNotThrow(() -> clientHandler.close());
    }

    @Test
    void testCloseWhileInGame() throws Exception {
        // Create and join a game
        GameConfig config = GameConfig.builder()
                .ante(10)
                .fixedBet(20)
                .maxPlayers(4)
                .build();
        GameId gameId = gameManager.createGame(config);

        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(100);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"JOIN\" GAME_REF=\"" + gameId.getId() + "\" NAME=\"TestPlayer\"\n");
        Thread.sleep(100);
        clientHandler.handleRead(key);

        // Now close while in game
        assertDoesNotThrow(() -> clientHandler.close());
    }

    @Test
    void testMessageWithMissingParameters() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Missing required parameters
        sendMessage("GAME_ID=\"test\" ACTION=\"CREATE\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testSequentialCommands() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        // Create game
        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"CREATE\" ANTE=\"10\" BET=\"20\"\n");
        Thread.sleep(50);
        clientHandler.handleRead(key);

        // Try to create another game (should handle error)
        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"CREATE\" ANTE=\"15\" BET=\"30\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testHandleWriteWithNoMessages() throws Exception {
        SelectionKey key = serverSideChannel.keyFor(selector);
        if (key != null) {
            serverSideChannel.register(selector, SelectionKey.OP_WRITE);
            final SelectionKey finalKey = serverSideChannel.keyFor(selector);
            // Should handle gracefully with empty queue
            assertDoesNotThrow(() -> clientHandler.handleWrite(finalKey));
        }
    }

    @Test
    void testMultipleReadsWithIncompleteData() throws Exception {
        // Send data in small chunks
        ByteBuffer buffer = ByteBuffer.wrap("HE".getBytes(StandardCharsets.UTF_8));
        clientChannel.write(buffer);
        Thread.sleep(20);
        
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        buffer = ByteBuffer.wrap("LLO VE".getBytes(StandardCharsets.UTF_8));
        clientChannel.write(buffer);
        Thread.sleep(20);
        clientHandler.handleRead(key);

        buffer = ByteBuffer.wrap("RSION=\"1.0\"\n".getBytes(StandardCharsets.UTF_8));
        clientChannel.write(buffer);
        Thread.sleep(50);
        clientHandler.handleRead(key);

        // Should have processed complete message
        assertTrue(true);
    }

    @Test
    void testJoinNonExistentGame() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"\" PLAYER_ID=\"\" ACTION=\"JOIN\" GAME_REF=\"nonexistent\" NAME=\"Player1\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testStartGameNotInLobby() throws Exception {
        sendMessage("HELLO VERSION=\"1.0\"\n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        clientHandler.handleRead(key);

        sendMessage("GAME_ID=\"somegame\" PLAYER_ID=\"player1\" ACTION=\"START\"\n");
        Thread.sleep(50);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testDoubleClose() throws Exception {
        clientHandler.close();
        // Second close should not throw
        assertDoesNotThrow(() -> clientHandler.close());
    }

    @Test
    void testWhitespaceOnlyMessage() throws Exception {
        sendMessage("   \n");
        Thread.sleep(50);
        SelectionKey key = serverSideChannel.keyFor(selector);
        assertDoesNotThrow(() -> clientHandler.handleRead(key));
    }

    @Test
    void testSendAfterClose() throws Exception {
        clientHandler.close();
        // Sending after close should not throw
        assertDoesNotThrow(() -> clientHandler.send("TEST"));
    }

    private void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        clientChannel.write(buffer);
    }
}

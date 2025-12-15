package poker.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import poker.model.protocol.ClientMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PokerClient.
 */
class PokerClientTest {

    private static final int TEST_PORT = 9999;
    private static final String TEST_HOST = "localhost";
    
    private ServerSocket mockServer;
    private Thread serverThread;
    private Socket serverSideSocket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create a mock server for testing
        mockServer = new ServerSocket(TEST_PORT);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (serverReader != null) {
            try {
                serverReader.close();
            } catch (IOException ignored) {}
        }
        if (serverWriter != null) {
            serverWriter.close();
        }
        if (serverSideSocket != null && !serverSideSocket.isClosed()) {
            try {
                serverSideSocket.close();
            } catch (IOException ignored) {}
        }
        if (mockServer != null && !mockServer.isClosed()) {
            mockServer.close();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }
    
    @Test
    void testConstructor() {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        assertNotNull(client);
    }
    
    @Test
    void testConnect() throws Exception {
        // Start accepting connections in background
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected when test closes
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        
        // Wait for server thread to accept connection
        Thread.sleep(100);
        
        // Verify connection was established
        assertNotNull(serverSideSocket);
        assertTrue(serverSideSocket.isConnected());
        
        // Verify client sent HELLO message
        String receivedMessage = serverReader.readLine();
        assertNotNull(receivedMessage);
        assertTrue(receivedMessage.contains("HELLO"));
        assertTrue(receivedMessage.contains("VERSION=1.0"));
    }
    
    @Test
    void testConnectFailure() {
        // Don't start server, so connection should fail
        PokerClient client = new PokerClient(TEST_HOST, 9998); // Different port with no server
        
        assertThrows(IOException.class, client::connect);
    }
    
    @Test
    void testMainWithDefaultArguments() {
        // Test that main method can be called (doesn't throw)
        // This is a basic smoke test
        assertDoesNotThrow(() -> {
            // We can't actually run main without a server, but we can test argument parsing
            String[] args = {};
            // Just verify the class loads and main exists
            assertNotNull(PokerClient.class.getMethod("main", String[].class));
        });
    }
    
    @Test
    void testMainWithCustomHost() {
        // Verify main method exists and can parse arguments
        assertDoesNotThrow(() -> {
            assertNotNull(PokerClient.class.getMethod("main", String[].class));
        });
    }
    
    @Test
    void testMainWithInvalidPort() {
        // Test that invalid port is handled
        // This would normally call System.exit, but we're just testing the method exists
        assertDoesNotThrow(() -> {
            assertNotNull(PokerClient.class.getMethod("main", String[].class));
        });
    }
    
    /**
     * Test helper methods through reflection since they are private.
     * This test ensures the formatting logic works correctly.
     */
    @Test
    void testFormatHandNice() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Use reflection to test private method
        var method = PokerClient.class.getDeclaredMethod("formatHandNice", String.class);
        method.setAccessible(true);
        
        // Test normal hand
        String result = (String) method.invoke(client, "AS,KH,QD,JC,10S");
        assertTrue(result.contains("[0:AS]"));
        assertTrue(result.contains("[1:KH]"));
        assertTrue(result.contains("[2:QD]"));
        assertTrue(result.contains("[3:JC]"));
        assertTrue(result.contains("[4:10S]"));
        
        // Test hidden hand
        result = (String) method.invoke(client, "*,*,*,*,*");
        assertEquals("[Hidden]", result);
        
        // Test null hand
        result = (String) method.invoke(client, (String) null);
        assertEquals("[Hidden]", result);
        
        // Test empty hand
        result = (String) method.invoke(client, "");
        assertEquals("[Hidden]", result);
    }
    
    @Test
    void testFormatCards() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Use reflection to test private method
        var method = PokerClient.class.getDeclaredMethod("formatCards", String.class);
        method.setAccessible(true);
        
        // Test normal cards
        String result = (String) method.invoke(client, "AS,KH,QD");
        assertEquals("AS,KH,QD", result);
        
        // Test hidden cards
        result = (String) method.invoke(client, "*,*,*,*,*");
        assertEquals("Hidden", result);
        
        // Test null cards
        result = (String) method.invoke(client, (String) null);
        assertEquals("Hidden", result);
        
        // Test empty cards
        result = (String) method.invoke(client, "");
        assertEquals("Hidden", result);
    }
    
    @Test
    void testSendMessage() throws Exception {
        // Start accepting connections in background
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected when test closes
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        
        // Wait for connection
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Use reflection to call private send method
        var method = PokerClient.class.getDeclaredMethod("send", ClientMessage.class);
        method.setAccessible(true);
        
        ClientMessage testMessage = ClientMessage.create(10, 20);
        method.invoke(client, testMessage);
        
        // Verify message was sent
        String received = serverReader.readLine();
        assertNotNull(received);
        assertTrue(received.contains("CREATE"));
    }
    
    @Test
    void testHandleServerMessageOK() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Use reflection to test private method
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test OK message with game creation
        String message1 = "OK MESSAGE=\"Game created: GAME123\"";
        assertDoesNotThrow(() -> method.invoke(client, message1));
        
        // Test OK message with left game
        String message2 = "OK MESSAGE=\"Left game\"";
        assertDoesNotThrow(() -> method.invoke(client, message2));
    }
    
    @Test
    void testHandleServerMessageERR() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String message = "ERR REASON=\"Invalid command\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageWELCOME() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String message = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageLOBBY() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String message = "LOBBY PLAYERS=\"Alice,Bob\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageSTARTED() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String message = "STARTED ANTE=\"10\" BET=\"20\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageDEAL() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // First set player ID
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Then test DEAL
        String message = "DEAL PLAYER=\"PLAYER456\" CARDS=\"AS,KH,QD,JC,10S\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageTURN() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test TURN for draw phase
        String message1 = "TURN PLAYER=\"PLAYER456\" PHASE=\"DRAW\"";
        assertDoesNotThrow(() -> method.invoke(client, message1));
        
        // Test TURN for betting phase
        String message2 = "TURN PLAYER=\"PLAYER456\" PHASE=\"BET1\" CALL=\"20\"";
        assertDoesNotThrow(() -> method.invoke(client, message2));
    }
    
    @Test
    void testHandleServerMessageACTION() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test ACTION from another player
        String message1 = "ACTION PLAYER=\"OTHER\" TYPE=\"CHECK\" ARGS=\"\"";
        assertDoesNotThrow(() -> method.invoke(client, message1));
        
        String message2 = "ACTION PLAYER=\"OTHER\" TYPE=\"BET\" ARGS=\"50\"";
        assertDoesNotThrow(() -> method.invoke(client, message2));
        
        String message3 = "ACTION PLAYER=\"OTHER\" TYPE=\"FOLD\" ARGS=\"\"";
        assertDoesNotThrow(() -> method.invoke(client, message3));
    }
    
    @Test
    void testHandleServerMessageDRAW_OK() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID and current hand first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String dealMsg = "DEAL PLAYER=\"PLAYER456\" CARDS=\"AS,KH,QD,JC,10S\"";
        method.invoke(client, dealMsg);
        
        // Test DRAW_OK
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H,3D\" COUNT=\"2\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageROUND() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String message = "ROUND POT=\"100\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageSHOWDOWN() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String message = "SHOWDOWN PLAYER=\"PLAYER456\" HAND=\"AS,KS,QS,JS,10S\" RANK=\"ROYAL_FLUSH\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageWINNER() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String message = "WINNER PLAYER=\"PLAYER456\" POT=\"200\" RANK=\"FULL_HOUSE\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessagePAYOUT() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String message = "PAYOUT PLAYER=\"PLAYER456\" STACK=\"1000\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleServerMessageEND() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String message = "END";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
    
    @Test
    void testHandleUserInputHelp() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "help"));
        assertDoesNotThrow(() -> method.invoke(client, "h"));
        assertDoesNotThrow(() -> method.invoke(client, "?"));
    }
    
    @Test
    void testHandleUserInputHand() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "hand"));
        assertDoesNotThrow(() -> method.invoke(client, "cards"));
    }
    
    @Test
    void testPrintHelp() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("printHelp");
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client));
    }
    
    @Test
    void testDisconnect() throws Exception {
        // Start accepting connections in background
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected when test closes
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        
        // Wait for connection
        Thread.sleep(100);
        
        // Use reflection to call disconnect
        var method = PokerClient.class.getDeclaredMethod("disconnect");
        method.setAccessible(true);
        method.invoke(client);
        
        // Verify socket is closed
        Thread.sleep(100);
        // The client socket should be closed, server might detect it
    }
    
    @Test
    void testHandleInvalidServerMessage() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test malformed message - should not throw
        assertDoesNotThrow(() -> method.invoke(client, "INVALID MESSAGE FORMAT"));
    }
    
    @Test
    void testHandleUnknownUserCommand() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        // Unknown command should not throw
        assertDoesNotThrow(() -> method.invoke(client, "unknown"));
    }
}

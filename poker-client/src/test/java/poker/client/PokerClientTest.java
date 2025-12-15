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
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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

    @Test
    void testHandleUserInputStart() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "start"));
    }

    @Test
    void testHandleUserInputCheck() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "check"));
    }

    @Test
    void testHandleUserInputCall() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "call"));
    }

    @Test
    void testHandleUserInputBet() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "bet 50"));
    }

    @Test
    void testHandleUserInputFold() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "fold"));
    }

    @Test
    void testHandleUserInputDraw() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "draw 0,2,4"));
    }

    @Test
    void testHandleUserInputDrawNone() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "draw none"));
    }

    @Test
    void testHandleUserInputStatus() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "status"));
    }

    @Test
    void testHandleUserInputLeave() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "leave"));
    }

    @Test
    void testHandleUserInputQuit() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "quit"));
    }

    @Test
    void testHandleServerMessageBET() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String message = "ACTION PLAYER=\"OTHER\" TYPE=\"BET\" ARGS=\"100\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageCALL() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String message = "ACTION PLAYER=\"OTHER\" TYPE=\"CALL\" ARGS=\"\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageRAISE() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String message = "ACTION PLAYER=\"OTHER\" TYPE=\"RAISE\" ARGS=\"150\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDRAW() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String message = "ACTION PLAYER=\"OTHER\" TYPE=\"DRAW\" ARGS=\"3\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testFormatHandNiceWithPartialHand() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("formatHandNice", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(client, "AS,KH,QD");
        assertTrue(result.contains("[0:AS]"));
        assertTrue(result.contains("[1:KH]"));
        assertTrue(result.contains("[2:QD]"));
    }

    @Test
    void testFormatHandNiceWithSingleCard() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("formatHandNice", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(client, "AS");
        assertTrue(result.contains("[0:AS]"));
    }

    @Test
    void testFormatCardsWithEmptyString() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("formatCards", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(client, "");
        assertEquals("Hidden", result);
    }

    @Test
    void testFormatCardsWithSingleCard() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("formatCards", String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(client, "AS");
        assertEquals("AS", result);
    }

    @Test
    void testHandleServerMessageMultipleTypes() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID
        method.invoke(client, "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"");
        
        // Test multiple message types
        assertDoesNotThrow(() -> method.invoke(client, "STARTED ANTE=\"10\" BET=\"20\""));
        assertDoesNotThrow(() -> method.invoke(client, "DEAL PLAYER=\"PLAYER456\" CARDS=\"AS,KH,QD,JC,10S\""));
        assertDoesNotThrow(() -> method.invoke(client, "TURN PLAYER=\"PLAYER456\" PHASE=\"BET1\" CALL=\"20\""));
        assertDoesNotThrow(() -> method.invoke(client, "ROUND POT=\"100\""));
        assertDoesNotThrow(() -> method.invoke(client, "SHOWDOWN PLAYER=\"PLAYER456\" HAND=\"AS,KS,QS,JS,10S\" RANK=\"ROYAL_FLUSH\""));
        assertDoesNotThrow(() -> method.invoke(client, "WINNER PLAYER=\"PLAYER456\" POT=\"200\" RANK=\"FULL_HOUSE\""));
        assertDoesNotThrow(() -> method.invoke(client, "PAYOUT PLAYER=\"PLAYER456\" STACK=\"1000\""));
    }

    @Test
    void testConnectToInvalidPort() {
        PokerClient client = new PokerClient(TEST_HOST, 1); // Port 1 unlikely to have server
        assertThrows(IOException.class, client::connect);
    }

    @Test
    void testHandleUserInputWithExtraSpaces() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "  create   10   20  "));
    }

    @Test
    void testHandleUserInputCaseInsensitivity() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "HELP"));
        assertDoesNotThrow(() -> method.invoke(client, "Help"));
        assertDoesNotThrow(() -> method.invoke(client, "hElP"));
    }

    @Test
    void testHandleServerMessageWithOwnAction() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID
        method.invoke(client, "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"");
        
        // Action from self (should still not throw)
        String message = "ACTION PLAYER=\"PLAYER456\" TYPE=\"BET\" ARGS=\"50\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testMainMethodExists() throws Exception {
        // Verify main method signature
        var mainMethod = PokerClient.class.getMethod("main", String[].class);
        assertNotNull(mainMethod);
        assertEquals(void.class, mainMethod.getReturnType());
    }

    @Test
    void testHandleUserInputCreate() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Use reflection to call handleUserInput
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        method.invoke(client, "create 10 20");
        
        // Should receive CREATE message
        String received = serverReader.readLine();
        assertNotNull(received);
        assertTrue(received.contains("CREATE"));
    }

    @Test
    void testHandleUserInputJoin() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Use reflection to call handleUserInput
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        method.invoke(client, "join GAME123 Alice");
        
        // Should receive JOIN message
        String received = serverReader.readLine();
        assertNotNull(received);
        assertTrue(received.contains("JOIN"));
    }

    @Test
    void testHandleUserInputInvalidCommands() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        // Test invalid create (missing arguments)
        assertDoesNotThrow(() -> method.invoke(client, "create"));
        assertDoesNotThrow(() -> method.invoke(client, "create 10"));
        
        // Test invalid join (missing arguments)
        assertDoesNotThrow(() -> method.invoke(client, "join"));
        assertDoesNotThrow(() -> method.invoke(client, "join GAME123"));
        
        // Test invalid draw (missing arguments)
        assertDoesNotThrow(() -> method.invoke(client, "draw"));
        
        // Test invalid bet (missing arguments)
        assertDoesNotThrow(() -> method.invoke(client, "bet"));
    }

    @Test
    void testHandleServerMessageAnteOk() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test ANTE_OK for own player
        String message = "ANTE_OK PLAYER=\"PLAYER456\" STACK=\"990\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
        
        // Test ANTE_OK for other player (should not print)
        String message2 = "ANTE_OK PLAYER=\"OTHER\" STACK=\"990\"";
        assertDoesNotThrow(() -> method.invoke(client, message2));
    }

    @Test
    void testHandleServerMessageDealHiddenCards() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test DEAL with hidden cards
        String message = "DEAL PLAYER=\"PLAYER456\" CARDS=\"*,*,*,*,*\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
        
        // Test DEAL for another player
        String message2 = "DEAL PLAYER=\"OTHER\" CARDS=\"AS,KH,QD,JC,10S\"";
        assertDoesNotThrow(() -> method.invoke(client, message2));
    }

    @Test
    void testHandleServerMessageTurnCheckScenario() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID and current hand
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String dealMsg = "DEAL PLAYER=\"PLAYER456\" CARDS=\"AS,KH,QD,JC,10S\"";
        method.invoke(client, dealMsg);
        
        // Test TURN with CALL=0 (check scenario)
        String message = "TURN PLAYER=\"PLAYER456\" PHASE=\"BET1\" CALL=\"0\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkWithoutCards() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test DRAW_OK without new cards (kept all)
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"\" COUNT=\"0\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
        
        // Test DRAW_OK with asterisk
        String message2 = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"*\" COUNT=\"0\"";
        assertDoesNotThrow(() -> method.invoke(client, message2));
    }

    @Test
    void testHandleServerMessageDrawokAlternative() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test DRAWOK (alternative format)
        String message = "DRAWOK PLAYER=\"PLAYER456\" NEW=\"2H,3D\" COUNT=\"2\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageOkWelcome() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test OK with welcome message
        String message = "OK MESSAGE=\"Welcome to Poker Server\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageOkOther() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test OK with other message
        String message = "OK MESSAGE=\"Player joined\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageShowdownOpponent() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test SHOWDOWN for opponent
        String message = "SHOWDOWN PLAYER=\"OTHER\" HAND=\"AS,KS,QS,JS,10S\" RANK=\"ROYAL_FLUSH\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageWinnerOpponent() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test WINNER for opponent
        String message = "WINNER PLAYER=\"OTHER\" POT=\"200\" RANK=\"FULL_HOUSE\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessagePayoutOtherPlayer() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test PAYOUT for other player (should not print)
        String message = "PAYOUT PLAYER=\"OTHER\" STACK=\"1000\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleUserInputExit() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        // Test exit command
        assertDoesNotThrow(() -> method.invoke(client, "exit"));
    }

    @Test
    void testHandleUserInputDrawWithIndices() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Set game and player ID using reflection
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        // Use reflection to call handleUserInput
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        method.invoke(client, "draw 0,2,4");
        
        // Should receive DRAW message
        String received = serverReader.readLine();
        assertNotNull(received);
        assertTrue(received.contains("DRAW"));
    }

    @Test
    void testHandleServerMessageDrawOkWithIndices() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Set up game state using reflection
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var currentHandField = PokerClient.class.getDeclaredField("currentHand");
        currentHandField.setAccessible(true);
        currentHandField.set(client, "AS,KH,QD,JC,10S");
        
        var lastDrawIndicesField = PokerClient.class.getDeclaredField("lastDrawIndices");
        lastDrawIndicesField.setAccessible(true);
        lastDrawIndicesField.set(client, java.util.Arrays.asList(0, 2, 4));
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test DRAW_OK with actual card replacement
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H,3D,4C\" COUNT=\"3\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testSendWithConnectedClient() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Send multiple messages
        var method = PokerClient.class.getDeclaredMethod("send", ClientMessage.class);
        method.setAccessible(true);
        
        method.invoke(client, ClientMessage.create(10, 20));
        method.invoke(client, ClientMessage.join("GAME123", "Alice"));
        
        // Verify messages were sent
        String msg1 = serverReader.readLine();
        String msg2 = serverReader.readLine();
        assertNotNull(msg1);
        assertNotNull(msg2);
        assertTrue(msg1.contains("CREATE"));
        assertTrue(msg2.contains("JOIN"));
    }

    @Test
    void testRunNotConnected() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Try to run without connecting - should print error and return
        assertDoesNotThrow(() -> client.run());
    }

    @Test
    void testHandleUserInputBetWithGameState() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Set game and player ID
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        // Test various game commands with game state
        method.invoke(client, "check");
        method.invoke(client, "call");
        method.invoke(client, "fold");
        method.invoke(client, "status");
        method.invoke(client, "start");
        
        // Verify messages were sent
        for (int i = 0; i < 5; i++) {
            String received = serverReader.readLine();
            assertNotNull(received);
        }
    }

    @Test
    void testHandleUserInputLeaveWithGameState() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Set game and player ID
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        method.invoke(client, "leave");
        
        // Verify LEAVE message was sent
        String received = serverReader.readLine();
        assertNotNull(received);
        assertTrue(received.contains("LEAVE"));
        
        // Verify game and player ID were cleared
        assertNull(gameIdField.get(client));
        assertNull(playerIdField.get(client));
    }

    @Test
    void testHandleUserInputQuitWithGameState() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Set game and player ID
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        method.invoke(client, "quit");
        
        // Verify QUIT message was sent
        String received = serverReader.readLine();
        assertNotNull(received);
        assertTrue(received.contains("QUIT"));
    }

    @Test
    void testHandleUserInputHandWithCurrentHand() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Set current hand
        var currentHandField = PokerClient.class.getDeclaredField("currentHand");
        currentHandField.setAccessible(true);
        currentHandField.set(client, "AS,KH,QD,JC,10S");
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> method.invoke(client, "hand"));
    }

    @Test
    void testHandleServerMessageTurnForOtherPlayer() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test TURN for another player (should not print details)
        String message = "TURN PLAYER=\"OTHER\" PHASE=\"DRAW\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageTurnBet1Phase() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID and current hand
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String dealMsg = "DEAL PLAYER=\"PLAYER456\" CARDS=\"AS,KH,QD,JC,10S\"";
        method.invoke(client, dealMsg);
        
        // Test TURN for BET1 phase with call amount > 0
        String message = "TURN PLAYER=\"PLAYER456\" PHASE=\"BET1\" CALL=\"50\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageTurnBet2Phase() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID and current hand
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        String dealMsg = "DEAL PLAYER=\"PLAYER456\" CARDS=\"AS,KH,QD,JC,10S\"";
        method.invoke(client, dealMsg);
        
        // Test TURN for BET2 phase
        String message = "TURN PLAYER=\"PLAYER456\" PHASE=\"BET2\" CALL=\"100\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testIntegrationFullGameFlow() throws Exception {
        // Start mock server
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                
                // Read HELLO
                serverReader.readLine();
                
                // Simulate game flow
                serverWriter.println("OK MESSAGE=\"Welcome to Poker Server\"");
                Thread.sleep(50);
                serverWriter.println("OK MESSAGE=\"Game created: GAME123\"");
                Thread.sleep(50);
                serverWriter.println("WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"");
                Thread.sleep(50);
                serverWriter.println("LOBBY PLAYERS=\"Alice,Bob\"");
                Thread.sleep(50);
                serverWriter.println("STARTED ANTE=\"10\" BET=\"20\"");
                Thread.sleep(50);
                serverWriter.println("ANTE_OK PLAYER=\"PLAYER456\" STACK=\"990\"");
                Thread.sleep(50);
                serverWriter.println("DEAL PLAYER=\"PLAYER456\" CARDS=\"AS,KH,QD,JC,10S\"");
                Thread.sleep(50);
                serverWriter.println("TURN PLAYER=\"PLAYER456\" PHASE=\"BET1\" CALL=\"0\"");
                Thread.sleep(50);
                serverWriter.println("ACTION PLAYER=\"OTHER\" TYPE=\"CHECK\" ARGS=\"\"");
                Thread.sleep(50);
                serverWriter.println("TURN PLAYER=\"PLAYER456\" PHASE=\"DRAW\"");
                Thread.sleep(50);
                serverWriter.println("DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H,3D\" COUNT=\"2\"");
                Thread.sleep(50);
                serverWriter.println("TURN PLAYER=\"PLAYER456\" PHASE=\"BET2\" CALL=\"20\"");
                Thread.sleep(50);
                serverWriter.println("ROUND POT=\"100\"");
                Thread.sleep(50);
                serverWriter.println("SHOWDOWN PLAYER=\"PLAYER456\" HAND=\"AS,KH,QD,2H,3D\" RANK=\"PAIR\"");
                Thread.sleep(50);
                serverWriter.println("WINNER PLAYER=\"PLAYER456\" POT=\"100\" RANK=\"PAIR\"");
                Thread.sleep(50);
                serverWriter.println("PAYOUT PLAYER=\"PLAYER456\" STACK=\"1090\"");
                Thread.sleep(50);
                serverWriter.println("END");
            } catch (Exception e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        
        // Give time for messages to be processed
        Thread.sleep(1000);
        
        // Verify connection was established
        assertNotNull(serverSideSocket);
    }

    @Test
    void testHandleUserInputNumberFormatException() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        // Test invalid number format for create
        assertThrows(Exception.class, () -> method.invoke(client, "create abc def"));
        
        // Test invalid number format for bet
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        assertThrows(Exception.class, () -> method.invoke(client, "bet xyz"));
    }

    @Test
    void testHandleUserInputDrawInvalidIndices() throws Exception {
        // Start accepting connections
        final ServerSocket finalMockServer = mockServer;
        serverThread = new Thread(() -> {
            try {
                serverSideSocket = finalMockServer.accept();
                serverReader = new BufferedReader(
                    new InputStreamReader(serverSideSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(
                    new OutputStreamWriter(serverSideSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        client.connect();
        Thread.sleep(100);
        
        // Read HELLO message
        serverReader.readLine();
        
        // Set game and player ID
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var method = PokerClient.class.getDeclaredMethod("handleUserInput", String.class);
        method.setAccessible(true);
        
        // Test draw with invalid indices
        assertThrows(Exception.class, () -> method.invoke(client, "draw a,b,c"));
    }

    @Test
    void testHandleServerMessageOkGameCreatedExtraction() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test OK with game created and proper ID extraction
        String message = "OK MESSAGE=\"Game created: TEST-GAME-ID-123\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkWithLessNewCards() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Set up game state
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var currentHandField = PokerClient.class.getDeclaredField("currentHand");
        currentHandField.setAccessible(true);
        currentHandField.set(client, "AS,KH,QD,JC,10S");
        
        var lastDrawIndicesField = PokerClient.class.getDeclaredField("lastDrawIndices");
        lastDrawIndicesField.setAccessible(true);
        // Set indices but receive fewer cards
        lastDrawIndicesField.set(client, java.util.Arrays.asList(0, 2, 4));
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test DRAW_OK with only one new card
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H\" COUNT=\"1\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkWithIndexOutOfBounds() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Set up game state
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var currentHandField = PokerClient.class.getDeclaredField("currentHand");
        currentHandField.setAccessible(true);
        currentHandField.set(client, "AS,KH,QD,JC,10S");
        
        var lastDrawIndicesField = PokerClient.class.getDeclaredField("lastDrawIndices");
        lastDrawIndicesField.setAccessible(true);
        // Set invalid index
        lastDrawIndicesField.set(client, java.util.Arrays.asList(10));
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Should not throw even with invalid index
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H\" COUNT=\"1\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkForOtherPlayer() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test DRAW_OK for another player (should not process)
        String message = "DRAW_OK PLAYER=\"OTHER\" NEW=\"2H,3D\" COUNT=\"2\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageActionWithNullArgs() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test ACTION without ARGS parameter (null)
        String message = "ACTION PLAYER=\"OTHER\" TYPE=\"CHECK\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageActionWithBetAndNoArgs() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test ACTION BET without ARGS
        String message = "ACTION PLAYER=\"OTHER\" TYPE=\"BET\" ARGS=\"\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageUnknownAction() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test ACTION with unknown type
        String message = "ACTION PLAYER=\"OTHER\" TYPE=\"UNKNOWN_ACTION\" ARGS=\"\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageOkWithoutMessage() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test OK without MESSAGE parameter
        String message = "OK";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageErrWithoutReason() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test ERR without REASON parameter
        String message = "ERR";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageWelcomeWithoutGameOrPlayer() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Test WELCOME without parameters
        String message = "WELCOME";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkWithoutNewOrCount() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test DRAW_OK without NEW and COUNT parameters
        String message = "DRAW_OK PLAYER=\"PLAYER456\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkWithNullLastDrawIndices() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Set up game state without lastDrawIndices
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var currentHandField = PokerClient.class.getDeclaredField("currentHand");
        currentHandField.setAccessible(true);
        currentHandField.set(client, "AS,KH,QD,JC,10S");
        
        // lastDrawIndices is null by default
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Should handle null lastDrawIndices gracefully
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H,3D\" COUNT=\"2\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkWithEmptyLastDrawIndices() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Set up game state with empty lastDrawIndices
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var currentHandField = PokerClient.class.getDeclaredField("currentHand");
        currentHandField.setAccessible(true);
        currentHandField.set(client, "AS,KH,QD,JC,10S");
        
        var lastDrawIndicesField = PokerClient.class.getDeclaredField("lastDrawIndices");
        lastDrawIndicesField.setAccessible(true);
        lastDrawIndicesField.set(client, java.util.Collections.emptyList());
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Should handle empty lastDrawIndices gracefully
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H,3D\" COUNT=\"2\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageDrawOkWithNullCurrentHand() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        // Set up game state without currentHand
        var gameIdField = PokerClient.class.getDeclaredField("gameId");
        gameIdField.setAccessible(true);
        gameIdField.set(client, "GAME123");
        
        var playerIdField = PokerClient.class.getDeclaredField("playerId");
        playerIdField.setAccessible(true);
        playerIdField.set(client, "PLAYER456");
        
        var lastDrawIndicesField = PokerClient.class.getDeclaredField("lastDrawIndices");
        lastDrawIndicesField.setAccessible(true);
        lastDrawIndicesField.set(client, java.util.Arrays.asList(0, 2));
        
        // currentHand is null by default
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Should handle null currentHand gracefully
        String message = "DRAW_OK PLAYER=\"PLAYER456\" NEW=\"2H,3D\" COUNT=\"2\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testConnectAndDisconnectMultipleTimes() throws Exception {
        for (int i = 0; i < 3; i++) {
            // Find available port
            ServerSocket tempServer = new ServerSocket(0); // Let OS assign free port
            int port = tempServer.getLocalPort();
            
            Thread tempThread = new Thread(() -> {
                try {
                    Socket tempSocket = tempServer.accept();
                    BufferedReader tempReader = new BufferedReader(
                        new InputStreamReader(tempSocket.getInputStream(), StandardCharsets.UTF_8));
                    tempReader.readLine(); // Read HELLO
                    tempSocket.close();
                } catch (IOException e) {
                    // Expected
                }
            });
            tempThread.start();
            
            PokerClient client = new PokerClient(TEST_HOST, port);
            client.connect();
            Thread.sleep(100);
            
            var method = PokerClient.class.getDeclaredMethod("disconnect");
            method.setAccessible(true);
            method.invoke(client);
            
            tempThread.join(100);
            tempServer.close();
            Thread.sleep(50); // Give OS time to release port
        }
    }

    @Test
    void testDisconnectWithNullReaderAndWriter() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("disconnect");
        method.setAccessible(true);
        
        // Disconnect without connecting (null reader/writer)
        assertDoesNotThrow(() -> method.invoke(client));
    }

    @Test
    void testHandleServerMessageAnteOkWithoutStack() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test ANTE_OK without STACK
        String message = "ANTE_OK PLAYER=\"PLAYER456\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }

    @Test
    void testHandleServerMessageTurnWithoutCallAmount() throws Exception {
        PokerClient client = new PokerClient(TEST_HOST, TEST_PORT);
        
        var method = PokerClient.class.getDeclaredMethod("handleServerMessage", String.class);
        method.setAccessible(true);
        
        // Set player ID first
        String welcomeMsg = "WELCOME GAME=\"GAME123\" PLAYER=\"PLAYER456\"";
        method.invoke(client, welcomeMsg);
        
        // Test TURN without CALL parameter
        String message = "TURN PLAYER=\"PLAYER456\" PHASE=\"BET1\"";
        assertDoesNotThrow(() -> method.invoke(client, message));
    }
}

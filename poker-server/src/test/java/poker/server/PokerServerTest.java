package poker.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PokerServerTest {
    private static final int TEST_PORT = 17777;
    private PokerServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() {
        server = new PokerServer(TEST_PORT);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
            try {
                serverThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    @Timeout(5)
    void testServerStartAndStop() throws Exception {
        CountDownLatch serverStarted = new CountDownLatch(1);
        
        serverThread = new Thread(() -> {
            try {
                serverStarted.countDown();
                server.start();
            } catch (IOException e) {
                // Expected when server is stopped
            }
        });
        serverThread.start();
        
        // Wait for server to start
        assertTrue(serverStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(100); // Give it a moment to bind
        
        assertNotNull(server.getSelector());
        assertTrue(server.getSelector().isOpen());
        
        server.stop();
        serverThread.join(2000);
        
        assertFalse(server.getSelector().isOpen());
    }

    @Test
    @Timeout(5)
    void testServerAcceptsConnections() throws Exception {
        CountDownLatch serverStarted = new CountDownLatch(1);
        
        serverThread = new Thread(() -> {
            try {
                serverStarted.countDown();
                server.start();
            } catch (IOException e) {
                // Expected when server is stopped
            }
        });
        serverThread.start();
        
        assertTrue(serverStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        // Try to connect
        try (SocketChannel client = SocketChannel.open()) {
            boolean connected = client.connect(new InetSocketAddress("localhost", TEST_PORT));
            assertTrue(connected || client.finishConnect());
            assertTrue(client.isConnected());
        }
        
        server.stop();
    }

    @Test
    @Timeout(5)
    void testMultipleClientConnections() throws Exception {
        CountDownLatch serverStarted = new CountDownLatch(1);
        
        serverThread = new Thread(() -> {
            try {
                serverStarted.countDown();
                server.start();
            } catch (IOException e) {
                // Expected when server is stopped
            }
        });
        serverThread.start();
        
        assertTrue(serverStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        // Connect multiple clients
        SocketChannel client1 = SocketChannel.open();
        SocketChannel client2 = SocketChannel.open();
        SocketChannel client3 = SocketChannel.open();
        
        try {
            client1.connect(new InetSocketAddress("localhost", TEST_PORT));
            client1.finishConnect();
            assertTrue(client1.isConnected());
            
            client2.connect(new InetSocketAddress("localhost", TEST_PORT));
            client2.finishConnect();
            assertTrue(client2.isConnected());
            
            client3.connect(new InetSocketAddress("localhost", TEST_PORT));
            client3.finishConnect();
            assertTrue(client3.isConnected());
        } finally {
            client1.close();
            client2.close();
            client3.close();
            server.stop();
        }
    }

    @Test
    void testServerCannotStartTwice() throws Exception {
        CountDownLatch serverStarted = new CountDownLatch(1);
        
        serverThread = new Thread(() -> {
            try {
                serverStarted.countDown();
                server.start();
            } catch (IOException e) {
                // Expected when server is stopped
            }
        });
        serverThread.start();
        
        assertTrue(serverStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        // Try to start again
        assertThrows(IllegalStateException.class, () -> server.start());
        
        server.stop();
    }

    @Test
    void testServerStopIsIdempotent() {
        assertDoesNotThrow(() -> {
            server.stop();
            server.stop();
            server.stop();
        });
    }

    @Test
    void testServerConstructorWithCustomPort() {
        int customPort = 18888;
        PokerServer customServer = new PokerServer(customPort);
        
        assertNotNull(customServer);
        
        customServer.stop();
    }

    @Test
    @Timeout(5)
    void testServerHandlesClientDisconnection() throws Exception {
        CountDownLatch serverStarted = new CountDownLatch(1);
        
        serverThread = new Thread(() -> {
            try {
                serverStarted.countDown();
                server.start();
            } catch (IOException e) {
                // Expected when server is stopped
            }
        });
        serverThread.start();
        
        assertTrue(serverStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        SocketChannel client = SocketChannel.open();
        client.connect(new InetSocketAddress("localhost", TEST_PORT));
        client.finishConnect();
        assertTrue(client.isConnected());
        
        // Close client connection
        client.close();
        Thread.sleep(100); // Give server time to process disconnect
        
        // Server should still be running
        assertNotNull(server.getSelector());
        assertTrue(server.getSelector().isOpen());
        
        server.stop();
    }
}

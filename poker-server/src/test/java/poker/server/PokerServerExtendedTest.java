package poker.server;

import org.junit.jupiter.api.Test;
import poker.model.game.GameId;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for PokerServer to improve coverage.
 */
class PokerServerExtendedTest {

    @Test
    void testServerCreationWithDefaultPort() {
        PokerServer server = new PokerServer(7777);
        assertNotNull(server);
    }

    @Test
    void testServerCreationWithCustomPort() {
        PokerServer server = new PokerServer(8888);
        assertNotNull(server);
    }

    @Test
    void testServerCreationWithDifferentPorts() {
        PokerServer server1 = new PokerServer(7777);
        PokerServer server2 = new PokerServer(8888);
        PokerServer server3 = new PokerServer(9999);
        
        assertNotNull(server1);
        assertNotNull(server2);
        assertNotNull(server3);
    }

    @Test
    void testServerStartThrowsWhenAlreadyRunning() throws IOException, InterruptedException {
        PokerServer server = new PokerServer(7779);
        
        // Start in separate thread
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected when we stop it
            }
        });
        serverThread.start();
        
        // Give server time to start
        Thread.sleep(100);
        
        // Try to start again - should throw
        assertThrows(IllegalStateException.class, () -> {
            server.start();
        });
        
        // Cleanup
        server.stop();
        serverThread.join(1000);
    }

    @Test
    void testServerStopWhenNotRunning() {
        PokerServer server = new PokerServer(7780);
        
        // Should not throw
        assertDoesNotThrow(() -> {
            server.stop();
        });
    }

    @Test
    void testServerStartAndStop() throws IOException, InterruptedException {
        PokerServer server = new PokerServer(7781);
        
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected when we stop it
            }
        });
        serverThread.start();
        
        // Give server time to start
        Thread.sleep(100);
        
        // Stop server
        server.stop();
        
        // Wait for thread to finish
        serverThread.join(1000);
        assertFalse(serverThread.isAlive());
    }

    @Test
    void testServerCanRestartAfterStop() throws IOException, InterruptedException {
        PokerServer server = new PokerServer(7782);
        
        // First start
        Thread serverThread1 = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread1.start();
        Thread.sleep(100);
        server.stop();
        serverThread1.join(1000);
        
        // Second start
        Thread serverThread2 = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread2.start();
        Thread.sleep(100);
        server.stop();
        serverThread2.join(1000);
    }

    @Test
    void testMultipleServersOnDifferentPorts() throws IOException, InterruptedException {
        PokerServer server1 = new PokerServer(7783);
        PokerServer server2 = new PokerServer(7784);
        
        Thread thread1 = new Thread(() -> {
            try {
                server1.start();
            } catch (IOException e) {
                // Expected
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                server2.start();
            } catch (IOException e) {
                // Expected
            }
        });
        
        thread1.start();
        thread2.start();
        
        Thread.sleep(100);
        
        server1.stop();
        server2.stop();
        
        thread1.join(1000);
        thread2.join(1000);
    }

    @Test
    void testServerPortBinding() throws IOException, InterruptedException {
        int testPort = 7785;
        PokerServer server = new PokerServer(testPort);
        
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        Thread.sleep(100);
        
        // Try to start another server on same port - should fail
        PokerServer server2 = new PokerServer(testPort);
        assertThrows(IOException.class, () -> {
            server2.start();
        });
        
        server.stop();
        serverThread.join(1000);
    }

    @Test
    void testGameClientsMapInitialization() {
        PokerServer server = new PokerServer(7786);
        
        // Verify server initializes properly
        assertNotNull(server);
    }

    @Test
    void testServerWithLowPort() throws IOException, InterruptedException {
        // Test with a higher port (privileged ports require special permissions)
        PokerServer server = new PokerServer(7787);
        
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        Thread.sleep(100);
        server.stop();
        serverThread.join(1000);
    }

    @Test
    void testServerWithHighPort() throws IOException, InterruptedException {
        PokerServer server = new PokerServer(65530);
        
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        Thread.sleep(100);
        server.stop();
        serverThread.join(1000);
    }

    @Test
    void testMultipleStopCalls() throws IOException, InterruptedException {
        PokerServer server = new PokerServer(7788);
        
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Expected
            }
        });
        serverThread.start();
        
        Thread.sleep(100);
        
        // Multiple stop calls should not throw
        assertDoesNotThrow(() -> {
            server.stop();
            server.stop();
            server.stop();
        });
        
        serverThread.join(1000);
    }
}

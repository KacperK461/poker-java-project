package poker.server;

import lombok.extern.slf4j.Slf4j;
import poker.model.game.GameId;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main poker server using JDK 21 virtual threads.
 */
@Slf4j
public class PokerServer {
    private static final int DEFAULT_PORT = 7777;
    
    private final int port;
    private final GameManager gameManager;
    private final Map<GameId, Set<ClientHandler>> gameClients;
    private final ExecutorService executor;
    private volatile boolean running;

    public PokerServer(int port) {
        this.port = port;
        this.gameManager = new GameManager();
        this.gameClients = new ConcurrentHashMap<>();
        this.executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        this.running = false;
    }

    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Poker server started on port {}", port);
            log.info("Using virtual threads for client connections");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, gameManager, gameClients);
                    executor.submit(handler);
                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        log.info("Shutting down server...");
        running = false;
        executor.shutdown();
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }

        PokerServer server = new PokerServer(port);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start server", e);
            System.exit(1);
        }
    }
}

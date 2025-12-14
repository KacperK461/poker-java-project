package poker.server;

import lombok.extern.slf4j.Slf4j;
import poker.model.game.GameId;
import poker.server.GameManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NIO-based poker server using non-blocking I/O with channels and selectors.
 * Implements Bonus 1: java.nio usage.
 */
@Slf4j
public class PokerServer {
    private static final int DEFAULT_PORT = 7777;
    
    private final int port;
    private final GameManager gameManager;
    private final Map<GameId, Set<ClientHandler>> gameClients;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running;

    public PokerServer(int port) {
        this.port = port;
        this.gameManager = new GameManager();
        this.gameClients = new ConcurrentHashMap<>();
        this.running = false;
    }

    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        running = true;
        log.info("NIO Poker server started on port {}", port);
        log.info("Using non-blocking I/O with Selector");

        while (running) {
            try {
                // Block until at least one channel is ready
                selector.select();

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (IOException e) {
                        log.error("Error handling key", e);
                        closeClient(key);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Error in selector loop", e);
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            
            ClientHandler handler = new ClientHandler(
                clientChannel, 
                gameManager, 
                gameClients,
                this
            );
            
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
            clientKey.attach(handler);
            
            log.info("Accepted connection from {}", clientChannel.getRemoteAddress());
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        if (handler != null) {
            handler.handleRead(key);
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        if (handler != null) {
            handler.handleWrite(key);
        }
    }

    private void closeClient(SelectionKey key) {
        ClientHandler handler = (ClientHandler) key.attachment();
        if (handler != null) {
            handler.close();
        }
        key.cancel();
    }

    public Selector getSelector() {
        return selector;
    }

    public void stop() {
        running = false;
        
        try {
            if (selector != null && selector.isOpen()) {
                selector.wakeup();
                selector.close();
            }
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
        } catch (IOException e) {
            log.error("Error stopping server", e);
        }
        
        log.info("NIO Poker server stopped");
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        
        PokerServer server = new PokerServer(port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received");
            server.stop();
        }));

        try {
            server.start();
        } catch (IOException e) {
            log.error("Server error", e);
            System.exit(1);
        }
    }
}

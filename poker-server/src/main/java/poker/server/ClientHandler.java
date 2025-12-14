package poker.server;

import lombok.extern.slf4j.Slf4j;
import poker.common.cards.Card;
import poker.model.exceptions.InvalidMoveException;
import poker.model.exceptions.ProtocolException;
import poker.model.game.*;
import poker.model.players.Player;
import poker.model.players.PlayerId;
import poker.model.protocol.Message;
import poker.model.protocol.ServerMessage;
import poker.server.GameManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * NIO-based client handler using non-blocking SocketChannel.
 */
@Slf4j
public class ClientHandler {
    private static final int BUFFER_SIZE = 8192;
    private static final String LINE_SEPARATOR = "\n";

    private final SocketChannel channel;
    private final GameManager gameManager;
    private final Map<GameId, Set<ClientHandler>> gameClients;
    private final PokerServer server;
    
    private final ByteBuffer readBuffer;
    private final StringBuilder messageBuilder;
    private final Queue<String> writeQueue;
    
    private PlayerId playerId;
    private GameId currentGameId;

    public ClientHandler(
            SocketChannel channel,
            GameManager gameManager,
            Map<GameId, Set<ClientHandler>> gameClients,
            PokerServer server) {
        this.channel = channel;
        this.gameManager = gameManager;
        this.gameClients = gameClients;
        this.server = server;
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.messageBuilder = new StringBuilder();
        this.writeQueue = new ConcurrentLinkedQueue<>();
    }

    public void handleRead(SelectionKey key) throws IOException {
        int bytesRead = channel.read(readBuffer);
        
        if (bytesRead == -1) {
            // Client disconnected
            close();
            return;
        }

        if (bytesRead > 0) {
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            readBuffer.clear();

            String chunk = new String(data, StandardCharsets.UTF_8);
            messageBuilder.append(chunk);

            // Process complete messages (lines ending with \n)
            processMessages();
        }
    }

    private void processMessages() {
        String buffered = messageBuilder.toString();
        int newlineIndex;
        
        while ((newlineIndex = buffered.indexOf(LINE_SEPARATOR)) != -1) {
            String message = buffered.substring(0, newlineIndex).trim();
            buffered = buffered.substring(newlineIndex + 1);
            
            if (!message.isEmpty()) {
                processMessage(message);
            }
        }
        
        messageBuilder.setLength(0);
        messageBuilder.append(buffered);
    }

    private void processMessage(String line) {
        log.debug("Received: {}", line);

        try {
            Message.ParsedMessage parsed = Message.parse(line);
            String action = parsed.getAction();

            switch (action) {
                case "HELLO" -> handleHello(parsed);
                case "CREATE" -> handleCreate(parsed);
                case "JOIN" -> handleJoin(parsed);
                case "LEAVE" -> handleLeave();
                case "START" -> handleStart();
                case "CHECK" -> handleCheck();
                case "CALL" -> handleCall();
                case "BET" -> handleBet(parsed);
                case "FOLD" -> handleFold();
                case "DRAW" -> handleDraw(parsed);
                default -> sendError("UNKNOWN_ACTION", "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", line, e);
            sendError("INVALID_FORMAT", "Invalid message format");
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        while (!writeQueue.isEmpty()) {
            String message = writeQueue.peek();
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
            
            int written = channel.write(buffer);
            
            if (buffer.hasRemaining()) {
                // Couldn't write all data, will try again later
                return;
            }
            
            // Successfully wrote the message
            writeQueue.poll();
        }
        
        // No more data to write, remove write interest
        if (writeQueue.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public void send(String message) {
        if (!message.endsWith(LINE_SEPARATOR)) {
            message += LINE_SEPARATOR;
        }
        
        log.debug("Queuing: {}", message.trim());
        writeQueue.offer(message);
        
        // Register write interest
        try {
            for (SelectionKey key : server.getSelector().keys()) {
                if (key.channel() == channel && key.isValid()) {
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    server.getSelector().wakeup();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error registering write interest", e);
        }
    }

    private void sendError(String code, String message) {
        send(ServerMessage.error(code, message).toProtocolString());
    }

    private void handleHello(Message.ParsedMessage msg) {
        String version = msg.getParams().get("VERSION");
        log.info("Client hello, version: {}", version);
        send(ServerMessage.ok("Welcome to Poker Server").toProtocolString());
    }

    private void handleCreate(Message.ParsedMessage msg) {
        try {
            int ante = Integer.parseInt(msg.getParams().get("ANTE"));
            int bet = Integer.parseInt(msg.getParams().get("BET"));

            GameConfig config = GameConfig.builder()
                .ante(ante)
                .fixedBet(bet)
                .build();

            GameId gameId = gameManager.createGame(config);
            currentGameId = gameId;

            log.info("Game created: {}", gameId.getId());
            send(ServerMessage.ok("Game created: " + gameId.getId()).toProtocolString());
        } catch (Exception e) {
            sendError("CREATE_FAILED", e.getMessage());
        }
    }

    private void handleJoin(Message.ParsedMessage msg) {
        try {
            String gameIdStr = msg.getParams().get("GAME");
            String playerName = msg.getParams().get("NAME");

            if (gameIdStr == null || playerName == null) {
                throw new ProtocolException("MISSING_PARAM", "GAME and NAME required");
            }

            GameId gameId = GameId.of(gameIdStr);
            PlayerId newPlayerId = PlayerId.generate();

            PokerGame game = gameManager.getGame(gameId);
            game.addPlayer(newPlayerId, playerName);

            this.playerId = newPlayerId;
            this.currentGameId = gameId;

            // Add to game clients
            gameClients.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(this);

            log.info("Player {} joined game {}", playerName, gameId.getId());

            send(ServerMessage.welcome(gameId.getId(), newPlayerId.getId()).toProtocolString());

            // Broadcast lobby update
            broadcastLobby(game);
        } catch (Exception e) {
            sendError("JOIN_FAILED", e.getMessage());
        }
    }

    private void handleLeave() {
        if (currentGameId == null || playerId == null) {
            sendError("NOT_IN_GAME", "Not in a game");
            return;
        }

        try {
            PokerGame game = gameManager.getGame(currentGameId);
            game.removePlayer(playerId);

            gameClients.get(currentGameId).remove(this);

            send(ServerMessage.ok("Left game").toProtocolString());
            broadcastLobby(game);

            currentGameId = null;
            playerId = null;
        } catch (Exception e) {
            sendError("LEAVE_FAILED", e.getMessage());
        }
    }

    private void handleStart() {
        if (currentGameId == null) {
            sendError("NOT_IN_GAME", "Not in a game");
            return;
        }

        try {
            PokerGame game = gameManager.getGame(currentGameId);
            game.startGame();

            GameConfig config = game.getConfig();
            broadcast(currentGameId, ServerMessage.started(
                currentGameId.getId(),
                game.getDealerId().getId(),
                config.getAnte(),
                config.getFixedBet()
            ).toProtocolString());

            // Collect ante
            game.collectAnte();
            for (Player player : game.getAllPlayers()) {
                broadcast(currentGameId, ServerMessage.anteOk(
                    currentGameId.getId(),
                    player.getId().getId(),
                    player.getChips()
                ).toProtocolString());
            }

            // Deal cards
            game.dealInitialCards();
            dealCards(game);

            // Start first betting round
            notifyTurn(game);
        } catch (Exception e) {
            sendError("START_FAILED", e.getMessage());
        }
    }

    private void handleCheck() {
        handleGameAction(game -> {
            game.check(playerId);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "CHECK", "").toProtocolString());
            advanceGame(game);
        });
    }

    private void handleCall() {
        handleGameAction(game -> {
            game.call(playerId);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "CALL", "").toProtocolString());
            advanceGame(game);
        });
    }

    private void handleBet(Message.ParsedMessage msg) {
        handleGameAction(game -> {
            int amount = Integer.parseInt(msg.getParams().get("AMOUNT"));
            game.raise(playerId, amount);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "BET", String.valueOf(amount)).toProtocolString());
            advanceGame(game);
        });
    }

    private void handleFold() {
        handleGameAction(game -> {
            game.fold(playerId);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "FOLD", "").toProtocolString());
            advanceGame(game);
        });
    }

    private void handleDraw(Message.ParsedMessage msg) {
        handleGameAction(game -> {
            String cardsStr = msg.getParams().get("CARDS");
            List<Integer> indices = parseCardIndices(cardsStr);
            
            List<Card> newCards = game.draw(playerId, indices);
            
            broadcast(currentGameId, ServerMessage.drawOk(
                currentGameId.getId(),
                playerId.getId(),
                indices.size(),
                "*"
            ).toProtocolString());
            
            // Send new cards only to the player
            String cardStr = newCards.stream()
                .map(Card::toString)
                .collect(Collectors.joining(","));
            send(ServerMessage.drawOk(
                currentGameId.getId(),
                playerId.getId(),
                indices.size(),
                cardStr
            ).toProtocolString());
            
            advanceGame(game);
        });
    }

    private List<Integer> parseCardIndices(String cardsStr) {
        if (cardsStr == null || cardsStr.isEmpty() || cardsStr.equalsIgnoreCase("none")) {
            return List.of();
        }
        return Arrays.stream(cardsStr.split(","))
            .map(String::trim)
            .map(Integer::parseInt)
            .toList();
    }

    private void handleGameAction(GameAction action) {
        if (currentGameId == null || playerId == null) {
            sendError("NOT_IN_GAME", "Not in a game");
            return;
        }

        try {
            PokerGame game = gameManager.getGame(currentGameId);
            action.execute(game);
        } catch (InvalidMoveException e) {
            sendError(e.getCode(), e.getMessage());
        } catch (Exception e) {
            sendError("ACTION_FAILED", e.getMessage());
        }
    }

    private void advanceGame(PokerGame game) {
        GameState state = game.getState();

        if (state == GameState.SHOWDOWN) {
            // Evaluate hands
            Map<PlayerId, HandRank> rankings = game.showdown();
            
            for (Map.Entry<PlayerId, HandRank> entry : rankings.entrySet()) {
                Player player = game.getPlayer(entry.getKey());
                String handStr = player.getHand().stream()
                    .map(Card::toString)
                    .collect(Collectors.joining(","));
                
                broadcast(currentGameId, ServerMessage.showdown(
                    currentGameId.getId(),
                    entry.getKey().getId(),
                    handStr,
                    entry.getValue().toProtocolString()
                ).toProtocolString());
            }

            // Distribute pot
            List<PokerGame.Payout> payouts = game.distributePot(rankings);
            
            for (PokerGame.Payout payout : payouts) {
                HandRank winningRank = rankings.get(payout.playerId());
                
                broadcast(currentGameId, ServerMessage.winner(
                    currentGameId.getId(),
                    payout.playerId().getId(),
                    payout.amount(),
                    winningRank.toProtocolString()
                ).toProtocolString());
                
                broadcast(currentGameId, ServerMessage.payout(
                    currentGameId.getId(),
                    payout.playerId().getId(),
                    payout.amount(),
                    payout.newStack()
                ).toProtocolString());
            }

            broadcast(currentGameId, ServerMessage.end(currentGameId.getId(), "Normal").toProtocolString());
        } else if (state == GameState.DRAW || state == GameState.BET1 || state == GameState.BET2) {
            notifyTurn(game);
        }
    }

    private void dealCards(PokerGame game) {
        for (Player player : game.getAllPlayers()) {
            if (player.isActive()) {
                // Send masked cards to others
                broadcast(currentGameId, ServerMessage.deal(
                    currentGameId.getId(),
                    player.getId().getId(),
                    "*,*,*,*,*"
                ).toProtocolString());

                // Send actual cards to the player
                String cardStr = player.getHand().stream()
                    .map(Card::toString)
                    .collect(Collectors.joining(","));
                
                sendToPlayer(currentGameId, player.getId(), ServerMessage.deal(
                    currentGameId.getId(),
                    player.getId().getId(),
                    cardStr
                ).toProtocolString());
            }
        }
    }

    private void notifyTurn(PokerGame game) {
        PlayerId currentPlayer = game.getCurrentTurn();
        if (currentPlayer != null) {
            Player player = game.getPlayer(currentPlayer);
            int callAmount = Math.max(0, game.getCurrentBet() - player.getCurrentBet());

            broadcast(currentGameId, ServerMessage.turn(
                currentGameId.getId(),
                currentPlayer.getId(),
                game.getState().name(),
                callAmount,
                player.getChips()
            ).toProtocolString());
        }
    }

    private void broadcastLobby(PokerGame game) {
        String playerNames = game.getAllPlayers().stream()
            .map(Player::getName)
            .collect(Collectors.joining(","));
        broadcast(currentGameId, ServerMessage.lobby(currentGameId.getId(), playerNames).toProtocolString());
    }

    private void broadcast(GameId gameId, String message) {
        Set<ClientHandler> clients = gameClients.get(gameId);
        if (clients != null) {
            for (ClientHandler client : clients) {
                client.send(message);
            }
        }
    }

    private void sendToPlayer(GameId gameId, PlayerId targetPlayerId, String message) {
        Set<ClientHandler> clients = gameClients.get(gameId);
        if (clients != null) {
            for (ClientHandler handler : clients) {
                if (targetPlayerId.equals(handler.playerId)) {
                    handler.send(message);
                    break;
                }
            }
        }
    }

    @FunctionalInterface
    private interface GameAction {
        void execute(PokerGame game) throws Exception;
    }

    public void close() {
        try {
            if (currentGameId != null && playerId != null) {
                try {
                    PokerGame game = gameManager.getGame(currentGameId);
                    game.removePlayer(playerId);
                    
                    Set<ClientHandler> clients = gameClients.get(currentGameId);
                    if (clients != null) {
                        clients.remove(this);
                    }
                } catch (Exception e) {
                    log.error("Error removing player on disconnect", e);
                }
            }
            
            channel.close();
            log.info("Client disconnected");
        } catch (IOException e) {
            log.error("Error closing channel", e);
        }
    }
}

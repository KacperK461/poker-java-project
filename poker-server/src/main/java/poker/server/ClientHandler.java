package poker.server;

import lombok.extern.slf4j.Slf4j;
import poker.model.game.*;
import poker.model.players.Player;
import poker.model.players.PlayerId;
import poker.model.protocol.Message;
import poker.model.protocol.ServerMessage;
import poker.model.exceptions.*;
import poker.common.cards.Card;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles a client connection in the poker server.
 */
@Slf4j
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameManager gameManager;
    private final Map<GameId, Set<ClientHandler>> gameClients;
    
    private BufferedReader reader;
    private PrintWriter writer;
    private PlayerId playerId;
    private GameId currentGameId;
    private volatile boolean running;

    public ClientHandler(Socket socket, GameManager gameManager, 
                        Map<GameId, Set<ClientHandler>> gameClients) {
        this.socket = socket;
        this.gameManager = gameManager;
        this.gameClients = gameClients;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            log.info("Client connected from {}", socket.getRemoteSocketAddress());

            String line;
            while (running && (line = reader.readLine()) != null) {
                try {
                    handleMessage(line.trim());
                } catch (Exception e) {
                    log.error("Error handling message: {}", line, e);
                    sendError(e);
                }
            }
        } catch (IOException e) {
            log.error("Connection error", e);
        } finally {
            cleanup();
        }
    }

    private void handleMessage(String line) {
        if (line.isEmpty()) {
            return;
        }

        log.debug("Received: {}", line);

        Message.ParsedMessage parsed = Message.parse(line);
        String action = parsed.getAction();

        switch (action) {
            case "HELLO" -> handleHello(parsed);
            case "CREATE" -> handleCreate(parsed);
            case "JOIN" -> handleJoin(parsed);
            case "LEAVE" -> handleLeave(parsed);
            case "START" -> handleStart(parsed);
            case "CHECK" -> handleCheck(parsed);
            case "CALL" -> handleCall(parsed);
            case "BET" -> handleBet(parsed);
            case "FOLD" -> handleFold(parsed);
            case "DRAW" -> handleDraw(parsed);
            case "STATUS" -> handleStatus(parsed);
            case "QUIT" -> handleQuit(parsed);
            default -> sendError("UNKNOWN_ACTION", "Unknown action: " + action);
        }
    }

    private void handleHello(Message.ParsedMessage msg) {
        String version = msg.getParams().get("VERSION");
        log.info("Client hello, version: {}", version);
        send(ServerMessage.ok("Welcome to Poker Server"));
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
            send(ServerMessage.ok("Game created: " + gameId.getId()));
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

            send(ServerMessage.welcome(gameId.getId(), newPlayerId.getId()));

            // Broadcast lobby update
            broadcastLobby(game);
        } catch (Exception e) {
            sendError("JOIN_FAILED", e.getMessage());
        }
    }

    private void handleLeave(Message.ParsedMessage msg) {
        if (currentGameId == null || playerId == null) {
            sendError("NOT_IN_GAME", "Not in a game");
            return;
        }

        try {
            PokerGame game = gameManager.getGame(currentGameId);
            game.removePlayer(playerId);

            gameClients.get(currentGameId).remove(this);

            send(ServerMessage.ok("Left game"));
            broadcastLobby(game);

            currentGameId = null;
            playerId = null;
        } catch (Exception e) {
            sendError("LEAVE_FAILED", e.getMessage());
        }
    }

    private void handleStart(Message.ParsedMessage msg) {
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
            ));

            // Collect ante
            game.collectAnte();
            for (Player player : game.getAllPlayers()) {
                broadcast(currentGameId, ServerMessage.anteOk(
                    currentGameId.getId(),
                    player.getId().getId(),
                    player.getChips()
                ));
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

    private void handleCheck(Message.ParsedMessage msg) {
        handleGameAction(game -> {
            game.check(playerId);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "CHECK", ""));
            advanceGame(game);
        });
    }

    private void handleCall(Message.ParsedMessage msg) {
        handleGameAction(game -> {
            game.call(playerId);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "CALL", ""));
            advanceGame(game);
        });
    }

    private void handleBet(Message.ParsedMessage msg) {
        handleGameAction(game -> {
            int amount = Integer.parseInt(msg.getParams().get("AMOUNT"));
            game.raise(playerId, amount);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "BET", String.valueOf(amount)));
            advanceGame(game);
        });
    }

    private void handleFold(Message.ParsedMessage msg) {
        handleGameAction(game -> {
            game.fold(playerId);
            broadcast(currentGameId, ServerMessage.action(
                currentGameId.getId(), playerId.getId(), "FOLD", ""));
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
            ));
            
            // Send new cards only to the player
            String cardStr = newCards.stream()
                .map(Card::toString)
                .collect(Collectors.joining(","));
            send(ServerMessage.drawOk(
                currentGameId.getId(),
                playerId.getId(),
                indices.size(),
                cardStr
            ));
            
            advanceGame(game);
        });
    }

    private void handleStatus(Message.ParsedMessage msg) {
        if (currentGameId == null) {
            sendError("NOT_IN_GAME", "Not in a game");
            return;
        }

        try {
            PokerGame game = gameManager.getGame(currentGameId);
            send(ServerMessage.round(
                currentGameId.getId(),
                game.getPot(),
                game.getCurrentBet()
            ));
        } catch (Exception e) {
            sendError("STATUS_FAILED", e.getMessage());
        }
    }

    private void handleQuit(Message.ParsedMessage msg) {
        send(ServerMessage.ok("Goodbye"));
        running = false;
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
                ));
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
                ));
                
                broadcast(currentGameId, ServerMessage.payout(
                    currentGameId.getId(),
                    payout.playerId().getId(),
                    payout.amount(),
                    payout.newStack()
                ));
            }

            broadcast(currentGameId, ServerMessage.end(currentGameId.getId(), "Normal"));
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
                ));

                // Send actual cards to the player
                String cardStr = player.getHand().stream()
                    .map(Card::toString)
                    .collect(Collectors.joining(","));
                
                sendToPlayer(currentGameId, player.getId(), ServerMessage.deal(
                    currentGameId.getId(),
                    player.getId().getId(),
                    cardStr
                ));
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
                game.getConfig().getFixedBet()
            ));
        }
    }

    private void broadcastLobby(PokerGame game) {
        String playerList = game.getAllPlayers().stream()
            .map(Player::getName)
            .collect(Collectors.joining(","));

        broadcast(currentGameId, ServerMessage.lobby(
            currentGameId.getId(),
            playerList
        ));
    }

    private List<Integer> parseCardIndices(String cardsStr) {
        if (cardsStr == null || cardsStr.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(cardsStr.split(","))
            .map(String::trim)
            .map(Integer::parseInt)
            .collect(Collectors.toList());
    }

    private void send(ServerMessage message) {
        if (writer != null) {
            String msg = message.toProtocolString();
            log.debug("Sending to {}: {}", playerId != null ? playerId.getId() : "unknown", msg);
            writer.println(msg);
        }
    }

    private void sendError(String code, String reason) {
        send(ServerMessage.error(code, reason));
    }

    private void sendError(Exception e) {
        if (e instanceof InvalidMoveException ime) {
            sendError(ime.getCode(), ime.getMessage());
        } else if (e instanceof ProtocolException pe) {
            sendError(pe.getCode(), pe.getMessage());
        } else {
            sendError("ERROR", e.getMessage());
        }
    }

    private void broadcast(GameId gameId, ServerMessage message) {
        Set<ClientHandler> clients = gameClients.get(gameId);
        if (clients != null) {
            for (ClientHandler client : clients) {
                client.send(message);
            }
        }
    }

    private void sendToPlayer(GameId gameId, PlayerId targetPlayerId, ServerMessage message) {
        Set<ClientHandler> clients = gameClients.get(gameId);
        if (clients != null) {
            for (ClientHandler client : clients) {
                if (targetPlayerId.equals(client.playerId)) {
                    client.send(message);
                    break;
                }
            }
        }
    }

    private void cleanup() {
        running = false;

        if (currentGameId != null && playerId != null) {
            try {
                PokerGame game = gameManager.getGame(currentGameId);
                if (game != null && game.getState() == GameState.LOBBY) {
                    game.removePlayer(playerId);
                }
                
                Set<ClientHandler> clients = gameClients.get(currentGameId);
                if (clients != null) {
                    clients.remove(this);
                }
            } catch (Exception e) {
                log.error("Error during cleanup", e);
            }
        }

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("Error closing connection", e);
        }

        log.info("Client disconnected");
    }

    @FunctionalInterface
    private interface GameAction {
        void execute(PokerGame game) throws Exception;
    }
}

package poker.client;

import lombok.extern.slf4j.Slf4j;
import poker.model.protocol.ClientMessage;
import poker.model.protocol.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Console-based poker client.
 */
@Slf4j
public class PokerClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 7777;
    private static final String VERSION = "1.0";

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String gameId;
    private String playerId;
    private String currentHand;
    private List<Integer> lastDrawIndices;
    private volatile boolean running;

    public PokerClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.running = false;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        log.info("Connected to {}:{}", host, port);
        System.out.println("Connected to poker server!");
        System.out.println("========================================");

        // Send hello
        send(ClientMessage.hello(VERSION));
    }

    public void run() {
        if (socket == null || !socket.isConnected()) {
            System.err.println("Not connected to server");
            return;
        }

        running = true;

        // Start reader thread
        Thread readerThread = Thread.ofVirtual().start(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleServerMessage(line);
                }
            } catch (IOException e) {
                if (running) {
                    log.error("Error reading from server", e);
                }
            }
        });

        // Main input loop
        try (Scanner scanner = new Scanner(System.in)) {
            printHelp();

            while (running) {
                System.out.print("> ");
                System.out.flush();
                
                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }

                try {
                    handleUserInput(input);
                    // Small delay to let server response arrive before showing next prompt
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    log.error("Error handling input", e);
                }
            }
        }

        // Wait for reader thread to finish
        try {
            readerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        disconnect();
    }

    private void handleServerMessage(String line) {
        log.debug("Received: {}", line);
        
        try {
            Message.ParsedMessage msg = Message.parse(line);
            String action = msg.getAction();
            boolean shouldShowPrompt = false;

            switch (action) {
                case "OK" -> {
                    String message = msg.getParams().get("MESSAGE");
                    if (message != null) {
                        if (message.startsWith("Game created:")) {
                            String gId = message.substring("Game created: ".length());
                            System.out.println("\n[OK] Game created successfully!");
                            System.out.println("  Game ID: " + gId);
                            System.out.println("  Share this ID with other players to join");
                        } else if (message.startsWith("Left game")) {
                            System.out.println("[OK] " + message);
                        } else if (!message.equals("Welcome to Poker Server")) {
                            System.out.println("[OK] " + message);
                        }
                    }
                }
                case "ERR" -> {
                    String reason = msg.getParams().get("REASON");
                    System.out.println("\n[ERROR] " + reason);
                }
                case "WELCOME" -> {
                    gameId = msg.getParams().get("GAME");
                    playerId = msg.getParams().get("PLAYER");
                    System.out.println("\n" + "=".repeat(40));
                    System.out.println("[OK] Successfully joined the game!");
                    System.out.println("  Your Player ID: " + playerId.substring(0, 8) + "...");
                    System.out.println("=".repeat(40));
                }
                case "LOBBY" -> {
                    String players = msg.getParams().get("PLAYERS");
                    String[] playerList = players.split(",");
                    System.out.println("\n[LOBBY] Players (" + playerList.length + "):");
                    for (String p : playerList) {
                        System.out.println("   - " + p);
                    }
                }
                case "STARTED" -> {
                    String ante = msg.getParams().get("ANTE");
                    String bet = msg.getParams().get("BET");
                    System.out.println("\n" + "=".repeat(40));
                    System.out.println("*** GAME STARTED ***");
                    System.out.println("   Ante: " + ante + " chips");
                    System.out.println("   Fixed Bet: " + bet + " chips");
                    System.out.println("=".repeat(40));
                }
                case "ANTE_OK" -> {
                    String player = msg.getParams().get("PLAYER");
                    String stack = msg.getParams().get("STACK");
                    if (player.equals(playerId)) {
                        System.out.println("[ANTE] Paid. Your stack: " + stack + " chips");
                    }
                }
                case "DEAL" -> {
                    String player = msg.getParams().get("PLAYER");
                    String cards = msg.getParams().get("CARDS");
                    if (player.equals(playerId) && !cards.equals("*,*,*,*,*")) {
                        currentHand = cards;
                        System.out.println("\n" + "=".repeat(40));
                        System.out.println("[YOUR HAND]");
                        System.out.println("   " + formatHandNice(cards));
                        System.out.println("=".repeat(40));
                    }
                }
                case "TURN" -> {
                    String player = msg.getParams().get("PLAYER");
                    String phase = msg.getParams().get("PHASE");
                    
                    if (player.equals(playerId)) {
                        System.out.println("\n" + "=".repeat(50));
                        System.out.println("*** YOUR TURN ***");
                        System.out.println("=".repeat(50));
                        
                        if ("DRAW".equals(phase)) {
                            System.out.println("\n[DRAW PHASE]");
                            System.out.println("   Your hand: " + formatHandNice(currentHand));
                            System.out.println("   Choose cards to replace or keep all");
                            System.out.println("   - draw 0,2,4  - Replace cards at positions 0, 2, 4");
                            System.out.println("   - draw none   - Keep all cards");
                        } else {
                            int callAmount = Integer.parseInt(msg.getParams().get("CALL"));
                            
                            System.out.println("\n[BETTING - " + phase + "]");
                            System.out.println("   Call amount: " + callAmount + " chips");
                            System.out.println("\n   Available actions:");
                            if (callAmount == 0) {
                                System.out.println("     - check       (no bet)");
                                System.out.println("     - bet <amt>   (make a bet)");
                            } else {
                                System.out.println("     - call        (match " + callAmount + " chips)");
                                System.out.println("     - bet <amt>   (raise)");
                            }
                            System.out.println("     - fold        (give up)");
                        }
                        System.out.println("=".repeat(50));
                    }
                }
                case "ACTION" -> {
                    String player = msg.getParams().get("PLAYER");
                    String type = msg.getParams().get("TYPE");
                    String args = msg.getParams().get("ARGS");
                    
                    if (!player.equals(playerId)) {
                        String actionDesc = switch (type) {
                            case "CHECK" -> "checked";
                            case "CALL" -> "called";
                            case "BET", "RAISE" -> args != null ? "bet " + args : "bet";
                            case "FOLD" -> "folded";
                            default -> type.toLowerCase();
                        };
                        System.out.println("   > Opponent " + actionDesc);
                        shouldShowPrompt = true;
                    }
                }
                case "DRAW_OK", "DRAWOK" -> {
                    String player = msg.getParams().get("PLAYER");
                    if (player != null && player.equals(playerId)) {
                        String newCards = msg.getParams().get("NEW");
                        String count = msg.getParams().get("COUNT");
                        
                        if (newCards != null && !newCards.equals("*") && !newCards.isEmpty()) {
                            // Update hand with new cards
                            if (lastDrawIndices != null && !lastDrawIndices.isEmpty() && currentHand != null) {
                                String[] hand = currentHand.split(",");
                                String[] drawn = newCards.split(",");
                                
                                // Replace the drawn cards with new ones
                                for (int i = 0; i < Math.min(lastDrawIndices.size(), drawn.length); i++) {
                                    int index = lastDrawIndices.get(i);
                                    if (index >= 0 && index < hand.length) {
                                        hand[index] = drawn[i];
                                    }
                                }
                                
                                currentHand = String.join(",", hand);
                            }
                            System.out.println("\n[DRAW] Drew " + count + " new card(s): " + newCards);
                        } else {
                            System.out.println("\n[DRAW] Kept all cards");
                        }
                    }
                }
                case "ROUND" -> {
                    String pot = msg.getParams().get("POT");
                    System.out.println("\n[POT] Current: " + pot + " chips");
                }
                case "SHOWDOWN" -> {
                    String player = msg.getParams().get("PLAYER");
                    String hand = msg.getParams().get("HAND");
                    String rank = msg.getParams().get("RANK");
                    
                    System.out.println("\n" + "=".repeat(40));
                    if (player.equals(playerId)) {
                        System.out.println("[SHOWDOWN] YOUR HAND");
                        System.out.println("   " + formatHandNice(hand));
                    } else {
                        System.out.println("[SHOWDOWN] OPPONENT");
                        System.out.println("   " + formatHandNice(hand));
                    }
                    System.out.println("   Rank: " + rank);
                    System.out.println("=".repeat(40));
                }
                case "WINNER" -> {
                    String player = msg.getParams().get("PLAYER");
                    String pot = msg.getParams().get("POT");
                    String rank = msg.getParams().get("RANK");
                    
                    System.out.println("\n" + "=".repeat(50));
                    if (player.equals(playerId)) {
                        System.out.println("*** YOU WIN! ***");
                    } else {
                        System.out.println("*** Opponent wins ***");
                    }
                    System.out.println("   Pot won: " + pot + " chips");
                    System.out.println("   Winning hand: " + rank);
                    System.out.println("=".repeat(50));
                }
                case "PAYOUT" -> {
                    String player = msg.getParams().get("PLAYER");
                    String stack = msg.getParams().get("STACK");
                    if (player.equals(playerId)) {
                        System.out.println("[STACK] Your chips: " + stack);
                    }
                }
                case "END" -> {
                    System.out.println("\n" + "=".repeat(40));
                    System.out.println("*** GAME ENDED ***");
                    System.out.println("=".repeat(40));
                }
            }
            
            // Redisplay prompt after async messages
            if (shouldShowPrompt) {
                System.out.print("> ");
                System.out.flush();
            }
        } catch (Exception e) {
            log.error("Error handling server message: {}", line, e);
        }
    }

    private void handleUserInput(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help", "h", "?" -> printHelp();
            case "hand", "cards" -> {
                if (currentHand != null) {
                    System.out.println("\n[YOUR HAND]");
                    System.out.println("   " + formatHandNice(currentHand));
                } else {
                    System.out.println("No hand dealt yet");
                }
            }
            case "create" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: create <ante> <bet>");
                    return;
                }
                int ante = Integer.parseInt(parts[1]);
                int bet = Integer.parseInt(parts[2]);
                send(ClientMessage.create(ante, bet));
            }
            case "join" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: join <gameId> <yourName>");
                    return;
                }
                String gId = parts[1];
                String name = parts[2];
                send(ClientMessage.join(gId, name));
            }
            case "start" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                send(ClientMessage.start(gameId, playerId));
            }
            case "check" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                send(ClientMessage.check(gameId, playerId));
            }
            case "call" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                send(ClientMessage.call(gameId, playerId));
            }
            case "bet" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                if (parts.length < 2) {
                    System.out.println("Usage: bet <amount>");
                    return;
                }
                int amount = Integer.parseInt(parts[1]);
                send(ClientMessage.bet(gameId, playerId, amount));
            }
            case "fold" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                send(ClientMessage.fold(gameId, playerId));
            }
            case "draw" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                if (parts.length < 2) {
                    System.out.println("Usage: draw <indices> (e.g., '0,2,4' or 'none')");
                    return;
                }
                String indices = parts[1].equals("none") ? "" : parts[1];
                
                // Track which indices we're drawing for later update
                if (!indices.isEmpty()) {
                    lastDrawIndices = Arrays.stream(indices.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                } else {
                    lastDrawIndices = Collections.emptyList();
                }
                
                send(ClientMessage.draw(gameId, playerId, indices));
            }
            case "status" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                send(ClientMessage.status(gameId, playerId));
            }
            case "leave" -> {
                if (gameId == null || playerId == null) {
                    System.out.println("Not in a game");
                    return;
                }
                send(ClientMessage.leave(gameId, playerId));
                gameId = null;
                playerId = null;
            }
            case "quit", "exit" -> {
                if (gameId != null && playerId != null) {
                    send(ClientMessage.quit(gameId, playerId));
                }
                running = false;
            }
            default -> System.out.println("Unknown command. Type 'help' for commands.");
        }
    }

    private void send(ClientMessage message) {
        String msg = message.toProtocolString();
        log.debug("Sending: {}", msg);
        writer.println(msg);
    }

    private String formatCards(String cardStr) {
        if (cardStr == null || cardStr.isEmpty() || cardStr.equals("*,*,*,*,*")) {
            return "Hidden";
        }
        return cardStr;
    }

    private String formatHandNice(String cardStr) {
        if (cardStr == null || cardStr.isEmpty() || cardStr.equals("*,*,*,*,*")) {
            return "[Hidden]";
        }
        String[] cards = cardStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.length; i++) {
            sb.append("[").append(i).append(":").append(cards[i]).append("] ");
        }
        return sb.toString().trim();
    }

    private void printHelp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("              POKER CLIENT - COMMANDS");
        System.out.println("=".repeat(60));
        System.out.println(" Game Setup:");
        System.out.println("   create <ante> <bet>  - Create new game");
        System.out.println("   join <gameId> <name> - Join existing game");
        System.out.println("   start                - Start the game");
        System.out.println();
        System.out.println(" During Game:");
        System.out.println("   hand                 - Show your current hand");
        System.out.println("   status               - View game status");
        System.out.println();
        System.out.println(" Betting Actions:");
        System.out.println("   check                - Check (no bet)");
        System.out.println("   call                 - Match current bet");
        System.out.println("   bet <amount>         - Raise the bet");
        System.out.println("   fold                 - Fold your hand");
        System.out.println();
        System.out.println(" Draw Phase:");
        System.out.println("   draw 0,2,4           - Replace cards at positions 0,2,4");
        System.out.println("   draw none            - Keep all cards");
        System.out.println();
        System.out.println(" Other:");
        System.out.println("   leave                - Leave current game");
        System.out.println("   quit                 - Exit client");
        System.out.println("   help                 - Show this help");
        System.out.println("=".repeat(60));
    }

    private void disconnect() {
        running = false;

        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.error("Error disconnecting", e);
        }

        System.out.println("Disconnected from server");
    }

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1]);
                System.exit(1);
            }
        }

        PokerClient client = new PokerClient(host, port);

        try {
            client.connect();
            client.run();
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.exit(1);
        }
    }
}

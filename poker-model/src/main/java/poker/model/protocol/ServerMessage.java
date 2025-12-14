package poker.model.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-to-Client messages.
 */
public class ServerMessage extends Message {

    private ServerMessage(String gameId, String playerId, String action, Map<String, String> params) {
        super(gameId, playerId, action, params);
    }

    public static ServerMessage ok() {
        return new ServerMessage(null, null, "OK", new HashMap<>());
    }

    public static ServerMessage ok(String message) {
        Map<String, String> params = new HashMap<>();
        params.put("MESSAGE", message);
        return new ServerMessage(null, null, "OK", params);
    }

    public static ServerMessage error(String code, String reason) {
        Map<String, String> params = new HashMap<>();
        params.put("CODE", code);
        params.put("REASON", reason);
        return new ServerMessage(null, null, "ERR", params);
    }

    public static ServerMessage welcome(String gameId, String playerId) {
        Map<String, String> params = new HashMap<>();
        params.put("GAME", gameId);
        params.put("PLAYER", playerId);
        return new ServerMessage(null, null, "WELCOME", params);
    }

    public static ServerMessage lobby(String gameId, String players) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYERS", players);
        return new ServerMessage(gameId, null, "LOBBY", params);
    }

    public static ServerMessage started(String gameId, String dealerId, int ante, int bet) {
        Map<String, String> params = new HashMap<>();
        params.put("DEALER", dealerId);
        params.put("ANTE", String.valueOf(ante));
        params.put("BET", String.valueOf(bet));
        return new ServerMessage(gameId, null, "STARTED", params);
    }

    public static ServerMessage anteRequest(String gameId, String playerId, int amount) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("AMOUNT", String.valueOf(amount));
        return new ServerMessage(gameId, null, "ANTE", params);
    }

    public static ServerMessage anteOk(String gameId, String playerId, int stack) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("STACK", String.valueOf(stack));
        return new ServerMessage(gameId, null, "ANTE_OK", params);
    }

    public static ServerMessage deal(String gameId, String playerId, String cards) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("CARDS", cards);
        return new ServerMessage(gameId, null, "DEAL", params);
    }

    public static ServerMessage turn(String gameId, String playerId, String phase, int callAmount, int minRaise) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("PHASE", phase);
        params.put("CALL", String.valueOf(callAmount));
        params.put("MINRAISE", String.valueOf(minRaise));
        return new ServerMessage(gameId, null, "TURN", params);
    }

    public static ServerMessage action(String gameId, String playerId, String type, String args) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("TYPE", type);
        if (args != null && !args.isEmpty()) {
            params.put("ARGS", args);
        }
        return new ServerMessage(gameId, null, "ACTION", params);
    }

    public static ServerMessage drawOk(String gameId, String playerId, int count, String newCards) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("COUNT", String.valueOf(count));
        params.put("NEW", newCards);
        return new ServerMessage(gameId, null, "DRAWOK", params);
    }

    public static ServerMessage round(String gameId, int pot, int highestBet) {
        Map<String, String> params = new HashMap<>();
        params.put("POT", String.valueOf(pot));
        params.put("HIGHESTBET", String.valueOf(highestBet));
        return new ServerMessage(gameId, null, "ROUND", params);
    }

    public static ServerMessage showdown(String gameId, String playerId, String hand, String rank) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("HAND", hand);
        params.put("RANK", rank);
        return new ServerMessage(gameId, null, "SHOWDOWN", params);
    }

    public static ServerMessage winner(String gameId, String playerId, int pot, String rank) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("POT", String.valueOf(pot));
        params.put("RANK", rank);
        return new ServerMessage(gameId, null, "WINNER", params);
    }

    public static ServerMessage payout(String gameId, String playerId, int amount, int stack) {
        Map<String, String> params = new HashMap<>();
        params.put("PLAYER", playerId);
        params.put("AMOUNT", String.valueOf(amount));
        params.put("STACK", String.valueOf(stack));
        return new ServerMessage(gameId, null, "PAYOUT", params);
    }

    public static ServerMessage end(String gameId, String reason) {
        Map<String, String> params = new HashMap<>();
        params.put("REASON", reason);
        return new ServerMessage(gameId, null, "END", params);
    }
}

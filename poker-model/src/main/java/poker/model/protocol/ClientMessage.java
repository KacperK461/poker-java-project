package poker.model.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-to-Server messages.
 */
public class ClientMessage extends Message {

    private ClientMessage(String gameId, String playerId, String action, Map<String, String> params) {
        super(gameId, playerId, action, params);
    }

    public static ClientMessage hello(String version) {
        Map<String, String> params = new HashMap<>();
        params.put("VERSION", version);
        return new ClientMessage(null, null, "HELLO", params);
    }

    public static ClientMessage create(int ante, int bet) {
        Map<String, String> params = new HashMap<>();
        params.put("ANTE", String.valueOf(ante));
        params.put("BET", String.valueOf(bet));
        params.put("LIMIT", "FIXED");
        return new ClientMessage(null, null, "CREATE", params);
    }

    public static ClientMessage join(String gameId, String name) {
        Map<String, String> params = new HashMap<>();
        params.put("GAME", gameId);
        params.put("NAME", name);
        return new ClientMessage(null, null, "JOIN", params);
    }

    public static ClientMessage leave(String gameId, String playerId) {
        return new ClientMessage(gameId, playerId, "LEAVE", new HashMap<>());
    }

    public static ClientMessage start(String gameId, String playerId) {
        return new ClientMessage(gameId, playerId, "START", new HashMap<>());
    }

    public static ClientMessage bet(String gameId, String playerId, int amount) {
        Map<String, String> params = new HashMap<>();
        params.put("AMOUNT", String.valueOf(amount));
        return new ClientMessage(gameId, playerId, "BET", params);
    }

    public static ClientMessage call(String gameId, String playerId) {
        return new ClientMessage(gameId, playerId, "CALL", new HashMap<>());
    }

    public static ClientMessage check(String gameId, String playerId) {
        return new ClientMessage(gameId, playerId, "CHECK", new HashMap<>());
    }

    public static ClientMessage fold(String gameId, String playerId) {
        return new ClientMessage(gameId, playerId, "FOLD", new HashMap<>());
    }

    public static ClientMessage draw(String gameId, String playerId, String cardIndices) {
        Map<String, String> params = new HashMap<>();
        params.put("CARDS", cardIndices);
        return new ClientMessage(gameId, playerId, "DRAW", params);
    }

    public static ClientMessage status(String gameId, String playerId) {
        return new ClientMessage(gameId, playerId, "STATUS", new HashMap<>());
    }

    public static ClientMessage quit(String gameId, String playerId) {
        return new ClientMessage(gameId, playerId, "QUIT", new HashMap<>());
    }
}

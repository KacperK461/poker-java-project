package poker.server;

import poker.model.game.GameConfig;
import poker.model.game.GameId;
import poker.model.game.PokerGame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active poker games on the server.
 */
public class GameManager {
    private final Map<GameId, PokerGame> games = new ConcurrentHashMap<>();

    public GameId createGame(GameConfig config) {
        GameId gameId = GameId.generate();
        PokerGame game = new PokerGame(gameId, config);
        games.put(gameId, game);
        return gameId;
    }

    public PokerGame getGame(GameId gameId) {
        PokerGame game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId.getId());
        }
        return game;
    }

    public void removeGame(GameId gameId) {
        games.remove(gameId);
    }

    public int getGameCount() {
        return games.size();
    }
}

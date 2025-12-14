package poker.model.game;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Unique identifier for a game.
 */
@Getter
@ToString
@EqualsAndHashCode
public class GameId {
    private final String id;

    private GameId(String id) {
        this.id = id;
    }

    public static GameId generate() {
        return new GameId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    }

    public static GameId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Game ID cannot be null or empty");
        }
        return new GameId(id);
    }
}

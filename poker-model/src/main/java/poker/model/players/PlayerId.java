package poker.model.players;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Unique identifier for a player.
 */
@Getter
@ToString
@EqualsAndHashCode
public class PlayerId {
    private final String id;

    private PlayerId(String id) {
        this.id = id;
    }

    public static PlayerId generate() {
        return new PlayerId(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    }

    public static PlayerId of(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }
        return new PlayerId(id);
    }
}

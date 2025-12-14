package poker.model.protocol;

import lombok.Getter;
import poker.model.exceptions.ProtocolException;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for protocol messages.
 */
@Getter
public abstract class Message {
    private final String gameId;
    private final String playerId;
    private final String action;
    private final Map<String, String> params;

    protected Message(String gameId, String playerId, String action) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.action = action;
        this.params = new HashMap<>();
    }

    protected Message(String gameId, String playerId, String action, Map<String, String> params) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.action = action;
        this.params = new HashMap<>(params);
    }

    public String getParam(String key) {
        return params.get(key);
    }

    public String getParam(String key, String defaultValue) {
        return params.getOrDefault(key, defaultValue);
    }

    public int getIntParam(String key) {
        String value = params.get(key);
        if (value == null) {
            throw new ProtocolException("MISSING_PARAM", "Missing required parameter: " + key);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ProtocolException("INVALID_PARAM", "Invalid integer parameter: " + key);
        }
    }

    public int getIntParam(String key, int defaultValue) {
        String value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Converts message to protocol string format.
     * Format: GAME_ID PLAYER_ID ACTION [PARAM1=VALUE1 PARAM2=VALUE2 ...]
     */
    public String toProtocolString() {
        StringBuilder sb = new StringBuilder();
        sb.append(gameId != null ? gameId : "-");
        sb.append(" ");
        sb.append(playerId != null ? playerId : "-");
        sb.append(" ");
        sb.append(action);
        
        if (!params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(" ");
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(entry.getValue());
            }
        }
        
        return sb.toString();
    }

    /**
     * Parse a protocol string into a Message object.
     */
    public static ParsedMessage parse(String line) {
        if (line == null || line.isBlank()) {
            throw new ProtocolException("INVALID_FORMAT", "Empty message");
        }

        if (line.length() > 512) {
            throw new ProtocolException("MESSAGE_TOO_LONG", "Message exceeds 512 bytes");
        }

        String trimmed = line.trim();
        
        // Split only first 3 parts (gameId, playerId, action), rest is parameters
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace < 0) {
            throw new ProtocolException("INVALID_FORMAT", 
                "Message must have at least: GAME_ID PLAYER_ID ACTION");
        }
        
        String gameIdStr = trimmed.substring(0, firstSpace);
        String remaining = trimmed.substring(firstSpace + 1).trim();
        
        int secondSpace = remaining.indexOf(' ');
        if (secondSpace < 0) {
            throw new ProtocolException("INVALID_FORMAT", 
                "Message must have at least: GAME_ID PLAYER_ID ACTION");
        }
        
        String playerIdStr = remaining.substring(0, secondSpace);
        remaining = remaining.substring(secondSpace + 1).trim();
        
        int thirdSpace = remaining.indexOf(' ');
        String action;
        String paramsStr;
        
        if (thirdSpace < 0) {
            action = remaining;
            paramsStr = "";
        } else {
            action = remaining.substring(0, thirdSpace);
            paramsStr = remaining.substring(thirdSpace + 1).trim();
        }
        
        String gameId = gameIdStr.equals("-") ? null : gameIdStr;
        String playerId = playerIdStr.equals("-") ? null : playerIdStr;

        Map<String, String> params = new HashMap<>();
        
        if (!paramsStr.isEmpty()) {
            // Parse parameters: KEY=VALUE separated by spaces, but values can contain spaces
            // We look for pattern KEY= to identify parameter boundaries
            int pos = 0;
            while (pos < paramsStr.length()) {
                // Find next KEY=
                int equalsPos = paramsStr.indexOf('=', pos);
                if (equalsPos < 0) {
                    break; // No more parameters
                }
                
                // Find key (go back to find start of key)
                int keyStart = pos;
                // Skip any leading spaces
                while (keyStart < equalsPos && paramsStr.charAt(keyStart) == ' ') {
                    keyStart++;
                }
                
                String key = paramsStr.substring(keyStart, equalsPos);
                
                // Find value (from after = until next KEY= or end)
                int valueStart = equalsPos + 1;
                int valueEnd = paramsStr.length();
                
                // Look for next parameter by finding next space followed by WORD=
                for (int i = valueStart + 1; i < paramsStr.length(); i++) {
                    if (paramsStr.charAt(i) == ' ') {
                        // Check if this space is followed by KEY=
                        int nextSpace = i;
                        while (nextSpace < paramsStr.length() && paramsStr.charAt(nextSpace) == ' ') {
                            nextSpace++;
                        }
                        
                        if (nextSpace < paramsStr.length()) {
                            // Look for = after this space
                            int nextEquals = paramsStr.indexOf('=', nextSpace);
                            if (nextEquals > 0) {
                                // Check if there's no space between nextSpace and nextEquals
                                boolean isNextParam = true;
                                for (int j = nextSpace; j < nextEquals; j++) {
                                    if (paramsStr.charAt(j) == ' ') {
                                        isNextParam = false;
                                        break;
                                    }
                                }
                                
                                if (isNextParam) {
                                    valueEnd = i;
                                    break;
                                }
                            }
                        }
                    }
                }
                
                String value = paramsStr.substring(valueStart, valueEnd).trim();
                params.put(key, value);
                
                pos = valueEnd;
            }
        }

        return new ParsedMessage(gameId, playerId, action, params);
    }

    /**
     * Simple parsed message container.
     */
    @Getter
    public static class ParsedMessage {
        private final String gameId;
        private final String playerId;
        private final String action;
        private final Map<String, String> params;

        public ParsedMessage(String gameId, String playerId, String action, Map<String, String> params) {
            this.gameId = gameId;
            this.playerId = playerId;
            this.action = action;
            this.params = params;
        }
    }
}

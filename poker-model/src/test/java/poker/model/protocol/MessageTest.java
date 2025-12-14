package poker.model.protocol;

import org.junit.jupiter.api.Test;
import poker.model.exceptions.ProtocolException;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testParseValidMessage() {
        String line = "GAME123 PLAYER456 JOIN NAME=Alice GAME=GAME123";
        Message.ParsedMessage msg = Message.parse(line);

        assertEquals("GAME123", msg.getGameId());
        assertEquals("PLAYER456", msg.getPlayerId());
        assertEquals("JOIN", msg.getAction());
        assertEquals("Alice", msg.getParams().get("NAME"));
        assertEquals("GAME123", msg.getParams().get("GAME"));
    }

    @Test
    void testParseMessageWithNullIds() {
        String line = "- - HELLO VERSION=1.0";
        Message.ParsedMessage msg = Message.parse(line);

        assertNull(msg.getGameId());
        assertNull(msg.getPlayerId());
        assertEquals("HELLO", msg.getAction());
        assertEquals("1.0", msg.getParams().get("VERSION"));
    }

    @Test
    void testParseMessageWithoutParams() {
        String line = "GAME123 PLAYER456 START";
        Message.ParsedMessage msg = Message.parse(line);

        assertEquals("GAME123", msg.getGameId());
        assertEquals("PLAYER456", msg.getPlayerId());
        assertEquals("START", msg.getAction());
        assertTrue(msg.getParams().isEmpty());
    }

    @Test
    void testParseEmptyMessage() {
        assertThrows(ProtocolException.class, () -> Message.parse(""));
    }

    @Test
    void testParseNullMessage() {
        assertThrows(ProtocolException.class, () -> Message.parse(null));
    }

    @Test
    void testParseTooShortMessage() {
        assertThrows(ProtocolException.class, () -> Message.parse("GAME123 PLAYER456"));
    }

    @Test
    void testParseMessageWithEmptyValue() {
        // Empty value should be accepted (edge case)
        String line = "GAME123 PLAYER456 ACTION KEY=";
        Message.ParsedMessage msg = Message.parse(line);

        assertEquals("GAME123", msg.getGameId());
        assertEquals("PLAYER456", msg.getPlayerId());
        assertEquals("ACTION", msg.getAction());
        assertEquals("", msg.getParams().get("KEY"));
    }

    @Test
    void testParseTooLongMessage() {
        String longMessage = "A ".repeat(300) + "B C";
        assertThrows(ProtocolException.class, () -> Message.parse(longMessage));
    }

    @Test
    void testClientMessageToProtocol() {
        ClientMessage msg = ClientMessage.join("GAME123", "Alice");
        String protocol = msg.toProtocolString();

        assertTrue(protocol.contains("JOIN"));
        assertTrue(protocol.contains("GAME=GAME123"));
        assertTrue(protocol.contains("NAME=Alice"));
    }

    @Test
    void testServerMessageToProtocol() {
        ServerMessage msg = ServerMessage.welcome("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();

        assertTrue(protocol.contains("WELCOME"));
        assertTrue(protocol.contains("GAME=GAME123"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
    }

    @Test
    void testErrorMessage() {
        ServerMessage msg = ServerMessage.error("NOT_FOUND", "Game not found");
        String protocol = msg.toProtocolString();

        assertTrue(protocol.contains("ERR"));
        assertTrue(protocol.contains("CODE=NOT_FOUND"));
        assertTrue(protocol.contains("REASON=Game not found"));
    }

    @Test
    void testParseMessageWithSpacesInValue() {
        String line = "- - OK MESSAGE=Welcome to Poker Server";
        Message.ParsedMessage msg = Message.parse(line);

        assertNull(msg.getGameId());
        assertNull(msg.getPlayerId());
        assertEquals("OK", msg.getAction());
        assertEquals("Welcome to Poker Server", msg.getParams().get("MESSAGE"));
    }

    @Test
    void testParseMessageWithMultipleWordsAndColon() {
        String line = "- - OK MESSAGE=Game created: abc123def456";
        Message.ParsedMessage msg = Message.parse(line);

        assertNull(msg.getGameId());
        assertNull(msg.getPlayerId());
        assertEquals("OK", msg.getAction());
        assertEquals("Game created: abc123def456", msg.getParams().get("MESSAGE"));
    }

    @Test
    void testParseMessageWithMultipleParameters() {
        String line = "GAME123 PLAYER456 DEAL CARDS=2♠ 3♥ 4♦ 5♣ 6♠ POT=100";
        Message.ParsedMessage msg = Message.parse(line);

        assertEquals("GAME123", msg.getGameId());
        assertEquals("PLAYER456", msg.getPlayerId());
        assertEquals("DEAL", msg.getAction());
        assertEquals("2♠ 3♥ 4♦ 5♣ 6♠", msg.getParams().get("CARDS"));
        assertEquals("100", msg.getParams().get("POT"));
    }

    @Test
    void testParseMessageWithExtraSpaces() {
        String line = "GAME123  PLAYER456   ACTION   KEY=value with  spaces";
        Message.ParsedMessage msg = Message.parse(line);

        assertEquals("GAME123", msg.getGameId());
        assertEquals("PLAYER456", msg.getPlayerId());
        assertEquals("ACTION", msg.getAction());
        assertEquals("value with  spaces", msg.getParams().get("KEY"));
    }
}

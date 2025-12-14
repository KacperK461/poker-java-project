package poker.model.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientMessageTest {

    @Test
    void testHelloMessage() {
        ClientMessage msg = ClientMessage.hello("1.0");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("HELLO"));
        assertTrue(protocol.contains("VERSION=1.0"));
        assertNull(msg.getGameId());
        assertNull(msg.getPlayerId());
    }

    @Test
    void testCreateMessage() {
        ClientMessage msg = ClientMessage.create(10, 20);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("CREATE"));
        assertTrue(protocol.contains("ANTE=10"));
        assertTrue(protocol.contains("BET=20"));
        assertTrue(protocol.contains("LIMIT=FIXED"));
    }

    @Test
    void testJoinMessage() {
        ClientMessage msg = ClientMessage.join("GAME123", "Alice");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("JOIN"));
        assertTrue(protocol.contains("GAME=GAME123"));
        assertTrue(protocol.contains("NAME=Alice"));
    }

    @Test
    void testLeaveMessage() {
        ClientMessage msg = ClientMessage.leave("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("LEAVE"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testStartMessage() {
        ClientMessage msg = ClientMessage.start("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("START"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testBetMessage() {
        ClientMessage msg = ClientMessage.bet("GAME123", "PLAYER456", 50);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("BET"));
        assertTrue(protocol.contains("AMOUNT=50"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testCallMessage() {
        ClientMessage msg = ClientMessage.call("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("CALL"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testCheckMessage() {
        ClientMessage msg = ClientMessage.check("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("CHECK"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testFoldMessage() {
        ClientMessage msg = ClientMessage.fold("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("FOLD"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testDrawMessage() {
        ClientMessage msg = ClientMessage.draw("GAME123", "PLAYER456", "0,2,4");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("DRAW"));
        assertTrue(protocol.contains("CARDS=0,2,4"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testDrawMessageEmptyIndices() {
        ClientMessage msg = ClientMessage.draw("GAME123", "PLAYER456", "");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("DRAW"));
        assertTrue(protocol.contains("CARDS="));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testStatusMessage() {
        ClientMessage msg = ClientMessage.status("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("STATUS"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testQuitMessage() {
        ClientMessage msg = ClientMessage.quit("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("QUIT"));
        assertTrue(protocol.contains("GAME123"));
        assertTrue(protocol.contains("PLAYER456"));
    }

    @Test
    void testCreateMessageWithDifferentValues() {
        ClientMessage msg = ClientMessage.create(100, 200);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("ANTE=100"));
        assertTrue(protocol.contains("BET=200"));
    }

    @Test
    void testBetMessageWithZeroAmount() {
        ClientMessage msg = ClientMessage.bet("GAME123", "PLAYER456", 0);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("AMOUNT=0"));
    }

    @Test
    void testBetMessageWithLargeAmount() {
        ClientMessage msg = ClientMessage.bet("GAME123", "PLAYER456", 999999);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("AMOUNT=999999"));
    }
}

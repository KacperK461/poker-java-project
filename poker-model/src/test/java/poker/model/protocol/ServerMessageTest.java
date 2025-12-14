package poker.model.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerMessageTest {

    @Test
    void testOkMessageWithoutText() {
        ServerMessage msg = ServerMessage.ok();
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("OK"));
        assertNull(msg.getGameId());
        assertNull(msg.getPlayerId());
    }

    @Test
    void testOkMessageWithText() {
        ServerMessage msg = ServerMessage.ok("Success");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("OK"));
        assertTrue(protocol.contains("MESSAGE=Success"));
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
    void testWelcomeMessage() {
        ServerMessage msg = ServerMessage.welcome("GAME123", "PLAYER456");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("WELCOME"));
        assertTrue(protocol.contains("GAME=GAME123"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
    }

    @Test
    void testLobbyMessage() {
        ServerMessage msg = ServerMessage.lobby("GAME123", "Alice,Bob,Charlie");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("LOBBY"));
        assertTrue(protocol.contains("PLAYERS=Alice,Bob,Charlie"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testStartedMessage() {
        ServerMessage msg = ServerMessage.started("GAME123", "PLAYER456", 10, 20);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("STARTED"));
        assertTrue(protocol.contains("DEALER=PLAYER456"));
        assertTrue(protocol.contains("ANTE=10"));
        assertTrue(protocol.contains("BET=20"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testAnteOkMessage() {
        ServerMessage msg = ServerMessage.anteOk("GAME123", "PLAYER456", 990);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("ANTE_OK"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("STACK=990"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testDealMessage() {
        ServerMessage msg = ServerMessage.deal("GAME123", "PLAYER456", "2♠,3♥,4♦,5♣,6♠");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("DEAL"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("CARDS=2♠,3♥,4♦,5♣,6♠"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testTurnMessage() {
        ServerMessage msg = ServerMessage.turn("GAME123", "PLAYER456", "BET", 20, 40);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("TURN"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("PHASE=BET"));
        assertTrue(protocol.contains("CALL=20"));
        assertTrue(protocol.contains("MINRAISE=40"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testActionMessage() {
        ServerMessage msg = ServerMessage.action("GAME123", "PLAYER456", "BET", "50");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("ACTION"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("TYPE=BET"));
        assertTrue(protocol.contains("ARGS=50"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testShowdownMessage() {
        ServerMessage msg = ServerMessage.showdown("GAME123", "PLAYER456", "2♠,3♥,4♦,5♣,6♠", "HIGH_CARD");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("SHOWDOWN"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("HAND=2♠,3♥,4♦,5♣,6♠"));
        assertTrue(protocol.contains("RANK=HIGH_CARD"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testWinnerMessage() {
        ServerMessage msg = ServerMessage.winner("GAME123", "PLAYER456", 150, "PAIR");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("WINNER"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("POT=150"));
        assertTrue(protocol.contains("RANK=PAIR"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testEndMessage() {
        ServerMessage msg = ServerMessage.end("GAME123", "All players folded");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("END"));
        assertTrue(protocol.contains("REASON=All players folded"));
        assertTrue(protocol.contains("GAME123"));
    }

    @Test
    void testLobbyMessageWithEmptyPlayers() {
        ServerMessage msg = ServerMessage.lobby("GAME123", "");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("LOBBY"));
        assertTrue(protocol.contains("PLAYERS="));
    }

    @Test
    void testActionMessageWithoutArgs() {
        ServerMessage msg = ServerMessage.action("GAME123", "PLAYER456", "CHECK", "");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("TYPE=CHECK"));
        assertFalse(protocol.contains("ARGS="));
    }

    @Test
    void testDrawOkMessage() {
        ServerMessage msg = ServerMessage.drawOk("GAME123", "PLAYER456", 2, "2♠,3♥");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("DRAWOK"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("COUNT=2"));
        assertTrue(protocol.contains("NEW=2♠,3♥"));
    }

    @Test
    void testRoundMessage() {
        ServerMessage msg = ServerMessage.round("GAME123", 150, 50);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("ROUND"));
        assertTrue(protocol.contains("POT=150"));
        assertTrue(protocol.contains("HIGHESTBET=50"));
    }

    @Test
    void testPayoutMessage() {
        ServerMessage msg = ServerMessage.payout("GAME123", "PLAYER456", 100, 1100);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("PAYOUT"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("AMOUNT=100"));
        assertTrue(protocol.contains("STACK=1100"));
    }

    @Test
    void testAnteRequestMessage() {
        ServerMessage msg = ServerMessage.anteRequest("GAME123", "PLAYER456", 10);
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("ANTE"));
        assertTrue(protocol.contains("PLAYER=PLAYER456"));
        assertTrue(protocol.contains("AMOUNT=10"));
    }

    @Test
    void testErrorMessageWithLongReason() {
        ServerMessage msg = ServerMessage.error("ERROR_CODE", "This is a very long error message that explains what went wrong");
        String protocol = msg.toProtocolString();
        
        assertTrue(protocol.contains("REASON=This is a very long error message that explains what went wrong"));
    }
}

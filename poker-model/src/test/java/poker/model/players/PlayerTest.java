package poker.model.players;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import poker.common.cards.Card;
import poker.common.cards.Rank;
import poker.common.cards.Suit;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private PlayerId playerId;
    private Player player;

    @BeforeEach
    void setUp() {
        playerId = PlayerId.of("TEST123");
        player = new Player(playerId, "TestPlayer", 1000);
    }

    @Test
    void testPlayerCreation() {
        assertEquals(playerId, player.getId());
        assertEquals("TestPlayer", player.getName());
        assertEquals(1000, player.getChips());
        assertEquals(PlayerState.ACTIVE, player.getState());
        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void testAddCard() {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        player.addCard(card);

        assertEquals(1, player.getHandSize());
        assertTrue(player.getHand().contains(card));
    }

    @Test
    void testAddCards() {
        List<Card> cards = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.KING),
            new Card(Suit.DIAMONDS, Rank.QUEEN)
        );

        player.addCards(cards);
        assertEquals(3, player.getHandSize());
    }

    @Test
    void testAddTooManyCards() {
        List<Card> cards = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.KING),
            new Card(Suit.DIAMONDS, Rank.QUEEN),
            new Card(Suit.CLUBS, Rank.JACK),
            new Card(Suit.SPADES, Rank.TEN),
            new Card(Suit.HEARTS, Rank.NINE)
        );

        assertThrows(IllegalStateException.class, () -> player.addCards(cards));
    }

    @Test
    void testRemoveCards() {
        List<Card> cards = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.KING),
            new Card(Suit.DIAMONDS, Rank.QUEEN),
            new Card(Suit.CLUBS, Rank.JACK),
            new Card(Suit.SPADES, Rank.TEN)
        );

        player.addCards(cards);
        player.removeCards(Arrays.asList(0, 2, 4));

        assertEquals(2, player.getHandSize());
    }

    @Test
    void testBet() {
        player.bet(100);

        assertEquals(900, player.getChips());
        assertEquals(100, player.getCurrentBet());
    }

    @Test
    void testBetAllIn() {
        player.bet(1000);

        assertEquals(0, player.getChips());
        assertEquals(1000, player.getCurrentBet());
        assertEquals(PlayerState.ALL_IN, player.getState());
    }

    @Test
    void testBetTooMuch() {
        assertThrows(IllegalArgumentException.class, () -> player.bet(1001));
    }

    @Test
    void testFold() {
        player.fold();
        assertEquals(PlayerState.FOLDED, player.getState());
        assertFalse(player.isActive());
    }

    @Test
    void testClearHand() {
        player.addCards(Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.KING)
        ));
        player.bet(50);

        player.clearHand();

        assertTrue(player.getHand().isEmpty());
        assertEquals(0, player.getCurrentBet());
    }

    @Test
    void testResetForNewRound() {
        player.bet(100);
        player.fold();

        player.resetForNewRound();

        assertEquals(0, player.getCurrentBet());
        assertEquals(PlayerState.ACTIVE, player.getState());
    }

    @Test
    void testAddChips() {
        player.addChips(500);
        assertEquals(1500, player.getChips());
    }

    @Test
    void testInvalidPlayerCreation() {
        assertThrows(IllegalArgumentException.class, 
            () -> new Player(null, "Test", 1000));
        assertThrows(IllegalArgumentException.class, 
            () -> new Player(playerId, "", 1000));
        assertThrows(IllegalArgumentException.class, 
            () -> new Player(playerId, "Test", -1));
    }
}

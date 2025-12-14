package poker.common.cards;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void testCardCreation() {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        assertEquals(Suit.SPADES, card.suit());
        assertEquals(Rank.ACE, card.rank());
    }

    @Test
    void testCardCreationWithNullSuit() {
        assertThrows(IllegalArgumentException.class, () -> new Card(null, Rank.ACE));
    }

    @Test
    void testCardCreationWithNullRank() {
        assertThrows(IllegalArgumentException.class, () -> new Card(Suit.SPADES, null));
    }

    @Test
    void testCompareTo() {
        Card aceSpades = new Card(Suit.SPADES, Rank.ACE);
        Card kingSpades = new Card(Suit.SPADES, Rank.KING);
        Card aceHearts = new Card(Suit.HEARTS, Rank.ACE);
        
        assertTrue(aceSpades.compareTo(kingSpades) > 0);
        assertTrue(kingSpades.compareTo(aceSpades) < 0);
        assertTrue(aceSpades.compareTo(aceHearts) > 0); // Spades > Hearts
        assertEquals(0, aceSpades.compareTo(new Card(Suit.SPADES, Rank.ACE)));
    }

    @Test
    void testEquals() {
        Card card1 = new Card(Suit.SPADES, Rank.ACE);
        Card card2 = new Card(Suit.SPADES, Rank.ACE);
        Card card3 = new Card(Suit.HEARTS, Rank.ACE);
        
        assertEquals(card1, card2);
        assertNotEquals(card1, card3);
    }

    @Test
    void testHashCode() {
        Card card1 = new Card(Suit.SPADES, Rank.ACE);
        Card card2 = new Card(Suit.SPADES, Rank.ACE);
        Card card3 = new Card(Suit.HEARTS, Rank.ACE);
        
        assertEquals(card1.hashCode(), card2.hashCode());
        assertNotEquals(card1.hashCode(), card3.hashCode());
    }

    @Test
    void testHashSet() {
        Set<Card> cardSet = new HashSet<>();
        Card card1 = new Card(Suit.SPADES, Rank.ACE);
        Card card2 = new Card(Suit.SPADES, Rank.ACE);
        Card card3 = new Card(Suit.HEARTS, Rank.ACE);
        
        cardSet.add(card1);
        cardSet.add(card2);
        cardSet.add(card3);
        
        assertEquals(2, cardSet.size());
        assertTrue(cardSet.contains(card1));
        assertTrue(cardSet.contains(card3));
    }

    @Test
    void testToString() {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        assertEquals("AS", card.toString());
        
        Card card2 = new Card(Suit.HEARTS, Rank.TEN);
        assertEquals("TH", card2.toString());
    }

    @Test
    void testToDisplayString() {
        Card card = new Card(Suit.SPADES, Rank.ACE);
        assertEquals("A♠", card.toDisplayString());
    }

    @Test
    void testFromString() {
        Card card = Card.fromString("AS");
        assertEquals(Suit.SPADES, card.suit());
        assertEquals(Rank.ACE, card.rank());
        
        Card card2 = Card.fromString("TH");
        assertEquals(Suit.HEARTS, card2.suit());
        assertEquals(Rank.TEN, card2.rank());
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Card.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> Card.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> Card.fromString("A"));
        assertThrows(IllegalArgumentException.class, () -> Card.fromString("AX"));
    }
}

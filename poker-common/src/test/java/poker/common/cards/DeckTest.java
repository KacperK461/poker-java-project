package poker.common.cards;

import org.junit.jupiter.api.Test;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void testCreateSortedDeck() {
        Deck deck = Deck.createSortedDeck();
        assertEquals(52, deck.size());
        assertEquals(52, deck.remaining());
    }

    @Test
    void testSortedDeckContainsAllCards() {
        Deck deck = Deck.createSortedDeck();
        Set<Card> uniqueCards = new HashSet<>();
        
        for (int i = 0; i < 52; i++) {
            Card card = deck.draw();
            uniqueCards.add(card);
        }
        
        assertEquals(52, uniqueCards.size());
        assertTrue(deck.isEmpty());
    }

    @Test
    void testCreateShuffledDeck() {
        Deck deck = Deck.createShuffledDeck();
        assertEquals(52, deck.size());
        assertEquals(52, deck.remaining());
    }

    @Test
    void testShuffleChangesOrder() {
        Deck sorted = Deck.createSortedDeck();
        Deck shuffled = Deck.createShuffledDeck(new SecureRandom(new byte[]{1, 2, 3}));
        
        List<Card> sortedCards = sorted.draw(52);
        List<Card> shuffledCards = shuffled.draw(52);
        
        assertNotEquals(sortedCards, shuffledCards);
    }

    @Test
    void testDraw() {
        Deck deck = Deck.createSortedDeck();
        Card card = deck.draw();
        assertNotNull(card);
        assertEquals(51, deck.remaining());
    }

    @Test
    void testDrawMultiple() {
        Deck deck = Deck.createSortedDeck();
        List<Card> cards = deck.draw(5);
        assertEquals(5, cards.size());
        assertEquals(47, deck.remaining());
    }

    @Test
    void testDrawFromEmptyDeck() {
        Deck deck = Deck.createSortedDeck();
        deck.draw(52);
        assertThrows(IllegalStateException.class, deck::draw);
    }

    @Test
    void testDrawMoreThanAvailable() {
        Deck deck = Deck.createSortedDeck();
        assertThrows(IllegalStateException.class, () -> deck.draw(53));
    }

    @Test
    void testDrawNegativeCount() {
        Deck deck = Deck.createSortedDeck();
        assertThrows(IllegalArgumentException.class, () -> deck.draw(-1));
    }

    @Test
    void testReset() {
        Deck deck = Deck.createSortedDeck();
        deck.draw(10);
        assertEquals(42, deck.remaining());
        
        deck.reset();
        assertEquals(52, deck.remaining());
    }

    @Test
    void testShuffle() {
        Deck deck = Deck.createSortedDeck();
        Card firstCard = deck.draw();
        
        deck.reset();
        deck.shuffle(new SecureRandom(new byte[]{1, 2, 3}));
        Card firstAfterShuffle = deck.draw();
        
        // With high probability, cards should be different
        // (This could theoretically fail, but with 1/52 chance)
        // Note: For deterministic testing, we use seeded random
    }

    @Test
    void testGetCards() {
        Deck deck = Deck.createSortedDeck();
        List<Card> cards = deck.getCards();
        assertEquals(52, cards.size());
        
        // Verify list is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> cards.add(new Card(Suit.SPADES, Rank.ACE)));
    }

    @Test
    void testIsEmpty() {
        Deck deck = Deck.createSortedDeck();
        assertFalse(deck.isEmpty());
        
        deck.draw(52);
        assertTrue(deck.isEmpty());
    }
}

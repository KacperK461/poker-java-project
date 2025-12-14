package poker.common.cards;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a deck of 52 playing cards.
 * Provides factory methods for creating sorted and shuffled decks.
 */
public class Deck {
    private final List<Card> cards;
    private int currentIndex;

    private Deck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
        this.currentIndex = 0;
    }

    /**
     * Factory method: creates a sorted deck (all 52 cards in order).
     * @return A new sorted Deck
     */
    public static Deck createSortedDeck() {
        List<Card> cards = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        return new Deck(cards);
    }

    /**
     * Factory method: creates a shuffled deck using SecureRandom.
     * @return A new shuffled Deck
     */
    public static Deck createShuffledDeck() {
        return createShuffledDeck(new SecureRandom());
    }

    /**
     * Factory method: creates a shuffled deck using the provided random source.
     * @param random The random number generator to use
     * @return A new shuffled Deck
     */
    public static Deck createShuffledDeck(SecureRandom random) {
        Deck deck = createSortedDeck();
        return deck.shuffle(random);
    }

    /**
     * Shuffles this deck using SecureRandom and returns it.
     * @return This deck (for method chaining)
     */
    public Deck shuffle() {
        return shuffle(new SecureRandom());
    }

    /**
     * Shuffles this deck using the provided random source and returns it.
     * @param random The random number generator to use
     * @return This deck (for method chaining)
     */
    public Deck shuffle(SecureRandom random) {
        Collections.shuffle(cards, random);
        currentIndex = 0;
        return this;
    }

    /**
     * Draws the next card from the deck.
     * @return The next Card
     * @throws IllegalStateException if deck is empty
     */
    public Card draw() {
        if (isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return cards.get(currentIndex++);
    }

    /**
     * Draws multiple cards from the deck.
     * @param count Number of cards to draw
     * @return List of drawn cards
     * @throws IllegalStateException if not enough cards remain
     */
    public List<Card> draw(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative");
        }
        if (count > remaining()) {
            throw new IllegalStateException("Not enough cards remaining: requested " + 
                count + ", available " + remaining());
        }
        
        List<Card> drawn = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            drawn.add(draw());
        }
        return drawn;
    }

    /**
     * Returns the number of cards remaining in the deck.
     * @return Number of undrawn cards
     */
    public int remaining() {
        return cards.size() - currentIndex;
    }

    /**
     * Checks if the deck is empty.
     * @return true if no cards remain
     */
    public boolean isEmpty() {
        return remaining() == 0;
    }

    /**
     * Returns the total number of cards in the deck.
     * @return Total card count (52 for a standard deck)
     */
    public int size() {
        return cards.size();
    }

    /**
     * Resets the deck to its initial state (no cards drawn).
     */
    public void reset() {
        currentIndex = 0;
    }

    /**
     * Returns an immutable view of all cards in the deck.
     * @return Unmodifiable list of cards
     */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
}

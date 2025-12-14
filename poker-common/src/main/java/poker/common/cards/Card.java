package poker.common.cards;

/**
 * Immutable record representing a playing card.
 * Implements Comparable to allow sorting by rank first, then suit.
 */
public record Card(Suit suit, Rank rank) implements Comparable<Card> {

    public Card {
        if (suit == null) {
            throw new IllegalArgumentException("Suit cannot be null");
        }
        if (rank == null) {
            throw new IllegalArgumentException("Rank cannot be null");
        }
    }

    /**
     * Compare cards by rank first, then by suit.
     */
    @Override
    public int compareTo(Card other) {
        int rankComparison = this.rank.compareTo(other.rank);
        if (rankComparison != 0) {
            return rankComparison;
        }
        return this.suit.compareTo(other.suit);
    }

    /**
     * Returns a string representation in format "RS" (e.g., "AS" for Ace of Spades).
     */
    @Override
    public String toString() {
        return rank.getSymbol() + suit.name().charAt(0);
    }

    /**
     * Returns a user-friendly string representation (e.g., "A♠").
     */
    public String toDisplayString() {
        return rank.getSymbol() + suit.getSymbol();
    }

    /**
     * Parse card from string format "RS" (e.g., "AS", "2H").
     * @param cardString The string representation
     * @return The Card object
     * @throws IllegalArgumentException if format is invalid
     */
    public static Card fromString(String cardString) {
        if (cardString == null || cardString.length() < 2) {
            throw new IllegalArgumentException("Invalid card string: " + cardString);
        }
        
        String rankSymbol = cardString.substring(0, cardString.length() - 1);
        String suitChar = cardString.substring(cardString.length() - 1).toUpperCase();
        
        Rank rank = Rank.fromSymbol(rankSymbol);
        Suit suit = switch (suitChar) {
            case "C" -> Suit.CLUBS;
            case "D" -> Suit.DIAMONDS;
            case "H" -> Suit.HEARTS;
            case "S" -> Suit.SPADES;
            default -> throw new IllegalArgumentException("Invalid suit: " + suitChar);
        };
        
        return new Card(suit, rank);
    }
}

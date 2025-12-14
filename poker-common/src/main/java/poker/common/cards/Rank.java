package poker.common.cards;

/**
 * Enum representing card ranks in a standard deck.
 * Ordered from lowest to highest for comparison purposes.
 */
public enum Rank {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("T", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14);

    private final String symbol;
    private final int value;

    Rank(String symbol, int value) {
        this.symbol = symbol;
        this.value = value;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return symbol;
    }

    /**
     * Parse rank from symbol string.
     * @param symbol The symbol (e.g., "A", "K", "2")
     * @return The corresponding Rank
     * @throws IllegalArgumentException if symbol is invalid
     */
    public static Rank fromSymbol(String symbol) {
        for (Rank rank : values()) {
            if (rank.symbol.equals(symbol)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Invalid rank symbol: " + symbol);
    }
}

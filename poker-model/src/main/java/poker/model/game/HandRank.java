package poker.model.game;

import lombok.Getter;
import poker.common.cards.Rank;

/**
 * Represents the ranking of a poker hand.
 * Implements Comparable to allow comparing hands.
 */
@Getter
public class HandRank implements Comparable<HandRank> {
    
    public enum PokerRank {
        HIGH_CARD(1, "High Card"),
        PAIR(2, "Pair"),
        TWO_PAIR(3, "Two Pair"),
        THREE_OF_A_KIND(4, "Three of a Kind"),
        STRAIGHT(5, "Straight"),
        FLUSH(6, "Flush"),
        FULL_HOUSE(7, "Full House"),
        FOUR_OF_A_KIND(8, "Four of a Kind"),
        STRAIGHT_FLUSH(9, "Straight Flush"),
        ROYAL_FLUSH(10, "Royal Flush");

        private final int value;
        private final String displayName;

        PokerRank(int value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public int getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final PokerRank rank;
    private final Rank[] kickers; // Sorted in descending order

    public HandRank(PokerRank rank, Rank... kickers) {
        this.rank = rank;
        this.kickers = kickers;
    }

    @Override
    public int compareTo(HandRank other) {
        // First compare by poker rank
        int rankComparison = Integer.compare(this.rank.getValue(), other.rank.getValue());
        if (rankComparison != 0) {
            return rankComparison;
        }

        // If ranks are equal, compare kickers
        int minLength = Math.min(this.kickers.length, other.kickers.length);
        for (int i = 0; i < minLength; i++) {
            int kickerComparison = this.kickers[i].compareTo(other.kickers[i]);
            if (kickerComparison != 0) {
                return kickerComparison;
            }
        }

        // If all kickers are equal, compare by number of kickers
        return Integer.compare(this.kickers.length, other.kickers.length);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(rank.getDisplayName());
        if (kickers.length > 0) {
            sb.append(" (");
            for (int i = 0; i < kickers.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(kickers[i].getSymbol());
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public String toProtocolString() {
        StringBuilder sb = new StringBuilder(rank.name());
        if (kickers.length > 0) {
            sb.append("_");
            for (Rank kicker : kickers) {
                sb.append(kicker.getSymbol());
            }
        }
        return sb.toString();
    }
}

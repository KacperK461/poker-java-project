package poker.model.game;

import poker.common.cards.Card;
import poker.common.cards.Rank;
import poker.common.cards.Suit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy interface for evaluating poker hands.
 * Implements the Strategy pattern to allow different evaluation algorithms.
 */
public interface HandEvaluator {
    HandRank evaluate(List<Card> cards);

    /**
     * Standard 5-card draw poker hand evaluator.
     */
    class StandardPokerEvaluator implements HandEvaluator {

        @Override
        public HandRank evaluate(List<Card> cards) {
            if (cards == null || cards.size() != 5) {
                throw new IllegalArgumentException("Hand must contain exactly 5 cards");
            }

            List<Card> sorted = new ArrayList<>(cards);
            sorted.sort(Comparator.comparing(Card::rank).reversed());

            boolean isFlush = checkFlush(sorted);
            boolean isStraight = checkStraight(sorted);
            Map<Rank, Integer> rankCounts = countRanks(sorted);

            // Check for Royal Flush
            if (isFlush && isStraight && sorted.get(0).rank() == Rank.ACE) {
                return new HandRank(HandRank.PokerRank.ROYAL_FLUSH, Rank.ACE);
            }

            // Check for Straight Flush
            if (isFlush && isStraight) {
                return new HandRank(HandRank.PokerRank.STRAIGHT_FLUSH, sorted.get(0).rank());
            }

            // Check for Four of a Kind
            Rank fourOfAKind = findNOfAKind(rankCounts, 4);
            if (fourOfAKind != null) {
                Rank kicker = findKicker(sorted, fourOfAKind);
                return new HandRank(HandRank.PokerRank.FOUR_OF_A_KIND, fourOfAKind, kicker);
            }

            // Check for Full House
            Rank threeOfAKind = findNOfAKind(rankCounts, 3);
            Rank pair = findNOfAKind(rankCounts, 2);
            if (threeOfAKind != null && pair != null) {
                return new HandRank(HandRank.PokerRank.FULL_HOUSE, threeOfAKind, pair);
            }

            // Check for Flush
            if (isFlush) {
                Rank[] kickers = sorted.stream().map(Card::rank).toArray(Rank[]::new);
                return new HandRank(HandRank.PokerRank.FLUSH, kickers);
            }

            // Check for Straight
            if (isStraight) {
                return new HandRank(HandRank.PokerRank.STRAIGHT, sorted.get(0).rank());
            }

            // Check for Three of a Kind
            if (threeOfAKind != null) {
                List<Rank> kickers = findKickers(sorted, threeOfAKind, 2);
                return new HandRank(HandRank.PokerRank.THREE_OF_A_KIND, 
                    threeOfAKind, kickers.get(0), kickers.get(1));
            }

            // Check for Two Pair
            List<Rank> pairs = findAllPairs(rankCounts);
            if (pairs.size() >= 2) {
                Rank highPair = pairs.get(0);
                Rank lowPair = pairs.get(1);
                Rank kicker = findKickers(sorted, Arrays.asList(highPair, lowPair), 1).get(0);
                return new HandRank(HandRank.PokerRank.TWO_PAIR, highPair, lowPair, kicker);
            }

            // Check for Pair
            if (pair != null) {
                List<Rank> kickers = findKickers(sorted, pair, 3);
                return new HandRank(HandRank.PokerRank.PAIR, 
                    pair, kickers.get(0), kickers.get(1), kickers.get(2));
            }

            // High Card
            Rank[] kickers = sorted.stream().map(Card::rank).toArray(Rank[]::new);
            return new HandRank(HandRank.PokerRank.HIGH_CARD, kickers);
        }

        private boolean checkFlush(List<Card> cards) {
            Suit firstSuit = cards.get(0).suit();
            return cards.stream().allMatch(card -> card.suit() == firstSuit);
        }

        private boolean checkStraight(List<Card> cards) {
            // Check for A-2-3-4-5 (wheel/bicycle)
            if (cards.get(0).rank() == Rank.ACE &&
                cards.get(1).rank() == Rank.FIVE &&
                cards.get(2).rank() == Rank.FOUR &&
                cards.get(3).rank() == Rank.THREE &&
                cards.get(4).rank() == Rank.TWO) {
                return true;
            }

            // Check for regular straight
            for (int i = 0; i < cards.size() - 1; i++) {
                if (cards.get(i).rank().getValue() - cards.get(i + 1).rank().getValue() != 1) {
                    return false;
                }
            }
            return true;
        }

        private Map<Rank, Integer> countRanks(List<Card> cards) {
            Map<Rank, Integer> counts = new HashMap<>();
            for (Card card : cards) {
                counts.merge(card.rank(), 1, Integer::sum);
            }
            return counts;
        }

        private Rank findNOfAKind(Map<Rank, Integer> rankCounts, int n) {
            return rankCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == n)
                .map(Map.Entry::getKey)
                .max(Rank::compareTo)
                .orElse(null);
        }

        private List<Rank> findAllPairs(Map<Rank, Integer> rankCounts) {
            return rankCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == 2)
                .map(Map.Entry::getKey)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        }

        private Rank findKicker(List<Card> cards, Rank exclude) {
            return cards.stream()
                .map(Card::rank)
                .filter(rank -> rank != exclude)
                .findFirst()
                .orElseThrow();
        }

        private List<Rank> findKickers(List<Card> cards, Rank exclude, int count) {
            return findKickers(cards, Collections.singletonList(exclude), count);
        }

        private List<Rank> findKickers(List<Card> cards, List<Rank> exclude, int count) {
            return cards.stream()
                .map(Card::rank)
                .filter(rank -> !exclude.contains(rank))
                .limit(count)
                .collect(Collectors.toList());
        }
    }
}

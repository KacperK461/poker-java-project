package poker.model.game;

import org.junit.jupiter.api.Test;
import poker.common.cards.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandEvaluatorTest {

    private final HandEvaluator evaluator = new HandEvaluator.StandardPokerEvaluator();

    @Test
    void testRoyalFlush() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.SPADES, Rank.KING),
            new Card(Suit.SPADES, Rank.QUEEN),
            new Card(Suit.SPADES, Rank.JACK),
            new Card(Suit.SPADES, Rank.TEN)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.ROYAL_FLUSH, rank.getRank());
    }

    @Test
    void testStraightFlush() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.HEARTS, Rank.NINE),
            new Card(Suit.HEARTS, Rank.EIGHT),
            new Card(Suit.HEARTS, Rank.SEVEN),
            new Card(Suit.HEARTS, Rank.SIX),
            new Card(Suit.HEARTS, Rank.FIVE)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.STRAIGHT_FLUSH, rank.getRank());
    }

    @Test
    void testFourOfAKind() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.ACE),
            new Card(Suit.DIAMONDS, Rank.ACE),
            new Card(Suit.CLUBS, Rank.ACE),
            new Card(Suit.SPADES, Rank.KING)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.FOUR_OF_A_KIND, rank.getRank());
        assertEquals(Rank.ACE, rank.getKickers()[0]);
    }

    @Test
    void testFullHouse() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.KING),
            new Card(Suit.HEARTS, Rank.KING),
            new Card(Suit.DIAMONDS, Rank.KING),
            new Card(Suit.CLUBS, Rank.QUEEN),
            new Card(Suit.SPADES, Rank.QUEEN)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.FULL_HOUSE, rank.getRank());
    }

    @Test
    void testFlush() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.DIAMONDS, Rank.ACE),
            new Card(Suit.DIAMONDS, Rank.JACK),
            new Card(Suit.DIAMONDS, Rank.NINE),
            new Card(Suit.DIAMONDS, Rank.SIX),
            new Card(Suit.DIAMONDS, Rank.THREE)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.FLUSH, rank.getRank());
    }

    @Test
    void testStraight() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.TEN),
            new Card(Suit.HEARTS, Rank.NINE),
            new Card(Suit.DIAMONDS, Rank.EIGHT),
            new Card(Suit.CLUBS, Rank.SEVEN),
            new Card(Suit.SPADES, Rank.SIX)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.STRAIGHT, rank.getRank());
    }

    @Test
    void testThreeOfAKind() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.SEVEN),
            new Card(Suit.HEARTS, Rank.SEVEN),
            new Card(Suit.DIAMONDS, Rank.SEVEN),
            new Card(Suit.CLUBS, Rank.KING),
            new Card(Suit.SPADES, Rank.THREE)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.THREE_OF_A_KIND, rank.getRank());
    }

    @Test
    void testTwoPair() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.JACK),
            new Card(Suit.HEARTS, Rank.JACK),
            new Card(Suit.DIAMONDS, Rank.THREE),
            new Card(Suit.CLUBS, Rank.THREE),
            new Card(Suit.SPADES, Rank.TWO)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.TWO_PAIR, rank.getRank());
    }

    @Test
    void testPair() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.TEN),
            new Card(Suit.HEARTS, Rank.TEN),
            new Card(Suit.DIAMONDS, Rank.KING),
            new Card(Suit.CLUBS, Rank.FIVE),
            new Card(Suit.SPADES, Rank.THREE)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.PAIR, rank.getRank());
    }

    @Test
    void testHighCard() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.JACK),
            new Card(Suit.DIAMONDS, Rank.NINE),
            new Card(Suit.CLUBS, Rank.SIX),
            new Card(Suit.SPADES, Rank.TWO)
        );

        HandRank rank = evaluator.evaluate(hand);
        assertEquals(HandRank.PokerRank.HIGH_CARD, rank.getRank());
    }

    @Test
    void testCompareHands() {
        List<Card> royalFlush = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.SPADES, Rank.KING),
            new Card(Suit.SPADES, Rank.QUEEN),
            new Card(Suit.SPADES, Rank.JACK),
            new Card(Suit.SPADES, Rank.TEN)
        );

        List<Card> fourOfAKind = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.ACE),
            new Card(Suit.DIAMONDS, Rank.ACE),
            new Card(Suit.CLUBS, Rank.ACE),
            new Card(Suit.SPADES, Rank.KING)
        );

        HandRank rank1 = evaluator.evaluate(royalFlush);
        HandRank rank2 = evaluator.evaluate(fourOfAKind);

        assertTrue(rank1.compareTo(rank2) > 0);
    }

    @Test
    void testInvalidHandSize() {
        List<Card> hand = Arrays.asList(
            new Card(Suit.SPADES, Rank.ACE),
            new Card(Suit.HEARTS, Rank.KING)
        );

        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(hand));
    }
}

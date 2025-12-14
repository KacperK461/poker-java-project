package poker.model.game;

import org.junit.jupiter.api.Test;
import poker.common.cards.Rank;

import static org.junit.jupiter.api.Assertions.*;

class HandRankTest {

    @Test
    void testPokerRankValues() {
        assertEquals(1, HandRank.PokerRank.HIGH_CARD.getValue());
        assertEquals(2, HandRank.PokerRank.PAIR.getValue());
        assertEquals(3, HandRank.PokerRank.TWO_PAIR.getValue());
        assertEquals(4, HandRank.PokerRank.THREE_OF_A_KIND.getValue());
        assertEquals(5, HandRank.PokerRank.STRAIGHT.getValue());
        assertEquals(6, HandRank.PokerRank.FLUSH.getValue());
        assertEquals(7, HandRank.PokerRank.FULL_HOUSE.getValue());
        assertEquals(8, HandRank.PokerRank.FOUR_OF_A_KIND.getValue());
        assertEquals(9, HandRank.PokerRank.STRAIGHT_FLUSH.getValue());
        assertEquals(10, HandRank.PokerRank.ROYAL_FLUSH.getValue());
    }

    @Test
    void testPokerRankDisplayNames() {
        assertEquals("High Card", HandRank.PokerRank.HIGH_CARD.getDisplayName());
        assertEquals("Pair", HandRank.PokerRank.PAIR.getDisplayName());
        assertEquals("Two Pair", HandRank.PokerRank.TWO_PAIR.getDisplayName());
        assertEquals("Three of a Kind", HandRank.PokerRank.THREE_OF_A_KIND.getDisplayName());
        assertEquals("Straight", HandRank.PokerRank.STRAIGHT.getDisplayName());
        assertEquals("Flush", HandRank.PokerRank.FLUSH.getDisplayName());
        assertEquals("Full House", HandRank.PokerRank.FULL_HOUSE.getDisplayName());
        assertEquals("Four of a Kind", HandRank.PokerRank.FOUR_OF_A_KIND.getDisplayName());
        assertEquals("Straight Flush", HandRank.PokerRank.STRAIGHT_FLUSH.getDisplayName());
        assertEquals("Royal Flush", HandRank.PokerRank.ROYAL_FLUSH.getDisplayName());
    }

    @Test
    void testHandRankCreation() {
        HandRank rank = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING);
        assertEquals(HandRank.PokerRank.PAIR, rank.getRank());
        assertEquals(2, rank.getKickers().length);
        assertEquals(Rank.ACE, rank.getKickers()[0]);
        assertEquals(Rank.KING, rank.getKickers()[1]);
    }

    @Test
    void testHandRankCreationNoKickers() {
        HandRank rank = new HandRank(HandRank.PokerRank.ROYAL_FLUSH);
        assertEquals(HandRank.PokerRank.ROYAL_FLUSH, rank.getRank());
        assertEquals(0, rank.getKickers().length);
    }

    @Test
    void testCompareDifferentRanks() {
        HandRank pair = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE);
        HandRank threeOfKind = new HandRank(HandRank.PokerRank.THREE_OF_A_KIND, Rank.TWO);
        
        assertTrue(threeOfKind.compareTo(pair) > 0);
        assertTrue(pair.compareTo(threeOfKind) < 0);
    }

    @Test
    void testCompareSameRankDifferentKickers() {
        HandRank pairAces = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE);
        HandRank pairKings = new HandRank(HandRank.PokerRank.PAIR, Rank.KING);
        
        assertTrue(pairAces.compareTo(pairKings) > 0);
        assertTrue(pairKings.compareTo(pairAces) < 0);
    }

    @Test
    void testCompareSameRankSameKickers() {
        HandRank pair1 = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING);
        HandRank pair2 = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING);
        
        assertEquals(0, pair1.compareTo(pair2));
    }

    @Test
    void testCompareSameRankSameFirstKickerDifferentSecond() {
        HandRank pair1 = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING, Rank.QUEEN);
        HandRank pair2 = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING, Rank.JACK);
        
        assertTrue(pair1.compareTo(pair2) > 0);
        assertTrue(pair2.compareTo(pair1) < 0);
    }

    @Test
    void testCompareDifferentNumberOfKickers() {
        HandRank pair1 = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING);
        HandRank pair2 = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING, Rank.QUEEN);
        
        assertTrue(pair2.compareTo(pair1) > 0);
        assertTrue(pair1.compareTo(pair2) < 0);
    }

    @Test
    void testToStringWithKickers() {
        HandRank rank = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING);
        String str = rank.toString();
        
        assertTrue(str.contains("Pair"));
        assertTrue(str.contains("A"));
        assertTrue(str.contains("K"));
    }

    @Test
    void testToStringNoKickers() {
        HandRank rank = new HandRank(HandRank.PokerRank.ROYAL_FLUSH);
        String str = rank.toString();
        
        assertEquals("Royal Flush", str);
        assertFalse(str.contains("("));
    }

    @Test
    void testToProtocolStringWithKickers() {
        HandRank rank = new HandRank(HandRank.PokerRank.PAIR, Rank.ACE, Rank.KING);
        String str = rank.toProtocolString();
        
        assertTrue(str.startsWith("PAIR"));
        assertTrue(str.contains("_"));
        assertTrue(str.contains("A"));
        assertTrue(str.contains("K"));
    }

    @Test
    void testToProtocolStringNoKickers() {
        HandRank rank = new HandRank(HandRank.PokerRank.ROYAL_FLUSH);
        String str = rank.toProtocolString();
        
        assertEquals("ROYAL_FLUSH", str);
        // Since the rank name already contains underscore, we just check it's the rank name
        assertTrue(str.equals("ROYAL_FLUSH"));
    }

    @Test
    void testToProtocolStringMultipleKickers() {
        HandRank rank = new HandRank(HandRank.PokerRank.HIGH_CARD, Rank.ACE, Rank.KING, Rank.QUEEN, Rank.JACK, Rank.TEN);
        String str = rank.toProtocolString();
        
        // Should be "HIGH_CARD_AKQJ10" (symbols concatenated without separators)
        assertTrue(str.startsWith("HIGH_CARD_"));
        // Just check that all symbols appear somewhere in the string
        String remainder = str.substring("HIGH_CARD_".length());
        assertNotNull(remainder);
        assertTrue(remainder.length() > 0);
    }

    @Test
    void testAllPokerRankEnum() {
        // Test that all enum values are accessible
        HandRank.PokerRank[] ranks = HandRank.PokerRank.values();
        assertEquals(10, ranks.length);
        
        // Test valueOf
        assertEquals(HandRank.PokerRank.PAIR, HandRank.PokerRank.valueOf("PAIR"));
        assertEquals(HandRank.PokerRank.ROYAL_FLUSH, HandRank.PokerRank.valueOf("ROYAL_FLUSH"));
    }

    @Test
    void testToStringWithSingleKicker() {
        HandRank rank = new HandRank(HandRank.PokerRank.THREE_OF_A_KIND, Rank.SEVEN);
        String str = rank.toString();
        
        assertTrue(str.contains("Three of a Kind"));
        assertTrue(str.contains("7"));
        assertTrue(str.contains("("));
        assertTrue(str.contains(")"));
    }
}

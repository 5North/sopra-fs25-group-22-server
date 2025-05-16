package ch.uzh.ifi.hase.soprafs24.game.items;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

 class CardTest {

    @Test
     void cardInitialiseValidInputInitialised() {
        Card card = CardFactory.getCard(Suit.DENARI, 7);
        assertNotNull(card);
        assertEquals(Suit.DENARI, card.getSuit());
        assertEquals(7, card.getValue());
    }

    @Test
     void testInvalidLowValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CardFactory.getCard(Suit.COPPE, 0);
        });
        assertEquals("Card value must be between 1 and 10", exception.getMessage());
    }

    @Test
     void testInvalidHighValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CardFactory.getCard(Suit.BASTONI, 11);
        });
        assertEquals("Card value must be between 1 and 10", exception.getMessage());
    }

    @Test
     void testEqualityAndHashCode() {
        Card card1 = CardFactory.getCard(Suit.SPADE, 7);
        Card card2 = CardFactory.getCard(Suit.SPADE, 7);
        assertEquals(card1, card2);
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    @Test
     void testFactoryCaching() {
        Card card1 = CardFactory.getCard(Suit.DENARI, 3);
        Card card2 = CardFactory.getCard(Suit.DENARI, 3);
        assertSame(card1, card2);
    }

    @Test
     void testCardEquality() {
        Card card1 = CardFactory.getCard(Suit.DENARI, 5);
        Card card2 = CardFactory.getCard(Suit.DENARI, 5);
        Card card3 = CardFactory.getCard(Suit.COPPE, 5);
        Card card4 = CardFactory.getCard(Suit.DENARI, 6);
        assertTrue(card1.equals(card1), "A card should equal itself (reflexivity)");
        assertTrue(card1.equals(card2), "Cards with same suit and value must be equal");
        assertTrue(card2.equals(card1), "Equality should be symmetric");
        assertFalse(card1.equals(card3), "Cards with different suits should not be equal");
        assertFalse(card1.equals(card4), "Cards with different values should not be equal");
        assertFalse(card1.equals(null), "A card should not be equal to null");
        assertFalse(card1.equals((Object) "Not a card"), "A card should not be equal to an object of a different type");
    }

    @Test
     void testEqualsBranches() {
        Card card = CardFactory.getCard(Suit.DENARI, 5);

        assertTrue(card.equals(card), "A card should equal itself (reflexivity).");

        assertFalse(card.equals("Not a Card"), "A card should not equal an object of a different type.");

        Card sameCard = CardFactory.getCard(Suit.DENARI, 5);
        Card differentValue = CardFactory.getCard(Suit.DENARI, 7);
        Card differentSuit = CardFactory.getCard(Suit.COPPE, 5);

        assertTrue(card.equals(sameCard), "Cards with the same suit and value should be equal.");
        assertFalse(card.equals(differentValue), "Cards with different values should not be equal.");
        assertFalse(card.equals(differentSuit), "Cards with different suits should not be equal.");
    }

}

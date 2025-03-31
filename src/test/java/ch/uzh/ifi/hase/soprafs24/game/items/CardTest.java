package ch.uzh.ifi.hase.soprafs24.game.items;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CardTest {

    @Test
    public void cardInitialiseValidInputInitialised() {
        Card card = CardFactory.getCard(Suit.DENARI, 7);
        assertNotNull(card);
        assertEquals(Suit.DENARI, card.getSuit());
        assertEquals(7, card.getValue());
    }

    @Test
    public void testInvalidLowValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CardFactory.getCard(Suit.COPPE, 0);
        });
        assertEquals("Card value must be between 1 and 10", exception.getMessage());
    }

    @Test
    public void testInvalidHighValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CardFactory.getCard(Suit.BASTONI, 11);
        });
        assertEquals("Card value must be between 1 and 10", exception.getMessage());
    }

    @Test
    public void testEqualityAndHashCode() {
        Card card1 = CardFactory.getCard(Suit.SPADE, 7);
        Card card2 = CardFactory.getCard(Suit.SPADE, 7);
        assertEquals(card1, card2);
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    @Test
    public void testFactoryCaching() {
        Card card1 = CardFactory.getCard(Suit.DENARI, 3);
        Card card2 = CardFactory.getCard(Suit.DENARI, 3);
        assertSame(card1, card2);
    }

}

package ch.uzh.ifi.hase.soprafs24.game.items;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CardTest {

    @Test
    public void cardInitialiseValidInputInitialised() {
        Card card = new Card(Suit.DENARI, 1);
        assertEquals(Suit.DENARI, card.getSuit());
        assertEquals(1, card.getValue() );
    }


    @Test
    public void cardInitialiseNotValidValueBiggerThrowsException() {
        assertThrows(
                IllegalArgumentException.class, () -> new Card(Suit.DENARI, 20));
    }

    @Test
    public void cardInitialiseNotValidValueSmallerThrowsException() {
        assertThrows(
                IllegalArgumentException.class, () -> new Card(Suit.DENARI, 0));
    }

}

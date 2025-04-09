package ch.uzh.ifi.hase.soprafs24.game.items;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DateFormat.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CardFactoryTest {

    @Test
    public void testSameInstanceForSameCard() {
        Card card1 = CardFactory.getCard(Suit.COPPE, 4);
        Card card2 = CardFactory.getCard(Suit.COPPE, 4);
        assertSame(card1, card2, "CardFactory should return the same instance for the same card parameters");
    }

    @Test
    public void testDifferentInstancesForDifferentCards() {
        Card card1 = CardFactory.getCard(Suit.COPPE, 4);
        Card card2 = CardFactory.getCard(Suit.DENARI, 4);
        assertNotSame(card1, card2, "Cards with different suits should be different instances");
    }

    @Test
    public void testFactoryCacheSize() {
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 10; value++) {
                CardFactory.getCard(suit, value);
            }
        }
    }

    @Test
    public void testFactoryCacheContent() throws Exception {
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 10; value++) {
                CardFactory.getCard(suit, value);
            }
        }
        java.lang.reflect.Field cacheField = CardFactory.class.getDeclaredField("cardCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Card> cache = (Map<String, Card>) cacheField.get(null);
        assertEquals(40, cache.size(), "The card cache should contain exactly 40 entries.");
    }
}

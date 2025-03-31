package ch.uzh.ifi.hase.soprafs24.game.items;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeckTest {

    @Test
    public void testDeckInitialization() {
        Deck deck = new Deck();
        List<Card> cards = deck.getCards();

        assertEquals(40, cards.size());
        assertTrue(deck.checkCorrectness());
    }

    @Test
    public void testShuffleChangesOrder() {
        Deck deck = new Deck();
        String initialOrder = deck.getCards().toString();
        deck.shuffle();
        String newOrder = deck.getCards().toString();
        assertNotEquals(initialOrder, newOrder);
    }

    @Test
    public void testUniqueCardsInDeck() {
        Deck deck = new Deck();
        List<Card> cards = deck.getCards();
        Set<String> uniqueCards = new HashSet<>();

        for (Card card : cards) {
            String key = card.getSuit().toString() + "-" + card.getValue();
            assertFalse(uniqueCards.contains(key), "Found: " + key);
            uniqueCards.add(key);
        }
        assertEquals(40, uniqueCards.size());
    }
}

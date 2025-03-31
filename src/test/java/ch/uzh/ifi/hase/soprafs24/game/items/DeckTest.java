package ch.uzh.ifi.hase.soprafs24.game.items;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DeckTest {

    @Test
    public void deckInitialization_createsCorrectDeck() {
        Deck deck = new Deck();
        List<Card> cards = deck.getCards();
        assertEquals(40, cards.size());
        assertTrue(deck.checkCorrectness());
    }

    @Test
    public void shuffle_changesOrder() {
        Deck deck = new Deck();
        List<Card> initialOrder = deck.getCards();
        String orderBefore = initialOrder.toString();

        deck.shuffle();
        List<Card> newOrder = deck.getCards();
        String orderAfter = newOrder.toString();
        assertNotEquals(orderBefore, orderAfter);
    }

    @Test
    public void deck_hasUniqueCards() {
        Deck deck = new Deck();
        List<Card> cards = deck.getCards();
        Set<String> uniqueCards = new HashSet<>();
        for (Card card : cards) {
            String key = card.getSuit().toString() + "-" + card.getValue();
            assertFalse(uniqueCards.contains(key), "Duplicate card found: " + key);
            uniqueCards.add(key);
        }
        assertEquals(40, uniqueCards.size());
    }
}

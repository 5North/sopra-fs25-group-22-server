package ch.uzh.ifi.hase.soprafs24.game.items;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.text.DateFormat.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

 class DeckTest {

    @Test
     void testDeckInitialization() {
        Deck deck = new Deck();
        List<Card> cards = deck.getCards();

        assertEquals(40, cards.size());
        assertTrue(deck.checkCorrectness());
    }

    @Test
     void testShuffleChangesOrder() {
        Deck deck = new Deck();
        String initialOrder = deck.getCards().toString();
        deck.shuffle();
        String newOrder = deck.getCards().toString();
        assertNotEquals(initialOrder, newOrder);
    }

    @Test
     void testUniqueCardsInDeck() {
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

    @Test
     void testCheckCorrectnessFailure() throws Exception {
        Deck deck = new Deck();
        List<Card> fakeCards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fakeCards.add(CardFactory.getCard(Suit.COPPE, 1));
        }
        java.lang.reflect.Field cardsField = Deck.class.getDeclaredField("cards");
        cardsField.setAccessible(true);
        cardsField.set(deck, fakeCards);

        assertFalse(deck.checkCorrectness(), "checkCorrectness() should return false for an artificially low sum.");
    }

    @Test
     void testShuffleLoopWithOverriddenCheckCorrectness() {
        Deck deck = new Deck() {
            private int callCount = 0;

            @Override
            public boolean checkCorrectness() {
                callCount++;
                return callCount > 1;
            }
        };

        List<Card> beforeShuffle = new ArrayList<>(deck.getCards());
        deck.shuffle();
        List<Card> afterShuffle = deck.getCards();

        assertNotEquals(beforeShuffle.toString(), afterShuffle.toString(),
                "The deck order should change after shuffle.");
    }
}

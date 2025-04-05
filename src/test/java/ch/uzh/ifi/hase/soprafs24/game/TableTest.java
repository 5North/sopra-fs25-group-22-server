package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;

public class TableTest {

    // Utility method to create a list of cards with the given count and suit.
    private List<Card> createCards(int count, Suit suit) {
        List<Card> cards = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            cards.add(CardFactory.getCard(suit, i));
        }
        return cards;
    }

    @Test
    public void testTableInstantiationWithFourCards() {
        // Create a list of exactly 4 cards.
        List<Card> initialCards = createCards(4, Suit.COPPE);
        Table table = new Table(initialCards);

        // Get the cards from the table and verify that they are the same.
        List<Card> tableCards = table.getCards();
        assertEquals(4, tableCards.size(), "Table should contain exactly 4 cards.");
        for (Card card : initialCards) {
            assertTrue(tableCards.contains(card), "Table should contain card: " + card);
        }
    }

    @Test
    public void testTableInstantiationWithInvalidNumberOfCards() {
        // Create a list with less than 4 cards.
        List<Card> lessThanFour = createCards(3, Suit.COPPE);
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            new Table(lessThanFour);
        }, "Instantiating a Table with less than 4 cards should throw an exception.");
        assertEquals("Initial cards list must contain exactly 4 cards.", exception1.getMessage());

        // Create a list with more than 4 cards.
        List<Card> moreThanFour = createCards(5, Suit.COPPE);
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            new Table(moreThanFour);
        }, "Instantiating a Table with more than 4 cards should throw an exception.");
        assertEquals("Initial cards list must contain exactly 4 cards.", exception2.getMessage());

        // Test passing null.
        Exception exception3 = assertThrows(IllegalArgumentException.class, () -> {
            new Table(null);
        }, "Instantiating a Table with null should throw an exception.");
        assertEquals("Initial cards list must contain exactly 4 cards.", exception3.getMessage());
    }

    @Test
    public void testGetCardsUnmodifiable() {
        List<Card> initialCards = createCards(4, Suit.COPPE);
        Table table = new Table(initialCards);
        List<Card> tableCards = table.getCards();

        // Attempt to modify the list returned by getCards should throw an exception.
        assertThrows(UnsupportedOperationException.class, () -> {
            tableCards.add(CardFactory.getCard(Suit.DENARI, 7));
        }, "The list returned by getCards should be unmodifiable.");
    }

    @Test
    public void testTableInternalStateNotAffectedByExternalListModification() {
        // Create an initial list of 4 cards.
        List<Card> initialCards = createCards(4, Suit.COPPE);
        Table table = new Table(initialCards);

        // Modify the original list.
        initialCards.clear();

        // The table's internal list should remain unchanged.
        List<Card> tableCards = table.getCards();
        assertEquals(4, tableCards.size(), "Modifying the original list should not affect the Table's internal state.");
    }

    @Test
    public void testGetCardsReturnsCorrectSize() {
        // Create a table with exactly 4 cards.
        List<Card> initialCards = createCards(4, Suit.COPPE);
        Table table = new Table(initialCards);
        List<Card> tableCards = table.getCards();
        assertEquals(4, tableCards.size(),
                "getCards should return a list of size 4 when table is initialized with 4 cards.");
    }

    @Test
    public void testGetCardsReturnsEmptyWhenInternalListCleared() throws Exception {
        List<Card> initialCards = createCards(4, Suit.COPPE);
        Table table = new Table(initialCards);

        Field field = Table.class.getDeclaredField("cardsOnTable");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Card> internalList = (List<Card>) field.get(table);
        internalList.clear();

        List<Card> tableCards = table.getCards();
        assertTrue(tableCards.isEmpty(), "getCards should return an empty list when the internal list is cleared.");
    }
}

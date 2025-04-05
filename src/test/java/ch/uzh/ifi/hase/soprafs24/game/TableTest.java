package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private List<Card> createCardsFromValues(List<Integer> values, Suit suit) {
        List<Card> cards = new ArrayList<>();
        for (Integer value : values) {
            cards.add(CardFactory.getCard(suit, value));
        }
        return cards;
    }

    private Map<Integer, Integer> getValueFrequency(List<Card> cards) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (Card card : cards) {
            freq.put(card.getValue(), freq.getOrDefault(card.getValue(), 0) + 1);
        }
        return freq;
    }

    @Test
    public void testGetCaptureOptionsDeterministicSingleOption() {
        // Table: [7, 3, 4, 2]
        List<Card> initialCards = createCardsFromValues(List.of(7, 3, 4, 2), Suit.COPPE);
        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        List<List<Card>> options = table.getCaptureOptions(playedCard);

        assertFalse(options.isEmpty(), "Expected at least one capture option.");
        for (List<Card> option : options) {
            assertEquals(1, option.size(), "Expected a single-card option when a matching card exists.");
            Card c = option.get(0);
            assertEquals(7, c.getValue(), "The single card option must have value 7.");
            assertEquals(Suit.COPPE, c.getSuit(), "The card suit should be COPPE.");
        }
    }

    @Test
    public void testGetCaptureOptionsNonDeterministic() {
        // Table: [3, 4, 2, 2]
        List<Card> initialCards = createCardsFromValues(List.of(3, 4, 2, 2), Suit.COPPE);
        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertEquals(2, options.size(), "Expected two capture options for played card 7 with table [3,4,2,2].");

        boolean foundOption1 = false;
        boolean foundOption2 = false;

        for (List<Card> option : options) {
            int sum = option.stream().mapToInt(Card::getValue).sum();
            assertEquals(7, sum, "Each capture option must sum to 7.");

            Map<Integer, Integer> freq = getValueFrequency(option);
            if (freq.size() == 2 && freq.getOrDefault(3, 0) == 1 && freq.getOrDefault(4, 0) == 1) {
                foundOption1 = true;
            } else if (freq.size() == 2 && freq.getOrDefault(3, 0) == 1 && freq.getOrDefault(2, 0) == 2) {
                foundOption2 = true;
            }
        }
        assertTrue(foundOption1, "Expected option [3,4] not found.");
        assertTrue(foundOption2, "Expected option [3,2,2] not found.");
    }

    @Test
    public void testGetCaptureOptionsNoCapture() {
        List<Card> initialCards = createCardsFromValues(List.of(2, 3, 5, 6), Suit.COPPE);
        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 4);

        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertTrue(options.isEmpty(),
                "Expected no capture options when no combination sums to the played card's value.");
    }

    @Test
    public void testApplyCaptureOptionDeterministic() {
        // Tavolo: [7, 3, 4, 2]
        List<Card> initialCards = createCardsFromValues(List.of(7, 3, 4, 2), Suit.COPPE);
        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertFalse(options.isEmpty(), "Expected capture options for played card 7.");
        List<Card> selectedOption = options.get(0);
        table.applyCaptureOption(selectedOption);
        List<Card> remaining = table.getCards();

        for (Card captured : selectedOption) {
            assertFalse(remaining.contains(captured), "The captured card should have been removed from the table.");
        }
        assertEquals(3, remaining.size(), "Table should have 3 cards remaining after applying capture option.");
    }

    @Test
    public void testApplyCaptureOptionNonDeterministic() {
        List<Card> initialCards = createCardsFromValues(List.of(3, 4, 2, 2), Suit.COPPE);
        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertEquals(2, options.size(), "Expected two capture options.");

        List<Card> selectedOption = options.get(0);
        table.applyCaptureOption(selectedOption);
        List<Card> remaining = table.getCards();

        for (Card captured : selectedOption) {
            assertFalse(remaining.contains(captured), "The captured card should have been removed from the table.");
        }
        assertEquals(4 - selectedOption.size(), remaining.size(), "Remaining cards count is incorrect after capture.");
    }

    @Test
    public void testAddCardAfterNoCapture() {
        // Table: [2, 3, 5, 6]
        List<Card> initialCards = createCardsFromValues(List.of(2, 3, 5, 6), Suit.COPPE);
        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 4);
        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertTrue(options.isEmpty(), "No capture options should be available.");

        table.addCard(playedCard);
        List<Card> updated = table.getCards();
        assertEquals(5, updated.size(), "Table should have 5 cards after adding the played card.");
        assertTrue(updated.contains(playedCard), "Table should contain the played card after addCard.");
    }

    @Test
    public void testFullCaptureProcess() {
        // Table: [7, 3, 4, 2]
        List<Card> initialCards = createCardsFromValues(List.of(7, 3, 4, 2), Suit.COPPE);
        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertFalse(options.isEmpty(), "Capture options should not be empty.");
        for (List<Card> option : options) {
            assertEquals(1, option.size(), "Expected a single-card capture option.");
            assertEquals(7, option.get(0).getValue(), "The card in the capture option should have value 7.");
        }
        List<Card> selectedOption = options.get(0);
        table.applyCaptureOption(selectedOption);

        List<Card> remaining = table.getCards();
        for (Card captured : selectedOption) {
            assertFalse(remaining.contains(captured), "Captured card should be removed from the table.");
        }
        assertEquals(3, remaining.size(), "Table should have 3 cards remaining after full capture process.");
    }

    @Test
    public void testGetCaptureOptionsWithTwo6AndTwo3() {
        // Table: [3, 3, 6, 6]
        List<Card> initialCards = new ArrayList<>();
        initialCards.add(CardFactory.getCard(Suit.COPPE, 3));
        initialCards.add(CardFactory.getCard(Suit.COPPE, 3));
        initialCards.add(CardFactory.getCard(Suit.COPPE, 6));
        initialCards.add(CardFactory.getCard(Suit.COPPE, 6));

        Table table = new Table(initialCards);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 6);

        List<List<Card>> options = table.getCaptureOptions(playedCard);

        assertEquals(2, options.size(), "Expected exactly 2 capture options (each a single 6).");
        for (List<Card> option : options) {
            assertEquals(1, option.size(), "Each capture option should contain exactly one card.");
            Card optionCard = option.get(0);
            assertEquals(6, optionCard.getValue(), "Each capture option must have a card with value 6.");
            assertEquals(Suit.COPPE, optionCard.getSuit(), "The card should have suit COPPE.");
        }
    }

    @Test
    public void testCaptureOptionsWithMixedAndApplyChosenOption() {
        Card card1 = CardFactory.getCard(Suit.DENARI, 3);
        Card card2 = CardFactory.getCard(Suit.DENARI, 4);
        Card card3 = CardFactory.getCard(Suit.COPPE, 3);
        Card card4 = CardFactory.getCard(Suit.DENARI, 10);
        List<Card> initialCards = new ArrayList<>();
        initialCards.add(card1);
        initialCards.add(card2);
        initialCards.add(card3);
        initialCards.add(card4);

        Table table = new Table(initialCards);

        Card playedCard = CardFactory.getCard(Suit.DENARI, 7);

        List<List<Card>> options = table.getCaptureOptions(playedCard);

        assertEquals(2, options.size(), "Expected exactly 2 capture options for played card 7.");

        boolean foundOption1 = false;
        boolean foundOption2 = false;
        for (List<Card> option : options) {
            int sum = option.stream().mapToInt(Card::getValue).sum();
            assertEquals(7, sum, "Each capture option must sum to 7.");
            if (option.contains(card1) && option.contains(card2) && option.size() == 2) {
                foundOption1 = true;
            }
            if (option.contains(card3) && option.contains(card2) && option.size() == 2) {
                foundOption2 = true;
            }
        }
        assertTrue(foundOption1, "Expected capture option [3 of DENARI, 4 of DENARI] not found.");
        assertTrue(foundOption2, "Expected capture option [3 of COPPE, 4 of DENARI] not found.");

        List<Card> selectedOption = null;
        for (List<Card> option : options) {
            if (option.contains(card1) && option.contains(card2)) {
                selectedOption = option;
                break;
            }
        }
        assertNotNull(selectedOption, "Capture option [3 of DENARI, 4 of DENARI] must be found.");

        table.applyCaptureOption(selectedOption);
        List<Card> remaining = table.getCards();

        assertFalse(remaining.contains(card1), "3 of DENARI should have been captured.");
        assertFalse(remaining.contains(card2), "4 of DENARI should have been captured.");

        assertTrue(remaining.contains(card3), "3 of COPPE should remain on the table.");

        long capturingCandidates = remaining.stream().filter(c -> c.getValue() < 7).count();
        assertEquals(1, capturingCandidates, "Only one capturing candidate (a 3) should remain on the table.");
    }

}

package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;

public class PlayerTest {

    // Utility method to create an initial hand of 9 cards.
    private List<Card> createInitialHand() {
        List<Card> initialHand = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            initialHand.add(CardFactory.getCard(Suit.COPPE, i));
        }
        return initialHand;
    }

    @Test
    public void testGetUserId() {
        List<Card> initialHand = createInitialHand();
        String userId = "user123";
        Player player = new Player(userId, initialHand);
        assertEquals(userId, player.getUserId(), "The user ID should match the one provided");
    }

    @Test
    public void testGetHand() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("user456", initialHand);
        List<Card> hand = player.getHand();
        assertEquals(9, hand.size(), "The initial hand should contain 9 cards");
        for (Card card : initialHand) {
            assertTrue(hand.contains(card), "The hand should contain the card: " + card);
        }
    }

    @Test
    public void testPickPlayedCardSuccess() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("user789", initialHand);
        Card cardToPlay = CardFactory.getCard(Suit.COPPE, 5);
        Card playedCard = player.pickPlayedCard(cardToPlay);
        assertEquals(cardToPlay, playedCard, "The played card should be the one picked from the hand");
        assertFalse(player.getHand().contains(cardToPlay), "The hand should no longer contain the played card");
        assertEquals(8, player.getHand().size(), "The hand size should decrease by one after playing a card");
    }

    @Test
    public void testPickPlayedCardNotInHand() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("user987", initialHand);

        // Use a card from a different suit (assuming Suit.BASTONI is different from
        // Suit.COPPE)
        Card cardNotInHand = CardFactory.getCard(Suit.BASTONI, 5);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            player.pickPlayedCard(cardNotInHand);
        }, "Should throw an exception if the card is not present in the hand");

        String expectedMessage = "not present in the player's hand";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage),
                "Error message should indicate that the card is not present");
    }

    @Test
    public void testGetHandUnmodifiable() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("user321", initialHand);
        List<Card> hand = player.getHand();

        assertThrows(UnsupportedOperationException.class, () -> {
            hand.add(CardFactory.getCard(Suit.COPPE, 10));
        }, "The returned hand list should be unmodifiable");
    }

    @Test
    public void testGetHandAfterPlayingSomeCards() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("userAI", initialHand);
        for (int i = 1; i <= 7; i++) {
            Card cardToPlay = CardFactory.getCard(Suit.COPPE, i);
            player.pickPlayedCard(cardToPlay);
        }

        List<Card> remainingHand = player.getHand();
        assertEquals(2, remainingHand.size(), "After playing 7 cards, the remaining hand should contain 2 cards");
    }

    // --- Tests for collectCards functionality ---

    @Test
    public void testInitialTreasureIsEmpty() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("userTreasure", initialHand);

        assertTrue(player.getTreasure().isEmpty(), "Treasure should initially be empty");
        assertEquals(0, player.getScopaCount(), "Initial scopa count should be 0");
    }

    @Test
    public void testCollectCardsWithoutScopa() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("userCollect", initialHand);

        // Prepare a list of cards to collect.
        List<Card> collectedCards = new ArrayList<>();
        collectedCards.add(CardFactory.getCard(Suit.COPPE, 3));
        collectedCards.add(CardFactory.getCard(Suit.COPPE, 7));

        // Call collectCards with scopa flag set to false.
        player.collectCards(collectedCards, false);

        // Verify that the treasure contains the collected cards.
        assertEquals(2, player.getTreasure().size(), "Treasure should contain the collected cards");
        // Verify that scopaCount remains 0.
        assertEquals(0, player.getScopaCount(), "Scopa count should not have increased when scopa flag is false");
    }

    @Test
    public void testCollectCardsWithScopa() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("userScopa", initialHand);

        // Prepare a list of cards to collect.
        List<Card> collectedCards = new ArrayList<>();
        collectedCards.add(CardFactory.getCard(Suit.COPPE, 4));
        collectedCards.add(CardFactory.getCard(Suit.COPPE, 8));

        // Call collectCards with scopa flag set to true.
        player.collectCards(collectedCards, true);

        // Verify that the treasure is updated and scopaCount is incremented.
        assertEquals(2, player.getTreasure().size(), "Treasure should contain the collected cards");
        assertEquals(1, player.getScopaCount(), "Scopa count should increase by 1 when scopa flag is true");
    }

    @Test
    public void testMultipleCollectCardsCalls() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("userMulti", initialHand);

        // First collection: without scopa.
        List<Card> collectedCards1 = new ArrayList<>();
        collectedCards1.add(CardFactory.getCard(Suit.COPPE, 2));
        collectedCards1.add(CardFactory.getCard(Suit.COPPE, 6));
        player.collectCards(collectedCards1, false);

        // Second collection: with scopa.
        List<Card> collectedCards2 = new ArrayList<>();
        collectedCards2.add(CardFactory.getCard(Suit.COPPE, 9));
        player.collectCards(collectedCards2, true);

        // Third collection: with scopa.
        List<Card> collectedCards3 = new ArrayList<>();
        collectedCards3.add(CardFactory.getCard(Suit.COPPE, 1));
        collectedCards3.add(CardFactory.getCard(Suit.COPPE, 3));
        player.collectCards(collectedCards3, true);

        // Total collected cards should be 2 + 1 + 2 = 5.
        assertEquals(5, player.getTreasure().size(),
                "Treasure should contain all collected cards after multiple calls");
        // Scopa count should be 2 (from the two collections with scopa true).
        assertEquals(2, player.getScopaCount(), "Scopa count should correctly reflect the number of scopa events");
    }

    @Test
    public void testGetTreasureUnmodifiable() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("userUnmod", initialHand);

        // Collect some cards to have non-empty treasure.
        List<Card> collectedCards = new ArrayList<>();
        collectedCards.add(CardFactory.getCard(Suit.COPPE, 5));
        player.collectCards(collectedCards, false);

        List<Card> treasure = player.getTreasure();
        // Attempting to modify the returned treasure list should throw an
        // UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> {
            treasure.add(CardFactory.getCard(Suit.COPPE, 10));
        }, "The treasure list should be unmodifiable");
    }

    @Test
    public void testInitialScopaCountIsZero() {
        List<Card> initialHand = createInitialHand();
        Player player = new Player("userScopaTest", initialHand);
        assertEquals(0, player.getScopaCount(), "Initial scopa count should be 0");
    }
}

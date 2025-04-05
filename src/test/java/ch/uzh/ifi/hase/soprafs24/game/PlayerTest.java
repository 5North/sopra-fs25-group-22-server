package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;

public class PlayerTest {

    @Test
    public void testGetUserId() {
        // Create a player with an initial hand of 9 cards
        List<Card> initialHand = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            initialHand.add(CardFactory.getCard(Suit.COPPE, i));
        }
        String userId = "user123";
        Player player = new Player(userId, initialHand);
        assertEquals(userId, player.getUserId(), "The user ID should match the one provided");
    }

    @Test
    public void testGetHand() {
        List<Card> initialHand = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            initialHand.add(CardFactory.getCard(Suit.COPPE, i));
        }
        Player player = new Player("user456", initialHand);
        List<Card> hand = player.getHand();
        assertEquals(9, hand.size(), "The initial hand should contain 9 cards");
        for (Card card : initialHand) {
            assertTrue(hand.contains(card), "The hand should contain the card: " + card);
        }
    }

    @Test
    public void testPickPlayedCardSuccess() {
        List<Card> initialHand = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            initialHand.add(CardFactory.getCard(Suit.COPPE, i));
        }
        Player player = new Player("user789", initialHand);
        Card cardToPlay = CardFactory.getCard(Suit.COPPE, 5);
        Card playedCard = player.pickPlayedCard(cardToPlay);
        assertEquals(cardToPlay, playedCard, "The played card should be the one picked from the hand");
        assertFalse(player.getHand().contains(cardToPlay), "The hand should no longer contain the played card");
        assertEquals(8, player.getHand().size(), "The hand size should decrease by one after playing a card");
    }

    @Test
    public void testPickPlayedCardNotInHand() {
        List<Card> initialHand = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            initialHand.add(CardFactory.getCard(Suit.COPPE, i));
        }
        Player player = new Player("user987", initialHand);

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
        List<Card> initialHand = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            initialHand.add(CardFactory.getCard(Suit.COPPE, i));
        }
        Player player = new Player("user321", initialHand);
        List<Card> hand = player.getHand();

        assertThrows(UnsupportedOperationException.class, () -> {
            hand.add(CardFactory.getCard(Suit.COPPE, 10));
        }, "The returned hand list should be unmodifiable");
    }

    @Test
    public void testGetHandAfterPlayingSomeCards() {
        List<Card> initialHand = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            initialHand.add(CardFactory.getCard(Suit.COPPE, i));
        }
        Player player = new Player("userAI", initialHand);
        for (int i = 1; i <= 7; i++) {
            Card cardToPlay = CardFactory.getCard(Suit.COPPE, i);
            player.pickPlayedCard(cardToPlay);
        }

        List<Card> remainingHand = player.getHand();
        assertEquals(2, remainingHand.size(), "After playing 7 cards, the remaining hand should contain 2 cards");
    }
}

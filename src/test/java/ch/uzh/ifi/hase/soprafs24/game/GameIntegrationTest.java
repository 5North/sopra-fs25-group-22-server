package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameStatisticsUtil;

 class GameIntegrationTest {

    private Long gameId;
    private List<Long> playerIds;
    private GameSession gameSession;

    @BeforeEach
     void setup() {
        UserRepository dummyRepo = mock(UserRepository.class);
        GameStatisticsUtil.setUserRepository(dummyRepo);
        gameId = 10L;
        playerIds = List.of(100L, 200L, 300L, 400L);
        gameSession = new GameSession(gameId, playerIds);
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

    private void setPlayerHand(Player player, List<Card> newHand) {
        try {
            java.lang.reflect.Field field = Player.class.getDeclaredField("hand");
            field.setAccessible(true);
            field.set(player, new ArrayList<>(newHand));
        } catch (Exception e) {
            fail("Failed to set player's hand: " + e.getMessage());
        }
    }

    @Test
     void testDeterministicCaptureTurn() {
        Table table = gameSession.getTable();
        table.clearTable();
        Card tableCard = CardFactory.getCard(Suit.COPPE, 7);
        table.addCard(tableCard);
        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 7));
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        gameSession.playTurn(playedCard, null);
        assertTrue(table.isEmpty(), "After deterministic capture, table should be empty.");
        Player capturingPlayer = gameSession.getPlayers().get(gameSession.getLastGetterIndex());
        List<Card> treasure = capturingPlayer.getTreasure();
        assertEquals(2, treasure.size(), "Player should have collected 2 cards (played + captured).");
        assertTrue(treasure.stream().anyMatch(c -> c.getValue() == 7 && c.getSuit() == Suit.COPPE),
                "Player's treasure should contain the played 7.");

        assertEquals(1, capturingPlayer.getScopaCount(), "Player should have 1 scopa count when table becomes empty.");

        assertEquals(1, gameSession.getCurrentPlayerIndex(), "Turn should pass to next player.");
    }

    @Test
     void testNonDeterministicCaptureTurn() {
        Table table = gameSession.getTable();
        table.clearTable();
        List<Card> customCards = createCardsFromValues(List.of(3, 4, 2, 5), Suit.COPPE);
        for (Card c : customCards) {
            table.addCard(c);
        }

        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 7));
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        try {
            gameSession.playTurn(playedCard, null);
        } catch (IllegalStateException e) {
        }

        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertFalse(options.isEmpty(), "There should be capture options for played card 7.");

        List<Card> selectedOption = null;
        for (List<Card> option : options) {
            int sum = option.stream().mapToInt(Card::getValue).sum();
            if (sum == 7 && option.size() == 2) {
                Map<Integer, Integer> freq = getValueFrequency(option);
                if (freq.getOrDefault(3, 0) == 1 && freq.getOrDefault(4, 0) == 1) {
                    selectedOption = option;
                    break;
                }
            }
        }
        assertNotNull(selectedOption, "Expected capture option [3,4] not found.");

        gameSession.playTurn(playedCard, selectedOption);

        List<Card> remainingTable = table.getCards();
        assertTrue(remainingTable.stream().anyMatch(c -> c.getValue() == 2),
                "Table should contain card with value 2.");
        assertTrue(remainingTable.stream().anyMatch(c -> c.getValue() == 5),
                "Table should contain card with value 5.");
        for (Card c : selectedOption) {
            assertFalse(remainingTable.contains(c), "Captured card should have been removed from the table.");
        }

        Player capturingPlayer = gameSession.getPlayers().get(gameSession.getLastGetterIndex());
        List<Card> treasure = capturingPlayer.getTreasure();
        assertEquals(3, treasure.size(), "Player should have collected 3 cards (played + 2 captured).");
        assertTrue(treasure.stream().anyMatch(c -> c.getValue() == 7),
                "Treasure should contain 7.");
        assertTrue(treasure.stream().anyMatch(c -> c.getValue() == 3),
                "Treasure should contain 3.");
        assertTrue(treasure.stream().anyMatch(c -> c.getValue() == 4),
                "Treasure should contain 4.");

        assertEquals(1, gameSession.getTurnCounter(), "Turn counter should have increased by 1 after the turn.");
    }

    @Test
     void testNoCaptureTurn() {
        Table table = gameSession.getTable();
        table.clearTable();
        List<Card> customCards = createCardsFromValues(List.of(2, 3, 5, 6), Suit.COPPE);
        for (Card c : customCards) {
            table.addCard(c);
        }

        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 4));
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);
        Card playedCard = CardFactory.getCard(Suit.COPPE, 4);

        List<List<Card>> options = table.getCaptureOptions(playedCard);
        assertTrue(options.isEmpty(), "No capture options should be available for played card 4.");

        gameSession.playTurn(playedCard, null);

        List<Card> remainingTable = table.getCards();
        assertTrue(remainingTable.stream().anyMatch(c -> c.getValue() == 4 && c.getSuit() == Suit.COPPE),
                "Played card should have been added to the table.");

        int previousPlayerIndex = (gameSession.getCurrentPlayerIndex() + gameSession.getPlayers().size() - 1)
                % gameSession.getPlayers().size();
        Player previousPlayer = gameSession.getPlayers().get(previousPlayerIndex);
        assertTrue(previousPlayer.getTreasure().isEmpty(),
                "Player's treasure should remain unchanged if no capture occurred.");
    }

    @Test
     void testFinishGame() {
        Table table = gameSession.getTable();
        table.clearTable();
        List<Card> residualCards = createCardsFromValues(List.of(10, 2, 3), Suit.COPPE);
        for (Card c : residualCards) {
            table.addCard(c);
        }
        setTurnCounter(gameSession, 36);
        setLastGetterIndex(gameSession, 2);

        gameSession.finishGame();

        assertTrue(table.isEmpty(), "After finishGame(), the table should be empty.");
        Player lastGetter = gameSession.getPlayers().get(2);
        assertEquals(3, lastGetter.getTreasure().size(), "Last getter should have collected 3 residual cards.");
    }

    @Test
     void testIsGameOver() {
        int turnsToSimulate = 36;
        for (int i = 0; i < turnsToSimulate; i++) {
            Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
            if (currentPlayer.getHand().isEmpty())
                break;
            List<Card> forcedHand = new ArrayList<>();
            forcedHand.add(CardFactory.getCard(Suit.COPPE, 4));
            forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
            setPlayerHand(currentPlayer, forcedHand);
            Card playedCard = CardFactory.getCard(Suit.COPPE, 4);
            gameSession.getTable().clearTable();
            List<Card> nonCapturable = createCardsFromValues(List.of(10, 10, 10, 10), Suit.COPPE);
            for (Card c : nonCapturable) {
                gameSession.getTable().addCard(c);
            }

            gameSession.playTurn(playedCard, null);
        }
        assertTrue(gameSession.isGameOver(), "Game should be over after 36 turns.");
    }

    private void setTurnCounter(GameSession session, int value) {
        try {
            java.lang.reflect.Field field = GameSession.class.getDeclaredField("turnCounter");
            field.setAccessible(true);
            field.setInt(session, value);
        } catch (Exception e) {
            fail("Failed to set turnCounter: " + e.getMessage());
        }
    }

    private void setLastGetterIndex(GameSession session, int value) {
        try {
            java.lang.reflect.Field field = GameSession.class.getDeclaredField("lastGetterIndex");
            field.setAccessible(true);
            field.setInt(session, value);
        } catch (Exception e) {
            fail("Failed to set lastGetterIndex: " + e.getMessage());
        }
    }

    @Test
     void testMultipleCaptureOptionsWithoutSelectionThrowsException() {
        Table table = gameSession.getTable();
        table.clearTable();
        List<Card> customCards = createCardsFromValues(List.of(3, 4, 2, 5), Suit.COPPE);
        for (Card c : customCards) {
            table.addCard(c);
        }
        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 7));
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);

        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            gameSession.playTurn(playedCard, null);
        });
        assertTrue(exception.getMessage().contains("Multiple capture options exist"),
                "Expected IllegalStateException when multiple capture options exist and no selection is provided.");
    }

    @Test
     void testInvalidCaptureOptionThrowsException() {
        Table table = gameSession.getTable();
        table.clearTable();
        List<Card> customCards = createCardsFromValues(List.of(3, 4, 2, 5), Suit.COPPE);
        for (Card c : customCards) {
            table.addCard(c);
        }
        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 7));
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);

        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);

        try {
            gameSession.playTurn(playedCard, null);
        } catch (Exception e) {
        }

        List<Card> invalidOption = new ArrayList<>();
        invalidOption.add(CardFactory.getCard(Suit.COPPE, 8));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            gameSession.playTurn(playedCard, invalidOption);
        });
        assertTrue(exception.getMessage().contains("Selected capture option is not valid"),
                "Expected IllegalArgumentException for an invalid capture option.");
    }

    @Test
     void testLastTurnNoScopa() {
        Table table = gameSession.getTable();
        table.clearTable();
        Card tableCard = CardFactory.getCard(Suit.COPPE, 7);
        table.addCard(tableCard);
        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 7));
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);
        setTurnCounter(gameSession, 35);

        Card playedCard = CardFactory.getCard(Suit.COPPE, 7);
        gameSession.playTurn(playedCard, null);

        Player capturingPlayer = gameSession.getPlayers().get(gameSession.getLastGetterIndex());
        assertEquals(0, capturingPlayer.getScopaCount(),
                "In the last turn, even if the table is emptied, scopa should not be counted.");
    }

}

package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.service.AIService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public class GameServiceIntegrationTest {

    private GameService gameService;
    private WebSocketService webSocketService;
    private AIService aiService;

    @BeforeEach
    public void setup() {
        webSocketService = mock(WebSocketService.class);
        aiService = mock(AIService.class);
        gameService = new GameService(webSocketService, aiService);
    }

    @Test
    public void testDeterministicTurnFlow() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(300L);
        lobby.addUsers(100L);
        lobby.addUsers(200L);
        GameSession session = gameService.startGame(lobby);
        assertNotNull(session);
        assertEquals(300L, session.getGameId());

        session.getTable().clearTable();
        session.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));
        session.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));
        session.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));
        session.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));

        Player currentPlayer = session.getPlayers().get(session.getCurrentPlayerIndex());
        List<Card> newHand = new ArrayList<>();
        Card card7 = CardFactory.getCard(Suit.COPPE, 7);
        newHand.add(card7);
        setPlayerHand(currentPlayer, newHand);

        session.playTurn(card7, null);

        boolean stillPresent = currentPlayer.getHand().stream().anyMatch(c -> c.equals(card7));
        assertFalse(stillPresent);
    }

    @Test
    public void testNonDeterministicTurnFlowWorks() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(400L);
        lobby.addUsers(10L);
        lobby.addUsers(20L);
        GameSession session = gameService.startGame(lobby);
        assertNotNull(session);

        session.getTable().clearTable();
        List<Card> customCards = createCardsFromValues(Arrays.asList(3, 4, 2, 5), Suit.COPPE);
        for (Card c : customCards) {
            session.getTable().addCard(c);
        }

        Player currentPlayer = session.getPlayers().get(session.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        Card card7 = CardFactory.getCard(Suit.COPPE, 7);
        forcedHand.add(card7);
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);

        assertTrue(currentPlayer.getHand().contains(card7));

        try {
            session.playTurn(card7, null);
            fail();
        } catch (IllegalStateException e) {
        }
        assertNotNull(session.getLastCardPlayed());

        List<List<Card>> options = session.getTable().getCaptureOptions(card7);
        assertFalse(options.isEmpty());

        List<Card> validOption = options.get(0);

        for (Card card : validOption) {
            assertTrue(session.getTable().getCards().contains(card));
        }

        gameService.processPlayTurn(session.getGameId(), validOption);

        List<Card> remainingTable = session.getTable().getCards();
        for (Card captured : validOption) {
            assertFalse(remainingTable.contains(captured));
        }
        int expectedRemaining = customCards.size() - validOption.size();
        assertEquals(expectedRemaining, remainingTable.size());

        Player capturingPlayer = session.getPlayers().get(session.getLastGetterIndex());
        List<Card> treasure = capturingPlayer.getTreasure();
        int expectedTreasureSize = validOption.size() + 1;
        assertEquals(expectedTreasureSize, treasure.size());
        assertTrue(treasure.contains(card7));
        for (Card card : validOption) {
            assertTrue(treasure.contains(card));
        }
    }

    @Test
    public void testAiSuggestionReturnsDto() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(500L);
        lobby.addUsers(55L);
        gameService.startGame(lobby);

        String fake = "Play 7 of Denari; Play 4 of Coppe";
        when(aiService.generateAISuggestion(
                anyList(), anyList())).thenReturn(fake);

        AISuggestionDTO dto = gameService.aiSuggestion(500L, 55L);
        assertNotNull(dto);
        assertEquals(fake, dto.getSuggestion());
    }

    @Test
    public void testAiSuggestionThrowsForUnknownUser() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(600L);
        gameService.startGame(lobby);

        assertThrows(NoSuchElementException.class,
                () -> gameService.aiSuggestion(600L, 99L));
    }

    // --- Helper Methods ---

    private void setPlayerHand(Player player, List<Card> newHand) {
        try {
            Field handField = Player.class.getDeclaredField("hand");
            handField.setAccessible(true);
            handField.set(player, newHand);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Card> createCardsFromValues(List<Integer> values, Suit suit) {
        List<Card> cards = new ArrayList<>();
        for (Integer value : values) {
            cards.add(CardFactory.getCard(suit, value));
        }
        return cards;
    }
}

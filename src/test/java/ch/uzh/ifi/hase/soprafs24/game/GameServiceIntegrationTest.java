package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.AIService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.GameStatisticsUtil;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.timer.TimerStrategy;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class GameServiceIntegrationTest {

    private GameService gameService;
    private WebSocketService webSocketService;
    private AIService aiService;
    private TimerService timerService;
    private UserRepository userRepository;

    @BeforeEach
    public void setup() throws Exception {
        webSocketService = mock(WebSocketService.class);
        aiService = mock(AIService.class);
        timerService = mock(TimerService.class);
        // mock delle strategie richieste da timerService
        TimerStrategy playStrat = mock(TimerStrategy.class);
        TimerStrategy choiceStrat = mock(TimerStrategy.class);
        when(timerService.getPlayStrategy()).thenReturn(playStrat);
        when(timerService.getChoiceStrategy()).thenReturn(choiceStrat);

        // MOCK del UserRepository e iniezione in GameStatisticsUtil
        userRepository = mock(UserRepository.class);
        // ogni findById restituisce un utente dummy (per evitare NPE)
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(new User()));
        Field repoField = GameStatisticsUtil.class.getDeclaredField("userRepository");
        repoField.setAccessible(true);
        repoField.set(null, userRepository);

        // infine istanzi il service con tutti i mock
        gameService = new GameService(webSocketService, aiService, timerService);
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

    @Test
    public void testQuitGameIntegration_RemovesSessionAndNotifiesOutcomes() throws Exception {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(99L);
        lobby.addUsers(9L);
        lobby.addUsers(8L);
        Field sessionsField = GameService.class.getDeclaredField("gameSessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, GameSession> sessions = (Map<Long, GameSession>) sessionsField.get(gameService);
        GameSession gs = new GameSession(99L, List.of(9L, 8L));
        sessions.put(99L, gs);

        List<QuitGameResultDTO> results = gameService.quitGame(99L, 9L);

        assertEquals(2, results.size());
        var byUser = results.stream().collect(Collectors.toMap(QuitGameResultDTO::getUserId, dto -> dto));
        assertEquals("LOST", byUser.get(9L).getOutcome());
        assertEquals("WON", byUser.get(8L).getOutcome());

        assertNull(gameService.getGameSessionById(99L));
    }
}

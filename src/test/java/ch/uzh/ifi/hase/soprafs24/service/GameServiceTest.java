package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.utils.GameStatisticsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import ch.uzh.ifi.hase.soprafs24.timer.TimerStrategy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)

@MockitoSettings(strictness = Strictness.LENIENT)
class GameServiceTest {

    @Mock
    private WebSocketService webSocketService;
    @Mock
    private AIService aiService;
    @Mock
    private TimerService timerService;
    @Mock
    private TimerStrategy playTimerStrategy;
    @Mock
    private TimerStrategy choiceTimerStrategy;

    @InjectMocks
    private GameService gameService;

    private Lobby lobby;
    private Long gameId;
    private Long playerA;
    private Long playerB;

    @BeforeEach
    void setup() {
        lobby = new Lobby();
        lobby.setLobbyId(42L);
        lobby.addUser(100L);
        lobby.addUser(200L);
        gameService.startGame(lobby);
        gameId = lobby.getLobbyId();
        playerA = 100L;
        playerB = 200L;
        when(timerService.getPlayStrategy()).thenReturn(playTimerStrategy);
        when(timerService.getChoiceStrategy()).thenReturn(choiceTimerStrategy);
    }

    @Test
    void testStartGameCreatesSession() {
        lobby.addUser(3L);
        lobby.addUser(4L);

        GameSession gameSession = gameService.startGame(lobby);
        assertNotNull(gameSession);
        assertEquals(gameId, gameSession.getGameId());
        assertEquals(4, gameSession.getPlayers().size());
    }

    @Test
    void testGetGameSessionById_success() {
        GameSession gameSession = gameService.startGame(lobby);

        GameSession retrieved = gameService.getGameSessionById(gameId);
        assertNotNull(retrieved);
        assertEquals(gameSession, retrieved);
    }

    @Test
    void testGetGameSessionById_throwsNoSuchElementException() {
        gameService.startGame(lobby);

        assertThrows(NoSuchElementException.class,
                () -> gameService.getGameSessionById(8000L));
    }

    @Test
    void testPlayCardDeterministic() {
        GameSession gameSession = gameService.startGame(lobby);

        gameSession.getTable().clearTable();
        gameSession.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));
        gameSession.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));
        gameSession.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));
        gameSession.getTable().addCard(CardFactory.getCard(Suit.COPPE, 8));

        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> hand = new ArrayList<>();
        Card card7 = CardFactory.getCard(Suit.COPPE, 7);
        hand.add(card7);
        setPlayerHand(currentPlayer, hand);

        CardDTO cardDTO = new CardDTO("COPPE", 7);
        Pair<GameSession, Player> result = gameService.playCard(gameId, cardDTO, playerA);
        assertNotNull(result, "La risposta non deve essere null.");
        GameSession updatedSession = result.getFirst();
        Player nextPlayer = result.getSecond();
        assertNotNull(updatedSession);
        assertNotNull(nextPlayer);

        assertFalse(currentPlayer.getHand().stream().anyMatch(c -> c.getValue() == 7));
    }

    @Test
    void testProcessPlayTurnValidCapture() {
        GameSession gameSession = gameService.startGame(lobby);

        gameSession.getTable().clearTable();
        List<Card> customCards = createCardsFromValues(Arrays.asList(3, 4, 2, 5), Suit.COPPE);
        for (Card c : customCards) {
            gameSession.getTable().addCard(c);
        }

        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> forcedHand = new ArrayList<>();
        Card card7 = CardFactory.getCard(Suit.COPPE, 7);
        forcedHand.add(card7);
        forcedHand.add(CardFactory.getCard(Suit.COPPE, 8));
        setPlayerHand(currentPlayer, forcedHand);

        try {
            gameSession.playTurn(card7, null);
        } catch (IllegalStateException e) {
        }
        List<List<Card>> options = gameSession.getTable().getCaptureOptions(card7);
        assertFalse(options.isEmpty());

        List<Card> selectedOption = null;
        for (List<Card> option : options) {
            int sum = option.stream().mapToInt(Card::getValue).sum();
            if (sum == 7 && option.size() == 2) {
                selectedOption = option;
                break;
            }
        }
        assertNotNull(selectedOption);

        gameService.processPlayTurn(gameId, selectedOption);

        List<Card> remainingTable = gameSession.getTable().getCards();
        for (Card captured : selectedOption) {
            assertFalse(remainingTable.contains(captured));
        }

        assertTrue(remainingTable.stream().anyMatch(c -> c.getValue() == 2));
        assertTrue(remainingTable.stream().anyMatch(c -> c.getValue() == 5));

        Player capturingPlayer = gameSession.getPlayers().get(gameSession.getLastGetterIndex());
        List<Card> treasure = capturingPlayer.getTreasure();
        assertEquals(3, treasure.size());
    }

    @Test
    void testIsGameOverRemovesSession() {
        try {
            Field userRepoField = GameStatisticsUtil.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(null, new DummyUserRepository());
        } catch (Exception e) {
            fail("Reflection failed su userRepository: " + e.getMessage());
        }

        lobby.addUser(3L);
        lobby.addUser(4L);
        GameSession gameSession = gameService.startGame(lobby);

        try {
            Field lastGetterIndexField = GameSession.class.getDeclaredField("lastGetterIndex");
            lastGetterIndexField.setAccessible(true);
            lastGetterIndexField.set(gameSession, 0);
        } catch (Exception e) {
            fail("Reflection failed su lastGetterIndex: " + e.getMessage());
        }

        try {
            Field turnCounterField = GameSession.class.getDeclaredField("turnCounter");
            turnCounterField.setAccessible(true);
            turnCounterField.set(gameSession, 36);
        } catch (Exception e) {
            fail("Reflection failed su turnCounter: " + e.getMessage());
        }

        boolean isOver = gameService.isGameOver(gameId);
        assertTrue(isOver);
        assertThrows(NoSuchElementException.class,
                () -> gameService.getGameSessionById(gameId));
    }

    @Test
    void testPlayCardWithNullGameId() {

        gameService.startGame(lobby);
        CardDTO cardDTO = new CardDTO("COPPE", 7);
        assertThrows(IllegalArgumentException.class, () -> gameService.playCard(null, cardDTO, playerA));
    }

    @Test
    void testPlayCardInvalidCard() {

        GameSession gameSession = gameService.startGame(lobby);

        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> hand = new ArrayList<>();
        hand.add(CardFactory.getCard(Suit.COPPE, 9));
        setPlayerHand(currentPlayer, hand);

        CardDTO cardDTO = new CardDTO("COPPE", 7);
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.playCard(gameId, cardDTO, playerA));
        assertTrue(exception.getMessage().contains("Invalid card played. Unable to process played card."));
    }

    @Test
    void testProcessPlayTurnInvalidCaptureOption() {
        GameSession gameSession = gameService.startGame(lobby);

        gameSession.getTable().clearTable();
        List<Card> customCards = createCardsFromValues(Arrays.asList(3, 4, 2, 5), Suit.COPPE);
        for (Card c : customCards) {
            gameSession.getTable().addCard(c);
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
        List<Card> invalidOption = new ArrayList<>();
        invalidOption.add(CardFactory.getCard(Suit.COPPE, 8));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.processPlayTurn(gameId, invalidOption));
        assertTrue(exception.getMessage().contains("Selected capture option is not valid"));
    }

    @Test
    void testAiSuggestionSuccess() {
        String rawSuggestion = "DENARI-7; COPPE-4";
        GameSession session = gameService.getGameSessionById(gameId);
        List<Card> hand = session.getPlayers().get(0).getHand();
        List<Card> table = session.getTable().getCards();

        when(aiService.generateAISuggestion(hand, table))
                .thenReturn(rawSuggestion);

        AISuggestionDTO dto = gameService.aiSuggestion(gameId, playerA);

        assertNotNull(dto);
        assertEquals(rawSuggestion, dto.getSuggestion());
    }

    @Test
    void testAiSuggestionThrowsForUnknownUser() {
        assertThrows(NoSuchElementException.class,
                () -> gameService.aiSuggestion(gameId, 999L));
    }

    // --- Helper Methods ---
    private void setPlayerHand(Player player, List<Card> newHand) {
        try {
            Field handField = Player.class.getDeclaredField("hand");
            handField.setAccessible(true);
            handField.set(player, newHand);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set player hand", e);
        }
    }

    @Test
    void testQuitGameForfeitRemovesSessionAndReturnsCorrectOutcomes() throws Exception {
        Field urf = GameStatisticsUtil.class.getDeclaredField("userRepository");
        urf.setAccessible(true);
        urf.set(null, new DummyUserRepository());

        GameSession gs = new GameSession(gameId, Arrays.asList(playerA, playerB));
        Field sessionsField = GameService.class.getDeclaredField("gameSessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, GameSession> sessions = (Map<Long, GameSession>) sessionsField.get(gameService);
        sessions.put(gameId, gs);

        List<QuitGameResultDTO> results = gameService.quitGame(gameId, playerA);

        assertEquals(2, results.size(), "Should return one DTO per player in the session");

        Map<Long, QuitGameResultDTO> byUser = results.stream()
                .collect(Collectors.toMap(QuitGameResultDTO::getUserId, dto -> dto));

        assertTrue(byUser.containsKey(playerA), "Result must contain quitting player");
        assertTrue(byUser.containsKey(playerB), "Result must contain other player");

        assertEquals("LOST", byUser.get(playerA).getOutcome());
        assertEquals("You lost by forfeit.", byUser.get(playerA).getMessage());

        assertEquals("WON", byUser.get(playerB).getOutcome());
        assertEquals("You won by forfeit.", byUser.get(playerB).getMessage());

        assertThrows(NoSuchElementException.class,
                () -> gameService.getGameSessionById(gameId));
    }

    @Test
    void testPlayCardGameNotFound() {
        CardDTO dto = new CardDTO("COPPE", 5);
        assertThrows(NoSuchElementException.class,
                () -> gameService.playCard(999L, dto, playerA));
    }

    @Test
    void testProcessPlayTurnSessionNotFound() {
        assertThrows(NoSuchElementException.class,
                () -> gameService.processPlayTurn(999L, List.of()));
    }

    @Test
    void testIsGameOverFalse() {
        assertFalse(gameService.isGameOver(gameId));
    }

    @Test
    void testQuitGameSessionNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gameService.quitGame(999L, playerA));
        assertEquals("Game session not found for id: 999", ex.getMessage());
    }

    @Test
    void testPlayCardMultipleCaptureOptionsSchedulesChoicePhase() {
        GameSession session = gameService.startGame(lobby);

        session.getTable().clearTable();
        List<Card> tableCards = createCardsFromValues(Arrays.asList(3, 4, 2, 5), Suit.COPPE);
        tableCards.forEach(session.getTable()::addCard);

        Player current = session.getPlayers().get(session.getCurrentPlayerIndex());
        Card seven = CardFactory.getCard(Suit.COPPE, 7);
        List<Card> oneCard = new ArrayList<>(List.of(seven));
        setPlayerHand(current, oneCard);

        Pair<GameSession, Player> result = gameService.playCard(gameId, new CardDTO("COPPE", 7), playerA);

        assertNotNull(result.getSecond(), "Expected current player returned even with multiple capture options");
        verify(webSocketService)
                .sentLobbyNotifications(eq(playerA), any(List.class));
        verify(timerService)
                .schedule(gameId, choiceTimerStrategy, playerA);
    }

    @Test
    void testProcessPlayTurnSessionNotFoundThrowsIllegalArgument() {
        GameService spyService = spy(gameService);

        doReturn(null).when(spyService).getGameSessionById(9999L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> spyService.processPlayTurn(9999L, Collections.emptyList()));

        assertEquals(
                "Game session not found for gameId: 9999",
                ex.getMessage());
    }

    private List<Card> createCardsFromValues(List<Integer> values, Suit suit) {
        List<Card> cards = new ArrayList<>();
        for (Integer value : values) {
            cards.add(CardFactory.getCard(suit, value));
        }
        return cards;
    }

    private static class DummyUserRepository implements UserRepository {
        @Override
        public Optional<User> findById(Long id) {
            User user = new User();
            user.setId(id);
            user.setUsername("dummyUser" + id);
            return Optional.of(user);
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>();
        }

        @Override
        public User findByUsername(String username) {
            return null;
        }

        @Override
        public User save(User user) {
            return user;
        }

        @Override
        public List<User> findAll(org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public List<User> findAllById(Iterable<Long> ids) {
            throw new UnsupportedOperationException("Unimplemented method 'findAllById'");
        }

        @Override
        public <S extends User> List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException("Unimplemented method 'saveAll'");
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException("Unimplemented method 'flush'");
        }

        @Override
        public <S extends User> S saveAndFlush(S entity) {
            throw new UnsupportedOperationException("Unimplemented method 'saveAndFlush'");
        }

        @Override
        public void deleteInBatch(Iterable<User> entities) {
            throw new UnsupportedOperationException("Unimplemented method 'deleteInBatch'");
        }

        @Override
        public void deleteAllInBatch() {
            throw new UnsupportedOperationException("Unimplemented method 'deleteAllInBatch'");
        }

        @Override
        public User getOne(Long id) {
            throw new UnsupportedOperationException("Unimplemented method 'getOne'");
        }

        @Override
        public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example,
                org.springframework.data.domain.Sort sort) {
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public boolean existsById(Long id) {
            throw new UnsupportedOperationException("Unimplemented method 'existsById'");
        }

        @Override
        public long count() {
            throw new UnsupportedOperationException("Unimplemented method 'count'");
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException("Unimplemented method 'deleteById'");
        }

        @Override
        public void delete(User entity) {
            throw new UnsupportedOperationException("Unimplemented method 'delete'");
        }

        @Override
        public void deleteAll(Iterable<? extends User> entities) {
            throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
        }

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
        }

        @Override
        public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Unimplemented method 'findOne'");
        }

        @Override
        public <S extends User> org.springframework.data.domain.Page<S> findAll(
                org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public <S extends User> long count(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Unimplemented method 'count'");
        }

        @Override
        public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) {
            throw new UnsupportedOperationException("Unimplemented method 'exists'");
        }

        @Override
        public User findByToken(String token) {
            throw new UnsupportedOperationException("Unimplemented method 'findByToken'");
        }
    }

}

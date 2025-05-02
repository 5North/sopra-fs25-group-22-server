package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private GameService gameService;

    @Mock
    private AIService aiService;

    private Lobby lobby;
    private Long gameId;
    private Long playerA;
    private Long playerB;

    @BeforeEach
    public void setup() {
        lobby = new Lobby();
        lobby.setLobbyId(42L);
        lobby.addUsers(100L);
        lobby.addUsers(200L);
        gameService.startGame(lobby);
        gameId = lobby.getLobbyId();
        playerA = 100L;
        playerB = 200L;
    }

    @Test
    public void testStartGameCreatesSession() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(100L);
        lobby.addUsers(1L);
        lobby.addUsers(2L);
        lobby.addUsers(3L);
        lobby.addUsers(4L);

        GameSession gameSession = gameService.startGame(lobby);
        assertNotNull(gameSession);
        assertEquals(100L, gameSession.getGameId());
        assertEquals(4, gameSession.getPlayers().size());
    }

    @Test
    public void testGetGameSessionById() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(200L);
        lobby.addUsers(10L);
        lobby.addUsers(20L);
        GameSession gameSession = gameService.startGame(lobby);

        GameSession retrieved = gameService.getGameSessionById(200L);
        assertNotNull(retrieved);
        assertEquals(gameSession, retrieved);
    }

    @Test
    public void testPlayCardDeterministic() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(300L);
        lobby.addUsers(100L);
        lobby.addUsers(200L);
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
        Pair<GameSession, Player> result = gameService.playCard(300L, cardDTO, 100L);
        assertNotNull(result, "La risposta non deve essere null.");
        GameSession updatedSession = result.getFirst();
        Player nextPlayer = result.getSecond();
        assertNotNull(updatedSession);
        assertNotNull(nextPlayer);

        assertFalse(currentPlayer.getHand().stream().anyMatch(c -> c.getValue() == 7));
    }

    @Test
    public void testProcessPlayTurnValidCapture() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(400L);
        lobby.addUsers(10L);
        lobby.addUsers(20L);
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

        gameService.processPlayTurn(400L, selectedOption);

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
    public void testIsGameOverRemovesSession() {
        try {
            Field userRepoField = GameStatisticsUtil.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(null, new DummyUserRepository());
        } catch (Exception e) {
            fail("Reflection failed su userRepository: " + e.getMessage());
        }

        Lobby lobby = new Lobby();
        lobby.setLobbyId(500L);
        lobby.addUsers(1L);
        lobby.addUsers(2L);
        lobby.addUsers(3L);
        lobby.addUsers(4L);
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

        boolean isOver = gameService.isGameOver(500L);
        assertTrue(isOver);
        assertNull(gameService.getGameSessionById(500L));
    }

    @Test
    public void testPlayCardWithNullGameId() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(600L);
        lobby.addUsers(1L);
        lobby.addUsers(2L);
        gameService.startGame(lobby);
        CardDTO cardDTO = new CardDTO("COPPE", 7);
        assertThrows(IllegalArgumentException.class, () -> gameService.playCard(null, cardDTO, 1L));
    }

    @Test
    public void testPlayCardInvalidCard() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(700L);
        lobby.addUsers(10L);
        lobby.addUsers(20L);
        GameSession gameSession = gameService.startGame(lobby);

        Player currentPlayer = gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex());
        List<Card> hand = new ArrayList<>();
        hand.add(CardFactory.getCard(Suit.COPPE, 9));
        setPlayerHand(currentPlayer, hand);

        CardDTO cardDTO = new CardDTO("COPPE", 7);
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> gameService.playCard(700L, cardDTO, 10L));
        assertTrue(exception.getMessage().contains("Invalid card played. Unable to process played card."));
    }

    @Test
    public void testProcessPlayTurnInvalidCaptureOption() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(800L);
        lobby.addUsers(10L);
        lobby.addUsers(20L);
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
                () -> gameService.processPlayTurn(800L, invalidOption));
        assertTrue(exception.getMessage().contains("Selected capture option is not valid"));
    }

    @Test
    public void testAiSuggestionSuccess() {
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
    public void testAiSuggestionThrowsForUnknownUser() {
        assertThrows(NoSuchElementException.class,
                () -> gameService.aiSuggestion(gameId, /* userId inesistente */ 999L));
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
    public void testQuitGameForfeitRemovesSessionAndReturnsCorrectOutcomes() throws Exception {
        Long gameId = 123L;
        Long playerA = 10L;
        Long playerB = 20L;

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

        assertNull(gameService.getGameSessionById(gameId), "Session should be removed after forfeit");
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

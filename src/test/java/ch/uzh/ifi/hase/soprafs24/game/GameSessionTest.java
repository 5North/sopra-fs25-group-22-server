package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.Deck;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameStatisticsUtil;

public class GameSessionTest {

    private Long gameId;
    private List<Long> playerIds;
    private GameSession gameSession;

    @BeforeEach
    public void setup() throws Exception {
        Field userRepoField = GameStatisticsUtil.class.getDeclaredField("userRepository");
        userRepoField.setAccessible(true);
        userRepoField.set(null, new DummyUserRepository());

        gameId = 10L;
        playerIds = List.of(100L, 200L, 300L, 400L);
        gameSession = new GameSession(gameId, playerIds);
    }

    @Test
    public void testLobbyIdIsSetCorrectly() {
        assertEquals(gameId, gameSession.getGameId(), "The gameId (lobbyId) should match the provided value.");
    }

    @Test
    public void testPlayersCount() {
        List<Player> players = gameSession.getPlayers();
        assertEquals(4, players.size(), "There should be exactly 4 players in the game session.");
    }

    @Test
    public void testPlayersInitialHandAndStatus() {
        for (Player player : gameSession.getPlayers()) {
            assertEquals(9, player.getHand().size(), "Each player's hand should contain 9 cards.");
            assertTrue(player.getTreasure().isEmpty(), "Each player's treasure should be empty initially.");
            assertEquals(0, player.getScopaCount(), "Each player's scopa count should be 0 initially.");
        }
    }

    @Test
    public void testTableInitialization() {
        Table table = gameSession.getTable();
        assertEquals(4, table.getCards().size(), "The table should be initialized with exactly 4 cards.");
    }

    @Test
    public void testDeckInitialization() {
        Deck deck = gameSession.getDeck();
        assertNotNull(deck, "The deck should not be null.");
        assertEquals(40, deck.getCards().size(), "The deck should contain 40 cards.");
    }

    @Test
    public void testTurnManagementInitialization() {
        assertEquals(0, gameSession.getCurrentPlayerIndex(), "Initial current player index should be 0.");
        assertEquals(-1, gameSession.getLastGetterIndex(), "Initial last getter index should be -1.");
    }

    @Test
    public void testTurnCounterInitialization() {
        assertEquals(0, gameSession.getTurnCounter(), "Initial turn counter should be 0.");
    }

    @Test
    public void testFinishForfeitWhenOddIndexQuits() {
        // Player at index 1 (200L) quits → team {200,400} loses, {100,300} wins
        Long quittingPlayer = 200L;
        Map<Long, String> outcomes = gameSession.finishForfeit(quittingPlayer);

        assertEquals("LOST", outcomes.get(200L), "Quitting player should lose");
        assertEquals("LOST", outcomes.get(400L), "Teammate of quitting player should lose");
        assertEquals("WON", outcomes.get(100L), "Opposing team member should win");
        assertEquals("WON", outcomes.get(300L), "Opposing team member should win");
    }

    @Test
    public void testFinishForfeitWhenEvenIndexQuits() {
        // Player at index 0 (100L) quits → team {100,300} loses, {200,400} wins
        Long quittingPlayer = 100L;
        Map<Long, String> outcomes = gameSession.finishForfeit(quittingPlayer);

        assertEquals("LOST", outcomes.get(100L), "Quitting player should lose");
        assertEquals("LOST", outcomes.get(300L), "Teammate of quitting player should lose");
        assertEquals("WON", outcomes.get(200L), "Opposing team member should win");
        assertEquals("WON", outcomes.get(400L), "Opposing team member should win");
    }

    @Test
    public void testFinishForfeitThrowsForUnknownUser() {
        assertThrows(IllegalArgumentException.class,
                () -> gameSession.finishForfeit(999L),
                "finishForfeit should reject a user ID not in the game");
    }

    @Test
    public void testPlayTurnWithSelectedOptionWithoutLastCardThrows() {
        Card dummy = gameSession.getPlayers().get(0).getHand().get(0);
        List<Card> fakeOption = List.of(dummy);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> gameSession.playTurn(dummy, fakeOption));
        assertEquals(
                "No last card played available for processing capture selection.",
                ex.getMessage());
    }

    @Test
    public void testGetPlayerByIdNotFoundReturnsNull() {
        assertNull(gameSession.getPlayerById(999L));
    }

    @Test
    public void testLastPickedCardsGetterAndSetter() {
        List<Card> picked = new ArrayList<>();
        picked.add(gameSession.getPlayers().get(1).getHand().get(0));
        picked.add(gameSession.getPlayers().get(2).getHand().get(1));

        gameSession.setLastPickedCards(picked);
        assertEquals(picked, gameSession.getLastCardPickedCards());
    }

    @Test
    public void testIsGameOverTrueAfterManuallyIncrementingTurnCounter() throws Exception {
        Field turnCounterField = GameSession.class.getDeclaredField("turnCounter");
        turnCounterField.setAccessible(true);
        turnCounterField.setInt(gameSession, 36);
        assertTrue(gameSession.isGameOver());
    }

    @Test
    public void testFinishGameCollectsRemainingTableCards() throws Exception {
        assertFalse(gameSession.getTable().getCards().isEmpty());
        Field lastGetterField = GameSession.class.getDeclaredField("lastGetterIndex");
        lastGetterField.setAccessible(true);
        lastGetterField.setInt(gameSession, 2);

        List<Card> remaining = new ArrayList<>(gameSession.getTable().getCards());
        gameSession.finishGame();

        assertTrue(gameSession.getTable().getCards().isEmpty());
        Player collector = gameSession.getPlayers().get(2);
        assertEquals(remaining.size(), collector.getTreasure().size());
        assertTrue(collector.getTreasure().containsAll(remaining));
    }

    private static class DummyUserRepository implements UserRepository {
        @Override
        public Optional<User> findById(Long id) {
            User u = new User();
            u.setId(id);
            return Optional.of(u);
        }

        @Override
        public List<User> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends User> S save(S entity) {
            return entity;
        }

        @Override
        public boolean existsById(Long id) {
            return true;
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void deleteById(Long id) {
        }

        @Override
        public void delete(User entity) {
        }

        @Override
        public void deleteAll() {
        }

        @Override
        public List<User> findAllById(Iterable<Long> ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <S extends User> List<S> saveAll(Iterable<S> entities) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void flush() {
        }

        @Override
        public <S extends User> S saveAndFlush(S entity) {
            return entity;
        }

        @Override
        public void deleteInBatch(Iterable<User> entities) {
        }

        @Override
        public void deleteAllInBatch() {
        }

        @Override
        public User getOne(Long id) {
            throw new UnsupportedOperationException();
        }

        // ... e così via per gli altri metodi non usati dal test
        @Override
        public List<User> findAll(Sort sort) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public <S extends User> List<S> findAll(Example<S> example) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public <S extends User> List<S> findAll(Example<S> example, Sort sort) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public Page<User> findAll(Pageable pageable) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public void deleteAll(Iterable<? extends User> entities) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'deleteAll'");
        }

        @Override
        public <S extends User> Optional<S> findOne(Example<S> example) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findOne'");
        }

        @Override
        public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findAll'");
        }

        @Override
        public <S extends User> long count(Example<S> example) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'count'");
        }

        @Override
        public <S extends User> boolean exists(Example<S> example) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'exists'");
        }

        @Override
        public User findByUsername(String username) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findByUsername'");
        }

        @Override
        public User findByToken(String token) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'findByToken'");
        }
    }
}

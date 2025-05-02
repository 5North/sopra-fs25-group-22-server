package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.game.items.Deck;

public class GameSessionTest {

    private Long gameId;
    private List<Long> playerIds;
    private GameSession gameSession;

    @BeforeEach
    public void setup() {
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
        // If user not in session, expect IllegalArgumentException
        assertThrows(IllegalArgumentException.class,
                () -> gameSession.finishForfeit(999L),
                "finishForfeit should reject a user ID not in the game");
    }
}

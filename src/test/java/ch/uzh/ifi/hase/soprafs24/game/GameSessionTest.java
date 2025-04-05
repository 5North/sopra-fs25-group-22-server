package ch.uzh.ifi.hase.soprafs24.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.game.items.Deck;

public class GameSessionTest {

    private Long gameId;
    private List<Long> playerIds;
    private GameSession gameSession;

    @BeforeEach
    public void setup() {
        // Impostiamo un gameId e una lista di 4 playerIds (di tipo Long)
        gameId = 10L;
        playerIds = List.of(100L, 200L, 300L, 400L);
        gameSession = new GameSession(gameId, playerIds);
    }

    @Test
    public void testLobbyIdIsSetCorrectly() {
        // Verifica che il gameId (lobbyId) sia impostato correttamente
        assertEquals(gameId, gameSession.getLobbyId(), "The gameId (lobbyId) should match the provided value.");
    }

    @Test
    public void testPlayersCount() {
        // Verifica che la GameSession abbia esattamente 4 giocatori
        List<Player> players = gameSession.getPlayers();
        assertEquals(4, players.size(), "There should be exactly 4 players in the game session.");
    }

    @Test
    public void testPlayersInitialHandAndStatus() {
        // Per ciascun giocatore, verifica che la mano contenga 9 carte, il tesoro sia
        // vuoto e lo scopaCount sia 0
        for (Player player : gameSession.getPlayers()) {
            assertEquals(9, player.getHand().size(), "Each player's hand should contain 9 cards.");
            assertTrue(player.getTreasure().isEmpty(), "Each player's treasure should be empty initially.");
            assertEquals(0, player.getScopaCount(), "Each player's scopa count should be 0 initially.");
        }
    }

    @Test
    public void testTableInitialization() {
        // Verifica che il Table sia inizializzato con 4 carte
        Table table = gameSession.getTable();
        assertEquals(4, table.getCards().size(), "The table should be initialized with exactly 4 cards.");
    }

    @Test
    public void testDeckInitialization() {
        // Verifica che il mazzo (Deck) contenga 40 carte
        Deck deck = gameSession.getDeck();
        assertNotNull(deck, "The deck should not be null.");
        assertEquals(40, deck.getCards().size(), "The deck should contain 40 cards.");
    }

    @Test
    public void testTurnManagementInitialization() {
        // Verifica che il turno corrente sia impostato su 0 e che nessun giocatore
        // abbia ancora preso (lastGetterIndex = -1)
        assertEquals(0, gameSession.getCurrentPlayerIndex(), "Initial current player index should be 0.");
        assertEquals(-1, gameSession.getLastGetterIndex(), "Initial last getter index should be -1.");
    }
}

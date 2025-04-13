package ch.uzh.ifi.hase.soprafs24.game;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    public void setup() {
        gameService = new GameService();
    }

    @Test
    public void testStartGame_createsGameSession_withCorrectPlayerOrder() {
        // Create a dummy lobby
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1234L);

        // Set the lobby owner (creator)
        User owner = new User();
        owner.setId(10L);
        lobby.setUser(owner);

        // Add extra user IDs to the lobby; these come from users joining via WebSocket,
        // etc.
        List<Long> lobbyUsers = new ArrayList<>(Arrays.asList(20L, 30L, 40L));
        for (Long id : lobbyUsers) {
            lobby.addUsers(id);
        }

        // Call startGame: first player (index 0) must be the lobby owner, then the rest
        // in order.
        GameSession gameSession = gameService.startGame(lobby);

        // Verify game session is created with lobby id as game id
        assertNotNull(gameSession);
        assertEquals(1234L, gameSession.getGameId());

        // Verify that the players are in the correct order: owner first, then others as
        // in lobby.getUsers()
        List<Player> players = gameSession.getPlayers();
        assertEquals(4, players.size());
        assertEquals(10L, players.get(0).getUserId());
        assertEquals(20L, players.get(1).getUserId());
        assertEquals(30L, players.get(2).getUserId());
        assertEquals(40L, players.get(3).getUserId());

        // The gameSessions map should contain an entry with key = 1234L
        GameSession retrieved = gameService.getGameSessionById(1234L);
        assertSame(gameSession, retrieved);
    }

    @Test
    public void testPlayTurn_updatesTurnCounterAndPlayerIndex() {
        List<Long> playerIds = new ArrayList<>(Arrays.asList(1L, 2L));
        GameSession gameSession = new GameSession(999L, playerIds);

        int initialTurn = gameSession.getTurnCounter();
        int initialPlayerIndex = gameSession.getCurrentPlayerIndex();

        Player currentPlayer = gameSession.getPlayers().get(initialPlayerIndex);
        Card playedCard = currentPlayer.getHand().get(0);

        gameSession.getTable().clearTable();

        if (playedCard.getValue() != 10) {
            for (int i = 0; i < 4; i++) {
                gameSession.getTable().addCard(CardFactory.getCard(Suit.COPPE, 10));
            }
        } else {
            for (int i = 0; i < 4; i++) {
                gameSession.getTable().addCard(CardFactory.getCard(Suit.COPPE, 1));
            }
        }

        gameSession.playTurn(playedCard, null);

        assertEquals(initialTurn + 1, gameSession.getTurnCounter());
        assertEquals((initialPlayerIndex + 1) % playerIds.size(), gameSession.getCurrentPlayerIndex());
    }

    @Test
    public void testFinishGame_collectsRemainingCards() {
        List<Long> playerIds = new ArrayList<>(Arrays.asList(1L, 2L));
        GameSession gameSession = new GameSession(888L, playerIds);

        gameSession.getTable().clearTable();
        Card card1 = CardFactory.getCard(Suit.COPPE, 5);
        Card card2 = CardFactory.getCard(Suit.COPPE, 6);
        gameSession.getTable().addCard(card1);
        gameSession.getTable().addCard(card2);

        try {
            Field field = GameSession.class.getDeclaredField("lastGetterIndex");
            field.setAccessible(true);
            field.setInt(gameSession, 0);
        } catch (Exception e) {
            fail("Reflection error: " + e.getMessage());
        }

        gameSession.finishGame();
        assertTrue(gameSession.getTable().isEmpty());
        Player p = gameSession.getPlayers().get(0);
        List<Card> treasure = p.getTreasure();
        assertTrue(treasure.contains(card1) && treasure.contains(card2));
    }
}

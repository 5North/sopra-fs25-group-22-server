package ch.uzh.ifi.hase.soprafs24.game;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceIntegrationTest {

    private GameService gameService;

    @BeforeEach
    public void setup() {
        gameService = new GameService();
    }

    @Test
    public void testGameSessionMappingFromLobby() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(5678L);
        User owner = new User();
        owner.setId(100L);
        lobby.setUser(owner);
        List<Long> extraUserIds = new ArrayList<>(Arrays.asList(200L, 300L, 400L));
        for (Long id : extraUserIds) {
            lobby.addUsers(id);
        }

        GameSession gameSession = gameService.startGame(lobby);

        assertNotNull(lobby.getGameSession());
        List<Player> players = gameSession.getPlayers();
        assertEquals(4, players.size());
        assertEquals(100L, players.get(0).getUserId());
        assertEquals(200L, players.get(1).getUserId());
        assertEquals(300L, players.get(2).getUserId());
        assertEquals(400L, players.get(3).getUserId());

        GameSession retrieved = gameService.getGameSessionById(5678L);
        assertSame(gameSession, retrieved);
    }
}

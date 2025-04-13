package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
@Transactional
public class GameService {

    private final Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();

    /**
     * Starts a new game based on the given lobby. The owner of the lobby is
     * inserted first
     * (if present) followed by other users. The resulting GameSession is associated
     * with the lobby
     * and added to the internal map for future lookup.
     * 
     * @param lobby the lobby from which to start the game
     * @return the created GameSession
     */
    public GameSession startGame(Lobby lobby) {
        Long gameId = lobby.getLobbyId();
        List<Long> playerIds = new ArrayList<>();

        if (lobby.getUser() != null) {
            playerIds.add(lobby.getUser().getId());
        }

        for (Long userId : lobby.getUsers()) {
            if (lobby.getUser() == null || !lobby.getUser().getId().equals(userId)) {
                playerIds.add(userId);
            }
        }

        GameSession gameSession = new GameSession(gameId, playerIds);

        lobby.setGameSession(gameSession);

        gameSessions.put(gameId, gameSession);

        return gameSession;
    }

    public GameSession getGameSessionById(Long gameId) {
        return gameSessions.get(gameId);
    }
}

package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class GameService {

    private final Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();

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

    public GameSession getGameSessionForUser(Long userId) {
        for (GameSession session : gameSessions.values()) {
            if (session.getPlayers().stream().anyMatch(p -> p.getUserId().equals(userId))) {
                return session;
            }
        }
        return null;
    }

    public void processPlayTurn(Long gameId, Card playedCard, List<Card> selectedOption) {
        GameSession game = getGameSessionById(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game session not found for gameId: " + gameId);
        }
        game.playTurn(playedCard, selectedOption);
        if (game.isGameOver()) {
            game.finishGame();
        }
    }
}

package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.ResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class GameService {

    private final Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();

    private final WebSocketService webSocketService;

    @Autowired
    public GameService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    public GameSession startGame(Lobby lobby) {
        Long gameId = lobby.getLobbyId();
        List<Long> playerIds = lobby.getUsers();

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

    public Pair<GameSessionDTO, PrivatePlayerDTO> playCard(Long gameId, CardDTO cardDTO, Long userId) {
        GameSessionDTO updatedGameDTO;
        PrivatePlayerDTO updatedPrivateDTO;
        GameSession game = getGameSessionById(gameId);
        Card playedCard = GameSessionMapper.convertCardDTOtoEntity(cardDTO);
        if (game == null) {
            throw new IllegalArgumentException("Game session not found for gameId: " + gameId);
        }


        if (gameId == null) {
            throw new IllegalArgumentException("Game ID not provided. Unable to process played card.");
        }

        try {
            game.playTurn(playedCard, null);
        } catch (IllegalStateException e) {
            List<List<Card>> options = getGameSessionById(gameId).getTable().getCaptureOptions(playedCard);
            List<List<CardDTO>> optionsDTO = GameSessionMapper.convertCaptureOptionsToDTO(options);
            webSocketService.lobbyNotifications(userId, optionsDTO);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid card played. Unable to process played card.");
        }

        updatedGameDTO = GameSessionMapper.convertToGameSessionDTO(game);

        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        updatedPrivateDTO = GameSessionMapper.convertToPrivatePlayerDTO(currentPlayer);


        //DO NOT TOUCH
        if (game.isGameOver()) {
            game.finishGame();

            game.getPlayers().forEach(player -> {
                ResultDTO resultDTO = GameSessionMapper.convertResultToDTO(game.calculateResult(), player.getUserId());
                webSocketService.lobbyNotifications(player.getUserId(), resultDTO);
            });
        }
        return Pair.of(updatedGameDTO, updatedPrivateDTO);
    }

    public void processPlayTurn(Long gameId, List<Card> selectedOption) {
        GameSession game = getGameSessionById(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game session not found for gameId: " + gameId);
        }
        Card playedCard = game.getLastCardPlayed();
        game.playTurn(playedCard, selectedOption);
        if (game.isGameOver()) {
            game.finishGame();
        }
    }
}

package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.LastCardsDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.ResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.result.Result;
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
    private final AIService aiService;

    @Autowired
    public GameService(WebSocketService webSocketService, AIService aiService) {
        this.webSocketService = webSocketService;
        this.aiService = aiService;
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

    public Pair<GameSession, Player> playCard(Long gameId, CardDTO cardDTO, Long userId) {
        if (gameId == null) {
            throw new IllegalArgumentException("Game ID not provided. Unable to process played card.");
        }
        GameSession game = getGameSessionById(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game session not found for gameId: " + gameId);
        }
        Card playedCard = GameSessionMapper.convertCardDTOtoEntity(cardDTO);
        try {
            Player playingPlayer = game.getCurrentPlayer();
            game.playTurn(playedCard, null);
            return Pair.of(game, playingPlayer);
        } catch (IllegalStateException e) {
            List<List<Card>> options = game.getTable().getCaptureOptions(playedCard);
            List<List<CardDTO>> optionsDTO = GameSessionMapper.convertCaptureOptionsToDTO(options);
            webSocketService.lobbyNotifications(userId, optionsDTO);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid card played. Unable to process played card.");
        }
        return Pair.of(game, null);
    }

    public void processPlayTurn(Long gameId, List<Card> selectedOption) {
        GameSession game = getGameSessionById(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game session not found for gameId: " + gameId);
        }
        Card playedCard = game.getLastCardPlayed();
        game.playTurn(playedCard, selectedOption);
    }

    public boolean isGameOver(Long gameId) {
        GameSession game = getGameSessionById(gameId);
        if (game.isGameOver()) {
            game.finishGame();

            Long playerId = game.getLastPickedPlayerId();
            List<Card> lastCards = game.getTable().getCards();

            LastCardsDTO lastCardsDTO = GameSessionMapper.convertToLastCardsDTO(playerId, lastCards);
            lastCardsDTO.setUserId(playerId);
            webSocketService.broadCastLobbyNotifications(gameId, lastCardsDTO);

            Result result = game.calculateResult();

            game.getPlayers().forEach(player -> {
                ResultDTO resultDTO = GameSessionMapper.convertResultToDTO(result, player.getUserId());
                webSocketService.lobbyNotifications(player.getUserId(), resultDTO);
            });
            gameSessions.remove(gameId);
            return true;
        }
        return false;
    }

    public AISuggestionDTO aiSuggestion(Long gameId, Long userId) {
        GameSession game = getGameSessionById(gameId);
        Player player = game.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
        String raw = aiService.generateAISuggestion(player.getHand(), game.getTable().getCards());
        return new AISuggestionDTO(raw);
    }

}
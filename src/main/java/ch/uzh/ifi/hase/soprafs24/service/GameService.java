package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.LastCardsDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.ResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
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
    private final WebSocketService webSocketService;
    private final AIService aiService;
    private final TimerService timerService;

    @Autowired
    public GameService(WebSocketService webSocketService,
            AIService aiService,
            TimerService timerService) {
        this.webSocketService = webSocketService;
        this.aiService = aiService;
        this.timerService = timerService;
    }

    /** Expose TimerService to allow the strategies to access it */
    public TimerService getTimerService() {
        return this.timerService;
    }

    public GameSession startGame(Lobby lobby) {
        Long gameId = lobby.getLobbyId();
        List<Long> players = lobby.getUsers();

        GameSession gameSession = new GameSession(gameId, players);
        lobby.setGameSession(gameSession);
        gameSessions.put(gameId, gameSession);

        // start turn timer (30s)
        timerService.schedule(gameId,
                timerService.getPlayStrategy(),
                null);

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

        // abort current timer
        timerService.cancel(gameId, timerService.getPlayStrategy());

        Card playedCard = GameSessionMapper.convertCardDTOtoEntity(cardDTO);
        try {
            Player current = game.getCurrentPlayer();
            game.playTurn(playedCard, null);

            // dopo la mossa deterministica, rischedula turno
            timerService.schedule(gameId,
                    timerService.getPlayStrategy(),
                    null);

            return Pair.of(game, current);

        } catch (IllegalStateException e) {
            // più opzioni di cattura: invia le choice options e avvia timer di scelta
            List<List<Card>> options = game.getTable().getCaptureOptions(playedCard);
            List<List<CardDTO>> optsDto = GameSessionMapper.convertCaptureOptionsToDTO(options);
            webSocketService.lobbyNotifications(userId, optsDto);

            timerService.schedule(gameId,
                    timerService.getChoiceStrategy(),
                    userId);

            return Pair.of(game, null);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid card played. Unable to process played card.");
        }
    }

    public void processPlayTurn(Long gameId, List<Card> selectedOption) {
        GameSession game = getGameSessionById(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game session not found for gameId: " + gameId);
        }

        // delete choice-timer if present
        timerService.cancel(gameId, timerService.getChoiceStrategy());

        Card lastPlayed = game.getLastCardPlayed();
        game.playTurn(lastPlayed, selectedOption);

        // schedule again the turn, after the choice
        timerService.schedule(gameId,
                timerService.getPlayStrategy(),
                null);
    }

    public boolean isGameOver(Long gameId) {
        GameSession game = getGameSessionById(gameId);
        boolean over = game.isGameOver();
        if (over) {
            // delete every timer
            timerService.cancel(gameId, timerService.getPlayStrategy());
            timerService.cancel(gameId, timerService.getChoiceStrategy());

            List<Card> lastCards = game.getTable().getCards();
            game.finishGame();

            Long playerId = game.getLastPickedPlayerId();
            LastCardsDTO lastCardsDTO = GameSessionMapper.convertToLastCardsDTO(playerId, lastCards);
            lastCardsDTO.setUserId(playerId);
            webSocketService.broadCastLobbyNotifications(gameId, lastCardsDTO);

            Result result = game.calculateResult();
            game.getPlayers().forEach(player -> {
                ResultDTO resultDTO = GameSessionMapper.convertResultToDTO(result, player.getUserId());
                webSocketService.lobbyNotifications(player.getUserId(), resultDTO);
            });

            gameSessions.remove(gameId);
        }
        return over;
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

    public List<QuitGameResultDTO> quitGame(Long gameId, Long quittingUserId) {
        GameSession game = gameSessions.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game session not found for id: " + gameId);
        }

        Map<Long, String> outcomes = game.finishForfeit(quittingUserId);
        List<QuitGameResultDTO> resultDTOs = new ArrayList<>();
        outcomes.forEach((uid, oc) -> {
            String msg = oc.equals("WON") ? "You won by forfeit." : "You lost by forfeit.";
            resultDTOs.add(GameSessionMapper.toQuitGameResultDTO(uid, oc, msg));
        });

        // delete every timer
        timerService.cancel(gameId, timerService.getPlayStrategy());
        timerService.cancel(gameId, timerService.getChoiceStrategy());

        gameSessions.remove(gameId);
        return resultDTOs;
    }
}

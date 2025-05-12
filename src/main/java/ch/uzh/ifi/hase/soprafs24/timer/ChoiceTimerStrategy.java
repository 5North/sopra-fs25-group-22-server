package ch.uzh.ifi.hase.soprafs24.timer;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.TimeLeftDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component("choiceTimerStrategy")
public class ChoiceTimerStrategy implements TimerStrategy {

    private static final long TIMEOUT_SECONDS = 15L;
    private final GameService gameService;
    private final WebSocketService webSocketService;
    private final TimerService timerService;
    private final Random random = new Random();

    @Autowired
    public ChoiceTimerStrategy(@Lazy GameService gameService,
            WebSocketService webSocketService,
            @Lazy TimerService timerService) {
        this.gameService = gameService;
        this.webSocketService = webSocketService;
        this.timerService = timerService;
    }

    @Override
    public long getTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    public void onTimeout(Long gameId, Long forUserId) {
        GameSession game = gameService.getGameSessionById(gameId);
        if (game == null || game.isGameOver() || forUserId == null)
            return;

        Player chooser = game.getPlayerById(forUserId);
        if (chooser == null)
            return;

        try {
            List<List<Card>> opts = game.getTable().getCaptureOptions(game.getLastCardPlayed());
            if (!opts.isEmpty()) {
                gameService.processPlayTurn(gameId, opts.get(random.nextInt(opts.size())));
            }
        } catch (Exception ignored) {
        }

        GameSession updated = gameService.getGameSessionById(gameId);

        GameSessionDTO publicDto = GameSessionMapper.convertToGameSessionDTO(updated);
        webSocketService.broadCastLobbyNotifications(gameId, publicDto);

        PrivatePlayerDTO privateDto = GameSessionMapper.convertToPrivatePlayerDTO(chooser);
        webSocketService.lobbyNotifications(forUserId, privateDto);

        MoveActionDTO moveDto;
        try {
            moveDto = GameSessionMapper.convertToMoveActionDTO(
                    forUserId,
                    updated.getLastCardPlayed(),
                    updated.getLastCardPickedCards());
        } catch (Exception e) {
            moveDto = new MoveActionDTO();
        }
        webSocketService.broadCastLobbyNotifications(gameId, moveDto);

        // check if game is over
        if(gameService.isGameOver(gameId)){
            return;
        }

        var playStrat = timerService.getPlayStrategy();
        timerService.schedule(gameId, playStrat, null);

        long rem = timerService.getRemainingSeconds(gameId, playStrat);
        TimeLeftDTO timeDto = GameSessionMapper.toTimeToPlayDTO(gameId, rem);
        webSocketService.broadCastLobbyNotifications(gameId, timeDto);
    }
}

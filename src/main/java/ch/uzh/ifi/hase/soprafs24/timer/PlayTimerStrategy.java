package ch.uzh.ifi.hase.soprafs24.timer;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.TimeLeftDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("playTimerStrategy")
public class PlayTimerStrategy implements TimerStrategy {

    private static final long TIMEOUT_SECONDS = 30L;
    private static final Logger log = LoggerFactory.getLogger(PlayTimerStrategy.class);
    private final GameService gameService;
    private final WebSocketService webSocketService;
    private final TimerService timerService;
    private final Random random = new Random();

    @Autowired
    public PlayTimerStrategy(@Lazy GameService gameService,
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
        if (game == null || game.isGameOver())
            return;

        Player current = game.getCurrentPlayer();
        Long currentUserId = current.getUserId();

        // Try auto-play only, swallow exceptions without blocking the rest
        var hand = current.getHand();
        if (!hand.isEmpty()) {
            try {
                var c = hand.get(random.nextInt(hand.size()));
                gameService.playCard(
                        gameId,
                        new CardDTO(c.getSuit().toString(), c.getValue()),
                        currentUserId);
            } catch (Exception e) {
                log.warn("Failed to auto-play card on timeout", e);
            }
        }

        GameSession updated = gameService.getGameSessionById(gameId);

        GameSessionDTO publicDto = GameSessionMapper.convertToGameSessionDTO(updated);
        webSocketService.broadCastLobbyNotifications(gameId, publicDto);
        log.debug("Message broadcasted to lobby {}: game update on play timeout", gameId);

        PrivatePlayerDTO privateDto = GameSessionMapper.convertToPrivatePlayerDTO(
                updated.getPlayerById(currentUserId));
        webSocketService.sentLobbyNotifications(currentUserId, privateDto);
        log.debug("Message sent to user {}: game update on play timeout", currentUserId);

        MoveActionDTO moveDto;
        try {
            moveDto = GameSessionMapper.convertToMoveActionDTO(
                    currentUserId,
                    updated.getLastCardPlayed(),
                    updated.getLastCardPickedCards());
        } catch (Exception e) {
            moveDto = new MoveActionDTO();
        }
        webSocketService.broadCastLobbyNotifications(gameId, moveDto);
        log.info("Message broadcasted to lobby {}: move update on play timeout", gameId);

        // check if game is over before rescheduling
        if (gameService.isGameOver(gameId)) {
            return;
        }

        // reschedule next auto-play
        timerService.schedule(gameId, this, null);

        long rem = timerService.getRemainingSeconds(gameId, this);
        TimeLeftDTO timeDto = GameSessionMapper.toTimeToPlayDTO(gameId, rem);
        webSocketService.broadCastLobbyNotifications(gameId, timeDto);
        log.debug("Message broadcasted to lobby {}: time left for play", gameId);
    }
}

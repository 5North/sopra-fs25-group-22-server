package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.TimeOutNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;

@Service
public class TimerService {

    static final long TURN_TIMEOUT_SECONDS = 30;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final GameService gameService;
    private final WebSocketService webSocketService;

    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Map<Long, Long> expirations = new ConcurrentHashMap<>();

    @Autowired
    public TimerService(@Lazy GameService gameService,
            WebSocketService webSocketService) {
        this.gameService = gameService;
        this.webSocketService = webSocketService;
    }

    /**
     * Schedule a timeout for the given gameId.
     * Cancels any existing timeout for that game first.
     */
    public void schedule(Long gameId) {
        cancel(gameId);
        long expireAt = System.currentTimeMillis() + TURN_TIMEOUT_SECONDS * 1000;
        expirations.put(gameId, expireAt);

        ScheduledFuture<?> future = scheduler.schedule(
                () -> timeoutAction(gameId),
                TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        tasks.put(gameId, future);
    }

    /**
     * Cancel any pending timeout for the given gameId.
     */
    public void cancel(Long gameId) {
        expirations.remove(gameId);
        Optional.ofNullable(tasks.remove(gameId))
                .ifPresent(f -> f.cancel(false));
    }

    /**
     * Return the remaining seconds until timeout for the given gameId,
     * or 0 if none is scheduled or it's already expired.
     */
    public long getRemainingSeconds(Long gameId) {
        Long exp = expirations.get(gameId);
        if (exp == null) {
            return 0;
        }
        long rem = (exp - System.currentTimeMillis()) / 1000;
        return Math.max(rem, 0);
    }

    /**
     * Called when a timeout fires: performs an automatic play,
     * broadcasts the usual turn messages plus a timeout notification,
     * then checks for game end and either reschedules or cleans up.
     */
    private void timeoutAction(Long gameId) {
        GameSession game = gameService.getGameSessionById(gameId);
        if (game == null) {
            cancel(gameId);
            return;
        }

        synchronized (game) {
            if (game.isGameOver()) {
                cancel(gameId);
                return;
            }

            Player current = game.getCurrentPlayer();
            List<Card> hand = current.getHand();
            Card randomCard = hand.get(new Random().nextInt(hand.size()));

            List<List<Card>> options = game.getTable().getCaptureOptions(randomCard);
            List<Card> choice = options.isEmpty()
                    ? Collections.emptyList()
                    : options.get(0);

            game.playTurn(randomCard, choice);

            MoveActionDTO moveDto = GameSessionMapper
                    .convertToMoveActionDTO(
                            current.getUserId(),
                            randomCard,
                            game.getLastCardPickedCards());
            webSocketService.broadCastLobbyNotifications(gameId, moveDto);

            GameSessionDTO publicDto = GameSessionMapper
                    .convertToGameSessionDTO(game);
            webSocketService.broadCastLobbyNotifications(gameId, publicDto);

            PrivatePlayerDTO privateDto = GameSessionMapper
                    .convertToPrivatePlayerDTO(game.getCurrentPlayer());
            webSocketService.lobbyNotifications(
                    game.getCurrentPlayer().getUserId(),
                    privateDto);

            TimeOutNotificationDTO timeoutDto = new TimeOutNotificationDTO(
                    current.getUserId(),
                    "Time expired: automatic move executed");
            webSocketService.broadCastLobbyNotifications(gameId, timeoutDto);

            boolean ended = gameService.isGameOver(gameId);
            if (!ended) {
                schedule(gameId);
            } else {
                cancel(gameId);
            }
        }
    }
}

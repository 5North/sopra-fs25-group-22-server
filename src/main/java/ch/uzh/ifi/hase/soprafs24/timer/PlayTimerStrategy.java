// src/main/java/ch/uzh/ifi/hase/soprafs24/timer/PlayTimerStrategy.java
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("playTimerStrategy")
public class PlayTimerStrategy implements TimerStrategy {

    private static final long TIMEOUT_SECONDS = 30L;
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

        // 1) Identifico il player corrente
        Player current = game.getCurrentPlayer();
        Long currentUserId = current.getUserId();

        // 2) Provo a giocare una carta casuale (swallow eventuali exception di mock)
        try {
            var hand = current.getHand();
            if (!hand.isEmpty()) {
                var c = hand.get(random.nextInt(hand.size()));
                gameService.playCard(gameId,
                        new CardDTO(c.getSuit().toString(), c.getValue()),
                        currentUserId);
            }
        } catch (Exception ignored) {
        }

        // 3) Recupero stato aggiornato
        GameSession updated = gameService.getGameSessionById(gameId);

        // 4) Broadcast stato pubblico
        GameSessionDTO publicDto = GameSessionMapper.convertToGameSessionDTO(updated);
        webSocketService.broadCastLobbyNotifications(gameId, publicDto);

        // 5) Notifica privata mano aggiornata
        PrivatePlayerDTO privateDto = GameSessionMapper.convertToPrivatePlayerDTO(
                updated.getPlayerById(currentUserId));
        webSocketService.lobbyNotifications(currentUserId, privateDto);

        // 6) Broadcast MoveActionDTO (safe guard su NPE)
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

        // check if game is over
        if(gameService.isGameOver(gameId)){
            return;
        }

        // 7) Rischedulo play‐phase subito
        timerService.schedule(gameId, this, null);

        // 8) Broadcast timer rimanente
        long rem = timerService.getRemainingSeconds(gameId, this);
        TimeLeftDTO timeDto = GameSessionMapper.toTimeToPlayDTO(gameId, rem);
        webSocketService.broadCastLobbyNotifications(gameId, timeDto);
    }
}

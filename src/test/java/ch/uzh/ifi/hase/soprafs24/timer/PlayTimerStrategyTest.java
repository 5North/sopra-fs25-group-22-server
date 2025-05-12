// src/test/java/ch/uzh/ifi/hase/soprafs24/timer/PlayTimerStrategyTest.java
package ch.uzh.ifi.hase.soprafs24.timer;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlayTimerStrategyTest {

    private GameService gameService;
    private WebSocketService webSocketService;
    private TimerService timerService;
    private PlayTimerStrategy strategy;

    @BeforeEach
    void setup() {
        gameService = mock(GameService.class);
        webSocketService = mock(WebSocketService.class);
        timerService = mock(TimerService.class);
        strategy = new PlayTimerStrategy(gameService, webSocketService, timerService);
    }

    @Test
    void onTimeout_validGame_broadcastsAndReschedules() {
        Long gameId = 1L;


        GameSession session = new GameSession(gameId, List.of(10L, 20L, 30L, 40L));


        when(gameService.getGameSessionById(gameId)).thenReturn(session);

        when(gameService.playCard(eq(gameId), any(CardDTO.class), isNull()))
                .thenReturn(Pair.of(session, session.getCurrentPlayer()));

        when(timerService.getRemainingSeconds(gameId, strategy)).thenReturn(15L);


        strategy.onTimeout(gameId, null);


        verify(webSocketService, times(3))
                .broadCastLobbyNotifications(eq(gameId), any());

        verify(timerService).schedule(eq(gameId), eq(strategy), isNull());
    }

    @Test
    void onTimeout_noGame_doesNothing() {
        Long gameId = 2L;
        when(gameService.getGameSessionById(gameId)).thenReturn(null);

        strategy.onTimeout(gameId, null);

        verifyNoInteractions(webSocketService, timerService);
    }
}

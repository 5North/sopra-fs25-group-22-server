package ch.uzh.ifi.hase.soprafs24.timer;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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

    @Test
    void onTimeout_gameNull_doesNothing() {
        when(gameService.getGameSessionById(1L)).thenReturn(null);
        strategy.onTimeout(1L, 10L);
        verifyNoInteractions(webSocketService, timerService);
    }

    @Test
    void onTimeout_gameOver_doesNothing() {
        GameSession over = mock(GameSession.class);
        when(over.isGameOver()).thenReturn(true);
        when(gameService.getGameSessionById(2L)).thenReturn(over);
        strategy.onTimeout(2L, 10L);
        verifyNoInteractions(webSocketService, timerService);
    }

    @Test
    void onTimeout_emptyHand_stillBroadcastsAndReschedules() {
        long gameId = 3L;
        long uid = 20L;

        GameSession real = new GameSession(gameId, List.of(uid, 2L, 3L, 4L));
        Player emptyPlayer = mock(Player.class);
        when(emptyPlayer.getUserId()).thenReturn(uid);
        when(emptyPlayer.getHand()).thenReturn(emptyList());

        GameSession session = spy(real);
        doReturn(emptyPlayer).when(session).getCurrentPlayer();

        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        when(gameService.isGameOver(gameId)).thenReturn(false).thenReturn(true);
        when(timerService.getRemainingSeconds(gameId, strategy)).thenReturn(15L);

        strategy.onTimeout(gameId, uid);

        verify(webSocketService, times(3)).broadCastLobbyNotifications(eq(gameId), any());
        verify(webSocketService).sentLobbyNotifications(eq(uid), any());
        verify(timerService).schedule(eq(gameId), eq(strategy), isNull());
    }

    @Test
    void onTimeout_exceptionDuringPlay_stillBroadcastsAndReschedules() {
        long gameId = 4L;
        long uid = 30L;

        GameSession real = new GameSession(gameId, List.of(uid, 2L, 3L, 4L));
        Card card = CardFactory.getCard(Suit.COPPE, 7);

        Player stubPlayer = mock(Player.class);
        when(stubPlayer.getUserId()).thenReturn(uid);
        when(stubPlayer.getHand()).thenReturn(singletonList(card));

        GameSession session = spy(real);
        doReturn(stubPlayer).when(session).getCurrentPlayer();
        doReturn(stubPlayer).when(session).getPlayerById(uid);

        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        doThrow(new RuntimeException("boom"))
                .when(gameService).playCard(eq(gameId), any(CardDTO.class), eq(uid));
        when(gameService.isGameOver(gameId)).thenReturn(false).thenReturn(true);
        when(timerService.getRemainingSeconds(gameId, strategy)).thenReturn(20L);

        strategy.onTimeout(gameId, uid);

        verify(webSocketService, times(3)).broadCastLobbyNotifications(eq(gameId), any());
        verify(webSocketService).sentLobbyNotifications(eq(uid), any());
        verify(timerService).schedule(eq(gameId), eq(strategy), isNull());
    }

}

package ch.uzh.ifi.hase.soprafs24.timer;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Table;

import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChoiceTimerStrategyTest {

    private GameService gameService;
    private WebSocketService webSocketService;
    private TimerService timerService;
    private ChoiceTimerStrategy strategy;
    private TimerStrategy fakePlay;
    private final Long userId = 30L;

    @BeforeEach
    void setup() {
        gameService = mock(GameService.class);
        webSocketService = mock(WebSocketService.class);
        timerService = mock(TimerService.class);

        fakePlay = mock(TimerStrategy.class);
        when(timerService.getPlayStrategy()).thenReturn(fakePlay);
        when(timerService.getChoiceStrategy())
                .thenReturn(new ChoiceTimerStrategy(gameService, webSocketService, timerService));

        strategy = new ChoiceTimerStrategy(gameService, webSocketService, timerService);
    }

    @Test
    void onTimeout_validGame_broadcastsAndReschedules() {
        Long gameId = 3L;

        GameSession session = new GameSession(gameId, List.of(userId, 2L, 3L, 4L));

        Table table = session.getTable();
        table.clearTable();

        table.addCard(CardFactory.getCard(Suit.COPPE, 2));
        table.addCard(CardFactory.getCard(Suit.COPPE, 3));
        table.addCard(CardFactory.getCard(Suit.COPPE, 6));
        table.addCard(CardFactory.getCard(Suit.COPPE, 5));

        session.setLastCardPlayed(CardFactory.getCard(Suit.COPPE, 5));

        when(gameService.getGameSessionById(gameId)).thenReturn(session);

        strategy.onTimeout(gameId, userId);

        verify(webSocketService, times(3))
                .broadCastLobbyNotifications(eq(gameId), any());

        verify(webSocketService).sentLobbyNotifications(eq(userId), any());

        verify(timerService).schedule(eq(gameId), eq(fakePlay), isNull());
    }

    @Test
    void onTimeout_invalidGameOrNullUser_doesNothing() {
        Long gameId = 4L;
        when(gameService.getGameSessionById(gameId)).thenReturn(null);
        strategy.onTimeout(gameId, userId);
        verifyNoInteractions(webSocketService, timerService);

        GameSession session = new GameSession(gameId, List.of(userId, 2L, 3L, 4L));
        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        strategy.onTimeout(gameId, null);
        verifyNoInteractions(webSocketService, timerService);
    }

    @Test
    void testGetTimeoutSeconds() {
        assertEquals(15L, strategy.getTimeoutSeconds());
    }

    @Test
    void testOnTimeout_gameIsOver_doesNothing() {
        Long gameId = 10L;
        GameSession overSession = mock(GameSession.class);
        when(overSession.isGameOver()).thenReturn(true);
        when(gameService.getGameSessionById(gameId)).thenReturn(overSession);

        strategy.onTimeout(gameId, userId);

        verifyNoInteractions(webSocketService);
        verify(timerService, never()).schedule(anyLong(), any(), any());
    }

    @Test
    void testOnTimeout_chooserNotInGame_doesNothing() {
        Long gameId = 11L;
        GameSession session = mock(GameSession.class);
        when(session.isGameOver()).thenReturn(false);
        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        when(session.getPlayerById(userId)).thenReturn(null);

        strategy.onTimeout(gameId, userId);

        verifyNoInteractions(webSocketService);
        verify(timerService, never()).schedule(anyLong(), any(), any());
    }
}

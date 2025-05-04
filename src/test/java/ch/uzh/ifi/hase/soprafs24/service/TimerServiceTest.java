package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.Table;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.TimeOutNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimerServiceTest {

    private TimerService timerService;
    private GameService gameService;
    private WebSocketService ws;

    private final Long gameId = 123L;
    private final Card card7 = CardFactory.getCard(Suit.COPPE, 7);
    private final Card card5 = CardFactory.getCard(Suit.COPPE, 5);
    private final Card card3 = CardFactory.getCard(Suit.COPPE, 3);

    @BeforeEach
    public void setUp() {
        gameService = mock(GameService.class);
        ws = mock(WebSocketService.class);
        timerService = new TimerService(gameService, ws);
    }

    @Test
    public void testScheduleAndCancel_affectsRemainingSeconds() {
        timerService.schedule(gameId);
        long rem1 = timerService.getRemainingSeconds(gameId);
        assertTrue(rem1 > 0 && rem1 <= TimerService.TURN_TIMEOUT_SECONDS,
                "Remaining seconds should be >0 after scheduling");

        timerService.cancel(gameId);
        long rem2 = timerService.getRemainingSeconds(gameId);
        assertEquals(0, rem2, "Remaining seconds should be 0 after cancel");
    }

    @Test
    public void testTimeoutAction_noSession_doesNotThrow() throws Exception {
        when(gameService.getGameSessionById(gameId)).thenReturn(null);

        Method m = TimerService.class.getDeclaredMethod("timeoutAction", Long.class);
        m.setAccessible(true);
        m.invoke(timerService, gameId);

        verifyNoInteractions(ws);
    }

    @Test
    public void testTimeoutAction_gameOver_cancelsOnly() throws Exception {
        GameSession session = mock(GameSession.class);
        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        when(session.isGameOver()).thenReturn(true);

        TimerService spy = spy(timerService);
        doNothing().when(spy).cancel(gameId);

        Method m = TimerService.class.getDeclaredMethod("timeoutAction", Long.class);
        m.setAccessible(true);
        m.invoke(spy, gameId);

        verify(spy).cancel(gameId);
        verifyNoInteractions(ws);
    }

    @Test
    public void testTimeoutAction_autoPlay_noCaptureOptions() throws Exception {
        GameSession session = mock(GameSession.class);
        Player current = mock(Player.class);

        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        when(session.isGameOver()).thenReturn(false);
        when(session.getCurrentPlayer()).thenReturn(current);
        when(current.getHand()).thenReturn(Collections.singletonList(card7));

        Table table = mock(Table.class);
        when(session.getTable()).thenReturn(table);
        when(table.getCaptureOptions(card7)).thenReturn(Collections.emptyList());

        TimerService spy = spy(timerService);
        doNothing().when(spy).schedule(gameId);

        Method m = TimerService.class.getDeclaredMethod("timeoutAction", Long.class);
        m.setAccessible(true);
        m.invoke(spy, gameId);

        verify(session).playTurn(card7, Collections.emptyList());

        verify(ws).broadCastLobbyNotifications(eq(gameId), any(MoveActionDTO.class));
        verify(ws).broadCastLobbyNotifications(eq(gameId), any(GameSessionDTO.class));
        verify(ws).lobbyNotifications(anyLong(), any(PrivatePlayerDTO.class));
        verify(ws).broadCastLobbyNotifications(eq(gameId), any(TimeOutNotificationDTO.class));

        verify(spy).schedule(gameId);
    }

    @Test
    public void testTimeoutAction_autoPlay_singleCaptureOption() throws Exception {
        GameSession session = mock(GameSession.class);
        Player current = mock(Player.class);

        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        when(session.isGameOver()).thenReturn(false);
        when(session.getCurrentPlayer()).thenReturn(current);
        when(current.getHand()).thenReturn(Collections.singletonList(card5));

        Table table = mock(Table.class);
        when(session.getTable()).thenReturn(table);
        List<Card> singleOpt = Collections.singletonList(card3);
        when(table.getCaptureOptions(card5)).thenReturn(Collections.singletonList(singleOpt));

        TimerService spy = spy(timerService);
        doNothing().when(spy).schedule(gameId);

        Method m = TimerService.class.getDeclaredMethod("timeoutAction", Long.class);
        m.setAccessible(true);
        m.invoke(spy, gameId);

        verify(session).playTurn(card5, singleOpt);
    }

    @Test
    public void testTimeoutAction_autoPlay_multipleCapture_picksFirst() throws Exception {
        GameSession session = mock(GameSession.class);
        Player current = mock(Player.class);

        when(gameService.getGameSessionById(gameId)).thenReturn(session);
        when(session.isGameOver()).thenReturn(false);
        when(session.getCurrentPlayer()).thenReturn(current);
        when(current.getHand()).thenReturn(Collections.singletonList(card7));

        Table table = mock(Table.class);
        when(session.getTable()).thenReturn(table);
        List<Card> opt1 = Collections.singletonList(card3);
        List<Card> opt2 = Collections.singletonList(card5);
        when(table.getCaptureOptions(card7)).thenReturn(Arrays.asList(opt1, opt2));

        TimerService spy = spy(timerService);
        doNothing().when(spy).schedule(gameId);

        Method m = TimerService.class.getDeclaredMethod("timeoutAction", Long.class);
        m.setAccessible(true);
        m.invoke(spy, gameId);

        verify(session).playTurn(card7, opt1);
    }
}

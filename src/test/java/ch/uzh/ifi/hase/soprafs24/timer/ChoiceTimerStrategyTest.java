// src/test/java/ch/uzh/ifi/hase/soprafs24/timer/ChoiceTimerStrategyTest.java
package ch.uzh.ifi.hase.soprafs24.timer;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Table;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        // play‐strategy per reschedule
        fakePlay = mock(TimerStrategy.class);
        when(timerService.getPlayStrategy()).thenReturn(fakePlay);
        when(timerService.getChoiceStrategy())
                .thenReturn(new ChoiceTimerStrategy(gameService, webSocketService, timerService));

        strategy = new ChoiceTimerStrategy(gameService, webSocketService, timerService);
    }

    @Test
    void onTimeout_validGame_broadcastsAndReschedules() {
        Long gameId = 3L;

        // sessione con 4 giocatori
        GameSession session = new GameSession(gameId, List.of(userId, 2L, 3L, 4L));

        // preparo una Table pulita
        Table table = session.getTable();
        table.clearTable();
        // aggiungo 4 carte in modo che per playedCard=5 ci sia sempre un'opzione valida
        table.addCard(CardFactory.getCard(Suit.COPPE, 2));
        table.addCard(CardFactory.getCard(Suit.COPPE, 3));
        table.addCard(CardFactory.getCard(Suit.COPPE, 6));
        table.addCard(CardFactory.getCard(Suit.COPPE, 5));

        // imposto lastCardPlayed = 5
        session.setLastCardPlayed(CardFactory.getCard(Suit.COPPE, 5));

        // stub per il service
        when(gameService.getGameSessionById(gameId)).thenReturn(session);

        // eseguo la strategy
        strategy.onTimeout(gameId, userId);

        // verifico 3 broadcast (stato, azione, timer)
        verify(webSocketService, times(3))
                .broadCastLobbyNotifications(eq(gameId), any());
        // verifico notifica privata
        verify(webSocketService).lobbyNotifications(eq(userId), any());
        // verifico reschedule
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
}

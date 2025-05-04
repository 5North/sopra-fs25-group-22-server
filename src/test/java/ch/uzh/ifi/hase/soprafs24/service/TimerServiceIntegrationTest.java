package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TimerServiceIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private TimerService timerService;

    @SpyBean
    private WebSocketService webSocketService;

    private final Long gameId = 999L;

    @Test
    void testAutomaticTimeoutFiresWithinThirtySeconds() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            Object payload = inv.getArgument(1);
            if (payload instanceof MoveActionDTO) {
                latch.countDown();
            }
            return null;
        }).when(webSocketService)
                .broadCastLobbyNotifications(eq(gameId), any());

        Lobby lobby = new Lobby();
        lobby.setLobbyId(gameId);
        lobby.addUsers(1L);
        lobby.addUsers(2L);
        GameSession session = gameService.startGame(lobby);
        assertThat(session.getGameId()).isEqualTo(gameId);

        long waitSeconds = TimerService.TURN_TIMEOUT_SECONDS + 40;
        boolean fired = latch.await(waitSeconds, TimeUnit.SECONDS);
        assertThat(fired)
                .as("timeoutAction doveva essere eseguito entro %d secondi", waitSeconds)
                .isTrue();

        verify(webSocketService, timeout(500).times(1))
                .broadCastLobbyNotifications(eq(gameId), any(MoveActionDTO.class));
    }
}

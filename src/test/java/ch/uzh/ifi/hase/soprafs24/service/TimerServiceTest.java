package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.game.gameDTO.TimeLeftDTO;
import ch.uzh.ifi.hase.soprafs24.timer.TimerStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TimerServiceTest {

    private TimerService timerService;
    private DummyStrategy playStrategy;
    private DummyStrategy choiceStrategy;

    @BeforeEach
    void setup() {
        playStrategy = new DummyStrategy(1);
        choiceStrategy = new DummyStrategy(1);
        timerService = new TimerService(
                Mockito.mock(GameService.class),
                playStrategy,
                choiceStrategy,
                Mockito.mock(WebSocketService.class));
    }

    @Test
    void testScheduleAndGetRemainingSeconds() {
        long gameId = 100L;
        assertEquals(0, timerService.getRemainingSeconds(gameId, playStrategy));
        timerService.schedule(gameId, playStrategy, null);
        long rem = timerService.getRemainingSeconds(gameId, playStrategy);
        assertTrue(rem > 0 && rem <= playStrategy.getTimeoutSeconds());
    }

    @Test
    void testCancelRemovesScheduledTask() {
        long gameId = 101L;
        timerService.schedule(gameId, playStrategy, null);
        assertTrue(timerService.getRemainingSeconds(gameId, playStrategy) > 0);
        timerService.cancel(gameId, playStrategy);
        assertEquals(0, timerService.getRemainingSeconds(gameId, playStrategy));
    }

    @Test
    void testTimeoutExecutionForPlayStrategy() throws InterruptedException {
        long gameId = 102L;
        CountDownLatch latch = playStrategy.getLatch();
        timerService.schedule(gameId, playStrategy, 999L);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testCancelPreventsExecution() throws InterruptedException {
        long gameId = 103L;
        CountDownLatch latch = choiceStrategy.getLatch();
        timerService.schedule(gameId, choiceStrategy, 777L);
        timerService.cancel(gameId, choiceStrategy);
        assertFalse(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    void testMultipleStrategyIndependent() {
        long gameId = 200L;
        timerService.schedule(gameId, playStrategy, null);
        timerService.schedule(gameId, choiceStrategy, 555L);
        assertTrue(timerService.getRemainingSeconds(gameId, playStrategy) > 0);
        assertTrue(timerService.getRemainingSeconds(gameId, choiceStrategy) > 0);
    }

    @Test
    void testCancelOnlyOneStrategy() {
        long gameId = 201L;
        timerService.schedule(gameId, playStrategy, null);
        timerService.schedule(gameId, choiceStrategy, 888L);
        timerService.cancel(gameId, playStrategy);
        assertEquals(0, timerService.getRemainingSeconds(gameId, playStrategy));
        assertTrue(timerService.getRemainingSeconds(gameId, choiceStrategy) > 0);
    }

    @Test
    void testRescheduleSameStrategyCancelsOld() {
        long gameId = 203L;
        timerService.schedule(gameId, playStrategy, null);
        long first = timerService.getRemainingSeconds(gameId, playStrategy);
        timerService.schedule(gameId, playStrategy, null);
        long second = timerService.getRemainingSeconds(gameId, playStrategy);
        assertTrue(second <= playStrategy.getTimeoutSeconds());
        assertTrue(second > 0);
    }

    @Test
    void testGetRemainingSecondsUnknown() {
        assertEquals(0, timerService.getRemainingSeconds(999L, playStrategy));
        assertEquals(0, timerService.getRemainingSeconds(999L, choiceStrategy));
    }

    private static class DummyStrategy implements TimerStrategy {
        private final long timeoutSeconds;
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicInteger count = new AtomicInteger(0);

        DummyStrategy(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        @Override
        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        @Override
        public void onTimeout(Long gameId, Long forUserId) {
            count.incrementAndGet();
            latch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        public int getCount() {
            return count.get();
        }
    }

    @Test
    void testTimeLeftDTOGettersAndSetters() {
        Long initialGameId = 1L;
        long initialRemaining = 5L;
        String initialMessage = "Time to Play";
        TimeLeftDTO dto = new TimeLeftDTO(initialGameId, initialRemaining, initialMessage);

        assertEquals(initialGameId, dto.getGameId());
        assertEquals(initialRemaining, dto.getRemainingSeconds());
        assertEquals(initialMessage, dto.getMessage());

        Long newGameId = 2L;
        long newRemaining = 10L;
        String newMessage = "Time to Choose";
        dto.setGameId(newGameId);
        dto.setRemainingSeconds(newRemaining);
        dto.setMessage(newMessage);

        assertEquals(newGameId, dto.getGameId());
        assertEquals(newRemaining, dto.getRemainingSeconds());
        assertEquals(newMessage, dto.getMessage());
    }

    @Test
    void testGetRemainingSecondsExpiresImmediatelyWhenTimeoutZero() {
        TimerStrategy zeroStrategy = new TimerStrategy() {
            @Override
            public long getTimeoutSeconds() {
                return 0L;
            }

            @Override
            public void onTimeout(Long gameId, Long forUserId) {
                /* no-op */ }
        };
        var svc = new TimerService(
                Mockito.mock(GameService.class),
                zeroStrategy,
                choiceStrategy,
                Mockito.mock(WebSocketService.class));

        long gameId = 999L;
        svc.schedule(gameId, zeroStrategy, null);
        long rem = svc.getRemainingSeconds(gameId, zeroStrategy);

        assertEquals(0, rem);
    }

    @Test
    void testGetPlayAndChoiceStrategyReturnInjected() {
        assertSame(playStrategy, timerService.getPlayStrategy());
        assertSame(choiceStrategy, timerService.getChoiceStrategy());
    }

}
package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.timer.TimerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class TimerService {
    private static final Logger log = LoggerFactory.getLogger(TimerService.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final TimerStrategy playStrategy;
    private final TimerStrategy choiceStrategy;

    private final Map<Long, Map<TimerStrategy, ScheduledFuture<?>>> tasks = new ConcurrentHashMap<>();
    private final Map<Long, Map<TimerStrategy, Long>> expirations = new ConcurrentHashMap<>();

    @Autowired
    public TimerService(
            @Lazy GameService gameService,
            @Qualifier("playTimerStrategy") TimerStrategy playStrategy,
            @Qualifier("choiceTimerStrategy") TimerStrategy choiceStrategy,
            WebSocketService webSocketService) {
        this.playStrategy = playStrategy;
        this.choiceStrategy = choiceStrategy;
    }

    public void schedule(Long gameId, TimerStrategy strategy, Long forUserId) {
        // cancel any existing
        cancel(gameId, strategy);
        long timeout = strategy.getTimeoutSeconds();
        ScheduledFuture<?> f = scheduler.schedule(
                () -> strategy.onTimeout(gameId, forUserId),
                timeout, TimeUnit.SECONDS);
        tasks.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(strategy, f);
        expirations.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                .put(strategy, System.currentTimeMillis() + timeout * 1000);
        log.debug("Timer scheduled for game: {}", gameId);
    }

    public void cancel(Long gameId, TimerStrategy strategy) {
        var taskMap = tasks.get(gameId);
        if (taskMap != null && taskMap.containsKey(strategy)) {
            taskMap.get(strategy).cancel(true);
            taskMap.remove(strategy);
            log.debug("Schedule cancelled for game: {}", gameId);
        }

        var expMap = expirations.get(gameId);
        if (expMap != null && expMap.containsKey(strategy)) {
            expMap.remove(strategy);
            if (expMap.isEmpty()) {
                expirations.remove(gameId);
                log.debug("Timer discarded for game: {}", gameId);
            }
        }
    }

    public long getRemainingSeconds(Long gameId, TimerStrategy strategy) {
        var map = expirations.get(gameId);
        if (map != null && map.containsKey(strategy)) {
            long millis = map.get(strategy) - System.currentTimeMillis();
            if (millis <= 0) {
                return 0;
            }

            return (millis + 999) / 1000;
        }
        return 0;
    }

    // compatibility for existing tests
    public TimerStrategy getPlayStrategy() {
        return playStrategy;
    }

    public TimerStrategy getChoiceStrategy() {
        return choiceStrategy;
    }
}

package ch.uzh.ifi.hase.soprafs24.timer;

/**
 * Interface for the two strategies of timeout.
 */
public interface TimerStrategy {
    /**
     * @return length of timer (seconds)
     */
    long getTimeoutSeconds();

    /**
     * Invoked at timeout: contains only specific fallback.
     *
     * @param gameId    id game
     * @param forUserId optional: id of player (only for ChoiceTimer)
     */
    void onTimeout(Long gameId, Long forUserId);
}

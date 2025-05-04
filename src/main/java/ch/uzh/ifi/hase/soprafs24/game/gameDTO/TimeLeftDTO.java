package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class TimeLeftDTO {
    private Long gameId;
    private long remainingSeconds;

    public TimeLeftDTO() {
    }

    public TimeLeftDTO(Long gameId, long remainingSeconds) {
        this.gameId = gameId;
        this.remainingSeconds = remainingSeconds;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }
}

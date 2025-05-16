package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class TimeLeftDTO {
    private Long gameId;
    private long remainingSeconds;
    private String message; // “Time to Play” o “Time to Choose”


    public TimeLeftDTO(Long gameId, long remainingSeconds, String message) {
        this.gameId = gameId;
        this.remainingSeconds = remainingSeconds;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

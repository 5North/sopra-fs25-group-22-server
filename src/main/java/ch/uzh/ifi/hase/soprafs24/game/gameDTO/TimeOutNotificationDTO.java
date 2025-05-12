package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class TimeOutNotificationDTO {
    private Long timedOutPlayerId;
    private String message;

    public TimeOutNotificationDTO() {
    }

    public TimeOutNotificationDTO(Long timedOutPlayerId, String message) {
        this.timedOutPlayerId = timedOutPlayerId;
        this.message = message;
    }

    public Long getTimedOutPlayerId() {
        return timedOutPlayerId;
    }

    public void setTimedOutPlayerId(Long timedOutPlayerId) {
        this.timedOutPlayerId = timedOutPlayerId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

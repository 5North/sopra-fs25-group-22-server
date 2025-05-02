package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

/**
 * DTO for server response upon a forfeit quit.
 */
public class QuitGameResultDTO {
    private Long userId;
    private String outcome; // "WON" or "LOST"
    private String message;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

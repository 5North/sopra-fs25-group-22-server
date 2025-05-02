package ch.uzh.ifi.hase.soprafs24.websocket.DTO;

public class UserNotificationDTO {

    private boolean success;

    private String message;

    public void setSuccess(boolean success) {this.success = success;}

    public boolean getSuccess() {return success;}

    public void setMessage(String message) {this.message = message;}

    public String getMessage() {return message;}
}

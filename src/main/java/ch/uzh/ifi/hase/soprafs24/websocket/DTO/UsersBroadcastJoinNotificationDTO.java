package ch.uzh.ifi.hase.soprafs24.websocket.DTO;

public class UsersBroadcastJoinNotificationDTO {

    private String username;

    private String status;

    private wsLobbyDTO lobby;

    public void setUsername(String username) {this.username = username;}

    public String getUsername() {return username;}

    public void setStatus(String status) {this.status = status;}

    public String getStatus() {return status;}

    public void setLobby(wsLobbyDTO lobby) {this.lobby = lobby;}

    public wsLobbyDTO getLobby() {return lobby;}
}

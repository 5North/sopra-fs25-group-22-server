package ch.uzh.ifi.hase.soprafs24.websocket.DTO;

import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;

public class UsersBroadcastJoinNotificationDTO {

    private String username;

    private String status;

    private LobbyDTO lobby;

    public void setUsername(String username) {this.username = username;}

    public String getUsername() {return username;}

    public void setStatus(String status) {this.status = status;}

    public String getStatus() {return status;}

    public void setLobby(LobbyDTO lobby) {
        this.lobby = lobby;
    }

    public LobbyDTO getLobby() {
        return lobby;
    }
}

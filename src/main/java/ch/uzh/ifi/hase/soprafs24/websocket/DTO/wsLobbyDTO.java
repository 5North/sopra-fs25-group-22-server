package ch.uzh.ifi.hase.soprafs24.websocket.DTO;

import java.util.List;

public class wsLobbyDTO {

    private Long lobbyId;

    private Long hostId;

    private List<Long> usersIds;

    public Long getLobbyId() {return lobbyId;}

    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

    public Long getHostId() {return hostId;}

    public void setHostId(Long hostId) {this.hostId = hostId;}

    public List<Long> getUsersIds() {return usersIds;}

    public void setUsersIds(List<Long> usersIds) {this.usersIds = usersIds;}
}

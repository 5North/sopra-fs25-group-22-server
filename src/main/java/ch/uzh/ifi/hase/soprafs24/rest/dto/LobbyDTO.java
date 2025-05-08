package ch.uzh.ifi.hase.soprafs24.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LobbyDTO {

    private Long lobbyId;

    private Long hostId;

    private List<Long> usersIds;

    private List<Long> rematchersIds;

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public List<Long> getUsersIds() {
        return usersIds;
    }

    public void setUsersIds(List<Long> usersIds) {
        this.usersIds = usersIds;
    }

    public List<Long> getRematchersIds() {
        return rematchersIds;
    }

    public void setRematchersIds(List<Long> rematchersIds) {
        this.rematchersIds = rematchersIds;
    }
}

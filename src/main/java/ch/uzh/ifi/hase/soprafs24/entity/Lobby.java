package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Fetch;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "LOBBY")
public class Lobby implements Serializable {

    @Id
    private Long lobbyId;

    @OneToOne(mappedBy = "lobby")
    private User user;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<Long> users = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<Long> rematchers = new ArrayList<>();

    @Transient
    private transient GameSession gameSession;

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public List<Long> getUsers() {
        return users;
    }

    public void addUser(Long userId) {
        users.add(userId);
    }

    public boolean removeUser(Long userId) {
        return users.remove(userId);
    }

    public List<Long> getRematchers() {
        return rematchers;
    }

    public void addRematcher(Long userId) {
        rematchers.add(userId);
    }

    public boolean removeRematcher(Long userId) {
        return rematchers.remove(userId);
    }

    public void clearRematchers() {
        rematchers.clear();
    }

    public GameSession getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }
}

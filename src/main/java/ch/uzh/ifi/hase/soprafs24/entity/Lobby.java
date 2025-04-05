package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "LOBBY")
public class Lobby implements Serializable {

    @Id
    private Long lobbyId;

    @OneToOne(mappedBy = "lobby", cascade = CascadeType.ALL)
    private User user;


    @ElementCollection
    private List<Long> users = new ArrayList<>();

    @Embedded()
    private GameSession gameSession;

    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

    public Long getLobbyId() {return lobbyId;}

    public void setUser(User user) {this.user = user;}

    public User getUser() {return user;}

    public List<Long> getUsers() {return users;}

    public void addUsers(long userId) {users.add(userId);}

    public boolean removeUsers(long userId) {return users.remove(userId);}

    public GameSession getGameSession() {return gameSession;}

    public void setGameSession(GameSession gameSession) {this.gameSession = gameSession;}
}

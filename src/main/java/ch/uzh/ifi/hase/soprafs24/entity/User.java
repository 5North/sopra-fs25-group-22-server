package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "USER")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserStatus status;

  @Column(nullable = false)
  private Integer winCount = 0;

  @Column(nullable = false)
  private Integer lossCount = 0;

  @Column
  private Long lobbyJoined;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "lobby_id")
  private Lobby lobby;

  @Column(nullable = false)
  private Integer tieCount = 0;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public Integer getWinCount() {
    return winCount;
  }

  public void setWinCount(Integer winCount) {
    this.winCount = winCount;
  }

  public Integer getLossCount() {
    return lossCount;
  }

  public void setLossCount(Integer lossCount) {
    this.lossCount = lossCount;
  }

  public void setLobby(Lobby lobby) {
    this.lobby = lobby;
  }

  public Lobby getLobby() {
    return lobby;
  }

  public Integer getTieCount() {
    return tieCount;
  }

  public void setTieCount(Integer tieCount) {
    this.tieCount = tieCount;
  }

  public void incrementWinCount() {
    this.winCount++;
  }

  public void incrementLossCount() {
    this.lossCount++;
  }

  public void incrementTieCount() {
    this.tieCount++;
  }

  public Long getLobbyJoined() {
    return lobbyJoined;
  }

  public void setLobbyJoined(Long lobbyJoined) {
    this.lobbyJoined = lobbyJoined;
  }
}

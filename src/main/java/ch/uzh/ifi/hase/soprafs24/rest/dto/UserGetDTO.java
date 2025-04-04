package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

public class UserGetDTO {

  private Long id;
  private String username;
  private UserStatus status;
  private Integer winCount;
  private Integer lossCount;

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
}

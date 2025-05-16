package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }


  public User getUserById(Long id) {
      Optional<User> user = this.userRepository.findById(id);
      if (user.isEmpty()) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + id + " not found");
      }
      return user.get();
  }

  public String loginUser(User userInput) {
    User userByUsername = userRepository.findByUsername(userInput.getUsername());
    if (userByUsername == null) {
        log.debug("failed to login user {}", userInput.getUsername());
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid username or password");
    } else if (!userByUsername.getPassword().equals(userInput.getPassword())) {
        log.debug("failed to login user {}: wrong password", userInput.getUsername());
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid password");
    }
    userByUsername.setStatus(UserStatus.ONLINE);
    userByUsername.setToken(UUID.randomUUID().toString());
    log.info("user with id {} logged in", userByUsername.getId());
    return userByUsername.getToken();
  }

  public void logoutUser(User authUser) {
    authUser.setStatus(UserStatus.OFFLINE);
    log.info("user with id {} logged out", authUser.getId());
  }

  public User authorizeUser(String token) throws ResponseStatusException {
    User userByToken = userRepository.findByToken(token);
    if (userByToken == null || userByToken.getStatus() != UserStatus.ONLINE) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    return userByToken;
  }

    public void isUserAllowedToGetLobby(User user, Lobby lobby) {
        Long lobbyId = lobby.getLobbyId();
        if (!isUserInLobby(user, lobbyId) && !isUserLobbyOwner(user, lobbyId)) {
            String msg = String.format("User with id %d is not in the lobby", lobbyId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
        }
    }

    private boolean isUserLobbyOwner(User user, Long lobbyId) {
        Lobby userLobby = user.getLobby();
        return userLobby != null && Objects.equals(userLobby.getLobbyId(), lobbyId);
    }

    private boolean isUserInLobby(User user, Long lobbyId) {
        Long userLobbyJoined = user.getLobbyJoined();
        return userLobbyJoined != null && Objects.equals(userLobbyJoined, lobbyId);
    }

  public User createUser(User newUser) {
    if (userRepository.findByUsername(newUser.getUsername()) != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "User creation failed because username already exists");
    }
    String token = UUID.randomUUID().toString();
    newUser.setToken(token);
    newUser.setStatus(UserStatus.ONLINE);
    newUser.setWinCount(0);
    newUser.setLossCount(0);
    newUser.setTieCount(0);

    User createdUser = userRepository.save(newUser);
    log.info("user with id {} created", newUser.getId());

    return createdUser;
  }

    public Lobby getLobby(Long userId) {
        User user = getUserById(userId);
        if (user.getLobby() == null) {
            String msg = String.format("User with id %s does not have a lobby", userId);
            log.info("User with id {}: lobby not found", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
        }
        return user.getLobby();
    }

    public User checkIfUserExists(long userId) throws NotFoundException {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
          String msg = "User with id " + userId + " does not exist";
          throw new NotFoundException(msg);
      }
        return user.get();
    }

}

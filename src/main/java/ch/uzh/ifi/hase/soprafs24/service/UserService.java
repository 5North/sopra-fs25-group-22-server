package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

  public String loginUser(String username, String password) {
    User userByUsername = userRepository.findByUsername(username);
    if (userByUsername == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid username or password");
    } else if (!userByUsername.getPassword().equals(password)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid password");
    }
    userByUsername.setStatus(UserStatus.ONLINE);
    userByUsername.setToken(UUID.randomUUID().toString());
    return userByUsername.getToken();
  }

  public User createUser(User newUser) {
    newUser.setId(1L);
    newUser.setToken("placeholder-token");
    newUser.setStatus(UserStatus.ONLINE);
    newUser.setWinCount(0);
    newUser.setLossCount(0);
    return newUser;
  }


  public void logoutUser(User authUser) {
      authUser.setStatus(UserStatus.OFFLINE);
  }

  public User authorizeUser(String token) throws ResponseStatusException {
      User userByToken = userRepository.findByToken(token);
      if (userByToken == null || userByToken.getStatus() != UserStatus.ONLINE) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
      }
      return userByToken;
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    User userByName = userRepository.findByName(userToBeCreated.getName());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null && userByName != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format(baseErrorMessage, "username and the name", "are"));
    } else if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
    } else if (userByName != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
    }
  }


}

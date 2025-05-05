package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
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
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid username or password");
    } else if (!userByUsername.getPassword().equals(userInput.getPassword())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid password");
    }
    userByUsername.setStatus(UserStatus.ONLINE);
    userByUsername.setToken(UUID.randomUUID().toString());
    return userByUsername.getToken();
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
    return userRepository.save(newUser);
  }

    // TODO eventually refactor better to avoid duplicate code
    public User checkIfUserExists(long userId) throws NotFoundException {
      Optional<User> user = userRepository.findById(userId);

      if(user.isEmpty()) {
          String msg = "User with id " + userId + " does not exist";
          throw new NotFoundException(msg);
      }
      return user.get();
    }

}

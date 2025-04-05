package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

  @Qualifier("userRepository")
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @BeforeEach
  public void setup() {
    userRepository.deleteAll();
  }

  // # 1
  @Test
  public void createUser_validInputs_success() {
    // given:
    assertNull(userRepository.findByUsername("testUsername"));

    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");

    // when:
    User createdUser = userService.createUser(testUser);

    // then:
    assertNotNull(createdUser.getId(), "The id of the user was not created");
    assertEquals("testUsername", createdUser.getUsername());
    assertNotNull(createdUser.getToken(), "The token of the user was not created");
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
    assertEquals(0, createdUser.getWinCount());
    assertEquals(0, createdUser.getLossCount());
    assertNull(createdUser.getLobby(), "The lobby is not null created");
  }

  // # 1
  @Test
  public void createUser_duplicateUsername_throwsException() {
    // given:
    User testUser = new User();
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    User createdUser = userService.createUser(testUser);
    assertNotNull(createdUser);

    // when:
    User duplicateUser = new User();
    duplicateUser.setUsername("testUsername");
    duplicateUser.setPassword("anotherPassword");

    // then:
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      userService.createUser(duplicateUser);
    });
    assertEquals(409, exception.getStatus().value());
    assertEquals("User creation failed because username already exists", exception.getReason());
  }
}

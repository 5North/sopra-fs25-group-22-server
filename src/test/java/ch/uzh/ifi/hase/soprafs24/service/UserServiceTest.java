package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
    testUser.setToken("dummy-token");
    testUser.setStatus(UserStatus.OFFLINE);
    testUser.setWinCount(0);
    testUser.setLossCount(0);
    testUser.setTieCount(0);

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  // # 18
  @Test
  public void loginUser_validCredentials_success() {
    // given -> a first user has already been created
    userService.createUser(testUser);
    String previousToken = testUser.getToken();

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // call userService method
    userService.loginUser(testUser);

    assertEquals(UserStatus.ONLINE, testUser.getStatus());
    assertNotEquals(testUser.getToken(), previousToken);
  }

  // # 19
  @Test
  public void loginUser_notValidCredentials_throwsException() {

    // given -> a user who tries to log in with wrong credentials
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

    // then -> attempt to log in with invalid credentials -> check that an error
    // is thrown
    assertThrows(
        ResponseStatusException.class, () -> userService
            .loginUser(testUser));
  }

  // # 19
  @Test
  public void loginUser_notValidPassword_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // given an userInput with the same username but the wrong password
    User userInput = new User();
    userInput.setUsername("testUsername");
    userInput.setPassword("another-password");

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt login with invalid password -> check that an error
    // is thrown
    assertThrows(
        ResponseStatusException.class, () -> userService
            .loginUser(userInput));
  }

  // # 7
  @Test
  public void createUser_existingUser_throwsException() {
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

    User duplicateUser = new User();
    duplicateUser.setUsername("testUsername");
    duplicateUser.setPassword("anotherPassword");

    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      userService.createUser(duplicateUser);
    });

    assertEquals(409, exception.getStatus().value());
    assertEquals("User creation failed because username already exists", exception.getReason());
  }

  // #9
  @Test
  public void createUser_validInputs_success() {
    Mockito.when(userRepository.findByUsername(testUser.getUsername())).thenReturn(null);

    User createdUser = userService.createUser(testUser);

    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
    assertNotNull(createdUser);
    assertNotNull(createdUser.getId());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
    assertEquals(0, createdUser.getWinCount());
    assertEquals(0, createdUser.getLossCount());
    assertEquals(0, createdUser.getTieCount());
  }

  @Test
  public void authorizeUser_ValidAuth_success() {
    // given -> a first user has already been created
    userService.createUser(testUser);
    testUser.setStatus(UserStatus.ONLINE);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByToken(Mockito.any())).thenReturn(testUser);

    // call userService method
    User authUser = userService.authorizeUser("a valid token");
    assertNotNull(authUser);
  }

  @Test
  public void authorizeUser_NotValidToken_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByToken(Mockito.any())).thenReturn(null);

    // then -> attempt logout with invalid token -> check that an error
    // is thrown
    assertThrows(
        ResponseStatusException.class, () -> userService
            .authorizeUser("not a valid token"));
  }

  @Test
  public void authorizeUser_InvalidatedSessionToken_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);
    testUser.setStatus(UserStatus.OFFLINE);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByToken(Mockito.any())).thenReturn(testUser);

    // then -> attempt logout with invalid token -> check that an error
    // is thrown
    assertThrows(
        ResponseStatusException.class, () -> userService
            .authorizeUser("a valid token"));
  }

  @Test
  public void logoutUser_success() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByToken(Mockito.any())).thenReturn(testUser);

    // call userService method
    userService.logoutUser(testUser);
    assertEquals(UserStatus.OFFLINE, testUser.getStatus());
  }

  @Test
  public void testIncrementStatistics() {
    userService.createUser(testUser);

    testUser.incrementWinCount();
    testUser.incrementLossCount();
    testUser.incrementTieCount();

    assertEquals(1, testUser.getWinCount(), "Win count should be incremented to 1.");
    assertEquals(1, testUser.getLossCount(), "Loss count should be incremented to 1.");
    assertEquals(1, testUser.getTieCount(), "Tie count should be incremented to 1.");
  }

  @Test
  public void checkIfUserExists_success() throws NotFoundException {
    // given -> a first user has already been created
    userService.createUser(testUser);
    Optional<User> optionalUser = Optional.of(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findById(Mockito.anyLong())).thenReturn(optionalUser);

    assertDoesNotThrow(() -> userService.checkIfUserExists(testUser.getId()));

  }

  @Test
  public void checkIfUserExists_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);
    Optional<User> optionalEmptyUser = Optional.empty();

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findById(Mockito.anyLong())).thenReturn(optionalEmptyUser);

    assertThrows(
        NotFoundException.class, () -> userService.checkIfUserExists(1L));
  }

  @Test
  public void testGetUsersReturnsEmpty() {
    Mockito.when(userRepository.findAll()).thenReturn(new ArrayList<>());

    List<User> users = userService.getUsers();

    assertNotNull(users, "Returned list should not be null");
    assertEquals(0, users.size(), "The user list should be empty.");

    Mockito.verify(userRepository, Mockito.times(1)).findAll();
  }

  @Test
  public void testGetUsersSuccess() {
    List<User> usersFromRepo = new ArrayList<>();
    usersFromRepo.add(testUser);

    Mockito.when(userRepository.findAll()).thenReturn(usersFromRepo);

    List<User> returnedUsers = userService.getUsers();

    assertNotNull(returnedUsers, "Returned list should not be null");
    assertEquals(1, returnedUsers.size(), "The user list size should be 1.");
    assertEquals(testUser, returnedUsers.get(0), "The returned user should match the test user.");

    Mockito.verify(userRepository, Mockito.times(1)).findAll();
  }

  @Test
    void getUserById_success() {
      // given -> a first user has already been created
      userService.createUser(testUser);
      Mockito.when(userRepository.findById(Mockito.anyLong())).thenReturn(Optional.of(testUser));

      User returnedUser = userService.getUserById(testUser.getId());
      assertNotNull(returnedUser, "Returned user should not be null");
      assertEquals(testUser, returnedUser, "The returned user should match the test user.");
  }

    @Test
    void getUserById_throwsException() {
        // given
        // no user


        //when
        Mockito.when(userRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());
        assertThrows(
                ResponseStatusException.class, () -> userService.getUserById(1L));
    }
}

package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

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
                        .authorizeUser("not a valid token")
        );
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
                        .authorizeUser("a valid token")
        );
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

}

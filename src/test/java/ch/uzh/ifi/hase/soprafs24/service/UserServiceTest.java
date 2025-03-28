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

  // @Test
  // public void createUser_validInputs_success() {
  // // when -> any object is being save in the userRepository -> return the dummy
  // // testUser
  // User createdUser = userService.createUser(testUser);

  // // then
  // Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

  // assertEquals(testUser.getId(), createdUser.getId());
  // assertEquals(testUser.getName(), createdUser.getName());
  // assertEquals(testUser.getUsername(), createdUser.getUsername());
  // assertNotNull(createdUser.getToken());
  // assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
  // }

  // @Test
  // public void createUser_duplicateName_throwsException() {
  // // given -> a first user has already been created
  // userService.createUser(testUser);

  // // when -> setup additional mocks for UserRepository
  // Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
  // Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(null);

  // // then -> attempt to create second user with same user -> check that an
  // error
  // // is thrown
  // assertThrows(ResponseStatusException.class, () ->
  // userService.createUser(testUser));
  // }

  // @Test
  // public void createUser_duplicateInputs_throwsException() {
  // // given -> a first user has already been created
  // userService.createUser(testUser);

  // // when -> setup additional mocks for UserRepository
  // Mockito.when(userRepository.findByName(Mockito.any())).thenReturn(testUser);
  // Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

  // // then -> attempt to create second user with same user -> check that an
  // error
  // // is thrown
  // assertThrows(ResponseStatusException.class, () ->
  // userService.createUser(testUser));
  // }

  @Test
  public void loginUser_validCredentials_success() {
    // given -> a first user has already been created
    userService.createUser(testUser);
    String previousToken = testUser.getToken();

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // call userService method
    userService.loginUser(testUser.getUsername(), testUser.getPassword());

    assertEquals(UserStatus.ONLINE, testUser.getStatus());
    assertNotEquals(testUser.getToken(), previousToken);
  }

  @Test
  public void loginUser_notValidCredentials_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt to login with invalid credentials -> check that an error
    // is thrown
    assertThrows(
        ResponseStatusException.class, () -> userService
            .loginUser("not_correct_username", "not_correct_password"));
  }

  @Test
  public void loginUser_notValidPassword_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt login with invalid password -> check that an error
    // is thrown
    assertThrows(
        ResponseStatusException.class, () -> userService
            .loginUser(testUser.getUsername(), "not_correct_password"));
  }

  // # 7
  @Test
  public void createUser_existingUser_throwsException() {
    // Arrange: Simula che esista già un utente con username "testUsername".
    // Non aggiungiamo ulteriori campi come token o winCount, in quanto non sono
    // necessari per questo test.
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

    // Creiamo un nuovo utente con lo stesso username, ma con i soli campi minimi.
    User duplicateUser = new User();
    duplicateUser.setUsername("testUsername");
    duplicateUser.setPassword("anotherPassword");

    // Act & Assert: L'invocazione di createUser deve lanciare una
    // ResponseStatusException.
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      userService.createUser(duplicateUser);
    });

    // Verifica che lo status sia 409 (CONFLICT) e il messaggio sia quello atteso.
    assertEquals(409, exception.getStatus().value());
    assertEquals("User creation failed because username already exists", exception.getReason());
  }

}

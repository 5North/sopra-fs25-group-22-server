package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @InjectMocks
    private LobbyService lobbyService;

    @InjectMocks
    private Lobby testLobby;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private User testUser;

    @BeforeEach
    void setup() {
        // given
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

        testLobby = new Lobby();
        testLobby.setLobbyId(1000L);

        // when -> any object is being saved in the userRepository -> return the dummy
        // testLobby
        Mockito.when(lobbyRepository.save(Mockito.any())).thenReturn(testLobby);
    }

    @Test
    void createLobby_validInputs_success() {

        Lobby createdLobby = lobbyService.createLobby(testUser);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        assertNotNull(createdLobby);
        assertNotNull(createdLobby.getLobbyId());
        assertNotNull(testUser.getLobby());
    }

    @Test
    void createLobby_alreadyExists_success() {

        Lobby createdLobby = lobbyService.createLobby(testUser);
        Lobby createdLobby2 = lobbyService.createLobby(testUser);

        assertEquals(createdLobby.getLobbyId(), createdLobby2.getLobbyId());
    }

    @Test
    void isFull_true() {

        testLobby.addUsers(1L);
        testLobby.addUsers(2L);
        testLobby.addUsers(3L);
        testLobby.addUsers(4L);
        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);

        assertTrue(lobbyService.lobbyIsFull(testLobby.getLobbyId()));
    }

    @Test
    public void isFull_notEmpty_false() {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);

        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);

        assertFalse(lobbyService.lobbyIsFull(testLobby.getLobbyId()));
    }

    @Test
    public void isFull_Empty_false() {
        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);

        assertFalse(lobbyService.lobbyIsFull(testLobby.getLobbyId()));
    }

    @Test
    public void joinLobby_validInputs_success() throws NotFoundException {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);

        Optional<User> optionaluser = Optional.of(testUser);

        ArrayList<Long> users = new ArrayList<>();
        users.add(1L);
        users.add(2L);
        users.add(3L);

        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);
        Mockito.when(userRepository.findById(Mockito.anyLong())).thenReturn(optionaluser);

        lobbyService.joinLobby(testLobby.getLobbyId(), 3L);

        assertEquals(testLobby.getUsers(), users);
    }

    @Test
    public void joinLobby_NonExistentUser_noSuccess() throws NotFoundException {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);

        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);
        doThrow(new NotFoundException("error")).when(userService).checkIfUserExists(Mockito.anyLong());

        assertThrows(
                NotFoundException.class, () -> lobbyService
                        .joinLobby(testLobby.getLobbyId(), 3L));

    }

    @Test
    public void joinLobby_full_noSuccess() {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);
        testLobby.addUsers(3L);
        testLobby.addUsers(4L);

        Long lobbyId = testLobby.getLobbyId();

        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);

        assertThrows(
                IllegalStateException.class, () -> lobbyService
                        .joinLobby(lobbyId, 5L));

    }

    @Test
    public void leaveLobby_validInputs_success() throws Exception {
        testLobby.addUsers(1L);

        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);

        lobbyService.leaveLobby(testLobby.getLobbyId(), 1L);

        assertEquals(new ArrayList<>(), testLobby.getUsers());

    }

    @Test
    public void leaveLobby_NonExistentUser_noSuccess()  {
        testLobby.addUsers(1L);

        Long lobbyId = testLobby.getLobbyId();
        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findByLobbyId(Mockito.any())).thenReturn(testLobby);

        assertThrows(
                NoSuchElementException.class, () -> lobbyService
                        .leaveLobby(lobbyId, 2L));

    }

    @Test
    public void generateId_success() {
        Long id = lobbyService.generateId();
        assertNotNull(id);
    }

    @Test
    public void getLobbyById_notExists_throwsNoSuchElementException() {
        when(lobbyRepository.findByLobbyId(123L)).thenReturn(null);

        assertThrows(NoSuchElementException.class,
                () -> lobbyService.getLobbyById(123L));
    }

    @Test
    public void checkIfLobbyExists_exists_doesNotThrow() {
        when(lobbyRepository.findByLobbyId(testLobby.getLobbyId())).thenReturn(testLobby);

        assertDoesNotThrow(() -> lobbyService.checkIfLobbyExists(testLobby.getLobbyId()));
    }

    @Test
    public void checkIfLobbyExists_notExists_throwsNotFoundException() {
        when(lobbyRepository.findByLobbyId(999L)).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> lobbyService.checkIfLobbyExists(999L));
    }

}

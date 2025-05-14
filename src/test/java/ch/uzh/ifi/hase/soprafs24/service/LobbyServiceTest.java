package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

 class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @InjectMocks
    private LobbyService lobbyService;

    @Mock
    private Lobby testLobby;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
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
    void createLobby_alreadyExists_throwsException() {
        // given
        assertNull(testUser.getLobby());

        lobbyService.createLobby(testUser);
        assertNotNull(testUser.getLobby());

        // assert
        assertThrows(ResponseStatusException.class, () -> lobbyService.createLobby(testUser));
    }

    @Test
    void createLobby_userAlreadyInALobby_conflict() {

        testUser.setLobbyJoined(1111L);
        String expectedMsg = "409 CONFLICT \"User with id " + testUser.getId() + " already joined lobby "
                + testUser.getLobbyJoined() + "\"";
        Exception expectedException = assertThrows(
                ResponseStatusException.class, () -> lobbyService
                        .createLobby(testUser));
        assertEquals(expectedException.getMessage(), expectedMsg);

    }

    @Test
    void isFull_true() throws NotFoundException {

        testLobby.addUsers(1L);
        testLobby.addUsers(2L);
        testLobby.addUsers(3L);
        testLobby.addUsers(4L);
        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertTrue(lobbyService.lobbyIsFull(testLobby.getLobbyId()));
    }

    @Test
     void isFull_notEmpty_false() throws NotFoundException {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);

        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertFalse(lobbyService.lobbyIsFull(testLobby.getLobbyId()));
    }

    @Test
     void isFull_Empty_false() throws NotFoundException {
        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertFalse(lobbyService.lobbyIsFull(testLobby.getLobbyId()));
    }

    @Test
    void joinLobby_validInputs_success() throws NotFoundException {
        // given
        assertNull(testUser.getLobbyJoined());

        testLobby.addUsers(1L);
        testLobby.addUsers(2L);
        testLobby.adddRematchers(1L);
        testLobby.adddRematchers(2L);

        ArrayList<Long> users = new ArrayList<>();
        users.add(1L);
        users.add(2L);
        users.add(3L);

        ArrayList<Long> rematchers = new ArrayList<>();
        rematchers.add(1L);
        rematchers.add(2L);
        rematchers.add(3L);

        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        Mockito.when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);

        lobbyService.joinLobby(testLobby.getLobbyId(), 3L);

        assertEquals(users, testLobby.getUsers());
        assertEquals(rematchers, testLobby.getRematchers());
        assertEquals(testUser.getLobbyJoined(), testLobby.getLobbyId());
    }

    @Test
     void joinLobby_NonExistentUser_noSuccess() throws NotFoundException {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);

        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        doThrow(new NotFoundException("error")).when(userService).checkIfUserExists(Mockito.anyLong());

        assertThrows(
                NotFoundException.class, () -> lobbyService
                        .joinLobby(testLobby.getLobbyId(), 3L));

    }

    @Test
    void joinLobby_full_noSuccess() throws NotFoundException {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);
        testLobby.addUsers(3L);
        testLobby.addUsers(4L);

        Long lobbyId = testLobby.getLobbyId();

        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);

        assertEquals(4, testLobby.getUsers().size());
        assertThrows(
                IllegalStateException.class, () -> lobbyService
                        .joinLobby(lobbyId, 5L));

    }

    @Test
    void joinLobby_full_UserAlreadyIn_doesNotThrowException() throws NotFoundException {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);
        testLobby.addUsers(3L);
        testLobby.addUsers(4L);

        Long lobbyId = testLobby.getLobbyId();

        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);
        assertEquals(4, testLobby.getUsers().size());
        assertDoesNotThrow(()->lobbyService.joinLobby(lobbyId, 4L));

    }

    @Test
    void joinLobby_UserAlreadyIn_doesNothing() throws NotFoundException {
        testLobby.addUsers(1L);
        testLobby.addUsers(2L);

        Long lobbyId = testLobby.getLobbyId();

        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);
        lobbyService.joinLobby(lobbyId, 2L);
        assertEquals(2, testLobby.getUsers().size());

    }

    @Test
    void leaveLobby_validInputs_success() throws Exception {
        testLobby.addUsers(1L);
        testLobby.adddRematchers(1L);
        testUser.setLobbyJoined(testLobby.getLobbyId());
        assertNotNull(testUser.getLobbyJoined());

        // when -> setup additional mocks for userService and lobbyRepo
        Mockito.when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        lobbyService.leaveLobby(testLobby.getLobbyId(), 1L);

        assertEquals(new ArrayList<>(), testLobby.getUsers());
        assertEquals(new ArrayList<>(), testLobby.getRematchers());
        assertNull(testUser.getLobbyJoined());

    }

    @Test
    void leaveLobby_validInputs_deleteLobby_success() throws Exception {
        // given
        testLobby.addUsers(1L);
        testLobby.setUser(testUser);
        testUser.setLobby(testLobby);
        assertNotNull(testUser.getLobby());

        // when -> setup additional mocks for userService and lobbyRepo
        Mockito.when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        lobbyService.leaveLobby(testLobby.getLobbyId(), testUser.getId());

        assertNull(testUser.getLobby());

    }

    @Test
    void leaveLobby_noLobbyFound_deleteLobby_throwsException() throws Exception {
        testLobby.addUsers(1L);

        // when -> setup additional mocks for userService and lobbyRepo
        Mockito.when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> lobbyService.leaveLobby(testLobby.getLobbyId(), 1L));

    }

    @Test
    void leaveLobby_NonExistentUser_noSuccess() {
        testLobby.addUsers(1L);

        Long lobbyId = testLobby.getLobbyId();
        // when -> setup additional mocks for LobbyRepository and userRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertThrows(
                NoSuchElementException.class, () -> lobbyService
                        .leaveLobby(lobbyId, 2L));

    }

    @Test
     void generateId_success() {
        Long id = lobbyService.generateId();
        assertNotNull(id);
    }

    @Test
    void getLobbyById_notExists_throwsNoTFoundException() {
        when(lobbyRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> lobbyService.checkIfLobbyExists(123L));
    }

    @Test
     void checkIfLobbyExists_exists_doesNotThrow() throws NotFoundException {
        when(lobbyRepository.findById(testLobby.getLobbyId())).thenReturn(Optional.of(testLobby));

        assertDoesNotThrow(() -> lobbyService.checkIfLobbyExists(testLobby.getLobbyId()));
        assertEquals(lobbyService.checkIfLobbyExists(testLobby.getLobbyId()), testLobby);
    }

    @Test
     void checkIfLobbyExists_notExists_throwsNotFoundException() {
        when(lobbyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> lobbyService.checkIfLobbyExists(999L));
    }

    // TODO delete if useless
    @Test
    void deleteLobby_validInputs_success() throws NotFoundException {
        testLobby.setUser(testUser);
        testUser.setLobby(testLobby);
        // when
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        Mockito.when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);

        lobbyService.deleteLobby(testLobby.getLobbyId());
        // assert
        assertNull(testUser.getLobby());
    }

    // TODO delete if useless
    @Test
    void deleteLobby_noLobbyFound_throwsException() throws NotFoundException {
        testLobby.setUser(testUser);
        testUser.setLobby(testLobby);
        // when
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(userService.checkIfUserExists(Mockito.anyLong())).thenReturn(testUser);

        assertThrows(NotFoundException.class,
                () -> lobbyService.deleteLobby(testLobby.getLobbyId()));

    }

    @Test
    void isRematchFull_true() throws NotFoundException {

        testLobby.adddRematchers(1L);
        testLobby.adddRematchers(2L);
        testLobby.adddRematchers(3L);
        testLobby.adddRematchers(4L);
        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertTrue(lobbyService.rematchIsFull(testLobby.getLobbyId()));
    }

    @Test
    void isRematch_notEmpty_false() throws NotFoundException {
        testLobby.adddRematchers(1L);
        testLobby.adddRematchers(2L);

        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertFalse(lobbyService.rematchIsFull(testLobby.getLobbyId()));
    }

    @Test
    void isRematch_Empty_false() throws NotFoundException {
        // when -> setup additional mocks for LobbyRepository
        Mockito.when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));

        assertFalse(lobbyService.rematchIsFull(testLobby.getLobbyId()));
    }

    @Test
    void resetRematch_resets() {
        // given
        testLobby.adddRematchers(1L);
        testLobby.adddRematchers(2L);
        List<Long> emptyLst = new ArrayList<>();
        assertNotEquals(emptyLst, testLobby.getRematchers());

        // when
        when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        testLobby.clearRematchers();

        assertEquals(emptyLst, testLobby.getRematchers());
    }

    @Test
    void addRematcher_validInputs_success() throws NotFoundException {
        // given
        testLobby.addUsers(1L);
        assertEquals(new ArrayList<>(), testLobby.getRematchers());
        List<Long> rematchers = new ArrayList<>();
        rematchers.add(testUser.getId());

        // when
        when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
        // assert
        lobbyService.addRematcher(testLobby.getLobbyId(), testUser.getId());
        assertEquals(rematchers, testLobby.getRematchers());
    }

    @Test
    void addRematcher_UserNotExists_throwsNotFoundException() throws NotFoundException {

        // when
        when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.empty());
        when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
        // assert
        assertThrows(NotFoundException.class,
                () -> lobbyService.addRematcher(testLobby.getLobbyId(), testUser.getId()));
    }

    @Test
    void addRematcher_Lobby_NotExists_throwsNotFoundException() throws NotFoundException {

        // when
        when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
        when(userService.checkIfUserExists(anyLong())).thenThrow(NotFoundException.class);

        // assert
        assertThrows(NotFoundException.class,
                () -> lobbyService.addRematcher(testLobby.getLobbyId(), testUser.getId()));
    }

     @Test
     void addRematcher_userAlreadyInRematch() throws NotFoundException {
        testLobby.adddRematchers(testUser.getId());
        testLobby.addUsers(testUser.getId());
        List<Long> expectedRematchers = new ArrayList<>();
        expectedRematchers.add(testUser.getId());
        assertEquals(expectedRematchers, testLobby.getRematchers());

         // when
         when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
         when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);

         lobbyService.addRematcher(testLobby.getLobbyId(), testUser.getId());
         assertEquals(expectedRematchers, testLobby.getRematchers());
     }

     @Test
     void addRematcher_userNotInLobby() throws NotFoundException {

         // when
         when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
         when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);

         // assert
         assertThrows(NoSuchElementException.class,
                 () -> lobbyService.addRematcher(testLobby.getLobbyId(), testUser.getId()));
     }

     @Test
     void isUserInLobby_true() throws NotFoundException {
        testLobby.addUsers(testUser.getId());

         // when
         when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
         when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);

        assertEquals(Boolean.TRUE, lobbyService.isUserInLobby(testLobby.getLobbyId(), testUser.getId()));
     }

     @Test
     void isUserInLobby_false() throws NotFoundException {
        assertEquals(new ArrayList<>(), testLobby.getUsers());
         // when
         when(lobbyRepository.findById(Mockito.any())).thenReturn(Optional.of(testLobby));
         when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
        assertEquals(Boolean.FALSE, lobbyService.isUserInLobby(testLobby.getLobbyId(), testUser.getId()));
     }

    @Test
    void getLobbyByParticipantId_validInputs_success() throws NotFoundException {
        // given
        testUser.setLobbyJoined(1000L);
        // when
        when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
        Long lobbyId = lobbyService.getLobbyIdByParticipantId(testUser.getId());
        // then
        assertEquals(1000L, lobbyId);
    }

    @Test
    void getLobbyByParticipantId_nonExistingUser_throwsException() throws NotFoundException {
        // given
        testUser.setLobbyJoined(1000L);
        // when
        when(userService.checkIfUserExists(anyLong())).thenThrow(new NotFoundException(("error")));

        //assert
        assertThrows(NotFoundException.class,
                () -> lobbyService.getLobbyIdByParticipantId(testUser.getId()));
    }

    @Test
    void wsLobbyDTO_rematchersIdsGetterAndSetter() {
        LobbyDTO dto = new LobbyDTO();
        List<Long> rematchers = Arrays.asList(10L, 20L, 30L);
        dto.setRematchersIds(rematchers);
        assertEquals(rematchers, dto.getRematchersIds());
    }

    @Test
    void testGetLobbyByIdSuccess() throws NotFoundException {
        when(lobbyRepository.findById(testLobby.getLobbyId()))
                .thenReturn(Optional.of(testLobby));

        Lobby found = lobbyService.checkIfLobbyExists(testLobby.getLobbyId());
        assertSame(testLobby, found);
    }

    @Test
    void testResetRematchViaService() throws NotFoundException {
        testLobby.adddRematchers(1L);
        testLobby.adddRematchers(2L);
        when(lobbyRepository.findById(testLobby.getLobbyId()))
                .thenReturn(Optional.of(testLobby));

        lobbyService.resetRematch(testLobby.getLobbyId());

        assertTrue(testLobby.getRematchers().isEmpty());
    }

    @Test
    void testDeleteLobbySwallowEmptyResult() throws NotFoundException {
        when(lobbyRepository.findById(testLobby.getLobbyId()))
                .thenReturn(Optional.of(testLobby));
        doThrow(new EmptyResultDataAccessException(1))
                .when(lobbyRepository).deleteById(testLobby.getLobbyId());

        assertDoesNotThrow(() -> lobbyService.deleteLobby(testLobby.getLobbyId()));
    }

    @Test
    void testGenerateIdWithCollision() throws Exception {
        Random stubRandom = new Random() {
            @Override
            public int nextInt(int bound) {
                return 123;
            }
        };

        Field rndField = LobbyService.class.getDeclaredField("random");
        rndField.setAccessible(true);
        rndField.set(lobbyService, stubRandom);

        when(lobbyRepository.findById(1123L))
                .thenReturn(Optional.of(testLobby))
                .thenReturn(Optional.empty());

        Long id = lobbyService.generateId();
        assertEquals(1123L, id);
    }

    @Test
    void testJoinLobby_UserAlreadyJoinedAnotherLobby_Throws() throws NotFoundException {
        testUser.setLobbyJoined(999L);
        when(lobbyRepository.findById(testLobby.getLobbyId()))
                .thenReturn(Optional.of(testLobby));
        when(userService.checkIfUserExists(testUser.getId()))
                .thenReturn(testUser);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> lobbyService.joinLobby(testLobby.getLobbyId(), testUser.getId()));
        assertTrue(ex.getMessage().contains("already joined lobby"));
    }

    @Test
    void testSetAndGetGameSession() {
        Lobby lobby = new Lobby();
        GameSession session = new GameSession(123L, new ArrayList<>());

        lobby.setGameSession(session);
        assertNotNull(lobby.getGameSession());
        assertSame(session, lobby.getGameSession());
    }

}

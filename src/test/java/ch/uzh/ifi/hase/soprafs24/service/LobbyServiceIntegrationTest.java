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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;


/**
 * Test class for the LobbyResource REST resource.
 *
 * @see UserService
 */
@WebAppConfiguration
@SpringBootTest
public class LobbyServiceIntegrationTest {

    @Qualifier("lobbyRepository")
    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private LobbyService lobbyService;

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @InjectMocks
    private User testUser;

    @InjectMocks
    private User testUser2;

    @SpyBean
    private LobbyService spyLobbyService;

    @BeforeEach
    void setup() {
        // given
        MockitoAnnotations.openMocks(this);

        lobbyRepository.deleteAll();
        testUser.setId(1L);
        testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("testToken");
        testUser.setLossCount(0);
        testUser.setTieCount(0);
        testUser.setWinCount(0);

        testUser = userRepository.save(testUser);

        testUser2.setId(2L);
        testUser2.setUsername("testUsername2");
        testUser2.setPassword("testPassword");
        testUser2.setStatus(UserStatus.ONLINE);
        testUser2.setToken("testToken2");
        testUser2.setLossCount(0);
        testUser2.setTieCount(0);
        testUser2.setWinCount(0);

        testUser2 = userRepository.save(testUser2);

        // MOcking only a single method of the test class
        spyLobbyService = spy(lobbyService);
        Mockito.doReturn(1000L).when(spyLobbyService).generateId();

    }

    @Test
    void createLobby_success() {
        // given:
        assertTrue(lobbyRepository.findById(1000L).isEmpty());

        // when:
        Lobby createdLobby = lobbyService.createLobby(testUser);

        // then:
        assertNotNull(createdLobby.getLobbyId(), "The id of the lobby was not created");
        assertEquals(createdLobby.getLobbyId(), testUser.getLobby().getLobbyId(), "The correct db association was not created");
        assertEquals(createdLobby.getUser().getId(),testUser.getId(), "The correct db association was not created");
        assertNotNull(lobbyRepository.findById(1000L).get(), "The id of the lobby was not created");
    }

    @Test
    void joinLobby_success() throws NotFoundException {
        // given
        List<Long> emptyList = new ArrayList<>();
        Lobby createdLobby = lobbyService.createLobby(testUser);
        assertEquals(emptyList, createdLobby.getUsers());

        // when:
        lobbyService.joinLobby(createdLobby.getLobbyId(), testUser.getId());
        List<Long> userIds = new ArrayList<>();
        userIds.add(1L);

        Optional<Lobby> updatedLobby = lobbyRepository.findById(1000L);
        assertTrue(updatedLobby.isPresent(), "The id of the lobby was not created");
        assertEquals(userIds.toString(), updatedLobby.get().getUsers().toString());
    }

    @Test
    void leaveLobby_success() throws NotFoundException {
        // given
        List<Long> emptyList = new ArrayList<>();
        Lobby createdLobby = lobbyService.createLobby(testUser);
        assertEquals(emptyList, createdLobby.getUsers());

        // when:
        createdLobby.addUsers(1L);
        createdLobby.addUsers(2L);
        lobbyRepository.save(createdLobby);
        lobbyRepository.flush();

        List<Long> userIds = new ArrayList<>();
        userIds.add(testUser.getId());
        userIds.add(testUser2.getId());
        assertEquals(createdLobby.getUsers().toString(), userIds.toString());

        lobbyService.leaveLobby(createdLobby.getLobbyId(), testUser2.getId());

        userIds.remove(testUser2.getId());
        Optional<Lobby> updatedLobby = lobbyRepository.findById(1000L);
        assertTrue(updatedLobby.isPresent(), "The id of the lobby was not created");
        assertEquals(updatedLobby.get().getUsers().toString(), userIds.toString());

    }

    @Test
    void leaveLobby_andDestroyLobby_success() {
        // given
        List<Long> emptyList = new ArrayList<>();
        Lobby createdLobby = lobbyService.createLobby(testUser);
        assertEquals(emptyList, createdLobby.getUsers());

        createdLobby.addUsers(testUser.getId());
        lobbyRepository.save(createdLobby);
        lobbyRepository.flush();

        assertEquals(testUser.getLobby(), createdLobby);

        // when:
        assertThrows(IllegalStateException.class, () -> lobbyService.leaveLobby(createdLobby.getLobbyId(), testUser.getId()));
        assertTrue(lobbyRepository.findById(createdLobby.getLobbyId()).isEmpty());
    }

}

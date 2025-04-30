package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


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

    @Autowired
    private UserService userService;

    @InjectMocks
    private User testUser;

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
    }

    @Test
    void createLobby_success() {
        // given:
        assertNull(lobbyRepository.findByLobbyId(1000L));

        // when:
        Lobby createdLobby = lobbyService.createLobby(testUser);

        // then:
        assertNotNull(createdLobby.getLobbyId(), "The id of the lobby was not created");
        assertEquals(createdLobby.getLobbyId(), testUser.getLobby().getLobbyId(), "The correct db association was not created");
        assertEquals(createdLobby.getUser().getId(),testUser.getId(), "The correct db association was not created");
    }

    @Test
    void joinLobby_success() {
        // given
        List<Long> emptyList = new ArrayList<>();
        Lobby createdLobby = lobbyService.createLobby(testUser);
        assertEquals(emptyList, createdLobby.getUsers());

        // when:
        createdLobby.addUsers(1L);
        createdLobby.addUsers(2L);
        List<Long> userIds = new ArrayList<>();
        userIds.add(1L);
        userIds.add(2L);
        assertEquals(createdLobby.getUsers().toString(), userIds.toString());
    }

}

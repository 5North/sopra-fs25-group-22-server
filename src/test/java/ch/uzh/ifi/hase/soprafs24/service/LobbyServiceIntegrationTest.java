package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

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

    @BeforeEach
    public void setup() {
        lobbyRepository.deleteAll();
    }

    @Test
    public void createLobby_success() {
        // given:
        assertNull(lobbyRepository.findByLobbyId(1000L));

        User testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("testToken");
        testUser = userRepository.save(testUser);

        // when:
        Lobby createdLobby = lobbyService.createLobby(testUser);

        // then:
        assertNotNull(createdLobby.getLobbyId(), "The id of the lobby was not created");
        assertEquals(createdLobby.getLobbyId(), testUser.getLobby().getLobbyId(), "The correct db association was not created");
        assertEquals(createdLobby.getUser().getId(),testUser.getId(), "The correct db association was not created");
    }

}

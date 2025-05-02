package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

/**
 * Integration tests for LobbyService
 */
@WebAppConfiguration
@SpringBootTest
public class LobbyServiceIntegrationTest {

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private LobbyService lobbyService;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void setup() {
        // 1) Pulisci le tabelle USER e LOBBY
        lobbyRepository.deleteAll();
        userRepository.deleteAll();
        lobbyRepository.flush();
        userRepository.flush();

        // 2) Crea e salva due utenti distinti con token unici
        testUser = new User();
        testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("token1");
        testUser.setWinCount(0);
        testUser.setLossCount(0);
        testUser.setTieCount(0);
        testUser = userRepository.save(testUser);

        testUser2 = new User();
        testUser2.setUsername("testUsername2");
        testUser2.setPassword("testPassword2");
        testUser2.setStatus(UserStatus.ONLINE);
        testUser2.setToken("token2");
        testUser2.setWinCount(0);
        testUser2.setLossCount(0);
        testUser2.setTieCount(0);
        testUser2 = userRepository.save(testUser2);

        // 3) Stub generateId() su 1000
        doReturn(1000L).when(lobbyService).generateId();
    }

    @Test
    void createLobby_success() {
        // dato che non c'è ancora lobby 1000
        assertTrue(lobbyRepository.findById(1000L).isEmpty());

        // quando
        Lobby created = lobbyService.createLobby(testUser);
        Long id = created.getLobbyId();

        // allora
        assertNotNull(id);
        assertEquals(1000L, id);
        assertEquals(id, testUser.getLobby().getLobbyId());
        assertTrue(lobbyRepository.existsById(id));
    }

    @Test
    void joinLobby_success() throws NotFoundException {
        Lobby created = lobbyService.createLobby(testUser);
        Long id = created.getLobbyId();

        // inizialmente vuota
        assertTrue(created.getUsers().isEmpty());

        // join
        lobbyService.joinLobby(id, testUser.getId());

        // controllo
        Optional<Lobby> updated = lobbyRepository.findById(id);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getUsers().size());
        assertEquals(testUser.getId(), updated.get().getUsers().get(0));
    }

    @Test
    void leaveLobby_success() throws NotFoundException {
        Lobby created = lobbyService.createLobby(testUser);
        Long id = created.getLobbyId();

        created.addUsers(testUser.getId());
        created.addUsers(testUser2.getId());
        lobbyRepository.save(created);
        lobbyRepository.flush();

        // testUser2 leaves
        lobbyService.leaveLobby(id, testUser2.getId());

        // controllo
        Optional<Lobby> updated = lobbyRepository.findById(id);
        assertTrue(updated.isPresent());
        assertEquals(1, updated.get().getUsers().size());
        assertEquals(testUser.getId(), updated.get().getUsers().get(0));
    }

    @Test
    void leaveLobby_andDestroyLobby_success() throws NotFoundException {
        Lobby created = lobbyService.createLobby(testUser);
        Long id = created.getLobbyId();

        created.addUsers(testUser.getId());
        lobbyRepository.save(created);
        lobbyRepository.flush();

        // host leaves: deve sparire la lobby
        lobbyService.leaveLobby(id, testUser.getId());

        assertFalse(lobbyRepository.existsById(id), "Lobby non è stata cancellata");
    }
}

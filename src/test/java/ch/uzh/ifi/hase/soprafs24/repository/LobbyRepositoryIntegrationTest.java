package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class LobbyRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LobbyRepository lobbyRepository;

    // 1
    @Test
    public void findByLobbyId_success() {
        // given
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1000L);
        entityManager.persist(lobby);
        entityManager.flush();

        // when
        Lobby found = lobbyRepository.findByLobbyId(lobby.getLobbyId());

        // then
        assertNotNull(found.getLobbyId());
        assertEquals(found.getLobbyId(), lobby.getLobbyId());
    }

    @Test
    public void findByLobbyId_returnsNull() {
        // given
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1000L);

        entityManager.persist(lobby);
        entityManager.flush();

        // when
        Lobby found = lobbyRepository.findByLobbyId(1111L);

        // then
        assertNull(found);
    }
}

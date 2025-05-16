package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
 class LobbyRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LobbyRepository lobbyRepository;

    // 1
    @Test
     void findById_success() {
        // given
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1000L);
        entityManager.persist(lobby);
        entityManager.flush();

        // when
        Optional<Lobby> found = lobbyRepository.findById(lobby.getLobbyId());

        // then
        assertFalse(found.isEmpty());
        assertEquals(found.get().getLobbyId(), lobby.getLobbyId());
    }

    @Test
     void findById_returnsNull() {
        // given
        Lobby lobby = new Lobby();
        lobby.setLobbyId(1000L);

        entityManager.persist(lobby);
        entityManager.flush();

        // when
        Optional<Lobby> found = lobbyRepository.findById(1111L);

        // then
        assertTrue(found.isEmpty());
    }
}

package ch.uzh.ifi.hase.soprafs24.service;


import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.NoSuchElementException;
import java.util.Random;


/**
 * Lobby Service
 * This class is the "worker" and responsible for all functionality related to
 * the lobby
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */

@Service
@Transactional
public class LobbyService {
    private final Logger log = LoggerFactory.getLogger(LobbyService.class);

    private final LobbyRepository lobbyRepository;
    private final UserService userService;

    @Autowired
    public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository, UserService userService) {
        this.lobbyRepository = lobbyRepository;
        this.userService = userService;
    }

    public Lobby createLobby(User user){
        Lobby newLobby = new Lobby();
        newLobby.setLobbyId(generateId());
        user.setLobby(newLobby);
        newLobby.setUser(user);
        return lobbyRepository.save(newLobby);
    }

    public void joinLobby(long lobbyId, long userId) throws NotFoundException {
        checkIfLobbyExists(lobbyId);
        userService.checkIfUserExists(userId);
        Lobby lobby = lobbyRepository.findByLobbyId(lobbyId);
        lobby.addUsers(userId);
        this.lobbyIsFull(lobbyId);
    }

    public void leaveLobby(long lobbyId, long userId) throws NotFoundException {
        checkIfLobbyExists(lobbyId);
        userService.checkIfUserExists(userId);
        Lobby lobby = lobbyRepository.findByLobbyId(lobbyId);
        if(!lobby.removeUsers(userId)){
            String msg = "User " + userId + " is not part of lobby " + lobby.getLobbyId();
            throw new NoSuchElementException(msg);
        }
    }

    public boolean lobbyIsFull(long lobbyId) {
        Lobby lobby = lobbyRepository.findByLobbyId(lobbyId);
        return lobby.getUsers().size() == 4;
    }

    public void checkIfLobbyExists(long lobbyId) throws NotFoundException {
        if (lobbyRepository.findByLobbyId(lobbyId) == null) {
            String msg = "No lobby with id " + lobbyId + " found";
            throw new NotFoundException(msg);
        }

    }

    public long generateId() {
        Random random = new Random();
        long randomId;
        do {
            randomId = random.nextInt(9000) + 1000;
        } while (lobbyRepository.findByLobbyId(randomId) != null);
        return randomId;
    }
}

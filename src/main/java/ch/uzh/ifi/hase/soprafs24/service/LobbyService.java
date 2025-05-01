package ch.uzh.ifi.hase.soprafs24.service;


import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import javassist.NotFoundException;
import org.springframework.web.server.ResponseStatusException;


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
    private final UserRepository userRepository;
    private final UserService userService;
    private final Random random;

    @Autowired
    public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository, UserRepository userRepository, UserService userService) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.random = new Random();
    }


    public Lobby createLobby(User user) {
        if (user.getLobby() != null) {
            return user.getLobby();
        } else if (user.getLobbyJoined() != null) {
            String msg = "User with id " + user.getId() + " already joined lobby " +  user.getLobbyJoined();
            throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
        }
        Lobby newLobby = new Lobby();
        newLobby.setLobbyId(generateId());
        user.setLobby(newLobby);
        newLobby.setUser(user);
        userRepository.save(user);
        userRepository.flush();
        return newLobby;
    }


    public void joinLobby(Long lobbyId, Long userId) throws NotFoundException {
        Lobby lobby = checkIfLobbyExists(lobbyId);
        User user = userService.checkIfUserExists(userId);

        // check if lobby is already full
        if (lobbyIsFull(lobbyId)) {
            String msg = "The lobby is already full";
            throw new IllegalStateException(msg);
        }

        // check if user is already in another lobby
        if (user.getLobbyJoined() != null) {
            String msg = "User with id " + user.getId() + " already joined lobby " + user.getLobbyJoined();
            throw new IllegalStateException(msg);
        }

        // check if user is already in the lobby
        if (!lobby.getUsers().contains(userId)) {
            lobby.addUsers(userId);
            user.setLobbyJoined(lobbyId);
        }
        userRepository.save(user);
        userRepository.flush();
    }

    public void leaveLobby(Long lobbyId, Long userId) throws NotFoundException {
        User user = userService.checkIfUserExists(userId);
        Lobby lobby = checkIfLobbyExists(lobbyId);
        if(!lobby.removeUsers(userId)){
            String msg = "User " + userId + " is not part of lobby " + lobby.getLobbyId();
            throw new NoSuchElementException(msg);
        }
        user.setLobbyJoined(null);
        if (user.getLobby()!=null && user.getLobby().getLobbyId().equals(lobbyId)) {
            deleteLobby(lobbyId, userId);
            log.info("Lobby with id {} has been deleted", lobbyId);
            throw new IllegalStateException("Lobby with id " + lobbyId + " has been deleted");
        }
        userRepository.save(user);
        userRepository.flush();
    }

    public Lobby getLobbyById(Long lobbyId) throws NotFoundException {
        return checkIfLobbyExists(lobbyId);
    }

    public boolean lobbyIsFull(Long lobbyId) throws NotFoundException {
        Optional<Lobby> lobby = lobbyRepository.findById(lobbyId);
        if (lobby.isEmpty()) {
            String msg = "No lobby with id " + lobbyId;
            throw new NotFoundException(msg);
        }
        return lobby.get().getUsers().size() >= 4;
    }

    public Lobby checkIfLobbyExists(Long lobbyId) throws NotFoundException {
        Optional<Lobby> lobby = lobbyRepository.findById(lobbyId);
        if (lobby.isEmpty()) {
            String msg = "No lobby with id " + lobbyId + " found";
            throw new NotFoundException(msg);
        }
        return lobby.get();
    }

    public void deleteLobby(Long lobbyId, Long userId) throws NotFoundException {
        Lobby lobby = checkIfLobbyExists(lobbyId);
        User user = userService.checkIfUserExists(userId);
        lobby.setUser(null);
        user.setLobby(null);
        userRepository.save(user);
        lobbyRepository.delete(lobby);
        userRepository.flush();
    }

    public Long generateId() {
        Long randomId;
        do {
            randomId = (long) (random.nextInt(9000) + 1000);
        } while (lobbyRepository.findById(randomId).isPresent());
        return randomId;
    }
}

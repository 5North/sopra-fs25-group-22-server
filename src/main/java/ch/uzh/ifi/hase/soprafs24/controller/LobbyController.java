package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyPostResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Lobby Controller
 * This class is responsible for handling all REST request that are related to
 * the lobby.
 * The controller will receive the request and delegate the execution to the
 * LobbyService and finally return the result.
 */
@RestController
public class LobbyController {
    private final LobbyService lobbyService;
    private final UserService userService;

    LobbyController(LobbyService lobbyService, UserService userService) {
        this.lobbyService = lobbyService;
        this.userService = userService;
    }

    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyPostResponseDTO createLobby(@RequestHeader String token) {

        // authenticate user
        User authUser = userService.authorizeUser(token);

        Lobby createdLobby = lobbyService.createLobby(authUser);

        return DTOMapper.INSTANCE.convertEntityToLobbyPostResponseDTO(createdLobby);
    }
    // TODO remove code
   /* @GetMapping("/lobbies/{lobbyId}")

    public ResponseEntity<String> joinLobby(@RequestHeader String token, @PathVariable String lobbyId) {

        // authenticate user
        userService.authorizeUser(token);

        lobbyService.joinLobby(lobbyId);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Upgrade", "websocket");
        responseHeaders.set("Connection", "Upgrade");
        return new ResponseEntity<>(responseHeaders, HttpStatus.SWITCHING_PROTOCOLS);
    }

    @DeleteMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void exitLobby(@RequestHeader String token, @PathVariable String lobbyId) {

        // authenticate user
        User authUser = userService.authorizeUser(token);

        lobbyService.leaveLobby(lobbyId, authUser);
    }*/
}

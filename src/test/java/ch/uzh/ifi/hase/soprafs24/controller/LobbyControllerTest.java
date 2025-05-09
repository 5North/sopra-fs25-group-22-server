package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * LobbyControllerTest
 * This is a WebMvcTest which allows to test the LobbyController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the LobbyController works.
 */

@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LobbyService lobbyService;

    @MockBean
    private UserService userService;

    @Test
    void createLobby_lobbyCreated() throws Exception {
        //given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1225L);
        lobby.setUser(user);


        given(userService.authorizeUser("placeholder-token")).willReturn(user);
        given(lobbyService.createLobby(Mockito.any(User.class))).willReturn(lobby);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .contentType(MediaType.APPLICATION_JSON).header("Token", "placeholder-token");

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lobbyId", is(lobby.getLobbyId().intValue())));
    }

    @Test
    public void createLobby_invalidAuth_lobbyNotCreated() throws Exception {
        //given

       Lobby lobby = new Lobby();
       lobby.setLobbyId(1225L);

        given(userService.authorizeUser("placeholder-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

       // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/lobbies")
               .contentType(MediaType.APPLICATION_JSON).header("Token", "placeholder-token");

       // then
      mockMvc.perform(postRequest)
              .andExpect(status().isUnauthorized());
    }

    @Test
    void createLobby_conflict() throws Exception {
        //given

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1225L);

        given(lobbyService.createLobby(Mockito.any())).willThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .contentType(MediaType.APPLICATION_JSON).header("Token", "placeholder-token");

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void createLobby_lobbyAlreadyExists_throwsException() throws Exception {
        //given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);


        given(userService.authorizeUser("placeholder-token")).willReturn(user);
        given(lobbyService.createLobby(Mockito.any(User.class))).willThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .contentType(MediaType.APPLICATION_JSON).header("Token", "placeholder-token");

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void getLobby_Success() throws Exception {
        //given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1111L);
        lobby.setUser(user);


        given(userService.authorizeUser("placeholder-token")).willReturn(user);
        given(userService.getLobby(userId)).willReturn(lobby);

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder getRequest = get("/lobbies?userId=" + userId)
                .contentType(MediaType.APPLICATION_JSON).header("Token", "placeholder-token");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lobbyId", is(lobby.getLobbyId().intValue())))
                .andExpect(jsonPath("$.hostId", is(lobby.getUser().getId().intValue())))
                .andExpect(jsonPath("$.usersIds", is(lobby.getUsers())));
    }

    @Test
    void getLobby_lobbyNotFound_throwsException() throws Exception {
        //given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1111L);
        lobby.setUser(user);


        given(userService.authorizeUser("placeholder-token")).willReturn(user);
        given(userService.getLobby(userId)).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder getRequest = get("/lobbies?userId=" + userId)
                .contentType(MediaType.APPLICATION_JSON).header("Token", "placeholder-token");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void getLobby_notAllowed_throwsException() throws Exception {
        //given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(1111L);
        lobby.setUser(user);


        given(userService.authorizeUser("placeholder-token")).willReturn(user);
        given(userService.getLobby(userId)).willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        // when/then -> do the request + validate the result
        MockHttpServletRequestBuilder getRequest = get("/lobbies?userId=" + userId)
                .contentType(MediaType.APPLICATION_JSON).header("Token", "placeholder-token");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isForbidden());
    }
}
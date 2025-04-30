package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        // # 1
        @Test
        void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
                // given
                User user = new User();
                user.setUsername("testUser");
                user.setStatus(UserStatus.OFFLINE);
                user.setToken("dummyToken");
                user.setWinCount(0);
                user.setLossCount(0);
                user.setTieCount(0);

                List<User> allUsers = Collections.singletonList(user);
                given(userService.authorizeUser(user.getToken())).willReturn(user);
                given(userService.getUsers()).willReturn(allUsers);

                // when
                MockHttpServletRequestBuilder getRequest = get("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Token", "dummyToken");

                // then
                mockMvc.perform(getRequest)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(1)))
                        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())))
                        .andExpect(jsonPath("$[0].winCount", is(user.getWinCount())))
                        .andExpect(jsonPath("$[0].lossCount", is(user.getLossCount())))
                        .andExpect(jsonPath("$[0].tieCount", is(user.getTieCount())));
        }

    @Test
    void givenUser_whenGetUser_thenReturnUser() throws Exception {
        // given
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);

        given(userService.authorizeUser(user.getToken())).willReturn(user);

        given(userService.getUserById(userId)).willReturn(user);

        // when
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Token", "ImAToken");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(user.getStatus().toString())))
                .andExpect(jsonPath("$.winCount", is(user.getWinCount())))
                .andExpect(jsonPath("$.lossCount", is(user.getLossCount())))
                .andExpect(jsonPath("$.tieCount", is(user.getTieCount())));
    }

    @Test
    void givenUser_whenGetUser_NotFoundException() throws Exception {
        // given
        // no user
        Long userId = 2L;

        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);

        given(userService.authorizeUser(user.getToken())).willReturn(user);

        given(userService.getUserById(userId)).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        // when
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Token", "ImAToken");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }
    @Test
    void givenUser_whenGetUser_unauthorized() throws Exception {
        // given
        // no user
        Long userId = 2L;

        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("IamAToken");
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);

        given(userService.authorizeUser("ImNotAToken"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // when
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Token", "ImNotAToken");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

        @Test
        public void logout_User_validAuth_userLoggedOut() throws Exception {
                // given
                User user = new User();
                user.setId(1L);
                user.setUsername("testUsername");
                user.setToken("hdbhdd7-dfjdhs923-wddhejkh3");
                user.setStatus(UserStatus.ONLINE);

                given(userService.authorizeUser("hdbhdd7-dfjdhs923-wddhejkh3")).willReturn(user);

                // when/then -> do the request + validate the result
                MockHttpServletRequestBuilder postRequest = post("/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Token", "hdbhdd7-dfjdhs923-wddhejkh3");

                // then
                mockMvc.perform(postRequest)
                                .andExpect(status().isNoContent());
        }

        @Test
        public void logout_User_notValidAuth_userNotLoggedOut() throws Exception {
                // given
                User user = new User();
                user.setId(1L);
                user.setUsername("testUsername");
                user.setToken("hdbhdd7-dfjdhs923-wddhejkh3");
                user.setStatus(UserStatus.ONLINE);

                given(userService.authorizeUser(""))
                                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

                // when/then -> do the request + validate the result
                MockHttpServletRequestBuilder postRequest = post("/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Token", "");

                // then
                mockMvc.perform(postRequest)
                                .andExpect(status().isUnauthorized());
        }

        // # 6
        @Test
        public void createUser_validInput_userCreated() throws Exception {
                User user = new User();
                user.setId(1L);
                user.setUsername("testUsername");
                user.setPassword("testPassword");
                user.setToken("generated-token");
                user.setStatus(UserStatus.ONLINE);
                user.setWinCount(0);
                user.setLossCount(0);
                user.setTieCount(0);

                UserPostDTO userPostDTO = new UserPostDTO();
                userPostDTO.setUsername("testUsername");
                userPostDTO.setPassword("testPassword");

                given(userService.createUser(Mockito.any())).willReturn(user);

                MockHttpServletRequestBuilder postRequest = post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(userPostDTO));

                mockMvc.perform(postRequest)
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Token", "generated-token"))
                                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                                .andExpect(jsonPath("$.username", is(user.getUsername())))
                                .andExpect(jsonPath("$.status", is(user.getStatus().toString())))
                                .andExpect(jsonPath("$.winCount", is(user.getWinCount())))
                                .andExpect(jsonPath("$.lossCount", is(user.getLossCount())))
                                .andExpect(jsonPath("$.tieCount", is(user.getTieCount())));
        }

        // # 18
        @Test
        public void loginUser_validInput_userLoggedIn() throws Exception {
                // given
                User user = new User();
                user.setId(1L);
                user.setUsername("john_doe");
                user.setPassword("correct-horse-battery-staple");
                user.setToken("generated-session-token");
                user.setStatus(UserStatus.ONLINE);
                user.setWinCount(0);
                user.setLossCount(0);

                UserPostDTO userPostDTO = new UserPostDTO();
                userPostDTO.setUsername("john_doe");
                userPostDTO.setPassword("correct-horse-battery-staple");

                given(userService.loginUser(Mockito.any())).willReturn(user.getToken());

                // when/then -> do the request + validate the result
                MockHttpServletRequestBuilder postRequest = post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(userPostDTO));

                // then
                mockMvc.perform(postRequest)
                                .andExpect(status().isOk())
                                .andExpect(header().string("Token", "generated-session-token"));
        }

        // # 19
        @Test
        public void loginUser_notValidInput_userNotLoggedIn() throws Exception {
                // given
                User user = new User();
                user.setId(1L);
                user.setUsername("john_doe");
                user.setPassword("correct-horse-battery-staple");
                user.setToken("generated-session-token");
                user.setStatus(UserStatus.ONLINE);
                user.setWinCount(0);
                user.setLossCount(0);

                UserPostDTO userPostDTO = new UserPostDTO();
                userPostDTO.setUsername("john_doe");
                userPostDTO.setPassword("correct-horse-battery-staple");

                given(userService.loginUser(Mockito.any()))
                                .willThrow(
                                                new ResponseStatusException(
                                                                HttpStatus.FORBIDDEN, "Invalid username or password"));

                // when/then -> do the request + validate the result
                MockHttpServletRequestBuilder postRequest = post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(userPostDTO));

                // then
                mockMvc.perform(postRequest)
                                .andExpect(status().isForbidden())
                                .andExpect(content().string("403 FORBIDDEN \"Invalid username or password\""));
        }

        // # 8
        @Test
        public void createUser_validInput_returnsTokenInHeader() throws Exception {
                User user = new User();
                user.setId(1L);
                user.setUsername("testUsername");
                user.setPassword("testPassword");
                user.setToken("generated-session-token");
                user.setStatus(UserStatus.ONLINE);
                user.setWinCount(0);
                user.setLossCount(0);
                user.setTieCount(0);

                UserPostDTO userPostDTO = new UserPostDTO();
                userPostDTO.setUsername("testUsername");
                userPostDTO.setPassword("testPassword");

                given(userService.createUser(Mockito.any())).willReturn(user);

                MockHttpServletRequestBuilder postRequest = post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(userPostDTO));

                mockMvc.perform(postRequest)
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Token", "generated-session-token"))
                                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                                .andExpect(jsonPath("$.username", is(user.getUsername())))
                                .andExpect(jsonPath("$.status", is(user.getStatus().toString())))
                                .andExpect(jsonPath("$.winCount", is(user.getWinCount())))
                                .andExpect(jsonPath("$.lossCount", is(user.getLossCount())))
                                .andExpect(jsonPath("$.tieCount", is(user.getLossCount())));
        }

        /**
         * Helper Method to convert userPostDTO into a JSON string such that the input
         * can be processed
         * Input will look like this: {"name": "Test User", "username": "testUsername"}
         * 
         * @param object
         * @return string
         */
        private String asJsonString(final Object object) {
                try {
                        return new ObjectMapper().writeValueAsString(object);
                } catch (JsonProcessingException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        String.format("The request body could not be created.%s", e.toString()));
                }
        }
}
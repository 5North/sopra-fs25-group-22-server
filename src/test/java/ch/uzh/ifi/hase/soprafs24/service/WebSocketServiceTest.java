package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.BroadcastNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class WebSocketServiceTest {
    @InjectMocks
    private WebSocketService webSocketService;

    @Mock
    private UserService userService;

    @Mock
    private LobbyService lobbyService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

    }

    @Test
    void ConvertToDTOForBroadcastSuccess() throws NotFoundException {
        // given
        Long userId = 1L;
        User user = new User();
        user.setUsername("johnDoe");
        user.setId(userId);

        Lobby lobby = new Lobby();
        lobby.setUser(user);
        lobby.setLobbyId(1000L);
        lobby.addUsers(1L);
        lobby.addUsers(2L);

        LobbyDTO lobbyDTO = new LobbyDTO();
        List<Long> userIds = new ArrayList<>();
        userIds.add(userId);
        userIds.add(2L);
        lobbyDTO.setLobbyId(1000L);
        lobbyDTO.setHostId(userId);
        lobbyDTO.setUsersIds(userIds);

        // when
        when(userService.checkIfUserExists(userId)).thenReturn(user);
        when(lobbyService.checkIfLobbyExists(lobby.getLobbyId())).thenReturn(lobby);

        String status = "subscribed";

        UsersBroadcastJoinNotificationDTO dto = webSocketService.convertToDTO(user.getId(), lobby.getLobbyId(), status);

        assertNotNull(dto);
        assertEquals(status, dto.getStatus());
        assertEquals("johnDoe", dto.getUsername());
        assertEquals(lobbyDTO.getLobbyId(), dto.getLobby().getLobbyId());
        assertEquals(lobbyDTO.getHostId(), dto.getLobby().getHostId());
        assertEquals(lobbyDTO.getUsersIds(), dto.getLobby().getUsersIds());
    }

    @Test
    void ConvertToDTOForUserJoinNotification() {
        // given
        String msg = "Joined successfully";
        boolean success = true;

        UserNotificationDTO dto = webSocketService.convertToDTO(msg, success);

        assertNotNull(dto);
        assertEquals(msg, dto.getMessage());
        assertTrue(dto.getSuccess());
    }

    @Test
    void convertToDTOForBroadcastNotification() {
        // given
        String msg = "Lobby has been deleted";

        // when
        BroadcastNotificationDTO dto = webSocketService.convertToDTO(msg);

        // then
        assertNotNull(dto);
        assertEquals(msg, dto.getMessage());
    }

}

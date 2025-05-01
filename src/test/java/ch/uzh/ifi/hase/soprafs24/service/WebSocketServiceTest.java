package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastNotificationDTO;
import javassist.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class WebSocketServiceTest {
    @InjectMocks
    private WebSocketService webSocketService;

    @Mock
    private UserService userService;


    @BeforeEach
    public void setup()  {
        MockitoAnnotations.openMocks(this);

    }

    @Test
    public void ConvertToDTOForBroadcastSuccess() throws NotFoundException {
        // given
        Long userId = 1L;
        User user = new User();
        user.setUsername("johnDoe");
        user.setId(userId);

        // when
        when(userService.checkIfUserExists(userId)).thenReturn(user);

        String status = "subscribed";

        UsersBroadcastNotificationDTO dto = webSocketService.convertToDTO(user.getId(), status);

        assertNotNull(dto);
        assertEquals(status, dto.getStatus());
        assertEquals("johnDoe", dto.getUsername());
    }

    @Test
    public void ConvertToDTOForUserJoinNotification() {
        // given
        String msg = "Joined successfully";
        boolean success = true;

        UserNotificationDTO dto = webSocketService.convertToDTO(msg, success);

        assertNotNull(dto);
        assertEquals(msg, dto.getMessage());
        assertTrue(dto.getSuccess());
    }

}

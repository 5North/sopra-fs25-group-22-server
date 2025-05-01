package ch.uzh.ifi.hase.soprafs24.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastNotificationDTO;
import javassist.NotFoundException;

class WebSocketEventListenerTest {

    @Mock
    private LobbyService lobbyService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private WebSocketEventListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Helper for building a  SessionSubscribeEvent (or Unsubscribe) with
     * simpDestination or sessionAttributes.
     */
    private SessionSubscribeEvent buildSubscribeEvent(String destination, Long userId) {
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", userId);

        Map<String, Object> headers = new HashMap<>();
        headers.put("simpDestination", destination);
        headers.put("simpSessionAttributes", sessionAttrs);

        Message<byte[]> message = new GenericMessage<>(
                new byte[0],
                new MessageHeaders(headers));
        return new SessionSubscribeEvent(this, message);
    }

    private SessionUnsubscribeEvent buildUnsubscribeEvent(String destination, Long userId) {
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", userId);

        Map<String, Object> headers = new HashMap<>();
        headers.put("simpDestination", destination);
        headers.put("simpSessionAttributes", sessionAttrs);

        Message<byte[]> message = new GenericMessage<>(
                new byte[0],
                new MessageHeaders(headers));
        return new SessionUnsubscribeEvent(this, message);
    }

    @Test
    void handleSubscribeEvent_success() throws Exception {
        SessionSubscribeEvent event = buildSubscribeEvent("/topic/lobby/1234", 55L);
        UsersBroadcastNotificationDTO broadcastDto = new UsersBroadcastNotificationDTO();
        broadcastDto.setUsername("pippo");
        broadcastDto.setStatus("subscribed");
        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("Lobby joined successfully");
        userDto.setSuccess(true);

        doNothing().when(lobbyService).joinLobby(1234L, 55L);
        when(webSocketService.convertToDTO(55L, "subscribed")).thenReturn(broadcastDto);
        when(webSocketService.convertToDTO("Lobby joined successfully", true)).thenReturn(userDto);

        listener.handleSubscribeEvent(event);

        verify(lobbyService).joinLobby(1234L, 55L);
        verify(webSocketService).broadCastLobbyNotifications(1234L, broadcastDto);
        verify(webSocketService).lobbyNotifications(55L, userDto);
    }

    @Test
    void handleSubscribeEvent_failure() throws Exception {
        SessionSubscribeEvent event = buildSubscribeEvent("/topic/lobby/2000", 77L);
        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("not found");
        userDto.setSuccess(false);

        doThrow(new NotFoundException("not found"))
                .when(lobbyService).joinLobby(2000L, 77L);
        when(webSocketService.convertToDTO("not found", false)).thenReturn(userDto);

        listener.handleSubscribeEvent(event);

        verify(lobbyService).joinLobby(2000L, 77L);
        verify(webSocketService, never()).broadCastLobbyNotifications(anyLong(), any());
        verify(webSocketService).lobbyNotifications(77L, userDto);
    }

    @Test
    void handleUnsubscribeEvent_success() throws Exception {
        SessionUnsubscribeEvent event = buildUnsubscribeEvent("/topic/lobby/3000", 88L);
        UsersBroadcastNotificationDTO broadcastDto = new UsersBroadcastNotificationDTO();
        broadcastDto.setUsername("pluto");
        broadcastDto.setStatus("subscribed");
        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("Lobby left successfully");
        userDto.setSuccess(true);

        doNothing().when(lobbyService).leaveLobby(3000L, 88L);
        when(webSocketService.convertToDTO(88L, "subscribed")).thenReturn(broadcastDto);
        when(webSocketService.convertToDTO("Lobby left successfully", true)).thenReturn(userDto);

        listener.handleUnsubscribeEvent(event);

        verify(lobbyService).leaveLobby(3000L, 88L);
        verify(webSocketService).broadCastLobbyNotifications(3000L, broadcastDto);
        verify(webSocketService).lobbyNotifications(88L, userDto);
    }

    @Test
    void handleUnsubscribeEvent_failure() throws Exception {
        SessionUnsubscribeEvent event = buildUnsubscribeEvent("/topic/lobby/4000", 99L);
        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("error!");
        userDto.setSuccess(false);

        doThrow(new IllegalStateException("error!"))
                .when(lobbyService).leaveLobby(4000L, 99L);
        when(webSocketService.convertToDTO("error!", false)).thenReturn(userDto);

        listener.handleUnsubscribeEvent(event);

        verify(lobbyService).leaveLobby(4000L, 99L);
        verify(webSocketService, never()).broadCastLobbyNotifications(anyLong(), any());
        verify(webSocketService).lobbyNotifications(99L, userDto);
    }

    @Test
    void handleSubscribeEvent_nonLobbyDestination() throws Exception {
        SessionSubscribeEvent event = buildSubscribeEvent("/topic/other", 5L);
        listener.handleSubscribeEvent(event);
        verifyNoInteractions(lobbyService, webSocketService);
    }

    @Test
    void handleUnsubscribeEvent_nonLobbyDestination() throws Exception {
        SessionUnsubscribeEvent event = buildUnsubscribeEvent("/topic/foo", 6L);
        listener.handleUnsubscribeEvent(event);
        verifyNoInteractions(lobbyService, webSocketService);
    }
}

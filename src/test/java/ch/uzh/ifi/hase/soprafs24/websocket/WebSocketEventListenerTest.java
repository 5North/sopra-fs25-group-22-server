package ch.uzh.ifi.hase.soprafs24.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.BroadcastNotificationDTO;
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
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
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
     * Helper for building a SessionSubscribeEvent (or Unsubscribe) with
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
        UsersBroadcastJoinNotificationDTO broadcastDto = new UsersBroadcastJoinNotificationDTO();
        broadcastDto.setUsername("pippo");
        broadcastDto.setStatus("subscribed");
        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("Lobby joined successfully");
        userDto.setSuccess(true);

        doNothing().when(lobbyService).joinLobby(1234L, 55L);
        when(webSocketService.convertToDTO(55L, 1234L, "subscribed")).thenReturn(broadcastDto);
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
        UsersBroadcastJoinNotificationDTO broadcastDto = new UsersBroadcastJoinNotificationDTO();
        broadcastDto.setUsername("pluto");
        broadcastDto.setStatus("subscribed");
        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("Lobby left successfully");
        userDto.setSuccess(true);
        Lobby lobby = new Lobby();

        doNothing().when(lobbyService).leaveLobby(3000L, 88L);
        when(webSocketService.convertToDTO(88L, 3000L, "unsubscribed")).thenReturn(broadcastDto);
        when(webSocketService.convertToDTO("Lobby left successfully", true)).thenReturn(userDto);
        when(lobbyService.checkIfLobbyExists(anyLong())).thenReturn(lobby);

        listener.handleUnsubscribeEvent(event);

        verify(lobbyService).leaveLobby(3000L, 88L);
        verify(webSocketService).broadCastLobbyNotifications(3000L, broadcastDto);
        verify(webSocketService).lobbyNotifications(88L, userDto);
    }

    @Test
    void handleUnsubscribeEvent_lobbyDeletion_success() throws Exception {
        SessionUnsubscribeEvent event = buildUnsubscribeEvent("/topic/lobby/3000", 88L);

        UsersBroadcastJoinNotificationDTO broadcastDtoUnsubscribe = new UsersBroadcastJoinNotificationDTO();
        broadcastDtoUnsubscribe.setUsername("pluto");
        broadcastDtoUnsubscribe.setStatus("unsubscribed");

        BroadcastNotificationDTO broadcastDto = new BroadcastNotificationDTO();
        broadcastDto.setMessage("Lobby with id 3000 has been deleted");

        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("Lobby deleted successfully");
        userDto.setSuccess(true);

        doNothing().when(lobbyService).leaveLobby(3000L, 88L);
        when(lobbyService.checkIfLobbyExists(anyLong())).thenThrow(new NotFoundException("not found"));
        when(webSocketService.convertToDTO("Lobby with id 3000 has been deleted")).thenReturn(broadcastDto);
        when(webSocketService.convertToDTO("Lobby deleted successfully", true)).thenReturn(userDto);
        when(webSocketService.convertToDTO(88L, 3000L, "unsubscribed")).thenReturn(broadcastDtoUnsubscribe);

        listener.handleUnsubscribeEvent(event);

        verify(lobbyService).leaveLobby(3000L, 88L);
        verify(webSocketService).broadCastLobbyNotifications(3000L, broadcastDtoUnsubscribe);
        verify(webSocketService).broadCastLobbyNotifications(3000L, broadcastDto);
        verify(webSocketService).lobbyNotifications(88L, userDto);
    }

    @Test
    void handleUnsubscribeEvent_lobbyDeletion_failure() throws Exception {
        SessionUnsubscribeEvent event = buildUnsubscribeEvent("/topic/lobby/3000", 88L);

        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("No lobby with id <lobbyId> found");
        userDto.setSuccess(false);

        Lobby lobby = new Lobby();

        doThrow(new NotFoundException("No lobby with id <lobbyId> found")).when(lobbyService).leaveLobby(3000L, 88L);
        when(webSocketService.convertToDTO("No lobby with id <lobbyId> found", false)).thenReturn(userDto);
        when(lobbyService.checkIfLobbyExists(anyLong())).thenReturn(lobby);

        listener.handleUnsubscribeEvent(event);

        verify(lobbyService).leaveLobby(3000L, 88L);
        verify(webSocketService, never()).broadCastLobbyNotifications(anyLong(), any());
        verify(webSocketService).lobbyNotifications(88L, userDto);
    }

    @Test
    void handleUnsubscribeEvent_failure() throws Exception {
        SessionUnsubscribeEvent event = buildUnsubscribeEvent("/topic/lobby/4000", 99L);
        UserNotificationDTO userDto = new UserNotificationDTO();
        userDto.setMessage("error!");
        userDto.setSuccess(false);

        Lobby lobby = new Lobby();

        doThrow(new NotFoundException("error!"))
                .when(lobbyService).leaveLobby(4000L, 99L);
        when(webSocketService.convertToDTO("error!", false)).thenReturn(userDto);
        when(lobbyService.checkIfLobbyExists(anyLong())).thenReturn(lobby);

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

    @Test
    void testBroadcastNotificationDTOGetterSetter() {
        BroadcastNotificationDTO dto = new BroadcastNotificationDTO();
        assertNull(dto.getMessage(), "Default message should be null");
        dto.setMessage("Hello");
        assertEquals("Hello", dto.getMessage(), "Getter should return the value set");
    }

    // TODO
    @Test
    void handleSubscribeEvent_missingId_throwsNPE() {
        SessionSubscribeEvent event = buildSubscribeEvent("/topic/lobby", 11L);

        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> listener.handleSubscribeEvent(event));
        assertEquals("Could not find lobby ID", ex.getMessage());
    }


    // TODO
    @Test
    void handleSubscribeEvent_largeId_usesLast4Digits() throws Exception {
        SessionSubscribeEvent event = buildSubscribeEvent("/topic/lobby/56789", 22L);
        doNothing().when(lobbyService).joinLobby(anyLong(), anyLong());

        UsersBroadcastJoinNotificationDTO broadcastDto = new UsersBroadcastJoinNotificationDTO();
        when(webSocketService.convertToDTO(22L, 6789L, "subscribed"))
                .thenReturn(broadcastDto);
        UserNotificationDTO userDto = new UserNotificationDTO();
        when(webSocketService.convertToDTO("Lobby joined successfully", true))
                .thenReturn(userDto);

        listener.handleSubscribeEvent(event);

        verify(lobbyService).joinLobby(6789L, 22L);
        verify(webSocketService).broadCastLobbyNotifications(6789L, broadcastDto);
        verify(webSocketService).lobbyNotifications(22L, userDto);
    }

    // TODO
    @Test
    void handleUnsubscribeEvent_largeId_usesLast4Digits() throws Exception {
        SessionUnsubscribeEvent event = buildUnsubscribeEvent("/topic/lobby/98765", 44L);
        doNothing().when(lobbyService).leaveLobby(anyLong(), anyLong());
        when(lobbyService.checkIfLobbyExists(anyLong())).thenReturn(new Lobby());

        UsersBroadcastJoinNotificationDTO unsubDto = new UsersBroadcastJoinNotificationDTO();
        when(webSocketService.convertToDTO(44L, 8765L, "unsubscribed"))
                .thenReturn(unsubDto);
        UserNotificationDTO privateDto = new UserNotificationDTO();
        when(webSocketService.convertToDTO("Lobby left successfully", true))
                .thenReturn(privateDto);

        listener.handleUnsubscribeEvent(event);

        verify(lobbyService).leaveLobby(8765L, 44L);
        verify(webSocketService).broadCastLobbyNotifications(8765L, unsubDto);
        verify(webSocketService).lobbyNotifications(44L, privateDto);
    }
}

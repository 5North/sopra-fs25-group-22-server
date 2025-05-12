package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import javassist.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class WebSocketEventListenerIntegrationTest {

        @MockBean
        private LobbyService lobbyService;

        @SpyBean
        private WebSocketService webSocketService;

        @Autowired
        private WebSocketEventListener eventListener;

        // helper to create a dummy message with session attributes and header
        private Message<byte[]> createMessageSubscribe(String destination, Map<String, Object> sessionAttributes) {
                Map<String, Object> headers = new HashMap<>();
                headers.put("simpDestination", destination);
                headers.put("simpSessionAttributes", sessionAttributes);
                return new org.springframework.messaging.support.GenericMessage<>(new byte[0],
                                new MessageHeaders(headers));
        }

    private Message<byte[]> createMessageUnSubscribe(Map<String, Object> sessionAttributes) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("simpSessionAttributes", sessionAttributes);
        return new org.springframework.messaging.support.GenericMessage<>(new byte[0],
                new MessageHeaders(headers));
    }

        @Test
        void testSubscribeIntegration() throws NotFoundException, URISyntaxException {
                // given
                Map<String, Object> sessionAttributes = new HashMap<>();
                sessionAttributes.put("userId", 15L);
                String destination = "/topic/lobby/2000";
            Message<byte[]> message = createMessageSubscribe(destination, sessionAttributes);
                SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

                UsersBroadcastJoinNotificationDTO LobbyDTO = new UsersBroadcastJoinNotificationDTO();
                LobbyDTO.setUsername("username");
                LobbyDTO.setStatus("subscribed");

                UserNotificationDTO UserDTO = new UserNotificationDTO();
                UserDTO.setMessage("message");
                UserDTO.setSuccess(true);

                // when
                doNothing().when(lobbyService).joinLobby(anyLong(), anyLong());
                doReturn(LobbyDTO).when(webSocketService).convertToDTO(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
                doReturn(UserDTO).when(webSocketService).convertToDTO(Mockito.anyString(), Mockito.anyBoolean());

                // trigger the event listener for the integration test
                eventListener.handleSubscribeEvent(event);

                // Verify with spybean that the methods in webSocketService are called
                verify(webSocketService, atLeastOnce()).broadCastLobbyNotifications(eq(2000L), eq(LobbyDTO));
                verify(webSocketService, atLeastOnce()).sentLobbyNotifications(eq(15L), eq(UserDTO));
        }

        @Test
        void testSubscribeIntegrationThrowsException() throws NotFoundException, URISyntaxException {
                // given
                Map<String, Object> sessionAttributes = new HashMap<>();
                sessionAttributes.put("userId", 15L);
                String destination = "/topic/lobby/2000";
            Message<byte[]> message = createMessageSubscribe(destination, sessionAttributes);
                SessionSubscribeEvent event = new SessionSubscribeEvent(this, message);

                UsersBroadcastJoinNotificationDTO LobbyDTO = new UsersBroadcastJoinNotificationDTO();
                LobbyDTO.setUsername("username");
                LobbyDTO.setStatus("subscribed");

                UserNotificationDTO UserDTO = new UserNotificationDTO();
                UserDTO.setMessage("message");
                UserDTO.setSuccess(false);

                // when
                doThrow(new NotFoundException("message")).when(lobbyService).joinLobby(anyLong(), anyLong());
                doReturn(LobbyDTO).when(webSocketService).convertToDTO(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
                doReturn(UserDTO).when(webSocketService).convertToDTO(Mockito.anyString(), Mockito.anyBoolean());

                // trigger the event listener for the integration test
                eventListener.handleSubscribeEvent(event);

                // Verify with spybean that the methods in webSocketService are called
                verify(webSocketService, never()).broadCastLobbyNotifications(eq(2000L), eq(LobbyDTO));
                verify(webSocketService, atLeastOnce()).sentLobbyNotifications(eq(15L), eq(UserDTO));
        }

        @Test
        void testUnsubscribeIntegration_success() throws NotFoundException {
                Map<String, Object> sessionAttributes = new HashMap<>();
                sessionAttributes.put("userId", 42L);
            Message<byte[]> message = createMessageUnSubscribe(sessionAttributes);
                SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message);

                UsersBroadcastJoinNotificationDTO broadcastDTO = new UsersBroadcastJoinNotificationDTO();
                broadcastDTO.setUsername("userX");
                broadcastDTO.setStatus("subscribed");

                UserNotificationDTO userDTO = new UserNotificationDTO();
                userDTO.setSuccess(true);
                userDTO.setMessage("Lobby left successfully");

            Long lobbyId = 3000L;
                Lobby lobby = new Lobby();
            lobby.setLobbyId(lobbyId);

            when(lobbyService.getLobbyIdByParticipantId(42L)).thenReturn(lobbyId);
                doNothing().when(lobbyService).leaveLobby(3000L, 42L);

                doReturn(broadcastDTO)
                                .when(webSocketService).convertToDTO(42L, 3000L, "unsubscribed");

                doReturn(userDTO)
                                .when(webSocketService).convertToDTO(anyString(), anyBoolean());

                when(lobbyService.checkIfLobbyExists(anyLong())).thenReturn(lobby);

                eventListener.handleUnsubscribeEvent(event);

                verify(webSocketService, atLeastOnce())
                                .broadCastLobbyNotifications(eq(3000L), eq(broadcastDTO));
                verify(webSocketService, atLeastOnce())
                                .sentLobbyNotifications(eq(42L), eq(userDTO));
        }

        @Test
        void testUnsubscribeIntegration_throwsException() throws NotFoundException {
                Map<String, Object> sessionAttributes = new HashMap<>();
                sessionAttributes.put("userId", 99L);
            Message<byte[]> message = createMessageUnSubscribe(sessionAttributes);
                SessionUnsubscribeEvent event = new SessionUnsubscribeEvent(this, message);

                UserNotificationDTO userDTO = new UserNotificationDTO();
                userDTO.setSuccess(false);
                userDTO.setMessage("oops");

                Lobby lobby = new Lobby();

                doThrow(new NotFoundException("oops"))
                                .when(lobbyService).leaveLobby(4000L, 99L);
                doReturn(userDTO).when(webSocketService).convertToDTO(anyString(), anyBoolean());

                when(lobbyService.checkIfLobbyExists(anyLong())).thenReturn(lobby);

                eventListener.handleUnsubscribeEvent(event);

                verify(webSocketService, never())
                                .broadCastLobbyNotifications(anyLong(), any());
                verify(webSocketService, atLeastOnce())
                                .sentLobbyNotifications(eq(99L), eq(userDTO));
        }

}

package ch.uzh.ifi.hase.soprafs24.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import ch.uzh.ifi.hase.soprafs24.websocket.DTO.BroadcastNotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
import javassist.NotFoundException;

@Component
public class WebSocketEventListener {
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final WebSocketService webSocketService;
    private final LobbyService lobbyService;

    @Autowired
    WebSocketEventListener(WebSocketService webSocketService, LobbyService lobbyService) {
        this.webSocketService = webSocketService;
        this.lobbyService = lobbyService;
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) throws URISyntaxException {
        Long lobbyId;

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String simpDestination = getSimpDestination(event);

        if (simpDestination.startsWith("/topic/lobby")) {
            log.info("Subscribing to websocket topic/lobby/{lobbyId}");

            // Retrieve the userid of the current session, which was saved during auth
            // before handshake
            Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
            Long userId = (Long) userIdAttr;

            String msg = "Lobby joined successfully";
            boolean success = true;

            // join lobby
            try {
                lobbyId = getLobbyId(event);
                lobbyService.joinLobby(lobbyId, userId);
                log.info("Lobby {} joined successfully", lobbyId);

                // broadcast msg to lobby
                UsersBroadcastJoinNotificationDTO DTO = webSocketService.convertToDTO(userId, lobbyId, "subscribed");
                webSocketService.broadCastLobbyNotifications(lobbyId, DTO);
                log.info("Message broadcast to lobby {}: lobby joined successfully by user {}", lobbyId, userId);
            } catch (NotFoundException | IllegalStateException e) {
                msg = e.getMessage();
                success = false;
            }

            // notify user
            UserNotificationDTO DTO = webSocketService.convertToDTO(msg, success);
            webSocketService.sentLobbyNotifications(userId, DTO);
            log.info("Message broadcast to user {}: lobby join success {}", userId, success);
        } else {
            log.debug("Received subscription event to other destination: {}", event.getMessage());
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        Long lobbyId = null;

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

            // Retrieve the userid of the current session, which was saved during auth
            // before handshake
            Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
            Long userId = (Long) userIdAttr;

            String msg = "Lobby left successfully";
            boolean success = true;

            // leave lobby
            try {
                lobbyId = lobbyService.getLobbyIdByParticipantId(userId);
                lobbyService.leaveLobby(lobbyId, userId);
                log.info("Lobby {} left successfully", lobbyId);

                // broadcast msg to lobby
                UsersBroadcastJoinNotificationDTO broadcastDTO = webSocketService.convertToDTO(userId, lobbyId, "unsubscribed");
                webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
                log.info("Message broadcast to lobby {}: lobby left by user {}", lobbyId, userId);
            } catch (NotFoundException e) {
                msg = e.getMessage();
                success = false;
            }
            // sent notification to user
            UserNotificationDTO privateDTO = webSocketService.convertToDTO(msg, success);
            webSocketService.sentLobbyNotifications(userId, privateDTO);
            log.info("Message sent to user {}: lobby leave success {}", userId, success);

            // check if lobby has been deleted and set and broadcast right msg
        if (lobbyId != null) {
            try {
                lobbyService.checkIfLobbyExists(lobbyId);
            }
            catch (NotFoundException e) {
                msg = "Lobby with id " + lobbyId + " has been deleted";
                BroadcastNotificationDTO broadcastDTO = webSocketService.convertToDTO(msg);
                webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
                log.info("Message broadcast to lobby {}: lobby deleted", lobbyId);
                msg = "Lobby deleted successfully";
                privateDTO = webSocketService.convertToDTO(msg, success);
                webSocketService.sentLobbyNotifications(userId, privateDTO);
                log.info("Message sent to user {}: lobby deleted successfully", userId);
            }
        }

    }

    // Get string destination from event
    private String getSimpDestination(AbstractSubProtocolEvent event) {
        return Objects.requireNonNull(event
                .getMessage()
                .getHeaders()
                .get("simpDestination"))
                .toString();
    }

    // Get lobbyId from the URI extracted
    private Long getLobbyId(AbstractSubProtocolEvent event) throws URISyntaxException {
        Long lobbyId = null;
        String simpDestination = getSimpDestination(event);
        if (simpDestination.startsWith("/topic/lobby/")) {
            lobbyId = getLobbyIdFromDestination(simpDestination);
        }
        if (lobbyId == null) {
            throw new NullPointerException("Could not find lobby ID");
        }
        return lobbyId;
    }

    // extract lobbyId from destination uri
    private Long getLobbyIdFromDestination(String simpDestination) throws URISyntaxException {
        URI uri = new URI(simpDestination);
        String path = uri.getPath();
        String lobby = path.substring(path.length() - 4);
        return Long.valueOf(lobby);
    }
}

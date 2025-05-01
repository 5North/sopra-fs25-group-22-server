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
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastNotificationDTO;
import javassist.NotFoundException;

@Component
public class WebSocketEventListener {
    private final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
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
                UsersBroadcastNotificationDTO DTO = webSocketService.convertToDTO(userId, "subscribed");
                webSocketService.broadCastLobbyNotifications(lobbyId, DTO);
            } catch (NotFoundException | IllegalStateException e) {
                msg = e.getMessage();
                success = false;
            }

            // notify user
            UserNotificationDTO DTO = webSocketService.convertToDTO(msg, success);
            webSocketService.lobbyNotifications(userId, DTO);
        } else {
            log.debug("Received other sub protocol event: {}", event.getMessage());
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) throws URISyntaxException {
        Long lobbyId;

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String simpDestination = getSimpDestination(event);

        if (simpDestination.startsWith("/topic/lobby")) {

            // Retrieve the userid of the current session, which was saved during auth
            // before handshake
            Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
            Long userId = (Long) userIdAttr;

            String msg = "Lobby left successfully";
            boolean success = true;

            lobbyId = getLobbyId(event);
            // leave lobby
            try {
                lobbyService.leaveLobby(lobbyId, userId);
                log.info("Lobby {} left successfully", lobbyId);

                // broadcast msg to lobby
                UsersBroadcastNotificationDTO DTO = webSocketService.convertToDTO(userId, "unsubscribed");
                webSocketService.broadCastLobbyNotifications(lobbyId, DTO);
            } catch (NotFoundException e) {
                msg = e.getMessage();
                success = false;
            } catch (IllegalStateException e) {
                msg = e.getMessage();
                BroadcastNotificationDTO broadcastDTO = webSocketService.convertToDTO(msg);
                webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
                msg = "Lobby deleted successfully";
            }

            // notify user
            UserNotificationDTO DTO = webSocketService.convertToDTO(msg, success);
            webSocketService.lobbyNotifications(userId, DTO);
        } else {
            log.debug("Received other sub protocol event: {}", event.getMessage());
        }

    }

    // TODO investigate
    /*
     * @EventListener
     * public void handleDisconnectEvent(SessionDisconnectEvent event) throws
     * URISyntaxException {
     * StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
     * 
     * Long lobbyId = getLobbyId(event);
     * 
     * // Retrieve the userid of the current session, which was saved during auth
     * before handshake
     * Object userIdAttr =
     * Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
     * Long userId = (Long) userIdAttr;
     * 
     * 
     * String msg = "Lobby left successfully";
     * boolean success = true;
     * 
     * // leave lobby
     * try {
     * lobbyService.leaveLobby(lobbyId, userId);
     * 
     * // broadcast msg to lobby
     * UsersBroadcastJoinNotificationDTO DTO = webSocketService.convertToDTO(userId,
     * "subscribed");
     * webSocketService.broadCastLobbyNotifications(lobbyId, DTO);
     * 
     * } catch (NotFoundException | NoSuchElementException e) {
     * success = false;
     * msg = e.getMessage();
     * }
     * 
     * // notify user
     * UserJoinNotificationDTO DTO = webSocketService.convertToDTO(msg, success);
     * webSocketService.lobbyNotifications(userId, DTO);
     * 
     * }
     */

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

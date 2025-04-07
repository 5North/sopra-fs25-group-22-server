package ch.uzh.ifi.hase.soprafs24.websocket;


import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserJoinNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Objects;

@Component
public class WebSocketEventListener {

    private final WebSocketService webSocketService;
    private final LobbyService lobbyService;
    private Long lobbyId;

    @Autowired
     WebSocketEventListener(WebSocketService webSocketService, LobbyService lobbyService) {
        this.webSocketService = webSocketService;
        this.lobbyService = lobbyService;
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) throws URISyntaxException, NotFoundException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        lobbyId = getLobbyId(event);

        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        long userId = (Long) userIdAttr;


        String msg = "Lobby joined successfully";
        boolean success = true;

        // join lobby
        try {
            lobbyService.joinLobby(lobbyId, userId);

            // broadcast msg to lobby
            UsersBroadcastJoinNotificationDTO DTO = webSocketService.convertToDTO(userId, "subscribed");
            webSocketService.broadCastLobbyNotifications(lobbyId, DTO);
        }
        catch (NotFoundException | IllegalStateException e) {
            msg = e.getMessage();
            success = false;
        }

        // notify user
        UserJoinNotificationDTO DTO = webSocketService.convertToDTO(msg, success);
        webSocketService.lobbyNotifications(userId, DTO);
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) throws URISyntaxException, NotFoundException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        lobbyId = getLobbyId(event);

        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        long userId = (Long) userIdAttr;

        String msg = "Lobby left successfully";
        boolean success = true;

        // leave lobby
        try {
            lobbyService.leaveLobby(lobbyId, userId);

            // broadcast msg to lobby
            UsersBroadcastJoinNotificationDTO DTO = webSocketService.convertToDTO(userId, "subscribed");
            webSocketService.broadCastLobbyNotifications(lobbyId, DTO);
        } catch (NotFoundException  | IllegalStateException e) {
             msg = e.getMessage();
             success = false;
        }

        // notify user
        UserJoinNotificationDTO DTO = webSocketService.convertToDTO(msg, success);
        webSocketService.lobbyNotifications(userId, DTO);

    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) throws URISyntaxException, NotFoundException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        lobbyId = getLobbyId(event);

        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        long userId = (Long) userIdAttr;


        String msg = "Lobby left successfully";
        boolean success = true;

        // leave lobby
        try {
            lobbyService.leaveLobby(lobbyId, userId);

            // broadcast msg to lobby
            UsersBroadcastJoinNotificationDTO DTO = webSocketService.convertToDTO(userId, "subscribed");
            webSocketService.broadCastLobbyNotifications(lobbyId, DTO);

        } catch (NotFoundException | NoSuchElementException e) {
            success = false;
            msg = e.getMessage();
        }

        // notify user
        UserJoinNotificationDTO DTO = webSocketService.convertToDTO(msg, success);
        webSocketService.lobbyNotifications(userId, DTO);

    }

    // Get lobbyId from the URI extracted
    private Long getLobbyId(AbstractSubProtocolEvent event) throws URISyntaxException {
        String simpDestination = Objects.requireNonNull(event
                        .getMessage()
                        .getHeaders()
                        .get("simpDestination"))
                .toString();
        if (simpDestination.startsWith("/topic/lobby/")) {
            lobbyId = getLobbyIdFromDestination(simpDestination);
        }
        return lobbyId;
    }

    // extract lobbyId from destination uri
    private Long getLobbyIdFromDestination(String simpDestination) throws URISyntaxException {

        URI uri = new URI(simpDestination);
        String path = uri.getPath();

        String[] segments = path.split("/");
        return Long.parseLong((segments[segments.length - 1]));
    }
}

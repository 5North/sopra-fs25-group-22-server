package ch.uzh.ifi.hase.soprafs24.websocket;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserJoinNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
import javassist.NotFoundException;

@Component
public class WebSocketEventListener {

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

        try {
             lobbyId = getLobbyId(event);
        } catch (NullPointerException e){
            return;
        }

        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdAttr;


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
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) throws URISyntaxException {
        Long lobbyId;

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        try {
             lobbyId = getLobbyId(event);
        } catch (NullPointerException e){
            return;
        }

        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdAttr;

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

   /* @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) throws URISyntaxException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Long lobbyId = getLobbyId(event);

        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdAttr;


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

    }*/

    // Get lobbyId from the URI extracted
    private Long getLobbyId(AbstractSubProtocolEvent event) throws URISyntaxException {
        Long lobbyId = null;
        String simpDestination = Objects.requireNonNull(event
                        .getMessage()
                        .getHeaders()
                        .get("simpDestination"))
                .toString();
        if (simpDestination.startsWith("/topic/lobby/")) {
            lobbyId = getLobbyIdFromDestination(simpDestination);
        }
        if (lobbyId == null) {throw new NullPointerException("Could not find lobby ID");}
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

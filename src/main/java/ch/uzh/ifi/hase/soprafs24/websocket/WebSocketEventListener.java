package ch.uzh.ifi.hase.soprafs24.websocket;


import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;

import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Objects;

@Component
public class WebSocketEventListener {

    private final WebSocketService webSocketService;
    private final LobbyService lobbyService;
    private final UserService userService;
    private Long lobbyId;

    @Autowired
     WebSocketEventListener(WebSocketService webSocketService, LobbyService lobbyService, UserService userService) {
        this.webSocketService = webSocketService;
        this.lobbyService = lobbyService;
        this.userService = userService;
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) throws URISyntaxException, NotFoundException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String simpDestination = Objects.requireNonNull(event
                        .getMessage()
                        .getHeaders()
                        .get("simpDestination"))
                        .toString();


        //TODO ev refactor into ws service

        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        User user = userService.checkIfUserExists((Long) userIdAttr);
        long userId = user.getId();

        if (simpDestination.startsWith("/topic/lobby/")) {
            lobbyId = webSocketService.getLobbyId(simpDestination);
        }

        String msg = "Lobby joined successfully";
        boolean success = true;

        // join lobby
        try {
            lobbyService.joinLobby(lobbyId, userId);

            // broadcast msg to lobby
            webSocketService.broadCastLobbyNotifications(userId, lobbyId, "subscribed");
        }
        catch (NotFoundException | IllegalStateException e) {
            msg = e.getMessage();
            success = false;
        }

        // notify user
        webSocketService.lobbyNotifications(userId, msg, success);
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) throws URISyntaxException, NotFoundException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String simpDestination = Objects.requireNonNull(event
                        .getMessage()
                        .getHeaders()
                        .get("simpDestination"))
                        .toString();


        //TODO ev refactor into ws service
        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        User user = userService.checkIfUserExists((Long) userIdAttr);
        long userId = user.getId();

        if (simpDestination.startsWith("/topic/lobby/")) {
            lobbyId = webSocketService.getLobbyId(simpDestination);
        }

        String msg = "Lobby left successfully";
        boolean success = true;

        // leave lobby
        try {
            lobbyService.leaveLobby(lobbyId, userId);

            // broadcast msg to lobby
            webSocketService.broadCastLobbyNotifications(userId, lobbyId, "unsubscribed");
        } catch (NotFoundException  | IllegalStateException e) {
             msg = e.getMessage();
             success = false;
        }

        // notify user
        webSocketService.lobbyNotifications(userId, msg, success);

    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) throws URISyntaxException, NotFoundException {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String simpDestination = Objects.requireNonNull(event
                .getMessage()
                .getHeaders()
                .get("simpDestination"))
                .toString();

        //TODO ev refactor into ws service
        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        User user = userService.checkIfUserExists((Long) userIdAttr);
        long userId = user.getId();

        if (simpDestination.startsWith("/topic/lobby/")) {
            lobbyId = webSocketService.getLobbyId(simpDestination);
        }

        String msg = "Lobby left successfully";
        boolean success = true;

        // leave lobby
        try {
            lobbyService.leaveLobby(lobbyId, userId);

            // broadcast msg to lobby
            webSocketService.broadCastLobbyNotifications(userId, lobbyId, "disconnected");
        } catch (NotFoundException | NoSuchElementException e) {
            success = false;
            msg = e.getMessage();
        }

        // notify user
        webSocketService.lobbyNotifications(userId, msg, success);

    }
}

package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    // send message to specific user
    /* public void sendMessage(String username, String message) {
    //    messagingTemplate.convertAndSendToUser(username, "/queue/reply", message);
    } */

    // broadcast lobby join/leave msgs to lobby users
    public void broadCastLobbyNotifications(Long userId, Long lobbyId, String status){
        String message = formatLobbyNotificationSimpMessage(status, userId);
        SimpMessagingTemplate messagingTemplate = this.messagingTemplate;

        // broadcast notification to right lobby
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, message);
    }

    public String formatLobbyNotificationSimpMessage(String status, long userId) {
        User user = userRepository.findById(userId);
        String msg = switch (status) {
            case "subscribed" -> "has joined the lobby";
            case "unsubscribed", "disconnected" -> "has left the lobby";
            default -> "";
        };
        return "The user" + user.getUsername() + msg;
    }


    // get lobbyId from destination uri
    public Long getLobbyId(String simpDestination) throws URISyntaxException {

        URI uri = new URI(simpDestination);
        String path = uri.getPath();

        String[] segments = path.split("/");
        return Long.parseLong((segments[segments.length - 1]));
    }
}

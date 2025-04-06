package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    // broadcast lobby join/leave msgs to lobby users
    public void broadCastLobbyNotifications(Long userId, Long lobbyId, String status){
        Map<String, String> message = formatLobbyNotificationSimpMessage(userId, status);
        SimpMessagingTemplate messagingTemplate = this.messagingTemplate;

        // broadcast notification to right lobby
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, message);
    }

    // sent join notification back to specific user
    public void lobbyNotifications(Long userId, String msg, boolean success ){
        Map<String, String> message = formatLobbyNotificationSimpMessage(msg, success);
        SimpMessagingTemplate messagingTemplate = this.messagingTemplate;

        // send notification to user
        messagingTemplate.convertAndSendToUser(Long.toString(userId),"/queue/reply", message);

    }

    // method for formatting broadcast lobby notifications
    public Map<String, String> formatLobbyNotificationSimpMessage(Long userId, String status) {
        User user = userRepository.findById(userId.longValue());
        Map<String, String> message = new HashMap<>();
                message.put("user", user.getUsername());
                message.put("status", status);
        return message;
    }

    // method overload for  formatting lobbyNotification
    public Map<String, String> formatLobbyNotificationSimpMessage(String msg, boolean success) {
        Map<String, String> message = new HashMap<>();
                message.put("success", String.valueOf(success));
                message.put("msg", msg);
        return message;
    }


    // get lobbyId from destination uri
    public Long getLobbyId(String simpDestination) throws URISyntaxException {

        URI uri = new URI(simpDestination);
        String path = uri.getPath();

        String[] segments = path.split("/");
        return Long.parseLong((segments[segments.length - 1]));
    }
}

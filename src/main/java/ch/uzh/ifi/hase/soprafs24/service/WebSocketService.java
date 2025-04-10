package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserJoinNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    // broadcast lobby join/leave messages to lobby users
    public void broadCastLobbyNotifications(Long lobbyId, Object DTO) {

        // broadcast notification to right lobby
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, DTO);
    }

    // sent join notification back to specific user
    public void lobbyNotifications(Long userId, Object DTO) {

        // send notification to user
        messagingTemplate.convertAndSendToUser(Long.toString(userId),"/queue/reply", DTO);

    }

    // method for creating DTO to broadcast lobby joining notifications
    public UsersBroadcastJoinNotificationDTO convertToDTO(Long userId, String status) throws NotFoundException {
        User user = userService.checkIfUserExists(userId);
        UsersBroadcastJoinNotificationDTO DTO = new UsersBroadcastJoinNotificationDTO();
        DTO.setStatus(status);
        DTO.setUsername(user.getUsername());
        return DTO;
    }

    // method overload to create DTO for private user joining notifications
    public UserJoinNotificationDTO convertToDTO(String msg, boolean success) {
        UserJoinNotificationDTO DTO = new UserJoinNotificationDTO();
        DTO.setSuccess(success);
        DTO.setMessage(msg);
        return DTO;
    }
}

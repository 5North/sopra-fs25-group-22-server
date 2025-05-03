package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.BroadcastNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UsersBroadcastJoinNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.wsLobbyDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.mapper.wsDTOMapper;
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
    private final LobbyService lobbyService;
    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate, UserService userService, LobbyService lobbyService) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.lobbyService = lobbyService;
    }

    // broadcast lobby join/leave messages (and other notifications) to lobby users
    public void broadCastLobbyNotifications(Long lobbyId, Object DTO) {

        // broadcast notification to right lobby
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, DTO);
    }

    // sent (join) notification back to specific user
    public void lobbyNotifications(Long userId, Object DTO) {

        // send notification to user
        messagingTemplate.convertAndSendToUser(Long.toString(userId),"/queue/reply", DTO);

    }

    // method for creating DTO to broadcast lobby (joining) notifications
    public UsersBroadcastJoinNotificationDTO convertToDTO(Long userId, Long lobbyId, String status) throws NotFoundException {
        User user = userService.checkIfUserExists(userId);
        UsersBroadcastJoinNotificationDTO DTO = new UsersBroadcastJoinNotificationDTO();
        Lobby lobby = lobbyService.checkIfLobbyExists(lobbyId);
        wsLobbyDTO wsLobbyDTO = wsDTOMapper.INSTANCE.convertLobbyTowsLobbyDTO(lobby);
        DTO.setStatus(status);
        DTO.setUsername(user.getUsername());
        DTO.setLobbyDTO(wsLobbyDTO);
        return DTO;
    }

    // method overload to create DTO for private user (joining) notifications
    public UserNotificationDTO convertToDTO(String msg, boolean success) {
        UserNotificationDTO DTO = new UserNotificationDTO();
        DTO.setSuccess(success);
        DTO.setMessage(msg);
        return DTO;
    }

    // method overload to create DTO for plain broadcast notification (e.g. lobby deleted)
    public BroadcastNotificationDTO convertToDTO(String msg) {
        BroadcastNotificationDTO DTO = new BroadcastNotificationDTO();
        DTO.setMessage(msg);
        return DTO;
    }
}

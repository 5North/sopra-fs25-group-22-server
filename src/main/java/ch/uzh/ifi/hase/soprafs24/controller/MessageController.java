package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

// TODO update docs
/**
 * Message Controller
 * This class is responsible for handling all STOMP msg sent to the server.
 * The controller will receive the msg and delegate the execution to the
 * Websocketservice/LobbyService/Gameservice and finally return the result.
 */

@Controller
public class MessageController {

    // example method
    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public String processMessage(String message) {
        return "Received: " + message;
    }
}

package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;

// TODO update docs
/**
 * Message Controller
 * This class is responsible for handling all STOMP msg sent to the server.
 * The controller will receive the msg and delegate the execution to the LobbyService/Gameservice/UserService and then
 * send a response with methods from WebSocketService.
 */

@Controller
public class MessageController {

    private final LobbyService lobbyService;
    private final WebSocketService webSocketService;

    public MessageController(LobbyService lobbyService, WebSocketService webSocketService) {
        this.lobbyService = lobbyService;
        this.webSocketService = webSocketService;
    }

    // app/play/card -> respond with card played
    @MessageMapping("/app/startGame")
    public void processPlayedCard(String message, StompHeaderAccessor accessor) {
        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdAttr;
        Lobby lobby = lobbyService.getLobby(userId);
        GameSession game = gameService.startGame(lobby);
        GameSessionDTO gameDTO =  convertGameEntityToGameSessionDTO(game);
        webSocketService.broadCastLobbyNotifications(lobbyId, gameDTO);
    }

    // app/play/card -> respond with card played
    @MessageMapping("/app/playCard")
    public void processPlayedCard(String message, StompHeaderAccessor accessor) {
        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdAttr;

        gameService.playCard(Card, gameId);
        Player player = game.getCurrentPlayer();
        PlayerDTO playerDTO = convertPlayerEntityToPlayerDTO(player);

        webSocketService.lobbyNotifications(userid, playerDTO);
        webSocketService.broadCastLobbyNotifications(lobbyId, gameUpdateDTO);
    }

    @MessageMapping("/app/chooseCapture")
    public void processOptionChosen(String message, StompHeaderAccessor accessor) {
        // Retrieve the userid of the current session, which was saved during auth before handshake
        Object userIdAttr = Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
       gameService.playCard(List<Card>);
    }
}

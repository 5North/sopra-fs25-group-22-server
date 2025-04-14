package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.ChosenCaptureDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.PlayCardDTO;
import org.springframework.data.util.Pair;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;

@Controller
public class MessageController {

    private final LobbyService lobbyService;
    private final GameService gameService;
    private final WebSocketService webSocketService;

    public MessageController(LobbyService lobbyService, GameService gameService, WebSocketService webSocketService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.webSocketService = webSocketService;
    }

    @MessageMapping("/app/startGame")
    public void processStartGame(@Payload LobbyDTO lobbyDTO) {
        Long lobbyId = lobbyDTO.getLobbyId();
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        GameSession game = gameService.startGame(lobby);

        GameSessionDTO publicGameDTO = GameSessionMapper.convertToGameSessionDTO(game);
        webSocketService.broadCastLobbyNotifications(lobbyId, publicGameDTO);

        game.getPlayers().forEach(player -> {
            PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(player);
            webSocketService.lobbyNotifications(player.getUserId(), privateDTO);
        });
    }

    @MessageMapping("/app/playCard")
    public void processPlayCard(@Payload PlayCardDTO DTO,
            StompHeaderAccessor headerAccessor) {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        CardDTO cardDTO = DTO.getCard();
        Long gameId = DTO.getLobbyId();

        try {
            Pair<GameSessionDTO, PrivatePlayerDTO> pairDTO = gameService.playCard(gameId, cardDTO, userId);
            GameSessionDTO updateGameDTO = pairDTO.getFirst();
            PrivatePlayerDTO updatedPrivateDTO = pairDTO.getSecond();


            webSocketService.lobbyNotifications(userId, updatedPrivateDTO);
            webSocketService.broadCastLobbyNotifications(gameId, updateGameDTO);

        } catch (Exception e) {
            webSocketService.lobbyNotifications(userId, e.getMessage());
        }
    }

    @MessageMapping("/app/chooseCapture")
    public void processChooseCapture(@Payload ChosenCaptureDTO DTO,
            StompHeaderAccessor headerAccessor) {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        List<CardDTO> chosenOption = DTO.getChosenOption();
        Long gameId = DTO.getGameId();

        List<Card> selectedOption = GameSessionMapper.convertCardDTOListToEntity(chosenOption);

        try {
        gameService.processPlayTurn(gameId, selectedOption);

        GameSession game = gameService.getGameSessionById(gameId);
        GameSessionDTO updatedGameDTO = GameSessionMapper.convertToGameSessionDTO(game);
        webSocketService.broadCastLobbyNotifications(gameId, updatedGameDTO);
        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        PrivatePlayerDTO updatedPrivateDTO = GameSessionMapper.convertToPrivatePlayerDTO(currentPlayer);
        webSocketService.lobbyNotifications(userId, updatedPrivateDTO);

        } catch (Exception e) {
            webSocketService.lobbyNotifications(userId, e.getMessage());
        }
    }

}

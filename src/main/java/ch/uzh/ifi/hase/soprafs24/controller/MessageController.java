package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.AiRequestDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.ChosenCaptureDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.PlayCardDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserJoinNotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;

@Controller
public class MessageController {

    private final Logger log = LoggerFactory.getLogger(MessageController.class);
    private final LobbyService lobbyService;
    private final GameService gameService;
    private final WebSocketService webSocketService;

    public MessageController(LobbyService lobbyService, GameService gameService, WebSocketService webSocketService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.webSocketService = webSocketService;
    }

    @MessageMapping("/startGame/{lobbyId}")
    public void processStartGame(@DestinationVariable Long lobbyId) {
        String msg = "Starting game";
        boolean success = true;
        try {
            Lobby lobby = lobbyService.getLobbyById(lobbyId);
            // check if lobby is full
            if (!lobbyService.lobbyIsFull(lobbyId)) {
                throw new IllegalArgumentException("Lobby " + lobbyId + " is not full yet");
            }
            gameService.startGame(lobby);
        } catch (Exception e) {
            log.error(e.getMessage());
            msg = "Error starting game: " + e.getMessage();
            success = false;
        }

        // TODO refactor DTO name
        UserJoinNotificationDTO notificationDTO = webSocketService.convertToDTO(msg, success);
        webSocketService.broadCastLobbyNotifications(lobbyId, notificationDTO);
    }

    @MessageMapping("/updateGame/{gameId}")
    public void receiveUpdateGame(@DestinationVariable Long gameId,
            StompHeaderAccessor headerAccessor) {

        GameSession game = gameService.getGameSessionById(gameId);

        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        GameSessionDTO publicGameDTO = GameSessionMapper.convertToGameSessionDTO(game);

        Player player = game.getPlayerById(userId);

        PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(player);
        webSocketService.lobbyNotifications(userId, privateDTO);
        webSocketService.lobbyNotifications(userId, publicGameDTO);
    }

    @MessageMapping("/playCard")
    public void processPlayCard(@Payload PlayCardDTO DTO,
            StompHeaderAccessor headerAccessor) {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        CardDTO cardDTO = DTO.getCard();
        Long gameId = DTO.getLobbyId();

        try {
            Pair<GameSession, Player> pairDTO = gameService.playCard(gameId, cardDTO, userId);
            GameSession game = pairDTO.getFirst();
            Player currentPlayer = pairDTO.getSecond();

            if (currentPlayer != null) {
                Card lastPlayed = game.getLastCardPlayed();
                if (lastPlayed != null) {
                    List<Card> lastPicked = game.getLastCardPickedCards();
                    MoveActionDTO moveDto = GameSessionMapper.convertToMoveActionDTO(lastPlayed, lastPicked);
                    webSocketService.broadCastLobbyNotifications(gameId, moveDto);
                }
                GameSessionDTO updateGameDTO = GameSessionMapper.convertToGameSessionDTO(game);
                PrivatePlayerDTO updatedPrivateDTO = GameSessionMapper.convertToPrivatePlayerDTO(currentPlayer);

                webSocketService.lobbyNotifications(userId, updatedPrivateDTO);
                webSocketService.broadCastLobbyNotifications(gameId, updateGameDTO);
                gameService.isGameOver(gameId);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    @MessageMapping("/chooseCapture")
    public void processChooseCapture(@Payload ChosenCaptureDTO DTO,
            StompHeaderAccessor headerAccessor) {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        List<CardDTO> chosenOption = DTO.getChosenOption();
        Long gameId = DTO.getGameId();

        List<Card> selectedOption = GameSessionMapper.convertCardDTOListToEntity(chosenOption);

        try {
            GameSession gameBefore = gameService.getGameSessionById(gameId);
            Player currentPlayer = gameBefore.getPlayers().get(gameBefore.getCurrentPlayerIndex());
            gameService.processPlayTurn(gameId, selectedOption);

            GameSession game = gameService.getGameSessionById(gameId);
            Card lastPlayed = game.getLastCardPlayed();
            if (lastPlayed != null) {
                List<Card> lastPicked = game.getLastCardPickedCards();
                MoveActionDTO moveDto = GameSessionMapper.convertToMoveActionDTO(lastPlayed, lastPicked);
                webSocketService.broadCastLobbyNotifications(gameId, moveDto);
            }
            GameSessionDTO updatedGameDTO = GameSessionMapper.convertToGameSessionDTO(game);
            webSocketService.broadCastLobbyNotifications(gameId, updatedGameDTO);
            PrivatePlayerDTO updatedPrivateDTO = GameSessionMapper.convertToPrivatePlayerDTO(currentPlayer);
            webSocketService.lobbyNotifications(userId, updatedPrivateDTO);

            gameService.isGameOver(gameId);

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @MessageMapping("/ai")
    public void processAISuggestion(@Payload AiRequestDTO aiReq,
            StompHeaderAccessor header) {
        Long gameId = aiReq.getGameId();
        Long userId = (Long) Objects.requireNonNull(header.getSessionAttributes()).get("userId");
        AISuggestionDTO aiDto = gameService.aiSuggestion(gameId, userId);
        webSocketService.lobbyNotifications(userId, aiDto);
    }

}
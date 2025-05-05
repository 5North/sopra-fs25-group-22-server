package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.*;
import ch.uzh.ifi.hase.soprafs24.websocket.mapper.wsDTOMapper;
import javassist.NotFoundException;
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
    private final UserService userService;

    public MessageController(LobbyService lobbyService, GameService gameService, WebSocketService webSocketService, UserService userService) {
        this.lobbyService = lobbyService;
        this.gameService = gameService;
        this.webSocketService = webSocketService;
        this.userService = userService;
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
            // check if everyone already clicked rematch
            if (!lobbyService.rematchIsFull(lobbyId)) {
                throw new IllegalArgumentException(String.format("lobby %d: not everyone wants a rematch yet", lobbyId));
            }
            gameService.startGame(lobby);
            // reset rematch array
            lobbyService.resetRematch(lobbyId);

        } catch (Exception e) {
            log.error(e.getMessage());
            msg = "Error starting game: " + e.getMessage();
            success = false;
        }

        UserNotificationDTO notificationDTO = webSocketService.convertToDTO(msg, success);
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
                    MoveActionDTO moveDto = GameSessionMapper.convertToMoveActionDTO(userId, lastPlayed, lastPicked);
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
                MoveActionDTO moveDto = GameSessionMapper.convertToMoveActionDTO(userId, lastPlayed, lastPicked);
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

    @MessageMapping("/quitGame")
    public void processQuitGame(@Payload QuitGameDTO dto,
            StompHeaderAccessor headerAccessor) throws NotFoundException {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes())
                .get("userId");
        Long quittingUserId = (Long) userIdObj;
        User user = userService.checkIfUserExists(quittingUserId);
        Long lobbyId = user.getLobbyJoined();

        if (gameService.getGameSessionById(lobbyId) != null) {
            Long gameId = dto.getGameId();

            List<QuitGameResultDTO> results = gameService.quitGame(gameId, quittingUserId);
            for (QuitGameResultDTO result : results) {
                webSocketService.lobbyNotifications(result.getUserId(), result);
            }
        }

        // default msg and status
        String msg = String.format("Lobby with id %s has been deleted", lobbyId);
        boolean success = true;
        try {
            lobbyService.deleteLobby(lobbyId);
            BroadcastNotificationDTO broadcastDTO = webSocketService.convertToDTO(msg);
            webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
        }
        catch (NotFoundException e) {
            // msg and status for delete failure
            msg = String.format("The lobby with id %s was not found", lobbyId);
            success = false;
        }
        UserNotificationDTO privateDTO= webSocketService.convertToDTO(msg, success);
        webSocketService.broadCastLobbyNotifications(quittingUserId, privateDTO);
    }

    @MessageMapping("/rematch")
    public void rematch(StompHeaderAccessor headerAccessor)  throws NotFoundException {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes())
                .get("userId");
        Long userId = (Long) userIdObj;
        User user = userService.checkIfUserExists(userId);
        Long lobbyId = user.getLobbyJoined();
        Lobby lobby = lobbyService.getLobbyById(lobbyId);

        boolean success = true;
        String msg = "Rematcher has been added to the lobby";

        try {
            lobbyService.addRematcher(userId, lobbyId);
        }
        catch (NotFoundException e) {
            success = false;
            msg = e.getMessage();
        }
        UserNotificationDTO privateDTO = webSocketService.convertToDTO(msg, success);
        webSocketService.lobbyNotifications(userId, privateDTO);

        wsLobbyDTO broadcastDTO = wsDTOMapper.INSTANCE.convertLobbyTowsLobbyRematchDTO(lobby);
        webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
    }

    }
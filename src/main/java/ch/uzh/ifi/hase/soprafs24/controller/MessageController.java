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
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.*;
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

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
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
        log.info("Message at /startGame/{}", lobbyId);

        String msg = "Starting game";
        boolean success = true;
        try {
            Lobby lobby = lobbyService.getLobbyById(lobbyId);
            // check if lobby is full
            if (!lobbyService.lobbyIsFull(lobbyId)) {
                log.info("Lobby {} is not full yet", lobbyId);
                throw new IllegalArgumentException("Lobby " + lobbyId + " is not full yet");
            }
            // check if everyone already clicked rematch
            if (!lobbyService.rematchIsFull(lobbyId)) {
                log.info("Rematch in lobby {} is not full yet", lobbyId);
                throw new IllegalArgumentException(String.format("lobby %d: not everyone wants a rematch yet", lobbyId));
            }
            gameService.startGame(lobby);
            log.info("Game initialised");
            // reset rematch array
            lobbyService.resetRematch(lobbyId);

        } catch (Exception e) {
            log.error(e.getMessage());
            msg = "Error starting game: " + e.getMessage();
            success = false;
        }

        UserNotificationDTO notificationDTO = webSocketService.convertToDTO(msg, success);
        webSocketService.broadCastLobbyNotifications(lobbyId, notificationDTO);
        log.info("Message broadcast to lobby {}: game initialisation status", lobbyId);
    }

    @MessageMapping("/updateGame/{gameId}")
    public void receiveUpdateGame(@DestinationVariable Long gameId,
            StompHeaderAccessor headerAccessor) {
        log.info("Message at /updateGame/{}", gameId);
        GameSession game = gameService.getGameSessionById(gameId);

        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        GameSessionDTO publicGameDTO = GameSessionMapper.convertToGameSessionDTO(game);

        Player player = game.getPlayerById(userId);

        PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(player);
        webSocketService.sentLobbyNotifications(userId, privateDTO);
        log.info("Message sent to user {}: cards in hand update", userId);
        webSocketService.sentLobbyNotifications(userId, publicGameDTO);
        log.info("Message sent to user {}: game update", userId);
    }

    @MessageMapping("/playCard")
    public void processPlayCard(@Payload PlayCardDTO DTO,
            StompHeaderAccessor headerAccessor) {
        log.info("Message at /playCard");
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

                webSocketService.sentLobbyNotifications(userId, updatedPrivateDTO);
                log.info("Message sent to user {}: Cards in hand after card played", userId);
                webSocketService.broadCastLobbyNotifications(gameId, updateGameDTO);
                log.info("Message broadcast to lobby {}: game after cards played by {}", gameId, userId);
                gameService.isGameOver(gameId);
            }

        } catch (Exception e) {
            log.error("Error processing played card: {}", e.getMessage());
        }

    }

    @MessageMapping("/chooseCapture")
    public void processChooseCapture(@Payload ChosenCaptureDTO DTO,
            StompHeaderAccessor headerAccessor) {
        log.info("Message at /chooseCapture");
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
            log.info("Message broadcast to lobby {}: game after choose option", gameId);
            PrivatePlayerDTO updatedPrivateDTO = GameSessionMapper.convertToPrivatePlayerDTO(currentPlayer);
            webSocketService.sentLobbyNotifications(userId, updatedPrivateDTO);
            log.info("Message sent to user {}: cards in hand after choose option by {}", userId, userId);

            gameService.isGameOver(gameId);

        } catch (Exception e) {
            log.error("Error processing card choice: {}", e.getMessage());
        }
    }

    @MessageMapping("/ai")
    public void processAISuggestion(@Payload AiRequestDTO aiReq,
            StompHeaderAccessor header) {
        log.info("Message at /ai");
        Long gameId = aiReq.getGameId();
        Long userId = (Long) Objects.requireNonNull(header.getSessionAttributes()).get("userId");
        try {
            AISuggestionDTO aiDto = gameService.aiSuggestion(gameId, userId);
            webSocketService.sentLobbyNotifications(userId, aiDto);
            log.info("Message sent to user {}: Ai suggestion", userId);
        } catch (Exception e) {
            log.error("Error processing AI suggestion: {}", e.getMessage());
        }
    }

    @MessageMapping("/quitGame")
    public void processQuitGame(@Payload QuitGameDTO dto,
            StompHeaderAccessor headerAccessor) throws NotFoundException {
        log.info("Message at /quitGame");
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes())
                .get("userId");
        Long quittingUserId = (Long) userIdObj;
        User user = userService.checkIfUserExists(quittingUserId);
        Long lobbyId = user.getLobbyJoined();
        if (gameService.getGameSessionById(lobbyId) != null) {
            Long gameId = dto.getGameId();

            List<QuitGameResultDTO> results = gameService.quitGame(gameId, quittingUserId);
            for (QuitGameResultDTO result : results) {
                webSocketService.sentLobbyNotifications(result.getUserId(), result);
                log.info("Message sent to user {}: quit game result", result.getUserId());
            }
        }

        // default msg and status
        String msg = String.format("Lobby with id %s has been deleted", lobbyId);
        boolean success = true;
        try {
            lobbyService.deleteLobby(lobbyId);
            log.info("Lobby with id {} has been deleted", lobbyId);
            BroadcastNotificationDTO broadcastDTO = webSocketService.convertToDTO(msg);
            webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
            log.info("Message broadcast to lobby {}: delete notification", lobbyId);
        }
        catch (NotFoundException e) {
            // msg and status for delete failure
            msg = String.format("The lobby with id %s was not found", lobbyId);
            success = false;
        }
        UserNotificationDTO privateDTO= webSocketService.convertToDTO(msg, success);
        webSocketService.sentLobbyNotifications(quittingUserId, privateDTO);
        log.info("Message sent to user {}: quitting game request in lobby {}", quittingUserId, lobbyId);
    }

    @MessageMapping("/rematch")
    public void rematch(StompHeaderAccessor headerAccessor)  throws NotFoundException {
        log.info("Message at /rematch");
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes())
                .get("userId");
        Long userId = (Long) userIdObj;
        User user = userService.checkIfUserExists(userId);
        Long lobbyId = user.getLobbyJoined();
        Lobby lobby = lobbyService.getLobbyById(lobbyId);

        boolean success = true;
        String msg = "Rematcher has been added to the lobby";

        try {
            lobbyService.addRematcher(lobbyId, userId);
        }
        catch (NotFoundException e) {
            success = false;
            msg = e.getMessage();
        }
        UserNotificationDTO privateDTO = webSocketService.convertToDTO(msg, success);
        webSocketService.sentLobbyNotifications(userId, privateDTO);
        log.info("Message sent to user {}: rematch request in lobby {}", userId, lobbyId);

        LobbyDTO broadcastDTO = DTOMapper.INSTANCE.convertLobbyToLobbyRematchDTO(lobby);
        webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
        log.info("Message broadcast to lobby {}: update for new rematch user {} ", lobbyId, userId);
    }

    }
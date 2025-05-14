package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.*;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
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
import java.util.NoSuchElementException;
import java.util.Objects;

@Controller
public class MessageController {

        private static final Logger log = LoggerFactory.getLogger(MessageController.class);
        private final LobbyService lobbyService;
        private final GameService gameService;
        private final WebSocketService webSocketService;
        private final UserService userService;
        private final TimerService timerService;

        public MessageController(LobbyService lobbyService,
                        GameService gameService,
                        WebSocketService webSocketService,
                        UserService userService,
                        TimerService timerService) {
                this.lobbyService = lobbyService;
                this.gameService = gameService;
                this.webSocketService = webSocketService;
                this.userService = userService;
                this.timerService = timerService;
        }

    @MessageMapping("/startGame/{lobbyId}")
    public void processStartGame(@DestinationVariable Long lobbyId) {
        log.info("Message at /startGame/{}", lobbyId);

        String msg = "Starting game";
        boolean success = true;
        try {
            Lobby lobby = lobbyService.checkIfLobbyExists(lobbyId);
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
        log.info("Message broadcast to lobby {}: game initialisation success {}", lobbyId, success);

        // broadcast initial play-phase timer
        long remPlay = timerService.getRemainingSeconds(lobbyId, timerService.getPlayStrategy());
        TimeLeftDTO playTimeDTO = GameSessionMapper.toTimeToPlayDTO(lobbyId, remPlay);
        webSocketService.broadCastLobbyNotifications(lobbyId, playTimeDTO);
        log.info("Message broadcast to lobby {}: time remaining for first play", lobbyId);
    }

    @MessageMapping("/updateGame/{gameId}")
    public void receiveUpdateGame(@DestinationVariable Long gameId,
            StompHeaderAccessor headerAccessor) {
        Long userId = (Long) Objects.requireNonNull(
                headerAccessor.getSessionAttributes()).get("userId");

        // Try to catch game non-existing exception, so that if some client request update once the game is already
        // ended just send them a notification
        try{
             GameSession game = gameService.getGameSessionById(gameId);

             GameSessionDTO publicGameDTO = GameSessionMapper.convertToGameSessionDTO(game);

             Player player = game.getPlayerById(userId);

             PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(player);
             webSocketService.sentLobbyNotifications(userId, privateDTO);
             log.info("Message sent to user {}: update cards in hand", userId);
             webSocketService.sentLobbyNotifications(userId, publicGameDTO);
             log.info("Message sent to user {}: update game", userId);

             if (game.isChoosing() && userId.equals(game.getCurrentPlayer().getUserId())) {
                 var options = game.getTable().getCaptureOptions(game.getLastCardPlayed());
                 var optsDto = GameSessionMapper.convertCaptureOptionsToDTO(options);
                 webSocketService.sentLobbyNotifications(userId, optsDto);
                 log.info("Message sent to user {}: update card options", userId);
             }


             long remChoice = timerService.getRemainingSeconds(gameId, timerService.getChoiceStrategy());
             if (remChoice > 0) {
                 TimeLeftDTO choiceDTO = GameSessionMapper.toTimeToChooseDTO(gameId, remChoice);
                 webSocketService.sentLobbyNotifications(userId, choiceDTO);
                 log.info("Message sent to user {}: update remaining time for action", userId);
             }
             else {
                 long remPlay = timerService.getRemainingSeconds(gameId, timerService.getPlayStrategy());
                 TimeLeftDTO playDTO = GameSessionMapper.toTimeToPlayDTO(gameId, remPlay);
                 webSocketService.sentLobbyNotifications(userId, playDTO);
                 log.info("Message sent to user {}: update timeout, move on", userId);
             }
        } catch (NoSuchElementException e) {
            String msg = "Error updating game: " + e.getMessage();
            BroadcastNotificationDTO DTO = webSocketService.convertToDTO(msg);
            webSocketService.sentLobbyNotifications(userId, DTO);
            log.info("Message sent to user {}: {}", userId, e.getMessage());
        }
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
                        Pair<GameSession, Player> result = gameService.playCard(gameId, cardDTO, userId);
                        GameSession game = result.getFirst();
                        Player current = result.getSecond();

                        if (current != null) {
                                // public state update
                                GameSessionDTO sessionDTO = GameSessionMapper.convertToGameSessionDTO(game);
                                webSocketService.broadCastLobbyNotifications(gameId, sessionDTO);
                                log.info("Message broadcast to lobby {}: game update after play by user {}", gameId, userId);

                                // private state update
                                PrivatePlayerDTO playerDTO = GameSessionMapper.convertToPrivatePlayerDTO(current);
                                webSocketService.sentLobbyNotifications(userId, playerDTO);
                                log.info("Message sent to user {}: Cards in hand update after play", userId);

                                // broadcast move action only if a card was played
                                if (game.getLastCardPlayed() != null) {
                                        MoveActionDTO moveDTO = GameSessionMapper.convertToMoveActionDTO(
                                                        userId,
                                                        game.getLastCardPlayed(),
                                                        game.getLastCardPickedCards());
                                        webSocketService.broadCastLobbyNotifications(gameId, moveDTO);
                                        log.info("Message broadcast to lobby {}: moved cards {}", gameId, userId);
                                }


                            // check for game over
                            gameService.isGameOver(gameId);

                            // broadcast next play-phase timer
                            long remPlay = timerService.getRemainingSeconds(gameId, timerService.getPlayStrategy());
                            TimeLeftDTO nextPlayDTO = GameSessionMapper.toTimeToPlayDTO(gameId, remPlay);
                            webSocketService.broadCastLobbyNotifications(gameId, nextPlayDTO);
                            log.info("Message broadcast to lobby {}: time left for play", gameId);

                        } else {
                            // multiple-capture: sync choose-phase timer
                            long remChoice = timerService.getRemainingSeconds(gameId,
                                    timerService.getChoiceStrategy());
                            TimeLeftDTO chooseDTO = GameSessionMapper.toTimeToChooseDTO(gameId, remChoice);
                            webSocketService.broadCastLobbyNotifications(gameId, chooseDTO);
                            log.info("Message broadcast to lobby {}: time left for choice", gameId);
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

                try {
                        gameService.processPlayTurn(
                                        gameId,
                                        GameSessionMapper.convertCardDTOListToEntity(chosenOption));
                        GameSession game = gameService.getGameSessionById(gameId);
                        Player current = game.getPlayerById(userId);

                        // public state update
                        GameSessionDTO sessionDTO = GameSessionMapper.convertToGameSessionDTO(game);
                        webSocketService.broadCastLobbyNotifications(gameId, sessionDTO);
                        log.info("Message broadcast to lobby {}: game update after choice", gameId);

                        // private state update
                        PrivatePlayerDTO playerDTO = GameSessionMapper.convertToPrivatePlayerDTO(current);
                        webSocketService.sentLobbyNotifications(userId, playerDTO);
                        log.info("Message sent to user {}: cards in hand update after choice by user {}", userId, userId);

                        // broadcast move action
                        MoveActionDTO moveDTO = GameSessionMapper.convertToMoveActionDTO(
                                        userId,
                                        game.getLastCardPlayed(),
                                        game.getLastCardPickedCards());
                        webSocketService.broadCastLobbyNotifications(gameId, moveDTO);
                        log.info(
                                "Message broadcast to lobby {}: cards moved after choice by user {}",
                                gameId, userId);

                        // check for game over
                        gameService.isGameOver(gameId);

                        // broadcast next play-phase timer
                        long remPlay = timerService.getRemainingSeconds(gameId, timerService.getPlayStrategy());
                        TimeLeftDTO nextPlayDTO = GameSessionMapper.toTimeToPlayDTO(gameId, remPlay);
                        webSocketService.broadCastLobbyNotifications(gameId, nextPlayDTO);
                        log.info("Message broadcast to lobby {}: time left for next play", gameId);

                } catch (Exception e) {
                        log.error(e.getMessage());
                    log.error("Error processing card choice: {}", e.getMessage());
                }
        }

    @MessageMapping("/ai")
    public void processAISuggestion(@Payload AiRequestDTO aiReq,
            StompHeaderAccessor headerAccessor) {
        log.info("Message at /ai");
        Long gameId = aiReq.getGameId();
        Long userId = (Long) Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
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
                Long quittingUserId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                User user = userService.checkIfUserExists(quittingUserId);
                Long lobbyId = user.getLobbyJoined();

                // forfeit if game in progress
                GameSession game = gameService.getGameSessionById(lobbyId);
                if (game != null) {
                        List<QuitGameResultDTO> results = gameService.quitGame(dto.getGameId(), quittingUserId);
                        results.forEach(result -> webSocketService.sentLobbyNotifications(result.getUserId(), result));
                    log.info("Message sent to user {}: quit game result", quittingUserId);
                }

        // default msg and status
        String msg = String.format("Lobby with id %s has been deleted", lobbyId);
        boolean success = true;
        try {
            lobbyService.deleteLobby(lobbyId);
            log.info("Lobby with id {} has been deleted", lobbyId);
            BroadcastNotificationDTO broadcastDTO = webSocketService.convertToDTO(msg);
            webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
            log.info("Message broadcast to lobby {}: lobby delete", lobbyId);
        } catch (NotFoundException e) {
            // msg and status for delete failure
            msg = String.format("The lobby with id %s was not found", lobbyId);
            success = false;
        }
        UserNotificationDTO privateDTO= webSocketService.convertToDTO(msg, success);
        webSocketService.sentLobbyNotifications(quittingUserId, privateDTO);
        log.info("Message sent to user {}: quitting game {} success {}", quittingUserId, lobbyId, success);
    }

    @MessageMapping("/rematch")
    public void rematch(StompHeaderAccessor headerAccessor)  throws NotFoundException {
        log.info("Message at /rematch");
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes())
                .get("userId");
        Long userId = (Long) userIdObj;
        User user = userService.checkIfUserExists(userId);
        Long lobbyId = user.getLobbyJoined();

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
        log.info("Message sent to user {}: rematch in lobby {} success {}", userId, lobbyId, success);

        // update lobby
        Lobby lobby = lobbyService.checkIfLobbyExists(lobbyId);
        LobbyDTO broadcastDTO = DTOMapper.INSTANCE.convertLobbyToLobbyRematchDTO(lobby);
        webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
        log.info("Message broadcast to lobby {}: update rematch list, user {} joined", lobbyId, userId);
    }

    }
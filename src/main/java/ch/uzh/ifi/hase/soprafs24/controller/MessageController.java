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
import java.util.Objects;

@Controller
public class MessageController {

        private final Logger log = LoggerFactory.getLogger(MessageController.class);
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
                String msg = "Starting game";
                boolean success = true;
                try {
                        Lobby lobby = lobbyService.getLobbyById(lobbyId);
                        if (!lobbyService.lobbyIsFull(lobbyId)) {
                                throw new IllegalArgumentException("Lobby " + lobbyId + " is not full yet");
                        }
                        if (!lobbyService.rematchIsFull(lobbyId)) {
                                throw new IllegalArgumentException(
                                                String.format("lobby %d: not everyone wants a rematch yet", lobbyId));
                        }
                        gameService.startGame(lobby);
                        lobbyService.resetRematch(lobbyId);
                } catch (Exception e) {
                        log.error(e.getMessage());
                        msg = "Error starting game: " + e.getMessage();
                        success = false;
                }
                // private outcome notification
                UserNotificationDTO outcomeDTO = webSocketService.convertToDTO(msg, success);
                webSocketService.broadCastLobbyNotifications(lobbyId, outcomeDTO);

                // broadcast initial play-phase timer
                long remPlay = timerService.getRemainingSeconds(lobbyId, timerService.getPlayStrategy());
                TimeLeftDTO playTimeDTO = GameSessionMapper.toTimeToPlayDTO(lobbyId, remPlay);
                webSocketService.broadCastLobbyNotifications(lobbyId, playTimeDTO);
        }

        @MessageMapping("/updateGame/{gameId}")
        public void receiveUpdateGame(@DestinationVariable Long gameId,
                        StompHeaderAccessor headerAccessor) {
                Long userId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");

                GameSession game = gameService.getGameSessionById(gameId);

                // 1) stato privato e pubblico
                PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(
                                game.getPlayerById(userId));
                GameSessionDTO publicDTO = GameSessionMapper.convertToGameSessionDTO(game);
                webSocketService.lobbyNotifications(userId, privateDTO);
                webSocketService.lobbyNotifications(userId, publicDTO);

                // 2) se siamo in “choosing mode” e siamo proprio il giocatore di turno,
                // rimandiamo le opzioni
                if (game.isChoosing() && userId.equals(game.getCurrentPlayer().getUserId())) {
                        var options = game.getTable().getCaptureOptions(game.getLastCardPlayed());
                        var optsDto = GameSessionMapper.convertCaptureOptionsToDTO(options);
                        webSocketService.lobbyNotifications(userId, optsDto);
                        // non usciamo però, così l’UI può ancora ragionare sul timer
                }

                // 3) sincronizzazione timer: choice‐phase se attivo, altrimenti play‐phase
                long remChoice = timerService.getRemainingSeconds(gameId, timerService.getChoiceStrategy());
                if (remChoice > 0) {
                        TimeLeftDTO choiceDTO = GameSessionMapper.toTimeToChooseDTO(gameId, remChoice);
                        webSocketService.lobbyNotifications(userId, choiceDTO);
                } else {
                        long remPlay = timerService.getRemainingSeconds(gameId, timerService.getPlayStrategy());
                        TimeLeftDTO playDTO = GameSessionMapper.toTimeToPlayDTO(gameId, remPlay);
                        webSocketService.lobbyNotifications(userId, playDTO);
                }
        }

        @MessageMapping("/playCard")
        public void processPlayCard(@Payload PlayCardDTO dto,
                        StompHeaderAccessor headerAccessor) {
                Long userId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                Long gameId = dto.getLobbyId();

                try {
                        Pair<GameSession, Player> result = gameService.playCard(gameId, dto.getCard(), userId);
                        GameSession game = result.getFirst();
                        Player current = result.getSecond();

                        if (current != null) {
                                // public state update
                                GameSessionDTO sessionDTO = GameSessionMapper.convertToGameSessionDTO(game);
                                webSocketService.broadCastLobbyNotifications(gameId, sessionDTO);

                                // private state update
                                PrivatePlayerDTO playerDTO = GameSessionMapper.convertToPrivatePlayerDTO(current);
                                webSocketService.lobbyNotifications(userId, playerDTO);

                                // broadcast move action only if a card was played
                                if (game.getLastCardPlayed() != null) {
                                        MoveActionDTO moveDTO = GameSessionMapper.convertToMoveActionDTO(
                                                        userId,
                                                        game.getLastCardPlayed(),
                                                        game.getLastCardPickedCards());
                                        webSocketService.broadCastLobbyNotifications(gameId, moveDTO);
                                }

                                // check for game over
                                gameService.isGameOver(gameId);

                                // broadcast next play-phase timer
                                long remPlay = timerService.getRemainingSeconds(gameId, timerService.getPlayStrategy());
                                TimeLeftDTO nextPlayDTO = GameSessionMapper.toTimeToPlayDTO(gameId, remPlay);
                                webSocketService.broadCastLobbyNotifications(gameId, nextPlayDTO);

                        } else {
                                // multiple-capture: sync choose-phase timer
                                long remChoice = timerService.getRemainingSeconds(gameId,
                                                timerService.getChoiceStrategy());
                                TimeLeftDTO chooseDTO = GameSessionMapper.toTimeToChooseDTO(gameId, remChoice);
                                webSocketService.broadCastLobbyNotifications(gameId, chooseDTO);
                        }
                } catch (Exception e) {
                        log.error(e.getMessage());
                }
        }

        @MessageMapping("/chooseCapture")
        public void processChooseCapture(@Payload ChosenCaptureDTO dto,
                        StompHeaderAccessor headerAccessor) {
                Long userId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                Long gameId = dto.getGameId();

                try {
                        gameService.processPlayTurn(
                                        gameId,
                                        GameSessionMapper.convertCardDTOListToEntity(dto.getChosenOption()));
                        GameSession game = gameService.getGameSessionById(gameId);
                        Player current = game.getPlayerById(userId);

                        // public state update
                        GameSessionDTO sessionDTO = GameSessionMapper.convertToGameSessionDTO(game);
                        webSocketService.broadCastLobbyNotifications(gameId, sessionDTO);

                        // private state update
                        PrivatePlayerDTO playerDTO = GameSessionMapper.convertToPrivatePlayerDTO(current);
                        webSocketService.lobbyNotifications(userId, playerDTO);

                        // broadcast move action
                        MoveActionDTO moveDTO = GameSessionMapper.convertToMoveActionDTO(
                                        userId,
                                        game.getLastCardPlayed(),
                                        game.getLastCardPickedCards());
                        webSocketService.broadCastLobbyNotifications(gameId, moveDTO);

                        // check for game over
                        gameService.isGameOver(gameId);

                        // broadcast next play-phase timer
                        long remPlay = timerService.getRemainingSeconds(gameId, timerService.getPlayStrategy());
                        TimeLeftDTO nextPlayDTO = GameSessionMapper.toTimeToPlayDTO(gameId, remPlay);
                        webSocketService.broadCastLobbyNotifications(gameId, nextPlayDTO);

                } catch (Exception e) {
                        log.error(e.getMessage());
                }
        }

        @MessageMapping("/ai")
        public void processAISuggestion(@Payload AiRequestDTO aiReq,
                        StompHeaderAccessor headerAccessor) {
                Long userId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                AISuggestionDTO aiDTO = gameService.aiSuggestion(aiReq.getGameId(), userId);
                webSocketService.lobbyNotifications(userId, aiDTO);
        }

        @MessageMapping("/quitGame")
        public void processQuitGame(@Payload QuitGameDTO dto,
                        StompHeaderAccessor headerAccessor) throws NotFoundException {
                Long quittingUserId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                User user = userService.checkIfUserExists(quittingUserId);
                Long lobbyId = user.getLobbyJoined();

                // forfeit if game in progress
                GameSession game = gameService.getGameSessionById(lobbyId);
                if (game != null) {
                        List<QuitGameResultDTO> results = gameService.quitGame(dto.getGameId(), quittingUserId);
                        results.forEach(r -> webSocketService.lobbyNotifications(r.getUserId(), r));
                }

                // delete lobby
                String msg = String.format("Lobby with id %s has been deleted", lobbyId);
                boolean success = true;
                try {
                        lobbyService.deleteLobby(lobbyId);
                        BroadcastNotificationDTO broadcastDTO = webSocketService.convertToDTO(msg);
                        webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
                } catch (NotFoundException e) {
                        msg = String.format("The lobby with id %s was not found", lobbyId);
                        success = false;
                }
                // private deletion outcome
                UserNotificationDTO privateDTO = webSocketService.convertToDTO(msg, success);
                webSocketService.broadCastLobbyNotifications(quittingUserId, privateDTO);
        }

        @MessageMapping("/rematch")
        public void rematch(StompHeaderAccessor headerAccessor) throws NotFoundException {
                Long userId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                User user = userService.checkIfUserExists(userId);
                Long lobbyId = user.getLobbyJoined();
                Lobby lobby = lobbyService.getLobbyById(lobbyId);

                boolean success = true;
                String msg = "Rematcher has been added to the lobby";
                try {
                        lobbyService.addRematcher(lobbyId, userId);
                } catch (NotFoundException e) {
                        success = false;
                        msg = e.getMessage();
                }
                // private rematch notification
                UserNotificationDTO privateDTO = webSocketService.convertToDTO(msg, success);
                webSocketService.lobbyNotifications(userId, privateDTO);
                // broadcast rematch status
                LobbyDTO broadcastDTO = DTOMapper.INSTANCE.convertLobbyToLobbyRematchDTO(lobby);
                webSocketService.broadCastLobbyNotifications(lobbyId, broadcastDTO);
        }
}

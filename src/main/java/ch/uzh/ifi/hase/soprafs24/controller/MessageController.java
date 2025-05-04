package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.PlayCardDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.ChosenCaptureDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserNotificationDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.AiRequestDTO;
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
        private final TimerService timerService;

        public MessageController(LobbyService lobbyService,
                        GameService gameService,
                        WebSocketService webSocketService,
                        TimerService timerService) {
                this.lobbyService = lobbyService;
                this.gameService = gameService;
                this.webSocketService = webSocketService;
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
                        gameService.startGame(lobby);
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
                Long userId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");

                GameSession game = gameService.getGameSessionById(gameId);
                PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(game.getPlayerById(userId));
                GameSessionDTO publicDTO = GameSessionMapper.convertToGameSessionDTO(game);
                webSocketService.lobbyNotifications(userId, privateDTO);
                webSocketService.lobbyNotifications(userId, publicDTO);

                if (timerService != null) {
                        long rem = timerService.getRemainingSeconds(gameId);
                        webSocketService.lobbyNotifications(
                                        userId,
                                        GameSessionMapper.toTimeLeftDTO(gameId, rem));
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
                                GameSessionDTO publicDTO = GameSessionMapper.convertToGameSessionDTO(game);
                                webSocketService.broadCastLobbyNotifications(gameId, publicDTO);

                                PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(current);
                                webSocketService.lobbyNotifications(userId, privateDTO);

                                try {
                                        MoveActionDTO moveDto = GameSessionMapper
                                                        .convertToMoveActionDTO(userId,
                                                                        game.getLastCardPlayed(),
                                                                        game.getLastCardPickedCards());
                                        webSocketService.broadCastLobbyNotifications(gameId, moveDto);
                                } catch (Exception e) {
                                        log.error("Could not convert or send MoveActionDTO: " + e.getMessage());
                                }

                                gameService.isGameOver(gameId);

                                if (timerService != null) {
                                        Long nextId = game.getCurrentPlayer().getUserId();
                                        long rem = timerService.getRemainingSeconds(gameId);
                                        webSocketService.lobbyNotifications(
                                                        nextId,
                                                        GameSessionMapper.toTimeLeftDTO(gameId, rem));
                                }
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

                        GameSessionDTO publicDTO = GameSessionMapper.convertToGameSessionDTO(game);
                        webSocketService.broadCastLobbyNotifications(gameId, publicDTO);

                        PrivatePlayerDTO privateDTO = GameSessionMapper.convertToPrivatePlayerDTO(current);
                        webSocketService.lobbyNotifications(userId, privateDTO);

                        try {
                                MoveActionDTO moveDto = GameSessionMapper
                                                .convertToMoveActionDTO(userId,
                                                                game.getLastCardPlayed(),
                                                                game.getLastCardPickedCards());
                                webSocketService.broadCastLobbyNotifications(gameId, moveDto);
                        } catch (Exception e) {
                                log.error("Could not convert or send MoveActionDTO: " + e.getMessage());
                        }

                        gameService.isGameOver(gameId);

                        if (timerService != null) {
                                Long nextId = game.getCurrentPlayer().getUserId();
                                long rem = timerService.getRemainingSeconds(gameId);
                                webSocketService.lobbyNotifications(
                                                nextId,
                                                GameSessionMapper.toTimeLeftDTO(gameId, rem));
                        }
                } catch (Exception e) {
                        log.error(e.getMessage());
                }
        }

        @MessageMapping("/ai")
        public void processAISuggestion(@Payload AiRequestDTO aiReq,
                        StompHeaderAccessor headerAccessor) {
                Long userId = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                AISuggestionDTO aiDto = gameService.aiSuggestion(aiReq.getGameId(), userId);
                webSocketService.lobbyNotifications(userId, aiDto);
        }

        @MessageMapping("/quitGame")
        public void processQuitGame(@Payload QuitGameDTO dto,
                        StompHeaderAccessor headerAccessor) throws NotFoundException {
                Long quittingUser = (Long) Objects.requireNonNull(
                                headerAccessor.getSessionAttributes()).get("userId");
                List<QuitGameResultDTO> results = gameService.quitGame(dto.getGameId(), quittingUser);
                results.forEach(r -> webSocketService.lobbyNotifications(r.getUserId(), r));

                if (timerService != null) {
                        timerService.cancel(dto.getGameId());
                }
                lobbyService.deleteLobby(dto.getGameId());
        }
}

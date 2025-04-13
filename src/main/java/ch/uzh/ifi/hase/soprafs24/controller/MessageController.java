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
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
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
    public void processStartGame(LobbyDTO lobbyDTO) {
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
    public void processPlayCard(@Payload CardDTO cardDTO,
            @Header("gameId") Long gameId,
            SimpMessageHeaderAccessor headerAccessor) {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        if (gameId == null) {
            webSocketService.lobbyNotifications(userId, "Game ID not provided. Unable to process played card.");
            return;
        }

        Card playedCard = GameSessionMapper.convertCardDTOtoEntity(cardDTO);

        try {
            gameService.processPlayTurn(gameId, playedCard, null);
        } catch (IllegalStateException e) {
            List<List<Card>> options = gameService.getGameSessionById(gameId).getTable().getCaptureOptions(playedCard);
            var optionsDTO = GameSessionMapper.convertCaptureOptionsToDTO(options);
            webSocketService.lobbyNotifications(userId, optionsDTO);
            return;
        } catch (IllegalArgumentException e) {
            webSocketService.lobbyNotifications(userId, e.getMessage());
            return;
        }

        GameSession game = gameService.getGameSessionById(gameId);
        GameSessionDTO updatedGameDTO = GameSessionMapper.convertToGameSessionDTO(game);
        webSocketService.broadCastLobbyNotifications(gameId, updatedGameDTO);

        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        PrivatePlayerDTO updatedPrivateDTO = GameSessionMapper.convertToPrivatePlayerDTO(currentPlayer);
        webSocketService.lobbyNotifications(userId, updatedPrivateDTO);

        if (game.isGameOver()) {
            game.finishGame();

            game.getPlayers().forEach(player -> {
                var resultDTO = GameSessionMapper.convertResultToDTO(game.calculateResult(), player.getUserId());
                webSocketService.lobbyNotifications(player.getUserId(), resultDTO);
            });
        }
    }

    @MessageMapping("/app/chooseCapture")
    public void processChooseCapture(@Payload List<CardDTO> chosenOption,
            @Header("gameId") Long gameId,
            @Header("playedCard") CardDTO playedCardDTO,
            SimpMessageHeaderAccessor headerAccessor) {
        Object userIdObj = Objects.requireNonNull(headerAccessor.getSessionAttributes()).get("userId");
        Long userId = (Long) userIdObj;

        List<Card> selectedOption = GameSessionMapper.convertCardDTOListToEntity(chosenOption);
        Card playedCard = GameSessionMapper.convertCardDTOtoEntity(playedCardDTO);

        gameService.processPlayTurn(gameId, playedCard, selectedOption);

        GameSession game = gameService.getGameSessionById(gameId);
        GameSessionDTO updatedGameDTO = GameSessionMapper.convertToGameSessionDTO(game);
        webSocketService.broadCastLobbyNotifications(gameId, updatedGameDTO);

        Player currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
        PrivatePlayerDTO updatedPrivateDTO = GameSessionMapper.convertToPrivatePlayerDTO(currentPlayer);
        webSocketService.lobbyNotifications(userId, updatedPrivateDTO);
    }

}

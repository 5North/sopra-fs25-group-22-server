package ch.uzh.ifi.hase.soprafs24.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.ChosenCaptureDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.PlayCardDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.UserJoinNotificationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.AiRequestDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class MessageControllerTest {

    @Mock
    private LobbyService lobbyService;

    @Mock
    private GameService gameService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private MessageController messageController;

    private StompHeaderAccessor createHeaderAccessorWithUser(Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor
                .wrap(new org.springframework.messaging.support.GenericMessage<>(new byte[0]));
        accessor.setSessionAttributes(new HashMap<>());
        accessor.getSessionAttributes().put("userId", userId);
        return accessor;
    }

    // --- Test /app/startGame ---
    @Test
    void testProcessStartGameSuccess() {
        // given
        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setLobbyId(100L);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(100L);


        String msg = "Starting game";
        GameSession dummyGame = new GameSession(lobby.getLobbyId(), lobby.getUsers());
        UserJoinNotificationDTO dummyNotificationDTO = new UserJoinNotificationDTO();
        dummyNotificationDTO.setSuccess(Boolean.TRUE);
        dummyNotificationDTO.setMessage(msg);


        when(lobbyService.getLobbyById(100L)).thenReturn(lobby);
        when(lobbyService.lobbyIsFull(lobby.getLobbyId())).thenReturn(true);
        when(gameService.startGame(lobby)).thenReturn(dummyGame);
        when(webSocketService.convertToDTO(anyString(), anyBoolean())).thenReturn(dummyNotificationDTO);



        messageController.processStartGame(lobbyDTO.getLobbyId());


        verify(webSocketService, times(1))
                .convertToDTO(msg, true);
        verify(webSocketService, times(1))
                .broadCastLobbyNotifications(eq(100L), any(UserJoinNotificationDTO.class));
    }

    @Test
    void testProcessStartGameThrowsException() {
        // given
        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setLobbyId(100L);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(100L);


        String msg = "Lobby " + lobby.getLobbyId() + " is not full yet";
        UserJoinNotificationDTO dummyNotificationDTO = new UserJoinNotificationDTO();
        dummyNotificationDTO.setSuccess(Boolean.FALSE);
        dummyNotificationDTO.setMessage(msg);

        // when
        when(lobbyService.getLobbyById(100L)).thenReturn(lobby);
        when(lobbyService.lobbyIsFull(lobby.getLobbyId())).thenReturn(false);
        when(webSocketService.convertToDTO(anyString(), anyBoolean())).thenReturn(dummyNotificationDTO);

        messageController.processStartGame(lobbyDTO.getLobbyId());


        verify(webSocketService, times(1))
                .convertToDTO("Error starting game: " + msg, false);
        verify(webSocketService, times(1))
                .broadCastLobbyNotifications(eq(100L), any(UserJoinNotificationDTO.class));
    }

    @Test
    void testGameUpdateRequest() {
        Long userId = 1L;
        StompHeaderAccessor headerAccessor = createHeaderAccessorWithUser(userId);
        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setLobbyId(100L);

        Lobby lobby = new Lobby();
        lobby.setLobbyId(100L);
        lobby.addUsers(1L);
        lobby.addUsers(2L);

        GameSession session = new GameSession(100L, new ArrayList<>(lobby.getUsers()));
        when(gameService.getGameSessionById(100L)).thenReturn(session);

        messageController.receiveUpdateGame(session.getGameId(), headerAccessor);

        verify(webSocketService, times(1)).lobbyNotifications(anyLong(), any(PrivatePlayerDTO.class));
        verify(webSocketService, times(1)).lobbyNotifications(anyLong(), any(GameSessionDTO.class));
    }


    // --- Test /app/playCard ---
    @Test
    public void testProcessPlayCard() {
        PlayCardDTO playCardDTO = new PlayCardDTO();
        playCardDTO.setLobbyId(300L);
        playCardDTO.setCard(new CardDTO("COPPE", 7));
        StompHeaderAccessor accessor = createHeaderAccessorWithUser(100L);

        GameSession session = new GameSession(300L, Arrays.asList(100L, 200L));
        Player currentPlayer = new Player(200L, new ArrayList<>());
        Pair<GameSession, Player> returnedPair = Pair.of(session, currentPlayer);
        when(gameService.playCard(eq(300L), any(CardDTO.class), eq(100L))).thenReturn(returnedPair);

        messageController.processPlayCard(playCardDTO, accessor);

        verify(webSocketService, atLeastOnce()).broadCastLobbyNotifications(eq(300L), any());
        verify(webSocketService, atLeastOnce()).lobbyNotifications(eq(100L), any());
        verify(gameService, atLeastOnce()).isGameOver(300L);
    }

    // --- Test /app/chooseCapture ---
    @Test
    public void testProcessChooseCapture() {
        Lobby lobby = new Lobby();
        lobby.setLobbyId(400L);
        lobby.addUsers(10L);
        lobby.addUsers(20L);
        GameSession session = new GameSession(400L, new ArrayList<>(lobby.getUsers()));
        when(gameService.getGameSessionById(400L)).thenReturn(session);

        ChosenCaptureDTO chosenCaptureDTO = new ChosenCaptureDTO();
        chosenCaptureDTO.setGameId(400L);
        List<CardDTO> chosenOption = new ArrayList<>();
        chosenOption.add(new CardDTO("COPPE", 3));
        chosenOption.add(new CardDTO("COPPE", 4));
        chosenCaptureDTO.setChosenOption(chosenOption);

        StompHeaderAccessor accessor = createHeaderAccessorWithUser(10L);

        messageController.processChooseCapture(chosenCaptureDTO, accessor);

        verify(webSocketService, atLeastOnce()).broadCastLobbyNotifications(eq(400L), any());
        verify(webSocketService, atLeastOnce()).lobbyNotifications(eq(10L), any());
    }

    // --- Further ---
    @Test
    public void testProcessPlayCardThrowsExceptionAndNotifiesUser() {

        PlayCardDTO playCardDTO = new PlayCardDTO();
        playCardDTO.setLobbyId(500L);
        playCardDTO.setCard(new CardDTO("COPPE", 7));
        StompHeaderAccessor accessor = createHeaderAccessorWithUser(300L);
        when(gameService.playCard(eq(500L), any(CardDTO.class), eq(300L)))
                .thenThrow(new IllegalArgumentException("Invalid card played. Unable to process played card."));

        assertDoesNotThrow(() -> messageController.processPlayCard(playCardDTO, accessor));
        verify(webSocketService, atLeastOnce()).lobbyNotifications(eq(300L), contains("Invalid card played"));
    }

    // --- Test /app/ai ---
    @Test
    public void testProcessAISuggestion() {
        AiRequestDTO aiReq = new AiRequestDTO();
        aiReq.setGameId(123L);

        StompHeaderAccessor accessor = createHeaderAccessorWithUser(42L);

        AISuggestionDTO expectedDto = new AISuggestionDTO("DENARI-7; COPPE-4");
        when(gameService.aiSuggestion(123L, 42L)).thenReturn(expectedDto);

        messageController.processAISuggestion(aiReq, accessor);

        verify(webSocketService, times(1))
                .lobbyNotifications(eq(42L), eq(expectedDto));
    }

}

package ch.uzh.ifi.hase.soprafs24.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.TimerService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.WebSocketService;
import ch.uzh.ifi.hase.soprafs24.timer.TimerStrategy;
import ch.uzh.ifi.hase.soprafs24.websocket.DTO.*;
import javassist.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;

import static org.junit.jupiter.api.Assertions.*;

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
        private UserService userService;

        @Mock
        private WebSocketService webSocketService;

        @Mock
        private DTOMapper DTOMapper;

        @Mock
        private TimerService timerService;
        @Mock
        private TimerStrategy playTimerStrategy;
        @Mock
        private TimerStrategy choiceTimerStrategy;

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
        void testProcessStartGameSuccess() throws NotFoundException {
                // given
                LobbyDTO lobbyDTO = new LobbyDTO();
                lobbyDTO.setLobbyId(100L);

                Lobby lobby = new Lobby();
                lobby.setLobbyId(100L);

                String msg = "Starting game";
                GameSession dummyGame = new GameSession(lobby.getLobbyId(), lobby.getUsers());
                UserNotificationDTO dummyNotificationDTO = new UserNotificationDTO();
                dummyNotificationDTO.setSuccess(Boolean.TRUE);
                dummyNotificationDTO.setMessage(msg);

                when(lobbyService.getLobbyById(100L)).thenReturn(lobby);
                when(lobbyService.lobbyIsFull(lobby.getLobbyId())).thenReturn(true);
                when(lobbyService.rematchIsFull(lobby.getLobbyId())).thenReturn(true);
                when(gameService.startGame(lobby)).thenReturn(dummyGame);
                when(webSocketService.convertToDTO(anyString(), anyBoolean())).thenReturn(dummyNotificationDTO);

                messageController.processStartGame(lobbyDTO.getLobbyId());

                verify(gameService, times(1)).startGame(lobby);
                verify(lobbyService, times(1)).resetRematch(lobby.getLobbyId());
                verify(webSocketService, times(1))
                                .convertToDTO(msg, true);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(eq(100L), any(UserNotificationDTO.class));
        }

        @Test
        void testProcessStartGameThrowsException() throws NotFoundException {
                // given
                LobbyDTO lobbyDTO = new LobbyDTO();
                lobbyDTO.setLobbyId(100L);

                Lobby lobby = new Lobby();
                lobby.setLobbyId(100L);

                String msg = "Lobby " + lobby.getLobbyId() + " is not full yet";
                UserNotificationDTO dummyNotificationDTO = new UserNotificationDTO();
                dummyNotificationDTO.setSuccess(Boolean.FALSE);
                dummyNotificationDTO.setMessage(msg);

                // when
                when(lobbyService.getLobbyById(100L)).thenReturn(lobby);
                when(lobbyService.lobbyIsFull(lobby.getLobbyId())).thenReturn(false);
                when(webSocketService.convertToDTO(anyString(), anyBoolean())).thenReturn(dummyNotificationDTO);

                messageController.processStartGame(lobbyDTO.getLobbyId());

                verify(gameService, never()).startGame(lobby);
                verify(lobbyService, never()).resetRematch(lobby.getLobbyId());
                verify(webSocketService, times(1))
                                .convertToDTO("Error starting game: " + msg, false);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(eq(100L), any(UserNotificationDTO.class));
        }

        @Test
        void testProcessStartGameRematchThrowsException() throws NotFoundException {
                // given
                LobbyDTO lobbyDTO = new LobbyDTO();
                lobbyDTO.setLobbyId(100L);

                Lobby lobby = new Lobby();
                lobby.setLobbyId(100L);

                String msg = String.format("lobby %d: not everyone wants a rematch yet", lobby.getLobbyId());
                UserNotificationDTO dummyNotificationDTO = new UserNotificationDTO();
                dummyNotificationDTO.setSuccess(Boolean.FALSE);
                dummyNotificationDTO.setMessage(msg);

                // when
                when(lobbyService.getLobbyById(100L)).thenReturn(lobby);
                when(lobbyService.lobbyIsFull(lobby.getLobbyId())).thenReturn(true);
                when(lobbyService.rematchIsFull(lobby.getLobbyId())).thenReturn(false);
                when(webSocketService.convertToDTO(anyString(), anyBoolean())).thenReturn(dummyNotificationDTO);

                messageController.processStartGame(lobbyDTO.getLobbyId());

                verify(gameService, never()).startGame(lobby);
                verify(lobbyService, never()).resetRematch(lobby.getLobbyId());
                verify(webSocketService, times(1))
                                .convertToDTO("Error starting game: " + msg, false);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(eq(100L), any(UserNotificationDTO.class));
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
                lobby.setLobbyId(4000L);
                lobby.addUsers(10L);
                lobby.addUsers(20L);
                GameSession session = new GameSession(4000L, new ArrayList<>(lobby.getUsers()));
                when(gameService.getGameSessionById(4000L)).thenReturn(session);

                ChosenCaptureDTO chosenCaptureDTO = new ChosenCaptureDTO();
                chosenCaptureDTO.setGameId(4000L);
                List<CardDTO> chosenOption = new ArrayList<>();
                chosenOption.add(new CardDTO("COPPE", 3));
                chosenOption.add(new CardDTO("COPPE", 4));
                chosenCaptureDTO.setChosenOption(chosenOption);

                StompHeaderAccessor accessor = createHeaderAccessorWithUser(10L);

                messageController.processChooseCapture(chosenCaptureDTO, accessor);

                verify(webSocketService, atLeastOnce()).broadCastLobbyNotifications(eq(4000L), any());
                verify(webSocketService, atLeastOnce()).lobbyNotifications(eq(10L), any());
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

        // --- Test /app/quit ---
        @Test
        void testProcessQuitGame() throws Exception {
                // Given
                Long quittingUserId = 5L;
                User testUser = new User();
                testUser.setId(quittingUserId);
                testUser.setLobbyJoined(4000L);
                Long gameId = 4000L;
                GameSession game = new GameSession(gameId, new ArrayList<>());
                StompHeaderAccessor accessor = createHeaderAccessorWithUser(quittingUserId);

                QuitGameDTO dto = new QuitGameDTO();
                dto.setGameId(gameId);

                QuitGameResultDTO r1 = new QuitGameResultDTO();
                r1.setUserId(5L);
                r1.setOutcome("LOST");
                r1.setMessage("You lost by forfeit.");

                QuitGameResultDTO r2 = new QuitGameResultDTO();
                r2.setUserId(8L);
                r2.setOutcome("WON");
                r2.setMessage("You won by forfeit.");

                List<QuitGameResultDTO> results = Arrays.asList(r1, r2);

                String msg = "Lobby with id 4000 has been deleted";

                BroadcastNotificationDTO broadcastDTO = new BroadcastNotificationDTO();
                broadcastDTO.setMessage(msg);

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setMessage(msg);
                privateDTO.setSuccess(Boolean.TRUE);

                when(gameService.quitGame(gameId, quittingUserId)).thenReturn(results);
                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                when(gameService.getGameSessionById(4000L)).thenReturn(game);
                when(webSocketService.convertToDTO(anyString())).thenReturn(broadcastDTO);
                when(webSocketService.convertToDTO(msg, true)).thenReturn(privateDTO);

                messageController.processQuitGame(dto, accessor);

                verify(webSocketService, times(1))
                                .lobbyNotifications(eq(5L), eq(r1));
                verify(webSocketService, times(1))
                                .lobbyNotifications(eq(8L), eq(r2));

                verify(lobbyService, times(1)).deleteLobby(gameId);

                verify(webSocketService, times(1))
                                .convertToDTO(msg);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(anyLong(), any(BroadcastNotificationDTO.class));
                verify(webSocketService, times(1))
                                .convertToDTO(msg, true);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(anyLong(), any(UserNotificationDTO.class));
        }

        @Test
        void testProcessQuit() throws Exception {
                // Given
                Long quittingUserId = 5L;
                User testUser = new User();
                testUser.setId(quittingUserId);
                testUser.setLobbyJoined(4000L);
                Long gameId = 4000L;
                GameSession game = new GameSession(gameId, new ArrayList<>());
                StompHeaderAccessor accessor = createHeaderAccessorWithUser(quittingUserId);

                QuitGameDTO dto = new QuitGameDTO();
                dto.setGameId(gameId);

                String msg = "Lobby with id 4000 has been deleted";

                BroadcastNotificationDTO broadcastDTO = new BroadcastNotificationDTO();
                broadcastDTO.setMessage(msg);

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setMessage(msg);
                privateDTO.setSuccess(Boolean.TRUE);

                // when
                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                when(gameService.getGameSessionById(anyLong())).thenReturn(null);
                when(webSocketService.convertToDTO(anyString())).thenReturn(broadcastDTO);
                when(webSocketService.convertToDTO(msg, true)).thenReturn(privateDTO);

                messageController.processQuitGame(dto, accessor);

                verify(gameService, never()).quitGame(anyLong(), anyLong());
                verify(lobbyService, times(1)).deleteLobby(gameId);
                verify(webSocketService, times(1))
                                .convertToDTO(msg);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(anyLong(), any(BroadcastNotificationDTO.class));
                verify(webSocketService, times(1))
                                .convertToDTO(msg, true);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(anyLong(), any(UserNotificationDTO.class));
        }

        @Test
        void testProcessQuitThrowsException() throws Exception {
                // Given
                Long quittingUserId = 5L;
                User testUser = new User();
                testUser.setId(quittingUserId);
                testUser.setLobbyJoined(4000L);
                Long gameId = 4000L;
                GameSession game = new GameSession(gameId, new ArrayList<>());
                StompHeaderAccessor accessor = createHeaderAccessorWithUser(quittingUserId);

                QuitGameDTO dto = new QuitGameDTO();
                dto.setGameId(gameId);

                String msg = "The lobby with id 4000 was not found";

                BroadcastNotificationDTO broadcastDTO = new BroadcastNotificationDTO();
                broadcastDTO.setMessage(msg);

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setMessage(msg);
                privateDTO.setSuccess(Boolean.TRUE);

                // when
                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                doThrow(new NotFoundException(msg))
                                .when(lobbyService).deleteLobby(gameId);
                when(gameService.getGameSessionById(anyLong())).thenReturn(null);
                when(webSocketService.convertToDTO(msg, false)).thenReturn(privateDTO);

                messageController.processQuitGame(dto, accessor);

                verify(gameService, never()).quitGame(anyLong(), anyLong());
                verify(lobbyService, times(1)).deleteLobby(gameId);
                verify(webSocketService, never())
                                .convertToDTO(msg);
                verify(webSocketService, never())
                                .broadCastLobbyNotifications(anyLong(), any(BroadcastNotificationDTO.class));
                verify(webSocketService, times(1))
                                .convertToDTO(msg, false);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(anyLong(), any(UserNotificationDTO.class));
        }

        @Test
        void testRematchSuccess() throws NotFoundException {
                // given
                Long userId = 1L;
                User testUser = new User();
                testUser.setId(userId);
                testUser.setLobbyJoined(1000L);

                Long lobbyId = 1000L;
                Lobby testLobby = new Lobby();
                testLobby.setLobbyId(lobbyId);
                testLobby.setUser(testUser);

                LobbyDTO lobbyDTO = new LobbyDTO();
                lobbyDTO.setLobbyId(lobbyId);
                lobbyDTO.setHostId(userId);

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setSuccess(true);
                privateDTO.setMessage("Rematcher has been added to the lobby");

                StompHeaderAccessor accessor = createHeaderAccessorWithUser(userId);

                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                when(lobbyService.getLobbyById(lobbyId)).thenReturn(testLobby);
                when(webSocketService.convertToDTO(anyString(), anyBoolean())).thenReturn(privateDTO);

                messageController.rematch(accessor);

                // verify
                verify(lobbyService, times(1))
                                .addRematcher(lobbyId, userId);
                verify(webSocketService, times(1))
                                .convertToDTO(anyString(), anyBoolean());
                verify(webSocketService, times(1))
                                .lobbyNotifications(eq(userId), eq(privateDTO));
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(anyLong(), any(LobbyDTO.class));

        }

        @Test
        void testRematchNotFound() throws NotFoundException {
                // given
                Long userId = 1L;
                User testUser = new User();
                testUser.setId(userId);
                testUser.setLobbyJoined(1000L);

                Long lobbyId = 1000L;
                Lobby testLobby = new Lobby();
                testLobby.setLobbyId(lobbyId);
                testLobby.setUser(testUser);

                LobbyDTO lobbyDTO = new LobbyDTO();
                lobbyDTO.setLobbyId(lobbyId);
                lobbyDTO.setHostId(userId);

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setSuccess(false);
                privateDTO.setMessage("No lobby with id 1000L found");

                StompHeaderAccessor accessor = createHeaderAccessorWithUser(userId);

                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                when(lobbyService.getLobbyById(lobbyId)).thenReturn(testLobby);
                doThrow(new NotFoundException("No lobby with id 1000L found")).when(lobbyService)
                                .addRematcher(anyLong(), anyLong());
                when(webSocketService.convertToDTO(anyString(), anyBoolean())).thenReturn(privateDTO);

                messageController.rematch(accessor);

                // verify
                verify(lobbyService, times(1))
                                .addRematcher(anyLong(), anyLong());
                verify(webSocketService, times(1))
                                .convertToDTO(anyString(), anyBoolean());
                verify(webSocketService, times(1))
                                .lobbyNotifications(eq(userId), eq(privateDTO));
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(anyLong(), any(LobbyDTO.class));

        }

        @Test
        void testProcessPlayCardExceptionIsCaught() {
                PlayCardDTO dto = new PlayCardDTO();
                dto.setLobbyId(123L);
                dto.setCard(new CardDTO("DENARI", 5));
                StompHeaderAccessor acc = createHeaderAccessorWithUser(42L);

                when(gameService.playCard(eq(123L), any(CardDTO.class), eq(42L)))
                                .thenThrow(new RuntimeException("boom"));

                assertDoesNotThrow(() -> messageController.processPlayCard(dto, acc));

                verify(webSocketService, never())
                                .broadCastLobbyNotifications(eq(123L), any(MoveActionDTO.class));
        }

        @Test
        public void testProcessPlayCardEmitsMoveActionDTO() {
                GameSession session = spy(new GameSession(500L, List.of(77L)));
                Player player = new Player(77L, new ArrayList<>());

                Card lastPlayed = CardFactory.getCard(Suit.COPPE, 2);
                List<Card> lastPicked = List.of(CardFactory.getCard(Suit.DENARI, 7));

                doReturn(lastPlayed).when(session).getLastCardPlayed();
                doReturn(lastPicked).when(session).getLastCardPickedCards();

                when(gameService.playCard(eq(500L), any(CardDTO.class), eq(77L)))
                                .thenReturn(Pair.of(session, player));

                PlayCardDTO dto = new PlayCardDTO();
                dto.setLobbyId(500L);
                dto.setCard(new CardDTO("COPPE", 2));

                StompHeaderAccessor acc = createHeaderAccessorWithUser(77L);
                messageController.processPlayCard(dto, acc);

                verify(webSocketService).broadCastLobbyNotifications(eq(500L), any(MoveActionDTO.class));
                verify(webSocketService).lobbyNotifications(eq(77L), any(PrivatePlayerDTO.class));
                verify(webSocketService, atLeastOnce())
                                .broadCastLobbyNotifications(eq(500L), any(GameSessionDTO.class));
        }

        @Test
        public void testProcessChooseCaptureEmitsMoveActionDTO() {
                GameSession session = spy(new GameSession(600L, List.of(33L)));

                Card lastPlayed = CardFactory.getCard(Suit.SPADE, 3);
                List<Card> lastPicked = List.of(CardFactory.getCard(Suit.BASTONI, 4));

                doReturn(lastPlayed).when(session).getLastCardPlayed();
                doReturn(lastPicked).when(session).getLastCardPickedCards();

                when(gameService.getGameSessionById(600L)).thenReturn(session);

                ChosenCaptureDTO cap = new ChosenCaptureDTO();
                cap.setGameId(600L);
                cap.setChosenOption(List.of(new CardDTO("SPADE", 3)));

                StompHeaderAccessor acc = createHeaderAccessorWithUser(33L);
                messageController.processChooseCapture(cap, acc);

                verify(webSocketService).broadCastLobbyNotifications(eq(600L), any(MoveActionDTO.class));
                verify(webSocketService).broadCastLobbyNotifications(eq(600L), any(GameSessionDTO.class));
                verify(webSocketService).lobbyNotifications(eq(33L), any(PrivatePlayerDTO.class));
        }

}

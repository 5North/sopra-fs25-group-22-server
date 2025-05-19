package ch.uzh.ifi.hase.soprafs24.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.Table;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.TimeLeftDTO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

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
                // Given
                Long lobbyId = 100L;
                Lobby lobby = new Lobby();
                lobby.setLobbyId(lobbyId);
                lobby.addUser(1L);
                lobby.addUser(2L);

                when(lobbyService.checkIfLobbyExists(lobbyId)).thenReturn(lobby);
                when(lobbyService.lobbyIsFull(lobbyId)).thenReturn(true);
                when(lobbyService.rematchIsFull(lobbyId)).thenReturn(true);

                GameSession dummySession = Mockito.mock(GameSession.class);
                when(gameService.startGame(lobby)).thenReturn(dummySession);

                UserNotificationDTO notificationDTO = new UserNotificationDTO();
                notificationDTO.setSuccess(true);
                notificationDTO.setMessage("Starting game");
                when(webSocketService.convertToDTO("Starting game", true))
                                .thenReturn(notificationDTO);

                // When
                messageController.processStartGame(lobbyId);

                // Then
                verify(gameService, times(1)).startGame(lobby);
                verify(lobbyService, times(1)).resetRematch(lobbyId);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(eq(lobbyId), any(UserNotificationDTO.class));
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
                when(lobbyService.checkIfLobbyExists(100L)).thenReturn(lobby);
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
                when(lobbyService.checkIfLobbyExists(100L)).thenReturn(lobby);
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
        void testGameUpdateRequest() throws NotFoundException {
                Long userId = 1L;
                StompHeaderAccessor headerAccessor = createHeaderAccessorWithUser(userId);
                LobbyDTO lobbyDTO = new LobbyDTO();
                lobbyDTO.setLobbyId(100L);

                Lobby lobby = new Lobby();
                lobby.setLobbyId(100L);
                lobby.addUser(1L);
                lobby.addUser(2L);

                GameSession session = new GameSession(100L, new ArrayList<>(lobby.getUsers()));
                when(gameService.getGameSessionById(100L)).thenReturn(session);

                messageController.receiveUpdateGame(session.getGameId(), headerAccessor);

                verify(webSocketService, times(1)).sentLobbyNotifications(anyLong(), any(PrivatePlayerDTO.class));
                verify(webSocketService, times(1)).sentLobbyNotifications(anyLong(), any(GameSessionDTO.class));
        }

        @Test
        void testGameUpdateRequest_noGameSession() throws NotFoundException {
                Long userId = 1L;
                StompHeaderAccessor headerAccessor = createHeaderAccessorWithUser(userId);
                LobbyDTO lobbyDTO = new LobbyDTO();
                lobbyDTO.setLobbyId(1000L);

                Lobby lobby = new Lobby();
                lobby.setLobbyId(1000L);
                lobby.addUser(1L);
                lobby.addUser(2L);

                BroadcastNotificationDTO notificationDTO = new BroadcastNotificationDTO();
                notificationDTO.setMessage("Error updating game: Game with id 1000L does not exist");

                when(gameService.getGameSessionById(1000L))
                                .thenThrow(new NoSuchElementException("Game with id 1000L does not exist"));
                when(webSocketService.convertToDTO("Error updating game: Game with id 1000L does not exist"))
                                .thenReturn(notificationDTO);

                messageController.receiveUpdateGame(lobbyDTO.getLobbyId(), headerAccessor);

                verify(webSocketService, times(1)).sentLobbyNotifications(anyLong(),
                                any(BroadcastNotificationDTO.class));
        }

        // --- Test /app/playCard ---
        @Test
        void testProcessPlayCard() {
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
                verify(webSocketService, atLeastOnce()).sentLobbyNotifications(eq(100L), any());
                verify(gameService, atLeastOnce()).isGameOver(300L);
        }

        // --- Test /app/chooseCapture ---
        @Test
        void testProcessChooseCapture() {
                Lobby lobby = new Lobby();
                lobby.setLobbyId(4000L);
                lobby.addUser(10L);
                lobby.addUser(20L);
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
                verify(webSocketService, atLeastOnce()).sentLobbyNotifications(eq(10L), any());
        }

        // --- Test /app/ai ---
        @Test
        void testProcessAISuggestion() {
                AiRequestDTO aiReq = new AiRequestDTO();
                aiReq.setGameId(123L);

                StompHeaderAccessor accessor = createHeaderAccessorWithUser(42L);

                AISuggestionDTO expectedDto = new AISuggestionDTO("DENARI-7; COPPE-4");
                when(gameService.aiSuggestion(123L, 42L)).thenReturn(expectedDto);

                messageController.processAISuggestion(aiReq, accessor);

                verify(webSocketService, times(1))
                                .sentLobbyNotifications(42L, expectedDto);
        }

        // --- Test /app/quit ---
        @Test
        void testProcessQuitGame() throws Exception {
                // Given
                Long quittingUserId = 5L;
                User testUser = new User();
                testUser.setId(quittingUserId);
                testUser.setLobbyJoined(4000L);

                Lobby lobby = new Lobby();
                lobby.setLobbyId(4000L);
                lobby.addUser(5L);
                lobby.addUser(8L);

                GameSession game = Mockito.mock(GameSession.class);

                QuitGameDTO dto = new QuitGameDTO();
                dto.setGameId(4000L);

                QuitGameResultDTO r1 = new QuitGameResultDTO();
                r1.setUserId(5L);
                r1.setOutcome("LOST");
                r1.setMessage("You lost by forfeit.");

                QuitGameResultDTO r2 = new QuitGameResultDTO();
                r2.setUserId(8L);
                r2.setOutcome("WON");
                r2.setMessage("You won by forfeit.");

                List<QuitGameResultDTO> results = Arrays.asList(r1, r2);

                BroadcastNotificationDTO broadcastDTO = new BroadcastNotificationDTO();
                broadcastDTO.setMessage("Lobby with id 4000 has been deleted");

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setMessage("Lobby with id 4000 has been deleted");
                privateDTO.setSuccess(true);

                when(userService.checkIfUserExists(quittingUserId)).thenReturn(testUser);
                when(gameService.getGameSessionById(4000L)).thenReturn(game);
                when(gameService.quitGame(4000L, quittingUserId)).thenReturn(results);
                when(webSocketService.convertToDTO(anyString())).thenReturn(broadcastDTO);
                when(webSocketService.convertToDTO(anyString(), eq(true))).thenReturn(privateDTO);

                // When
                messageController.processQuitGame(dto, createHeaderAccessorWithUser(quittingUserId));

                // Then:
                verify(webSocketService, times(1)).sentLobbyNotifications(5L, r1);
                verify(webSocketService, times(1)).sentLobbyNotifications(8L, r2);

                verify(lobbyService, times(1)).deleteLobby(4000L);
                verify(webSocketService, times(1))
                                .broadCastLobbyNotifications(eq(4000L), any(BroadcastNotificationDTO.class));
                verify(webSocketService, times(1))
                                .sentLobbyNotifications(eq(quittingUserId), any(UserNotificationDTO.class));
        }

        @Test
        void testProcessQuit_NoGameSession() throws Exception {
                // Given
                Long quittingUserId = 5L;
                User testUser = new User();
                testUser.setId(quittingUserId);
                testUser.setLobbyJoined(4000L);

                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                when(gameService.getGameSessionById(anyLong())).thenReturn(null);

                BroadcastNotificationDTO broadcastDTO = new BroadcastNotificationDTO();
                broadcastDTO.setMessage("Lobby with id 4000 has been deleted");
                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setMessage("Lobby with id 4000 has been deleted");
                privateDTO.setSuccess(true);

                when(webSocketService.convertToDTO(anyString())).thenReturn(broadcastDTO);
                when(webSocketService.convertToDTO(anyString(), eq(true))).thenReturn(privateDTO);

                QuitGameDTO dto = new QuitGameDTO();
                dto.setGameId(4000L);

                // When
                messageController.processQuitGame(dto, createHeaderAccessorWithUser(quittingUserId));

                // Then
                verify(gameService, never()).quitGame(anyLong(), anyLong());
                verify(lobbyService, times(1)).deleteLobby(4000L);
                verify(webSocketService, times(1)).broadCastLobbyNotifications(eq(4000L),
                                any(BroadcastNotificationDTO.class));
                verify(webSocketService, times(1)).sentLobbyNotifications(eq(quittingUserId),
                                any(UserNotificationDTO.class));
        }

        @Test
        void testProcessQuit_DeletionFails() throws Exception {
                // Given
                Long quittingUserId = 5L;
                User testUser = new User();
                testUser.setId(quittingUserId);
                testUser.setLobbyJoined(4000L);

                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                when(gameService.getGameSessionById(anyLong())).thenReturn(null);
                doThrow(new NotFoundException("not found")).when(lobbyService).deleteLobby(4000L);

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setMessage("The lobby with id 4000 was not found");
                privateDTO.setSuccess(false);
                when(webSocketService.convertToDTO("The lobby with id 4000 was not found", false))
                                .thenReturn(privateDTO);

                QuitGameDTO dto = new QuitGameDTO();
                dto.setGameId(4000L);

                // When
                messageController.processQuitGame(dto, createHeaderAccessorWithUser(quittingUserId));

                // Then
                verify(lobbyService, times(1)).deleteLobby(4000L);
                verify(webSocketService, never()).broadCastLobbyNotifications(anyLong(), any());
                verify(webSocketService, times(1))
                                .sentLobbyNotifications(eq(quittingUserId), any(UserNotificationDTO.class));
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
                testLobby.addUser(userId);

                UserNotificationDTO privateDTO = new UserNotificationDTO();
                privateDTO.setSuccess(true);
                privateDTO.setMessage("Rematcher has been added to the lobby");

                List<Long> rematchers = new ArrayList<>();
                rematchers.add(userId);

                LobbyDTO rematchDTO = new LobbyDTO();
                rematchDTO.setUsersIds(rematchers);
                rematchDTO.setLobbyId(lobbyId);
                rematchDTO.setHostId(userId);
                rematchDTO.setRematchersIds(rematchers);

                StompHeaderAccessor accessor = createHeaderAccessorWithUser(userId);

                when(userService.checkIfUserExists(anyLong())).thenReturn(testUser);
                when(lobbyService.checkIfLobbyExists(lobbyId)).thenReturn(testLobby);
                when(webSocketService.convertToDTO("Rematcher has been added to the lobby", true))
                                .thenReturn(privateDTO);
                doAnswer(new Answer<Void>() {
                        @Override
                        public Void answer(InvocationOnMock invocation) throws Throwable {
                                testLobby.addRematcher(userId);
                                return null;
                        }
                }).when(lobbyService).addRematcher(lobbyId, userId);

                messageController.rematch(accessor);

                // Captor DTOs and verify
                verify(lobbyService, times(1))
                                .addRematcher(lobbyId, userId);

                verify(webSocketService, times(1)).sentLobbyNotifications(eq(userId), any(UserNotificationDTO.class));

                verify(webSocketService, times(1))
                                .convertToDTO(anyString(), anyBoolean());

                ArgumentCaptor<LobbyDTO> broadcastCaptor = ArgumentCaptor.forClass(LobbyDTO.class);
                verify(webSocketService, times(1)).broadCastLobbyNotifications(eq(lobbyId), broadcastCaptor.capture());

                LobbyDTO actualBroadcastDTO = broadcastCaptor.getValue();
                assertEquals(rematchDTO.getLobbyId(), actualBroadcastDTO.getLobbyId());
                assertEquals(rematchDTO.getUsersIds(), actualBroadcastDTO.getUsersIds());
                assertEquals(rematchDTO.getHostId(), actualBroadcastDTO.getHostId());
                assertEquals(rematchDTO.getRematchersIds(), actualBroadcastDTO.getRematchersIds());

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
                when(lobbyService.checkIfLobbyExists(lobbyId)).thenReturn(testLobby);
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
                                .sentLobbyNotifications(userId, privateDTO);
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
        void testProcessPlayCardEmitsMoveActionDTO() {
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
                verify(webSocketService).sentLobbyNotifications(eq(77L), any(PrivatePlayerDTO.class));
                verify(webSocketService, atLeastOnce())
                                .broadCastLobbyNotifications(eq(500L), any(GameSessionDTO.class));
        }

        @Test
        void testProcessChooseCaptureEmitsMoveActionDTO() {
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
                verify(webSocketService).sentLobbyNotifications(eq(33L), any(PrivatePlayerDTO.class));
        }

        @Test
        void testReceiveUpdateGame_WhenChoosing_SendsCaptureOptions() throws NotFoundException {
                Long userId = 42L;
                StompHeaderAccessor header = createHeaderAccessorWithUser(userId);

                // spy of session
                GameSession session = spy(new GameSession(123L, List.of(userId, 99L)));

                // mock of player that has the turn
                Player mockPlayer = mock(Player.class);
                doReturn(userId).when(mockPlayer).getUserId();
                doReturn(mockPlayer).when(session).getCurrentPlayer();
                doReturn(true).when(session).isChoosing();

                // mock of table and capture options
                Table mockTable = mock(Table.class);
                Card lastPlayed = CardFactory.getCard(Suit.COPPE, 5);
                List<List<Card>> opts = List.of(List.of(lastPlayed));
                when(mockTable.getCaptureOptions(lastPlayed)).thenReturn(opts);
                doReturn(mockTable).when(session).getTable();
                doReturn(lastPlayed).when(session).getLastCardPlayed();

                when(gameService.getGameSessionById(123L)).thenReturn(session);

                messageController.receiveUpdateGame(123L, header);

                verify(webSocketService).sentLobbyNotifications(eq(userId), any(PrivatePlayerDTO.class));
                verify(webSocketService).sentLobbyNotifications(eq(userId), any(GameSessionDTO.class));
                // in choosing mode send options
                verify(webSocketService).sentLobbyNotifications(eq(userId), anyList());
                // timerService get interrogated 2 times: remChoice and remPlay
                verify(timerService, times(2))
                                .getRemainingSeconds(eq(123L), any());
        }

        @Test
        void testProcessAISuggestionExceptionIsCaught() {
                AiRequestDTO aiReq = new AiRequestDTO();
                aiReq.setGameId(123L);
                StompHeaderAccessor accessor = createHeaderAccessorWithUser(42L);

                when(gameService.aiSuggestion(123L, 42L))
                                .thenThrow(new RuntimeException("AI service down"));

                assertDoesNotThrow(() -> messageController.processAISuggestion(aiReq, accessor));

                verify(webSocketService, never())
                                .sentLobbyNotifications(anyLong(), any(AISuggestionDTO.class));
        }

        @Test
        void testProcessPlayCardWhenChoosing_SendsChoiceTime() {
                Long userId = 101L;
                Long gameId = 500L;
                PlayCardDTO dto = new PlayCardDTO();
                dto.setLobbyId(gameId);
                dto.setCard(new CardDTO("COPPE", 5));
                StompHeaderAccessor accessor = createHeaderAccessorWithUser(userId);

                GameSession session = mock(GameSession.class);
                when(session.isChoosing()).thenReturn(true);

                Player player = new Player(userId, new ArrayList<>());
                when(gameService.playCard(eq(gameId), any(CardDTO.class), eq(userId)))
                                .thenReturn(Pair.of(session, player));

                when(timerService.getRemainingSeconds(eq(gameId), any()))
                                .thenReturn(7L);

                messageController.processPlayCard(dto, accessor);

                verify(timerService).getRemainingSeconds(eq(gameId), any());
                verify(webSocketService)
                                .broadCastLobbyNotifications(eq(gameId), any(TimeLeftDTO.class));

                verify(webSocketService, never())
                                .sentLobbyNotifications(eq(userId), any(PrivatePlayerDTO.class));
                verify(webSocketService, never())
                                .broadCastLobbyNotifications(eq(gameId), any(MoveActionDTO.class));
        }

        @Test
        void testReceiveUpdateGame_RemChoicePositive_SendsChoiceTime() throws NotFoundException {
                Long userId = 55L;
                Long gameId = 600L;
                StompHeaderAccessor header = createHeaderAccessorWithUser(userId);

                GameSession session = new GameSession(gameId, List.of(userId, 99L));
                when(gameService.getGameSessionById(gameId)).thenReturn(session);

                when(timerService.getRemainingSeconds(eq(gameId), any()))
                                .thenReturn(12L);

                messageController.receiveUpdateGame(gameId, header);

                verify(timerService).getRemainingSeconds(eq(gameId), any());
                verify(webSocketService)
                                .sentLobbyNotifications(eq(userId), any(TimeLeftDTO.class));
        }

        @Test
        void testReceiveUpdateGame_RemChoiceZero_SendsPlayTime() throws NotFoundException {
                Long userId = 66L;
                Long gameId = 700L;
                StompHeaderAccessor header = createHeaderAccessorWithUser(userId);

                GameSession session = new GameSession(gameId, List.of(userId, 99L));
                when(gameService.getGameSessionById(gameId)).thenReturn(session);

                when(timerService.getRemainingSeconds(eq(gameId), any()))
                                .thenReturn(0L)
                                .thenReturn(15L);

                messageController.receiveUpdateGame(gameId, header);

                verify(timerService, times(2))
                                .getRemainingSeconds(eq(gameId), any());
                verify(webSocketService)
                                .sentLobbyNotifications(eq(userId), any(TimeLeftDTO.class));
        }

        @Test
        void testProcessPlayCard_MultipleOptions_ControllerOnlySendsChoiceTimer() {
                Long userId = 42L;
                Long gameId = 777L;
                PlayCardDTO dto = new PlayCardDTO();
                dto.setLobbyId(gameId);
                dto.setCard(new CardDTO("DENARI", 9));
                StompHeaderAccessor acc = createHeaderAccessorWithUser(userId);

                GameSession session = mock(GameSession.class);
                when(session.isChoosing()).thenReturn(true);
                Player current = new Player(userId, new ArrayList<>());
                when(gameService.playCard(eq(gameId), any(CardDTO.class), eq(userId)))
                                .thenReturn(Pair.of(session, current));

                when(timerService.getRemainingSeconds(eq(gameId), any()))
                                .thenReturn(12L);

                messageController.processPlayCard(dto, acc);

                verify(webSocketService, never())
                                .sentLobbyNotifications(eq(userId), any(PrivatePlayerDTO.class));

                verify(webSocketService).broadCastLobbyNotifications(eq(gameId), any(TimeLeftDTO.class));
        }

}

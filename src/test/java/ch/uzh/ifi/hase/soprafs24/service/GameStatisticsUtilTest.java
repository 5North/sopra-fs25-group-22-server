package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.result.Outcome;
import ch.uzh.ifi.hase.soprafs24.game.result.Result;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GameStatisticsUtilTest {

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        new GameStatisticsUtil(userRepository);
    }

    private Player createDummyPlayer(Long userId) {
        return new Player(userId, new ArrayList<>());
    }

    private User createDummyUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);
        return user;
    }

    @Test
    public void testUpdateUserStatistics_WinLoss() {
        Player p1 = createDummyPlayer(1L);
        Player p2 = createDummyPlayer(2L);
        Player p3 = createDummyPlayer(3L);
        Player p4 = createDummyPlayer(4L);
        List<Player> players = Arrays.asList(p1, p2, p3, p4);

        Result result = new Result(100L, players);
        result.getTeam1().setOutcome(Outcome.WON);
        result.getTeam2().setOutcome(Outcome.LOST);

        User user1 = createDummyUser(1L);
        User user2 = createDummyUser(2L);
        User user3 = createDummyUser(3L);
        User user4 = createDummyUser(4L);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        Mockito.when(userRepository.findById(4L)).thenReturn(Optional.of(user4));

        GameStatisticsUtil.updateUserStatistics(result);

        assertEquals(1, user1.getWinCount(), "User1 winCount should be incremented.");
        assertEquals(1, user3.getWinCount(), "User3 winCount should be incremented.");

        assertEquals(1, user2.getLossCount(), "User2 lossCount should be incremented.");
        assertEquals(1, user4.getLossCount(), "User4 lossCount should be incremented.");

        assertEquals(0, user1.getTieCount());
        assertEquals(0, user2.getTieCount());
        assertEquals(0, user3.getTieCount());
        assertEquals(0, user4.getTieCount());

        Mockito.verify(userRepository, Mockito.times(4)).save(Mockito.any(User.class));
    }

    @Test
    public void testUpdateUserStatistics_Tie() {
        Player p1 = createDummyPlayer(1L);
        Player p2 = createDummyPlayer(2L);
        Player p3 = createDummyPlayer(3L);
        Player p4 = createDummyPlayer(4L);
        List<Player> players = Arrays.asList(p1, p2, p3, p4);

        Result result = new Result(101L, players);
        result.getTeam1().setOutcome(Outcome.TIE);
        result.getTeam2().setOutcome(Outcome.TIE);

        User user1 = createDummyUser(1L);
        User user2 = createDummyUser(2L);
        User user3 = createDummyUser(3L);
        User user4 = createDummyUser(4L);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        Mockito.when(userRepository.findById(4L)).thenReturn(Optional.of(user4));

        GameStatisticsUtil.updateUserStatistics(result);

        assertEquals(1, user1.getTieCount(), "User1 tieCount should be incremented to 1.");
        assertEquals(1, user2.getTieCount(), "User2 tieCount should be incremented to 1.");
        assertEquals(1, user3.getTieCount(), "User3 tieCount should be incremented to 1.");
        assertEquals(1, user4.getTieCount(), "User4 tieCount should be incremented to 1.");

        assertEquals(0, user1.getWinCount());
        assertEquals(0, user2.getLossCount());
        assertEquals(0, user3.getWinCount());
        assertEquals(0, user4.getLossCount());

        Mockito.verify(userRepository, Mockito.times(4)).save(Mockito.any(User.class));
    }

    @Test
    public void testUpdateUserStatistics_UserNotFound() {
        Player p1 = createDummyPlayer(1L);
        Player p2 = createDummyPlayer(2L);
        Player p3 = createDummyPlayer(3L);
        Player p4 = createDummyPlayer(4L);
        List<Player> players = Arrays.asList(p1, p2, p3, p4);

        Result result = new Result(300L, players);
        result.getTeam1().setOutcome(Outcome.WON);
        result.getTeam2().setOutcome(Outcome.WON);

        User user2 = createDummyUser(2L);
        User user3 = createDummyUser(3L);
        User user4 = createDummyUser(4L);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.empty());
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        Mockito.when(userRepository.findById(4L)).thenReturn(Optional.of(user4));

        GameStatisticsUtil.updateUserStatistics(result);

        Mockito.verify(userRepository, Mockito.times(3)).save(Mockito.any(User.class));

        assertEquals(1, user2.getWinCount(), "User2 winCount should be incremented.");
        assertEquals(1, user3.getWinCount(), "User3 winCount should be incremented.");
        assertEquals(1, user4.getWinCount(), "User4 winCount should be incremented.");
    }
}

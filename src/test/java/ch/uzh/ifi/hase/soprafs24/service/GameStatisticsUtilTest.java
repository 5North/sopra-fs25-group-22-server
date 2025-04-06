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

    // Poiché GameStatisticsUtil ha un campo statico per il repository, lo
    // inizializziamo nel setup
    @BeforeEach
    public void setup() {
        new GameStatisticsUtil(userRepository);
    }

    // Helper per creare un dummy Player con solo l'ID (la mano non è rilevante per
    // questi test)
    private Player createDummyPlayer(Long userId) {
        return new Player(userId, new ArrayList<>());
    }

    // Helper per creare un dummy User con contatori iniziali a 0
    private User createDummyUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setWinCount(0);
        user.setLossCount(0);
        user.setTieCount(0);
        return user;
    }

    /**
     * Test per il caso in cui il team vincente (team1) ottiene outcome WON e
     * l'altro (team2) outcome LOST.
     * In questo scenario, per ogni utente del team1 il winCount deve essere
     * incrementato (da 0 a 1),
     * mentre per ogni utente del team2 il lossCount deve essere incrementato.
     */
    @Test
    public void testUpdateUserStatistics_WinLoss() {
        // Creiamo 4 dummy Player con ID specifici
        Player p1 = createDummyPlayer(1L);
        Player p2 = createDummyPlayer(2L);
        Player p3 = createDummyPlayer(3L);
        Player p4 = createDummyPlayer(4L);
        List<Player> players = Arrays.asList(p1, p2, p3, p4);

        // Crea un Result (usando il costruttore di Result) e poi forziamo manualmente
        // gli outcome:
        Result result = new Result(100L, players);
        result.getTeam1().setOutcome(Outcome.WON);
        result.getTeam2().setOutcome(Outcome.LOST);

        // Prepara dummy User per ciascun player e configura il repository per ritornare
        // questi utenti.
        User user1 = createDummyUser(1L);
        User user2 = createDummyUser(2L);
        User user3 = createDummyUser(3L);
        User user4 = createDummyUser(4L);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        Mockito.when(userRepository.findById(4L)).thenReturn(Optional.of(user4));

        // Chiamiamo il metodo che aggiorna le statistiche
        GameStatisticsUtil.updateUserStatistics(result);

        // Verifichiamo: per il team vincente (team1: p1 e p3), winCount deve essere
        // incrementato.
        assertEquals(1, user1.getWinCount(), "User1 winCount should be incremented.");
        assertEquals(1, user3.getWinCount(), "User3 winCount should be incremented.");

        // Per il team perdente (team2: p2 e p4), lossCount deve essere incrementato.
        assertEquals(1, user2.getLossCount(), "User2 lossCount should be incremented.");
        assertEquals(1, user4.getLossCount(), "User4 lossCount should be incremented.");

        // In questo scenario tieCount non viene modificato.
        assertEquals(0, user1.getTieCount());
        assertEquals(0, user2.getTieCount());
        assertEquals(0, user3.getTieCount());
        assertEquals(0, user4.getTieCount());

        // Verifica che il repository.save() sia stato chiamato per ciascun utente.
        Mockito.verify(userRepository, Mockito.times(4)).save(Mockito.any(User.class));
    }

    /**
     * Test per il caso di pareggio: tutti i team hanno outcome TIE.
     * In questo scenario, per ogni utente deve essere incrementato il tieCount.
     */
    @Test
    public void testUpdateUserStatistics_Tie() {
        // Creiamo 4 dummy Player
        Player p1 = createDummyPlayer(1L);
        Player p2 = createDummyPlayer(2L);
        Player p3 = createDummyPlayer(3L);
        Player p4 = createDummyPlayer(4L);
        List<Player> players = Arrays.asList(p1, p2, p3, p4);

        // Creiamo un Result e forziamo outcome TIE per entrambi i team.
        Result result = new Result(101L, players);
        result.getTeam1().setOutcome(Outcome.TIE);
        result.getTeam2().setOutcome(Outcome.TIE);

        // Creiamo dummy User per ciascun player.
        User user1 = createDummyUser(1L);
        User user2 = createDummyUser(2L);
        User user3 = createDummyUser(3L);
        User user4 = createDummyUser(4L);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        Mockito.when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        Mockito.when(userRepository.findById(4L)).thenReturn(Optional.of(user4));

        // Chiamiamo il metodo di aggiornamento
        GameStatisticsUtil.updateUserStatistics(result);

        // Per un pareggio, tieCount deve essere incrementato per ogni utente.
        assertEquals(1, user1.getTieCount(), "User1 tieCount should be incremented to 1.");
        assertEquals(1, user2.getTieCount(), "User2 tieCount should be incremented to 1.");
        assertEquals(1, user3.getTieCount(), "User3 tieCount should be incremented to 1.");
        assertEquals(1, user4.getTieCount(), "User4 tieCount should be incremented to 1.");

        // WinCount e LossCount non devono essere modificate.
        assertEquals(0, user1.getWinCount());
        assertEquals(0, user2.getLossCount());
        assertEquals(0, user3.getWinCount());
        assertEquals(0, user4.getLossCount());

        Mockito.verify(userRepository, Mockito.times(4)).save(Mockito.any(User.class));
    }
}

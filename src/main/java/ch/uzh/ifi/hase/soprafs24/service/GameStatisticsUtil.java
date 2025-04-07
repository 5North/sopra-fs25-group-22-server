package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.game.result.Outcome;
import ch.uzh.ifi.hase.soprafs24.game.result.Result;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameStatisticsUtil {

    private static UserRepository userRepository;

    @Autowired
    public GameStatisticsUtil(UserRepository userRepository) {
        GameStatisticsUtil.userRepository = userRepository;
    }

    /**
     * Updates user statistics (winCount, lossCount, tieCount) based on the result
     * of the game.
     *
     * @param result the Result object calculated from the GameSession.
     */
    public static void updateUserStatistics(Result result) {
        updateTeamStats(result.getTeam1().getPlayerIds(), result.getTeam1().getOutcome());
        updateTeamStats(result.getTeam2().getPlayerIds(), result.getTeam2().getOutcome());
    }

    private static void updateTeamStats(List<Long> userIds, Outcome outcome) {
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                switch (outcome) {
                    case WON:
                        user.incrementWinCount();
                        break;
                    case LOST:
                        user.incrementLossCount();
                        break;
                    case TIE:
                        user.incrementTieCount();
                        break;
                }
                userRepository.save(user);
            }
        }
    }
}

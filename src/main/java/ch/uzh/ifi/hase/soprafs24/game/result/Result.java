package ch.uzh.ifi.hase.soprafs24.game.result;

import java.util.List;
import ch.uzh.ifi.hase.soprafs24.game.Player;

public class Result {
    private Long gameId;
    private TeamResult team1;
    private TeamResult team2;

    public Result(Long gameId, List<Player> players) {
        if (players.size() != 4) {
            throw new IllegalArgumentException("There must be exactly 4 players.");
        }
        this.gameId = gameId;
        this.team1 = new TeamResult(players.get(0), players.get(2));
        this.team2 = new TeamResult(players.get(1), players.get(3));

        team1.setComparisonResults(team2.getPrimieraRaw());
        team2.setComparisonResults(team1.getPrimieraRaw());

        if (team1.getTotal() > team2.getTotal()) {
            team1.setOutcome(Outcome.WON);
            team2.setOutcome(Outcome.LOST);
        } else if (team1.getTotal() < team2.getTotal()) {
            team1.setOutcome(Outcome.LOST);
            team2.setOutcome(Outcome.WON);
        } else {
            team1.setOutcome(Outcome.TIE);
            team2.setOutcome(Outcome.TIE);
        }
    }

    public Long getGameId() {
        return gameId;
    }

    public TeamResult getTeam1() {
        return team1;
    }

    public TeamResult getTeam2() {
        return team2;
    }
}

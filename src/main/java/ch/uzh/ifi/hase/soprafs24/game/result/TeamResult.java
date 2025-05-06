package ch.uzh.ifi.hase.soprafs24.game.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;

public class TeamResult {
    private List<Long> playerIds;

    private int cartePointsRaw;
    private int denariPointsRaw;
    private int primieraRaw;
    private int settebelloRaw;
    private int scopaRaw;

    private int carteResult;
    private int denariResult;
    private int primieraResult;
    private int settebelloResult;
    private int scopaResult;

    private int total;
    private Outcome outcome;

    public TeamResult(Player p1, Player p2) {
        this.playerIds = new ArrayList<>();
        this.playerIds.add(p1.getUserId());
        this.playerIds.add(p2.getUserId());

        List<Card> treasure = new ArrayList<>();
        treasure.addAll(p1.getTreasure());
        treasure.addAll(p2.getTreasure());

        this.cartePointsRaw = treasure.size();
        this.denariPointsRaw = (int) treasure.stream().filter(c -> c.getSuit() == Suit.DENARI).count();
        this.primieraRaw = calculatePrimieraScore(treasure);
        this.settebelloRaw = treasure.stream().anyMatch(c -> c.getSuit() == Suit.DENARI && c.getValue() == 7) ? 1 : 0;
        this.scopaRaw = p1.getScopaCount() + p2.getScopaCount();

        this.carteResult = (cartePointsRaw > 20) ? 1 : 0;
        this.denariResult = (denariPointsRaw > 5) ? 1 : 0;
        this.settebelloResult = settebelloRaw;
        this.scopaResult = scopaRaw;
        this.primieraResult = 0;
        this.total = 0;
    }

    private int calculatePrimieraScore(List<Card> treasure) {
        Map<Suit, Integer> bestPerSuit = new HashMap<>();
        for (Card card : treasure) {
            int score = getPrimieraValue(card);
            bestPerSuit.merge(card.getSuit(), score, Math::max);
        }
        if (bestPerSuit.size() < 4)
            return 0;
        int sum = 0;
        for (Integer score : bestPerSuit.values()) {
            sum += score;
        }
        return sum;
    }

    private int getPrimieraValue(Card card) {
        int value = card.getValue();
        switch (value) {
            case 7:
                return 21;
            case 6:
                return 18;
            case 1:
                return 16;
            case 5:
                return 15;
            case 4:
                return 14;
            case 3:
                return 13;
            case 2:
                return 12;
            case 8:
                return 10;
            case 9:
                return 10;
            case 10:
                return 10;
            default:
                return 0;
        }
    }

    public void setComparisonResults(int opponentPrimieraRaw) {
        this.primieraResult = (this.primieraRaw > opponentPrimieraRaw) ? 1 : 0;
        this.total = this.carteResult + this.denariResult + this.primieraResult + this.settebelloResult
                + this.scopaResult;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    // Getters

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public int getTotal() {
        return total;
    }

    public int getCarteResult() {
        return carteResult;
    }

    public int getDenariResult() {
        return denariResult;
    }

    public int getPrimieraResult() {
        return primieraResult;
    }

    public int getSettebelloResult() {
        return settebelloResult;
    }

    public int getScopaResult() {
        return scopaResult;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public int getPrimieraRaw() {
        return primieraRaw;
    }

    public int getCartePointsRaw() {
        return cartePointsRaw;
    }

    public int getDenariPointsRaw() {
        return denariPointsRaw;
    }

    public int getSettebelloRaw() {
        return settebelloRaw;
    }

    public int getScopaRaw() {
        return scopaRaw;
    }
}

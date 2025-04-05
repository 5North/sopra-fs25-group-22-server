package ch.uzh.ifi.hase.soprafs24.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;

public class Player {

    private final Long userId;
    private final List<Card> hand;
    private final List<Card> treasure;
    private int scopaCount;

    public Player(Long userId, List<Card> initialHand) {
        this.userId = userId;
        this.hand = new ArrayList<>(initialHand);
        this.treasure = new ArrayList<>();
        this.scopaCount = 0;
    }

    // Setters

    public Card pickPlayedCard(Card playedCard) {
        if (!hand.contains(playedCard)) {
            throw new IllegalArgumentException("not present in the player's hand");
        }
        hand.remove(playedCard);
        return playedCard;
    }

    public void collectCards(List<Card> cards, boolean scopa) {
        treasure.addAll(cards);
        if (scopa) {
            scopaCount++;
        }
    }

    // Getters

    public List<Card> getTreasure() {
        return Collections.unmodifiableList(treasure);
    }

    public int getScopaCount() {
        return scopaCount;
    }

    public List<Card> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public Long getUserId() {
        return userId;
    }
}

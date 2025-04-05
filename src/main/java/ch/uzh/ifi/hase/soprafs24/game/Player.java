package ch.uzh.ifi.hase.soprafs24.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;

public class Player {

    private final String userId;
    private final List<Card> hand;

    public Player(String userId, List<Card> initialHand) {
        this.userId = userId;
        this.hand = new ArrayList<>(initialHand);
    }

    public Card pickPlayedCard(Card playedCard) {
        if (!hand.contains(playedCard)) {
            throw new IllegalArgumentException("not present in the player's hand");
        }
        hand.remove(playedCard);
        return playedCard;
    }

    public List<Card> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public String getUserId() {
        return userId;
    }
}

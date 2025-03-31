package ch.uzh.ifi.hase.soprafs24.game.items;

import java.util.HashMap;
import java.util.Map;

public class CardFactory {
    private static final Map<String, Card> cardCache = new HashMap<>();

    public static Card getCard(Suit suit, int value) {
        String key = suit.toString() + "-" + value;
        if (!cardCache.containsKey(key)) {
            cardCache.put(key, Card.create(suit, value));
        }
        return cardCache.get(key);
    }
}

package ch.uzh.ifi.hase.soprafs24.game.items;

import java.util.HashMap;
import java.util.Map;

public class CardFactory {

    private CardFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<String, Card> cardCache = new HashMap<>();

    public static Card getCard(Suit suit, int value) {
        String key = suit.toString() + "-" + value;
        return cardCache.computeIfAbsent(key, k -> Card.create(suit, value));
    }
}
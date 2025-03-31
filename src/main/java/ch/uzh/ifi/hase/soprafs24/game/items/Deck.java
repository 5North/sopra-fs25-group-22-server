package ch.uzh.ifi.hase.soprafs24.game.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 10; value++) {
                cards.add(new Card(suit, value));
            }
        }
        do {
            shuffle();
        } while (!checkCorrectness());
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public boolean checkCorrectness() {
        if (cards.size() < 4) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            sum += cards.get(i).getValue();
        }
        return sum > 10;
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
}

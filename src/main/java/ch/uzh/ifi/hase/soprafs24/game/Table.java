package ch.uzh.ifi.hase.soprafs24.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.uzh.ifi.hase.soprafs24.game.items.Card;

public class Table {

    private final List<Card> cardsOnTable;

    public Table(List<Card> initialCards) {
        if (initialCards == null || initialCards.size() != 4) {
            throw new IllegalArgumentException("Initial cards list must contain exactly 4 cards.");
        }
        this.cardsOnTable = new ArrayList<>(initialCards);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cardsOnTable);
    }
}

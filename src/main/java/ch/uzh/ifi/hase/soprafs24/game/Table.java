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

    public List<List<Card>> getCaptureOptions(Card playedCard) {
        int target = playedCard.getValue();
        List<List<Card>> results = new ArrayList<>();
        findSubsets(cardsOnTable, 0, target, new ArrayList<>(), results);
        List<List<Card>> singleCardOptions = new ArrayList<>();
        for (List<Card> option : results) {
            if (option.size() == 1) {
                singleCardOptions.add(option);
            }
        }
        if (!singleCardOptions.isEmpty()) {
            return singleCardOptions;
        }
        return results;
    }

    private void findSubsets(List<Card> cards, int index, int target, List<Card> current, List<List<Card>> results) {
        if (target == 0) {
            results.add(new ArrayList<>(current));
            return;
        }
        if (target < 0 || index >= cards.size()) {
            return;
        }
        Card card = cards.get(index);
        current.add(card);
        findSubsets(cards, index + 1, target - card.getValue(), current, results);
        current.remove(current.size() - 1);
        findSubsets(cards, index + 1, target, current, results);
    }

    public void applyCaptureOption(List<Card> selectedOption) {
        if (selectedOption == null)
            return;
        cardsOnTable.removeAll(selectedOption);
    }

    public void addCard(Card card) {
        if (card != null) {
            cardsOnTable.add(card);
        }
    }

    public boolean isEmpty() {
        return cardsOnTable.isEmpty();
    }
}

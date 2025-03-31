package ch.uzh.ifi.hase.soprafs24.game.items;

import java.util.Objects;

public class Card {
    private final Suit suit;
    private final int value;

    private Card(Suit suit, int value) {
        if (value < 1 || value > 10) {
            throw new IllegalArgumentException("Card value must be between 1 and 10");
        }
        this.suit = suit;
        this.value = value;
    }

    static Card create(Suit suit, int value) {
        return new Card(suit, value);
    }

    public Suit getSuit() {
        return suit;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Card))
            return false;
        Card card = (Card) o;
        return value == card.value && suit == card.suit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, value);
    }
}

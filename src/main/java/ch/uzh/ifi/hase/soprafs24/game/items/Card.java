package ch.uzh.ifi.hase.soprafs24.game.items;

public class Card {
    private final Suit suit;
    private final int value;

    public Card(Suit suit, int value) {
        if (value < 1 || value > 10) {
            throw new IllegalArgumentException("Card value must be between 1 and 10");
        }
        this.suit = suit;
        this.value = value;
    }
    public Suit getSuit() {return suit;}

    public int getValue() {return value;}
}

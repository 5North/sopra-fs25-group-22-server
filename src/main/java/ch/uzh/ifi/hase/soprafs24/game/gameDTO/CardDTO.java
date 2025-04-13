package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class CardDTO {
    private String suit;
    private int value;

    public CardDTO() {
    }

    public CardDTO(String suit, int value) {
        this.suit = suit;
        this.value = value;
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}

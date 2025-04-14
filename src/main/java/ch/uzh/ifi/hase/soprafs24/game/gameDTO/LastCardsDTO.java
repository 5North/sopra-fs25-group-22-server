package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

import java.util.List;

public class LastCardsDTO {
    private Long userId;
    private List<CardDTO> cards;

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public List<CardDTO> getCards() {
        return cards;
    }
    public void setCards(List<CardDTO> cards) {
        this.cards = cards;
    }
}
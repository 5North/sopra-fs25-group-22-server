package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

import java.util.List;

public class PrivatePlayerDTO {
    private Long userId;
    private List<CardDTO> handCards;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CardDTO> getHandCards() {
        return handCards;
    }

    public void setHandCards(List<CardDTO> handCards) {
        this.handCards = handCards;
    }
}

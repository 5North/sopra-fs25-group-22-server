// src/main/java/ch/uzh/ifi/hase/soprafs24/game/gameDTO/MoveActionDTO.java
package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

import java.util.List;

public class MoveActionDTO {
    private Long playerId;
    private CardDTO playedCard;
    private List<CardDTO> pickedCards;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public CardDTO getPlayedCard() {
        return playedCard;
    }

    public void setPlayedCard(CardDTO playedCard) {
        this.playedCard = playedCard;
    }

    public List<CardDTO> getPickedCards() {
        return pickedCards;
    }

    public void setPickedCards(List<CardDTO> pickedCards) {
        this.pickedCards = pickedCards;
    }
}

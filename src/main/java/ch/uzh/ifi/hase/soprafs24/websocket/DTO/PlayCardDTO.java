package ch.uzh.ifi.hase.soprafs24.websocket.DTO;

import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;

public class PlayCardDTO {

    private Long lobbyId;

    private CardDTO Card;

    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

    public Long getLobbyId() {return lobbyId;}

    public CardDTO getCard() {return Card;}

    public void setCard(CardDTO card) {this.Card = card;}

}


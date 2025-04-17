package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

/**
 * DTO per la richiesta di giocare una carta via WebSocket.
 */
public class PlayCardDTO {
    private Long lobbyId;
    private CardDTO card;

    public PlayCardDTO() {
    }

    public PlayCardDTO(Long lobbyId, CardDTO card) {
        this.lobbyId = lobbyId;
        this.card = card;
    }

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public CardDTO getCard() {
        return card;
    }

    public void setCard(CardDTO card) {
        this.card = card;
    }
}
package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

import java.util.List;

/**
 * DTO per la scelta dell'opzione di cattura via WebSocket.
 */
public class ChosenCaptureDTO {
    private Long gameId;
    private List<CardDTO> chosenOption;

    public ChosenCaptureDTO() {
    }

    public ChosenCaptureDTO(Long gameId, List<CardDTO> chosenOption) {
        this.gameId = gameId;
        this.chosenOption = chosenOption;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public List<CardDTO> getChosenOption() {
        return chosenOption;
    }

    public void setChosenOption(List<CardDTO> chosenOption) {
        this.chosenOption = chosenOption;
    }
}
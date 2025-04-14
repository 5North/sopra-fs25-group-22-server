package ch.uzh.ifi.hase.soprafs24.websocket.DTO;

import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;

import java.util.List;

public class ChosenCaptureDTO {

    private Long gameId;

    private List<CardDTO> chosenOption;

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getGameId() {return gameId;}

    public void setChosenOption(List<CardDTO> chosenOption) {this.chosenOption = chosenOption;}

    public List<CardDTO> getChosenOption() {return chosenOption;}

}

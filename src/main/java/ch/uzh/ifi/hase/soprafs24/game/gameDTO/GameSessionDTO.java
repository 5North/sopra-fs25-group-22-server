package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

import java.util.List;

public class GameSessionDTO {
    private Long gameId;
    private List<CardDTO> tableCards;
    private List<PlayerInfoDTO> players;
    private Long currentPlayerId;

    // Getters and setters
    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public List<CardDTO> getTableCards() {
        return tableCards;
    }

    public void setTableCards(List<CardDTO> tableCards) {
        this.tableCards = tableCards;
    }

    public List<PlayerInfoDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfoDTO> players) {
        this.players = players;
    }

    public Long getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(Long currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }
}

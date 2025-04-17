package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class AIRequestDTO {
    private Long gameId;

    public AIRequestDTO() {
    }

    public AIRequestDTO(Long gameId) {
        this.gameId = gameId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
}

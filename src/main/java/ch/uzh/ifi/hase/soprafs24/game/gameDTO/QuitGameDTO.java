package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

/**
 * DTO for client request to quit a game.
 */
public class QuitGameDTO {
    private Long gameId;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
}
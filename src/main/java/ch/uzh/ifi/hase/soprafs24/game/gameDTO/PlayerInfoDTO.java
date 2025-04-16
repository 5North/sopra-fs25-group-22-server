package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class PlayerInfoDTO {
    private Long userId;
    private int handSize;
    private int scopaCount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getHandSize() {
        return handSize;
    }

    public void setHandSize(int handSize) {
        this.handSize = handSize;
    }

    public int getScopaCount() {
        return scopaCount;
    }

    public void setScopaCount(int scopaCount) {
        this.scopaCount = scopaCount;
    }
}

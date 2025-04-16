package ch.uzh.ifi.hase.soprafs24.game.gameDTO;

public class ResultDTO {
    private Long gameId;
    private Long userId;
    private String outcome;
    private int myTotal;
    private int otherTotal;

    private int myCarteResult;
    private int myDenariResult;
    private int myPrimieraResult;
    private int mySettebelloResult;
    private int myScopaResult;

    private int otherCarteResult;
    private int otherDenariResult;
    private int otherPrimieraResult;
    private int otherSettebelloResult;
    private int otherScopaResult;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public int getMyTotal() {
        return myTotal;
    }

    public void setMyTotal(int myTotal) {
        this.myTotal = myTotal;
    }

    public int getOtherTotal() {
        return otherTotal;
    }

    public void setOtherTotal(int otherTotal) {
        this.otherTotal = otherTotal;
    }

    public int getMyCarteResult() {
        return myCarteResult;
    }

    public void setMyCarteResult(int myCarteResult) {
        this.myCarteResult = myCarteResult;
    }

    public int getMyDenariResult() {
        return myDenariResult;
    }

    public void setMyDenariResult(int myDenariResult) {
        this.myDenariResult = myDenariResult;
    }

    public int getMyPrimieraResult() {
        return myPrimieraResult;
    }

    public void setMyPrimieraResult(int myPrimieraResult) {
        this.myPrimieraResult = myPrimieraResult;
    }

    public int getMySettebelloResult() {
        return mySettebelloResult;
    }

    public void setMySettebelloResult(int mySettebelloResult) {
        this.mySettebelloResult = mySettebelloResult;
    }

    public int getMyScopaResult() {
        return myScopaResult;
    }

    public void setMyScopaResult(int myScopaResult) {
        this.myScopaResult = myScopaResult;
    }

    public int getOtherCarteResult() {
        return otherCarteResult;
    }

    public void setOtherCarteResult(int otherCarteResult) {
        this.otherCarteResult = otherCarteResult;
    }

    public int getOtherDenariResult() {
        return otherDenariResult;
    }

    public void setOtherDenariResult(int otherDenariResult) {
        this.otherDenariResult = otherDenariResult;
    }

    public int getOtherPrimieraResult() {
        return otherPrimieraResult;
    }

    public void setOtherPrimieraResult(int otherPrimieraResult) {
        this.otherPrimieraResult = otherPrimieraResult;
    }

    public int getOtherSettebelloResult() {
        return otherSettebelloResult;
    }

    public void setOtherSettebelloResult(int otherSettebelloResult) {
        this.otherSettebelloResult = otherSettebelloResult;
    }

    public int getOtherScopaResult() {
        return otherScopaResult;
    }

    public void setOtherScopaResult(int otherScopaResult) {
        this.otherScopaResult = otherScopaResult;
    }
}

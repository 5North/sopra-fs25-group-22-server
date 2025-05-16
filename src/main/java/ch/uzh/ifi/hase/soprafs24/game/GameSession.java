package ch.uzh.ifi.hase.soprafs24.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ch.uzh.ifi.hase.soprafs24.game.result.Outcome;
import ch.uzh.ifi.hase.soprafs24.game.result.Result;
import ch.uzh.ifi.hase.soprafs24.utils.GameStatisticsUtil;

import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.Deck;

public class GameSession {

    private final Long gameId;
    private final List<Player> players;
    private final Deck deck;
    private final Table table;
    private int currentPlayerIndex;
    private int lastGetterIndex;
    private int turnCounter;
    private static final int TOTAL_TURNS = 36;
    private Card lastCardPlayed;
    private List<Card> lastPickedCards;

    // new flag to track if we're waiting on a choice
    private boolean choosing = false;

    public GameSession(Long gameId, List<Long> playerIds) {
        this.gameId = gameId;
        this.deck = new Deck();

        List<Card> deckCards = new ArrayList<>(deck.getCards());
        List<Card> tableCards = new ArrayList<>(deckCards.subList(0, 4));

        this.table = new Table(tableCards);
        this.players = new ArrayList<>();

        int cardIndex = 4;
        int cardsPerPlayer = 9;
        for (Long playerId : playerIds) {
            List<Card> hand = new ArrayList<>(deckCards.subList(cardIndex, cardIndex + cardsPerPlayer));
            cardIndex += cardsPerPlayer;
            players.add(new Player(playerId, hand));
        }

        this.currentPlayerIndex = new Random().nextInt(players.size());
        this.lastGetterIndex = -1;
        this.turnCounter = 0;
    }

    // existing getters...
    public Long getGameId() {
        return gameId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Deck getDeck() {
        return deck;
    }

    public Table getTable() {
        return table;
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public int getLastGetterIndex() {
        return lastGetterIndex;
    }

    public int getTurnCounter() {
        return turnCounter;
    }

    public boolean isGameOver() {
        return turnCounter >= TOTAL_TURNS;
    }

    public void setLastCardPlayed(Card lastCardPlayed) {
        this.lastCardPlayed = lastCardPlayed;
    }

    public Card getLastCardPlayed() {
        return lastCardPlayed;
    }

    public void setLastPickedCards(List<Card> lastPickedCards) {
        this.lastPickedCards = lastPickedCards;
    }

    public List<Card> getLastCardPickedCards() {
        return lastPickedCards;
    }

    public Long getLastPickedPlayerId() {
        return players.get(lastGetterIndex).getUserId();
    }

    public Player getPlayerById(Long playerId) {
        return players.stream()
                .filter(p -> p.getUserId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    // new getter for choosing state
    public boolean isChoosing() {
        return choosing;
    }

    public void playTurn(Card playedCard, List<Card> selectedOption) {
        Player currentPlayer = players.get(currentPlayerIndex);
        Card cardPlayed;

        // reset choosing each time we enter
        choosing = false;

        if (selectedOption == null || selectedOption.isEmpty()) {
            cardPlayed = currentPlayer.pickPlayedCard(playedCard);
            this.setLastCardPlayed(cardPlayed);
        } else {
            if (this.getLastCardPlayed() == null) {
                throw new IllegalArgumentException("No last card played available for processing capture selection.");
            }
            cardPlayed = this.getLastCardPlayed();
            this.setLastPickedCards(selectedOption);
        }

        List<List<Card>> captureOptions = table.getCaptureOptions(cardPlayed);
        boolean captureOccurred = false;
        List<Card> capturedCards = new ArrayList<>();

        if (!captureOptions.isEmpty()) {
            List<Card> optionToApply = null;
            if (captureOptions.size() == 1) {
                optionToApply = captureOptions.get(0);
                this.setLastPickedCards(optionToApply);
            } else if (selectedOption != null && !selectedOption.isEmpty()) {
                boolean valid = captureOptions.stream().anyMatch(opt -> opt.equals(selectedOption));
                if (!valid)
                    throw new IllegalArgumentException("Selected capture option is not valid.");
                optionToApply = selectedOption;
            } else {
                // now waiting for client choice
                choosing = true;
                throw new IllegalStateException("Multiple capture options exist; a selection must be provided.");
            }

            table.applyCaptureOption(optionToApply);
            captureOccurred = true;
            capturedCards.addAll(optionToApply);
        } else {
            this.setLastPickedCards(new ArrayList<>());
            table.addCard(cardPlayed);
        }

        if (captureOccurred) {
            boolean isScopa = (turnCounter != TOTAL_TURNS - 1) && table.isEmpty();
            lastGetterIndex = currentPlayerIndex;
            List<Card> cardsToCollect = new ArrayList<>();
            cardsToCollect.add(cardPlayed);
            cardsToCollect.addAll(capturedCards);
            currentPlayer.collectCards(cardsToCollect, isScopa);
            this.setLastCardPlayed(cardPlayed);
        }

        turnCounter++;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public void finishGame() {
        if (!table.isEmpty() && lastGetterIndex != -1) {
            Player lastGetter = players.get(lastGetterIndex);
            List<Card> remainingCards = new ArrayList<>(table.getCards());
            table.clearTable();
            lastGetter.collectCards(remainingCards, false);
        }
    }

    public Result calculateResult() {
        Result result = new Result(this.gameId, this.players);
        GameStatisticsUtil.updateUserStatistics(result);
        return result;
    }

    public Map<Long, String> finishForfeit(Long quittingUserId) {
        Map<Long, String> outcomes = new HashMap<>();
        int quittingIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUserId().equals(quittingUserId)) {
                quittingIndex = i;
                break;
            }
        }
        if (quittingIndex < 0) {
            throw new IllegalArgumentException("Quitting user not part of this game");
        }
        boolean quittingTeamEven = (quittingIndex % 2 == 0);
        for (int i = 0; i < players.size(); i++) {
            Long uid = players.get(i).getUserId();
            boolean sameTeam = ((i % 2 == 0) == quittingTeamEven);
            String outcome = sameTeam ? Outcome.LOST.name() : Outcome.WON.name();
            outcomes.put(uid, outcome);
            if (Outcome.WON.name().equals(outcome)) {
                GameStatisticsUtil.incrementWin(uid);
            } else {
                GameStatisticsUtil.incrementLoss(uid);
            }
        }
        return outcomes;
    }
}
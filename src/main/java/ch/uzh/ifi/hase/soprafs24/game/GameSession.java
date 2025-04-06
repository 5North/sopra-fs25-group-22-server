package ch.uzh.ifi.hase.soprafs24.game;

import java.util.ArrayList;
import java.util.List;

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
            Player player = new Player(playerId, hand);
            players.add(player);
        }

        this.currentPlayerIndex = 0;
        this.lastGetterIndex = -1;
        this.turnCounter = 0;
    }

    // Getters
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

    /**
     * Processes a single turn.
     * 
     * The flow is:
     * 1. The current player plays the card (removing it from their hand).
     * 2. The Table is queried for capture options for the played card.
     * - If there are capture options:
     * * If only one option exists, it is applied automatically.
     * * If multiple options exist, the GameService must supply the selectedOption
     * based on the client’s choice.
     * - If no capture option exists, the played card is added to the Table.
     * 3. If a capture occurred, the current player collects the played card plus
     * the captured cards.
     * * If the Table becomes empty, this counts as a scopa, except in the last
     * turn.
     * 4. The turnCounter is incremented and the turn passes to the next player.
     *
     * @param playedCard     The card that the current player wants to play.
     * @param selectedOption The capture option chosen by the client (if multiple
     *                       options exist); can be null if only one option exists.
     */
    public void playTurn(Card playedCard, List<Card> selectedOption) {
        Player currentPlayer = players.get(currentPlayerIndex);

        Card cardPlayed = currentPlayer.pickPlayedCard(playedCard);

        List<List<Card>> captureOptions = table.getCaptureOptions(cardPlayed);
        boolean captureOccurred = false;
        List<Card> capturedCards = new ArrayList<>();

        if (!captureOptions.isEmpty()) {
            List<Card> optionToApply = null;
            if (captureOptions.size() == 1) {
                optionToApply = captureOptions.get(0);
            } else if (selectedOption != null) {
                boolean valid = false;
                for (List<Card> option : captureOptions) {
                    if (option.equals(selectedOption)) {
                        valid = true;
                        optionToApply = option;
                        break;
                    }
                }
                if (!valid) {
                    throw new IllegalArgumentException("Selected capture option is not valid.");
                }
            } else {
                throw new IllegalStateException("Multiple capture options exist; a selection must be provided.");
            }

            table.applyCaptureOption(optionToApply);
            captureOccurred = true;
            capturedCards.addAll(optionToApply);
        } else {
            table.addCard(cardPlayed);
        }

        if (captureOccurred) {
            boolean isScopa = (turnCounter != TOTAL_TURNS - 1) && table.isEmpty();
            lastGetterIndex = currentPlayerIndex;
            List<Card> cardsToCollect = new ArrayList<>();
            cardsToCollect.add(cardPlayed);
            cardsToCollect.addAll(capturedCards);
            currentPlayer.collectCards(cardsToCollect, isScopa);
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
}

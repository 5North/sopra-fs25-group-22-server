package ch.uzh.ifi.hase.soprafs24.game;

import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.hase.soprafs24.game.items.Deck;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;

public class GameSession {

    private final Long lobbyId;
    private final List<Player> players;
    private final Deck deck;
    private final Table table;
    private int currentPlayerIndex;
    private int lastGetterIndex;

    public GameSession(Long lobbyId, List<Long> playerIds) {
        this.lobbyId = lobbyId;
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
    }

    public Long getLobbyId() {
        return lobbyId;
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

    // Metodi per la gestione dei turni e la logica di gioco verranno aggiunti nei
    // prossimi step.
}

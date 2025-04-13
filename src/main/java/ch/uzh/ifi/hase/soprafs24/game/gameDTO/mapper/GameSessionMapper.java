package ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PlayerInfoDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;

import java.util.List;
import java.util.stream.Collectors;

public class GameSessionMapper {

    /**
     * Convert a GameSession into a GameSessionDTO for broadcast.
     * This DTO contains public information such as the game ID, the state of the
     * table (as a list of CardDTO),
     * a summary for each player (with player ID, number of cards left, scopa
     * count),
     * and the ID of the current player.
     *
     * @param gameSession the game session to map.
     * @return a GameSessionDTO containing public game state.
     */
    public static GameSessionDTO convertToDTO(GameSession gameSession) {
        GameSessionDTO dto = new GameSessionDTO();
        dto.setGameId(gameSession.getGameId());
        // Map the table cards to DTOs
        List<CardDTO> tableCards = gameSession.getTable().getCards().stream()
                .map(GameSessionMapper::convertCardToDTO)
                .collect(Collectors.toList());
        dto.setTableCards(tableCards);

        // Map each player to a public player info DTO (contains userId, handSize,
        // scopaCount)
        List<PlayerInfoDTO> playerInfoDTOs = gameSession.getPlayers().stream()
                .map(GameSessionMapper::convertPlayerToInfoDTO)
                .collect(Collectors.toList());
        dto.setPlayers(playerInfoDTOs);

        // Set the current player id (the player whose turn it is)
        dto.setCurrentPlayerId(gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex()).getUserId());

        return dto;
    }

    /**
     * Convert a Player into a PrivatePlayerDTO for a personal update.
     * This DTO includes the full hand of cards for the player.
     *
     * @param player the player to map.
     * @return a PrivatePlayerDTO containing detailed private state.
     */
    public static PrivatePlayerDTO convertPlayerToPrivateDTO(Player player) {
        PrivatePlayerDTO dto = new PrivatePlayerDTO();
        dto.setUserId(player.getUserId());
        List<CardDTO> handCards = player.getHand().stream()
                .map(GameSessionMapper::convertCardToDTO)
                .collect(Collectors.toList());
        dto.setHandCards(handCards);
        return dto;
    }

    /**
     * Convert a Card entity to a CardDTO.
     *
     * @param card the card to map.
     * @return the CardDTO with its properties.
     */
    public static CardDTO convertCardToDTO(Card card) {
        CardDTO dto = new CardDTO();
        dto.setSuit(card.getSuit().toString());
        dto.setValue(card.getValue());
        return dto;
    }

    /**
     * Convert a Player into a public PlayerInfoDTO.
     * This DTO includes only the information that other players should see, such as
     * the number of cards in hand
     * (but not the cards themselves) and the scopa count.
     *
     * @param player the player to convert.
     * @return the PlayerInfoDTO containing public info.
     */
    public static PlayerInfoDTO convertPlayerToInfoDTO(Player player) {
        PlayerInfoDTO dto = new PlayerInfoDTO();
        dto.setUserId(player.getUserId());
        dto.setHandSize(player.getHand().size());
        dto.setScopaCount(player.getScopaCount());
        return dto;
    }
}

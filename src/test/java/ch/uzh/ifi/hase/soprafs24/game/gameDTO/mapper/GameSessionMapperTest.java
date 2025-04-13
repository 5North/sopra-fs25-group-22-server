package ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper.GameSessionMapper;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameSessionMapperTest {

    @Test
    public void testConvertToDTO_returnsNonNullDTO() {
        GameSession gameSession = new GameSession(999L, List.of(1L, 2L, 3L, 4L));

        GameSessionDTO dto = GameSessionMapper.convertToDTO(gameSession);
        assertNotNull(dto, "Mapped GameSessionDTO should not be null.");

        assertEquals(999L, dto.getGameId());
    }

    @Test
    public void testConvertPlayerToPrivateDTO_returnsExpectedStructure() {
        Player player = new Player(5L, List.of(
                CardFactory.getCard(Suit.COPPE, 2),
                CardFactory.getCard(Suit.DENARI, 5)));

        ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO privateDTO = GameSessionMapper
                .convertPlayerToPrivateDTO(player);
        assertNotNull(privateDTO);
        assertEquals(5L, privateDTO.getUserId());
        assertNotNull(privateDTO.getHandCards());
        assertEquals(player.getHand().size(), privateDTO.getHandCards().size());
    }
}

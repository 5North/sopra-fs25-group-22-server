package ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PlayerInfoDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.ResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameSessionMapperTest {

    @Test
    public void testConvertToCardDTO() {
        Card card = CardFactory.getCard(Suit.DENARI, 7);
        CardDTO dto = GameSessionMapper.convertToCardDTO(card);
        assertNotNull(dto);
        assertEquals("DENARI", dto.getSuit());
        assertEquals(7, dto.getValue());
    }

    @Test
    public void testConvertCardDTOtoEntity() {
        CardDTO dto = new CardDTO("coppe", 3);
        Card card = GameSessionMapper.convertCardDTOtoEntity(dto);
        assertNotNull(card);
        assertEquals(Suit.COPPE, card.getSuit());
        assertEquals(3, card.getValue());
    }

    @Test
    public void testConvertCaptureOptionsToDTO() {
        List<Card> option1 = Collections.singletonList(CardFactory.getCard(Suit.BASTONI, 2));
        List<Card> option2 = Arrays.asList(CardFactory.getCard(Suit.SPADE, 5), CardFactory.getCard(Suit.DENARI, 7));
        List<List<Card>> captureOptions = Arrays.asList(option1, option2);

        List<List<CardDTO>> dtoOptions = GameSessionMapper.convertCaptureOptionsToDTO(captureOptions);
        assertNotNull(dtoOptions);
        assertEquals(2, dtoOptions.size());

        List<CardDTO> dtoOption1 = dtoOptions.get(0);
        assertEquals(1, dtoOption1.size());
        assertEquals("BASTONI", dtoOption1.get(0).getSuit());
        assertEquals(2, dtoOption1.get(0).getValue());

        List<CardDTO> dtoOption2 = dtoOptions.get(1);
        assertEquals(2, dtoOption2.size());
        assertEquals("SPADE", dtoOption2.get(0).getSuit());
        assertEquals(5, dtoOption2.get(0).getValue());
        assertEquals("DENARI", dtoOption2.get(1).getSuit());
        assertEquals(7, dtoOption2.get(1).getValue());
    }

    @Test
    public void testConvertCardDTOListToEntity() {
        List<CardDTO> dtoList = Arrays.asList(
                new CardDTO("denari", 1),
                new CardDTO("coppe", 2),
                new CardDTO("bastoni", 3));
        List<Card> cards = GameSessionMapper.convertCardDTOListToEntity(dtoList);
        assertNotNull(cards);
        assertEquals(3, cards.size());
        assertEquals(Suit.DENARI, cards.get(0).getSuit());
        assertEquals(1, cards.get(0).getValue());
        assertEquals(Suit.COPPE, cards.get(1).getSuit());
        assertEquals(2, cards.get(1).getValue());
        assertEquals(Suit.BASTONI, cards.get(2).getSuit());
        assertEquals(3, cards.get(2).getValue());
    }

    @Test
    public void testConvertToPrivatePlayerDTO() {
        List<Card> hand = new ArrayList<>();
        hand.add(CardFactory.getCard(Suit.BASTONI, 4));
        hand.add(CardFactory.getCard(Suit.SPADE, 8));
        Player player = new Player(300L, hand);

        PrivatePlayerDTO dto = GameSessionMapper.convertToPrivatePlayerDTO(player);
        assertNotNull(dto);
        assertEquals(300L, dto.getUserId());
        assertNotNull(dto.getHandCards());
        assertEquals(2, dto.getHandCards().size());
        assertEquals("BASTONI", dto.getHandCards().get(0).getSuit());
        assertEquals(4, dto.getHandCards().get(0).getValue());
        assertEquals("SPADE", dto.getHandCards().get(1).getSuit());
        assertEquals(8, dto.getHandCards().get(1).getValue());
    }

    @Test
    public void testConvertResultToDTO() {
        ResultDTO dto = GameSessionMapper.convertResultToDTO(
                new ch.uzh.ifi.hase.soprafs24.game.result.Result(777L, Arrays.asList(
                        new Player(100L, new ArrayList<>()),
                        new Player(200L, new ArrayList<>()),
                        new Player(300L, new ArrayList<>()),
                        new Player(400L, new ArrayList<>()))),
                100L);
        assertNotNull(dto);
        assertNotEquals("UNKNOWN", dto.getOutcome());
    }
}

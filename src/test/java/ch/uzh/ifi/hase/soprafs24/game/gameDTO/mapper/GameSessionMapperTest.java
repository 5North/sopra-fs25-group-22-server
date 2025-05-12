package ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.Table;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.CardDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.GameSessionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.LastCardsDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.MoveActionDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PlayerInfoDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.PrivatePlayerDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.QuitGameResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.ResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.game.result.Result;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

 class GameSessionMapperTest {

    @Test
     void testConvertToCardDTO() {
        Card card = CardFactory.getCard(Suit.DENARI, 7);
        CardDTO dto = GameSessionMapper.convertToCardDTO(card);
        assertNotNull(dto);
        assertEquals("DENARI", dto.getSuit());
        assertEquals(7, dto.getValue());
    }

    @Test
     void testConvertCardDTOtoEntity() {
        CardDTO dto = new CardDTO("coppe", 3);
        Card card = GameSessionMapper.convertCardDTOtoEntity(dto);
        assertNotNull(card);
        assertEquals(Suit.COPPE, card.getSuit());
        assertEquals(3, card.getValue());
    }

    @Test
     void testConvertCaptureOptionsToDTO() {
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
     void testConvertCardDTOListToEntity() {
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
     void testConvertToPrivatePlayerDTO() {
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
     void testConvertResultToDTO() {
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

    @Test
     void testConvertToGameSessionDTO() {
        List<Long> playerIds = Arrays.asList(1L, 2L, 3L, 4L);
        GameSession gameSession = new GameSession(999L, playerIds);

        GameSessionDTO dto = GameSessionMapper.convertToGameSessionDTO(gameSession);

        assertEquals(999L, dto.getGameId());

        assertNotNull(dto.getTableCards());
        assertEquals(4, dto.getTableCards().size());

        assertNotNull(dto.getPlayers());
        assertEquals(4, dto.getPlayers().size());

        assertEquals(1L, dto.getCurrentPlayerId());
    }

    @Test
     void testConvertToLastCardsDTO() {
        List<Card> lastCards = Arrays.asList(
                CardFactory.getCard(Suit.DENARI, 7),
                CardFactory.getCard(Suit.SPADE, 5));

        LastCardsDTO dto = GameSessionMapper.convertToLastCardsDTO(1L, lastCards);

        assertEquals(1L, dto.getUserId());

        assertNotNull(dto.getCards());
        assertEquals(2, dto.getCards().size());

        CardDTO card1 = dto.getCards().get(0);
        CardDTO card2 = dto.getCards().get(1);

        assertEquals("DENARI", card1.getSuit());
        assertEquals(7, card1.getValue());

        assertEquals("SPADE", card2.getSuit());
        assertEquals(5, card2.getValue());
    }

    @Test
     void testConvertCaptureOptionsToDTO_empty() {
        List<List<Card>> empty = Collections.emptyList();
        List<List<CardDTO>> dto = GameSessionMapper.convertCaptureOptionsToDTO(empty);
        assertNotNull(dto, "Should return non-null list");
        assertTrue(dto.isEmpty(), "Empty input should produce empty output");
    }

    @Test
     void testConvertToMoveActionDTO_noCaptured_withUser() {
        long userId = 42L;
        Card played = CardFactory.getCard(Suit.DENARI, 9);
        List<Card> picked = Collections.emptyList();

        MoveActionDTO dto = GameSessionMapper.convertToMoveActionDTO(userId, played, picked);

        assertNotNull(dto);
        assertEquals(userId, dto.getPlayerId(), "UserId must be preserved in DTO");

        CardDTO playedDto = dto.getPlayedCard();
        assertNotNull(playedDto);
        assertEquals("DENARI", playedDto.getSuit());
        assertEquals(9, playedDto.getValue());

        List<CardDTO> caps = dto.getPickedCards();
        assertNotNull(caps);
        assertTrue(caps.isEmpty(), "pickedCards deve essere vuota se non ci sono catture");
    }

    @Test
     void testConvertToMoveActionDTO_withCaptured_withUser() {
        long userId = 99L;
        Card played = CardFactory.getCard(Suit.COPPE, 7);
        List<Card> picked = new ArrayList<>();
        picked.add(CardFactory.getCard(Suit.SPADE, 3));
        picked.add(CardFactory.getCard(Suit.DENARI, 4));

        MoveActionDTO dto = GameSessionMapper.convertToMoveActionDTO(userId, played, picked);

        assertNotNull(dto);
        assertEquals(userId, dto.getPlayerId(), "UserId deve corrispondere a quello passato");

        CardDTO playedDto = dto.getPlayedCard();
        assertNotNull(playedDto);
        assertEquals("COPPE", playedDto.getSuit());
        assertEquals(7, playedDto.getValue());

        List<CardDTO> caps = dto.getPickedCards();
        assertNotNull(caps);
        assertEquals(2, caps.size(), "Dovrebbero esserci due carte catturate");

        CardDTO first = caps.get(0), second = caps.get(1);
        assertEquals("SPADE", first.getSuit());
        assertEquals(3, first.getValue());
        assertEquals("DENARI", second.getSuit());
        assertEquals(4, second.getValue());
    }

    @Test
     void testToQuitGameResultDTO() {
        Long userId = 123L;
        String outcome = "WON";
        String message = "You won by forfeit.";
        QuitGameResultDTO dto = GameSessionMapper.toQuitGameResultDTO(userId, outcome, message);
        assertNotNull(dto, "DTO should not be null");
        assertEquals(userId, dto.getUserId(), "UserId should match");
        assertEquals(outcome, dto.getOutcome(), "Outcome should match");
        assertEquals(message, dto.getMessage(), "Message should match");
    }

    @Test
     void testToQuitGameResultDTO_LostCase() {
        Long userId = 456L;
        String outcome = "LOST";
        String message = "You lost by forfeit.";
        QuitGameResultDTO dto = GameSessionMapper.toQuitGameResultDTO(userId, outcome, message);
        assertNotNull(dto);
        assertEquals(userId, dto.getUserId());
        assertEquals(outcome, dto.getOutcome());
        assertEquals(message, dto.getMessage());
    }

    @Test
     void testConvertToLastCardsDTO_nullList() {
        LastCardsDTO dto = GameSessionMapper.convertToLastCardsDTO(42L, null);
        assertEquals(42L, dto.getUserId());
        assertNotNull(dto.getCards());
        assertTrue(dto.getCards().isEmpty());
    }

    @Test
     void testConvertResultToDTO_secondTeamAndUnknown() {
        List<Player> players = Arrays.asList(
                new Player(1L, Collections.emptyList()),
                new Player(2L, Collections.emptyList()),
                new Player(3L, Collections.emptyList()),
                new Player(4L, Collections.emptyList()));
        Result result = new Result(77L, players);

        Long user2 = 3L;
        ResultDTO dto2 = GameSessionMapper.convertResultToDTO(result, user2);
        assertEquals(user2, dto2.getUserId());
        assertEquals(result.getTeam2().getOutcome().name(), dto2.getOutcome());
        assertEquals(result.getTeam2().getTotal(), dto2.getMyTotal());
        assertEquals(result.getTeam1().getTotal(), dto2.getOtherTotal());

        Long unknown = 999L;
        ResultDTO dtoU = GameSessionMapper.convertResultToDTO(result, unknown);
        assertEquals(unknown, dtoU.getUserId());
        assertEquals("UNKNOWN", dtoU.getOutcome());
    }

    @Test
     void testConvertToGameSessionDTO_whenTableIsNullAndNoPlayers() {
        GameSession stub = new GameSession(55L, Collections.emptyList()) {
            @Override
            public Table getTable() {
                return null;
            }

            @Override
            public List<Player> getPlayers() {
                return Collections.emptyList();
            }
        };

        GameSessionDTO dto = GameSessionMapper.convertToGameSessionDTO(stub);
        assertEquals(55L, dto.getGameId());
        assertNotNull(dto.getTableCards());
        assertTrue(dto.getTableCards().isEmpty());
        assertNotNull(dto.getPlayers());
        assertTrue(dto.getPlayers().isEmpty());
        assertNull(dto.getCurrentPlayerId());
    }

}

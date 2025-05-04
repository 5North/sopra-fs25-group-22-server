package ch.uzh.ifi.hase.soprafs24.game.gameDTO.mapper;

import ch.uzh.ifi.hase.soprafs24.game.GameSession;
import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.*;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import ch.uzh.ifi.hase.soprafs24.game.result.Result;
import ch.uzh.ifi.hase.soprafs24.game.result.TeamResult;

import java.util.ArrayList;
import java.util.List;

public class GameSessionMapper {

    public static CardDTO convertToCardDTO(Card card) {
        CardDTO dto = new CardDTO();
        dto.setSuit(card.getSuit().toString());
        dto.setValue(card.getValue());
        return dto;
    }

    public static Card convertCardDTOtoEntity(CardDTO cardDTO) {
        Suit suit = Suit.valueOf(cardDTO.getSuit().toUpperCase());
        return CardFactory.getCard(suit, cardDTO.getValue());
    }

    public static List<List<CardDTO>> convertCaptureOptionsToDTO(List<List<Card>> captureOptions) {
        List<List<CardDTO>> dtoOptions = new ArrayList<>();
        for (List<Card> option : captureOptions) {
            List<CardDTO> optionDTO = new ArrayList<>();
            for (Card card : option) {
                optionDTO.add(convertToCardDTO(card));
            }
            dtoOptions.add(optionDTO);
        }
        return dtoOptions;
    }

    public static List<Card> convertCardDTOListToEntity(List<CardDTO> cardDTOList) {
        List<Card> cards = new ArrayList<>();
        for (CardDTO dto : cardDTOList) {
            cards.add(convertCardDTOtoEntity(dto));
        }
        return cards;
    }

    public static GameSessionDTO convertToGameSessionDTO(GameSession gameSession) {
        GameSessionDTO dto = new GameSessionDTO();
        dto.setGameId(gameSession.getGameId());

        if (gameSession.getTable() != null) {
            List<CardDTO> tableCards = new ArrayList<>();
            for (Card card : gameSession.getTable().getCards()) {
                tableCards.add(convertToCardDTO(card));
            }
            dto.setTableCards(tableCards);
        } else {
            dto.setTableCards(new ArrayList<>());
        }

        List<PlayerInfoDTO> playerInfos = new ArrayList<>();
        for (Player player : gameSession.getPlayers()) {
            PlayerInfoDTO pInfo = new PlayerInfoDTO();
            pInfo.setUserId(player.getUserId());
            pInfo.setHandSize(player.getHand().size());
            pInfo.setScopaCount(player.getScopaCount());
            playerInfos.add(pInfo);
        }
        dto.setPlayers(playerInfos);

        if (!gameSession.getPlayers().isEmpty()) {
            dto.setCurrentPlayerId(gameSession.getPlayers().get(gameSession.getCurrentPlayerIndex()).getUserId());
        }
        return dto;
    }

    public static PrivatePlayerDTO convertToPrivatePlayerDTO(Player player) {
        PrivatePlayerDTO dto = new PrivatePlayerDTO();
        dto.setUserId(player.getUserId());
        List<CardDTO> handCards = new ArrayList<>();
        for (Card card : player.getHand()) {
            handCards.add(convertToCardDTO(card));
        }
        dto.setHandCards(handCards);
        return dto;
    }

    public static ResultDTO convertResultToDTO(Result result, Long userId) {
        ResultDTO dto = new ResultDTO();
        dto.setGameId(result.getGameId());
        TeamResult team1 = result.getTeam1();
        TeamResult team2 = result.getTeam2();
        if (team1.getPlayerIds().contains(userId)) {
            dto.setUserId(userId);
            dto.setOutcome(team1.getOutcome().name());
            dto.setMyTotal(team1.getTotal());
            dto.setOtherTotal(team2.getTotal());
            dto.setMyCarteResult(team1.getCarteResult());
            dto.setMyDenariResult(team1.getDenariResult());
            dto.setMyPrimieraResult(team1.getPrimieraResult());
            dto.setMySettebelloResult(team1.getSettebelloResult());
            dto.setMyScopaResult(team1.getScopaResult());
            dto.setOtherCarteResult(team2.getCarteResult());
            dto.setOtherDenariResult(team2.getDenariResult());
            dto.setOtherPrimieraResult(team2.getPrimieraResult());
            dto.setOtherSettebelloResult(team2.getSettebelloResult());
            dto.setOtherScopaResult(team2.getScopaResult());
        } else if (team2.getPlayerIds().contains(userId)) {
            dto.setUserId(userId);
            dto.setOutcome(team2.getOutcome().name());
            dto.setMyTotal(team2.getTotal());
            dto.setOtherTotal(team1.getTotal());
            dto.setMyCarteResult(team2.getCarteResult());
            dto.setMyDenariResult(team2.getDenariResult());
            dto.setMyPrimieraResult(team2.getPrimieraResult());
            dto.setMySettebelloResult(team2.getSettebelloResult());
            dto.setMyScopaResult(team2.getScopaResult());
            dto.setOtherCarteResult(team1.getCarteResult());
            dto.setOtherDenariResult(team1.getDenariResult());
            dto.setOtherPrimieraResult(team1.getPrimieraResult());
            dto.setOtherSettebelloResult(team1.getSettebelloResult());
            dto.setOtherScopaResult(team1.getScopaResult());
        } else {
            dto.setUserId(userId);
            dto.setOutcome("UNKNOWN");
        }
        return dto;
    }

    // public static MoveActionDTO convertToMoveActionDTO(Long playerId, Card
    // playedCard, List<Card> capturedCards) {
    // MoveActionDTO dto = new MoveActionDTO();
    // dto.setPlayerId(playerId);
    // dto.setPlayedCard(convertToCardDTO(playedCard));

    // List<CardDTO> capturedCardsDTO = new ArrayList<>();
    // if (capturedCards != null) {
    // for (Card card : capturedCards) {
    // capturedCardsDTO.add(convertToCardDTO(card));
    // }
    // }
    // dto.setCapturedCards(capturedCardsDTO);

    // return dto;
    // }

    public static LastCardsDTO convertToLastCardsDTO(Long userId, List<Card> lastCards) {
        LastCardsDTO dto = new LastCardsDTO();
        dto.setUserId(userId);
        List<CardDTO> cardDTOs = new ArrayList<>();
        if (lastCards != null) {
            for (Card card : lastCards) {
                cardDTOs.add(convertToCardDTO(card));
            }
        }
        dto.setCards(cardDTOs);
        return dto;
    }

    public static MoveActionDTO convertToMoveActionDTO(Long playerId, Card lastPlayed, List<Card> lastPicked) {
        MoveActionDTO dto = new MoveActionDTO();
        dto.setPlayerId(playerId);
        dto.setPlayedCard(convertToCardDTO(lastPlayed));

        List<CardDTO> pickedCardsDTO = new ArrayList<>();
        for (Card c : lastPicked) {
            pickedCardsDTO.add(convertToCardDTO(c));
        }
        dto.setPickedCards(pickedCardsDTO);

        return dto;
    }

    public static QuitGameResultDTO toQuitGameResultDTO(Long userId,
            String outcome,
            String message) {
        QuitGameResultDTO dto = new QuitGameResultDTO();
        dto.setUserId(userId);
        dto.setOutcome(outcome);
        dto.setMessage(message);
        return dto;
    }

    public static TimeLeftDTO toTimeLeftDTO(Long gameId, long remainingSeconds) {
        TimeLeftDTO dto = new TimeLeftDTO();
        dto.setGameId(gameId);
        dto.setRemainingSeconds(remainingSeconds);
        return dto;
    }

    public static TimeOutNotificationDTO toTimeOutNotificationDTO(Long timedOutPlayerId, String message) {
        TimeOutNotificationDTO dto = new TimeOutNotificationDTO();
        dto.setTimedOutPlayerId(timedOutPlayerId);
        dto.setMessage(message);
        return dto;
    }

}

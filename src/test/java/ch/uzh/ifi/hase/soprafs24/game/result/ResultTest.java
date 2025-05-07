package ch.uzh.ifi.hase.soprafs24.game.result;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.game.Player;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.ResultDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;

public class ResultTest {

    private List<Card> createCardsFromValues(List<Integer> values, Suit suit) {
        List<Card> cards = new ArrayList<>();
        for (Integer value : values) {
            cards.add(CardFactory.getCard(suit, value));
        }
        return cards;
    }

    private Player createPlayerWithTreasure(Long userId, List<Card> treasure, int scopaCount) {

        Player player = new Player(userId, new ArrayList<>());
        try {
            Field treasureField = Player.class.getDeclaredField("treasure");
            treasureField.setAccessible(true);
            treasureField.set(player, new ArrayList<>(treasure));

            Field scopaField = Player.class.getDeclaredField("scopaCount");
            scopaField.setAccessible(true);
            scopaField.setInt(player, scopaCount);
        } catch (Exception e) {
            fail("Failed to create dummy player: " + e.getMessage());
        }
        return player;
    }

    // --- Test for TeamResult ---

    @Test
    public void testPrimieraCalculation_AllSuitsPresent() {

        List<Card> treasure = new ArrayList<>();
        treasure.addAll(createCardsFromValues(List.of(7), Suit.DENARI));
        treasure.addAll(createCardsFromValues(List.of(6), Suit.COPPE));
        treasure.addAll(createCardsFromValues(List.of(1), Suit.BASTONI));
        treasure.addAll(createCardsFromValues(List.of(5), Suit.SPADE));

        Player p1 = createPlayerWithTreasure(100L, treasure, 1);
        Player p2 = createPlayerWithTreasure(300L, treasure, 2);

        TeamResult teamResult = new TeamResult(p1, p2);
        int expectedPrimiera = 21 + 18 + 16 + 15;
        assertEquals(expectedPrimiera, teamResult.getPrimieraRaw(),
                "Primiera raw should be 70 when best cards per suit are 7,6,1,5.");
    }

    @Test
    public void testPrimieraCalculation_MissingSuit() {
        List<Card> treasure = new ArrayList<>();
        treasure.addAll(createCardsFromValues(List.of(7), Suit.DENARI));
        treasure.addAll(createCardsFromValues(List.of(6), Suit.COPPE));
        treasure.addAll(createCardsFromValues(List.of(1), Suit.BASTONI));

        Player p1 = createPlayerWithTreasure(101L, treasure, 0);
        Player p2 = createPlayerWithTreasure(301L, treasure, 0);

        TeamResult teamResult = new TeamResult(p1, p2);
        assertEquals(0, teamResult.getPrimieraRaw(), "Primiera raw should be 0 if at least one suit is missing.");
    }

    @Test
    public void testFixedPointsCalculation() {

        List<Card> treasure = new ArrayList<>();

        List<Card> denariCards = createCardsFromValues(java.util.stream.IntStream.rangeClosed(1, 10)
                .boxed().toList(), Suit.DENARI);
        denariCards.add(CardFactory.getCard(Suit.DENARI, 10));
        treasure.addAll(denariCards);

        List<Card> coppeCards = createCardsFromValues(java.util.stream.IntStream.rangeClosed(1, 10)
                .boxed().toList(), Suit.COPPE);
        coppeCards.add(CardFactory.getCard(Suit.COPPE, 10));
        treasure.addAll(coppeCards);

        Player p1 = createPlayerWithTreasure(102L, treasure, 0);
        Player p2 = createPlayerWithTreasure(302L, treasure, 0);
        TeamResult teamResult = new TeamResult(p1, p2);

        assertEquals(1, teamResult.getCarteResult(), "Carte result should be 1 if more than 20 cards are collected.");
        assertEquals(1, teamResult.getDenariResult(), "Denari result should be 1 if more than 5 denari are collected.");
    }

    @Test
    public void testTotalCalculation() {

        List<Card> treasure = new ArrayList<>();

        for (int i = 0; i < 22; i++) {
            treasure.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasure.addAll(
                createCardsFromValues(java.util.stream.IntStream.rangeClosed(1, 7).boxed().toList(), Suit.DENARI));
        treasure.add(CardFactory.getCard(Suit.DENARI, 7));
        treasure.addAll(createCardsFromValues(List.of(7), Suit.SPADE));
        int scopaCount = 2;

        Player p1 = createPlayerWithTreasure(103L, treasure, scopaCount);
        Player p2 = createPlayerWithTreasure(303L, treasure, scopaCount);

        TeamResult teamResult = new TeamResult(p1, p2);

        teamResult.setComparisonResults(60);
        int expectedTotal = teamResult.getCarteResult()
                + teamResult.getDenariResult()
                + teamResult.getPrimieraResult()
                + teamResult.getSettebelloResult()
                + teamResult.getScopaResult();
        assertEquals(expectedTotal, teamResult.getTotal(),
                "Total points should equal the sum of fixed category points and scopa.");
    }

    @Test
    public void testResultOutcome_WinLoss() {

        List<Card> treasureTeam1 = new ArrayList<>();

        for (int i = 0; i < 22; i++) {
            treasureTeam1.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasureTeam1.addAll(createCardsFromValues(List.of(1, 2, 3, 7), Suit.DENARI));
        List<Card> treasureTeam2 = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            treasureTeam2.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasureTeam2.addAll(createCardsFromValues(List.of(1, 2, 3), Suit.DENARI));

        Player t1p1 = createPlayerWithTreasure(100L, treasureTeam1, 1);
        Player t1p2 = createPlayerWithTreasure(300L, treasureTeam1, 1);
        Player t2p1 = createPlayerWithTreasure(200L, treasureTeam2, 2);
        Player t2p2 = createPlayerWithTreasure(400L, treasureTeam2, 0);

        List<Player> players = new ArrayList<>();
        players.add(t1p1);
        players.add(t2p1);
        players.add(t1p2);
        players.add(t2p2);

        Result result = new Result(12345L, players);

        assertEquals(Outcome.WON, result.getTeam1().getOutcome(), "Team1 should have won.");
        assertEquals(Outcome.LOST, result.getTeam2().getOutcome(), "Team2 should have lost.");
    }

    @Test
    public void testResultOutcome_Tie() {
        List<Card> treasure = new ArrayList<>();
        for (int i = 0; i < 22; i++) {
            treasure.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasure.addAll(createCardsFromValues(List.of(1, 2, 3, 7), Suit.DENARI));

        Player t1p1 = createPlayerWithTreasure(101L, treasure, 1);
        Player t1p2 = createPlayerWithTreasure(301L, treasure, 1);
        Player t2p1 = createPlayerWithTreasure(201L, treasure, 1);
        Player t2p2 = createPlayerWithTreasure(401L, treasure, 1);

        List<Player> players = new ArrayList<>();
        players.add(t1p1);
        players.add(t2p1);
        players.add(t1p2);
        players.add(t2p2);

        Result result = new Result(54321L, players);

        assertEquals(Outcome.TIE, result.getTeam1().getOutcome(), "Team1 should be tied.");
        assertEquals(Outcome.TIE, result.getTeam2().getOutcome(), "Team2 should be tied.");
    }

    @Test
    public void testTeamResultGettersAndSetters() {

        List<Card> treasure = new ArrayList<>();
        for (int i = 0; i < 22; i++) {
            treasure.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasure.addAll(createCardsFromValues(List.of(7, 2, 3, 4, 5, 6, 8), Suit.DENARI));
        treasure.addAll(createCardsFromValues(List.of(7), Suit.SPADE));

        Player p1 = createPlayerWithTreasure(100L, treasure, 2);
        Player p2 = createPlayerWithTreasure(300L, treasure, 1);

        TeamResult teamResult = new TeamResult(p1, p2);

        List<Long> ids = teamResult.getPlayerIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(100L));
        assertTrue(ids.contains(300L));

        assertEquals(1, teamResult.getCarteResult(), "Carte result should be 1 if treasure size > 20.");
        assertEquals(1, teamResult.getDenariResult(), "Denari result should be 1 if denari count > 5.");
        assertEquals(1, teamResult.getSettebelloResult(), "Settebello result should be 1 if 7 of DENARI is present.");
        assertEquals(3, teamResult.getScopaResult(), "Scopa result should be sum of scope counts (2+1=3).");

        assertEquals(0, teamResult.getPrimieraRaw(), "Primiera raw should be 0 if not all suits are present.");

        teamResult.setComparisonResults(60);
        assertEquals(0, teamResult.getPrimieraResult(),
                "Primiera result should be 0 if team primiera raw is less than opponent's.");

        int expectedTotal = teamResult.getCarteResult() + teamResult.getDenariResult()
                + teamResult.getPrimieraResult() + teamResult.getSettebelloResult() + teamResult.getScopaResult();
        assertEquals(expectedTotal, teamResult.getTotal(),
                "Total should equal the sum of fixed category points and scopa points.");

        teamResult.setOutcome(Outcome.WON);
        assertEquals(Outcome.WON, teamResult.getOutcome(), "Outcome should be set to WON.");
    }

    // --- Test for Result ---

    @Test
    public void testResultGettersAndOutcome() {
        List<Card> treasureTeam1 = new ArrayList<>();

        for (int i = 0; i < 22; i++) {
            treasureTeam1.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasureTeam1.addAll(createCardsFromValues(List.of(7, 2, 3, 4, 5, 6), Suit.DENARI));
        treasureTeam1.addAll(createCardsFromValues(List.of(7), Suit.SPADE));

        List<Card> treasureTeam2 = new ArrayList<>();

        for (int i = 0; i < 18; i++) {
            treasureTeam2.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasureTeam2.addAll(createCardsFromValues(List.of(2, 3, 4, 5, 6), Suit.DENARI));
        treasureTeam2.addAll(createCardsFromValues(List.of(7), Suit.SPADE));

        Player t1p1 = createPlayerWithTreasure(100L, treasureTeam1, 1);
        Player t1p2 = createPlayerWithTreasure(300L, treasureTeam1, 1);
        Player t2p1 = createPlayerWithTreasure(200L, treasureTeam2, 2);
        Player t2p2 = createPlayerWithTreasure(400L, treasureTeam2, 0);

        List<Player> players = new ArrayList<>();
        players.add(t1p1);
        players.add(t2p1);
        players.add(t1p2);
        players.add(t2p2);

        Result result = new Result(12345L, players);

        assertEquals(12345L, result.getGameId(), "GameId should match.");

        List<Long> team1Ids = result.getTeam1().getPlayerIds();
        assertTrue(team1Ids.contains(100L));
        assertTrue(team1Ids.contains(300L));
        List<Long> team2Ids = result.getTeam2().getPlayerIds();
        assertTrue(team2Ids.contains(200L));
        assertTrue(team2Ids.contains(400L));

        assertEquals(Outcome.WON, result.getTeam1().getOutcome(), "Team1 should have WON.");
        assertEquals(Outcome.LOST, result.getTeam2().getOutcome(), "Team2 should have LOST.");
    }

    @Test
    public void testTeamResultSetterOutcome() {
        List<Card> treasure = new ArrayList<>();
        treasure.addAll(createCardsFromValues(List.of(7), Suit.DENARI));
        treasure.addAll(createCardsFromValues(List.of(6), Suit.COPPE));
        treasure.addAll(createCardsFromValues(List.of(1), Suit.BASTONI));
        treasure.addAll(createCardsFromValues(List.of(5), Suit.SPADE));

        Player p1 = createPlayerWithTreasure(11L, treasure, 1);
        Player p2 = createPlayerWithTreasure(12L, treasure, 2);
        TeamResult teamResult = new TeamResult(p1, p2);

        teamResult.setComparisonResults(50);
        assertEquals(1, teamResult.getPrimieraResult(),
                "PrimieraResult should be 1 when team's primiera raw > opponent's.");

        teamResult.setOutcome(Outcome.TIE);
        assertEquals(Outcome.TIE, teamResult.getOutcome(), "Outcome should be set to TIE.");
    }

    @Test
    public void testResultConstructorWithInvalidPlayers() {
        List<Player> players = new ArrayList<>();
        players.add(createPlayerWithTreasure(1L, new ArrayList<>(), 0));
        players.add(createPlayerWithTreasure(2L, new ArrayList<>(), 0));
        players.add(createPlayerWithTreasure(3L, new ArrayList<>(), 0));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Result(999L, players);
        });
        assertTrue(exception.getMessage().contains("There must be exactly 4 players."),
                "Result constructor should throw exception if players list size != 4.");
    }

    @Test
    public void testBorderlinePointsCalculation() {

        List<Card> treasureP1 = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            treasureP1.add(CardFactory.getCard(Suit.COPPE, 2));
        }

        for (int i = 0; i < 3; i++) {
            treasureP1.add(CardFactory.getCard(Suit.DENARI, 2));
        }

        List<Card> treasureP2 = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            treasureP2.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        for (int i = 0; i < 2; i++) {
            treasureP2.add(CardFactory.getCard(Suit.DENARI, 2));
        }

        Player p1 = createPlayerWithTreasure(10L, treasureP1, 0);
        Player p2 = createPlayerWithTreasure(20L, treasureP2, 0);
        TeamResult teamResult = new TeamResult(p1, p2);

        assertEquals(0, teamResult.getCarteResult(), "Exactly 20 cards should yield 0 for carteResult.");
        assertEquals(0, teamResult.getDenariResult(), "Exactly 5 denari should yield 0 for denariResult.");
    }

    @Test
    public void testTeamTotalPointsCalculation() {
        List<Card> treasure = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            treasure.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        for (int i = 1; i <= 6; i++) {
            treasure.add(CardFactory.getCard(Suit.DENARI, i));
        }
        treasure.add(CardFactory.getCard(Suit.DENARI, 7));
        treasure.add(CardFactory.getCard(Suit.BASTONI, 7));
        treasure.add(CardFactory.getCard(Suit.SPADE, 7));

        Player p1 = createPlayerWithTreasure(110L, treasure, 2);
        Player p2 = createPlayerWithTreasure(210L, treasure, 3);

        TeamResult teamResult = new TeamResult(p1, p2);
        assertEquals(1, teamResult.getCarteResult(), "Carte result should be 1 if treasure size > 20.");
        assertEquals(1, teamResult.getDenariResult(), "Denari result should be 1 if denari count > 5.");
        assertEquals(1, teamResult.getSettebelloResult(), "Settebello result should be 1 if 7 of DENARI is present.");
        assertEquals(5, teamResult.getScopaResult(), "Scopa result should be sum of scope counts (2+3=5).");

        teamResult.setComparisonResults(70);

        int expectedTotal = 1 + 1 + 1 + 1 + 5;
        assertEquals(expectedTotal, teamResult.getTotal(),
                "Total should equal the sum of fixed category points and scopa (expected " + expectedTotal + ").");
    }

    @Test
    public void testPrimieraCalculation_DefaultBranchUsingReflection() {
        Card invalidCard = CardFactory.getCard(Suit.BASTONI, 1);
        int originalValue = invalidCard.getValue();
        Field valueField = null;

        try {
            valueField = invalidCard.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            valueField.setInt(invalidCard, 11);
        } catch (Exception e) {
            fail("Unable to set the card value to 11 via reflection: " + e.getMessage());
        }

        List<Card> treasure = new ArrayList<>();
        treasure.add(invalidCard);
        treasure.addAll(createCardsFromValues(List.of(6), Suit.COPPE));
        treasure.addAll(createCardsFromValues(List.of(1), Suit.DENARI));
        treasure.addAll(createCardsFromValues(List.of(5), Suit.SPADE));

        Player p1 = createPlayerWithTreasure(777L, treasure, 0);
        Player p2 = createPlayerWithTreasure(888L, treasure, 0);

        TeamResult teamResult = new TeamResult(p1, p2);

        try {
            assertEquals(49, teamResult.getPrimieraRaw());
        } finally {
            if (valueField != null) {
                try {
                    valueField.setInt(invalidCard, originalValue);
                } catch (Exception e) {
                    fail("Unable to restore the card value via reflection: " + e.getMessage());
                }
            }
        }
    }

    @Test
    public void testResultOutcome_Loss() {

        List<Card> treasureTeam1 = new ArrayList<>();
        for (int i = 0; i < 22; i++) {
            treasureTeam1.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasureTeam1.addAll(createCardsFromValues(List.of(1, 2, 3, 7), Suit.DENARI));

        List<Card> treasureTeam2 = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            treasureTeam2.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        treasureTeam2.addAll(createCardsFromValues(List.of(1, 2, 3), Suit.DENARI));

        Player team1Player1 = createPlayerWithTreasure(100L, treasureTeam2, 1);
        Player team1Player2 = createPlayerWithTreasure(300L, treasureTeam2, 1);
        Player team2Player1 = createPlayerWithTreasure(200L, treasureTeam1, 2);
        Player team2Player2 = createPlayerWithTreasure(400L, treasureTeam1, 0);

        List<Player> players = new ArrayList<>();
        players.add(team1Player1);
        players.add(team2Player1);
        players.add(team1Player2);
        players.add(team2Player2);

        Result result = new Result(12345L, players);

        assertEquals(Outcome.LOST, result.getTeam1().getOutcome(), "Team1 should have lost.");
        assertEquals(Outcome.WON, result.getTeam2().getOutcome(), "Team2 should have won.");
    }

    @Test
    public void testTeamResultRawGetters() {
        List<Card> treasure = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            treasure.add(CardFactory.getCard(Suit.COPPE, 2));
        }
        for (int i = 0; i < 3; i++) {
            treasure.add(CardFactory.getCard(Suit.DENARI, i + 1));
        }
        treasure.add(CardFactory.getCard(Suit.BASTONI, 7));
        treasure.add(CardFactory.getCard(Suit.COPPE, 6));
        treasure.add(CardFactory.getCard(Suit.SPADE, 1));
        treasure.add(CardFactory.getCard(Suit.DENARI, 7));
        int scopaCountP1 = 1;
        int scopaCountP2 = 1;

        Player p1 = createPlayerWithTreasure(900L, treasure, scopaCountP1);
        Player p2 = createPlayerWithTreasure(901L, treasure, scopaCountP2);
        TeamResult tr = new TeamResult(p1, p2);

        assertEquals(24, tr.getCartePointsRaw());
        assertEquals(8, tr.getDenariPointsRaw());
        int primieraExpected = 21 + 18 + 16 + 21;
        assertEquals(primieraExpected, tr.getPrimieraRaw());
        assertEquals(1, tr.getSettebelloRaw());
        assertEquals(scopaCountP1 + scopaCountP2, tr.getScopaRaw());
    }

    @Test
    public void testResultDTO_GettersAndSetters() {
        ResultDTO dto = new ResultDTO();

        dto.setGameId(123L);
        dto.setUserId(456L);
        dto.setOutcome("WON");
        dto.setMyTotal(15);
        dto.setOtherTotal(10);
        dto.setMyCarteResult(7);
        dto.setMyDenariResult(4);
        dto.setMyPrimieraResult(20);
        dto.setMySettebelloResult(1);
        dto.setMyScopaResult(2);
        dto.setOtherCarteResult(5);
        dto.setOtherDenariResult(3);
        dto.setOtherPrimieraResult(18);
        dto.setOtherSettebelloResult(0);
        dto.setOtherScopaResult(1);

        assertEquals(123L, dto.getGameId());
        assertEquals(456L, dto.getUserId());
        assertEquals("WON", dto.getOutcome());
        assertEquals(15, dto.getMyTotal());
        assertEquals(10, dto.getOtherTotal());
        assertEquals(7, dto.getMyCarteResult());
        assertEquals(4, dto.getMyDenariResult());
        assertEquals(20, dto.getMyPrimieraResult());
        assertEquals(1, dto.getMySettebelloResult());
        assertEquals(2, dto.getMyScopaResult());
        assertEquals(5, dto.getOtherCarteResult());
        assertEquals(3, dto.getOtherDenariResult());
        assertEquals(18, dto.getOtherPrimieraResult());
        assertEquals(0, dto.getOtherSettebelloResult());
        assertEquals(1, dto.getOtherScopaResult());
    }

}

package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

@Service
@Transactional
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private final OpenAiClient openAiClient;

    public AIService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public String generateAISuggestion(List<Card> hand, List<Card> table) {
        String prompt = buildPrompt(hand, table);
        String raw = openAiClient.createCompletion(prompt);
        return raw;
    }

    private String buildPrompt(List<Card> hand, List<Card> table) {
        String handStr = hand.stream()
                .map(c -> c.getSuit() + "-" + c.getValue())
                .collect(Collectors.joining(", "));
        String tableStr = table.stream()
                .map(c -> c.getSuit() + "-" + c.getValue())
                .collect(Collectors.joining(", "));

        log.debug("Ai assistant prompt built");
        return String.format(
                "You are an expert Scopa player. Your hand: [%s]. Cards on table: [%s]. " +
                        "Scopa is played with a 40-card deck (Denari, Coppe, Spade, Bastoni). " +
                        "\n\n" +
                        "RULES:\n" +
                        "1. On your turn, play one card. You capture cards if your card matches the value of one card or the sum of multiple cards on the table. For example if on the table there is a 3 and a 4 and you play a 7 you capture both"
                        +
                        "If it matches a single card, you must take that one. " +
                        "If you clear the table completely, it's a 'Scopa' and scores 1 point. " +
                        "Avoid leaving the table open for an easy scopa next turn.\n" +
                        "\n" +
                        "STRATEGY:\n" +
                        "- Prioritize capturing cards like 7 of Denari, the settebello, or in general the 7s and/or Denari suits.\n"
                        +
                        "- Try to avoid to create a combination on the table that sums to 7, for example if there is a 4 do not suggest to play a three"
                        +
                        "- Avoid to play Denari if possible, avoid to play a 7 if possible" +
                        "\n" +
                        "SPECIAL CASE - EMPTY TABLE:\n" +
                        "- If there are no cards on the table, suggest playing a card of which you hold a duplicate (e.g., if you have two 5s, play one). "
                        +
                        "- If you have no duplicates in hand you must you must explicitly say 'Play whatever cards by trying to remember which cards are no longer in game.'\n"
                        +
                        "\n" +
                        "OUTPUT FORMAT:\n" +
                        "Suggest up to 3 cards to play, separated by semicolons, only when strategically sensible. " +
                        "Return exactly like: 'Play 7 of Denari; Play 4 of Coppe; Play 2 of Spade'." +
                        "For no reason return another format than 'Play 7 of Denari; Play 4 of Coppe; Play 2 of Spade' or 'Play a card by trying to remember which cards are no longer in game (already been played) and try to play one of them to minimize the risk for the next player to do SCOPA.'"
                        +
                        "If the table is not empty in any case always suggest to play something",
                handStr, tableStr);
    }
}

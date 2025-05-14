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
                        "Scopa Italian played with a 40-card deck with for suits (Denari, Coppe, Spade, Bastoni). " +
                        "On your turn, you must play one card. You take cards from the table if your card matches " +
                        "either the value of one card or the sum of multiple cards that are on table. If a single card matches the value, "
                        +
                        "you must take that one. If you take all cards on the table, it's a 'Scopa' and scores 1 point. Avoid maybe to leave tha table with possibility of scopa for the next player"
                        +
                        "You should aim to capture high-value cards like 7 of Denari (settebello), the most cards, and Denari cards. "
                        +
                        "Suggest up to 3 cards to play, separated by semicolons, but only if it makes strategic sense. "
                        +
                        "Return suggestions in this exact format: 'Play 7 of Denari; Play 4 of Coppe'.",
                handStr, tableStr);
    }
}

package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import ch.uzh.ifi.hase.soprafs24.game.gameDTO.AISuggestionDTO;
import ch.uzh.ifi.hase.soprafs24.game.items.Card;
import ch.uzh.ifi.hase.soprafs24.game.items.CardFactory;
import ch.uzh.ifi.hase.soprafs24.game.items.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
 class AIServiceTest {

    @Mock
    private OpenAiClient openAiClient;

    @InjectMocks
    private AIService aiService;

    private Card cDenari7;
    private Card cCoppe5;
    private Card cSpade3;

    @BeforeEach
     void setup() {
        cDenari7 = CardFactory.getCard(Suit.DENARI, 7);
        cCoppe5 = CardFactory.getCard(Suit.COPPE, 5);
        cSpade3 = CardFactory.getCard(Suit.SPADE, 3);
    }

    @Test
     void testGenerateAISuggestion_CallsClientWithCorrectPrompt() {
        List<Card> hand = List.of(cDenari7);
        List<Card> table = List.of(cCoppe5, cSpade3);

        when(openAiClient.createCompletion(anyString())).thenReturn("Play 7 of Denari");

        String suggestion = aiService.generateAISuggestion(hand, table);

        assertEquals("Play 7 of Denari", suggestion);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(openAiClient).createCompletion(captor.capture());
        String prompt = captor.getValue();

        assertTrue(prompt.contains("Your hand: [DENARI-7]"));
        assertTrue(prompt.contains("Cards on table: [COPPE-5, SPADE-3]"));

        assertTrue(prompt.contains("Return suggestions in this exact format"));
    }

    @Test
     void testGenerateAISuggestion_EmptyHandAndTable() {
        when(openAiClient.createCompletion(anyString())).thenReturn("No move");
        String suggestion = aiService.generateAISuggestion(List.of(), List.of());
        assertEquals("No move", suggestion);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(openAiClient).createCompletion(captor.capture());
        String prompt = captor.getValue();

        assertTrue(prompt.contains("Your hand: []"));
        assertTrue(prompt.contains("Cards on table: []"));
    }

    @Test
     void testGenerateAISuggestion_PropagatesException() {
        List<Card> hand = List.of(cDenari7);
        List<Card> table = List.of(cCoppe5);
        when(openAiClient.createCompletion(anyString()))
                .thenThrow(new RuntimeException("API error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> aiService.generateAISuggestion(hand, table));
        assertEquals("API error", ex.getMessage());
    }

    @Test
     void testAISuggestionDTO_DefaultConstructorAndSetterGetter() {
        AISuggestionDTO dto = new AISuggestionDTO();
        assertNull(dto.getSuggestion());
        dto.setSuggestion("Try this");
        assertEquals("Try this", dto.getSuggestion());
    }

    @Test
     void testAISuggestionDTO_ToString() {
        AISuggestionDTO dto = new AISuggestionDTO("Example");
        String s = dto.toString();
        assertTrue(s.startsWith("AISuggestionDTO{"));
        assertTrue(s.contains("suggestion='Example'"));
        assertTrue(s.endsWith("}"));
    }
}

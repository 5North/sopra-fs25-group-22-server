package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class OpenAiClientTest {

    private OpenAiClient client;

    @BeforeEach
    void setUp() {
        client = new OpenAiClient();
    }

    @Test
    void testCreateCompletion_Success() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.body()).thenReturn(
                "{\"choices\":[{\"message\":{\"content\":\"AI says hi\"}}]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        Field clientField = OpenAiClient.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(client, mockHttpClient);

        String result = client.createCompletion("any prompt");
        assertEquals("AI says hi", result);
    }

    @Test
    void testCreateCompletion_NetworkError() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("network failure"));

        Field clientField = OpenAiClient.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(client, mockHttpClient);

        OpenAIClientException ex = assertThrows(
                OpenAIClientException.class,
                () -> client.createCompletion("prompt"));

        assertTrue(ex.getMessage().contains("I/O error"));

        assertTrue(ex.getCause() instanceof IOException);
    }
}
